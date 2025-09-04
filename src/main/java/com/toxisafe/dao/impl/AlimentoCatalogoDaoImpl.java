package com.toxisafe.dao.impl;

import com.toxisafe.dao.AlimentoCatalogoDao;
import com.toxisafe.model.AlimentoCatalogo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AlimentoCatalogoDaoImpl implements AlimentoCatalogoDao {
    private final Connection connection;

    public AlimentoCatalogoDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(AlimentoCatalogo c) throws SQLException {
        String sql = "INSERT INTO ALIMENTO_CATALOGO (id_catalogo, nombre_canonico, nombre_norm, categoria) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getIdCatalogo());
            ps.setString(2, c.getNombreCanonico());
            ps.setString(3, c.getNombreNorm());
            ps.setString(4, c.getCategoria());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<AlimentoCatalogo> findById(String id) throws SQLException {
        String sql = "SELECT * FROM ALIMENTO_CATALOGO WHERE id_catalogo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(AlimentoCatalogo c) throws SQLException {
        String sql = "UPDATE ALIMENTO_CATALOGO SET nombre_canonico=?, nombre_norm=?, categoria=? WHERE id_catalogo=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNombreCanonico());
            ps.setString(2, c.getNombreNorm());
            ps.setString(3, c.getCategoria());
            ps.setString(4, c.getIdCatalogo());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ALIMENTO_CATALOGO WHERE id_catalogo=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<AlimentoCatalogo> findAll() throws SQLException {
        List<AlimentoCatalogo> out = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ALIMENTO_CATALOGO")) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public Optional<AlimentoCatalogo> findByNombreCanonico(String nombreCanonico) throws SQLException {
        String sql = "SELECT * FROM ALIMENTO_CATALOGO WHERE nombre_canonico = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombreCanonico);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    @Override
    public Optional<AlimentoCatalogo> findByNombreNorm(String nombreNorm) throws SQLException {
        String sql = "SELECT * FROM ALIMENTO_CATALOGO WHERE nombre_norm = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombreNorm);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    private static AlimentoCatalogo map(ResultSet rs) throws SQLException {
        return new AlimentoCatalogo(
                rs.getString("id_catalogo"),
                rs.getString("nombre_canonico"),
                rs.getString("nombre_norm"),
                rs.getString("categoria")
        );
    }

    @Override
    public List<String> findAllNombres() throws SQLException {
        String sql = "SELECT nombre_canonico FROM ALIMENTO_CATALOGO ORDER BY nombre_canonico";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> out = new ArrayList<>();
            while (rs.next()) out.add(rs.getString(1));
            return out;
        }
    }

    @Override
    public List<String> findByPrefix(String prefix) throws SQLException {
        // Buscamos por el nombre normalizado (coincidencia por prefijo)
        String sql = "SELECT nombre_canonico " +
                "FROM ALIMENTO_CATALOGO " +
                "WHERE nombre_norm LIKE ? " +
                "ORDER BY nombre_canonico";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, prefix.toLowerCase(Locale.ROOT).trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }

    @Override
    public boolean existsNombreExacto(String nombre) throws SQLException {
        String sql = "SELECT 1 FROM ALIMENTO_CATALOGO WHERE nombre_canonico = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
