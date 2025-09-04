package com.toxisafe.service;

        import com.toxisafe.dao.BroteDao;
        import com.toxisafe.dao.PersonaExpuestaDao;
        import com.toxisafe.model.Brote;
        import com.toxisafe.model.BroteEncuestador;
        import com.toxisafe.model.PersonaExpuesta;
        import com.toxisafe.model.Usuario;
        import com.toxisafe.sync.SyncEmitter;

        import java.sql.SQLException;
        import java.time.format.DateTimeFormatter;
        import java.util.*;
        import java.util.regex.Pattern;
        import java.util.stream.Collectors;

/**
 * Servicio de dominio para PersonaExpuesta con emisión de cambios a sync.
 * - Valida datos (fechas ISO, 0/1, documento, teléfonos)
 * - Aplica permisos por rol
 * - Escritura bloqueada si el brote está CERRADO
 * - Emite INSERT/UPDATE/DELETE a través de SyncEmitter (si se inyecta)
 */
public class PersonaExpuestaService {

    private final PersonaExpuestaDao personaExpuestaDao;
    private final BroteDao broteDao;
    private final BroteEncuestadorService broteEncuestadorService;

    // Emisor opcional de sincronización (carpeta compartida)
    private SyncEmitter sync;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /* =================== CONSTRUCTORES =================== */

    public PersonaExpuestaService(PersonaExpuestaDao personaExpuestaDao,
                                  BroteDao broteDao,
                                  BroteEncuestadorService broteEncuestadorService) {
        this(personaExpuestaDao, broteDao, broteEncuestadorService, null);
    }

    public PersonaExpuestaService(PersonaExpuestaDao personaExpuestaDao,
                                  BroteDao broteDao,
                                  BroteEncuestadorService broteEncuestadorService,
                                  SyncEmitter syncEmitter) {
        this.personaExpuestaDao = personaExpuestaDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        this.sync = syncEmitter;
    }

    /** Permite inyectar el emisor después de construir. */
    public void setSyncEmitter(SyncEmitter emitter) {
        this.sync = emitter;
    }

    /* =================== API PÚBLICA =================== */

    public PersonaExpuesta create(PersonaExpuesta pe, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(pe, "PersonaExpuesta requerida");
        validarCampos(pe, false);

        // Brote debe existir y estar ACTIVO
        Brote brote = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("El brote no existe: " + pe.getIdBrote()));
        assertBroteActivo(brote);

        // Permisos
        checkPermisoSobreBrote(actor, brote.getIdBrote(), "crear");

        // id si falta
        if (isBlank(pe.getIdExpuesto())) {
            pe.setIdExpuesto(UUID.randomUUID().toString());
        }

        personaExpuestaDao.insert(pe);

        // Emitir sync
        emitInsert("PERSONA_EXPUESTA", pe.getIdExpuesto(), toMap(pe));

