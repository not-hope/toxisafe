package com.toxisafe.sync.util;

import com.toxisafe.dao.*;
import com.toxisafe.sync.SyncEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * Orquestador de sincronización por carpeta compartida.
 * - Expone un SyncEmitter para que los Services emitan cambios (outbox).
 * - Planifica la ingestión periódica de archivos JSON (inbox) mediante SharedFolderIngestor.
 * NO duplica la lógica del emisor ni del ingestor.
 */
public final class SyncService implements AutoCloseable {

    private final SyncEmitter emitter;                 // escribe JSON a outbox
    private final SharedFolderIngestor ingestor;       // lee/aplica JSON desde inbox
    private final ScheduledExecutorService scheduler;  // planificador del consume

    private SyncService(SyncEmitter emitter, SharedFolderIngestor ingestor) {
        this.emitter = Objects.requireNonNull(emitter, "emitter");
        this.ingestor = Objects.requireNonNull(ingestor, "ingestor");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "toxisafe-sync");
            t.setDaemon(true);
            return t;
        });
    }

    /** Devuelve el emisor para inyectarlo en tus Services. */
    public SyncEmitter emitter() {
        return emitter;
    }

    /** Arranca el planificador de ingestión (cada {@code periodSeconds}). */
    public void start(long periodSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ingestor.consumeOnce();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, periodSeconds, TimeUnit.SECONDS);
    }

    /** Ejecuta una pasada de ingestión bajo demanda (útil en debug o botón “Sincronizar ahora”). */
    public void consumeOnceNow() {
        try {
            ingestor.consumeOnce();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void close() {
        scheduler.shutdownNow();
    }

    // ---------- Fábrica recomendada para tu app ----------

    /**
     * Construye el ecosistema de Sync usando tu Connection, carpeta compartida y DAOs.
     * Crea (si no existen) subcarpetas: sharedRoot/outbox y sharedRoot/inbox.
     * Firma alineada con lo que ya pasas a SharedFolderIngestor.
     */
    public static SyncService createDefault(
            Connection conn,
            Path sharedRoot,
            BroteDao broteDao,
            PersonaExpuestaDao personaExpuestaDao,
            IngestaDao ingestaDao,
            AlimentoDao alimentoDao,
            SintomasGeneralesExpuestoDao sintomasGeneralesDao,
            ExposicionSintomaDao exposicionSintomaDao,
            BroteEncuestadorDao broteEncuestadorDao,
            InformeDao informeDao,
            IngestaPersonaExpuestaDao ingestaPersonaExpuestaDao
    ) throws IOException, SQLException {

        Objects.requireNonNull(conn, "conn");
        Objects.requireNonNull(sharedRoot, "sharedRoot");

        Path outbox = sharedRoot.resolve("outbox");
        Path inbox  = sharedRoot.resolve("inbox");
        Files.createDirectories(outbox);
        Files.createDirectories(inbox);

        String instanceId = ensureInstanceId(sharedRoot);

        // Emisor -> escribe a outbox
        SyncEmitter emitter = new FileSyncEmitter(outbox, instanceId);

        // Ingestor -> lee de inbox y aplica con DAOs
        SharedFolderIngestor ingestor = new SharedFolderIngestor(
                conn,
                inbox,
                instanceId,
                broteDao,
                personaExpuestaDao,
                ingestaDao,
                alimentoDao,
                sintomasGeneralesDao,
                exposicionSintomaDao,
                broteEncuestadorDao,
                informeDao,
                ingestaPersonaExpuestaDao
        );

        return new SyncService(emitter, ingestor);
    }

    /** Genera/recupera un UUID estable por equipo en sharedRoot/instance.id */
    public static String ensureInstanceId(Path sharedRoot) throws IOException {
        Path f = sharedRoot.resolve("instance.id");
        if (Files.exists(f)) {
            return Files.readString(f, StandardCharsets.UTF_8).trim();
        }
        String id = UUID.randomUUID().toString();
        Files.writeString(f, id, StandardCharsets.UTF_8, CREATE_NEW);
        return id;
    }
}

