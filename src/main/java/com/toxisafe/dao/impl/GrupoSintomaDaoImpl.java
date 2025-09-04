package com.toxisafe.dao.impl;

import com.toxisafe.dao.GrupoSintomaDao;
import com.toxisafe.model.GrupoSintoma;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GrupoSintomaDaoImpl implements GrupoSintomaDao {

    private final Connection connection;

    public GrupoSintomaDaoImpl(Connection connection) { this.connection = connection; }

    @Override
    public void insert(GrupoSintoma g) throws SQLException {
        final String sql = "INSERT INTO GRUPO_SINTOMA (id_grupo_sintomas, descripcion_grupo) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, g.getIdGrupoSintomas());
            ps.setString(2, g.getDescripcionGrupo());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<GrupoSintoma> findById(String id) throws SQLException {
        final String sql = "SELECT id_grupo_sintomas, descripcion_grupo FROM GRUPO_SINTOMA WHERE id_grupo_sintomas = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new GrupoSintoma(
                            rs.getString("id_grupo_sintomas"),
                            rs.getString("descripcion_grupo")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(GrupoSintoma g) throws SQLException {
        final String sql = "UPDATE GRUPO_SINTOMA SET descripcion_grupo = ? WHERE id_grupo_sintomas = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, g.getDescripcionGrupo());
            ps.setString(2, g.getIdGrupoSintomas());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM GRUPO_SINTOMA WHERE id_grupo_sintomas = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<GrupoSintoma> findAll() throws SQLException {
        final String sql = "SELECT id_grupo_sintomas, descripcion_grupo FROM GRUPO_SINTOMA ORDER BY descripcion_grupo";
        List<GrupoSintoma> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new GrupoSintoma(
                        rs.getString("id_grupo_sintomas"),
                        rs.getString("descripcion_grupo")
                ));
            }
        }
        return out;
    }

    @Override
    public Optional<GrupoSintoma> findByDescripcion(String descripcion) throws SQLException {
        final String sql = "SELECT id_grupo_sintomas, descripcion_grupo FROM GRUPO_SINTOMA WHERE descripcion_grupo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, descripcion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new GrupoSintoma(
                            rs.getString("id_grupo_sintomas"),
                            rs.getString("descripcion_grupo")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
