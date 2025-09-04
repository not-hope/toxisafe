package com.toxisafe.dao.impl;

import com.toxisafe.dao.PersonaExpuestaDao;
import com.toxisafe.model.PersonaExpuesta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonaExpuestaDaoImpl implements PersonaExpuestaDao {

    private final Connection connection;

    public PersonaExpuestaDaoImpl(Connection connection) {
        this.connection = connection;
    }

    // ===== Helpers =====
    private static Integer getInteger(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? null : ((Number) o).intValue();
    }

    private static PersonaExpuesta mapRow(ResultSet rs) throws SQLException {
        return new PersonaExpuesta(
                rs.getString("id_expuesto"),
                rs.getString("id_brote"),
                rs.getString("nombre_expuesto"),
                rs.getString("apellido_expuesto"),
                rs.getString("tfno1_expuesto"),
                rs.getString("tfno2_expuesto"),
                rs.getString("nhusa_expuesto"),
                rs.getString("tipo_documento_expuesto"),
                rs.getString("num_documento_expuesto"),
                rs.getString("sexo_expuesto"),
                getInteger(rs, "edad_expuesto"),
                rs.getString("fecha_nacimiento_expuesto"),
                rs.getString("direccion_expuesto"),
                rs.getString("centro_salud_expuesto"),
                rs.getString("profesion_expuesto"),
                getInteger(rs, "manipulador_expuesto"),
                rs.getString("grupo_expuesto"),
                getInteger(rs, "enfermo_expuesto"),
                getInteger(rs, "atencion_medica_expuesto"),
                getInteger(rs, "atencion_hospitalaria_expuesto"),
                rs.getString("fecha_atencion_medica_expuesto"),
                rs.getString("lugar_atencion_medica_expuesto"),
                rs.getString("evolucion_expuesto"),
                rs.getString("tratamiento_expuesto"),
                getInteger(rs, "solicitud_coprocultivo_expuesto"),
                getInteger(rs, "estado_coprocultivo_expuesto"),
                rs.getString("fecha_coprocultivo_expuesto"),
                rs.getString("laboratorio_coprocultivo_expuesto"),
                rs.getString("resultado_coprocultivo_expuesto"),
                rs.getString("patogeno_coprocultivo_expuesto"),
                rs.getString("observaciones_coprocultivo_expuesto"),
                getInteger(rs, "solicitud_frotis_expuesto"),
                getInteger(rs, "estado_frotis_expuesto"),
                rs.getString("fecha_frotis_expuesto"),
                rs.getString("laboratorio_frotis_expuesto"),
                rs.getString("resultado_frotis_expuesto"),
                rs.getString("patogeno_frotis_expuesto"),
                rs.getString("observaciones_frotis_expuesto")
        );
    }

    // ===== CRUD =====

    @Override
    public void insert(PersonaExpuesta p) throws SQLException {
        final String sql =
                "INSERT INTO PERSONA_EXPUESTA (" +
                        "id_expuesto, id_brote, nombre_expuesto, apellido_expuesto, tfno1_expuesto, tfno2_expuesto, " +
                        "nhusa_expuesto, tipo_documento_expuesto, num_documento_expuesto, sexo_expuesto, edad_expuesto, " +
                        "fecha_nacimiento_expuesto, direccion_expuesto, centro_salud_expuesto, profesion_expuesto, " +
                        "manipulador_expuesto, grupo_expuesto, enfermo_expuesto, atencion_medica_expuesto, " +
                        "atencion_hospitalaria_expuesto, fecha_atencion_medica_expuesto, lugar_atencion_medica_expuesto, " +
                        "evolucion_expuesto, tratamiento_expuesto, solicitud_coprocultivo_expuesto, " +
                        "estado_coprocultivo_expuesto, fecha_coprocultivo_expuesto, laboratorio_coprocultivo_expuesto, " +
                        "resultado_coprocultivo_expuesto, patogeno_coprocultivo_expuesto, observaciones_coprocultivo_expuesto, " +
                        "solicitud_frotis_expuesto, estado_frotis_expuesto, fecha_frotis_expuesto, laboratorio_frotis_expuesto, " +
                        "resultado_frotis_expuesto, patogeno_frotis_expuesto, observaciones_frotis_expuesto" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, p.getIdExpuesto());
            ps.setString(i++, p.getIdBrote());
            ps.setString(i++, p.getNombreExpuesto());
            ps.setString(i++, p.getApellidoExpuesto());
            ps.setObject(i++, p.getTfno1Expuesto());
            ps.setString(i++, p.getTfno2Expuesto());
            ps.setString(i++, p.getNhusaExpuesto());
            ps.setString(i++, p.getTipoDocumentoExpuesto());
            ps.setString(i++, p.getNumDocumentoExpuesto());
            ps.setString(i++, p.getSexoExpuesto());
            ps.setObject(i++, p.getEdadExpuesto());
            ps.setString(i++, p.getFechaNacimientoExpuesto());
            ps.setString(i++, p.getDireccionExpuesto());
            ps.setString(i++, p.getCentroSaludExpuesto());
            ps.setString(i++, p.getProfesionExpuesto());
            ps.setObject(i++, p.isManipuladorExpuesto());
            ps.setString(i++, p.getGrupoExpuesto());
            ps.setObject(i++, p.isEnfermoExpuesto());
            ps.setObject(i++, p.isAtencionMedicaExpuesto());
            ps.setObject(i++, p.isAtencionHospitalariaExpuesto());
            ps.setString(i++, p.getFechaAtencionMedicaExpuesto());
            ps.setString(i++, p.getLugarAtencionMedicaExpuesto());
            ps.setString(i++, p.getEvolucionExpuesto());
            ps.setString(i++, p.getTratamientoExpuesto());
            ps.setObject(i++, p.isSolicitudCoprocultivoExpuesto());
            ps.setObject(i++, p.isEstadoCoprocultivoExpuesto());
            ps.setString(i++, p.getFechaCoprocultivoExpuesto());
            ps.setString(i++, p.getLaboratorioCoprocultivoExpuesto());
            ps.setString(i++, p.getResultadoCoprocultivoExpuesto());
            ps.setString(i++, p.getPatogenoCoprocultivoExpuesto());
            ps.setString(i++, p.getObservacionesCoprocultivoExpuesto());
            ps.setObject(i++, p.isSolicitudFrotisExpuesto());
            ps.setObject(i++, p.isEstadoFrotisExpuesto());
            ps.setString(i++, p.getFechaFrotisExpuesto());
            ps.setString(i++, p.getLaboratorioFrotisExpuesto());
            ps.setString(i++, p.getResultadoFrotisExpuesto());
            ps.setString(i++, p.getPatogenoFrotisExpuesto());
            ps.setString(i++, p.getObservacionesFrotisExpuesto());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<PersonaExpuesta> findById(String id) throws SQLException {
        final String sql = "SELECT * FROM PERSONA_EXPUESTA WHERE id_expuesto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(PersonaExpuesta p) throws SQLException {
        final String sql =
                "UPDATE PERSONA_EXPUESTA SET " +
                        "id_brote=?, nombre_expuesto=?, apellido_expuesto=?, tfno1_expuesto=?, tfno2_expuesto=?, " +
                        "nhusa_expuesto=?, tipo_documento_expuesto=?, num_documento_expuesto=?, sexo_expuesto=?, edad_expuesto=?, " +
                        "fecha_nacimiento_expuesto=?, direccion_expuesto=?, centro_salud_expuesto=?, profesion_expuesto=?, " +
                        "manipulador_expuesto=?, grupo_expuesto=?, enfermo_expuesto=?, atencion_medica_expuesto=?, " +
                        "atencion_hospitalaria_expuesto=?, fecha_atencion_medica_expuesto=?, lugar_atencion_medica_expuesto=?, " +
                        "evolucion_expuesto=?, tratamiento_expuesto=?, solicitud_coprocultivo_expuesto=?, estado_coprocultivo_expuesto=?, " +
                        "fecha_coprocultivo_expuesto=?, laboratorio_coprocultivo_expuesto=?, resultado_coprocultivo_expuesto=?, " +
                        "patogeno_coprocultivo_expuesto=?, observaciones_coprocultivo_expuesto=?, solicitud_frotis_expuesto=?, " +
                        "estado_frotis_expuesto=?, fecha_frotis_expuesto=?, laboratorio_frotis_expuesto=?, resultado_frotis_expuesto=?, " +
                        "patogeno_frotis_expuesto=?, observaciones_frotis_expuesto=? " +
                        "WHERE id_expuesto=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, p.getIdBrote());
            ps.setString(i++, p.getNombreExpuesto());
            ps.setString(i++, p.getApellidoExpuesto());
            ps.setObject(i++, p.getTfno1Expuesto());
            ps.setString(i++, p.getTfno2Expuesto());
            ps.setString(i++, p.getNhusaExpuesto());
            ps.setString(i++, p.getTipoDocumentoExpuesto());
            ps.setString(i++, p.getNumDocumentoExpuesto());
            ps.setString(i++, p.getSexoExpuesto());
            ps.setObject(i++, p.getEdadExpuesto());
            ps.setString(i++, p.getFechaNacimientoExpuesto());
            ps.setString(i++, p.getDireccionExpuesto());
            ps.setString(i++, p.getCentroSaludExpuesto());
            ps.setString(i++, p.getProfesionExpuesto());
            ps.setObject(i++, p.isManipuladorExpuesto());
            ps.setString(i++, p.getGrupoExpuesto());
            ps.setObject(i++, p.isEnfermoExpuesto());
            ps.setObject(i++, p.isAtencionMedicaExpuesto());
            ps.setObject(i++, p.isAtencionHospitalariaExpuesto());
            ps.setString(i++, p.getFechaAtencionMedicaExpuesto());
            ps.setString(i++, p.getLugarAtencionMedicaExpuesto());
            ps.setString(i++, p.getEvolucionExpuesto());
            ps.setString(i++, p.getTratamientoExpuesto());
            ps.setObject(i++, p.isSolicitudCoprocultivoExpuesto());
            ps.setObject(i++, p.isEstadoCoprocultivoExpuesto());
            ps.setString(i++, p.getFechaCoprocultivoExpuesto());
            ps.setString(i++, p.getLaboratorioCoprocultivoExpuesto());
            ps.setString(i++, p.getResultadoCoprocultivoExpuesto());
            ps.setString(i++, p.getPatogenoCoprocultivoExpuesto());
            ps.setString(i++, p.getObservacionesCoprocultivoExpuesto());
            ps.setObject(i++, p.isSolicitudFrotisExpuesto());
            ps.setObject(i++, p.isEstadoFrotisExpuesto());
            ps.setString(i++, p.getFechaFrotisExpuesto());
            ps.setString(i++, p.getLaboratorioFrotisExpuesto());
            ps.setString(i++, p.getResultadoFrotisExpuesto());
            ps.setString(i++, p.getPatogenoFrotisExpuesto());
            ps.setString(i++, p.getObservacionesFrotisExpuesto());
            ps.setString(i++, p.getIdExpuesto());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM PERSONA_EXPUESTA WHERE id_expuesto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<PersonaExpuesta> findAll() throws SQLException {
        final String sql = "SELECT * FROM PERSONA_EXPUESTA";
        List<PersonaExpuesta> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public List<PersonaExpuesta> findByBroteId(String broteId) throws SQLException {
        final String sql = "SELECT * FROM PERSONA_EXPUESTA WHERE id_brote = ?";
        List<PersonaExpuesta> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, broteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public boolean existsDocumentoEnBrote(String broteId, String tipoDocumento, String numDocumento) throws SQLException {
        if (broteId == null || tipoDocumento == null || numDocumento == null) return false;
        final String sql =
                "SELECT 1 " +
                        "FROM PERSONA_EXPUESTA " +
                        "WHERE id_brote = ? " +
                        "  AND LOWER(tipo_documento_expuesto) = LOWER(?) " +
                        "  AND UPPER(REPLACE(REPLACE(num_documento_expuesto,' ',''),'-','')) = ? " +
                        "LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, broteId);
            ps.setString(2, tipoDocumento);
            ps.setString(3, normalizarDoc(numDocumento));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean existsDocumentoEnBroteExcepto(String idExpuesto, String broteId, String tipoDocumento, String numDocumento) throws SQLException {
        if (idExpuesto == null || broteId == null || tipoDocumento == null || numDocumento == null) return false;
        final String sql =
                "SELECT 1 " +
                        "FROM PERSONA_EXPUESTA " +
                        "WHERE id_brote = ? " +
                        "  AND LOWER(tipo_documento_expuesto) = LOWER(?) " +
                        "  AND UPPER(REPLACE(REPLACE(num_documento_expuesto,' ',''),'-','')) = ? " +
                        "  AND id_expuesto <> ? " +
                        "LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, broteId);
            ps.setString(2, tipoDocumento);
            ps.setString(3, normalizarDoc(numDocumento));
            ps.setString(4, idExpuesto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ---------- helper privado en esta clase ---------- */
    private static String normalizarDoc(String num) {
        if (num == null) return null;
        String t = num.trim().replace(" ", "").replace("-", "");
        return t.toUpperCase(java.util.Locale.ROOT);
    }

}
