package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.BroteEncuestador;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.*;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BroteController {

    @FXML private TableView<Brote> tablaBrotes;
    @FXML private TableColumn<Brote, String> colNombre;
    @FXML private TableColumn<Brote, String> colFecha;
    @FXML private TableColumn<Brote, String> colResponsable;
    @FXML private TableColumn<Brote, String> colCreador;
    @FXML private TableColumn<Brote, String> colEstado;
    @FXML private TableColumn<Brote, Void> colAcciones;


    @FXML private TextField txtFiltro;
    @FXML private ComboBox<String> cmbScope;

    @FXML private Button btnAsignar, btnCrear, btnEditar, btnEliminar, btnExpuestos, btnCerrar, btnReabrir, btnAnalisis;

    private IngestaService ingestaService;
    private AlimentoService alimentoService;
    private Usuario currentUser;
    private BroteService broteService;
    private UsuarioService usuarioService;
    private BroteEncuestadorService broteEncuestadorService;
    private com.toxisafe.service.PersonaExpuestaService personaExpuestaService;
    private com.toxisafe.service.SintomasGeneralesExpuestoService sintomasGeneralesExpuestoService;
    private com.toxisafe.service.SintomaService sintomaService;
    private com.toxisafe.service.GrupoSintomaService grupoSintomaService;
    private com.toxisafe.service.InformeService informeService;
    private com.toxisafe.service.EstadisticaService estadisticaService;
    private MainController mainController;


    private FilteredList<Brote> filtered;
    private final Map<String, Usuario> usuariosCache = new HashMap<>();

    public void setContext(Usuario currentUser,
                           BroteService broteService,
                           UsuarioService usuarioService,
                           BroteEncuestadorService broteEncuestadorService) {
        this.currentUser = currentUser;
        this.broteService = broteService;
        this.usuarioService = usuarioService;
        this.broteEncuestadorService = broteEncuestadorService;

        configurarTabla();
        cargarUsuariosCache();
        cargarTabla();
        configurarScopeYBotones();
        configurarFiltro();
        colAcciones.setStyle("-fx-alignment: CENTER-RIGHT;");
        colAcciones.setPrefWidth(400);
    }

    public void setPersonaExpuestaService(com.toxisafe.service.PersonaExpuestaService s) { this.personaExpuestaService = s; }

    public void setSintomasController(SintomaService sintomaService, GrupoSintomaService grupoSintomaService, SintomasGeneralesExpuestoService sintomasGeneralesExpuestoService){
        this.sintomaService = sintomaService;
        this.grupoSintomaService = grupoSintomaService;
        this.sintomasGeneralesExpuestoService = sintomasGeneralesExpuestoService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getNombreBrote));
        colFecha.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getFechIniBrote));
        colResponsable.setCellValueFactory(d -> Bindings.createStringBinding(() -> resolveNombreUsuario(d.getValue().getResponsableBrote())));
        colCreador.setCellValueFactory(d -> Bindings.createStringBinding(() -> resolveNombreUsuario(d.getValue().getCreadorBrote())));
        colEstado.setCellValueFactory(d -> Bindings.createStringBinding(d.getValue()::getEstadoBrote));
        if (colAcciones == null) {
            colAcciones = new TableColumn<>("Acciones");
            tablaBrotes.getColumns().add(colAcciones);
        }
        configurarColumnaAcciones();

    }

    public void setIngestaServices(IngestaService i, AlimentoService a, BroteService b) {
        this.ingestaService = i;
        this.alimentoService = a;
        this.broteService = b;
    }

    public void setAnalisis(InformeService informeService, EstadisticaService estadisticaService){
        this.informeService = informeService;
        this.estadisticaService = estadisticaService;
    }

    private void configurarTabla() {
        tablaBrotes.setPlaceholder(new Label("Sin brotes"));
    }

    private void configurarColumnaAcciones() {
        // Opcional pero útil para algunas versiones de JavaFX con columnas Void:
        // colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));

        colAcciones.setCellFactory(col -> new TableCell<Brote, Void>() {

            private final Button btnAsignar = new Button();
            private final Button btnEditar  = new Button();
            private final Button btnEliminar= new Button();
            private final Button btnCerrar  = new Button();
            private final Button btnReabrir = new Button();
            private final Button btnAnalisis = new Button();
            private final Button btnExpuestos = new Button();

            private final HBox box = new HBox(10, btnAsignar, btnEditar, btnExpuestos, btnAnalisis, btnEliminar, btnCerrar, btnReabrir);
            {
                box.setStyle("-fx-alignment: CENTER_RIGHT;");

                // Estilos (si usas CSS propio)
                btnAsignar.getStyleClass().addAll("btn-sm","btn-square","btn-assign");
                btnEditar.getStyleClass().addAll("btn-sm","btn-square","btn-assign");
                btnEliminar.getStyleClass().addAll("btn-sm","btn-square","btn-danger");
                btnCerrar.getStyleClass().addAll("btn-sm","btn-rectangle","btn-close");
                btnReabrir.getStyleClass().addAll("btn-sm","btn-rectangle","btn-open");
                btnExpuestos.getStyleClass().addAll("btn-sm","btn-square","btn-assign");
                btnAnalisis.getStyleClass().addAll("btn-sm","btn-square","btn-assign");

// alineación fuerte a la derecha (además del estilo del HBox)
                box.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                // Selecciona la fila y llama a TUS handlers existentes
                btnAsignar.setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleAsignar(); });
                btnEditar .setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleEditar();   });
                btnEliminar.setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleEliminar(); });
                btnCerrar .setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleCerrarBrote();   });
                btnReabrir.setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleReabrirBrote(); });
                btnExpuestos.setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleExpuestos(); });
                btnAnalisis.setOnAction(e -> { tablaBrotes.getSelectionModel().select(getIndex()); handleAnalisis(); });


                btnAsignar .setGraphic(mdi("mdi2a-account-plus"));
                btnEditar  .setGraphic(mdi("mdi2p-pencil"));
                btnEliminar.setGraphic(mdi("mdi2d-delete"));
                btnCerrar  .setGraphic(mdi("mdi2l-lock"));
                btnReabrir .setGraphic(mdi("mdi2l-lock-open-variant"));
                btnExpuestos.setGraphic(mdi("mdi2e-emoticon-sick"));
                btnAnalisis.setGraphic(mdi("mdi2f-file-chart"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Brote row = getTableView().getItems().get(getIndex());
                boolean cerrado = "CERRADO".equalsIgnoreCase(row.getEstadoBrote());
                boolean puede = puedeEditar();

                btnAsignar.setDisable(cerrado || !puede);
                btnEditar .setDisable(cerrado || !puede);
                btnEliminar.setDisable(cerrado || !puede);
                btnAnalisis.setDisable(cerrado || !puede);

                // Mostramos uno u otro (Cerrar / Activar)
                btnCerrar.setVisible(!cerrado);
                btnCerrar.setManaged(!cerrado);
                btnCerrar.setDisable(!puede);

                btnReabrir.setVisible(cerrado);
                btnReabrir.setManaged(cerrado);
                btnReabrir.setDisable(!puede);

                boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRolUsuario());
                boolean isEpi = "EPIDEMIOLOGO".equalsIgnoreCase(currentUser.getRolUsuario());
                boolean isMir   = "MIR_SALUD_PUBLICA".equalsIgnoreCase(currentUser.getRolUsuario()); // NUEVO

                // Visibilidad botones por rol (ajusta según tu TFG si procede)
                boolean puedeEditar = isAdmin || isEpi || isMir;
                btnCrear.setVisible(puedeEditar);
                btnEditar.setVisible(puedeEditar);
                btnEliminar.setVisible(puedeEditar);
                btnAsignar.setVisible(puedeEditar);
                btnCerrar.setVisible(puedeEditar);
                btnReabrir.setVisible(puedeEditar);
                btnAnalisis.setVisible(puedeEditar);
                btnCrear.setManaged(puedeEditar);
                btnEditar.setManaged(puedeEditar);
                btnEliminar.setManaged(puedeEditar);
                btnAsignar.setManaged(puedeEditar);
                btnCerrar.setManaged(puedeEditar);
                btnReabrir.setManaged(puedeEditar);
                btnAnalisis.setManaged(puedeEditar);

                setGraphic(box);
            }
        });

        colAcciones.setSortable(false);
        colAcciones.setReorderable(false);
        colAcciones.setMinWidth(100); // ajusta a tu gusto
    }


    private void cargarUsuariosCache() {
        try {
            for (Usuario u : usuarioService.findAll()) {
                usuariosCache.put(u.getIdUsuario(), u);
            }
        } catch (SQLException e) {
            alertError("Error cargando usuarios: " + e.getMessage());
        }
    }

    private String resolveNombreUsuario(String id) {
        Usuario u = usuariosCache.get(id);
        return (u == null) ? id : (u.getNombreUsuario() + " (" + u.getUsernameUsuario() + ")");
    }

    private void configurarScopeYBotones() {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRolUsuario());
        boolean isEpi = "EPIDEMIOLOGO".equalsIgnoreCase(currentUser.getRolUsuario());
        boolean isEncu = "ENCUESTADOR".equalsIgnoreCase(currentUser.getRolUsuario());
        boolean isMir   = "MIR_SALUD_PUBLICA".equalsIgnoreCase(currentUser.getRolUsuario()); // NUEVO

        // Visibilidad botones por rol (ajusta según tu TFG si procede)
        boolean puedeEditar = isAdmin || isEpi || isMir;
        btnCrear.setVisible(puedeEditar);
        btnEditar.setVisible(puedeEditar);
        btnEliminar.setVisible(puedeEditar);
        btnAsignar.setVisible(puedeEditar);
        btnCerrar.setVisible(puedeEditar);
        btnReabrir.setVisible(puedeEditar);
        btnAnalisis.setVisible(puedeEditar);
        btnCrear.setManaged(puedeEditar);
        btnEditar.setManaged(puedeEditar);
        btnEliminar.setManaged(puedeEditar);
        btnAsignar.setManaged(puedeEditar);
        btnCerrar.setManaged(puedeEditar);
        btnReabrir.setManaged(puedeEditar);
        btnAnalisis.setManaged(puedeEditar);



        // Ámbitos de filtrado predefinidos
        ObservableList<String> scopes;
        if (isEncu) {
            scopes = FXCollections.observableArrayList(
                    "Asignados a mí",
                    "Asignados activos",
                    "Asignados cerrados"
            );
            cmbScope.setItems(scopes);
            cmbScope.getSelectionModel().select("Asignados a mí");
            cmbScope.setDisable(false);
        } else {
            scopes = FXCollections.observableArrayList(
                    "Todos",
                    "Activos",
                    "Cerrados",
                    "Creados por mí",
                    "Responsable yo"
            );
            cmbScope.setItems(scopes);
            cmbScope.getSelectionModel().select("Todos");
            cmbScope.setDisable(false);
        }
        cmbScope.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refrescarFiltroCombinado());
    }

    private void cargarTabla() {
        try {
            List<Brote> lista = broteService.brotesVisiblesPara(currentUser);
            filtered = new FilteredList<>(FXCollections.observableArrayList(lista), s -> true);
            tablaBrotes.setItems(filtered);
        } catch (SQLException e) {
            alertError("Error cargando brotes: " + e.getMessage());
        }
    }

    private void configurarFiltro() {
        txtFiltro.textProperty().addListener((obs, old, val) -> refrescarFiltroCombinado());
    }

    private void refrescarFiltroCombinado() {
        String q     = Optional.ofNullable(txtFiltro.getText()).orElse("").trim().toLowerCase(Locale.ROOT);
        String scope = Optional.ofNullable(cmbScope.getValue()).orElse("Todos");

        // ¿Necesitamos cargar asignaciones? (cualquier opción que empiece por "Asignados")
        final boolean scopeAsignados = scope.startsWith("Asignados");
        final Set<String> brotesAsignados;
        if (scopeAsignados) {
            try {
                brotesAsignados = broteEncuestadorService.findByUsuarioId(currentUser.getIdUsuario())
                        .stream()
                        .map(BroteEncuestador::getIdBrote)
                        .collect(Collectors.toSet());
            } catch (SQLException e) {
                alertError("No se pudieron cargar asignaciones: " + e.getMessage());
                return; // evita filtrar con set incompleto
            }
        } else {
            brotesAsignados = Set.of();
        }

        // Estado requerido según la opción elegida
        final boolean reqActivos  =
                scope.equals("Activos") || scope.equals("Asignados activos");
        final boolean reqCerrados =
                scope.equals("Cerrados") || scope.equals("Asignados cerrados");

        Predicate<Brote> p = b -> {
            // --- Texto ---
            boolean matchTexto = q.isBlank()
                    || b.getNombreBrote().toLowerCase(Locale.ROOT).contains(q)
                    || b.getFechIniBrote().toLowerCase(Locale.ROOT).contains(q)
                    || resolveNombreUsuario(b.getResponsableBrote()).toLowerCase(Locale.ROOT).contains(q)
                    || resolveNombreUsuario(b.getCreadorBrote()).toLowerCase(Locale.ROOT).contains(q);

            // --- Scope propietario/asignación ---
            boolean matchScope = switch (scope) {
                case "Creados por mí"      -> b.getCreadorBrote().equals(currentUser.getIdUsuario());
                case "Responsable yo"      -> b.getResponsableBrote().equals(currentUser.getIdUsuario());
                case "Asignados a mí",
                    "Asignados activos",
                    "Asignados cerrados" -> brotesAsignados.contains(b.getIdBrote());
                default                    -> true; // "Todos", "Activos", "Cerrados"
            };

            // --- Estado ---
            boolean isCerrado = estaCerrado(b);
            boolean matchEstado =
                    (reqActivos  && !isCerrado) ||
                            (reqCerrados &&  isCerrado) ||
                            (!reqActivos && !reqCerrados); // "Todos", "Creados por mí", "Responsable yo", "Asignados a mí"

            return matchTexto && matchScope && matchEstado;
        };

        if (filtered != null) filtered.setPredicate(p);
    }


    // ------------------- Acciones -------------------

    @FXML
    private void handleCrear() {
        if (!puedeEditar()) return;
        abrirDialogoBrote(null);
    }

    @FXML
    private void handleEditar() {
        if (!puedeEditar()) return;
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }
        if ("CERRADO".equalsIgnoreCase(sel.getEstadoBrote())) {
            alertInfo("Este brote está cerrado. No se pueden editar. Ábrelo primero");
            return;
        }
        abrirDialogoBrote(sel);
    }

    @FXML
    private void handleEliminar() {
        if (!puedeEditar()) return;
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }
        if ("CERRADO".equalsIgnoreCase(sel.getEstadoBrote())) {
            alertInfo("Este brote está cerrado. No se pueden gestionar encuestadores.");
            return;
        }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el brote '" + sel.getNombreBrote() + "'?",
                ButtonType.OK, ButtonType.CANCEL);
        conf.setHeaderText("Confirmar eliminación");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    broteService.delete(sel.getIdBrote());
                    cargarTabla();
                    refrescarFiltroCombinado();
                } catch (SQLException e) {
                    alertError("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleAsignar() {
        if (!puedeEditar()) return;
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }
        if ("CERRADO".equalsIgnoreCase(sel.getEstadoBrote())) {
            alertInfo("Este brote está cerrado. No se pueden gestionar encuestadores.");
            return;
        }
        abrirDialogoEncuestadores(sel);
    }

    private boolean puedeEditar() {
        String r = normalizaRol(currentUser.getRolUsuario());
        return "EPIDEMIOLOGO".equals(r) || "MIR_SALUD_PUBLICA".equals(r) || "ADMIN".equals(r);
    }

    @FXML
    private void handleCerrarBrote() {
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }
        if ("CERRADO".equalsIgnoreCase(sel.getEstadoBrote())) { alertInfo("El brote ya está cerrado."); return; }

        TextInputDialog d = new TextInputDialog(java.time.LocalDate.now().toString());
        d.setHeaderText("Fecha de cierre (YYYY-MM-DD)");
        d.setContentText("Fecha:");
        d.showAndWait().ifPresent(fecha -> {
            try {
                broteService.cerrarBrote(sel.getIdBrote(), fecha.trim());
                cargarTabla();
                refrescarFiltroCombinado();
            } catch (Exception e) {
                alertError("No se pudo cerrar: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleReabrirBrote() {
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }
        if ("ACTIVO".equalsIgnoreCase(sel.getEstadoBrote())) { alertInfo("El brote ya está activo."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Reabrir el brote '" + sel.getNombreBrote() + "'?", ButtonType.OK, ButtonType.CANCEL);
        conf.setHeaderText("Confirmar reapertura");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    broteService.reabrirBrote(sel.getIdBrote());
                    cargarTabla();
                    refrescarFiltroCombinado();
                } catch (Exception e) {
                    alertError("No se pudo reabrir: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleExpuestos() {
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }
        if ( mainController != null) {
            mainController.showExpuestos(sel);
        }
    }

    @FXML
    private void handleAnalisis() {
        Brote sel = tablaBrotes.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un brote."); return; }

        try {
                        FXMLLoader fx = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/estadistica.fxml"));
            Parent root = fx.load();
            EstadisticaController ctrl = fx.getController();
            // Firma típica (ajústala a la tuya si difiere):
            ctrl.init(currentUser, sel, estadisticaService, informeService, broteEncuestadorService);

            Stage dlg = new Stage();
            dlg.setTitle("Análisis — " + (sel.getNombreBrote() == null ? sel.getIdBrote() : sel.getNombreBrote()));
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setScene(new Scene(root));
            dlg.showAndWait();

        } catch (Exception e) {
            error("No se pudo abrir Análisis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }


    private static String normalizaRol(String r) {
        if (r == null) return "";
        String s = java.text.Normalizer.normalize(r, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return s.toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    private void abrirDialogoBrote(Brote existente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/brote_form.fxml"));
            Parent root = loader.load();
            BroteFormController form = loader.getController();
            form.init(existente, currentUser, broteService, usuarioService);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(tablaBrotes.getScene().getWindow());
            dialog.setTitle(existente == null ? "Nuevo brote" : "Editar brote");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            cargarTabla();
            refrescarFiltroCombinado();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Error abriendo formulario: " + e.getMessage());
        }
    }

    private void abrirDialogoEncuestadores(Brote brote) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/brote_encuestadores.fxml"));
            Parent root = loader.load();
            BroteEncuestadoresController ctl = loader.getController();
            ctl.init(brote, usuarioService, broteEncuestadorService);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(tablaBrotes.getScene().getWindow());
            dialog.setTitle("Asignar encuestadores");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            alertError("Error abriendo asignación: " + e.getMessage());
        }
    }

    private static boolean estaCerrado(Brote b) {
        String e = b.getEstadoBrote();
        return e != null && "CERRADO".equalsIgnoreCase(e);
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void alertError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }


    private FontIcon mdi(String literal) {
        FontIcon i = new FontIcon(literal);
        i.setIconSize(16);
        i.setIconColor(Color.WHITE);
        return i;
    }
}