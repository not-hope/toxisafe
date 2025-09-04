package com.toxisafe.model;

import java.util.Objects;

public class IngestaPersonaExpuesta {
    private String idIngestaPersonaExpuesta;
    private String idIngesta;
    private String idExpuesto;
    private Integer esSospechosaParaExpuesto; // 0/1

    public IngestaPersonaExpuesta(String idIngestaPersonaExpuesta, String idIngesta,
                                  String idExpuesto, Integer esSospechosaParaExpuesto) {
        this.idIngestaPersonaExpuesta = idIngestaPersonaExpuesta;
        this.idIngesta = idIngesta;
        this.idExpuesto = idExpuesto;
        this.esSospechosaParaExpuesto = esSospechosaParaExpuesto;
    }

    public String getIdIngestaPersonaExpuesta() { return idIngestaPersonaExpuesta; }
    public void setIdIngestaPersonaExpuesta(String id) { this.idIngestaPersonaExpuesta = id; }

    public String getIdIngesta() { return idIngesta; }
    public void setIdIngesta(String idIngesta) { this.idIngesta = idIngesta; }

    public String getIdExpuesto() { return idExpuesto; }
    public void setIdExpuesto(String idExpuesto) { this.idExpuesto = idExpuesto; }

    public Integer getEsSospechosaParaExpuesto() { return esSospechosaParaExpuesto; }
    public void setEsSospechosaParaExpuesto(Integer v) { this.esSospechosaParaExpuesto = v; }

    @Override
    public String toString() {
        return "IngestaPersonaExpuesta{" +
                "idIngestaPersonaExpuesta='" + idIngestaPersonaExpuesta + '\'' +
                ", idIngesta='" + idIngesta + '\'' +
                ", idExpuesto='" + idExpuesto + '\'' +
                ", esSospechosaParaExpuesto=" + esSospechosaParaExpuesto +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngestaPersonaExpuesta)) return false;
        IngestaPersonaExpuesta that = (IngestaPersonaExpuesta) o;
        return Objects.equals(idIngestaPersonaExpuesta, that.idIngestaPersonaExpuesta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idIngestaPersonaExpuesta);
    }
}
