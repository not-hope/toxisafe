package com.toxisafe.service;

import com.toxisafe.dao.BroteDao;
import com.toxisafe.dao.BroteEncuestadorDao;
import com.toxisafe.dao.UsuarioDao;
import com.toxisafe.model.Alimento;
import com.toxisafe.model.Brote;
import com.toxisafe.model.BroteEncuestador;
import com.toxisafe.model.Usuario;
import com.toxisafe.sync.SyncEmitter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para la gestión de Brotes.
 * Aplica validaciones de negocio y orquesta operaciones multi-DAO.
 */
public class BroteService {

    private final BroteDao broteDao;
    private final BroteEncuestadorDao broteEncuestadorDao;
    private final UsuarioDao usuarioDao;
    private SyncEmitter sync;

    // Roles permitidos para RESPONSABLE del brote (ajusta si en tu TFG son otros)
    private static final Set<String> ROLES_RESPONSABLE_VALIDOS =
            new HashSet<>(Arrays.asList("ADMIN", "EPIDEMIOLOGO", "MIR_SALUD_PUBLICA"));

    public void setSyncEmitter(SyncEmitter syncEmitter) {
        this.sync = syncEmitter;
    }

    public BroteService(BroteDao broteDao,
                        BroteEncuestadorDao broteEncuestadorDao,
                        UsuarioDao usuarioDao,
                        SyncEmitter sync) {
        this.broteDao = broteDao;
        this.broteEncuestadorDao = broteEncuestadorDao;
        this.usuarioDao = usuarioDao;
        this.sync = sync;
    }

    public BroteService(BroteDao broteDao,
                        BroteEncuestadorDao broteEncuestadorDao,
                        UsuarioDao usuarioDao) {
        this.broteDao = broteDao;
        this.broteEncuestadorDao = broteEncuestadorDao;
        this.usuarioDao = usuarioDao;
        this.sync = null;
    }

    /**
     * Crea un nuevo brote.
     *
     * @param creadorId        id del usuario creador (usuario logueado)
     * @param responsableId    id del usuario responsable (debe existir y tener rol válido)
     * @param fechaYYYYMMDD    fecha en formato ISO (DatePicker.toString) p.ej. 2025-09-01
     * @param nombre           nombre del brote
     */
    public Brote create(String creadorId,
                            String responsableId,
                            String fechaYYYYMMDD,
                            String nombre) throws SQLException {
        // Validaciones básicas
        if (isBlank(creadorId) || isBlank(responsableId))
            throw new IllegalArgumentException("Creador y responsable son obligatorios.");
        if (isBlank(nombre))
            throw new IllegalArgumentException("El nombre del brote es obligatorio.");
        validarFechaISO(fechaYYYYMMDD);

        // Validar existencia de usuarios
        Usuario creador = usuarioDao.findById(creadorId)
                .orElseThrow(() -> new IllegalArgumentException("Creador no existe."));
        Usuario responsable = usuarioDao.findById(responsableId)
                .orElseThrow(() -> new IllegalArgumentException("Responsable no existe."));

        // Validar rol del responsable
        if (!ROLES_RESPONSABLE_VALIDOS.contains(resp(responsable.getRolUsuario())))
            throw new IllegalArgumentException("El responsable debe tener rol ADMIN, EPIDEMIOLOGO o MIR_SALUD_PUBLICA.");

        java.time.LocalDate f = java.time.LocalDate.parse(fechaYYYYMMDD);
        if (f.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser posterior a hoy.");
        }

        if (broteDao.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un brote con ese nombre.");
        }
        // Insert
        String id = UUID.randomUUID().toString();
        Brote b = new Brote(id, creador.getIdUsuario(), responsable.getIdUsuario(), fechaYYYYMMDD, nombre);
        b.setEstadoBrote("ACTIVO");
        b.setFechaCierreBrote(null);

        broteDao.insert(b);

        emitInsert("BROTE", b.getIdBrote(), toMap(b));

        return b;
    }

