package com.toxisafe.dao.impl;

import com.toxisafe.dao.BroteEncuestadorDao;
import com.toxisafe.model.BroteEncuestador;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BroteEncuestadorDaoImpl implements BroteEncuestadorDao {

    private final Connection connection;

    public BroteEncuestadorDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(BroteEncuestador broteEncuestador) throws SQLException {
        String sql = "INSERT INTO BROTE_ENCUESTADOR (id_brote_encuestador, id_brote, id_usuario) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, broteEncuestador.getIdBroteEncuestador());
            pstmt.setString(2, broteEncuestador.getIdBrote());
            pstmt.setString(3, broteEncuestador.getIdUsuario());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<BroteEncuestador> findById(String id) throws SQLException {
        String sql = "SELECT * FROM BROTE_ENCUESTADOR WHERE id_brote_encuestador = ?";
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
    public void update(BroteEncuestador broteEncuestador) throws SQLException {
        String sql = "UPDATE BROTE_ENCUESTADOR SET id_brote = ?, id_usuario = ? WHERE id_brote_encuestador = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, broteEncuestador.getIdBrote());
            pstmt.setString(2, broteEncuestador.getIdUsuario());
            pstmt.setString(3, broteEncuestador.getIdBroteEncuestador());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM BROTE_ENCUESTADOR WHERE id_brote_encuestador = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<BroteEncuestador> findAll() throws SQLException {
        List<BroteEncuestador> encuestadores = new ArrayList<>();
        String sql = "SELECT * FROM BROTE_ENCUESTADOR";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                encuestadores.add(mapRow(rs));
            }
        }
        return encuestadores;
    }

    @Override
    public List<BroteEncuestador> findByBroteId(String broteId) throws SQLException {
        List<BroteEncuestador> encuestadores = new ArrayList<>();
        String sql = "SELECT * FROM BROTE_ENCUESTADOR WHERE id_brote = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, broteId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                encuestadores.add(mapRow(rs));
            }
        }
        return encuestadores;
    }

    @Override
    public List<BroteEncuestador> findByUsuarioId(String usuarioId) throws SQLException {
        List<BroteEncuestador> encuestadores = new ArrayList<>();
        String sql = "SELECT * FROM BROTE_ENCUESTADOR WHERE id_usuario = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, usuarioId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                encuestadores.add(mapRow(rs));
            }
        }
        return encuestadores;
    }

    @Override
    public Optional<BroteEncuestador> findByBroteAndUsuario(String broteId, String usuarioId) throws SQLException {
        String sql = "SELECT * FROM BROTE_ENCUESTADOR WHERE id_brote = ? AND id_usuario = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, broteId);
            pstmt.setString(2, usuarioId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    private BroteEncuestador mapRow(ResultSet rs) throws SQLException {
        return new BroteEncuestador(
                rs.getString("id_brote_encuestador"),
                rs.getString("id_brote"),
                rs.getString("id_usuario")
        );
    }
}
