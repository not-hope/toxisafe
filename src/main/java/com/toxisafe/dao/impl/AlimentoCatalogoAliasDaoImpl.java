package com.toxisafe.dao.impl;

import com.toxisafe.dao.AlimentoCatalogoAliasDao;
import com.toxisafe.model.AlimentoCatalogoAlias;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;


public class AlimentoCatalogoAliasDaoImpl implements AlimentoCatalogoAliasDao {
    private final Connection connection;

    public AlimentoCatalogoAliasDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(AlimentoCatalogoAlias a) throws SQLException {
        String sql = "INSERT INTO ALIMENTO_CATALOGO_ALIAS (alias, alias_norm, id_catalogo) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getAlias());
            ps.setString(2, a.getAliasNorm());
            ps.setString(3, a.getIdCatalogo());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<AlimentoCatalogoAlias> findById(String alias) throws SQLException {
        return findByAlias(alias);
    }

    @Override
    public void update(AlimentoCatalogoAlias a) throws SQLException {
        String sql = "UPDATE ALIMENTO_CATALOGO_ALIAS SET alias_norm=?, id_catalogo=? WHERE alias=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getAliasNorm());
            ps.setString(2, a.getIdCatalogo());
            ps.setString(3, a.getAlias());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String alias) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ALIMENTO_CATALOGO_ALIAS WHERE alias=?")) {
            ps.setString(1, alias);
            ps.executeUpdate();
        }
    }

    @Override
    public List<AlimentoCatalogoAlias> findAll() throws SQLException {
        List<AlimentoCatalogoAlias> out = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ALIMENTO_CATALOGO_ALIAS")) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public Optional<AlimentoCatalogoAlias> findByAlias(String alias) throws SQLException {
        String sql = "SELECT * FROM ALIMENTO_CATALOGO_ALIAS WHERE alias = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, alias);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    @Override
    public Optional<AlimentoCatalogoAlias> findByAliasNorm(String aliasNorm) throws SQLException {
        String sql = "SELECT * FROM ALIMENTO_CATALOGO_ALIAS WHERE alias_norm = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, aliasNorm);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    @Override
    public List<AlimentoCatalogoAlias> findByCatalogoId(String idCatalogo) throws SQLException {
        List<AlimentoCatalogoAlias> out = new ArrayList<>();
        String sql = "SELECT * FROM ALIMENTO_CATALOGO_ALIAS WHERE id_catalogo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idCatalogo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    private static AlimentoCatalogoAlias map(ResultSet rs) throws SQLException {
        return new AlimentoCatalogoAlias(
                rs.getString("alias"),
                rs.getString("alias_norm"),
                rs.getString("id_catalogo")
        );
    }

    @Override
    public Optional<String> canonicoDeAlias(String aliasExacto) throws SQLException {
        String sql = "SELECT c.nombre_canonico " +
                "FROM ALIMENTO_CATALOGO_ALIAS a " +
                "JOIN ALIMENTO_CATALOGO c ON c.id_catalogo = a.id_catalogo " +
                "WHERE a.alias = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, aliasExacto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString(1));
                return Optional.empty();
            }
        }
    }

    public List<String> findCanonicosByAliasPrefix(String prefix) throws SQLException {
        String sql = "SELECT DISTINCT c.nombre_canonico " +
                "FROM ALIMENTO_CATALOGO_ALIAS a " +
                "JOIN ALIMENTO_CATALOGO c ON c.id_catalogo = a.id_catalogo " +
                "WHERE a.alias_norm LIKE ? " +
                "ORDER BY c.nombre_canonico";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, prefix.toLowerCase(Locale.ROOT).trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }
}
