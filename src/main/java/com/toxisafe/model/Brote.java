package com.toxisafe.model;

public class Brote {
    private String idBrote;
    private String creadorBrote;       // id_usuario
    private String responsableBrote;   // id_usuario
    private String fechIniBrote;       // Formato YYYY-MM-DD
    private String nombreBrote;
    private String estadoBrote;        // "ACTIVO" | "CERRADO"
    private String fechaCierreBrote;   // YYYY-MM-DD o null

    // Constructor
    public Brote(String idBrote, String creadorBrote, String responsableBrote,
                 String fechIniBrote, String nombreBrote) {
        this.idBrote = idBrote;
        this.creadorBrote = creadorBrote;
        this.responsableBrote = responsableBrote;
        this.fechIniBrote = fechIniBrote;
        this.nombreBrote = nombreBrote;
        this.estadoBrote ="ACTIVADO";
        this.fechaCierreBrote = null;
    }

    public Brote(String idBrote, String creadorBrote, String responsableBrote,
                 String fechIniBrote, String estadoBrote, String nombreBrote) {
        this.idBrote = idBrote;
        this.creadorBrote = creadorBrote;
        this.responsableBrote = responsableBrote;
        this.fechIniBrote = fechIniBrote;
        this.nombreBrote = nombreBrote;
        this.estadoBrote = estadoBrote;
        this.fechaCierreBrote = null;
    }

    public Brote() {

    }

    // Getters y Setters
    public String getIdBrote() { return idBrote; }
    public void setIdBrote(String idBrote) { this.idBrote = idBrote; }

    public String getCreadorBrote() { return creadorBrote; }
    public void setCreadorBrote(String creadorBrote) { this.creadorBrote = creadorBrote; }

    public String getResponsableBrote() { return responsableBrote; }
    public void setResponsableBrote(String responsableBrote) { this.responsableBrote = responsableBrote; }

    public String getFechIniBrote() { return fechIniBrote; }
    public void setFechIniBrote(String fechIniBrote) { this.fechIniBrote = fechIniBrote; }

    public String getNombreBrote() { return nombreBrote; }
    public void setNombreBrote(String nombreBrote) { this.nombreBrote = nombreBrote; }
    public String getEstadoBrote() { return estadoBrote; }
    public void setEstadoBrote(String estadoBrote) { this.estadoBrote = estadoBrote; }
    public String getFechaCierreBrote() { return fechaCierreBrote; }
    public void setFechaCierreBrote(String fechaCierreBrote) { this.fechaCierreBrote = fechaCierreBrote; }
    @Override
    public String toString() {
        return "Brote{" +
                "idBrote='" + idBrote + '\'' +
                ", nombreBrote='" + nombreBrote + '\'' +
                '}';
    }
}
