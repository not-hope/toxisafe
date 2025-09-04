package com.toxisafe.service;

import com.toxisafe.dao.BroteDao;
import com.toxisafe.dao.InformeDao;
import com.toxisafe.model.Alimento;
import com.toxisafe.model.Brote;
import com.toxisafe.model.Informe;
import com.toxisafe.model.Usuario;
import com.toxisafe.sync.SyncEmitter;

import java.sql.SQLException;
import java.util.*;

public class InformeService {

    private final InformeDao informeDao;
    private final BroteDao broteDao;
    private final BroteEncuestadorService broteEncuestadorService;
    private SyncEmitter sync;

    public InformeService(InformeDao informeDao,
                          BroteDao broteDao,
                          BroteEncuestadorService broteEncuestadorService,
                          SyncEmitter sync) {
        this.informeDao = informeDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        this.sync = sync;
    }

    public InformeService(InformeDao informeDao,
                          BroteDao broteDao,
                          BroteEncuestadorService broteEncuestadorService) {
        this.informeDao = informeDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        this.sync = null;
    }

    public void setSyncEmitter(SyncEmitter syncEmitter) {
        this.sync = syncEmitter;
    }

    /* ================== CRUD con permisos ================== */

    public Informe create(Informe informe, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(informe, "Informe requerido");
        if (isBlank(informe.getIdBrote())) throw new IllegalArgumentException("id_brote obligatorio");
        validarContenido(informe);

        // Brote debe existir (se permite informe aunque el brote esté CERRADO)
        Brote b = broteDao.findById(informe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        checkPermisoSobreBrote(actor, b.getIdBrote(), "crear informe");

        if (isBlank(informe.getIdInforme())) {
            informe.setIdInforme(UUID.randomUUID().toString());
        }
        informeDao.insert(informe);
        emitInsert("INFORME", informe.getIdInforme(), toMap(informe));
        return informe;
    }

    public void update(Informe informe, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(informe, "Informe requerido");
        if (isBlank(informe.getIdInforme())) throw new IllegalArgumentException("id_informe obligatorio");
        if (isBlank(informe.getIdBrote())) throw new IllegalArgumentException("id_brote obligatorio");
        validarContenido(informe);

        Brote b = broteDao.findById(informe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        checkPermisoSobreBrote(actor, b.getIdBrote(), "actualizar informe");

        Optional<Informe> prevOpt = informeDao.findById(informe.getIdInforme());
        Map<String,Object> prevMap = prevOpt.isPresent() ? toMap(prevOpt.get()) : null;

        informeDao.update(informe);

        emitUpdate("INFORME", informe.getIdInforme(), prevMap, toMap(informe));

    }

    public void delete(String idInforme, Usuario actor) throws SQLException {
        requireActor(actor);
        if (isBlank(idInforme)) throw new IllegalArgumentException("id_informe obligatorio");

        var infOpt = informeDao.findById(idInforme);
        if (infOpt.isEmpty()) return; // idempotente
        Informe inf = infOpt.get();

        Brote b = broteDao.findById(inf.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        checkPermisoSobreBrote(actor, b.getIdBrote(), "eliminar informe");

        Optional<Informe> prevOpt = informeDao.findById(idInforme);

        informeDao.delete(idInforme);

        prevOpt.ifPresent(prev -> emitDelete("INFORME", idInforme, toMap(prev)));

    }

    /* ================== Lectura con visibilidad ================== */

    public List<Informe> findByBroteIdVisiblePara(String broteId, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(broteId, "id_brote requerido");

        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) {
            return informeDao.findByBroteId(broteId);
        }
        if (rol.equals("ENCUESTADOR")) {
            boolean asignado = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> broteId.equals(be.getIdBrote()));
            return asignado ? informeDao.findByBroteId(broteId) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /* ================== Helpers ================== */

    private void validarContenido(Informe i) {
        String c = Optional.ofNullable(i.getContenidoInforme()).orElse("").trim();
        if (c.isEmpty()) throw new IllegalArgumentException("El contenido del informe no puede estar vacío");
    }

    private void requireActor(Usuario actor) {
        if (actor == null || isBlank(actor.getIdUsuario()))
            throw new SecurityException("Sesión no válida.");
    }

    private void checkPermisoSobreBrote(Usuario actor, String idBrote, String accion) throws SQLException {
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) return;

        if (rol.equals("ENCUESTADOR")) {
            boolean asignado = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> idBrote.equals(be.getIdBrote()));
            if (!asignado) throw new SecurityException("No tiene permiso para " + accion + " de este brote.");
            return;
        }
        throw new SecurityException("Rol no autorizado: " + actor.getRolUsuario());
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String resp(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    // ---- emisión segura (no hace nada si sync == null) ----
    private void emitInsert(String tabla, String id, Map<String,Object> data) {
        if (sync != null) sync.emitInsert(tabla, id, data);
    }
    private void emitUpdate(String tabla, String id, Map<String,Object> oldV, Map<String,Object> newV) {
        if (sync != null) sync.emitUpdate(tabla, id, oldV, newV);
    }
    private void emitDelete(String tabla, String id, Map<String,Object> oldV) {
        if (sync != null) sync.emitDelete(tabla, id, oldV);
    }

    // ---- mapeo EXACTO a lo que consume tu ingestor para la tabla INFORME ----
    private static Map<String,Object> toMap(Informe i) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_informe",        i.getIdInforme());
        m.put("id_brote",          i.getIdBrote());
        m.put("contenido_informe", i.getContenidoInforme());
        return m;
    }

}
