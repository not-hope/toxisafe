package com.toxisafe.dao;

import com.toxisafe.model.Brote;
import java.sql.SQLException;
import java.util.List;

public interface BroteDao extends GenericDao<Brote, String> {
    List<Brote> findByCreador(String creadorId) throws SQLException;
    List<Brote> findByResponsable(String responsableId) throws SQLException;

    boolean existsByNombre(String nombre) throws SQLException;
    boolean existsByNombreExceptoId(String nombre, String idExcluir) throws SQLException;
    void actualizarEstado(String idBrote, String estado, String fechaCierreIso) throws SQLException;
    List<Brote> findByEstado(String estado) throws SQLException;

}
