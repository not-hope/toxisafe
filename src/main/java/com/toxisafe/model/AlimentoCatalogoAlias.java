package com.toxisafe.model;

public class AlimentoCatalogoAlias {
    private String alias;
    private String aliasNorm;
    private String idCatalogo;

    public AlimentoCatalogoAlias(String alias, String aliasNorm, String idCatalogo) {
        this.alias = alias;
        this.aliasNorm = aliasNorm;
        this.idCatalogo = idCatalogo;
    }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getAliasNorm() { return aliasNorm; }
    public void setAliasNorm(String aliasNorm) { this.aliasNorm = aliasNorm; }

    public String getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(String idCatalogo) { this.idCatalogo = idCatalogo; }
}