    /**
     * Actualiza campos de un brote existente.
     * Valida formato de fecha y rol del responsable si cambia.
     */
    public void update(Brote brote) throws SQLException {
        if (brote == null || isBlank(brote.getIdBrote()))
            throw new IllegalArgumentException("Brote inválido.");

        validarFechaISO(brote.getFechIniBrote());
        java.time.LocalDate f = java.time.LocalDate.parse(brote.getFechIniBrote());
        if (f.isAfter(java.time.LocalDate.now()))
            throw new IllegalArgumentException("La fecha no puede ser posterior a hoy.");

        if (broteDao.existsByNombreExceptoId(brote.getNombreBrote(), brote.getIdBrote()))
            throw new IllegalArgumentException("Ya existe otro brote con ese nombre.");

        // Validar que exista
        broteDao.findById(brote.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("El brote no existe."));
        if ("CERRADO".equalsIgnoreCase(brote.getEstadoBrote()))
            throw new IllegalStateException("No se puede editar un brote cerrado.");

        if (isBlank(brote.getNombreBrote()))
            throw new IllegalArgumentException("El nombre del brote es obligatorio.");

        validarFechaISO(brote.getFechIniBrote());

        // Validar responsable si viene informado
        if (isBlank(brote.getResponsableBrote()))
            throw new IllegalArgumentException("Responsable obligatorio.");

        Usuario responsable = usuarioDao.findById(brote.getResponsableBrote())
                .orElseThrow(() -> new IllegalArgumentException("Responsable no existe."));

        if (!ROLES_RESPONSABLE_VALIDOS.contains(resp(responsable.getRolUsuario())))
            throw new IllegalArgumentException("El responsable debe tener rol ADMIN o EPIDEMIOLOGO.");

        Optional<Brote> prevOpt = broteDao.findById(brote.getIdBrote());
        Map<String,Object> prevMap = prevOpt.isPresent() ? toMap(prevOpt.get()) : null;

        broteDao.update(brote);
        emitUpdate("BROTE", brote.getIdBrote(), prevMap, toMap(brote));

    }

    /**
     * Elimina un brote. Como no hay ON DELETE CASCADE, primero borra sus encuestadores asignados.
     */
    public void delete(String idBrote) throws SQLException {
        if (isBlank(idBrote)) throw new IllegalArgumentException("Id de brote inválido.");
        // Verificar que existe
        var brote = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("El brote no existe."));

        if ("CERRADO".equalsIgnoreCase(brote.getEstadoBrote()))
            throw new IllegalStateException("No se puede eliminar un brote cerrado.");

        // Borrar asignaciones de encuestadores (dependencias)
        List<BroteEncuestador> asignaciones = broteEncuestadorDao.findByBroteId(idBrote);
        for (BroteEncuestador be : asignaciones) {
            broteEncuestadorDao.delete(be.getIdBroteEncuestador());
        }

        Optional<Brote> prevOpt = broteDao.findById(idBrote);

