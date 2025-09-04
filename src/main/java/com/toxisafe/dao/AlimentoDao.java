package com.toxisafe.dao;

import com.toxisafe.model.Alimento;

import java.sql.SQLException;
import java.util.List;

public interface AlimentoDao extends GenericDao<Alimento, String> {
    List<Alimento> findByIngestaId(String ingestaId) throws SQLException;

    boolean existsNombreEnIngesta(String ingestaId, String nombre) throws SQLException;

    /** Borra todos los alimentos de una ingesta (si no tienes ON DELETE CASCADE). */
    void deleteByIngestaId(String ingestaId) throws SQLException;
}
