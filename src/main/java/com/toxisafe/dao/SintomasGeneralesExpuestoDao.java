package com.toxisafe.dao;

import com.toxisafe.model.SintomasGeneralesExpuesto;
import java.sql.SQLException;
import java.util.Optional;

public interface SintomasGeneralesExpuestoDao extends GenericDao<SintomasGeneralesExpuesto, String> {
    Optional<SintomasGeneralesExpuesto> findByExpuestoId(String expuestoId) throws SQLException;
}
