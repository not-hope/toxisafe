package com.toxisafe.dao.impl;

import com.toxisafe.dao.IngestaDao;
import com.toxisafe.model.Ingesta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngestaDaoImpl implements IngestaDao {

    private final Connection connection;

    public IngestaDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Ingesta ingesta) throws SQLException {
        final String sql = "INSERT INTO INGESTA (id_ingesta, fecha_consumo, lugar_consumo) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ingesta.getIdIngesta());
            pstmt.setString(2, ingesta.getFechaConsumo());
            pstmt.setString(3, ingesta.getLugarConsumo());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<Ingesta> findById(String id) throws SQLException {
        final String sql = "SELECT id_ingesta, fecha_consumo, lugar_consumo FROM INGESTA WHERE id_ingesta = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Ingesta(
                            rs.getString("id_ingesta"),
                            rs.getString("fecha_consumo"),
                            rs.getString("lugar_consumo")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(Ingesta ingesta) throws SQLException {
        final String sql = "UPDATE INGESTA SET fecha_consumo = ?, lugar_consumo = ? WHERE id_ingesta = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ingesta.getFechaConsumo());
            pstmt.setString(2, ingesta.getLugarConsumo());
            pstmt.setString(3, ingesta.getIdIngesta());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM INGESTA WHERE id_ingesta = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Ingesta> findAll() throws SQLException {
        final String sql = "SELECT id_ingesta, fecha_consumo, lugar_consumo FROM INGESTA";
        List<Ingesta> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Ingesta(
                        rs.getString("id_ingesta"),
                        rs.getString("fecha_consumo"),
                        rs.getString("lugar_consumo")
                ));
            }
        }
        return list;
    }
}
