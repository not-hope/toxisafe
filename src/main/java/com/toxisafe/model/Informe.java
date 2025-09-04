package com.toxisafe.model;

public class Informe {
    private String idInforme;
    private String idBrote;              // FK a BROTE
    private String contenidoInforme;     // HTML u otro formato serializado

    public Informe(String idInforme, String idBrote, String contenidoInforme) {
        this.idInforme = idInforme;
        this.idBrote = idBrote;
        this.contenidoInforme = contenidoInforme;
    }

    public String getIdInforme() { return idInforme; }
    public void setIdInforme(String idInforme) { this.idInforme = idInforme; }

    public String getIdBrote() { return idBrote; }
    public void setIdBrote(String idBrote) { this.idBrote = idBrote; }

    public String getContenidoInforme() { return contenidoInforme; }
    public void setContenidoInforme(String contenidoInforme) { this.contenidoInforme = contenidoInforme; }

    @Override
    public String toString() {
        return "Informe{" +
                "idInforme='" + idInforme + '\'' +
                ", idBrote='" + idBrote + '\'' +
                ", contenidoInforme(length)=" + (contenidoInforme == null ? 0 : contenidoInforme.length()) +
                '}';
    }
}
