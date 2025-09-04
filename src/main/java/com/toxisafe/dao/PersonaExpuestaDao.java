package com.toxisafe.dao;

import com.toxisafe.model.PersonaExpuesta;

import java.sql.SQLException;
import java.util.List;

public interface PersonaExpuestaDao extends GenericDao<PersonaExpuesta, String> {
    List<PersonaExpuesta> findByBroteId(String broteId) throws SQLException;

    boolean existsDocumentoEnBrote(String broteId, String tipoDocumento, String numDocumento) throws SQLException;

    boolean existsDocumentoEnBroteExcepto(String idExpuesto, String broteId, String tipoDocumento, String numDocumento) throws SQLException;

}
