package com.toxisafe.service;

import com.toxisafe.dao.BroteDao;
import com.toxisafe.dao.BroteEncuestadorDao;
import com.toxisafe.dao.UsuarioDao;
import com.toxisafe.model.Brote;
import com.toxisafe.model.BroteEncuestador;
import com.toxisafe.model.Usuario;
import com.toxisafe.sync.SyncEmitter;
import com.toxisafe.sync.util.SharedFolderIngestor;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de la relación Brote-Encuestador.
 * Valida rol, existencia y evita duplicados.
 */
public class BroteEncuestadorService {

    private final BroteEncuestadorDao broteEncuestadorDao;
    private final BroteDao broteDao;
    private final UsuarioDao usuarioDao;
    private SyncEmitter sync;

    public BroteEncuestadorService(BroteEncuestadorDao broteEncuestadorDao,
                                   BroteDao broteDao,
                                   UsuarioDao usuarioDao,
                                   SyncEmitter sync) {
        this.broteEncuestadorDao = broteEncuestadorDao;
        this.broteDao = broteDao;
        this.usuarioDao = usuarioDao;
        this.sync = sync;
    }

    public BroteEncuestadorService(BroteEncuestadorDao broteEncuestadorDao,
                                   BroteDao broteDao,
                                   UsuarioDao usuarioDao) {
        this.broteEncuestadorDao = broteEncuestadorDao;
        this.broteDao = broteDao;
        this.usuarioDao = usuarioDao;
        this.sync = null;
    }

    public void setSyncEmitter(SyncEmitter syncEmitter) {
        this.sync = syncEmitter;
    }

    /**
     * Asigna un encuestador a un brote.
     *  - Valida que el brote exista
     *  - Valida que el usuario exista y tenga rol ENCUESTADOR
     *  - Evita duplicados (findByBroteAndUsuario)
     */
    public BroteEncuestador asignarEncuestador(String idBrote, String idUsuario) throws SQLException {
        if (isBlank(idBrote) || isBlank(idUsuario))
            throw new IllegalArgumentException("Brote y usuario son obligatorios.");

        Brote brote = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("El brote no existe."));

        assertBroteActivo(idBrote);

        Usuario usuario = usuarioDao.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));

        String rol = (usuario.getRolUsuario() == null ? "" : usuario.getRolUsuario().trim().toUpperCase(Locale.ROOT));
        if (!"ENCUESTADOR".equals(rol))
            throw new IllegalArgumentException("Solo usuarios con rol ENCUESTADOR pueden ser asignados.");

        // Evitar duplicados
        if (broteEncuestadorDao.findByBroteAndUsuario(idBrote, idUsuario).isPresent()) {
            throw new IllegalArgumentException("Ese encuestador ya está asignado a este brote.");
        }

        String id = UUID.randomUUID().toString();
        BroteEncuestador be = new BroteEncuestador(id, brote.getIdBrote(), usuario.getIdUsuario());

        broteEncuestadorDao.insert(be);

        emitInsert("BROTE_ENCUESTADOR", be.getIdBroteEncuestador(), toMap(be));


        return be;
    }

    /**
     * Elimina una asignación por par (brote-usuario).
     */
    public void eliminarAsignacion(String idBrote, String idUsuario) throws SQLException {
        var brote = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(idBrote);

        broteEncuestadorDao.findByBroteAndUsuario(idBrote, idUsuario)
                .ifPresent(be -> {
                    try {
                        broteEncuestadorDao.delete(be.getIdBroteEncuestador());
                        emitDelete("BROTE_ENCUESTADOR", be.getIdBroteEncuestador(), toMap(be));

                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Lista asignaciones (objetos relación).
     */
    public List<BroteEncuestador> asignacionesDe(String idBrote) throws SQLException {
        var brote = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        return broteEncuestadorDao.findByBroteId(idBrote);
    }

    /**
     * Lista usuarios encuestadores asignados a un brote.
     */
    public List<Usuario> usuariosAsignados(String idBrote) throws SQLException {
        List<BroteEncuestador> rels = broteEncuestadorDao.findByBroteId(idBrote);
        List<Usuario> out = new ArrayList<>();
        for (BroteEncuestador be : rels) {
            usuarioDao.findById(be.getIdUsuario()).ifPresent(out::add);
        }
        // Deduplicar por si acaso
        return out.stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, u -> u, (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    public List<BroteEncuestador> findByUsuarioId(String usuarioId) throws SQLException {
        return broteEncuestadorDao.findByUsuarioId(usuarioId);
    }

    private void assertBroteActivo(String idBrote) throws SQLException {
        var b = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        if ("CERRADO".equalsIgnoreCase(b.getEstadoBrote())) {
            throw new IllegalStateException("No se pueden gestionar encuestadores de un brote cerrado.");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private Map<String,Object> toMap(BroteEncuestador be) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_brote_encuestador", be.getIdBroteEncuestador());
        m.put("id_brote", be.getIdBrote());
        m.put("id_usuario", be.getIdUsuario());
        return m;
    }

    private void emitInsert(String tabla, String id, Map<String,Object> data) {
        if (sync != null) sync.emitInsert(tabla, id, data);
    }
    private void emitDelete(String tabla, String id, Map<String,Object> oldV) {
        if (sync != null) sync.emitDelete(tabla, id, oldV);
    }


}
