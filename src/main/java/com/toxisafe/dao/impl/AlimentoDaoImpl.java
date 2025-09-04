package com.toxisafe.dao.impl;

import com.toxisafe.dao.AlimentoDao;
import com.toxisafe.model.Alimento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlimentoDaoImpl implements AlimentoDao {

    private final Connection connection;

    public AlimentoDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Alimento alimento) throws SQLException {
        final String sql = "INSERT INTO ALIMENTO (id_alimento, id_ingesta, nombre, id_catalogo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, alimento.getIdAlimento());
            ps.setString(2, alimento.getIdIngesta());
            ps.setString(3, alimento.getNombre());
            if (alimento.getIdCatalogo() == null || alimento.getIdCatalogo().isBlank())
                ps.setNull(4, java.sql.Types.VARCHAR);
            else
                ps.setString(4, alimento.getIdCatalogo());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Alimento> findById(String id) throws SQLException {
        final String sql = "SELECT id_alimento, id_ingesta, nombre, id_catalogo FROM ALIMENTO WHERE id_alimento = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Alimento(
                            rs.getString("id_alimento"),
                            rs.getString("id_ingesta"),
                            rs.getString("nombre"),
                            rs.getString("id_catalogo")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(Alimento alimento) throws SQLException {
        final String sql = "UPDATE ALIMENTO SET id_ingesta = ?, nombre = ?, id_catalogo = ? WHERE id_alimento = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, alimento.getIdIngesta());
            ps.setString(2, alimento.getNombre());
            if (alimento.getIdCatalogo() == null || alimento.getIdCatalogo().isBlank())
                ps.setNull(3, java.sql.Types.VARCHAR);
            else
                ps.setString(3, alimento.getIdCatalogo());
            ps.setString(4, alimento.getIdAlimento());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM ALIMENTO WHERE id_alimento = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Alimento> findAll() throws SQLException {
        final String sql = "SELECT id_alimento, id_ingesta, nombre, id_catalogo FROM ALIMENTO";
        List<Alimento> out = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Alimento(
                        rs.getString("id_alimento"),
                        rs.getString("id_ingesta"),
                        rs.getString("nombre"),
                        rs.getString("id_catalogo")
                ));
            }
        }
        return out;
    }

    @Override
    public List<Alimento> findByIngestaId(String ingestaId) throws SQLException {
        final String sql = "SELECT id_alimento, id_ingesta, nombre, id_catalogo FROM ALIMENTO WHERE id_ingesta = ? ORDER BY nombre";
        List<Alimento> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ingestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Alimento(
                            rs.getString("id_alimento"),
                            rs.getString("id_ingesta"),
                            rs.getString("nombre"),
                            rs.getString("id_catalogo")
                    ));
                }
            }
        }
        return out;
    }

    @Override
    public boolean existsNombreEnIngesta(String ingestaId, String nombre) throws SQLException {
        // case-insensitive: lower(nombre)=lower(?) o confiar en UNIQUE INDEX con COLLATE NOCASE
        String sql = "SELECT 1 FROM ALIMENTO WHERE id_ingesta = ? AND LOWER(nombre) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ingestaId);
            ps.setString(2, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    @Override
    public void deleteByIngestaId(String ingestaId) throws SQLException {
        String sql = "DELETE FROM ALIMENTO WHERE id_ingesta = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ingestaId);
            ps.executeUpdate();
        }
    }
}
