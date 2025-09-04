package com.toxisafe.model;

import java.util.Objects;

public class Ingesta {
    private String idIngesta;
    private String fechaConsumo; // YYYY-MM-DD
    private String lugarConsumo;

    public Ingesta(String idIngesta, String fechaConsumo, String lugarConsumo) {
        this.idIngesta = idIngesta;
        this.fechaConsumo = fechaConsumo;
        this.lugarConsumo = lugarConsumo;
    }

    public String getIdIngesta() { return idIngesta; }
    public void setIdIngesta(String idIngesta) { this.idIngesta = idIngesta; }

    public String getFechaConsumo() { return fechaConsumo; }
    public void setFechaConsumo(String fechaConsumo) { this.fechaConsumo = fechaConsumo; }

    public String getLugarConsumo() { return lugarConsumo; }
    public void setLugarConsumo(String lugarConsumo) { this.lugarConsumo = lugarConsumo; }

    @Override
    public String toString() {
        return "Ingesta{" +
                "idIngesta='" + idIngesta + '\'' +
                ", fechaConsumo='" + fechaConsumo + '\'' +
                ", lugarConsumo='" + lugarConsumo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ingesta)) return false;
        Ingesta ingesta = (Ingesta) o;
        return Objects.equals(idIngesta, ingesta.idIngesta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idIngesta);
    }
}
