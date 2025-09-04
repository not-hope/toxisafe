package com.toxisafe.ui.controller;

import com.toxisafe.model.Usuario;
import com.toxisafe.service.UsuarioService;
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
import java.util.function.Predicate;

public class UsuarioController {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsername;
    @FXML private TableColumn<Usuario, String> colRol;

    @FXML private TextField txtFiltro;
    @FXML private Button btnCrear, btnEditar, btnEliminar;

    private Usuario currentUser;
    private UsuarioService usuarioService;

    private FilteredList<Usuario> filtered;

    /** Inyectado desde MainController */
    public void setContext(Usuario currentUser, UsuarioService usuarioService) {
        this.currentUser = currentUser;
        this.usuarioService = usuarioService;
        cargarTabla();

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRolUsuario());
        btnCrear.setVisible(isAdmin);
        btnEditar.setVisible(isAdmin);
        btnEliminar.setVisible(isAdmin);
        btnCrear.setManaged(isAdmin);
        btnEditar.setManaged(isAdmin);
        btnEliminar.setManaged(isAdmin);

        btnEditar.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        btnEliminar.disableProperty().bind(tablaUsuarios.getSelectionModel().selectedItemProperty().isNull());

        cargarTabla();
        configurarFiltro();
    }

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(data -> Bindings.createStringBinding(data.getValue()::getNombreUsuario));
        colUsername.setCellValueFactory(data -> Bindings.createStringBinding(data.getValue()::getUsernameUsuario));
        colRol.setCellValueFactory(data -> Bindings.createStringBinding(data.getValue()::getRolUsuario));
    }

    private void cargarTabla() {
        try {
            List<Usuario> lista = usuarioService.findAll(); // ya existe en tu service
            filtered = new FilteredList<>(FXCollections.observableArrayList(lista), s -> true);
            tablaUsuarios.setItems(filtered);
        } catch (SQLException e) {
            alertError("Error cargando usuarios: " + e.getMessage());
        }
    }

    private void configurarFiltro() {
        txtFiltro.textProperty().addListener((obs, old, val) -> {
            String q = (val == null) ? "" : val.trim().toLowerCase();
            Predicate<Usuario> p = u ->
                    u.getNombreUsuario().toLowerCase().contains(q) ||
                    u.getUsernameUsuario().toLowerCase().contains(q);
            if (filtered != null) filtered.setPredicate(p);
        });
    }

    @FXML
    private void handleCrear() {
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRolUsuario())) return; // guard
        abrirDialogoUsuario(null);
    }

    @FXML
    private void handleEditar() {
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRolUsuario())) return; // guard
        Usuario sel = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un usuario."); return; }
        abrirDialogoUsuario(sel);
    }

    @FXML
    private void handleEliminar() {
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRolUsuario())) return; // guard
        Usuario sel = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) { alertInfo("Seleccione un usuario."); return; }
        if (sel.getIdUsuario().equals(currentUser.getIdUsuario())) {
            alertInfo("No puedes eliminar tu propio usuario mientras estás logueado.");
            return;
        }
        var conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al usuario " + sel.getUsernameUsuario() + "?",
                ButtonType.OK, ButtonType.CANCEL);
        conf.setHeaderText("Confirmar eliminación");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    usuarioService.delete(sel.getIdUsuario());
                    cargarTabla();
                } catch (SQLException e) {
                    alertError("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    /** Crear/Editar con el mismo diálogo (Stage modal) */
    private void abrirDialogoUsuario(Usuario existente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/usuario_form.fxml"));
            Parent root = loader.load();

            UsuarioFormController form = loader.getController();
            form.init(existente, usuarioService); // inyecta

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(tablaUsuarios.getScene().getWindow());
            dialog.setTitle(existente == null ? "Nuevo usuario" : "Editar usuario");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            // refresca tabla tras cerrar
            cargarTabla();

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
}


