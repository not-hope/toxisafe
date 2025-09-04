package com.toxisafe.ui.controller;

import com.toxisafe.model.PersonaExpuesta;
import com.toxisafe.model.Usuario;
import com.toxisafe.model.GrupoSintoma;
import com.toxisafe.model.Sintoma;
import com.toxisafe.model.SintomasGeneralesExpuesto;
import com.toxisafe.service.GrupoSintomaService;
import com.toxisafe.service.SintomaService;
import com.toxisafe.service.SintomasGeneralesExpuestoService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador de "Síntomas del Expuesto".
 * Usa EXCLUSIVAMENTE los Services:
 *  - GrupoSintomaService
 *  - SintomaService
 *  - SintomasGeneralesExpuestoService
 */
public class ExpuestoSintomasController {

    // --- UI
    @FXML private VBox root;
    @FXML private Label lblTitulo;
    @FXML private Label lblEstadoBrote;

    @FXML private FlowPane flowGrupos;

    @FXML private DatePicker dpInicio;
    @FXML private TextField  txtHoraInicio;   // HH:mm:ss
    @FXML private DatePicker dpFin;
    @FXML private TextField  txtHoraFin;      // HH:mm:ss
    @FXML private TextArea   txtObservaciones;

    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    // --- Estado/contexto
    private Usuario currentUser;
    private PersonaExpuesta expuesto;

    private GrupoSintomaService grupoSvc;
    private SintomaService sintomaSvc;
    private SintomasGeneralesExpuestoService generalesSvc;

    private SintomasGeneralesExpuesto generalActual;  // registro general (puede crearse)
    private final Map<String, CheckBox> checkBySintomaId = new HashMap<>();

    private boolean puedeEscribir = true; // puedes atarlo a rol/brote si lo necesitas

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================== Inyección de dependencias ==================
    /**
     * Debes llamar a este init() tras cargar el FXML.
     */
    public void init(Usuario usuario,
                     PersonaExpuesta expuesto,
                     GrupoSintomaService grupoSvc,
                     SintomaService sintomaSvc,
                     SintomasGeneralesExpuestoService generalesSvc,
                     String etiquetaEstadoBrote,  // opcional, para mostrar "Brote: ... — ACTIVO/CERRADO"
                     boolean puedeEscribir) {

        this.currentUser = Objects.requireNonNull(usuario);
        this.expuesto    = Objects.requireNonNull(expuesto);
        this.grupoSvc    = Objects.requireNonNull(grupoSvc);
        this.sintomaSvc  = Objects.requireNonNull(sintomaSvc);
        this.generalesSvc= Objects.requireNonNull(generalesSvc);
        this.puedeEscribir = puedeEscribir;

        lblTitulo.setText("Síntomas de: " +
                Optional.ofNullable(expuesto.getNombreExpuesto()).orElse("") + " " +
                Optional.ofNullable(expuesto.getApellidoExpuesto()).orElse(""));
        if (etiquetaEstadoBrote != null) lblEstadoBrote.setText(etiquetaEstadoBrote);

        if(Objects.equals(etiquetaEstadoBrote, "CERRADO")) this.puedeEscribir = false;

        cargarGruposYSintomas();
        cargarDatosGeneralesYSeleccion();
        configurarDatePickersSinFuturo();
        refrescarPermisos();
    }

    // ================== Carga UI dinámica ==================
    private void cargarGruposYSintomas() {
        flowGrupos.getChildren().clear();
        checkBySintomaId.clear();

        List<GrupoSintoma> grupos;
        try {
            grupos = grupoSvc.findAll();
        } catch (SQLException e) {
            error("Error cargando grupos de síntomas: " + e.getMessage());
            return;
        }

        for (GrupoSintoma g : grupos) {
            VBox panel = crearPanelGrupo(g);
            flowGrupos.getChildren().add(panel);
        }
    }

    private VBox crearPanelGrupo(GrupoSintoma grupo) {
        Label lbl = new Label(
                Optional.ofNullable(grupo.getDescripcionGrupo()).orElse("Grupo"));
        lbl.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 6 0;");

        VBox boxChecks = new VBox(6);
        boxChecks.setPadding(new Insets(6, 8, 8, 8));

        try {
            List<Sintoma> sintomas = sintomaSvc.findByGrupo(grupo.getIdGrupoSintomas());
            for (Sintoma s : sintomas) {
                CheckBox cb = new CheckBox(Optional.ofNullable(s.getNombreSintoma()).orElse("(sin nombre)"));
                cb.setDisable(!puedeEscribir);
                checkBySintomaId.put(s.getIdSintoma(), cb);
                boxChecks.getChildren().add(cb);
            }
        } catch (SQLException e) {
            error("Error cargando síntomas del grupo: " + e.getMessage());
        }

        VBox tarjeta = new VBox(4, lbl, boxChecks);
        tarjeta.setPadding(new Insets(8));
        tarjeta.setStyle("-fx-background-color: -fx-control-inner-background; " +
                "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #dddddd;");
        tarjeta.setPrefWidth(320); // para que el FlowPane cree “columnas”
        return tarjeta;
    }

