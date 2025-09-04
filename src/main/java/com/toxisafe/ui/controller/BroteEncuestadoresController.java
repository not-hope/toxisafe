package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.BroteEncuestador;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.BroteEncuestadorService;
import com.toxisafe.service.UsuarioService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class BroteEncuestadoresController {

    @FXML private Label lblTitulo;
    @FXML private ListView<Usuario> lstDisponibles;
    @FXML private ListView<Usuario> lstAsignados;
    @FXML private Button btnAgregar, btnQuitar;

    private Brote brote;
    private UsuarioService usuarioService;
    private BroteEncuestadorService broteEncuestadorService;

    private final Map<String, Usuario> usuariosById = new HashMap<>();

    public void init(Brote brote, UsuarioService usuarioService, BroteEncuestadorService broteEncuestadorService) {
        this.brote = brote;
        this.usuarioService = usuarioService;
        this.broteEncuestadorService = broteEncuestadorService;

        boolean cerrado = "CERRADO".equalsIgnoreCase(brote.getEstadoBrote());
        btnAgregar.setDisable(cerrado);
        btnQuitar.setDisable(cerrado);

        lblTitulo.setText("Asignar encuestadores — " + brote.getNombreBrote());
        configurarRender();
        cargarListas();
    }

    private void configurarRender() {
        lstDisponibles.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Usuario item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreUsuario() + " (" + item.getUsernameUsuario() + ")");
            }
        });
        lstAsignados.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Usuario item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombreUsuario() + " (" + item.getUsernameUsuario() + ")");
            }
        });
        lstDisponibles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lstAsignados.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void cargarListas() {
        try {
            List<Usuario> todos = usuarioService.findAll();
            List<Usuario> encuestadores = todos.stream()
                    .filter(u -> "ENCUESTADOR".equalsIgnoreCase(u.getRolUsuario()))
                    .collect(Collectors.toList());
            encuestadores.forEach(u -> usuariosById.put(u.getIdUsuario(), u));

            // Asignados actuales
            Set<String> asignadosIds = broteEncuestadorService.asignacionesDe(brote.getIdBrote())
                    .stream().map(BroteEncuestador::getIdUsuario).collect(Collectors.toSet());

            List<Usuario> asignados = encuestadores.stream()
                    .filter(u -> asignadosIds.contains(u.getIdUsuario()))
                    .collect(Collectors.toList());
            List<Usuario> disponibles = encuestadores.stream()
                    .filter(u -> !asignadosIds.contains(u.getIdUsuario()))
                    .collect(Collectors.toList());

            lstDisponibles.setItems(FXCollections.observableArrayList(disponibles));
            lstAsignados.setItems(FXCollections.observableArrayList(asignados));
        } catch (SQLException e) {
            alertError("Error cargando encuestadores: " + e.getMessage());
        }
    }

    @FXML private void agregarSeleccion() {
        moveSelected(lstDisponibles, lstAsignados);
    }
    @FXML private void quitarSeleccion() {
        moveSelected(lstAsignados, lstDisponibles);
    }

    private void moveSelected(ListView<Usuario> from, ListView<Usuario> to) {
        List<Usuario> sel = new ArrayList<>(from.getSelectionModel().getSelectedItems());
        if (sel.isEmpty()) return;
        from.getItems().removeAll(sel);
        to.getItems().addAll(sel);
    }

    @FXML
    private void guardar() {
        try {
            Set<String> nuevosAsignados = lstAsignados.getItems().stream().map(Usuario::getIdUsuario).collect(Collectors.toSet());
            Set<String> actuales = broteEncuestadorService.asignacionesDe(brote.getIdBrote())
                    .stream().map(BroteEncuestador::getIdUsuario).collect(Collectors.toSet());

            // Altas
            for (String uId : nuevosAsignados) {
                if (!actuales.contains(uId)) {
                    broteEncuestadorService.asignarEncuestador(brote.getIdBrote(), uId);
                }
            }
            // Bajas
            for (String uId : actuales) {
                if (!nuevosAsignados.contains(uId)) {
                    broteEncuestadorService.eliminarAsignacion(brote.getIdBrote(), uId);
                }
            }
            cerrar();
        } catch (SQLException e) {
            alertError("Error guardando asignaciones: " + e.getMessage());
        }
    }

    @FXML private void cerrar() { lstDisponibles.getScene().getWindow().hide(); }

    private void alertError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }
}

