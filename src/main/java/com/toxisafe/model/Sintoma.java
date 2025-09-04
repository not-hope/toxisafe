package com.toxisafe.model;

public class Sintoma {
    private String idSintoma;
    private String idGrupoSintomas; // FK
    private String nombreSintoma;

    public Sintoma(String idSintoma, String idGrupoSintomas, String nombreSintoma) {
        this.idSintoma = idSintoma;
        this.idGrupoSintomas = idGrupoSintomas;
        this.nombreSintoma = nombreSintoma;
    }

    public String getIdSintoma() { return idSintoma; }
    public void setIdSintoma(String idSintoma) { this.idSintoma = idSintoma; }
    public String getIdGrupoSintomas() { return idGrupoSintomas; }
    public void setIdGrupoSintomas(String idGrupoSintomas) { this.idGrupoSintomas = idGrupoSintomas; }
    public String getNombreSintoma() { return nombreSintoma; }
    public void setNombreSintoma(String nombreSintoma) { this.nombreSintoma = nombreSintoma; }

    @Override public String toString() {
        return "Sintoma{idSintoma='" + idSintoma + "', idGrupoSintomas='" + idGrupoSintomas + "', nombreSintoma='" + nombreSintoma + "'}";
    }
}
