package com.toxisafe.dao.impl;

import com.toxisafe.dao.IngestaPersonaExpuestaDao;
import com.toxisafe.model.IngestaPersonaExpuesta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngestaPersonaExpuestaDaoImpl implements IngestaPersonaExpuestaDao {

    private final Connection connection;

    public IngestaPersonaExpuestaDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(IngestaPersonaExpuesta v) throws SQLException {
        final String sql = "INSERT INTO INGESTA_PERSONA_EXPUESTA " +
                "(id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, v.getIdIngestaPersonaExpuesta());
            pstmt.setString(2, v.getIdIngesta());
            pstmt.setString(3, v.getIdExpuesto());
            pstmt.setObject(4, v.getEsSospechosaParaExpuesto());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<IngestaPersonaExpuesta> findById(String id) throws SQLException {
        final String sql = "SELECT id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto " +
                "FROM INGESTA_PERSONA_EXPUESTA WHERE id_ingesta_persona_expuesta = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new IngestaPersonaExpuesta(
                            rs.getString("id_ingesta_persona_expuesta"),
                            rs.getString("id_ingesta"),
                            rs.getString("id_expuesto"),
                            (Integer) rs.getObject("es_sospechosa_para_expuesto")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(IngestaPersonaExpuesta v) throws SQLException {
        final String sql = "UPDATE INGESTA_PERSONA_EXPUESTA SET id_ingesta = ?, id_expuesto = ?, " +
                "es_sospechosa_para_expuesto = ? WHERE id_ingesta_persona_expuesta = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, v.getIdIngesta());
            pstmt.setString(2, v.getIdExpuesto());
            pstmt.setObject(3, v.getEsSospechosaParaExpuesto());
            pstmt.setString(4, v.getIdIngestaPersonaExpuesta());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM INGESTA_PERSONA_EXPUESTA WHERE id_ingesta_persona_expuesta = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<IngestaPersonaExpuesta> findAll() throws SQLException {
        final String sql = "SELECT id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto " +
                "FROM INGESTA_PERSONA_EXPUESTA";
        List<IngestaPersonaExpuesta> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new IngestaPersonaExpuesta(
                        rs.getString("id_ingesta_persona_expuesta"),
                        rs.getString("id_ingesta"),
                        rs.getString("id_expuesto"),
                        (Integer) rs.getObject("es_sospechosa_para_expuesto")
                ));
            }
        }
        return list;
    }

    @Override
    public List<IngestaPersonaExpuesta> findByIngestaId(String ingestaId) throws SQLException {
        final String sql = "SELECT id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto " +
                "FROM INGESTA_PERSONA_EXPUESTA WHERE id_ingesta = ?";
        List<IngestaPersonaExpuesta> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ingestaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new IngestaPersonaExpuesta(
                            rs.getString("id_ingesta_persona_expuesta"),
                            rs.getString("id_ingesta"),
                            rs.getString("id_expuesto"),
                            (Integer) rs.getObject("es_sospechosa_para_expuesto")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public List<IngestaPersonaExpuesta> findByExpuestoId(String expuestoId) throws SQLException {
        final String sql = "SELECT id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto " +
                "FROM INGESTA_PERSONA_EXPUESTA WHERE id_expuesto = ?";
        List<IngestaPersonaExpuesta> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, expuestoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new IngestaPersonaExpuesta(
                            rs.getString("id_ingesta_persona_expuesta"),
                            rs.getString("id_ingesta"),
                            rs.getString("id_expuesto"),
                            (Integer) rs.getObject("es_sospechosa_para_expuesto")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public Optional<IngestaPersonaExpuesta> findByIngestaAndExpuesto(String ingestaId, String expuestoId) throws SQLException {
        final String sql = "SELECT id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto " +
                "FROM INGESTA_PERSONA_EXPUESTA WHERE id_ingesta = ? AND id_expuesto = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ingestaId);
            pstmt.setString(2, expuestoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new IngestaPersonaExpuesta(
                            rs.getString("id_ingesta_persona_expuesta"),
                            rs.getString("id_ingesta"),
                            rs.getString("id_expuesto"),
                            (Integer) rs.getObject("es_sospechosa_para_expuesto")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<IngestaPersonaExpuesta> findSospechosasByExpuestoId(String expuestoId) throws SQLException {
        final String sql = "SELECT id_ingesta_persona_expuesta, id_ingesta, id_expuesto, es_sospechosa_para_expuesto " +
                "FROM INGESTA_PERSONA_EXPUESTA WHERE id_expuesto = ? AND es_sospechosa_para_expuesto = 1";
        List<IngestaPersonaExpuesta> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, expuestoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new IngestaPersonaExpuesta(
                            rs.getString("id_ingesta_persona_expuesta"),
                            rs.getString("id_ingesta"),
                            rs.getString("id_expuesto"),
                            (Integer) rs.getObject("es_sospechosa_para_expuesto")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public void deleteByIngestaAndExpuesto(String ingestaId, String expuestoId) throws SQLException {
        String sql = "DELETE FROM INGESTA_PERSONA_EXPUESTA WHERE id_ingesta = ? AND id_expuesto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ingestaId);
            ps.setString(2, expuestoId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteByIngestaId(String ingestaId) throws SQLException {
        String sql = "DELETE FROM INGESTA_PERSONA_EXPUESTA WHERE id_ingesta = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ingestaId);
            ps.executeUpdate();
        }
    }
}