        broteDao.delete(idBrote);
        prevOpt.ifPresent(prev -> emitDelete("BROTE", idBrote, toMap(prev)));

    }

    /**
     * Listado simple.
     */
    public List<Brote> findAll() throws SQLException {
        return broteDao.findAll();
    }

    public Optional<Brote> findById(String id) throws SQLException {
        return broteDao.findById(id);
    }

    public List<Brote> findByCreador(String creadorId) throws SQLException {
        return broteDao.findByCreador(creadorId);
    }

    public List<Brote> findByResponsable(String responsableId) throws SQLException {
        return broteDao.findByResponsable(responsableId);
    }

    public List<Brote> findByEstado(String estado) throws SQLException {
        return broteDao.findByEstado(estado);
    }
    /**
     * Devuelve los brotes "visibles" para un usuario, aplicando una lógica simple de permisos:
     *  - ADMIN / EPIDEMIOLOGO: ven todos los brotes
     *  - ENCUESTADOR: ve los brotes donde esté asignado como encuestador
     */
    public List<Brote> brotesVisiblesPara(Usuario usuario) throws SQLException {
        if (usuario == null) return Collections.emptyList();
        String rol = resp(usuario.getRolUsuario());

        if ("ADMIN".equals(rol) || "EPIDEMIOLOGO".equals(rol) || "MIR_SALUD_PUBLICA".equals(rol)) {
            return broteDao.findAll();
        }
        if ("ENCUESTADOR".equals(rol)) {
            List<BroteEncuestador> misAsignaciones = broteEncuestadorDao.findByUsuarioId(usuario.getIdUsuario());
            // cargar brotes de esas asignaciones
            List<String> ids = misAsignaciones.stream()
                    .map(BroteEncuestador::getIdBrote)
                    .distinct()
                    .collect(Collectors.toList());
            List<Brote> out = new ArrayList<>();
            for (String id : ids) {
                broteDao.findById(id).ifPresent(out::add);
            }
            return out;
        }
        // Rol desconocido => nada
        return Collections.emptyList();
    }

    // ----------------- util -----------------

    private void validarFechaISO(String fecha) {
        if (isBlank(fecha)) throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        try {
            // Acepta ISO yyyy-MM-dd (lo que da DatePicker por defecto con toString)
            LocalDate.parse(fecha);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha inválida. Usa formato YYYY-MM-DD.");
        }
    }

    private String resp(String s) {
        if (s == null) return "";
        String up = s.trim().toUpperCase(Locale.ROOT)
                .replace('Á','A').replace('É','E').replace('Í','I').replace('Ó','O').replace('Ú','U')
                .replace('Ü','U').replace('Ñ','N')
                .replace(' ', '_');
        // alias comunes:
        if (up.equals("EPIDEMIOLOG@")) return "EPIDEMIOLOGO";
        return up;
    }

    // Cerrar brote
    public void cerrarBrote(String idBrote, String fechaCierreIso) throws SQLException {
        var brote = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        if ("CERRADO".equalsIgnoreCase(brote.getEstadoBrote())) return; // idempotente
        validarFechaISO(fechaCierreIso);
        if (java.time.LocalDate.parse(fechaCierreIso).isAfter(java.time.LocalDate.now()))
            throw new IllegalArgumentException("La fecha de cierre no puede ser posterior a hoy.");
        Map<String,Object> oldMap = toMap(brote);
        broteDao.actualizarEstado(idBrote, "CERRADO", fechaCierreIso);

        // >>> EMIT SYNC
        emitUpdate("BROTE", idBrote, oldMap, toMap(brote));

    }

    // Reabrir brote
    public void reabrirBrote(String idBrote) throws SQLException {
        var brote = broteDao.findById(idBrote)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        if ("ACTIVO".equalsIgnoreCase(brote.getEstadoBrote())) return; // idempotente

        Map<String,Object> oldMap = toMap(brote);
        broteDao.actualizarEstado(idBrote, "ACTIVO", null);

        // >>> EMIT SYNC
        emitUpdate("BROTE", idBrote, oldMap, toMap(brote));
    }


    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static Map<String,Object> toMap(Brote b) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_brote",                   b.getIdBrote());
        m.put("nombre_brote",               b.getNombreBrote());
        m.put("fecha_inicio_brote",         b.getFechIniBrote());
        m.put("estado_brote",               b.getEstadoBrote());
        m.put("id_creador",                 b.getCreadorBrote());      // <-- DESCOMENTA solo si EXISTE en tu schema
        m.put("id_responsable",             b.getResponsableBrote());  // <-- DESCOMENTA solo si EXISTE en tu schema
        return m;
    }

    private void emitInsert(String tabla, String id, Map<String,Object> data) {
        if (sync != null) sync.emitInsert(tabla, id, data);
    }
    private void emitUpdate(String tabla, String id, Map<String,Object> oldV, Map<String,Object> newV) {
        if (sync != null) sync.emitUpdate(tabla, id, oldV, newV);
    }
    private void emitDelete(String tabla, String id, Map<String,Object> oldV) {
        if (sync != null) sync.emitDelete(tabla, id, oldV);
    }
}
