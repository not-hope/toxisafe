package com.toxisafe.model;

public class SintomasGeneralesExpuesto {
    private String idSintomasGenerales;
    private String idExpuesto;              // FK a PERSONA_EXPUESTA
    private String fechaInicioConjunto;     // 'YYYY-MM-DD HH:MM:SS' (nullable)
    private String fechaFinConjunto;        // 'YYYY-MM-DD HH:MM:SS' (nullable)
    private String observacionesConjunto;   // (nullable)

    public SintomasGeneralesExpuesto(String idSintomasGenerales, String idExpuesto,
                                     String fechaInicioConjunto, String fechaFinConjunto,
                                     String observacionesConjunto) {
        this.idSintomasGenerales = idSintomasGenerales;
        this.idExpuesto = idExpuesto;
        this.fechaInicioConjunto = fechaInicioConjunto;
        this.fechaFinConjunto = fechaFinConjunto;
        this.observacionesConjunto = observacionesConjunto;
    }

    public String getIdSintomasGenerales() { return idSintomasGenerales; }
    public void setIdSintomasGenerales(String idSintomasGenerales) { this.idSintomasGenerales = idSintomasGenerales; }
    public String getIdExpuesto() { return idExpuesto; }
    public void setIdExpuesto(String idExpuesto) { this.idExpuesto = idExpuesto; }
    public String getFechaInicioConjunto() { return fechaInicioConjunto; }
    public void setFechaInicioConjunto(String fechaInicioConjunto) { this.fechaInicioConjunto = fechaInicioConjunto; }
    public String getFechaFinConjunto() { return fechaFinConjunto; }
    public void setFechaFinConjunto(String fechaFinConjunto) { this.fechaFinConjunto = fechaFinConjunto; }
    public String getObservacionesConjunto() { return observacionesConjunto; }
    public void setObservacionesConjunto(String observacionesConjunto) { this.observacionesConjunto = observacionesConjunto; }

    @Override public String toString() {
        return "SintomasGeneralesExpuesto{idSintomasGenerales='" + idSintomasGenerales + "', idExpuesto='" + idExpuesto
                + "', fechaInicioConjunto='" + fechaInicioConjunto + "', fechaFinConjunto='" + fechaFinConjunto + "'}";
    }
}
