package com.toxisafe.dao.impl;

import com.toxisafe.dao.InformeDao;
import com.toxisafe.model.Informe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InformeDaoImpl implements InformeDao {

    private final Connection connection;

    public InformeDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Informe informe) throws SQLException {
        final String sql = "INSERT INTO INFORME (id_informe, id_brote, contenido_informe) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, informe.getIdInforme());
            ps.setString(2, informe.getIdBrote());
            ps.setString(3, informe.getContenidoInforme());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Informe> findById(String id) throws SQLException {
        final String sql = "SELECT id_informe, id_brote, contenido_informe FROM INFORME WHERE id_informe = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(Informe informe) throws SQLException {
        final String sql = "UPDATE INFORME SET id_brote = ?, contenido_informe = ? WHERE id_informe = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, informe.getIdBrote());
            ps.setString(2, informe.getContenidoInforme());
            ps.setString(3, informe.getIdInforme());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM INFORME WHERE id_informe = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Informe> findAll() throws SQLException {
        final String sql = "SELECT id_informe, id_brote, contenido_informe FROM INFORME";
        List<Informe> out = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public List<Informe> findByBroteId(String broteId) throws SQLException {
        final String sql = "SELECT id_informe, id_brote, contenido_informe FROM INFORME WHERE id_brote = ?";
        List<Informe> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, broteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /* ------------- mapper ------------- */

    private Informe map(ResultSet rs) throws SQLException {
        return new Informe(
                rs.getString("id_informe"),
                rs.getString("id_brote"),
                rs.getString("contenido_informe")
        );
    }
}
