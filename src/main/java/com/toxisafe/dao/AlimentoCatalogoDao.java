package com.toxisafe.dao;

import com.toxisafe.model.AlimentoCatalogo;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlimentoCatalogoDao extends GenericDao<AlimentoCatalogo, String> {
    Optional<AlimentoCatalogo> findByNombreCanonico(String nombreCanonico) throws SQLException;
    Optional<AlimentoCatalogo> findByNombreNorm(String nombreNorm) throws SQLException;
    List<AlimentoCatalogo> findAll() throws SQLException;

    List<String> findAllNombres() throws SQLException;
    List<String> findByPrefix(String prefix) throws SQLException;
    boolean existsNombreExacto(String nombre) throws SQLException;
}
