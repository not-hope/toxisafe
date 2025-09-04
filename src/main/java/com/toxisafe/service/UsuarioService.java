package com.toxisafe.service;

import com.toxisafe.dao.UsuarioDao;
import com.toxisafe.model.Brote;
import com.toxisafe.model.Usuario;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UsuarioService {
    private UsuarioDao usuarioDao;

    public UsuarioService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     */
    public Usuario registrarUsuario(String nombre, String rol, String username, String password)
            throws SQLException, IllegalArgumentException {
        if (usuarioDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }
        String rolNorm = normalizaRol(rol);
        if (!ROLES_VALIDOS.contains(rolNorm)) {
            throw new IllegalArgumentException("Rol inválido: " + rol + ". Use uno de: " + ROLES_VALIDOS);
        }
        // Aquí iría el hashing real de la contraseña
        String id = UUID.randomUUID().toString();
        Usuario nuevoUsuario = new Usuario(id, nombre, rolNorm, username, password);

        usuarioDao.insert(nuevoUsuario);
        return nuevoUsuario;
    }

    /**
     * Autentica un usuario.
     */
    public Optional<Usuario> autenticarUsuario(String username, String password) throws SQLException {
        Optional<Usuario> usuarioOptional = usuarioDao.findByUsername(username);
        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();
            // Aquí iría la comprobación del hash en una app real
            if (password.equals(usuario.getPasswordUsuario())) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    public List<Usuario> findAll() throws SQLException {
        return usuarioDao.findAll();
    }

    public List<Usuario> search(String filtro) throws SQLException {
        return usuarioDao.search(filtro);
    }

    public void update(Usuario u) throws SQLException {
        if (u.getIdUsuario() == null || u.getIdUsuario().isBlank())
            throw new IllegalArgumentException("Id de usuario inválido.");
        String rolNorm = normalizaRol(u.getRolUsuario());
        if (!ROLES_VALIDOS.contains(rolNorm)) {
            throw new IllegalArgumentException("Rol inválido: " + u.getRolUsuario() + ". Use uno de: " + ROLES_VALIDOS);
        }
        usuarioDao.update(u);
    }

    public void delete(String id) throws SQLException {
        usuarioDao.delete(id);
    }

    /** NUEVO: comprobar si un username existe (para validación en tiempo real) */
    public boolean existsUsername(String username) throws SQLException {
        if (username == null || username.isBlank()) return false;
        return usuarioDao.findByUsername(username.trim()).isPresent();
    }

    private static final java.util.Set<String> ROLES_VALIDOS =
            new java.util.HashSet<>(java.util.Arrays.asList(
                    "ADMIN", "EPIDEMIOLOGO", "MIR_SALUD_PUBLICA", "ENCUESTADOR"
            ));

    private static String normalizaRol(String r) {
        if (r == null) return "";
        String s = java.text.Normalizer.normalize(r.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // quita acentos
        s = s.toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
        // alias habituales que pueden venir de la UI / docs
        if (s.equals("EPIDEMIOLOG@")) s = "EPIDEMIOLOGO";
        return s;
    }

    /** Útil para poblar el combo en la UI */
    public java.util.List<String> rolesPermitidos() {
        return new java.util.ArrayList<>(ROLES_VALIDOS);
    }


    public Optional<Usuario> findById(String id) throws SQLException {
        return usuarioDao.findById(id);
    }
}
