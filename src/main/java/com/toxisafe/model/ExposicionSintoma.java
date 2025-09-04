package com.toxisafe.model;

public class ExposicionSintoma {
    private String idExposicionSintoma;
    private String idSintomasGenerales; // FK a SINTOMAS_GENERALES_EXPUESTO
    private String idSintoma;           // FK a SINTOMA

    public ExposicionSintoma(String idExposicionSintoma, String idSintomasGenerales, String idSintoma) {
        this.idExposicionSintoma = idExposicionSintoma;
        this.idSintomasGenerales = idSintomasGenerales;
        this.idSintoma = idSintoma;
    }

    public String getIdExposicionSintoma() { return idExposicionSintoma; }
    public void setIdExposicionSintoma(String idExposicionSintoma) { this.idExposicionSintoma = idExposicionSintoma; }
    public String getIdSintomasGenerales() { return idSintomasGenerales; }
    public void setIdSintomasGenerales(String idSintomasGenerales) { this.idSintomasGenerales = idSintomasGenerales; }
    public String getIdSintoma() { return idSintoma; }
    public void setIdSintoma(String idSintoma) { this.idSintoma = idSintoma; }

    @Override public String toString() {
        return "ExposicionSintoma{idExposicionSintoma='" + idExposicionSintoma + "', idSintomasGenerales='"
                + idSintomasGenerales + "', idSintoma='" + idSintoma + "'}";
    }
}

