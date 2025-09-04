package com.toxisafe.sync.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toxisafe.sync.SyncChange;
import com.toxisafe.sync.SyncEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileSyncEmitter implements SyncEmitter {

    private final String instanceId;
    private final Path sharedDir; // usa una única carpeta (más simple)
    private final ObjectMapper om = new ObjectMapper();
    private final DateTimeFormatter fnameTs =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

    public FileSyncEmitter(Path sharedDir, String instanceId) throws IOException {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.sharedDir = Objects.requireNonNull(sharedDir, "sharedDir");
        Files.createDirectories(sharedDir);
    }

    public static String ensureInstanceId(Path workDir) throws IOException {
        Path f = workDir.resolve("instance.id");
        if (Files.exists(f)) return Files.readString(f, StandardCharsets.UTF_8).trim();
        String id = UUID.randomUUID().toString();
        Files.writeString(f, id, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return id;
    }

    @Override
    public void emitInsert(String tabla, String idRegistro, Map<String, Object> nuevos) {
        emitir(build(SyncChange.Op.INSERT, tabla, idRegistro, null, nuevos));
    }

    @Override
    public void emitUpdate(String tabla, String idRegistro, Map<String, Object> antiguos, Map<String, Object> nuevos) {
        emitir(build(SyncChange.Op.UPDATE, tabla, idRegistro, antiguos, nuevos));
    }

    @Override
    public void emitDelete(String tabla, String idRegistro, Map<String, Object> antiguos) {
        emitir(build(SyncChange.Op.DELETE, tabla, idRegistro, antiguos, null));
    }

    private SyncChange build(SyncChange.Op op, String tabla, String id, Map<String,Object> oldV, Map<String,Object> newV) {
        SyncChange c = new SyncChange();
        c.setIdCambio(UUID.randomUUID().toString());
        c.setInstanciaOrigen(instanceId);
        c.setTimestamp(Instant.now());
        c.setTipoOperacion(op);
        c.setNombreTabla(tabla);
        c.setIdRegistroAfectado(id);
        c.setDatosAntiguos(oldV);
        c.setDatosNuevos(newV);
        return c;
    }

    private void emitir(SyncChange c) {
        try {
            String ts = fnameTs.format(c.getTimestamp());
            String fname = ts + "-" + c.getIdCambio() + "-" + c.getInstanciaOrigen() + ".json";
            Path tmp = sharedDir.resolve(fname + ".tmp");
            Path dst = sharedDir.resolve(fname);

            byte[] json = om.writerWithDefaultPrettyPrinter().writeValueAsBytes(c);
            Files.write(tmp, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, dst, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace(); // registra si tienes logger
        }
    }
}
