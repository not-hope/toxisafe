package com.toxisafe.service;

import com.toxisafe.dao.GrupoSintomaDao;
import com.toxisafe.dao.SintomaDao;
import com.toxisafe.model.Sintoma;
import com.toxisafe.model.Usuario;

import java.sql.SQLException;
import java.util.*;

public class SintomaService {

    private final SintomaDao sintomaDao;
    private final GrupoSintomaDao grupoSintomaDao;

    public SintomaService(SintomaDao sintomaDao, GrupoSintomaDao grupoSintomaDao) {
        this.sintomaDao = sintomaDao;
        this.grupoSintomaDao = grupoSintomaDao;
    }

    // ===== API =====

    public Sintoma create(Sintoma s, Usuario actor) throws SQLException {
        requireActor(actor);
        assertPermisoCatalogo(actor, "crear síntoma");
        validarSintoma(s, false);

        if (isBlank(s.getIdSintoma())) {
            s.setIdSintoma(UUID.randomUUID().toString());
        }
        sintomaDao.insert(s);
        return s;
    }

    public void update(Sintoma s, Usuario actor) throws SQLException {
        requireActor(actor);
        assertPermisoCatalogo(actor, "actualizar síntoma");
        validarSintoma(s, true);
        sintomaDao.update(s);
    }

    public void delete(String idSintoma, Usuario actor) throws SQLException {
        requireActor(actor);
        assertPermisoCatalogo(actor, "eliminar síntoma");
        if (isBlank(idSintoma)) throw new IllegalArgumentException("id_sintoma requerido");
        // Si hay EXPOSICION_SINTOMA referenciando, el ON DELETE RESTRICT lo impedirá.
        sintomaDao.delete(idSintoma);
    }

    public Optional<Sintoma> findById(String id) throws SQLException {
        return sintomaDao.findById(id);
    }

    public List<Sintoma> findAll() throws SQLException {
        return sintomaDao.findAll();
    }

    public List<Sintoma> findByGrupo(String idGrupo) throws SQLException {
        return sintomaDao.findByGrupoSintomaId(idGrupo);
    }

    // ===== Validación =====

    private void validarSintoma(Sintoma s, boolean esUpdate) throws SQLException {
        Objects.requireNonNull(s, "Sintoma requerido");
        if (esUpdate && isBlank(s.getIdSintoma())) throw new IllegalArgumentException("id_sintoma requerido");
        if (isBlank(s.getIdGrupoSintomas())) throw new IllegalArgumentException("id_grupo_sintomas obligatorio");
        if (isBlank(s.getNombreSintoma())) throw new IllegalArgumentException("nombre_sintoma obligatorio");

        // Grupo debe existir
        grupoSintomaDao.findById(s.getIdGrupoSintomas())
                .orElseThrow(() -> new IllegalArgumentException("El grupo no existe."));

        // Evitar duplicados de nombre dentro del grupo (normalizando)
        String nuevoNorm = normNombre(s.getNombreSintoma());
        boolean dup = sintomaDao.findByGrupoSintomaId(s.getIdGrupoSintomas()).stream()
                .filter(x -> !esUpdate || !x.getIdSintoma().equals(s.getIdSintoma()))
                .anyMatch(x -> normNombre(x.getNombreSintoma()).equals(nuevoNorm));
        if (dup) throw new IllegalArgumentException("Ya existe un síntoma con ese nombre en el grupo.");
    }

    private static String normNombre(String s) {
        if (s == null) return "";
        String sinAcentos = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    // ===== Permisos / helpers =====

    private void requireActor(Usuario actor) {
        if (actor == null || isBlank(actor.getIdUsuario()))
            throw new SecurityException("Sesión no válida.");
    }

    private void assertPermisoCatalogo(Usuario actor, String accion) {
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) return;
        throw new SecurityException("No tiene permiso para " + accion + " del catálogo de síntomas.");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String resp(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }
}