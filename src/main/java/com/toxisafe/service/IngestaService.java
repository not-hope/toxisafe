package com.toxisafe.service;

import com.toxisafe.dao.BroteDao;
import com.toxisafe.dao.IngestaDao;
import com.toxisafe.dao.IngestaPersonaExpuestaDao;
import com.toxisafe.dao.PersonaExpuestaDao;
import com.toxisafe.model.*;
import com.toxisafe.sync.SyncEmitter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class IngestaService {

    private final IngestaDao ingestaDao;
    private final IngestaPersonaExpuestaDao linkDao;
    private final PersonaExpuestaDao personaExpuestaDao;
    private final BroteDao broteDao;
    private final BroteEncuestadorService broteEncuestadorService;
    private SyncEmitter sync;

    public IngestaService(IngestaDao ingestaDao,
                          IngestaPersonaExpuestaDao linkDao,
                          PersonaExpuestaDao personaExpuestaDao,
                          BroteDao broteDao,
                          BroteEncuestadorService broteEncuestadorService,
                          SyncEmitter sync) {
        this.ingestaDao = ingestaDao;
        this.linkDao = linkDao;
        this.personaExpuestaDao = personaExpuestaDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        java.util.Objects.requireNonNull(broteEncuestadorService,
                "IngestaService: broteEncuestadorService es obligatorio");
        this.sync = sync;
    }

    public IngestaService(IngestaDao ingestaDao,
                          IngestaPersonaExpuestaDao linkDao,
                          PersonaExpuestaDao personaExpuestaDao,
                          BroteDao broteDao,
                          BroteEncuestadorService broteEncuestadorService) {
        this.ingestaDao = ingestaDao;
        this.linkDao = linkDao;
        this.personaExpuestaDao = personaExpuestaDao;
        this.broteDao = broteDao;
        this.broteEncuestadorService = broteEncuestadorService;
        java.util.Objects.requireNonNull(broteEncuestadorService,
                "IngestaService: broteEncuestadorService es obligatorio");
        this.sync = null;
    }
    public void setSyncEmitter(SyncEmitter syncEmitter) {
        this.sync = syncEmitter;
    }

    /* ======================= Lecturas ======================= */

    public Optional<Ingesta> findById(String id, Usuario actor) throws SQLException {
        requireActor(actor);
        // Lectura simple (si necesitas, puedes filtrar por permisos a través de un expuesto concreto)
        return ingestaDao.findById(id);
    }

    /** Lista ingestas asociadas a un expuesto (visibles para el actor). */
    public List<Ingesta> findByExpuestoVisiblePara(String expuestoId, Usuario actor) throws SQLException {
        requireActor(actor);
        PersonaExpuesta pe = personaExpuestaDao.findById(expuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        String broteId = pe.getIdBrote();
        if (!puedeVer(actor, broteId)) return Collections.emptyList();

        List<IngestaPersonaExpuesta> links = linkDao.findByExpuestoId(expuestoId);
        List<Ingesta> out = new ArrayList<>();
        for (IngestaPersonaExpuesta l : links) {
            ingestaDao.findById(l.getIdIngesta()).ifPresent(out::add);
        }
        // por si hubiera duplicados (no debería)
        return out.stream().distinct().collect(Collectors.toList());
    }

    /** Devuelve los enlaces (N:M) de una ingesta, visibles para el actor. */
    public List<IngestaPersonaExpuesta> enlacesDeIngestaVisiblePara(String ingestaId, Usuario actor) throws SQLException {
        requireActor(actor);
        String broteId = inferirBroteDeIngesta(ingestaId).orElse(null);
        if (broteId != null && !puedeVer(actor, broteId)) return Collections.emptyList();
        return linkDao.findByIngestaId(ingestaId);
    }

    /* ======================= Escrituras ======================= */

    /** Crea una ingesta y la enlaza con un expuesto en el mismo paso (recomendado). */
    public Ingesta createWithLink(Ingesta ingesta,
                                  String expuestoId,
                                  Integer esSospechosa,
                                  Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(ingesta, "Ingesta requerida");
        validarIngesta(ingesta);

        PersonaExpuesta pe = personaExpuestaDao.findById(expuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        assertPermisoEscritura(actor, b.getIdBrote());

        if (isBlank(ingesta.getIdIngesta())) {
            ingesta.setIdIngesta(UUID.randomUUID().toString());
        }
        ingestaDao.insert(ingesta);

        IngestaPersonaExpuesta link = new IngestaPersonaExpuesta(
                UUID.randomUUID().toString(),
                ingesta.getIdIngesta(),
                expuestoId,
                n01(esSospechosa)
        );
        // Enforce integridad: si la ingesta ya tenía enlaces, todos deben pertenecer al mismo brote
        assertEnlaceConsistenteConBrote(ingesta.getIdIngesta(), pe.getIdBrote());
        linkDao.insert(link);

        // tras ingestaDao.insert(ingesta);
        emitInsert("INGESTA", ingesta.getIdIngesta(), toMap(ingesta));

        emitInsert("INGESTA", ingesta.getIdIngesta(), toMap(ingesta));

// EMIT: ENLACE (usa el PK REAL y todas las columnas)
        Map<String,Object> linkM = toMapLink(link, expuestoId, ingesta.getIdIngesta());
        emitInsert("INGESTA_PERSONA_EXPUESTA", link.getIdIngestaPersonaExpuesta(), linkM);


        return ingesta;
    }

    /** Crea una ingesta sin enlaces (permitido solo para ADMIN/EPI/MIR). */
    public Ingesta create(Ingesta ingesta, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(ingesta, "Ingesta requerida");
        validarIngesta(ingesta);

        String rol = resp(actor.getRolUsuario());
        if (!(rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")))
            throw new SecurityException("Solo ADMIN/EPIDEMIOLOGO/MIR pueden crear ingestas sin enlace inicial.");

        if (isBlank(ingesta.getIdIngesta())) {
            ingesta.setIdIngesta(UUID.randomUUID().toString());
        }
        ingestaDao.insert(ingesta);
        emitInsert("INGESTA", ingesta.getIdIngesta(), toMap(ingesta));

        return ingesta;
    }

    public void update(Ingesta ingesta, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(ingesta, "Ingesta requerida");
        if (isBlank(ingesta.getIdIngesta())) throw new IllegalArgumentException("id_ingesta requerido");
        validarIngesta(ingesta);

        // Si la ingesta ya está enlazada, validar permisos contra el brote inferido
        Optional<String> broteIdOpt = inferirBroteDeIngesta(ingesta.getIdIngesta());
        if (broteIdOpt.isPresent()) {
            Brote b = broteDao.findById(broteIdOpt.get())
                    .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
            assertBroteActivo(b);
            assertPermisoEscritura(actor, b.getIdBrote());
        } else {
            // sin enlaces: solo ADMIN/EPI/MIR pueden editar
            String rol = resp(actor.getRolUsuario());
            if (!(rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")))
                throw new SecurityException("Solo ADMIN/EPIDEMIOLOGO/MIR pueden editar ingestas sin enlaces.");
        }

        Optional<Ingesta> prevOpt = ingestaDao.findById(ingesta.getIdIngesta());
        Map<String,Object> prev = prevOpt.map(IngestaService::toMap).orElse(null);

        ingestaDao.update(ingesta);

        emitUpdate("INGESTA", ingesta.getIdIngesta(), prev, toMap(ingesta));

    }

    public void delete(String ingestaId, Usuario actor) throws SQLException {
        requireActor(actor);
        if (isBlank(ingestaId)) throw new IllegalArgumentException("id_ingesta requerido");

        Optional<String> broteIdOpt = inferirBroteDeIngesta(ingestaId);
        if (broteIdOpt.isPresent()) {
            Brote b = broteDao.findById(broteIdOpt.get())
                    .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
            assertBroteActivo(b);
            assertPermisoEscritura(actor, b.getIdBrote());
        } else {
            String rol = resp(actor.getRolUsuario());
            if (!(rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")))
                throw new SecurityException("Solo ADMIN/EPIDEMIOLOGO/MIR pueden borrar ingestas sin enlaces.");
        }

        List<IngestaPersonaExpuesta> enlaces = linkDao.findByIngestaId(ingestaId);
        for (IngestaPersonaExpuesta l : enlaces) {
            linkDao.delete(l.getIdIngestaPersonaExpuesta());
            emitDelete("INGESTA_PERSONA_EXPUESTA", l.getIdIngestaPersonaExpuesta(), toMapLink(l, l.getIdExpuesto(), l.getIdIngesta()));
        }

        Optional<Ingesta> prevOpt = ingestaDao.findById(ingestaId);
        ingestaDao.delete(ingestaId);

        prevOpt.ifPresent(p -> emitDelete("INGESTA", ingestaId, toMap(p)));

    }

    /* ============ Gestión de enlaces (N:M) ============ */
    public void marcarSospechosa(String ingestaId, String expuestoId, boolean sospechosa, Usuario actor) throws SQLException {
        requireActor(actor);
        IngestaPersonaExpuesta l = linkDao.findByIngestaAndExpuesto(ingestaId, expuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Enlace no encontrado"));

        PersonaExpuesta pe = personaExpuestaDao.findById(expuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Expuesto no encontrado"));
        Brote b = broteDao.findById(pe.getIdBrote())
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        assertPermisoEscritura(actor, b.getIdBrote());

        l.setEsSospechosaParaExpuesto(sospechosa ? 1 : 0);
        linkDao.update(l);
        Map<String,Object> link = toMapLink(l, expuestoId, ingestaId);
        emitUpdate("INGESTA_PERSONA_EXPUESTA", l.getIdIngestaPersonaExpuesta(), null, link);


    }

    /* ======================= Helpers ======================= */

    private void validarIngesta(Ingesta i) {
        if (isBlank(i.getFechaConsumo()))
            throw new IllegalArgumentException("fecha_consumo obligatoria");
        // ISO yyyy-MM-dd
        LocalDate d;
        try { d = LocalDate.parse(i.getFechaConsumo()); }
        catch (Exception e) { throw new IllegalArgumentException("fecha_consumo debe ser YYYY-MM-DD"); }
        if (d.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("fecha_consumo no puede ser futura");
        // lugar opcional -> recorta si viene
        if (i.getLugarConsumo() != null) i.setLugarConsumo(i.getLugarConsumo().trim());
    }

    private boolean puedeVer(Usuario actor, String broteId) throws SQLException {
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) return true;
        if (rol.equals("ENCUESTADOR")) {
            return broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                    .stream().anyMatch(be -> broteId.equals(be.getIdBrote()));
        }
        return false;
    }

    private void assertPermisoEscritura(Usuario actor, String broteId) throws SQLException {
        if (!puedeVer(actor, broteId))
            throw new SecurityException("No tiene permisos para modificar ingestas en este brote.");
        // ENCUESTADOR: puede escribir si está asignado (puedeVer ya cubre ese caso)
    }

    private void assertBroteActivo(Brote b) {
        if ("CERRADO".equalsIgnoreCase(b.getEstadoBrote()))
            throw new IllegalStateException("No se pueden modificar ingestas de un brote cerrado.");
    }

    /** Determina el brote de una ingesta via sus enlaces; si hay enlaces, todos deben compartir brote. */
    private Optional<String> inferirBroteDeIngesta(String ingestaId) throws SQLException {
        List<IngestaPersonaExpuesta> links = linkDao.findByIngestaId(ingestaId);
        if (links.isEmpty()) return Optional.empty();
        Set<String> brotes = new HashSet<>();
        for (IngestaPersonaExpuesta l : links) {
            String b = personaExpuestaDao.findById(l.getIdExpuesto())
                    .orElseThrow(() -> new IllegalStateException("Expuesto " + l.getIdExpuesto() + " no existe"))
                    .getIdBrote();
            brotes.add(b);
        }
        if (brotes.size() > 1)
            throw new IllegalStateException("Integridad: la ingesta mezcla expuestos de distintos brotes.");
        return Optional.of(brotes.iterator().next());
    }

    /** Evita que una ingesta se vincule a expuestos de brotes distintos. */
    private void assertEnlaceConsistenteConBrote(String ingestaId, String broteIdNuevo) throws SQLException {
        Optional<String> actual = inferirBroteDeIngesta(ingestaId);
        if (actual.isPresent() && !Objects.equals(actual.get(), broteIdNuevo)) {
            throw new IllegalArgumentException("La ingesta ya pertenece a otro brote.");
        }
    }

    private void requireActor(Usuario actor) {
        if (actor == null || isBlank(actor.getIdUsuario()))
            throw new SecurityException("Sesión no válida.");
    }

    public java.util.Optional<String> broteIdDeIngesta(String ingestaId) throws java.sql.SQLException {
        // delega en tu helper existente que mira links N:M y obtiene el brote
        return inferirBroteDeIngesta(ingestaId);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String resp(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    private static int n01(Integer v) {
        return (v != null && v == 1) ? 1 : 0;
    }

    // ---- emisión segura (no hace nada si sync == null) ----
    private void emitInsert(String tabla, String id, Map<String,Object> data) {
        if (sync != null) sync.emitInsert(tabla, id, data);
    }
    private void emitUpdate(String tabla, String id, Map<String,Object> oldV, Map<String,Object> newV) {
        if (sync != null) sync.emitUpdate(tabla, id, oldV, newV);
    }
    private void emitDelete(String tabla, String id, Map<String,Object> oldV) {
        if (sync != null) sync.emitDelete(tabla, id, oldV);
    }

    // ---- mapeo EXACTO a columnas de tu tabla INGESTA ----
// (No incluyas id_brote: tu esquema de INGESTA no lo tiene)
    private static Map<String,Object> toMap(Ingesta i) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_ingesta", i.getIdIngesta());
        m.put("fecha_consumo", i.getFechaConsumo());
        m.put("lugar_consumo", i.getLugarConsumo());
        return m;
    }

    private static Map<String,Object> toMapLink(IngestaPersonaExpuesta l, String idExpuesto, String idIngesta) {
        Map<String,Object> m = new LinkedHashMap<>();
        // Usa los nombres EXACTOS de tus columnas:
        m.put("id_ingesta_persona_expuesta", l.getIdIngestaPersonaExpuesta()); // PK real del enlace
        m.put("id_ingesta", idIngesta);
        m.put("id_expuesto", idExpuesto);
        m.put("es_sospechosa", l.getEsSospechosaParaExpuesto() == null ? 0 : l.getEsSospechosaParaExpuesto());
        return m;
    }


}