    private void cargarDatosGeneralesYSeleccion() {
        try {
            Optional<SintomasGeneralesExpuesto> opt = generalesSvc.findByExpuestoId(expuesto.getIdExpuesto());
            if (opt.isPresent()) {
                generalActual = opt.get();
                // fechas/hora
                setDateTimeIntoControls(generalActual.getFechaInicioConjunto(), dpInicio, txtHoraInicio);
                setDateTimeIntoControls(generalActual.getFechaFinConjunto(), dpFin, txtHoraFin);
                txtObservaciones.setText(
                        Optional.ofNullable(generalActual.getObservacionesConjunto()).orElse("")
                );
                // selección de síntomas
                Set<String> seleccion = generalesSvc.findSintomaIdsByGeneralId(generalActual.getIdSintomasGenerales());
                marcarSeleccion(seleccion);
            } else {
                generalActual = null;
                // vaciar/limpiar
                dpInicio.setValue(null); txtHoraInicio.clear();
                dpFin.setValue(null);    txtHoraFin.clear();
                txtObservaciones.clear();
                marcarSeleccion(Collections.emptySet());
            }
        } catch (SQLException e) {
            error("Error cargando síntomas del expuesto: " + e.getMessage());
        }
    }

    private void marcarSeleccion(Set<String> seleccion) {
        for (Map.Entry<String, CheckBox> e : checkBySintomaId.entrySet()) {
            e.getValue().setSelected(seleccion != null && seleccion.contains(e.getKey()));
        }
    }

    // ================== Acciones ==================
    @FXML
    private void handleGuardar() {
        if (!puedeEscribir) return;

        // 1) Validar/obtener fechas-hora en formato ISO "yyyy-MM-dd HH:mm:ss"
        String inicio = buildDateTime(dpInicio, txtHoraInicio);
        String fin    = buildDateTime(dpFin, txtHoraFin);
        if (inicio != null && fin != null) {
            // Validación temporal básica: inicio <= fin
            try {
                LocalDateTime t0 = LocalDateTime.parse(inicio, TS);
                LocalDateTime t1 = LocalDateTime.parse(fin, TS);
                if (t1.isBefore(t0)) {
                    info("La fecha/hora FIN no puede ser anterior al INICIO.");
                    return;
                }
            } catch (Exception ignore) {
                // Si el parse falla, el método buildDateTime ya habrá mostrado el motivo
            }
        }

        // 2) Upsert de registro general
        try {
            if (generalActual == null) {
                generalActual = new SintomasGeneralesExpuesto(
                        UUID.randomUUID().toString(),
                        expuesto.getIdExpuesto(),
                        inicio,
                        fin,
                        txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim()
                );
            } else {
                generalActual.setFechaInicioConjunto(inicio);
                generalActual.setFechaFinConjunto(fin);
                generalActual.setObservacionesConjunto(
                        txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim()
                );
            }

            // crea/actualiza y asegura id válido
            generalActual = generalesSvc.upsertGeneral(generalActual, currentUser);

            // 3) Reemplazar selección de síntomas
            Set<String> seleccion = new HashSet<>();
            for (Map.Entry<String, CheckBox> e : checkBySintomaId.entrySet()) {
                if (e.getValue().isSelected()) seleccion.add(e.getKey());
            }
            generalesSvc.replaceSintomas(generalActual.getIdSintomasGenerales(), seleccion, currentUser);

            info("Síntomas guardados correctamente.");
        } catch (SQLException | RuntimeException ex) {
            error("No se pudieron guardar los síntomas: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancelar() {
        // Cerrar ventana
        Node n = btnCancelar;
        if (n != null && n.getScene() != null && n.getScene().getWindow() != null) {
            n.getScene().getWindow().hide();
        }
    }

    // ================== Utilidades ==================
    private void setDateTimeIntoControls(String ts, DatePicker dp, TextField tfHora) {
        if (ts == null || ts.isBlank()) { dp.setValue(null); tfHora.clear(); return; }
        try {
            LocalDateTime t = LocalDateTime.parse(ts, TS);
            dp.setValue(t.toLocalDate());
            tfHora.setText(String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond()));
        } catch (Exception e) {
            // si guardaste algo no ISO, no revientes la UI
            dp.setValue(null);
            tfHora.setText(ts);
        }
    }

    /**
     * Devuelve "yyyy-MM-dd HH:mm:ss" o null si falta fecha y hora completamente.
     * Si hay fecha pero hora vacía, asume 00:00:00. Si hay hora pero no fecha, error.
     */
    private String buildDateTime(DatePicker dp, TextField tfHora) {
        LocalDate d = dp.getValue();
        String h = tfHora.getText() == null ? "" : tfHora.getText().trim();

        if (d == null && h.isEmpty()) return null;

        if (d == null) {
            info("Indica una FECHA si rellenas la hora.");
            throw new IllegalArgumentException("Hora sin fecha");
        }
        if (h.isEmpty()) {
            h = "00:00:00";
        } else if (!h.matches("^\\d{2}:\\d{2}(:\\d{2})?$")) {
            info("La hora debe tener formato HH:mm o HH:mm:ss");
            throw new IllegalArgumentException("Formato de hora inválido");
        } else if (h.length() == 5) {
            h = h + ":00";
        }

        return d.toString() + " " + h;
    }

    private void refrescarPermisos() {
        boolean dis = !puedeEscribir;
        dpInicio.setDisable(dis);
        txtHoraInicio.setDisable(dis);
        dpFin.setDisable(dis);
        txtHoraFin.setDisable(dis);
        txtObservaciones.setDisable(dis);
        btnGuardar.setDisable(dis);

        // checkboxes se deshabilitan al crearse; por si cambias permisos en runtime:
        checkBySintomaId.values().forEach(cb -> cb.setDisable(dis));
        if (root != null) {
            if (!puedeEscribir) {
                if (!root.getStyleClass().contains("readonly")) root.getStyleClass().add("readonly");
            } else {
                root.getStyleClass().remove("readonly");
            }
        }
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.showAndWait();
    }

    private void configurarDatePickersSinFuturo() {
        javafx.util.Callback<DatePicker, DateCell> noFuturas = dp -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        };
        dpInicio.setDayCellFactory(noFuturas);
        dpFin.setDayCellFactory(noFuturas);
    }
}
