package com.toxisafe.ui.controller;

import com.toxisafe.model.Alimento;
import com.toxisafe.model.Ingesta;
import com.toxisafe.model.PersonaExpuesta;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.AlimentoService;
import com.toxisafe.service.BroteService;
import com.toxisafe.service.IngestaService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ExpuestoIngestasController {

    // UI
    @FXML private Label lblTitulo;
    @FXML private Label lblEstadoBrote;

    @FXML private TableView<Ingesta> tvIngestas;
    @FXML private TableColumn<Ingesta, String> colFecha;
    @FXML private TableColumn<Ingesta, String> colLugar;

    @FXML private CheckBox chkSospechosa;

    @FXML private ComboBox<String> cmbCatalogo;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtLugar;

    @FXML private Button btnNuevaIngesta, btnEditarIngesta, btnEliminarIngesta;
    @FXML private Button btnGuardar, btnCancelar;

    // Alimentos
    @FXML private TableView<Alimento> tvAlimentos;
    @FXML private TableColumn<Alimento, String> colAlNombre;
    @FXML private TextField txtNuevoAlimento;
    @FXML private Button btnAddAlimento, btnEditAlimento, btnDelAlimento;

    // Estado / servicios
    private Usuario currentUser;
    private PersonaExpuesta expuesto;
    private IngestaService ingestaService;
    private AlimentoService alimentoService;
    private BroteService broteService;

    private final ObservableList<Ingesta> ingestas = FXCollections.observableArrayList();
    private final ObservableList<Alimento> alimentos = FXCollections.observableArrayList();

    // Lista FIJA para el combo del catálogo
    private final ObservableList<String> catalogoItems = FXCollections.observableArrayList();

    private Ingesta enEdicion = null; // null si no estamos editando/creando
    private boolean broteCerrado = false;
    private boolean puedeEscribir = false;

    // control de reentradas
    private volatile boolean updatingSuggestions = false;
    private volatile boolean internalComboChange = false;

    /* ======================== Inyección de contexto ======================== */

    public void init(Usuario usuario,
                     PersonaExpuesta expuesto,
                     IngestaService ingestaService,
                     AlimentoService alimentoService,
                     BroteService broteService) {
        this.currentUser = usuario;
        this.expuesto = expuesto;
        this.ingestaService = ingestaService;
        this.alimentoService = alimentoService;
        this.broteService = broteService;

        String nombre = (expuesto.getNombreExpuesto() == null ? "" : expuesto.getNombreExpuesto());
        String apell  = (expuesto.getApellidoExpuesto() == null ? "" : expuesto.getApellidoExpuesto());
        lblTitulo.setText("Ingestas de: " + nombre + " " + apell);

        refrescarEstadoYPermisos();
        configurarTablas();
        configurarForm();
        cargarIngestas();
        refrescarBotones();
    }

    /* ======================== Setup UI ======================== */

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(c -> Bindings.createStringBinding(c.getValue()::getFechaConsumo));
        colLugar.setCellValueFactory(c -> Bindings.createStringBinding(c.getValue()::getLugarConsumo));
        colAlNombre.setCellValueFactory(c -> Bindings.createStringBinding(c.getValue()::getNombre));

        tvIngestas.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            onSelIngestaChanged(newSel);
        });

        // Combo editable + lista fija
        cmbCatalogo.setEditable(true);
        cmbCatalogo.setItems(catalogoItems);

        // Flag durante cambios de value (selección desde el popup) -> ignora el text listener
        cmbCatalogo.valueProperty().addListener((obs, oldVal, newVal) -> {
            internalComboChange = true;
            Platform.runLater(() -> internalComboChange = false);
        });

        // Carga inicial de sugerencias
        Platform.runLater(() -> recargarSugerenciasCatalogo(""));

        // Listener del editor: solo si el usuario está escribiendo y no estamos en selección interna
        cmbCatalogo.getEditor().textProperty().addListener((obs, old, val) -> {
            if (updatingSuggestions || internalComboChange) return;
            if (!cmbCatalogo.getEditor().isFocused()) return; // evita disparar al rellenar por selección
            String pref = (val == null) ? "" : val.trim();
            recargarSugerenciasCatalogo(pref); // se ejecuta en background y actualiza en FX al final
        });

        tvAlimentos.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refrescarBotones());

        refrescarBotones();
    }

    private void configurarTablas() {
        tvIngestas.setItems(ingestas);
        tvIngestas.setPlaceholder(new Label("Sin ingestas"));
        tvAlimentos.setItems(alimentos);
        tvAlimentos.setPlaceholder(new Label("Sin alimentos"));
    }

    private void configurarForm() {
        dpFecha.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
        setModoEdicion(null);
    }

    /* ======================== Carga de datos ======================== */

    private void cargarIngestas() {
        try {
            List<Ingesta> lista = ingestaService.findByExpuestoVisiblePara(expuesto.getIdExpuesto(), currentUser);
            ingestas.setAll(lista);
            if (!ingestas.isEmpty()) {
                tvIngestas.getSelectionModel().selectFirst();
            } else {
                alimentos.clear();
                chkSospechosa.setSelected(false);
            }
        } catch (SQLException e) {
            error("Error cargando ingestas: " + e.getMessage());
        }
        refrescarBotones();
    }

    private void cargarAlimentosDeIngesta(String ingestaId) {
        try {
            var items = alimentoService.findByIngestaIdVisiblePara(ingestaId, currentUser);
            alimentos.setAll(items);
            if (!items.isEmpty()) {
                tvAlimentos.getSelectionModel().selectFirst();
            } else {
                tvAlimentos.getSelectionModel().clearSelection();
            }
        } catch (SQLException e) {
            error("No se pudieron cargar los alimentos: " + e.getMessage());
            alimentos.clear();
        } finally {
            refrescarBotones();
        }
    }

    private void refrescarSospechosa(String ingestaId) {
        if (ingestaId == null) { chkSospechosa.setSelected(false); return; }
        try {
            var enlaces = ingestaService.enlacesDeIngestaVisiblePara(ingestaId, currentUser);
            boolean isSosp = enlaces.stream()
                    .filter(l -> expuesto.getIdExpuesto().equals(l.getIdExpuesto()))
                    .map(l -> l.getEsSospechosaParaExpuesto() != null && l.getEsSospechosaParaExpuesto() == 1)
                    .findFirst().orElse(false);
            chkSospechosa.setSelected(isSosp);
        } catch (SQLException e) {
            error("Error consultando sospechosa: " + e.getMessage());
        }
    }

    private void onSelIngestaChanged(Ingesta sel) {
        if (sel == null) {
            alimentos.clear();
            chkSospechosa.setSelected(false);
            setModoEdicion(null);
        } else {
            setModoEdicion(null);
            dpFecha.setValue(parseFecha(sel.getFechaConsumo()));
            txtLugar.setText(nullToEmpty(sel.getLugarConsumo()));
            refrescarSospechosa(sel.getIdIngesta());
            cargarAlimentosDeIngesta(sel.getIdIngesta());
        }
        refrescarBotones();
    }

    /* ======================== Acciones Ingesta ======================== */

    @FXML
    private void handleNuevaIngesta() {
        if (!puedeEscribir || broteCerrado) return;
        Ingesta nueva = new Ingesta(UUID.randomUUID().toString(), LocalDate.now().toString(), "");
        setModoEdicion(nueva);
        dpFecha.setValue(LocalDate.now());
        txtLugar.setText("");
        chkSospechosa.setSelected(false);
    }

    @FXML
    private void handleEditarIngesta() {
        Ingesta sel = tvIngestas.getSelectionModel().getSelectedItem();
        if (sel == null || !puedeEscribir || broteCerrado) return;
        setModoEdicion(new Ingesta(sel.getIdIngesta(), sel.getFechaConsumo(), sel.getLugarConsumo()));
        dpFecha.setValue(parseFecha(sel.getFechaConsumo()));
        txtLugar.setText(nullToEmpty(sel.getLugarConsumo()));
        cargarAlimentosDeIngesta(sel.getIdIngesta());
    }

    @FXML
    private void handleEliminarIngesta() {
        Ingesta sel = tvIngestas.getSelectionModel().getSelectedItem();
        if (sel == null || !puedeEscribir || broteCerrado) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la ingesta seleccionada? (también se eliminarán sus enlaces con esta persona)",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    ingestaService.delete(sel.getIdIngesta(), currentUser);
                    cargarIngestas();
                } catch (SQLException | RuntimeException e) {
                    error("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleGuardarIngesta() {
        if (enEdicion == null || !puedeEscribir || broteCerrado) return;

        String fecha = (dpFecha.getValue() == null) ? null : dpFecha.getValue().toString();
        String lugar = txtLugar.getText() == null ? "" : txtLugar.getText().trim();

        enEdicion.setFechaConsumo(fecha);
        enEdicion.setLugarConsumo(lugar);

        String idGuardada = enEdicion.getIdIngesta();
        try {
            boolean esNueva = ingestas.stream().noneMatch(i -> i.getIdIngesta().equals(idGuardada));
            if (esNueva) {
                ingestaService.createWithLink(enEdicion, expuesto.getIdExpuesto(),
                        chkSospechosa.isSelected() ? 1 : 0, currentUser);
            } else {
                ingestaService.update(enEdicion, currentUser);
            }
            setModoEdicion(null);
            cargarIngestas();
            tvIngestas.getItems().stream()
                    .filter(i -> i.getIdIngesta().equals(idGuardada))
                    .findFirst()
                    .ifPresent(i -> tvIngestas.getSelectionModel().select(i));
        } catch (SQLException | IllegalArgumentException e) {
            error("No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelarEdicion() {
        setModoEdicion(null);
        onSelIngestaChanged(tvIngestas.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleToggleSospechosa() {
        Ingesta sel = tvIngestas.getSelectionModel().getSelectedItem();
        if (sel == null) { chkSospechosa.setSelected(false); return; }
        if (!puedeEscribir || broteCerrado) {
            refrescarSospechosa(sel.getIdIngesta());
            return;
        }
        try {
            ingestaService.marcarSospechosa(sel.getIdIngesta(), expuesto.getIdExpuesto(),
                    chkSospechosa.isSelected(), currentUser);
        } catch (SQLException | RuntimeException e) {
            error("No se pudo actualizar 'sospechosa': " + e.getMessage());
            refrescarSospechosa(sel.getIdIngesta());
        }
    }

    /* ======================== Acciones Alimento ======================== */

    @FXML
    private void handleAddAlimento() {
        Ingesta sel = tvIngestas.getSelectionModel().getSelectedItem();
        if (sel == null || !puedeEscribir || broteCerrado) return;

        try {
            String nombre = null;

            if (cmbCatalogo != null && cmbCatalogo.getEditor() != null) {
                String t = cmbCatalogo.getEditor().getText();
                if (t != null && !t.trim().isEmpty()) nombre = t.trim();
            }
            if ((nombre == null || nombre.isEmpty()) && cmbCatalogo != null) {
                String v = cmbCatalogo.getValue();
                if (v != null && !v.trim().isEmpty()) nombre = v.trim();
            }
            if ((nombre == null || nombre.isEmpty()) && txtNuevoAlimento != null) {
                String v = txtNuevoAlimento.getText();
                if (v != null && !v.trim().isEmpty()) nombre = v.trim();
            }

            if (nombre == null || nombre.isEmpty()) {
                info("Escribe o selecciona un alimento.");
                return;
            }

            Alimento nuevo = new Alimento(null, sel.getIdIngesta(), nombre);
            alimentoService.create(nuevo, currentUser);

            cargarAlimentosDeIngesta(sel.getIdIngesta());

            // Limpieza protegida para no disparar listeners del combo en medio de selección
            internalComboChange = true;
            try {
                if (cmbCatalogo != null) {
                    cmbCatalogo.hide();
                    cmbCatalogo.getSelectionModel().clearSelection();
                    cmbCatalogo.getEditor().clear();
                }
            } finally {
                Platform.runLater(() -> internalComboChange = false);
            }
            if (txtNuevoAlimento != null) txtNuevoAlimento.clear();

            refrescarBotones();
        } catch (SQLException | RuntimeException e) {
            error("No se pudo añadir alimento: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditAlimento() {
        Ingesta selIng = tvIngestas.getSelectionModel().getSelectedItem();
        Alimento sel = tvAlimentos.getSelectionModel().getSelectedItem();
        if (selIng == null || sel == null || !puedeEscribir || broteCerrado) return;

        TextInputDialog d = new TextInputDialog(sel.getNombre());
        d.setHeaderText("Renombrar alimento");
        d.setContentText("Nombre:");
        d.showAndWait().ifPresent(nuevo -> {
            String n = Optional.ofNullable(nuevo).orElse("").trim();
            if (n.isEmpty()) { info("Escribe un nuevo nombre."); return; }
            sel.setNombre(n);
            try {
                alimentoService.update(sel, currentUser);
                cargarAlimentosDeIngesta(selIng.getIdIngesta());
                refrescarBotones();
            } catch (SQLException | RuntimeException e) {
                error("No se pudo renombrar: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelAlimento() {
        Ingesta selIng = tvIngestas.getSelectionModel().getSelectedItem();
        Alimento sel = tvAlimentos.getSelectionModel().getSelectedItem();
        if (selIng == null || sel == null || !puedeEscribir || broteCerrado) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el alimento seleccionado?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    alimentoService.delete(sel.getIdAlimento(), currentUser);
                    cargarAlimentosDeIngesta(selIng.getIdIngesta());
                } catch (SQLException | RuntimeException e) {
                    error("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    /* ======================== Permisos/Estado UI ======================== */

    private void refrescarEstadoYPermisos() {
        String r = safe(currentUser.getRolUsuario());
        boolean isAdmin = r.equals("ADMIN");
        boolean isEpi   = r.equals("EPIDEMIOLOGO");
        boolean isMir   = r.equals("MIR_SALUD_PUBLICA");
        boolean isEncu  = r.equals("ENCUESTADOR");

        puedeEscribir = isAdmin || isEpi || isMir || isEncu;

        broteCerrado = false;
        try {
            var bOpt = broteService.findById(expuesto.getIdBrote());
            if (bOpt.isPresent()) {
                var b = bOpt.get();
                broteCerrado = "CERRADO".equalsIgnoreCase(b.getEstadoBrote());
                lblEstadoBrote.setText("Brote: " + b.getNombreBrote() + " — " + b.getEstadoBrote());
            } else {
                lblEstadoBrote.setText("Brote: (no encontrado)");
            }
        } catch (Exception e) {
            lblEstadoBrote.setText("Brote: (error consultando estado)");
        }

        refrescarBotones();
    }

    private void refrescarBotones() {
        boolean haySelIngesta = tvIngestas.getSelectionModel().getSelectedItem() != null;
        boolean editando = (enEdicion != null);

        btnNuevaIngesta.setDisable(!puedeEscribir || broteCerrado || editando);
        btnEditarIngesta.setDisable(!puedeEscribir || broteCerrado || !haySelIngesta || editando);
        btnEliminarIngesta.setDisable(!puedeEscribir || broteCerrado || !haySelIngesta || editando);

        btnGuardar.setDisable(!puedeEscribir || broteCerrado || !editando);
        btnCancelar.setDisable(!editando);

        chkSospechosa.setDisable(!puedeEscribir || broteCerrado || !haySelIngesta);

        boolean selIngesta = haySelIngesta && !editando;
        boolean haySelAlim = tvAlimentos.getSelectionModel().getSelectedItem() != null;
        btnAddAlimento.setDisable(!puedeEscribir || broteCerrado || !selIngesta);
        btnEditAlimento.setDisable(!puedeEscribir || broteCerrado || !selIngesta || !haySelAlim);
        btnDelAlimento.setDisable(!puedeEscribir || broteCerrado || !selIngesta || !haySelAlim);

        if (txtNuevoAlimento != null) txtNuevoAlimento.setDisable(!puedeEscribir || broteCerrado || !selIngesta);
    }

    private void setModoEdicion(Ingesta obj) {
        this.enEdicion = obj;
        boolean edit = (obj != null);
        dpFecha.setDisable(!edit);
        txtLugar.setDisable(!edit);
        refrescarBotones();
    }

    /* ======================== Utilidades ======================== */

    private static LocalDate parseFecha(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String safe(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }

    /**
     * Recarga de sugerencias: consulta en background y aplica el resultado en el hilo FX.
     * No toca selección ni editor; solo reemplaza el contenido de la lista fija.
     */
    private void recargarSugerenciasCatalogo(String texto) {
        final String pref = (texto == null) ? "" : texto.trim();
        if (updatingSuggestions) return;

        updatingSuggestions = true;
        new Thread(() -> {
            List<String> sugs;
            try {
                sugs = alimentoService.catalogoSugerencias(pref);
            } catch (Exception e) {
                sugs = List.of();
            }
            final List<String> result = sugs;
            Platform.runLater(() -> {
                try {
                    catalogoItems.setAll(result);
                    if (!result.isEmpty() && cmbCatalogo.isFocused() && !cmbCatalogo.isShowing()) {
                        cmbCatalogo.show();
                    }
                } finally {
                    updatingSuggestions = false;
                }
            });
        }, "catalogo-suggest-loader").start();
    }
}
