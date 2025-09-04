package com.toxisafe.model;

public class AlimentoCatalogo {
    private String idCatalogo;
    private String nombreCanonico;
    private String nombreNorm;
    private String categoria;

    public AlimentoCatalogo(String idCatalogo, String nombreCanonico, String nombreNorm, String categoria) {
        this.idCatalogo = idCatalogo;
        this.nombreCanonico = nombreCanonico;
        this.nombreNorm = nombreNorm;
        this.categoria = categoria;
    }

    public String getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(String idCatalogo) { this.idCatalogo = idCatalogo; }

    public String getNombreCanonico() { return nombreCanonico; }
    public void setNombreCanonico(String nombreCanonico) { this.nombreCanonico = nombreCanonico; }

    public String getNombreNorm() { return nombreNorm; }
    public void setNombreNorm(String nombreNorm) { this.nombreNorm = nombreNorm; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
