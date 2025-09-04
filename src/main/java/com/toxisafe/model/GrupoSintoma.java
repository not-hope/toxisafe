package com.toxisafe.model;

public class GrupoSintoma {
    private String idGrupoSintomas;
    private String descripcionGrupo;

    public GrupoSintoma(String idGrupoSintomas, String descripcionGrupo) {
        this.idGrupoSintomas = idGrupoSintomas;
        this.descripcionGrupo = descripcionGrupo;
    }

    public String getIdGrupoSintomas() { return idGrupoSintomas; }
    public void setIdGrupoSintomas(String idGrupoSintomas) { this.idGrupoSintomas = idGrupoSintomas; }
    public String getDescripcionGrupo() { return descripcionGrupo; }
    public void setDescripcionGrupo(String descripcionGrupo) { this.descripcionGrupo = descripcionGrupo; }

    @Override public String toString() {
        return "GrupoSintoma{idGrupoSintomas='" + idGrupoSintomas + "', descripcionGrupo='" + descripcionGrupo + "'}";
    }
}

