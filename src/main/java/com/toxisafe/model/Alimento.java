package com.toxisafe.model;

import java.util.Objects;

public class Alimento {
    private String idAlimento;
    private String idIngesta; // FK a INGESTA.id_ingesta
    private String nombre;
    private String idCatalogo;

    public Alimento(String idAlimento, String idIngesta, String nombre) {
        this.idAlimento = idAlimento;
        this.idIngesta = idIngesta;
        this.nombre = nombre;
    }

    public Alimento(String idAlimento, String idIngesta, String nombre, String idCatalogo) {
        this.idAlimento = idAlimento;
        this.idIngesta = idIngesta;
        this.nombre = nombre;
        this.idCatalogo = idCatalogo;
    }

    public String getIdAlimento() { return idAlimento; }
    public void setIdAlimento(String idAlimento) { this.idAlimento = idAlimento; }

    public String getIdIngesta() { return idIngesta; }
    public void setIdIngesta(String idIngesta) { this.idIngesta = idIngesta; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(String idCatalogo) { this.idCatalogo = idCatalogo; }

    @Override
    public String toString() {
        return "Alimento{" +
                "idAlimento='" + idAlimento + '\'' +
                ", idIngesta='" + idIngesta + '\'' +
                ", nombre='" + nombre + '\'' +
                ", idCatalogo='" + idCatalogo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alimento)) return false;
        Alimento that = (Alimento) o;
        return Objects.equals(idAlimento, that.idAlimento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAlimento);
    }
}
