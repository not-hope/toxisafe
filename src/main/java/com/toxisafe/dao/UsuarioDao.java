package com.toxisafe.dao;

import com.toxisafe.model.Usuario;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UsuarioDao extends GenericDao<Usuario, String> {
    Optional<Usuario> findByUsername(String username) throws SQLException;

    List<Usuario> search(String filtro) throws SQLException;
    // Otros métodos específicos de Usuario si fueran necesarios
}

