package com.toxisafe.dao;

import com.toxisafe.model.BroteEncuestador;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BroteEncuestadorDao extends GenericDao<BroteEncuestador, String> {
    List<BroteEncuestador> findByBroteId(String broteId) throws SQLException;
    List<BroteEncuestador> findByUsuarioId(String usuarioId) throws SQLException;
    Optional<BroteEncuestador> findByBroteAndUsuario(String broteId, String usuarioId) throws SQLException;
}
