package com.toxisafe.model;

public class BroteEncuestador {
    private String idBroteEncuestador;
    private String idBrote;
    private String idUsuario;

    // Constructor
    public BroteEncuestador(String idBroteEncuestador, String idBrote, String idUsuario) {
        this.idBroteEncuestador = idBroteEncuestador;
        this.idBrote = idBrote;
        this.idUsuario = idUsuario;
    }

    // Getters y Setters
    public String getIdBroteEncuestador() { return idBroteEncuestador; }
    public void setIdBroteEncuestador(String idBroteEncuestador) { this.idBroteEncuestador = idBroteEncuestador; }

    public String getIdBrote() { return idBrote; }
    public void setIdBrote(String idBrote) { this.idBrote = idBrote; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    @Override
    public String toString() {
        return "BroteEncuestador{" +
                "idBroteEncuestador='" + idBroteEncuestador + '\'' +
                ", idBrote='" + idBrote + '\'' +
                ", idUsuario='" + idUsuario + '\'' +
                '}';
    }
}
