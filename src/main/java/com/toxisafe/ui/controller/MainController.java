package com.toxisafe.ui.controller;

import com.toxisafe.app.MainApplication;
import com.toxisafe.dao.BroteDao;
import com.toxisafe.dao.PersonaExpuestaDao;
import com.toxisafe.dao.impl.BroteDaoImpl;
import com.toxisafe.dao.impl.PersonaExpuestaDaoImpl;
import com.toxisafe.model.Brote;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.*;
import com.toxisafe.util.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;


import java.sql.Connection;
import java.sql.SQLException;

public class MainController {
    @FXML private Label lblUsuario;
    @FXML private AnchorPane contentArea;
    @FXML private Button btnUsuarios;
    // Servicios compartidos por el módulo
    private BroteService broteService;
    private BroteEncuestadorService broteEncuestadorService;
    private Usuario currentUser;
    private UsuarioService usuarioService;
    private PersonaExpuestaService personaExpuestaService;

    // NUEVOS (ingestas/alimentos)
    private IngestaService ingestaService;
    private AlimentoService alimentoService;

    private SintomaService sintomaService;
    private GrupoSintomaService grupoSintomaService;
    private SintomasGeneralesExpuestoService sintomasGeneralesExpuestoService;
    private InformeService informeService;
    private EstadisticaService estadisticaService;



    /** Llamado desde LoginController */
    public void setContext(Usuario user, UsuarioService service) throws SQLException {
        this.currentUser = user;
        this.usuarioService = service;
        lblUsuario.setText("Bienvenido, " + currentUser.getNombreUsuario() + " (" + currentUser.getRolUsuario() + ")");

        boolean esAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRolUsuario());
        btnUsuarios.setVisible(esAdmin);
        btnUsuarios.setManaged(esAdmin);

        // Inicializa solo una vez
        if (this.broteService == null) {
            Connection conn = DBConnection.getConnection();

            // --- DAOs ---
            com.toxisafe.dao.BroteDao broteDao = new com.toxisafe.dao.impl.BroteDaoImpl(conn);
            com.toxisafe.dao.BroteEncuestadorDao broteEncDao = new com.toxisafe.dao.impl.BroteEncuestadorDaoImpl(conn);
            com.toxisafe.dao.PersonaExpuestaDao personaExpDao = new com.toxisafe.dao.impl.PersonaExpuestaDaoImpl(conn);
            com.toxisafe.dao.IngestaDao ingestaDao = new com.toxisafe.dao.impl.IngestaDaoImpl(conn);
            com.toxisafe.dao.IngestaPersonaExpuestaDao linkDao = new com.toxisafe.dao.impl.IngestaPersonaExpuestaDaoImpl(conn);
            com.toxisafe.dao.AlimentoDao alimentoDao = new com.toxisafe.dao.impl.AlimentoDaoImpl(conn);
            com.toxisafe.dao.UsuarioDao usuarioDao = new com.toxisafe.dao.impl.UsuarioDaoImpl(conn);
            com.toxisafe.dao.AlimentoCatalogoDao alimentoCatalogoDao = new com.toxisafe.dao.impl.AlimentoCatalogoDaoImpl(conn);
            com.toxisafe.dao.AlimentoCatalogoAliasDao alimentoCatalogoAliasDao = new com.toxisafe.dao.impl.AlimentoCatalogoAliasDaoImpl(conn);
            com.toxisafe.dao.GrupoSintomaDao grupoDao = new com.toxisafe.dao.impl.GrupoSintomaDaoImpl(conn);
            com.toxisafe.dao.SintomaDao sintomaDao = new com.toxisafe.dao.impl.SintomaDaoImpl(conn);
            com.toxisafe.dao.SintomasGeneralesExpuestoDao genDao = new com.toxisafe.dao.impl.SintomasGeneralesExpuestoDaoImpl(conn);
            com.toxisafe.dao.ExposicionSintomaDao expDao = new com.toxisafe.dao.impl.ExposicionSintomaDaoImpl(conn);
            com.toxisafe.dao.InformeDao informeDao = new com.toxisafe.dao.impl.InformeDaoImpl(conn);

            // --- Services base (ORDEN IMPORTANTE) ---
            this.broteEncuestadorService = new com.toxisafe.service.BroteEncuestadorService(broteEncDao, broteDao, usuarioDao);

            // Asegura que no es null ANTES de usarlo
            if (this.broteEncuestadorService == null) {
                throw new IllegalStateException("DI: broteEncuestadorService no inicializado");
            }

            this.broteService = new com.toxisafe.service.BroteService(
                    broteDao,
                    broteEncDao,
                    usuarioDao
            );

            this.personaExpuestaService = new com.toxisafe.service.PersonaExpuestaService(
                    personaExpDao,
                    broteDao,
                    this.broteEncuestadorService
            );

            // ⬇️ Aquí fallaba: pásale SIEMPRE el service ya construido
            this.ingestaService = new com.toxisafe.service.IngestaService(
                    ingestaDao, linkDao, personaExpDao, broteDao,
                    this.broteEncuestadorService
            );

            this.alimentoService = new com.toxisafe.service.AlimentoService(
                    alimentoDao,
                    ingestaId -> this.ingestaService.broteIdDeIngesta(ingestaId),
                    broteDao,
                    this.broteEncuestadorService,
                    alimentoCatalogoDao,
                    alimentoCatalogoAliasDao
            );

            this.sintomaService = new com.toxisafe.service.SintomaService(
                    sintomaDao,
                    grupoDao
            );

            this.grupoSintomaService = new com.toxisafe.service.GrupoSintomaService(
                    grupoDao,
                    sintomaDao
            );

            this.sintomasGeneralesExpuestoService = new com.toxisafe.service.SintomasGeneralesExpuestoService(
                    genDao,
                    expDao,
                    sintomaDao,
                    personaExpDao,
                    broteDao,
                    broteEncuestadorService
            );

            this.informeService = new com.toxisafe.service.InformeService(
                    informeDao,
                    broteDao,
                    broteEncuestadorService
            );

            this.estadisticaService = new com.toxisafe.service.EstadisticaService(
                    personaExpuestaService,
                    sintomasGeneralesExpuestoService,
                    ingestaService,
                    alimentoService,
                    sintomaService
            );

            // Diagnóstico inequívoco (usa "this." para evitar sombras)
            System.out.println("[DI] this.broteEncuestadorService=" + (this.broteEncuestadorService != null));
            System.out.println("[DI] this.ingestaService=" + (this.ingestaService != null));
        }

