package com.toxisafe.dao;

import com.toxisafe.model.GrupoSintoma;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface GrupoSintomaDao extends GenericDao<GrupoSintoma, String> {
    Optional<GrupoSintoma> findByDescripcion(String descripcion) throws SQLException;
    List<GrupoSintoma> findAll() throws SQLException; // (ya en Generic, lo re-declaro si te gusta explícito)
}
