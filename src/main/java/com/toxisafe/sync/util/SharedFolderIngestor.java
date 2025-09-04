package com.toxisafe.sync.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toxisafe.dao.*;
import com.toxisafe.model.*;
import com.toxisafe.sync.SyncChange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Módulo de sincronización por carpeta compartida (RF7.*).
 * - La capa de servicios llama a emitInsert/emitUpdate/emitDelete tras persistir en BD (RF7.2/RF7.3).
 * - Un planificador escanea periódicamente la carpeta, consume cambios ajenos y los aplica (RF7.4).
 * - Resolución de conflictos: last-writer-wins por timestamp del cambio (RF7.5). Si quieres,
 *   luego añadimos comparación con updated_at por tabla.
 * - Registra en CAMBIOS_PROCESADOS para no reprocesar (RF7.6).
 * - Limpieza de archivos antiguos (RF7.7).
 */
public class SharedFolderIngestor implements AutoCloseable {

    private final String instanceId;    // UUID estable por equipo (persistido)
    private final Path sharedDir;       // p.ej. \\SERVIDOR\TOXISAFE\cambios
    private final Path outboxDir;       // sharedDir/pendientes
    private final Path inboxDir;        // sharedDir/entrantes (se puede usar el mismo sharedDir)

    private final ObjectMapper om = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "toxisafe-sync");
        t.setDaemon(true);
        return t;
    });
    private final Connection connection; // para CAMBIOS_PROCESADOS y aplicar cambios

    // DAOs (aplicación de cambios). Pon aquí los que necesites consumir:
    private final BroteDao broteDao;
    private final PersonaExpuestaDao personaExpuestaDao;
    private final IngestaDao ingestaDao;
    private final AlimentoDao alimentoDao;
    private final SintomasGeneralesExpuestoDao sintomasGeneralesDao;
    private final ExposicionSintomaDao exposicionSintomaDao;
    private final BroteEncuestadorDao broteEncuestadorDao;
    private final InformeDao informeDao;
    private final IngestaPersonaExpuestaDao ingestaPersonaExpuestaDao;

    private final DateTimeFormatter fnameTs = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

    public SharedFolderIngestor(Connection connection,
                                Path sharedDir,
                                String instanceId,
                                // DAOs para aplicar cambios:
                                BroteDao broteDao,
                                PersonaExpuestaDao personaExpuestaDao,
                                IngestaDao ingestaDao,
                                AlimentoDao alimentoDao,
                                SintomasGeneralesExpuestoDao sintomasGeneralesDao,
                                ExposicionSintomaDao exposicionSintomaDao,
                                BroteEncuestadorDao broteEncuestadorDao,
                                InformeDao informeDao,
                                IngestaPersonaExpuestaDao ingestaPersonaExpuestaDao) throws SQLException, IOException {
        this.connection = connection;
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.sharedDir = Objects.requireNonNull(sharedDir, "sharedDir");

        this.outboxDir = sharedDir.resolve("outbox");
        this.inboxDir  = sharedDir.resolve("inbox"); // si prefieres una única carpeta, usa sharedDir

        Files.createDirectories(outboxDir);
        Files.createDirectories(inboxDir);

        this.broteDao = broteDao;
        this.personaExpuestaDao = personaExpuestaDao;
        this.ingestaDao = ingestaDao;
        this.alimentoDao = alimentoDao;
        this.sintomasGeneralesDao = sintomasGeneralesDao;
        this.exposicionSintomaDao = exposicionSintomaDao;
        this.broteEncuestadorDao = broteEncuestadorDao;
        this.informeDao = informeDao;
        this.ingestaPersonaExpuestaDao = ingestaPersonaExpuestaDao;

        ensureProcesadosTable();
    }

    public void start(long periodSeconds) {
        scheduler.scheduleAtFixedRate(this::consumeOnceSafe, 5, periodSeconds, TimeUnit.SECONDS);
    }

    @Override public void close() {
        scheduler.shutdownNow();
    }

    /* ======================= CONSUMO (loop) ======================= */

    private void consumeOnceSafe() {
        try { consumeOnce(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    void consumeOnce() throws IOException, SQLException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(inboxDir, "*.json")) {
            List<Path> files = new ArrayList<>();
            for (Path p : ds) files.add(p);
            files.sort(Comparator.comparing(Path::getFileName));

            for (Path f : files) {
                SyncChange c;
                try {
                    c = om.readValue(Files.readAllBytes(f), SyncChange.class);
                } catch (Exception e) {
                    // archivo corrupto -> muévelo a .bad
                    Files.move(f, f.resolveSibling(f.getFileName().toString() + ".bad"), REPLACE_EXISTING);
                    continue;
                }
                if (instanceId.equals(c.getInstanciaOrigen())) {
                    // propio -> opcionalmente eliminar
                    Files.deleteIfExists(f);
                    continue;
                }
                if (yaProcesado(c.getIdCambio())) {
                    Files.deleteIfExists(f);
                    continue;
                }

                // Transaccional
                boolean ok = false;
                try {
                    connection.setAutoCommit(false);
                    aplicarCambio(c); // last-writer-wins por timestamp de cambio (ver nota abajo)
                    registrarProcesado(c);
                    connection.commit();
                    ok = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    connection.rollback();
                } finally {
                    connection.setAutoCommit(true);
                    if (ok) Files.deleteIfExists(f);
                }
            }
        }
    }

    /* ======================= Aplicación de cambios ======================= */

    private void aplicarCambio(SyncChange c) throws Exception {
        // Estrategia simple LWW: aplicamos el cambio tal cual.
        // Si más adelante añadimos updated_at por tabla, aquí comprobaríamos timestamps.

        String t = c.getNombreTabla().toUpperCase(Locale.ROOT);

        switch (t) {
            case "BROTE" -> aplicarBrote(c);
            case "PERSONA_EXPUESTA" -> aplicarPersonaExpuesta(c);
            case "INGESTA" -> aplicarIngesta(c);
            case "ALIMENTO" -> aplicarAlimento(c);
            case "SINTOMAS_GENERALES_EXPUESTO" -> aplicarSintomasGenerales(c);
            case "EXPOSICION_SINTOMA" -> aplicarExposicionSintoma(c);
            case "BROTE_ENCUESTADOR" -> aplicarBroteEncuestador(c);
            case "INFORME" -> aplicarInforme(c);
            case "INGESTA_PERSONA_EXPUESTA" -> aplicarIngestaPersonaExpuesta(c);

            default -> {
                // tablas no sincronizadas aún: ignorar o log
                System.err.println("SYNC: Tabla no gestionada: " + t);
            }
        }
    }

    /* ==== Handlers por tabla (adapta a tus constructores/modelos) ==== */

    private void aplicarBrote(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            broteDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = (c.getTipoOperacion()==SyncChange.Op.INSERT ? c.getDatosNuevos() : c.getDatosNuevos());
        Brote b = new Brote(
                (String)m.get("id_brote"),
                (String)m.get("id_creador"),
                (String)m.get("id_responsable"),
                (String)m.get("fechaIniBrote"),
                (String)m.get("estado_brote"),
                (String)m.get("nombre_brote")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) broteDao.insert(b); else broteDao.update(b);
    }

    private void aplicarPersonaExpuesta(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            personaExpuestaDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        PersonaExpuesta pe = new PersonaExpuesta(
                (String)m.get("id_expuesto"),
                (String)m.get("id_brote"),
                (String)m.get("nombre_expuesto"),
                (String)m.get("apellido_expuesto"),
                (String)m.get("tfno1_expuesto"),
                (String)m.get("tfno2_expuesto"),
                (String)m.get("nhusaExpuesto"),
                (String)m.get("tipo_documento_expuesto"),
                (String)m.get("num_documento_expuesto"),
                (String)m.get("sexoExpuesto"),
                m.get("edadExpuesto") == null ? null : ((Number)m.get("edadExpuesto")).intValue(),
                (String)m.get("fecha_nacimiento_expuesto"),
                (String)m.get("direccionExpuesto"),
                (String)m.get("centroSaludExpuesto"),
                (String)m.get("profesionExpuesto"),
                m.get("manipuladorExpuesto") == null ? null : ((Number)m.get("manipuladorExpuesto")).intValue(),
                (String)m.get("grupoExpuesto"),
                m.get("enfermoExpuesto") == null ? null : ((Number)m.get("enfermoExpuesto")).intValue(),
                m.get("atencionMedicaExpuesto") == null ? null : ((Number)m.get("atencionMedicaExpuesto")).intValue(),
                m.get("atencionHospitalariaExpuesto") == null ? null : ((Number)m.get("atencionHospitalariaExpuesto")).intValue(),
                (String)m.get("fechaAtencionMedicaExpuesto"),
                (String)m.get("lugarAtencionMedicaExpuesto"),
                (String)m.get("evolucionExpuesto"),
                (String)m.get("tratamientoExpuesto"),
                m.get("solicitudCoprocultivoExpuesto") == null ? null : ((Number)m.get("solicitudCoprocultivoExpuesto")).intValue(),
                m.get("estadoCoprocultivoExpuesto") == null ? null : ((Number)m.get("estadoCoprocultivoExpuesto")).intValue(),
                (String)m.get("fechaCoprocultivoExpuesto"),
                (String)m.get("laboratorioCoprocultivoExpuesto"),
                (String)m.get("resultadoCoprocultivoExpuesto"),
                (String)m.get("patogenoCoprocultivoExpuesto"),
                (String)m.get("observacionesCoprocultivoExpuesto"),
                m.get("solicitudFrotisExpuesto") == null ? null : ((Number)m.get("solicitudFrotisExpuesto")).intValue(),
                m.get("estadoFrotisExpuesto") == null ? null : ((Number)m.get("estadoFrotisExpuesto")).intValue(),
                (String)m.get("fechaFrotisExpuesto"),
                (String)m.get("laboratorioFrotisExpuesto"),
                (String)m.get("resultadoFrotisExpuesto"),
                (String)m.get("patogenoFrotisExpuesto"),
                (String)m.get("observacionesFrotisExpuesto")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) personaExpuestaDao.insert(pe); else personaExpuestaDao.update(pe);
    }

    private void aplicarIngesta(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            ingestaDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        Ingesta i = new Ingesta(
                (String)m.get("id_ingesta"),
                (String)m.get("fecha_consumo"),
                (String)m.get("lugar_consumo")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) ingestaDao.insert(i); else ingestaDao.update(i);
    }

    private void aplicarAlimento(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            alimentoDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        Alimento a = new Alimento(
                (String)m.get("id_alimento"),
                (String)m.get("id_ingesta"),
                (String)m.get("nombre"),
                (String)m.get("id_catalogo")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) alimentoDao.insert(a); else alimentoDao.update(a);
    }

    private void aplicarSintomasGenerales(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            sintomasGeneralesDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        SintomasGeneralesExpuesto g = new SintomasGeneralesExpuesto(
                (String)m.get("id_sintomas_generales"),
                (String)m.get("id_expuesto"),
                (String)m.get("fecha_inicio_conjunto"),
                (String)m.get("fecha_fin_conjunto"),
                (String)m.get("observaciones_conjunto")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) sintomasGeneralesDao.insert(g); else sintomasGeneralesDao.update(g);
    }

    private void aplicarExposicionSintoma(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            exposicionSintomaDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        ExposicionSintoma e = new ExposicionSintoma(
                (String)m.get("id_exposicion_sintoma"),
                (String)m.get("id_sintomas_generales"),
                (String)m.get("id_sintoma")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) exposicionSintomaDao.insert(e); else exposicionSintomaDao.update(e);
    }

    private void aplicarBroteEncuestador(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            broteEncuestadorDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        BroteEncuestador be = new BroteEncuestador(
                (String)m.get("id_brote_encuestador"),
                (String)m.get("id_brote"),
                (String)m.get("id_usuario")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) broteEncuestadorDao.insert(be); else broteEncuestadorDao.update(be);
    }

    private void aplicarInforme(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            informeDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        Informe inf = new Informe(
                (String)m.get("id_informe"),
                (String)m.get("id_brote"),
                (String)m.get("contenido_informe")
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) informeDao.insert(inf); else informeDao.update(inf);
    }

    private void aplicarIngestaPersonaExpuesta(SyncChange c) throws SQLException {
        if (c.getTipoOperacion() == SyncChange.Op.DELETE) {
            ingestaPersonaExpuestaDao.delete(c.getIdRegistroAfectado());
            return;
        }
        Map<String,Object> m = c.getDatosNuevos();
        IngestaPersonaExpuesta l = new IngestaPersonaExpuesta(
                (String)m.get("id_ingesta_persona_expuesta"),
                (String)m.get("id_ingesta"),
                (String)m.get("id_expuesto"),
                ((Number)m.getOrDefault("es_sospechosa", 0)).intValue()
        );
        if (c.getTipoOperacion() == SyncChange.Op.INSERT) ingestaPersonaExpuestaDao.insert(l); else ingestaPersonaExpuestaDao.update(l);
    }


    /* ======================= Registro de procesados ======================= */

    private void ensureProcesadosTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS CAMBIOS_PROCESADOS (
                    id_cambio TEXT PRIMARY KEY,
                    timestamp_procesado TEXT NOT NULL,
                    instancia_origen TEXT NOT NULL
                )
            """);
        }
    }

    private boolean yaProcesado(String idCambio) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM CAMBIOS_PROCESADOS WHERE id_cambio=?")) {
            ps.setString(1, idCambio);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void registrarProcesado(SyncChange c) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO CAMBIOS_PROCESADOS(id_cambio,timestamp_procesado,instancia_origen) VALUES (?,?,?)")) {
            ps.setString(1, c.getIdCambio());
            ps.setString(2, Instant.now().toString());
            ps.setString(3, c.getInstanciaOrigen());
            ps.executeUpdate();
        }
    }

    /* ======================= Utilidades ======================= */

    public static String ensureInstanceId(Path workDir) throws IOException {
        Path f = workDir.resolve("instance.id");
        if (Files.exists(f)) return Files.readString(f, StandardCharsets.UTF_8).trim();
        String id = UUID.randomUUID().toString();
        Files.writeString(f, id, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return id;
    }

    /** Limpia .json antiguos (p.ej. > 7 días) */
    public void cleanupOldFiles(int days) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(inboxDir, "*.json")) {
            Instant lim = Instant.now().minusSeconds(days * 86400L);
            for (Path p : ds) {
                try {
                    Instant m = Files.getLastModifiedTime(p).toInstant();
                    if (m.isBefore(lim)) Files.deleteIfExists(p);
                } catch (Exception ignore) { }
            }
        } catch (Exception ignore) { }
    }
}
