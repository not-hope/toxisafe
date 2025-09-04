package com.toxisafe.model;

public class PersonaExpuesta {
    private String idExpuesto;
    private String idBrote;
    private String nombreExpuesto;
    private String apellidoExpuesto;
    private String tfno1Expuesto;
    private String tfno2Expuesto;
    private String nhusaExpuesto;
    private String tipoDocumentoExpuesto;
    private String numDocumentoExpuesto;
    private String sexoExpuesto;
    private Integer edadExpuesto;
    private String fechaNacimientoExpuesto; // YYYY-MM-DD
    private String direccionExpuesto;
    private String centroSaludExpuesto;
    private String profesionExpuesto;
    private Integer manipuladorExpuesto; // 0/1
    private String grupoExpuesto;
    private Integer enfermoExpuesto; // 0/1
    private Integer atencionMedicaExpuesto; // 0/1
    private Integer atencionHospitalariaExpuesto; // 0/1
    private String fechaAtencionMedicaExpuesto; // YYYY-MM-DD
    private String lugarAtencionMedicaExpuesto;
    private String evolucionExpuesto;
    private String tratamientoExpuesto;
    private Integer solicitudCoprocultivoExpuesto; // 0/1
    private Integer estadoCoprocultivoExpuesto; // 0/1
    private String fechaCoprocultivoExpuesto; // YYYY-MM-DD
    private String laboratorioCoprocultivoExpuesto;
    private String resultadoCoprocultivoExpuesto;
    private String patogenoCoprocultivoExpuesto;
    private String observacionesCoprocultivoExpuesto;
    private Integer solicitudFrotisExpuesto; // 0/1
    private Integer estadoFrotisExpuesto; // 0/1
    private String fechaFrotisExpuesto; // YYYY-MM-DD
    private String laboratorioFrotisExpuesto;
    private String resultadoFrotisExpuesto;
    private String patogenoFrotisExpuesto;
    private String observacionesFrotisExpuesto;

    public PersonaExpuesta() {}

