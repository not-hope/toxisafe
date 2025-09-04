package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.PersonaExpuesta;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.PersonaExpuestaService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class ExpuestoFormController {

    // Datos básicos
    @FXML private TextField txtNombre, txtApellido, txtTfno1, txtTfno2, txtNHUSA, txtNumDoc, txtDireccion, txtCentro, txtProfesion, txtGrupo;
    @FXML private ComboBox<String> cmbSexo, cmbTipoDoc;
    @FXML private Spinner<Integer> spEdad;
    @FXML private DatePicker dpNacimiento;

    // Estados/atención
    @FXML private CheckBox chkManipulador, chkEnfermo, chkAMedica, chkAHosp;
    @FXML private DatePicker dpAMedica;
    @FXML private TextField txtLugarAMedica;
    @FXML private TextArea txaEvolucion, txaTratamiento;

    // Coprocultivo
    @FXML private CheckBox chkSolCopro, chkEstCopro;
    @FXML private DatePicker dpCopro;
    @FXML private TextField txtLabCopro, txtResCopro, txtPatCopro;
    @FXML private TextArea txaObsCopro;

    // Frotis
    @FXML private CheckBox chkSolFrotis, chkEstFrotis;
    @FXML private DatePicker dpFrotis;
    @FXML private TextField txtLabFrotis, txtResFrotis, txtPatFrotis;
    @FXML private TextArea txaObsFrotis;
    @FXML private Button btnGuardar;

    @FXML private Label lblErrores;

    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private Brote brote;
    private PersonaExpuesta existente;
    private Usuario currentUser;
    private PersonaExpuestaService personaExpuestaService;

    public void init(Brote brote,
                     PersonaExpuesta existente,
                     Usuario currentUser,
                     PersonaExpuestaService personaExpuestaService) {
        this.brote = brote;
        this.existente = existente;
        this.currentUser = currentUser;
        this.personaExpuestaService = personaExpuestaService;

        // combos
        cmbSexo.getItems().setAll("Hombre", "Mujer", "Otro");
        cmbTipoDoc.getItems().setAll("DNI", "NIE", "PASAPORTE", "OTRO");

        if ("CERRADO".equalsIgnoreCase(brote.getEstadoBrote()) && btnGuardar != null) {
            btnGuardar.setDisable(true);
        }

        // spinner edad
        var vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 0);

        // Conversor que tolera vacío/errores y clampa al rango
        vf.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }
            @Override public Integer fromString(String s) {
                if (s == null || s.trim().isEmpty()) return null; // deja null si quieres validarlo luego
                try {
                    int v = Integer.parseInt(s.trim());
                    // clamp al rango del spinner
                    v = Math.max(vf.getMin(), Math.min(v, vf.getMax()));
                    return v;
                } catch (NumberFormatException e) {
                    // Si hay basura, mantenemos el valor actual
                    return vf.getValue();
                }
            }
        });

        spEdad.setValueFactory(vf);
        spEdad.setEditable(true);

        // Commit al perder foco y al pulsar Enter
        spEdad.focusedProperty().addListener((obs, old, nowFocused) -> {
            if (!nowFocused) commitEditorText(spEdad);
        });
        spEdad.getEditor().setOnAction(e -> commitEditorText(spEdad));

        spEdad.getEditor().textProperty().addListener((obs, old, txt) -> {
            if (txt == null) return;
            String limpio = txt.replaceAll("[^\\d]", ""); // solo dígitos
            if (!limpio.equals(txt)) {
                int caret = spEdad.getEditor().getCaretPosition();
                spEdad.getEditor().setText(limpio);
                spEdad.getEditor().positionCaret(Math.min(caret, limpio.length()));
            }
        });

        // opcional: impedir futuro en estos campos
        setNoFuture(dpNacimiento);
        setNoFuture(dpAMedica);
        setNoFuture(dpCopro);
        setNoFuture(dpFrotis);

        if (existente != null) {
            cargarEnFormulario(existente);
        }
    }

    private void setNoFuture(DatePicker dp) {
        dp.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    private void cargarEnFormulario(PersonaExpuesta pe) {
        txtNombre.setText(pe.getNombreExpuesto());
        txtApellido.setText(pe.getApellidoExpuesto());
        txtTfno1.setText(pe.getTfno1Expuesto() == null ? "" : String.valueOf(pe.getTfno1Expuesto()));
        txtTfno2.setText(pe.getTfno2Expuesto());
        txtNHUSA.setText(pe.getNhusaExpuesto());
        cmbTipoDoc.setValue(pe.getTipoDocumentoExpuesto());
        txtNumDoc.setText(pe.getNumDocumentoExpuesto());
        cmbSexo.setValue(pe.getSexoExpuesto());
        if (pe.getEdadExpuesto() != null) spEdad.getValueFactory().setValue(pe.getEdadExpuesto());
        setDateSafe(dpNacimiento, pe.getFechaNacimientoExpuesto());
        txtDireccion.setText(pe.getDireccionExpuesto());
        txtCentro.setText(pe.getCentroSaludExpuesto());
        txtProfesion.setText(pe.getProfesionExpuesto());
        chkManipulador.setSelected(int1(pe.isManipuladorExpuesto()));
        txtGrupo.setText(pe.getGrupoExpuesto());
        chkEnfermo.setSelected(int1(pe.isEnfermoExpuesto()));
        chkAMedica.setSelected(int1(pe.isAtencionMedicaExpuesto()));
        chkAHosp.setSelected(int1(pe.isAtencionHospitalariaExpuesto()));
        setDateSafe(dpAMedica, pe.getFechaAtencionMedicaExpuesto());
        txtLugarAMedica.setText(pe.getLugarAtencionMedicaExpuesto());
        txaEvolucion.setText(pe.getEvolucionExpuesto());
        txaTratamiento.setText(pe.getTratamientoExpuesto());
        chkSolCopro.setSelected(int1(pe.isSolicitudCoprocultivoExpuesto()));
        chkEstCopro.setSelected(int1(pe.isEstadoCoprocultivoExpuesto()));
        setDateSafe(dpCopro, pe.getFechaCoprocultivoExpuesto());
        txtLabCopro.setText(pe.getLaboratorioCoprocultivoExpuesto());
        txtResCopro.setText(pe.getResultadoCoprocultivoExpuesto());
        txtPatCopro.setText(pe.getPatogenoCoprocultivoExpuesto());
        txaObsCopro.setText(pe.getObservacionesCoprocultivoExpuesto());
        chkSolFrotis.setSelected(int1(pe.isSolicitudFrotisExpuesto()));
        chkEstFrotis.setSelected(int1(pe.isEstadoFrotisExpuesto()));
        setDateSafe(dpFrotis, pe.getFechaFrotisExpuesto());
        txtLabFrotis.setText(pe.getLaboratorioFrotisExpuesto());
        txtResFrotis.setText(pe.getResultadoFrotisExpuesto());
        txtPatFrotis.setText(pe.getPatogenoFrotisExpuesto());
        txaObsFrotis.setText(pe.getObservacionesFrotisExpuesto());
    }

    private boolean int1(Integer v) { return v != null && v == 1; }
    private String fmt(LocalDate d) { return d == null ? null : d.format(DF); }
    private void setDateSafe(DatePicker dp, String iso) {
        try { dp.setValue((iso == null || iso.isBlank()) ? null : LocalDate.parse(iso, DF)); } catch (Exception ignore) {}
    }

    @FXML
    private void handleGuardar() {
        lblErrores.setText("");
        String err = validar();
        if (err != null) { lblErrores.setText(err); return; }

        PersonaExpuesta pe = (existente != null) ? existente : new PersonaExpuesta();
        if (pe.getIdExpuesto() == null || pe.getIdExpuesto().isBlank()) {
            pe.setIdExpuesto(UUID.randomUUID().toString());
        }

        // Brote asociado
        pe.setIdBrote(brote.getIdBrote());

        // Datos básicos
        pe.setNombreExpuesto(txtNombre.getText().trim());
        pe.setApellidoExpuesto(txtApellido.getText().trim());
        pe.setTfno1Expuesto(emptyToNull(txtTfno1.getText()));
        pe.setTfno2Expuesto(emptyToNull(txtTfno2.getText()));
        pe.setNhusaExpuesto(emptyToNull(txtNHUSA.getText()));
        pe.setTipoDocumentoExpuesto(cmbTipoDoc.getValue());
        pe.setNumDocumentoExpuesto(emptyToNull(txtNumDoc.getText()));
        pe.setSexoExpuesto(cmbSexo.getValue());
        pe.setEdadExpuesto(spEdad.getValue());
        pe.setFechaNacimientoExpuesto(fmt(dpNacimiento.getValue()));
        pe.setDireccionExpuesto(emptyToNull(txtDireccion.getText()));
        pe.setCentroSaludExpuesto(emptyToNull(txtCentro.getText()));
        pe.setProfesionExpuesto(emptyToNull(txtProfesion.getText()));
        pe.setManipuladorExpuesto(chkManipulador.isSelected() ? 1 : 0);
        pe.setGrupoExpuesto(emptyToNull(txtGrupo.getText()));
        pe.setEnfermoExpuesto(chkEnfermo.isSelected() ? 1 : 0);
        pe.setAtencionMedicaExpuesto(chkAMedica.isSelected() ? 1 : 0);
        pe.setAtencionHospitalariaExpuesto(chkAHosp.isSelected() ? 1 : 0);
        pe.setFechaAtencionMedicaExpuesto(fmt(dpAMedica.getValue()));
        pe.setLugarAtencionMedicaExpuesto(emptyToNull(txtLugarAMedica.getText()));
        pe.setEvolucionExpuesto(emptyToNull(txaEvolucion.getText()));
        pe.setTratamientoExpuesto(emptyToNull(txaTratamiento.getText()));

        // Coprocultivo
        pe.setSolicitudCoprocultivoExpuesto(chkSolCopro.isSelected() ? 1 : 0);
        pe.setEstadoCoprocultivoExpuesto(chkEstCopro.isSelected() ? 1 : 0);
        pe.setFechaCoprocultivoExpuesto(fmt(dpCopro.getValue()));
        pe.setLaboratorioCoprocultivoExpuesto(emptyToNull(txtLabCopro.getText()));
        pe.setResultadoCoprocultivoExpuesto(emptyToNull(txtResCopro.getText()));
        pe.setPatogenoCoprocultivoExpuesto(emptyToNull(txtPatCopro.getText()));
        pe.setObservacionesCoprocultivoExpuesto(emptyToNull(txaObsCopro.getText()));

        // Frotis
        pe.setSolicitudFrotisExpuesto(chkSolFrotis.isSelected() ? 1 : 0);
        pe.setEstadoFrotisExpuesto(chkEstFrotis.isSelected() ? 1 : 0);
        pe.setFechaFrotisExpuesto(fmt(dpFrotis.getValue()));
        pe.setLaboratorioFrotisExpuesto(emptyToNull(txtLabFrotis.getText()));
        pe.setResultadoFrotisExpuesto(emptyToNull(txtResFrotis.getText()));
        pe.setPatogenoFrotisExpuesto(emptyToNull(txtPatFrotis.getText()));
        pe.setObservacionesFrotisExpuesto(emptyToNull(txaObsFrotis.getText()));

        commitEditorText(spEdad);

        try {
            if (existente == null) {
                personaExpuestaService.create(pe, currentUser);
            } else {
                personaExpuestaService.update(pe, currentUser);
            }
            close();
        } catch (IllegalArgumentException | SecurityException ex) {
            lblErrores.setText(ex.getMessage());
        } catch (SQLException e) {
            lblErrores.setText("Error guardando: " + e.getMessage());
        }
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String validar() {
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) return "El nombre es obligatorio.";
        if (txtApellido.getText() == null || txtApellido.getText().isBlank()) return "El apellido es obligatorio.";
        // fechas ya las validará el servicio en formato; aquí prevenimos futuros si lo deseas:
        if (dpNacimiento.getValue() != null && dpNacimiento.getValue().isAfter(LocalDate.now()))
            return "La fecha de nacimiento no puede ser posterior a hoy.";
        return null;
    }

    @FXML
    private void handleCancelar() { close(); }

    private void close() {
        Stage st = (Stage) lblErrores.getScene().getWindow();
        st.close();
    }

    private static <T> void commitEditorText(Spinner<T> spinner) {
        if (!spinner.isEditable()) return;
        final var valueFactory = spinner.getValueFactory();
        if (valueFactory == null) return;

        final var editor = spinner.getEditor();
        final var converter = valueFactory.getConverter();
        if (converter != null) {
            final String text = editor.getText();
            try {
                T value = converter.fromString(text);
                valueFactory.setValue(value);
            } catch (Exception ex) {
                // Revertir editor al valor actual si hubo error
                editor.setText(converter.toString(valueFactory.getValue()));
            }
        }
    }

}
