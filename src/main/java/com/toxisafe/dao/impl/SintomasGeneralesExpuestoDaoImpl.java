package com.toxisafe.dao.impl;

import com.toxisafe.dao.SintomasGeneralesExpuestoDao;
import com.toxisafe.model.SintomasGeneralesExpuesto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SintomasGeneralesExpuestoDaoImpl implements SintomasGeneralesExpuestoDao {

    private final Connection connection;

    public SintomasGeneralesExpuestoDaoImpl(Connection connection) { this.connection = connection; }

    private static void setStr(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v);
    }

    @Override
    public void insert(SintomasGeneralesExpuesto g) throws SQLException {
        final String sql = "INSERT INTO SINTOMAS_GENERALES_EXPUESTO (id_sintomas_generales, id_expuesto, fecha_inicio_conjunto, fecha_fin_conjunto, observaciones_conjunto) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, g.getIdSintomasGenerales());
            ps.setString(2, g.getIdExpuesto());
            setStr(ps, 3, g.getFechaInicioConjunto());
            setStr(ps, 4, g.getFechaFinConjunto());
            setStr(ps, 5, g.getObservacionesConjunto());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<SintomasGeneralesExpuesto> findById(String id) throws SQLException {
        final String sql = "SELECT id_sintomas_generales, id_expuesto, fecha_inicio_conjunto, fecha_fin_conjunto, observaciones_conjunto FROM SINTOMAS_GENERALES_EXPUESTO WHERE id_sintomas_generales = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SintomasGeneralesExpuesto(
                            rs.getString("id_sintomas_generales"),
                            rs.getString("id_expuesto"),
                            rs.getString("fecha_inicio_conjunto"),
                            rs.getString("fecha_fin_conjunto"),
                            rs.getString("observaciones_conjunto")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(SintomasGeneralesExpuesto g) throws SQLException {
        final String sql = "UPDATE SINTOMAS_GENERALES_EXPUESTO SET id_expuesto = ?, fecha_inicio_conjunto = ?, fecha_fin_conjunto = ?, observaciones_conjunto = ? WHERE id_sintomas_generales = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, g.getIdExpuesto());
            setStr(ps, 2, g.getFechaInicioConjunto());
            setStr(ps, 3, g.getFechaFinConjunto());
            setStr(ps, 4, g.getObservacionesConjunto());
            ps.setString(5, g.getIdSintomasGenerales());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        final String sql = "DELETE FROM SINTOMAS_GENERALES_EXPUESTO WHERE id_sintomas_generales = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<SintomasGeneralesExpuesto> findAll() throws SQLException {
        final String sql = "SELECT id_sintomas_generales, id_expuesto, fecha_inicio_conjunto, fecha_fin_conjunto, observaciones_conjunto FROM SINTOMAS_GENERALES_EXPUESTO";
        List<SintomasGeneralesExpuesto> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new SintomasGeneralesExpuesto(
                        rs.getString("id_sintomas_generales"),
                        rs.getString("id_expuesto"),
                        rs.getString("fecha_inicio_conjunto"),
                        rs.getString("fecha_fin_conjunto"),
                        rs.getString("observaciones_conjunto")
                ));
            }
        }
        return out;
    }

    @Override
    public Optional<SintomasGeneralesExpuesto> findByExpuestoId(String expuestoId) throws SQLException {
        final String sql = "SELECT id_sintomas_generales, id_expuesto, fecha_inicio_conjunto, fecha_fin_conjunto, observaciones_conjunto FROM SINTOMAS_GENERALES_EXPUESTO WHERE id_expuesto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, expuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SintomasGeneralesExpuesto(
                            rs.getString("id_sintomas_generales"),
                            rs.getString("id_expuesto"),
                            rs.getString("fecha_inicio_conjunto"),
                            rs.getString("fecha_fin_conjunto"),
                            rs.getString("observaciones_conjunto")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}