    // Constructor completo
    public PersonaExpuesta(String idExpuesto, String idBrote, String nombreExpuesto, String apellidoExpuesto,
                           String tfno1Expuesto, String tfno2Expuesto, String nhusaExpuesto,
                           String tipoDocumentoExpuesto, String numDocumentoExpuesto, String sexoExpuesto,
                           Integer edadExpuesto, String fechaNacimientoExpuesto, String direccionExpuesto,
                           String centroSaludExpuesto, String profesionExpuesto, Integer manipuladorExpuesto,
                           String grupoExpuesto, Integer enfermoExpuesto, Integer atencionMedicaExpuesto,
                           Integer atencionHospitalariaExpuesto, String fechaAtencionMedicaExpuesto,
                           String lugarAtencionMedicaExpuesto, String evolucionExpuesto, String tratamientoExpuesto,
                           Integer solicitudCoprocultivoExpuesto, Integer estadoCoprocultivoExpuesto,
                           String fechaCoprocultivoExpuesto, String laboratorioCoprocultivoExpuesto,
                           String resultadoCoprocultivoExpuesto, String patogenoCoprocultivoExpuesto,
                           String observacionesCoprocultivoExpuesto, Integer solicitudFrotisExpuesto,
                           Integer estadoFrotisExpuesto, String fechaFrotisExpuesto, String laboratorioFrotisExpuesto,
                           String resultadoFrotisExpuesto, String patogenoFrotisExpuesto,
                           String observacionesFrotisExpuesto) {
        this.idExpuesto = idExpuesto;
        this.idBrote = idBrote;
        this.nombreExpuesto = nombreExpuesto;
        this.apellidoExpuesto = apellidoExpuesto;
        this.tfno1Expuesto = tfno1Expuesto;
        this.tfno2Expuesto = tfno2Expuesto;
        this.nhusaExpuesto = nhusaExpuesto;
        this.tipoDocumentoExpuesto = tipoDocumentoExpuesto;
        this.numDocumentoExpuesto = numDocumentoExpuesto;
        this.sexoExpuesto = sexoExpuesto;
        this.edadExpuesto = edadExpuesto;
        this.fechaNacimientoExpuesto = fechaNacimientoExpuesto;
        this.direccionExpuesto = direccionExpuesto;
        this.centroSaludExpuesto = centroSaludExpuesto;
        this.profesionExpuesto = profesionExpuesto;
        this.manipuladorExpuesto = manipuladorExpuesto;
        this.grupoExpuesto = grupoExpuesto;
        this.enfermoExpuesto = enfermoExpuesto;
        this.atencionMedicaExpuesto = atencionMedicaExpuesto;
        this.atencionHospitalariaExpuesto = atencionHospitalariaExpuesto;
        this.fechaAtencionMedicaExpuesto = fechaAtencionMedicaExpuesto;
        this.lugarAtencionMedicaExpuesto = lugarAtencionMedicaExpuesto;
        this.evolucionExpuesto = evolucionExpuesto;
        this.tratamientoExpuesto = tratamientoExpuesto;
        this.solicitudCoprocultivoExpuesto = solicitudCoprocultivoExpuesto;
        this.estadoCoprocultivoExpuesto = estadoCoprocultivoExpuesto;
        this.fechaCoprocultivoExpuesto = fechaCoprocultivoExpuesto;
        this.laboratorioCoprocultivoExpuesto = laboratorioCoprocultivoExpuesto;
        this.resultadoCoprocultivoExpuesto = resultadoCoprocultivoExpuesto;
        this.patogenoCoprocultivoExpuesto = patogenoCoprocultivoExpuesto;
        this.observacionesCoprocultivoExpuesto = observacionesCoprocultivoExpuesto;
        this.solicitudFrotisExpuesto = solicitudFrotisExpuesto;
        this.estadoFrotisExpuesto = estadoFrotisExpuesto;
        this.fechaFrotisExpuesto = fechaFrotisExpuesto;
        this.laboratorioFrotisExpuesto = laboratorioFrotisExpuesto;
        this.resultadoFrotisExpuesto = resultadoFrotisExpuesto;
        this.patogenoFrotisExpuesto = patogenoFrotisExpuesto;
        this.observacionesFrotisExpuesto = observacionesFrotisExpuesto;
    }

