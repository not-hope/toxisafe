package com.toxisafe.dao.impl;

import com.toxisafe.dao.UsuarioDao;
import com.toxisafe.model.Usuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDaoImpl implements UsuarioDao {

    private Connection connection; // La conexión a la BD se inyecta

    public UsuarioDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO USUARIO (id_usuario, nombre_usuario, rol_usuario, username_usuario, password_usuario) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getIdUsuario());
            pstmt.setString(2, usuario.getNombreUsuario());
            pstmt.setString(3, usuario.getRolUsuario());
            pstmt.setString(4, usuario.getUsernameUsuario());
            pstmt.setString(5, usuario.getPasswordUsuario());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<Usuario> findById(String id) throws SQLException {
        String sql = "SELECT * FROM USUARIO WHERE id_usuario = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Usuario(
                        rs.getString("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("rol_usuario"),
                        rs.getString("username_usuario"),
                        rs.getString("password_usuario")
                ));
            }
        }
        return Optional.empty();
    }

    @Override
    public void update(Usuario usuario) throws SQLException {
        String sql = "UPDATE USUARIO SET nombre_usuario = ?, rol_usuario = ?, username_usuario = ?, password_usuario = ? WHERE id_usuario = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getNombreUsuario());
            pstmt.setString(2, usuario.getRolUsuario());
            pstmt.setString(3, usuario.getUsernameUsuario());
            pstmt.setString(4, usuario.getPasswordUsuario());
            pstmt.setString(5, usuario.getIdUsuario());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM USUARIO WHERE id_usuario = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Usuario> findAll() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM USUARIO";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(new Usuario(
                        rs.getString("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("rol_usuario"),
                        rs.getString("username_usuario"),
                        rs.getString("password_usuario")
                ));
            }
        }
        return usuarios;
    }

    @Override
    public Optional<Usuario> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM USUARIO WHERE username_usuario = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Usuario(
                        rs.getString("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("rol_usuario"),
                        rs.getString("username_usuario"),
                        rs.getString("password_usuario")
                ));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> search(String filtro) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE nombre LIKE ? OR username LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String pattern = "%" + filtro + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(mapResultSet(rs));
            }
        }
        return lista;
    }

    private Usuario mapResultSet(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getString("id"),
                rs.getString("nombre"),
                rs.getString("rol"),
                rs.getString("username"),
                rs.getString("password")
        );
    }
}

