package com.toxisafe.dao;

import com.toxisafe.model.Sintoma;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SintomaDao extends GenericDao<Sintoma, String> {
    List<Sintoma> findByGrupoSintomaId(String grupoId) throws SQLException;
    Optional<Sintoma> findByNombreInGrupo(String grupoId, String nombre) throws SQLException;
}

