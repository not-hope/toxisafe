package com.toxisafe.dao;

import com.toxisafe.model.Informe;

import java.sql.SQLException;
import java.util.List;

public interface InformeDao extends GenericDao<Informe, String> {
    List<Informe> findByBroteId(String broteId) throws SQLException;
}