        showDashboard();
    }


    public void setBroteServices(BroteService broteService, BroteEncuestadorService broteEncuestadorService) {
        this.broteService = broteService;
        this.broteEncuestadorService = broteEncuestadorService;
    }

    private void ensurePersonaExpuestaService() throws SQLException {
        if (personaExpuestaService == null) {
            // Reutiliza la conexión singleton de DBConnection
            Connection conn = DBConnection.getConnection();

            // DAO de expuestos
            PersonaExpuestaDao peDao = new PersonaExpuestaDaoImpl(conn);

            // Necesitamos un BroteDao para el service (no accedemos al del BroteService)
            BroteDao bDao = new BroteDaoImpl(conn);

            // Usa el BroteEncuestadorService que ya tienes (para chequear asignaciones)
            personaExpuestaService = new PersonaExpuestaService(peDao, bDao, broteEncuestadorService);
        }
    }
    private void loadViewSimple(String fxml) {
        try {
            AnchorPane view = FXMLLoader.load(getClass().getResource(fxml));
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void showDashboard() {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/dashboard.fxml"));
            Parent view = fx.load();
            DashboardController ctrl = fx.getController();
            ctrl.init(currentUser, broteService, broteEncuestadorService, personaExpuestaService, usuarioService);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            // muestra tu util de error si la tienes
        }
    }


    @FXML
    private void showUsuarios() {
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRolUsuario())) {
            new Alert(Alert.AlertType.WARNING, "No tiene permisos para gestionar usuarios.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/usuario.fxml"));
            Parent view = loader.load();
            var controller = loader.getController();
            ((com.toxisafe.ui.controller.UsuarioController) controller)
                    .setContext(currentUser, usuarioService);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir Usuarios: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void showBrotes() {
        if (broteService == null || broteEncuestadorService == null) {
            new Alert(Alert.AlertType.ERROR, "Servicios de brotes no inicializados. Llama a setContext(...) con BroteService y BroteEncuestadorService.").showAndWait();
            return;
        }
        try {
            ensurePersonaExpuestaService();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/brote.fxml"));
            javafx.scene.layout.BorderPane view = loader.load();
            BroteController controller = loader.getController();
            controller.setContext(currentUser, broteService, usuarioService, broteEncuestadorService);
            controller.setPersonaExpuestaService(personaExpuestaService);
            controller.setIngestaServices(ingestaService, alimentoService, broteService);
            controller.setSintomasController(sintomaService, grupoSintomaService, sintomasGeneralesExpuestoService);
            controller.setAnalisis(informeService, estadisticaService);
            controller.setMainController(this);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir Brotes: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void showExpuestos(Brote brote) {
        if (personaExpuestaService == null || broteEncuestadorService == null) {
            new Alert(Alert.AlertType.ERROR, "Servicios de expuestos no inicializados.").showAndWait();
            return;
        }
        try {
            ensurePersonaExpuestaService();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/expuesto.fxml"));
            javafx.scene.layout.BorderPane view = loader.load();
            ExpuestoController ctl = loader.getController();
            ctl.setContext(currentUser, brote, personaExpuestaService);
            ctl.setIngestaDeps(ingestaService, alimentoService, broteService);
            ctl.setSintomasController(sintomaService, grupoSintomaService, sintomasGeneralesExpuestoService);
            ctl.setMainController(this);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir Expuestos: " + e.getMessage()).showAndWait();
        }
    }


    @FXML private void showConfig() { loadViewSimple("/com/toxisafe/ui/view/config.fxml"); }

    @FXML
    private void logout(javafx.event.ActionEvent e) {
        try {
            // 1) Opcional: limpia el estado de sesión si tienes algo así
            //SessionManager.getInstance().clear();

            // 2) Carga el login
            javafx.stage.Stage stage =
                    (javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow();

            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/login.fxml"));
            javafx.scene.Parent loginRoot = loader.load();
            com.toxisafe.ui.controller.LoginController loginController = loader.getController();
            // Pasa la MISMA instancia que ya usas en la app (o crea una si no la tienes a mano)
            loginController.setUsuarioService(this.usuarioService);

            // Mantener la misma Scene (conserva tamaño, css, etc.)
            stage.getScene().setRoot(loginRoot);
            stage.centerOnScreen();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "No se pudo abrir la pantalla de login."
            ).showAndWait();
        }
    }
}