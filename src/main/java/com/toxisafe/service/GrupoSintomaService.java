package com.toxisafe.service;

import com.toxisafe.dao.GrupoSintomaDao;
import com.toxisafe.dao.SintomaDao;
import com.toxisafe.model.GrupoSintoma;
import com.toxisafe.model.Usuario;

import java.sql.SQLException;
import java.util.*;

public class GrupoSintomaService {

    private final GrupoSintomaDao grupoSintomaDao;
    private final SintomaDao sintomaDao;

    public GrupoSintomaService(GrupoSintomaDao grupoSintomaDao, SintomaDao sintomaDao) {
        this.grupoSintomaDao = grupoSintomaDao;
        this.sintomaDao = sintomaDao;
    }

    // ===== API =====

    public GrupoSintoma create(GrupoSintoma g, Usuario actor) throws SQLException {
        requireActor(actor);
        assertPermisoCatalogo(actor, "crear grupo");
        Objects.requireNonNull(g, "GrupoSintoma requerido");
        if (isBlank(g.getDescripcionGrupo())) throw new IllegalArgumentException("descripcion_grupo obligatoria");

        // id si falta
        if (isBlank(g.getIdGrupoSintomas())) {
            g.setIdGrupoSintomas(UUID.randomUUID().toString());
        }

        grupoSintomaDao.insert(g);
        return g;
    }

    public void update(GrupoSintoma g, Usuario actor) throws SQLException {
        requireActor(actor);
        assertPermisoCatalogo(actor, "actualizar grupo");
        Objects.requireNonNull(g, "GrupoSintoma requerido");
        if (isBlank(g.getIdGrupoSintomas())) throw new IllegalArgumentException("id_grupo_sintomas requerido");
        if (isBlank(g.getDescripcionGrupo())) throw new IllegalArgumentException("descripcion_grupo obligatoria");

        grupoSintomaDao.update(g);
    }

    public void delete(String idGrupo, Usuario actor) throws SQLException {
        requireActor(actor);
        assertPermisoCatalogo(actor, "eliminar grupo");
        if (isBlank(idGrupo)) throw new IllegalArgumentException("id_grupo_sintomas requerido");

        // Evitar borrar si hay síntomas en el grupo
        if (!sintomaDao.findByGrupoSintomaId(idGrupo).isEmpty()) {
            throw new IllegalStateException("No se puede eliminar: existen síntomas en este grupo.");
        }
        grupoSintomaDao.delete(idGrupo);
    }

    public Optional<GrupoSintoma> findById(String id) throws SQLException {
        return grupoSintomaDao.findById(id);
    }

    public List<GrupoSintoma> findAll() throws SQLException {
        return grupoSintomaDao.findAll();
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
