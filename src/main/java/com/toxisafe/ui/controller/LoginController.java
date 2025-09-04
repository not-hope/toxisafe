package com.toxisafe.ui.controller;

import com.toxisafe.app.MainApplication;
import com.toxisafe.dao.BroteDao;
import com.toxisafe.dao.BroteEncuestadorDao;
import com.toxisafe.dao.UsuarioDao;
import com.toxisafe.dao.impl.BroteDaoImpl;
import com.toxisafe.dao.impl.BroteEncuestadorDaoImpl;
import com.toxisafe.dao.impl.UsuarioDaoImpl;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.UsuarioService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    @FXML
    private ImageView logoImageView;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblError;

    private UsuarioService usuarioService;


    public void setUsuarioService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        lblError.setVisible(false);

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Por favor, rellene todos los campos.");
            lblError.setVisible(true);
            return;
        }

        try {
            var usuarioOpt = usuarioService.autenticarUsuario(username, password);
            if (usuarioOpt.isPresent()) {
                openMainWindow(usuarioOpt.get()); // <- pasa el objeto Usuario
            } else {
                lblError.setText("Usuario o contraseña incorrectos.");
                lblError.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblError.setText("Error al conectar con la base de datos.");
            lblError.setVisible(true);
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    private void openMainWindow(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/main.fxml"));
            javafx.scene.Parent root = loader.load();

            com.toxisafe.ui.controller.MainController main = loader.getController();
            main.setContext(usuario, usuarioService);

            java.sql.Connection conn = com.toxisafe.util.DBConnection.getConnection();

            com.toxisafe.dao.BroteDao bDao = new com.toxisafe.dao.impl.BroteDaoImpl(conn);
            com.toxisafe.dao.BroteEncuestadorDao beDao = new com.toxisafe.dao.impl.BroteEncuestadorDaoImpl(conn);
            com.toxisafe.dao.UsuarioDao uDao = new com.toxisafe.dao.impl.UsuarioDaoImpl(conn);

            com.toxisafe.service.BroteService broteService =
                    new com.toxisafe.service.BroteService(bDao, beDao, uDao);
            com.toxisafe.service.BroteEncuestadorService broteEncuestadorService =
                    new com.toxisafe.service.BroteEncuestadorService(beDao, bDao, uDao);

            main.setBroteServices(broteService, broteEncuestadorService);

            Stage stage = new Stage();
            stage.setMaximized(true);
            stage.setScene(new Scene(root));
            stage.setTitle("TOXISAFE - Panel Principal");
            stage.show();

            txtUsername.getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error cargando la ventana principal.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



}