    // Getters y Setters
    public String getIdExpuesto() { return idExpuesto; }
    public void setIdExpuesto(String idExpuesto) { this.idExpuesto = idExpuesto; }
    public String getIdBrote() { return idBrote; }
    public void setIdBrote(String idBrote) { this.idBrote = idBrote; }
    public String getNombreExpuesto() { return nombreExpuesto; }
    public void setNombreExpuesto(String nombreExpuesto) { this.nombreExpuesto = nombreExpuesto; }
    public String getApellidoExpuesto() { return apellidoExpuesto; }
    public void setApellidoExpuesto(String apellidoExpuesto) { this.apellidoExpuesto = apellidoExpuesto; }
    public String getTfno1Expuesto() { return tfno1Expuesto; }
    public void setTfno1Expuesto(String tfno1Expuesto) { this.tfno1Expuesto = tfno1Expuesto; }
    public String getTfno2Expuesto() { return tfno2Expuesto; }
    public void setTfno2Expuesto(String tfno2Expuesto) { this.tfno2Expuesto = tfno2Expuesto; }
    public String getNhusaExpuesto() { return nhusaExpuesto; }
    public void setNhusaExpuesto(String nhusaExpuesto) { this.nhusaExpuesto = nhusaExpuesto; }
    public String getTipoDocumentoExpuesto() { return tipoDocumentoExpuesto; }
    public void setTipoDocumentoExpuesto(String tipoDocumentoExpuesto) { this.tipoDocumentoExpuesto = tipoDocumentoExpuesto; }
    public String getNumDocumentoExpuesto() { return numDocumentoExpuesto; }
    public void setNumDocumentoExpuesto(String numDocumentoExpuesto) { this.numDocumentoExpuesto = numDocumentoExpuesto; }
    public String getSexoExpuesto() { return sexoExpuesto; }
    public void setSexoExpuesto(String sexoExpuesto) { this.sexoExpuesto = sexoExpuesto; }
    public Integer getEdadExpuesto() { return edadExpuesto; }
    public void setEdadExpuesto(Integer edadExpuesto) { this.edadExpuesto = edadExpuesto; }
    public String getFechaNacimientoExpuesto() { return fechaNacimientoExpuesto; }
    public void setFechaNacimientoExpuesto(String fechaNacimientoExpuesto) { this.fechaNacimientoExpuesto = fechaNacimientoExpuesto; }
    public String getDireccionExpuesto() { return direccionExpuesto; }
    public void setDireccionExpuesto(String direccionExpuesto) { this.direccionExpuesto = direccionExpuesto; }
    public String getCentroSaludExpuesto() { return centroSaludExpuesto; }
    public void setCentroSaludExpuesto(String centroSaludExpuesto) { this.centroSaludExpuesto = centroSaludExpuesto; }
    public String getProfesionExpuesto() { return profesionExpuesto; }
    public void setProfesionExpuesto(String profesionExpuesto) { this.profesionExpuesto = profesionExpuesto; }
    public Integer isManipuladorExpuesto() { return manipuladorExpuesto; }
    public void setManipuladorExpuesto(Integer manipuladorExpuesto) { this.manipuladorExpuesto = manipuladorExpuesto; }
    public String getGrupoExpuesto() { return grupoExpuesto; }
    public void setGrupoExpuesto(String grupoExpuesto) { this.grupoExpuesto = grupoExpuesto; }
    public Integer isEnfermoExpuesto() { return enfermoExpuesto; }
    public void setEnfermoExpuesto(Integer enfermoExpuesto) { this.enfermoExpuesto = enfermoExpuesto; }
    public Integer isAtencionMedicaExpuesto() { return atencionMedicaExpuesto; }
    public void setAtencionMedicaExpuesto(Integer atencionMedicaExpuesto) { this.atencionMedicaExpuesto = atencionMedicaExpuesto; }
    public Integer isAtencionHospitalariaExpuesto() { return atencionHospitalariaExpuesto; }
    public void setAtencionHospitalariaExpuesto(Integer atencionHospitalariaExpuesto) { this.atencionHospitalariaExpuesto = atencionHospitalariaExpuesto; }
    public String getFechaAtencionMedicaExpuesto() { return fechaAtencionMedicaExpuesto; }
    public void setFechaAtencionMedicaExpuesto(String fechaAtencionMedicaExpuesto) { this.fechaAtencionMedicaExpuesto = fechaAtencionMedicaExpuesto; }
    public String getLugarAtencionMedicaExpuesto() { return lugarAtencionMedicaExpuesto; }
    public void setLugarAtencionMedicaExpuesto(String lugarAtencionMedicaExpuesto) { this.lugarAtencionMedicaExpuesto = lugarAtencionMedicaExpuesto; }
    public String getEvolucionExpuesto() { return evolucionExpuesto; }
    public void setEvolucionExpuesto(String evolucionExpuesto) { this.evolucionExpuesto = evolucionExpuesto; }
    public String getTratamientoExpuesto() { return tratamientoExpuesto; }
    public void setTratamientoExpuesto(String tratamientoExpuesto) { this.tratamientoExpuesto = tratamientoExpuesto; }
    public Integer isSolicitudCoprocultivoExpuesto() { return solicitudCoprocultivoExpuesto; }
    public void setSolicitudCoprocultivoExpuesto(Integer solicitudCoprocultivoExpuesto) { this.solicitudCoprocultivoExpuesto = solicitudCoprocultivoExpuesto; }
    public Integer isEstadoCoprocultivoExpuesto() { return estadoCoprocultivoExpuesto; }
    public void setEstadoCoprocultivoExpuesto(Integer estadoCoprocultivoExpuesto) { this.estadoCoprocultivoExpuesto = estadoCoprocultivoExpuesto; }
    public String getFechaCoprocultivoExpuesto() { return fechaCoprocultivoExpuesto; }
    public void setFechaCoprocultivoExpuesto(String fechaCoprocultivoExpuesto) { this.fechaCoprocultivoExpuesto = fechaCoprocultivoExpuesto; }
    public String getLaboratorioCoprocultivoExpuesto() { return laboratorioCoprocultivoExpuesto; }
    public void setLaboratorioCoprocultivoExpuesto(String laboratorioCoprocultivoExpuesto) { this.laboratorioCoprocultivoExpuesto = laboratorioCoprocultivoExpuesto; }
    public String getResultadoCoprocultivoExpuesto() { return resultadoCoprocultivoExpuesto; }
    public void setResultadoCoprocultivoExpuesto(String resultadoCoprocultivoExpuesto) { this.resultadoCoprocultivoExpuesto = resultadoCoprocultivoExpuesto; }
    public String getPatogenoCoprocultivoExpuesto() { return patogenoCoprocultivoExpuesto; }
    public void setPatogenoCoprocultivoExpuesto(String patogenoCoprocultivoExpuesto) { this.patogenoCoprocultivoExpuesto = patogenoCoprocultivoExpuesto; }
    public String getObservacionesCoprocultivoExpuesto() { return observacionesCoprocultivoExpuesto; }
    public void setObservacionesCoprocultivoExpuesto(String observacionesCoprocultivoExpuesto) { this.observacionesCoprocultivoExpuesto = observacionesCoprocultivoExpuesto; }
    public Integer isSolicitudFrotisExpuesto() { return solicitudFrotisExpuesto; }
    public void setSolicitudFrotisExpuesto(Integer solicitudFrotisExpuesto) { this.solicitudFrotisExpuesto = solicitudFrotisExpuesto; }
    public Integer isEstadoFrotisExpuesto() { return estadoFrotisExpuesto; }
    public void setEstadoFrotisExpuesto(Integer estadoFrotisExpuesto) { this.estadoFrotisExpuesto = estadoFrotisExpuesto; }
    public String getFechaFrotisExpuesto() { return fechaFrotisExpuesto; }
    public void setFechaFrotisExpuesto(String fechaFrotisExpuesto) { this.fechaFrotisExpuesto = fechaFrotisExpuesto; }
    public String getLaboratorioFrotisExpuesto() { return laboratorioFrotisExpuesto; }
    public void setLaboratorioFrotisExpuesto(String laboratorioFrotisExpuesto) { this.laboratorioFrotisExpuesto = laboratorioFrotisExpuesto; }
    public String getResultadoFrotisExpuesto() { return resultadoFrotisExpuesto; }
    public void setResultadoFrotisExpuesto(String resultadoFrotisExpuesto) { this.resultadoFrotisExpuesto = resultadoFrotisExpuesto; }
    public String getPatogenoFrotisExpuesto() { return patogenoFrotisExpuesto; }
    public void setPatogenoFrotisExpuesto(String patogenoFrotisExpuesto) { this.patogenoFrotisExpuesto = patogenoFrotisExpuesto; }
    public String getObservacionesFrotisExpuesto() { return observacionesFrotisExpuesto; }
    public void setObservacionesFrotisExpuesto(String observacionesFrotisExpuesto) { this.observacionesFrotisExpuesto = observacionesFrotisExpuesto; }

    @Override
    public String toString() {
        return "PersonaExpuesta{" +
                "idExpuesto='" + idExpuesto + '\'' +
                ", nombreExpuesto='" + nombreExpuesto + '\'' +
                ", apellidoExpuesto='" + apellidoExpuesto + '\'' +
                ", idBrote='" + idBrote + '\'' +
                '}';
    }
}
