package com.toxisafe.dao.impl;

import com.toxisafe.dao.SintomaDao;
import com.toxisafe.model.Sintoma;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SintomaDaoImpl implements SintomaDao {

    private final Connection connection;

    public SintomaDaoImpl(Connection connection) { this.connection = connection; }

    @Override
    public void insert(Sintoma s) throws SQLException {
        final String sql = "INSERT INTO SINTOMA (id_sintoma, id_grupo_sintomas, nombre_sintoma) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getIdSintoma());
            ps.setString(2, s.getIdGrupoSintomas());
            ps.setString(3, s.getNombreSintoma());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Sintoma> findById(String id) throws SQLException {
        final String sql = "SELECT id_sintoma, id_grupo_sintomas, nombre_sintoma FROM SINTOMA WHERE id_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Sintoma(
                            rs.getString("id_sintoma"),
                            rs.getString("id_grupo_sintomas"),
                            rs.getString("nombre_sintoma")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(Sintoma s) throws SQLException {
        final String sql = "UPDATE SINTOMA SET id_grupo_sintomas = ?, nombre_sintoma = ? WHERE id_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getIdGrupoSintomas());
            ps.setString(2, s.getNombreSintoma());
            ps.setString(3, s.getIdSintoma());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM SINTOMA WHERE id_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Sintoma> findAll() throws SQLException {
        final String sql = "SELECT id_sintoma, id_grupo_sintomas, nombre_sintoma FROM SINTOMA ORDER BY nombre_sintoma";
        List<Sintoma> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Sintoma(
                        rs.getString("id_sintoma"),
                        rs.getString("id_grupo_sintomas"),
                        rs.getString("nombre_sintoma")
                ));
            }
        }
        return out;
    }

    @Override
    public List<Sintoma> findByGrupoSintomaId(String grupoId) throws SQLException {
        final String sql = "SELECT id_sintoma, id_grupo_sintomas, nombre_sintoma FROM SINTOMA WHERE id_grupo_sintomas = ? ORDER BY nombre_sintoma";
        List<Sintoma> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, grupoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Sintoma(
                            rs.getString("id_sintoma"),
                            rs.getString("id_grupo_sintomas"),
                            rs.getString("nombre_sintoma")
                    ));
                }
            }
        }
        return out;
    }

    @Override
    public Optional<Sintoma> findByNombreInGrupo(String grupoId, String nombre) throws SQLException {
        final String sql = "SELECT id_sintoma, id_grupo_sintomas, nombre_sintoma FROM SINTOMA WHERE id_grupo_sintomas = ? AND nombre_sintoma = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, grupoId);
            ps.setString(2, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Sintoma(
                            rs.getString("id_sintoma"),
                            rs.getString("id_grupo_sintomas"),
                            rs.getString("nombre_sintoma")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
