package com.toxisafe.dao.impl;

import com.toxisafe.dao.ExposicionSintomaDao;
import com.toxisafe.model.ExposicionSintoma;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExposicionSintomaDaoImpl implements ExposicionSintomaDao {

    private final Connection connection;

    public ExposicionSintomaDaoImpl(Connection connection) { this.connection = connection; }

    @Override
    public void insert(ExposicionSintoma e) throws SQLException {
        final String sql = "INSERT INTO EXPOSICION_SINTOMA (id_exposicion_sintoma, id_sintomas_generales, id_sintoma) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getIdExposicionSintoma());
            ps.setString(2, e.getIdSintomasGenerales());
            ps.setString(3, e.getIdSintoma());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<ExposicionSintoma> findById(String id) throws SQLException {
        final String sql = "SELECT id_exposicion_sintoma, id_sintomas_generales, id_sintoma FROM EXPOSICION_SINTOMA WHERE id_exposicion_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ExposicionSintoma(
                            rs.getString("id_exposicion_sintoma"),
                            rs.getString("id_sintomas_generales"),
                            rs.getString("id_sintoma")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(ExposicionSintoma e) throws SQLException {
        final String sql = "UPDATE EXPOSICION_SINTOMA SET id_sintomas_generales = ?, id_sintoma = ? WHERE id_exposicion_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getIdSintomasGenerales());
            ps.setString(2, e.getIdSintoma());
            ps.setString(3, e.getIdExposicionSintoma());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM EXPOSICION_SINTOMA WHERE id_exposicion_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<ExposicionSintoma> findAll() throws SQLException {
        final String sql = "SELECT id_exposicion_sintoma, id_sintomas_generales, id_sintoma FROM EXPOSICION_SINTOMA";
        List<ExposicionSintoma> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new ExposicionSintoma(
                        rs.getString("id_exposicion_sintoma"),
                        rs.getString("id_sintomas_generales"),
                        rs.getString("id_sintoma")
                ));
            }
        }
        return out;
    }

    @Override
    public List<ExposicionSintoma> findBySintomasGeneralesId(String sintomasGeneralesId) throws SQLException {
        final String sql = "SELECT id_exposicion_sintoma, id_sintomas_generales, id_sintoma FROM EXPOSICION_SINTOMA WHERE id_sintomas_generales = ?";
        List<ExposicionSintoma> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sintomasGeneralesId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ExposicionSintoma(
                            rs.getString("id_exposicion_sintoma"),
                            rs.getString("id_sintomas_generales"),
                            rs.getString("id_sintoma")
                    ));
                }
            }
        }
        return out;
    }

    @Override
    public Optional<ExposicionSintoma> findByPar(String idSintomasGenerales, String idSintoma) throws SQLException {
        final String sql = "SELECT id_exposicion_sintoma, id_sintomas_generales, id_sintoma FROM EXPOSICION_SINTOMA WHERE id_sintomas_generales = ? AND id_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idSintomasGenerales);
            ps.setString(2, idSintoma);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ExposicionSintoma(
                            rs.getString("id_exposicion_sintoma"),
                            rs.getString("id_sintomas_generales"),
                            rs.getString("id_sintoma")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
