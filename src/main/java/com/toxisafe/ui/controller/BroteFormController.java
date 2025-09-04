package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.BroteService;
import com.toxisafe.service.UsuarioService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class BroteFormController {

    @FXML private TextField txtNombre;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Usuario> cmbResponsable;
    @FXML private Label lblErrores;

    private Usuario currentUser;
    private BroteService broteService;
    private UsuarioService usuarioService;
    private Brote existente; // null si es alta

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void init(Brote existente, Usuario currentUser, BroteService broteService, UsuarioService usuarioService) {
        this.existente = existente;
        this.currentUser = currentUser;
        this.broteService = broteService;
        this.usuarioService = usuarioService;

        dpFecha.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(java.time.LocalDate.now())); // no permitir futuro
            }
        });

        cargarResponsables();
        if (existente != null) {
            txtNombre.setText(existente.getNombreBrote());
            try {
                dpFecha.setValue(LocalDate.parse(existente.getFechIniBrote(), DF));
            } catch (Exception ignore) { }
            preseleccionarResponsable(existente.getResponsableBrote());
        }
    }

    private void cargarResponsables() {
        try {
            List<Usuario> todos = usuarioService.findAll();
            // Según RF, responsable puede ser ADMIN o EPIDEMIOLOGO. Ajusta si procede.
            List<Usuario> candidatos = todos.stream()
                    .filter(u -> {
                        String r = u.getRolUsuario();
                        return "ADMIN".equalsIgnoreCase(r) || "EPIDEMIOLOGO".equalsIgnoreCase(r) || "MIR_SALUD_PUBLICA".equalsIgnoreCase(r);
                    })
                    .collect(Collectors.toList());

            cmbResponsable.getItems().setAll(candidatos);
            cmbResponsable.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(Usuario item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNombreUsuario() + " (" + item.getUsernameUsuario() + ")");
                }
            });
            cmbResponsable.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Usuario item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNombreUsuario() + " (" + item.getUsernameUsuario() + ")");
                }
            });
        } catch (SQLException e) {
            show("No se pudieron cargar usuarios: " + e.getMessage());
        }
    }

    private void preseleccionarResponsable(String idUsuario) {
        if (idUsuario == null) return;
        for (Usuario u : cmbResponsable.getItems()) {
            if (idUsuario.equals(u.getIdUsuario())) {
                cmbResponsable.getSelectionModel().select(u);
                break;
            }
        }
    }

    @FXML
    private void guardar() {
        String nombre = safe(txtNombre.getText());
        LocalDate fecha = dpFecha.getValue();
        Usuario responsable = cmbResponsable.getValue();

        String err = validar(nombre, fecha, responsable);
        if (err != null) { lblErrores.setText(err); return; }
        lblErrores.setText("");

        try {
            if (existente == null) {
                // ALTA: usar el servicio tal y como está definido
                broteService.create(
                        currentUser.getIdUsuario(),
                        responsable.getIdUsuario(),
                        fecha.format(DF),
                        nombre
                );
            } else {
                // EDICIÓN: si tu servicio tiene update(Brote), esto vale
                existente.setNombreBrote(nombre);
                existente.setFechIniBrote(fecha.format(DF));
                existente.setResponsableBrote(responsable.getIdUsuario());
                broteService.update(existente);

                // Si en tu servicio NO existe update(Brote) y tienes, por ejemplo,
                // update(String id, String responsableId, String fecha, String nombre),
                // cambia el bloque por:
                // broteService.update(existente.getIdBrote(), responsable.getIdUsuario(), fecha.format(DF), nombre);
            }
            close();
        } catch (IllegalArgumentException iae) {
            // Mensajes de validación que lanza el servicio (rol, fecha, etc.)
            lblErrores.setText(iae.getMessage());
        } catch (SQLException e) {
            show("Error guardando: " + e.getMessage());
        }
    }

    @FXML private void cancelar() { close(); }

    private String validar(String nombre, LocalDate fecha, Usuario responsable) {
        if (nombre.isBlank()) return "El nombre es obligatorio.";
        if (fecha == null) return "La fecha de inicio es obligatoria.";
        if (fecha.isAfter(LocalDate.now())) return "La fecha no puede ser posterior a hoy.";
        if (responsable == null) return "Debes seleccionar un responsable.";
        return null;
    }

    private void show(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void close() { txtNombre.getScene().getWindow().hide(); }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
