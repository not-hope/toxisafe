package com.toxisafe.dao;

import com.toxisafe.model.AlimentoCatalogoAlias;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlimentoCatalogoAliasDao extends GenericDao<AlimentoCatalogoAlias, String> {
    Optional<AlimentoCatalogoAlias> findByAlias(String alias) throws SQLException;
    Optional<AlimentoCatalogoAlias> findByAliasNorm(String aliasNorm) throws SQLException;
    List<AlimentoCatalogoAlias> findByCatalogoId(String idCatalogo) throws SQLException;

    Optional<String> canonicoDeAlias(String aliasExacto) throws SQLException;

    List<String> findCanonicosByAliasPrefix(String prefix) throws SQLException;
}
