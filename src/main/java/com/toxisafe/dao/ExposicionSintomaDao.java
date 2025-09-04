package com.toxisafe.dao;

import com.toxisafe.model.ExposicionSintoma;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ExposicionSintomaDao extends GenericDao<ExposicionSintoma, String> {
    List<ExposicionSintoma> findBySintomasGeneralesId(String sintomasGeneralesId) throws SQLException;
    Optional<ExposicionSintoma> findByPar(String idSintomasGenerales, String idSintoma) throws SQLException;
}
