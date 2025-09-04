package com.toxisafe.sync;

import java.time.Instant;
import java.util.Map;

public class SyncChange {
    public enum Op { INSERT, UPDATE, DELETE }

    private String idCambio;              // UUID
    private String instanciaOrigen;       // UUID de esta app
    private Instant timestamp;            // ISO-8601
    private Op tipoOperacion;             // INSERT/UPDATE/DELETE
    private String nombreTabla;           // p.ej. PERSONA_EXPUESTA
    private String idRegistroAfectado;    // PK del registro
    private Map<String, Object> datosAntiguos; // null en INSERT
    private Map<String, Object> datosNuevos;   // null en DELETE

    public SyncChange() {}

    // Getters/Setters
    public String getIdCambio() { return idCambio; }
    public void setIdCambio(String idCambio) { this.idCambio = idCambio; }
    public String getInstanciaOrigen() { return instanciaOrigen; }
    public void setInstanciaOrigen(String instanciaOrigen) { this.instanciaOrigen = instanciaOrigen; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Op getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(Op tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getNombreTabla() { return nombreTabla; }
    public void setNombreTabla(String nombreTabla) { this.nombreTabla = nombreTabla; }
    public String getIdRegistroAfectado() { return idRegistroAfectado; }
    public void setIdRegistroAfectado(String idRegistroAfectado) { this.idRegistroAfectado = idRegistroAfectado; }
    public Map<String, Object> getDatosAntiguos() { return datosAntiguos; }
    public void setDatosAntiguos(Map<String, Object> datosAntiguos) { this.datosAntiguos = datosAntiguos; }
    public Map<String, Object> getDatosNuevos() { return datosNuevos; }
    public void setDatosNuevos(Map<String, Object> datosNuevos) { this.datosNuevos = datosNuevos; }
}
