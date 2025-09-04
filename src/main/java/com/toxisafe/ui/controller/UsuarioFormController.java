package com.toxisafe.ui.controller;

import com.toxisafe.model.Usuario;
import com.toxisafe.service.UsuarioService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class UsuarioFormController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRol;
    @FXML private Label lblUserCheck; // etiqueta para mostrar disponibilidad

    private UsuarioService usuarioService;
    private Usuario existente; // null si es alta

    // Reglas de validación
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 32;
    private static final Pattern USERNAME_RX = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int PASSWORD_MIN = 6;

    /** Llamado desde UsuarioController tras cargar el FXML */
    public void init(Usuario existente, UsuarioService service) {
        this.existente = existente;
        this.usuarioService = service;

        // Ajusta los roles si tu TFG usa otros nombres
        cmbRol.getItems().setAll(usuarioService.rolesPermitidos());

        if (existente != null) {
            txtNombre.setText(existente.getNombreUsuario());
            txtUsername.setText(existente.getUsernameUsuario());
            txtPassword.setText("");
            cmbRol.getSelectionModel().select(existente.getRolUsuario());
            txtUsername.setDisable(true); // no cambiar username en edición
            lblUserCheck.setText(""); // sin validación en edición
        } else {
            cmbRol.getSelectionModel().select("ENCUESTADOR");

            // Restringir caracteres desde la escritura
            txtUsername.setTextFormatter(new TextFormatter<>(change -> {
                if (change.getText().matches("[a-zA-Z0-9._-]*")) {
                    return change;
                }
                return null; // bloquea caracteres inválidos
            }));

            // Comprobar en tiempo real si el username existe
            txtUsername.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() >= USERNAME_MIN) {
                    Platform.runLater(() -> {
                        try {
                            boolean exists = usuarioService.existsUsername(newVal);
                            if (exists) {
                                lblUserCheck.setStyle("-fx-text-fill: red;");
                                lblUserCheck.setText("Usuario no disponible");
                            } else {
                                lblUserCheck.setStyle("-fx-text-fill: green;");
                                lblUserCheck.setText("Usuario disponible");
                            }
                        } catch (SQLException e) {
                            lblUserCheck.setStyle("-fx-text-fill: orange;");
                            lblUserCheck.setText("Error al verificar");
                        }
                    });
                } else {
                    lblUserCheck.setText("");
                }
            });
        }
    }

    @FXML
    private void guardar() {
        String nombre = safe(txtNombre.getText());
        String username = safe(txtUsername.getText());
        String pass = txtPassword.getText() == null ? "" : txtPassword.getText();
        String rol = cmbRol.getValue();

        String err = validarCamposBasicos(nombre, username, rol);
        if (err != null) { show(err); return; }

        boolean esAlta = (existente == null);
        if (esAlta) {
            String errPass = validarPasswordAlta(pass);
            if (errPass != null) { show(errPass); return; }

            try {
                usuarioService.registrarUsuario(nombre, rol, username, pass);
                close();
            } catch (IllegalArgumentException iae) {
                show(iae.getMessage());
            } catch (SQLException ex) {
                show("Error guardando: " + ex.getMessage());
            }

        } else {
            existente.setNombreUsuario(nombre);
            existente.setRolUsuario(rol);
            existente.setUsernameUsuario(username);

            if (!pass.isBlank()) {
                String errPass = validarPasswordEdicion(pass);
                if (errPass != null) { show(errPass); return; }
                existente.setPasswordUsuario(pass);
            }

            try {
                usuarioService.update(existente);
                close();
            } catch (IllegalArgumentException iae) {
                show(iae.getMessage());
            } catch (SQLException ex) {
                show("Error guardando: " + ex.getMessage());
            }
        }
    }

    @FXML private void cancelar() { close(); }

    // --------- Validaciones ---------

    private String validarCamposBasicos(String nombre, String username, String rol) {
        if (nombre.isEmpty()) return "El nombre es obligatorio.";
        if (username.isEmpty()) return "El usuario es obligatorio.";
        if (rol == null || rol.isBlank()) return "Debes seleccionar un rol.";

        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            return "El usuario debe tener entre " + USERNAME_MIN + " y " + USERNAME_MAX + " caracteres.";
        }
        if (!USERNAME_RX.matcher(username).matches()) {
            return "El usuario solo puede contener letras, números, '.', '_' o '-'.";
        }
        return null;
    }

    private String validarPasswordAlta(String pass) {
        if (pass.isBlank()) return "La contraseña es obligatoria.";
        if (pass.length() < PASSWORD_MIN) {
            return "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres.";
        }
        return null;
    }

    private String validarPasswordEdicion(String pass) {
        if (pass.length() < PASSWORD_MIN) {
            return "La nueva contraseña debe tener al menos " + PASSWORD_MIN + " caracteres.";
        }
        return null;
    }

    // --------- util ---------

    private String safe(String s) { return (s == null) ? "" : s.trim(); }

    private void show(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void close() {
        txtNombre.getScene().getWindow().hide();
    }
}
