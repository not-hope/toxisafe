package com.toxisafe.service;

import com.toxisafe.dao.*;
import com.toxisafe.model.*;
import com.toxisafe.sync.SyncEmitter;

import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SintomasGeneralesExpuestoService {

    private final SintomasGeneralesExpuestoDao generalesDao;
    private final ExposicionSintomaDao exposicionDao;
    private final SintomaDao sintomaDao;
    private final PersonaExpuestaDao personaExpuestaDao;
    private final BroteDao broteDao;
    private final BroteEncuestadorService broteEncuestadorService;

    private SyncEmitter sync;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SintomasGeneralesExpuestoService(SintomasGeneralesExpuestoDao generalesDao,
                                   ExposicionSintomaDao exposicionDao,
                                   SintomaDao sintomaDao,
                                   PersonaExpuestaDao personaExpuestaDao,
                                   BroteDao broteDao,
                                   BroteEncuestadorService broteEncuestadorService,
                                            SyncEmitter sync) {
        this.generalesDao = generalesDao;
        this.exposicionDao = exposicionDao;
        this.sintomaDao = sintomaDao;
        this.personaExpuestaDao = personaExpuestaDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        this.sync = sync;
    }

    public SintomasGeneralesExpuestoService(SintomasGeneralesExpuestoDao generalesDao,
                                            ExposicionSintomaDao exposicionDao,
                                            SintomaDao sintomaDao,
                                            PersonaExpuestaDao personaExpuestaDao,
                                            BroteDao broteDao,
                                            BroteEncuestadorService broteEncuestadorService) {
        this.generalesDao = generalesDao;
        this.exposicionDao = exposicionDao;
        this.sintomaDao = sintomaDao;
        this.personaExpuestaDao = personaExpuestaDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        this.sync = null;
    }

    public void setSyncEmitter(SyncEmitter syncEmitter) {
        this.sync = syncEmitter;
    }

    // ========= RF5.1: crear/actualizar registro general de síntomas para un expuesto =========

    public SintomasGeneralesExpuesto upsertGenerales(SintomasGeneralesExpuesto g, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(g, "SintomasGeneralesExpuesto requerido");
        if (isBlank(g.getIdExpuesto())) throw new IllegalArgumentException("id_expuesto obligatorio");

        // Permisos & brote activo
        PersonaExpuesta pe = personaExpuestaDao.findById(g.getIdExpuesto())
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        checkPermisoSobreBrote(actor, b.getIdBrote(), "modificar síntomas del expuesto");

        validarGenerales(g);

        // upsert por expuesto: si existe, actualiza; si no, crea
        Optional<SintomasGeneralesExpuesto> existente = generalesDao.findByExpuestoId(g.getIdExpuesto());
        if (existente.isPresent()) {
            SintomasGeneralesExpuesto cur = existente.get();
            g.setIdSintomasGenerales(cur.getIdSintomasGenerales());
            generalesDao.update(g);
            emitUpdate("SINTOMAS_GENERALES_EXPUESTO",
                    g.getIdSintomasGenerales(),
                    toMap(cur),
                    toMap(g));
            return g;
        } else {
            if (isBlank(g.getIdSintomasGenerales())) {
                g.setIdSintomasGenerales(UUID.randomUUID().toString());
            }
            generalesDao.insert(g);
            emitInsert("SINTOMAS_GENERALES_EXPUESTO", g.getIdSintomasGenerales(), toMap(g));
            return g;
        }
    }

    /** Alias con el nombre que espera el Controller; delega en tu upsertGenerales(...) */
    public SintomasGeneralesExpuesto upsertGeneral(SintomasGeneralesExpuesto g, Usuario actor) throws SQLException {
        return upsertGenerales(g, actor);
    }

    /** Lectura simple por id_expuesto (sin control de permisos). El Controller la usa para pintar el formulario. */
    public Optional<SintomasGeneralesExpuesto> findByExpuestoId(String expuestoId) throws SQLException {
        return generalesDao.findByExpuestoId(expuestoId);
    }

    /** Devuelve sólo los IDs de síntoma asociados al conjunto. Útil para marcar checkboxes en UI. */
    public Set<String> findSintomaIdsByGeneralId(String idSintomasGenerales) throws SQLException {
        Set<String> out = new HashSet<>();
        for (ExposicionSintoma e : exposicionDao.findBySintomasGeneralesId(idSintomasGenerales)) {
            out.add(e.getIdSintoma());
        }
        return out;
    }

    // ========= RF5.3: visualización =========

    public Optional<SintomasGeneralesExpuesto> findGeneralesByExpuestoVisiblePara(String idExpuesto, Usuario actor) throws SQLException {
        requireActor(actor);
        // Visibilidad como en otros módulos
        PersonaExpuesta pe = personaExpuestaDao.findById(idExpuesto)
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) {
            return generalesDao.findByExpuestoId(idExpuesto);
        }
        if (rol.equals("ENCUESTADOR")) {
            boolean asignado = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> pe.getIdBrote().equals(be.getIdBrote()));
            return asignado ? generalesDao.findByExpuestoId(idExpuesto) : Optional.empty();
        }
        return Optional.empty();
    }

    public List<ExposicionSintoma> listExposiciones(String idSintomasGenerales, Usuario actor) throws SQLException {
        requireActor(actor);
        SintomasGeneralesExpuesto g = generalesDao.findById(idSintomasGenerales)
                .orElseThrow(() -> new IllegalArgumentException("Registro general no encontrado"));
        // permiso lectura igual que arriba
        return exposicionDao.findBySintomasGeneralesId(idSintomasGenerales);
    }

    // ========= RF5.4: modificar conjunto (añadir/quitar síntomas) =========

    public ExposicionSintoma addSintomaAlConjunto(String idSintomasGenerales, String idSintoma, Usuario actor) throws SQLException {
        requireActor(actor);
        SintomasGeneralesExpuesto g = generalesDao.findById(idSintomasGenerales)
                .orElseThrow(() -> new IllegalArgumentException("Registro general no encontrado"));
        PersonaExpuesta pe = personaExpuestaDao.findById(g.getIdExpuesto())
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        checkPermisoSobreBrote(actor, b.getIdBrote(), "añadir síntomas al expuesto");

        // Referencias válidas
        sintomaDao.findById(idSintoma).orElseThrow(() -> new IllegalArgumentException("Síntoma inexistente"));

        // Evita duplicados (además del UNIQUE en BD)
        boolean yaExiste = exposicionDao.findBySintomasGeneralesId(idSintomasGenerales).stream()
                .anyMatch(e -> e.getIdSintoma().equals(idSintoma));
        if (yaExiste) throw new IllegalArgumentException("Ese síntoma ya está en el conjunto.");

        ExposicionSintoma e = new ExposicionSintoma(UUID.randomUUID().toString(), idSintomasGenerales, idSintoma);
        exposicionDao.insert(e);
        emitInsert("EXPOSICION_SINTOMA", e.getIdExposicionSintoma(), toMap(e));
        return e;
    }

    public void removeSintomaDelConjunto(String idSintomasGenerales, String idSintoma, Usuario actor) throws SQLException {
        requireActor(actor);
        SintomasGeneralesExpuesto g = generalesDao.findById(idSintomasGenerales)
                .orElseThrow(() -> new IllegalArgumentException("Registro general no encontrado"));
        PersonaExpuesta pe = personaExpuestaDao.findById(g.getIdExpuesto())
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        checkPermisoSobreBrote(actor, b.getIdBrote(), "eliminar síntomas del expuesto");

        // Busca la exposición por par (conjunto, síntoma) y bórrala
        for (ExposicionSintoma e : exposicionDao.findBySintomasGeneralesId(idSintomasGenerales)) {
            if (e.getIdSintoma().equals(idSintoma)) {
                exposicionDao.delete(e.getIdExposicionSintoma());
                emitDelete("EXPOSICION_SINTOMA", e.getIdExposicionSintoma(), toMap(e));
                return;
            }
        }
        // idempotente: si no estaba, no pasa nada
    }

    /** Reemplaza la lista de síntomas del conjunto por los indicados (útil para multi-selección en UI). */
    public void replaceSintomas(String idSintomasGenerales, Collection<String> nuevosSintomas, Usuario actor) throws SQLException {
        requireActor(actor);
        SintomasGeneralesExpuesto g = generalesDao.findById(idSintomasGenerales)
                .orElseThrow(() -> new IllegalArgumentException("Registro general no encontrado"));
        PersonaExpuesta pe = personaExpuestaDao.findById(g.getIdExpuesto())
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        checkPermisoSobreBrote(actor, b.getIdBrote(), "modificar síntomas del expuesto");

        // Normaliza set destino
        Set<String> destino = new HashSet<>(nuevosSintomas == null ? Collections.emptyList() : nuevosSintomas);

        // Validar existencia de cada id_sintoma
        for (String idS : destino) {
            sintomaDao.findById(idS).orElseThrow(() -> new IllegalArgumentException("Síntoma inexistente: " + idS));
        }

        // Estado actual
        List<ExposicionSintoma> actuales = exposicionDao.findBySintomasGeneralesId(idSintomasGenerales);
        Set<String> actualesIds = new HashSet<>();
        for (ExposicionSintoma e : actuales) actualesIds.add(e.getIdSintoma());

        // Eliminar los que sobran
        for (ExposicionSintoma e : actuales) {
            if (!destino.contains(e.getIdSintoma())) {
                exposicionDao.delete(e.getIdExposicionSintoma());
                emitDelete("EXPOSICION_SINTOMA", e.getIdExposicionSintoma(), toMap(e));
            }
        }

        // Añadir los que faltan
        for (String idS : destino) {
            if (!actualesIds.contains(idS)) {
                ExposicionSintoma nuevo = new ExposicionSintoma(UUID.randomUUID().toString(), idSintomasGenerales, idS);
                exposicionDao.insert(new ExposicionSintoma(UUID.randomUUID().toString(), idSintomasGenerales, idS));
                emitInsert("EXPOSICION_SINTOMA", nuevo.getIdExposicionSintoma(), toMap(nuevo));
            }
        }
    }

    // ========= RF5.5: eliminar registro general (y sus síntomas) =========

    public void deleteGenerales(String idSintomasGenerales, Usuario actor) throws SQLException {
        requireActor(actor);
        if (isBlank(idSintomasGenerales)) throw new IllegalArgumentException("id_sintomas_generales requerido");
        SintomasGeneralesExpuesto g = generalesDao.findById(idSintomasGenerales)
                .orElseThrow(() -> new IllegalArgumentException("Registro general no encontrado"));

        PersonaExpuesta pe = personaExpuestaDao.findById(g.getIdExpuesto())
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        checkPermisoSobreBrote(actor, b.getIdBrote(), "eliminar registro de síntomas");

        // Gracias al ON DELETE CASCADE en EXPOSICION_SINTOMA, basta con borrar el general.
        generalesDao.delete(idSintomasGenerales);
        emitDelete("SINTOMAS_GENERALES_EXPUESTO", idSintomasGenerales, toMap(g));
    }

    // ========= Validación de fechas =========

    private void validarGenerales(SintomasGeneralesExpuesto g) {
        // Permitir null/blank; si vienen, que sean 'yyyy-MM-dd HH:mm:ss'
        LocalDateTime ini = parseOrNull(g.getFechaInicioConjunto());
        LocalDateTime fin = parseOrNull(g.getFechaFinConjunto());
        if (ini != null && fin != null && fin.isBefore(ini)) {
            throw new IllegalArgumentException("La fecha/hora de fin no puede ser anterior al inicio.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (ini != null && ini.isAfter(ahora)) {
            throw new IllegalArgumentException("La fecha/hora de inicio no puede ser futura.");
        }
        if (fin != null && fin.isAfter(ahora)) {
            throw new IllegalArgumentException("La fecha/hora de fin no puede ser futura.");
        }
    }

    private LocalDateTime parseOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s, DF); }
        catch (Exception e) { throw new IllegalArgumentException("Las fechas deben tener formato YYYY-MM-DD HH:MM:SS"); }
    }

    // ========= Permisos / helpers =========

    private void requireActor(Usuario actor) {
        if (actor == null || isBlank(actor.getIdUsuario()))
            throw new SecurityException("Sesión no válida.");
    }

    private void checkPermisoSobreBrote(Usuario actor, String idBrote, String accion) throws SQLException {
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) return;

        if (rol.equals("ENCUESTADOR")) {
            boolean asignado = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> idBrote.equals(be.getIdBrote()));
            if (!asignado) throw new SecurityException("No tiene permiso para " + accion + ".");
            return;
        }
        throw new SecurityException("Rol no autorizado: " + actor.getRolUsuario());
    }

    private void assertBroteActivo(Brote b) {
        if ("CERRADO".equalsIgnoreCase(b.getEstadoBrote()))
            throw new IllegalStateException("No se pueden modificar síntomas de un brote cerrado.");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String resp(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    /** Definición de caso “regla” basada en nombres de síntomas normalizados. */
    public static final class CasoDef {
        final Set<String> requiereTodos;       // Deben estar todos
        final Set<String> requiereAlMenosUno;  // Debe estar al menos uno
        final int minimoTotal;                 // (opcional) Nº mínimo de síntomas

        private CasoDef(Set<String> todos, Set<String> alguno, int minimoTotal) {
            this.requiereTodos = (todos == null ? Set.of() : todos);
            this.requiereAlMenosUno = (alguno == null ? Set.of() : alguno);
            this.minimoTotal = Math.max(0, minimoTotal);
        }

        /** Regla por defecto (ajústala si quieres): Diarrea Y (Fiebre o Vómitos o Dolor abdominal). */
        public static CasoDef porDefecto() {
            Set<String> todos = new HashSet<>(Set.of(norm("Diarrea")));
            Set<String> alguno = new HashSet<>(Arrays.asList(
                    norm("Fiebre"),
                    norm("Vómitos"), // “Vómitos” y “Vomitos” normalizan igual
                    norm("Dolor o calambres abdominales")
            ));
            return new CasoDef(todos, alguno, 0);
        }
    }

    /** Devuelve true si el expuesto cumple la definición de caso indicada. */
    public boolean esCaso(String idExpuesto, CasoDef def, Usuario actor) throws SQLException {
        if (def == null) def = CasoDef.porDefecto();

        var genOpt = findGeneralesByExpuestoVisiblePara(idExpuesto, actor);
        if (genOpt.isEmpty()) return false;

        var gen = genOpt.get();
        var expos = listExposiciones(gen.getIdSintomasGenerales(), actor);
        if (expos.isEmpty()) return false;

        // Conjunto de nombres de síntomas normalizados seleccionados por el expuesto
        Set<String> seleccion = new HashSet<>();
        for (ExposicionSintoma e : expos) {
            var sOpt = sintomaDao.findById(e.getIdSintoma());
            if (sOpt.isPresent()) {
                seleccion.add(norm(sOpt.get().getNombreSintoma()));
            }
        }

        if (def.minimoTotal > 0 && seleccion.size() < def.minimoTotal) return false;

        // Deben estar todos los requeridos
        if (!seleccion.containsAll(def.requiereTodos)) return false;

        // Debe estar al menos uno de los alternativos (si los hay)
        if (!def.requiereAlMenosUno.isEmpty()) {
            boolean alguno = false;
            for (String cand : def.requiereAlMenosUno) {
                if (seleccion.contains(cand)) { alguno = true; break; }
            }
            if (!alguno) return false;
        }

        return true;
    }

    /** Comodín: usa la definición por defecto. */
    public boolean esCasoPorDefecto(String idExpuesto, Usuario actor) throws SQLException {
        return esCaso(idExpuesto, CasoDef.porDefecto(), actor);
    }


    // Normaliza texto: mayúsculas, sin tildes/diacríticos y sin espacios extremos
    private static String norm(String s) {
        if (s == null) return "";
        String up = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(Locale.ROOT);
    }

    // --- emisión genérica (no rompe si sync == null) ---
    private void emitInsert(String tabla, String id, Map<String,Object> data) {
        if (sync != null) sync.emitInsert(tabla, id, data);
    }
    private void emitUpdate(String tabla, String id, Map<String,Object> oldV, Map<String,Object> newV) {
        if (sync != null) sync.emitUpdate(tabla, id, oldV, newV);
    }
    private void emitDelete(String tabla, String id, Map<String,Object> oldV) {
        if (sync != null) sync.emitDelete(tabla, id, oldV);
    }

    // --- mapeadores a Map<String,Object> con las CLAVES exactas que consume el ingestor ---
    private static Map<String,Object> toMap(SintomasGeneralesExpuesto g) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_sintomas_generales", g.getIdSintomasGenerales());
        m.put("id_expuesto", g.getIdExpuesto());
        m.put("fecha_inicio_conjunto", g.getFechaInicioConjunto());
        m.put("fecha_fin_conjunto", g.getFechaFinConjunto());
        m.put("observaciones_conjunto", g.getObservacionesConjunto());
        return m;
    }

    private static Map<String,Object> toMap(ExposicionSintoma e) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_exposicion_sintoma", e.getIdExposicionSintoma());
        m.put("id_sintomas_generales", e.getIdSintomasGenerales());
        m.put("id_sintoma", e.getIdSintoma());
        return m;
    }


}
