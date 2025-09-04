package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.PersonaExpuesta;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.*;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public class ExpuestoController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtFiltro;

    @FXML private TableView<PersonaExpuesta> tabla;
    @FXML private TableColumn<PersonaExpuesta, String> colNombre;
    @FXML private TableColumn<PersonaExpuesta, String> colApellido;
    @FXML private TableColumn<PersonaExpuesta, String> colSexo;
    @FXML private TableColumn<PersonaExpuesta, String> colEdad;
    @FXML private TableColumn<PersonaExpuesta, String> colTfno;
    @FXML private TableColumn<PersonaExpuesta, String> colAM; // atenc. médica
    @FXML private TableColumn<PersonaExpuesta, String> colEC; // estado coprocultivo
    @FXML private TableColumn<PersonaExpuesta, String> colEF; // estado frotis

    @FXML private Button btnNuevo, btnEditar, btnEliminar, btnCerrar, btnIngestas, btnSintomas;

    private IngestaService ingestaService;
    private AlimentoService alimentoService;
    private BroteService broteService;

    private Usuario currentUser;
    private Brote brote;
    private PersonaExpuestaService personaExpuestaService;

    private FilteredList<PersonaExpuesta> filtered;
    private SintomaService sintomaService;
    private GrupoSintomaService grupoSintomaService;
    private SintomasGeneralesExpuestoService sintomasGeneralesExpuestoService;
    private MainController mainController;

    public void setContext(Usuario currentUser,
                           Brote brote,
                           PersonaExpuestaService personaExpuestaService) {
        this.currentUser = currentUser;
        this.brote = brote;
        this.personaExpuestaService = personaExpuestaService;

        boolean cerrado = "CERRADO".equalsIgnoreCase(brote.getEstadoBrote());

        if (brote == null || currentUser == null || personaExpuestaService == null) {
            alertError("Contexto incompleto para Expuestos.");
            disableAll();
            return;
        }
        lblTitulo.setText("Expuestos — " + brote.getNombreBrote() + " — " +
                ("CERRADO".equalsIgnoreCase(brote.getEstadoBrote()) ? "CERRADO" : "ACTIVO"));

        configurarTabla();
        cargarDatos();
        configurarFiltro();
        configurarBotonesPorPermisos();
        if(cerrado) disableAll();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setIngestaDeps(IngestaService ingestaService,
                               AlimentoService alimentoService,
                               BroteService broteService) {
        this.ingestaService = ingestaService;
        this.alimentoService = alimentoService;
        this.broteService = broteService;
    }

    public void setSintomasController(SintomaService sintomaService, GrupoSintomaService grupoSintomaService, SintomasGeneralesExpuestoService sintomasGeneralesExpuestoService){
        this.sintomaService = sintomaService;
        this.grupoSintomaService = grupoSintomaService;
        this.sintomasGeneralesExpuestoService = sintomasGeneralesExpuestoService;
    }

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getNombreExpuesto));
        colApellido.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getApellidoExpuesto));
        colSexo.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getSexoExpuesto));
        colEdad.setCellValueFactory(d -> Bindings.createStringBinding(() -> {
            Integer e = d.getValue().getEdadExpuesto();
            return e == null ? "" : String.valueOf(e);
        }));
        colTfno.setCellValueFactory(d -> Bindings.createStringBinding(() -> {
            String t1 = d.getValue().getTfno1Expuesto();
            String t2 = d.getValue().getTfno2Expuesto();
            String a = (t1 == null ? "" : String.valueOf(t1));
            String b = (t2 == null ? "" : t2);
            if (a.isBlank()) return b;
            if (b.isBlank()) return a;
            return a + " / " + b;
        }));
        colAM.setCellValueFactory(d -> Bindings.createStringBinding(() -> toFlag(d.getValue().isAtencionMedicaExpuesto())));
        colEC.setCellValueFactory(d -> Bindings.createStringBinding(() -> toFlag(d.getValue().isEstadoCoprocultivoExpuesto())));
        colEF.setCellValueFactory(d -> Bindings.createStringBinding(() -> toFlag(d.getValue().isEstadoFrotisExpuesto())));

        tabla.setPlaceholder(new Label("Sin personas expuestas para este brote."));
    }

    private String toFlag(Integer v) {
        if (v == null) return "";
        return (v == 1) ? "Sí" : (v == 0 ? "No" : String.valueOf(v));
    }

    private void configurarTabla() {
        lblTitulo.setText("Expuestos — " + brote.getNombreBrote());
    }

    private void cargarDatos() {
        try {
            List<PersonaExpuesta> lista = personaExpuestaService.findByBroteIdVisiblePara(brote.getIdBrote(), currentUser);
            filtered = new FilteredList<>(FXCollections.observableArrayList(lista), s -> true);
            tabla.setItems(filtered);
        } catch (SecurityException se) {
            alertError(se.getMessage());
            disableAll();
        } catch (SQLException e) {
            alertError("Error cargando expuestos: " + e.getMessage());
        }
    }

    private void configurarFiltro() {
        txtFiltro.textProperty().addListener((obs, old, val) -> refrescarFiltro());
    }

    private void refrescarFiltro() {
        String q = Optional.ofNullable(txtFiltro.getText()).orElse("").trim().toLowerCase(Locale.ROOT);
        Predicate<PersonaExpuesta> p = pe -> {
            if (q.isBlank()) return true;
            return (safe(pe.getNombreExpuesto()).contains(q) ||
                    safe(pe.getApellidoExpuesto()).contains(q) ||
                    safe(pe.getSexoExpuesto()).contains(q) ||
                    safe(pe.getResultadoCoprocultivoExpuesto()).contains(q) ||
                    safe(pe.getResultadoFrotisExpuesto()).contains(q) ||
                    safe(pe.getPatogenoCoprocultivoExpuesto()).contains(q) ||
                    safe(pe.getPatogenoFrotisExpuesto()).contains(q));
        };
        if (filtered != null) filtered.setPredicate(p);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private void configurarBotonesPorPermisos() {
        String rol = norm(currentUser.getRolUsuario());
        boolean canManage = rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA");

        btnNuevo.setDisable(false);
        btnEditar.setDisable(false);
        btnEliminar.setDisable(false);

        // Si quieres limitar por rol directamente:
        if (!canManage && rol.equals("ENCUESTADOR")) {
            // dejamos activos; service decide por asignación.
        }
    }

    private void disableAll() {
        btnNuevo.setDisable(true);
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    // ===== Acciones =====

    @FXML
    private void handleNuevo() {
        abrirDialogo(null);
    }

    @FXML
    private void handleEditar() {
        PersonaExpuesta sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione una persona."); return; }
        abrirDialogo(sel);
    }

    @FXML
    private void handleEliminar() {
        PersonaExpuesta sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione una persona."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar a " + sel.getNombreExpuesto() + " " + sel.getApellidoExpuesto() + "?", ButtonType.OK, ButtonType.CANCEL);
        conf.setHeaderText("Confirmar eliminación");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    personaExpuestaService.delete(sel.getIdExpuesto(), currentUser);
                    cargarDatos();
                    refrescarFiltro();
                } catch (SecurityException se) {
                    alertError(se.getMessage());
                } catch (SQLException e) {
                    alertError("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCerrar() {
        // Cierra ventana (si está en un diálogo)
        mainController.showBrotes();
    }

    @FXML
    private void handleIngestas() {
        var sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione una persona expuesta."); return; }
        if (ingestaService == null || alimentoService == null || broteService == null) {
            alertError("Servicios de ingestas/alimentos no inicializados."); return;
        }
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource(
                    "/com/toxisafe/ui/view/expuesto_ingestas.fxml"));
            Parent root = fx.load();
            ExpuestoIngestasController ctl = fx.getController();

            // currentUser: usa el que ya guardas en este controller
            ctl.init(currentUser, sel, ingestaService, alimentoService, broteService);

            Stage dlg = new Stage();
            dlg.initOwner(tabla.getScene().getWindow());
            dlg.initModality(Modality.WINDOW_MODAL);
            dlg.setTitle("Ingestas de " + sel.getNombreExpuesto());
            dlg.setScene(new Scene(root));
            dlg.showAndWait();

            // si quieres refrescar la lista tras cerrar:
            cargarDatos();
            refrescarFiltro();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("No se pudo abrir la gestión de ingestas: " + e.getMessage());
        }
    }

    @FXML
    private void handleSintomas() {
        var sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione una persona expuesta."); return; }
        if (sintomasGeneralesExpuestoService == null) { alertError("SintomasGeneralesExpuestos Servicio no inicializado."); return; }
        if (sintomaService == null) { alertError("Sintomas Servicio no inicializado."); return; }
        if (grupoSintomaService == null) { alertError("GrupoSintoma Servicio no inicializado."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/expuesto_sintomas.fxml"));
            Parent view = loader.load();
            ExpuestoSintomasController ctl = loader.getController();
            ctl.init(currentUser, sel, grupoSintomaService, sintomaService, sintomasGeneralesExpuestoService, brote.getEstadoBrote(), true);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(tabla.getScene().getWindow());
            dialog.setTitle("Sintomas del Expuesto: " + sel.getNombreExpuesto());
            dialog.setScene(new Scene(view));
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("No se pudo abrir Expuestos: " + e.getMessage());
        }
    }


    private void abrirDialogo(PersonaExpuesta existente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/expuesto_form.fxml"));
            Parent root = loader.load();
            ExpuestoFormController form = loader.getController();
            form.init(brote, existente, currentUser, personaExpuestaService);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(tabla.getScene().getWindow());
            dialog.setTitle(existente == null ? "Nuevo expuesto" : "Editar expuesto");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            cargarDatos();
            refrescarFiltro();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Error abriendo formulario: " + e.getMessage());
        }
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void alertError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }

    private String norm(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