        return pe;
    }

    public void update(PersonaExpuesta pe, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(pe, "PersonaExpuesta requerida");
        if (isBlank(pe.getIdExpuesto()))
            throw new IllegalArgumentException("id_expuesto requerido para actualizar");

        validarCampos(pe, true);

        // Brote objetivo debe existir y estar ACTIVO
        Brote brote = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("El brote no existe: " + pe.getIdBrote()));
        assertBroteActivo(brote);

        // Permisos
        checkPermisoSobreBrote(actor, pe.getIdBrote(), "actualizar");

        // para change log
        Map<String,Object> prev = personaExpuestaDao.findById(pe.getIdExpuesto())
                .map(this::toMap).orElse(null);

        personaExpuestaDao.update(pe);

        // Emitir sync
        emitUpdate("PERSONA_EXPUESTA", pe.getIdExpuesto(), prev, toMap(pe));
    }

    public void delete(String idExpuesto, Usuario actor) throws SQLException {
        requireActor(actor);
        if (isBlank(idExpuesto)) throw new IllegalArgumentException("id_expuesto requerido");

        // Cargar expuesto para conocer su brote
        var opt = personaExpuestaDao.findById(idExpuesto);
        if (opt.isEmpty()) return; // idempotente
        PersonaExpuesta pe = opt.get();

        Brote brote = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("El brote no existe: " + pe.getIdBrote()));
        assertBroteActivo(brote);
        checkPermisoSobreBrote(actor, pe.getIdBrote(), "eliminar");

        Map<String,Object> prev = toMap(pe);

        personaExpuestaDao.delete(idExpuesto);

        // Emitir sync
        emitDelete("PERSONA_EXPUESTA", idExpuesto, prev);
    }

    public Optional<PersonaExpuesta> findById(String id) throws SQLException {
        return personaExpuestaDao.findById(id);
    }

    /**
     * Lectura visible:
     *  - ADMIN/EPIDEMIOLOGO/MIR: ven todos los expuestos del brote
     *  - ENCUESTADOR: solo si está asignado; si no, devuelve lista vacía
     */
    public List<PersonaExpuesta> findByBroteIdVisiblePara(String broteId, Usuario actor) throws SQLException {
        requireActor(actor);
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) {
            return personaExpuestaDao.findByBroteId(broteId);
        }
        if (rol.equals("ENCUESTADOR")) {
            boolean asignado = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> broteId.equals(be.getIdBrote()));
            return asignado ? personaExpuestaDao.findByBroteId(broteId) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public List<PersonaExpuesta> findAllVisibles(Usuario actor) throws SQLException {
        requireActor(actor);
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) {
            return personaExpuestaDao.findAll();
        }
        // ENCUESTADOR: sólo expuestos de brotes asignados
        var asignaciones = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario());
        Set<String> brotes = asignaciones.stream().map(BroteEncuestador::getIdBrote).collect(Collectors.toSet());
        List<PersonaExpuesta> out = new ArrayList<>();
        for (String idBrote : brotes) {
            out.addAll(personaExpuestaDao.findByBroteId(idBrote));
        }
        return out;
    }

    /* =================== VALIDACIONES =================== */

    private void validarCampos(PersonaExpuesta pe, boolean esUpdate) throws SQLException {
        if (isBlank(pe.getIdBrote())) throw new IllegalArgumentException("id_brote obligatorio");
        if (isBlank(pe.getNombreExpuesto())) throw new IllegalArgumentException("nombre obligatorio");
        if (isBlank(pe.getApellidoExpuesto())) throw new IllegalArgumentException("apellido obligatorio");
        if (isBlank(pe.getTfno1Expuesto())) throw new IllegalArgumentException("teléfono obligatorio");

        // Documento opcional, pero si viene uno, deben venir ambos
        String tipo = (pe.getTipoDocumentoExpuesto() == null ? null : pe.getTipoDocumentoExpuesto().trim());
        String num  = (pe.getNumDocumentoExpuesto()  == null ? null : pe.getNumDocumentoExpuesto().trim());

        if (!isBlank(tipo) || !isBlank(num)) {
            if (isBlank(tipo) || isBlank(num))
                throw new IllegalArgumentException("Tipo y número de documento deben informarse juntos.");
            validarDocumento(tipo, num);
            boolean exists = esUpdate
                    ? personaExpuestaDao.existsDocumentoEnBroteExcepto(
                    pe.getIdExpuesto(), pe.getIdBrote(), tipo, num)
                    : personaExpuestaDao.existsDocumentoEnBrote(
                    pe.getIdBrote(), tipo, num);
            if (exists) throw new IllegalArgumentException("Ya existe un expuesto con ese documento en este brote.");

            // Normaliza número y tipo en memoria (sin espacios/guiones)
            pe.setNumDocumentoExpuesto(normalizarDoc(num));
            pe.setTipoDocumentoExpuesto(tipo.trim());
        }

        // Teléfonos (ambos opcionales)
        if (pe.getTfno1Expuesto() != null) {
            String d = String.valueOf(pe.getTfno1Expuesto());
            if (!d.matches("\\+?\\d{6,15}"))
                throw new IllegalArgumentException("Teléfono 1 debe contener solo dígitos y opcional '+'.");
        }
        if (!isBlank(pe.getTfno2Expuesto())) {
            String n2 = normalizarTelefono(pe.getTfno2Expuesto());
            if (!n2.matches("\\+?\\d{6,15}"))
                throw new IllegalArgumentException("Teléfono 2 debe contener solo dígitos y opcional '+'.");
            pe.setTfno2Expuesto(n2);
        }

        if (pe.getEdadExpuesto() < 0)
            throw new IllegalArgumentException("La edad no puede ser negativa");
        if (pe.getEdadExpuesto() > 120) {
            throw new IllegalArgumentException("La edad debe ser menor de 120");
        }

        // Fechas en ISO si vienen (no obligatorias)
        mustBeIsoOrNull(pe.getFechaNacimientoExpuesto(), "fecha_nacimiento");
        mustBeIsoOrNull(pe.getFechaAtencionMedicaExpuesto(), "fecha_atencion_medica");
        mustBeIsoOrNull(pe.getFechaCoprocultivoExpuesto(), "fecha_coprocultivo");
        mustBeIsoOrNull(pe.getFechaFrotisExpuesto(), "fecha_frotis");

        // Normaliza banderas 0/1 (evita NPEs)
        normalizeFlag(pe::isManipuladorExpuesto, pe::setManipuladorExpuesto);
        normalizeFlag(pe::isEnfermoExpuesto, pe::setEnfermoExpuesto);
        normalizeFlag(pe::isAtencionMedicaExpuesto, pe::setAtencionMedicaExpuesto);
        normalizeFlag(pe::isAtencionHospitalariaExpuesto, pe::setAtencionHospitalariaExpuesto);
        normalizeFlag(pe::isSolicitudCoprocultivoExpuesto, pe::setSolicitudCoprocultivoExpuesto);
        normalizeFlag(pe::isEstadoCoprocultivoExpuesto, pe::setEstadoCoprocultivoExpuesto);
        normalizeFlag(pe::isSolicitudFrotisExpuesto, pe::setSolicitudFrotisExpuesto);
        normalizeFlag(pe::isEstadoFrotisExpuesto, pe::setEstadoFrotisExpuesto);
    }

    private void mustBeIsoOrNull(String value, String field) {
        if (value == null || value.isBlank()) return;
        if (!ISO_DATE.matcher(value).matches())
            throw new IllegalArgumentException(field + " debe tener formato YYYY-MM-DD");
    }

    private void normalizeFlag(java.util.function.Supplier<Integer> getter,
                               java.util.function.Consumer<Integer> setter) {
        Integer v = getter.get();
        if (v == null) setter.accept(0);
        else if (v != 0 && v != 1) throw new IllegalArgumentException("Valor de bandera debe ser 0 o 1");
    }

    /* =================== PERMISOS =================== */

    private void checkPermisoSobreBrote(Usuario actor, String idBrote, String accion) throws SQLException {
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) return;

        if (rol.equals("ENCUESTADOR")) {
            boolean asignado = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> idBrote.equals(be.getIdBrote()));
            if (!asignado) {
                throw new SecurityException("No tiene permiso para " + accion + " expuestos de este brote.");
            }
            return;
        }
        throw new SecurityException("Rol no autorizado: " + actor.getRolUsuario());
    }

    private void requireActor(Usuario actor) {
        if (actor == null || isBlank(actor.getIdUsuario()))
            throw new SecurityException("Sesión no válida.");
    }

    private void assertBroteActivo(Brote b) {
        if ("CERRADO".equalsIgnoreCase(b.getEstadoBrote()))
            throw new IllegalStateException("No se pueden modificar expuestos de un brote cerrado.");
    }

    /* =================== HELPERS =================== */

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String resp(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String normalizarDoc(String num) {
        if (num == null) return null;
        String t = num.trim().replace(" ", "").replace("-", "");
        return t.toUpperCase(java.util.Locale.ROOT);
    }

    private static String normalizarTelefono(String s) {
        if (s == null) return null;
        String t = s.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
        return t; // ej: +34911222333 o 611222333
    }

    private void validarDocumento(String tipo, String num) {
        String t = resp(tipo);
        String n = normalizarDoc(num);

        switch (t) {
            case "DNI":
                if (!Pattern.compile("^\\d{8}[A-HJ-NP-TV-Z]$").matcher(n).matches())
                    throw new IllegalArgumentException("Formato de DNI inválido. Ej: 12345678Z");
                break;
            case "NIE":
                if (!Pattern.compile("^[XYZ]\\d{7}[A-HJ-NP-TV-Z]$").matcher(n).matches())
                    throw new IllegalArgumentException("Formato de NIE inválido. Ej: X1234567L");
                break;
            case "PASAPORTE":
                if (!Pattern.compile("^[A-Z0-9]{3,9}$").matcher(n).matches())
                    throw new IllegalArgumentException("Formato de pasaporte inválido.");
                break;
            case "OTRO":
                if (!Pattern.compile("^[A-Z0-9\\-]{3,20}$").matcher(n).matches())
                    throw new IllegalArgumentException("Número de documento inválido.");
                break;
            default:
                throw new IllegalArgumentException("Tipo de documento desconocido.");
        }
    }

    /* =================== SYNC WRAPPERS =================== */

    private void emitInsert(String tabla, String id, Map<String,Object> nuevos) {
        if (sync != null) sync.emitInsert(tabla, id, nuevos);
    }
    private void emitUpdate(String tabla, String id, Map<String,Object> antiguos, Map<String,Object> nuevos) {
        if (sync != null) sync.emitUpdate(tabla, id, antiguos, nuevos);
    }
    private void emitDelete(String tabla, String id, Map<String,Object> antiguos) {
        if (sync != null) sync.emitDelete(tabla, id, antiguos);
    }

    /** Mapea el objeto a un Map<columna, valor> para el emisor de sync. Ajusta a tu DDL real. */
    private Map<String,Object> toMap(PersonaExpuesta p) {
        Map<String,Object> m = new LinkedHashMap<>();
        // Claves principales
        m.put("id_expuesto", p.getIdExpuesto());
        m.put("id_brote", p.getIdBrote());

        // Identificación
        m.put("nombre_expuesto", p.getNombreExpuesto());
        m.put("apellido_expuesto", p.getApellidoExpuesto());
        m.put("tipo_documento_expuesto", p.getTipoDocumentoExpuesto());
        m.put("num_documento_expuesto", p.getNumDocumentoExpuesto());

        // Contacto
        m.put("tfno1_expuesto", p.getTfno1Expuesto());
        m.put("tfno2_expuesto", p.getTfno2Expuesto());

        // Fechas
        m.put("fecha_nacimiento_expuesto", p.getFechaNacimientoExpuesto());
        m.put("fecha_atencion_medica_expuesto", p.getFechaAtencionMedicaExpuesto());
        m.put("fecha_coprocultivo_expuesto", p.getFechaCoprocultivoExpuesto());
        m.put("fecha_frotis_expuesto", p.getFechaFrotisExpuesto());

        // Banderas 0/1 (usa nombres/DDL reales)
        m.put("manipulador_expuesto", p.isManipuladorExpuesto());
        m.put("enfermo_expuesto", p.isEnfermoExpuesto());
        m.put("atencion_medica_expuesto", p.isAtencionMedicaExpuesto());
        m.put("atencion_hospitalaria_expuesto", p.isAtencionHospitalariaExpuesto());
        m.put("solicitud_coprocultivo_expuesto", p.isSolicitudCoprocultivoExpuesto());
        m.put("estado_coprocultivo_expuesto", p.isEstadoCoprocultivoExpuesto());
        m.put("solicitud_frotis_expuesto", p.isSolicitudFrotisExpuesto());
        m.put("estado_frotis_expuesto", p.isEstadoFrotisExpuesto());

        // Si tu modelo/tabla tiene más columnas (dirección, email, observaciones, etc.), añádelas aquí.
        return m;
    }
}
