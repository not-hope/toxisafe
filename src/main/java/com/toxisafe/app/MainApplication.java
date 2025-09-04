package com.toxisafe.app;

import com.toxisafe.dao.*;
import com.toxisafe.dao.impl.*; // si usas *Impl concretas
import com.toxisafe.service.*;
import com.toxisafe.sync.SyncEmitter;
import com.toxisafe.sync.util.SyncService;
import com.toxisafe.ui.controller.LoginController;
import com.toxisafe.util.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Optional;

public class MainApplication extends Application {

    private Connection connection;

    // DAOs (ajusta a tus nombres/paquetes reales)
    private BroteDao broteDao;
    private PersonaExpuestaDao personaExpuestaDao;
    private IngestaDao ingestaDao;
    private AlimentoDao alimentoDao;
    private IngestaPersonaExpuestaDao ingestaPersonaExpuestaDao;
    private SintomasGeneralesExpuestoDao sintomasGeneralesDao;
    private ExposicionSintomaDao exposicionSintomaDao;
    private BroteEncuestadorDao broteEncuestadorDao;
    private InformeDao informeDao;
    private AlimentoCatalogoAliasDao alimentoCatalogoAliasDao;
    private AlimentoCatalogoDao alimentoCatalogoDao;


    // Services
    private BroteService broteService;
    private PersonaExpuestaService personaExpuestaService;
    private IngestaService ingestaService;
    private AlimentoService alimentoService;
    private SintomasGeneralesExpuestoService sintomasGeneralesExpuestoService;
    private BroteEncuestadorService broteEncuestadorService;
    private InformeService informeService;
    private EstadisticaService estadisticaService;
    private SintomaService sintomaService;
    private SyncEmitter syncEmitter;
    private AlimentoService.IngestaLookup ingestaLookup;

    // Sync
    private SyncService sync;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1) BD
        connection = DBConnection.getConnection();

        // 2) DAOs (usa tus implementaciones reales)
        UsuarioDao usuarioDao = new UsuarioDaoImpl(connection);
        broteDao               = new BroteDaoImpl(connection);
        personaExpuestaDao     = new PersonaExpuestaDaoImpl(connection);
        ingestaDao             = new IngestaDaoImpl(connection);
        alimentoDao            = new AlimentoDaoImpl(connection);
        sintomasGeneralesDao   = new SintomasGeneralesExpuestoDaoImpl(connection);
        exposicionSintomaDao   = new ExposicionSintomaDaoImpl(connection);
        broteEncuestadorDao    = new BroteEncuestadorDaoImpl(connection);
        informeDao             = new InformeDaoImpl(connection);
        SintomaDao sintomaDao = new SintomaDaoImpl(connection);
        alimentoCatalogoAliasDao = new AlimentoCatalogoAliasDaoImpl(connection);
        alimentoCatalogoDao    = new AlimentoCatalogoDaoImpl(connection);
        ingestaPersonaExpuestaDao = new IngestaPersonaExpuestaDaoImpl(connection);
        AlimentoService.IngestaLookup ingestaLookup = (String idIngesta) -> {
            var enlaces = ingestaPersonaExpuestaDao.findByIngestaId(idIngesta);
            if (enlaces.isEmpty()) return Optional.empty();
            String expuestoId = enlaces.get(0).getIdExpuesto();
            return personaExpuestaDao.findById(expuestoId).map(pe -> pe.getIdBrote());
        };


        // 3) Services (con tus constructores reales)
        broteEncuestadorService      = new BroteEncuestadorService(broteEncuestadorDao, broteDao, usuarioDao);
        broteService                 = new BroteService(broteDao, broteEncuestadorDao, usuarioDao, syncEmitter);
        personaExpuestaService       = new PersonaExpuestaService(personaExpuestaDao, broteDao, broteEncuestadorService);
        ingestaService               = new IngestaService(ingestaDao, new IngestaPersonaExpuestaDaoImpl(connection),
                personaExpuestaDao, broteDao, broteEncuestadorService, syncEmitter);
        alimentoService              = new AlimentoService(alimentoDao,ingestaLookup, broteDao, broteEncuestadorService,
                alimentoCatalogoDao, alimentoCatalogoAliasDao, syncEmitter);
        sintomasGeneralesExpuestoService = new SintomasGeneralesExpuestoService(
                sintomasGeneralesDao, exposicionSintomaDao, sintomaDao, personaExpuestaDao, broteDao,
                broteEncuestadorService,syncEmitter);
        informeService               = new InformeService(informeDao, broteDao, broteEncuestadorService, syncEmitter);
        estadisticaService           = new EstadisticaService(personaExpuestaService, sintomasGeneralesExpuestoService,
                ingestaService, alimentoService, sintomaService);
        UsuarioService usuarioService = new UsuarioService(usuarioDao);

        // 4) Sync (emisor + ingestor) — carpeta de sincronización
        Path sharedRoot = resolveSharedRoot(); // %TOXISAFE_SYNC_DIR% o ./sync-shared
        sync = SyncService.createDefault(
                connection, sharedRoot,
                broteDao, personaExpuestaDao, ingestaDao, alimentoDao,
                sintomasGeneralesDao, exposicionSintomaDao,
                broteEncuestadorDao, informeDao, ingestaPersonaExpuestaDao
        );
        // 4.1) Inyección del emitter en los servicios que sincronizan
        SyncEmitter emitter = sync.emitter();
        broteService.setSyncEmitter(emitter);
        personaExpuestaService.setSyncEmitter(emitter);
        ingestaService.setSyncEmitter(emitter);
        alimentoService.setSyncEmitter(emitter);
        sintomasGeneralesExpuestoService.setSyncEmitter(emitter);
        broteEncuestadorService.setSyncEmitter(emitter);
        informeService.setSyncEmitter(emitter);
        // (añade más si los tienes)

        // 4.2) Arrancar ingestión periódica (cada 5–10 s)
        sync.start(7);
        // 5) A partir de aquí, tu flujo normal (login, cargar ventanas, etc.)

        // Carga el FXML de login
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/login.fxml"));
        AnchorPane root = loader.load();

        // Inyecta el servicio al controlador
        LoginController controller = loader.getController();
        controller.setUsuarioService(usuarioService);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("TOXISAFE - Login");
        primaryStage.show();
    }

    private Path resolveSharedRoot() {
        // Prioridad: propiedad JVM -> variable entorno -> carpeta local por defecto
        String prop = System.getProperty("toxisafe.shared.dir");
        if (prop != null && !prop.isBlank()) return Paths.get(prop);
        String env = System.getenv("TOXISAFE_SYNC_DIR");
        if (env != null && !env.isBlank()) return Paths.get(env);
        return Paths.get("sync-shared"); // carpeta local junto a la app (portable)
    }

    @Override
    public void stop() throws Exception {
        if (sync != null) sync.close();
        if (connection != null) connection.close();
        super.stop();
    }

    public static void main(String[] args) {
        // No hace falta inicializar la DB manualmente, DBConnection lo hace
        launch(args);
    }
}


