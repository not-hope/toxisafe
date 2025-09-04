package com.toxisafe.dao;

import com.toxisafe.model.IngestaPersonaExpuesta;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IngestaPersonaExpuestaDao extends GenericDao<IngestaPersonaExpuesta, String> {
    List<IngestaPersonaExpuesta> findByIngestaId(String ingestaId) throws SQLException;
    List<IngestaPersonaExpuesta> findByExpuestoId(String expuestoId) throws SQLException;
    Optional<IngestaPersonaExpuesta> findByIngestaAndExpuesto(String ingestaId, String expuestoId) throws SQLException;
    List<IngestaPersonaExpuesta> findSospechosasByExpuestoId(String expuestoId) throws SQLException;
    void deleteByIngestaAndExpuesto(String ingestaId, String expuestoId) throws SQLException;
    void deleteByIngestaId(String ingestaId) throws SQLException;
}
