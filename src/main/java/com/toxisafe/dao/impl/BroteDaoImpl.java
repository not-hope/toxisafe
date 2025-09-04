package com.toxisafe.dao.impl;

import com.toxisafe.dao.BroteDao;
import com.toxisafe.model.Brote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BroteDaoImpl implements BroteDao {

    private final Connection connection;

    public BroteDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Brote brote) throws SQLException {
        String sql = "INSERT INTO BROTE (id_brote, creador_brote, responsable_brote, fech_ini_brote, nombre_brote) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, brote.getIdBrote());
            pstmt.setString(2, brote.getCreadorBrote());
            pstmt.setString(3, brote.getResponsableBrote());
            pstmt.setString(4, brote.getFechIniBrote());
            pstmt.setString(5, brote.getNombreBrote());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<Brote> findById(String id) throws SQLException {
        String sql = "SELECT * FROM BROTE WHERE id_brote = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(Brote brote) throws SQLException {
        String sql = "UPDATE BROTE SET creador_brote = ?, responsable_brote = ?, fech_ini_brote = ?, nombre_brote = ? WHERE id_brote = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, brote.getCreadorBrote());
            pstmt.setString(2, brote.getResponsableBrote());
            pstmt.setString(3, brote.getFechIniBrote());
            pstmt.setString(4, brote.getNombreBrote());
            pstmt.setString(5, brote.getIdBrote());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM BROTE WHERE id_brote = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Brote> findAll() throws SQLException {
        List<Brote> brotes = new ArrayList<>();
        String sql = "SELECT * FROM BROTE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                brotes.add(mapRow(rs));
            }
        }
        return brotes;
    }

    @Override
    public List<Brote> findByCreador(String creadorId) throws SQLException {
        List<Brote> brotes = new ArrayList<>();
        String sql = "SELECT * FROM BROTE WHERE creador_brote = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, creadorId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                brotes.add(mapRow(rs));
            }
        }
        return brotes;
    }

    @Override
    public List<Brote> findByResponsable(String responsableId) throws SQLException {
        List<Brote> brotes = new ArrayList<>();
        String sql = "SELECT * FROM BROTE WHERE responsable_brote = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, responsableId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                brotes.add(mapRow(rs));
            }
        }
        return brotes;
    }

    @Override
    public boolean existsByNombre(String nombre) throws SQLException {
        final String sql = "SELECT 1 FROM BROTE WHERE LOWER(nombre_brote) = LOWER(?) LIMIT 1";
        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public boolean existsByNombreExceptoId(String nombre, String idExcluir) throws SQLException {
        final String sql = "SELECT 1 FROM BROTE WHERE LOWER(nombre_brote) = LOWER(?) AND id_brote <> ? LIMIT 1";
        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, idExcluir);
            try (var rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private Brote mapRow(ResultSet rs) throws SQLException {
        Brote b = new Brote(
                rs.getString("id_brote"),
                rs.getString("creador_brote"),
                rs.getString("responsable_brote"),
                rs.getString("fech_ini_brote"),
                rs.getString("nombre_brote")
        );
        b.setEstadoBrote(rs.getString("estado_brote"));        // "ACTIVO" | "CERRADO"
        b.setFechaCierreBrote(rs.getString("fecha_cierre_brote")); // YYYY-MM-DD o null
        return b;
    }

    @Override
    public List<Brote> findByEstado(String estado) throws SQLException {
        List<Brote> list = new ArrayList<>();
        final String sql = "SELECT * FROM BROTE WHERE estado_brote = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public void actualizarEstado(String idBrote, String estado, String fechaCierreIso) throws SQLException {
        final String sql = "UPDATE BROTE SET estado_brote=?, fecha_cierre_brote=? WHERE id_brote=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, fechaCierreIso); // puede ser null
            ps.setString(3, idBrote);
            ps.executeUpdate();
        }
    }

}
