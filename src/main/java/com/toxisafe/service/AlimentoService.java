package com.toxisafe.service;

import com.toxisafe.dao.AlimentoCatalogoAliasDao;
import com.toxisafe.dao.AlimentoCatalogoDao;
import com.toxisafe.dao.AlimentoDao;
import com.toxisafe.dao.BroteDao;
import com.toxisafe.model.Alimento;
import com.toxisafe.model.Brote;
import com.toxisafe.model.BroteEncuestador;
import com.toxisafe.model.Usuario;
import com.toxisafe.sync.SyncEmitter;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.*;

public class AlimentoService {

    /** Permite resolver id_brote a partir de id_ingesta (implementado por IngestaService). */
    public interface IngestaLookup {
        Optional<String> findBroteIdByIngestaId(String ingestaId) throws SQLException;
    }

    private final AlimentoDao alimentoDao;
    private final IngestaLookup ingestaLookup;
    private final BroteDao broteDao;
    private final BroteEncuestadorService broteEncuestadorService;

    private final AlimentoCatalogoDao catalogoDao;
    private final AlimentoCatalogoAliasDao aliasDao;
    private SyncEmitter sync;

    public AlimentoService(AlimentoDao alimentoDao,
                           IngestaLookup ingestaLookup,
                           BroteDao broteDao,
                           BroteEncuestadorService broteEncuestadorService,
                           AlimentoCatalogoDao alimentoCatalogoDao,
                           AlimentoCatalogoAliasDao alimentoCatalogoAliasDao,
                           SyncEmitter sync) {
        this.alimentoDao = Objects.requireNonNull(alimentoDao, "alimentoDao");
        this.ingestaLookup = Objects.requireNonNull(ingestaLookup, "ingestaLookup");
        this.broteDao = Objects.requireNonNull(broteDao, "broteDao");
        this.broteEncuestadorService = Objects.requireNonNull(broteEncuestadorService, "broteEncuestadorService");
        this.catalogoDao = Objects.requireNonNull(alimentoCatalogoDao, "alimentoCatalogoDao");
        this.aliasDao = Objects.requireNonNull(alimentoCatalogoAliasDao, "alimentoCatalogoAliasDao");
        this.sync = sync;
    }

    public AlimentoService(AlimentoDao alimentoDao,
                           IngestaLookup ingestaLookup,
                           BroteDao broteDao,
                           BroteEncuestadorService broteEncuestadorService,
                           AlimentoCatalogoDao alimentoCatalogoDao,
                           AlimentoCatalogoAliasDao alimentoCatalogoAliasDao) {
        this.alimentoDao = Objects.requireNonNull(alimentoDao, "alimentoDao");
        this.ingestaLookup = Objects.requireNonNull(ingestaLookup, "ingestaLookup");
        this.broteDao = Objects.requireNonNull(broteDao, "broteDao");
        this.broteEncuestadorService = Objects.requireNonNull(broteEncuestadorService, "broteEncuestadorService");
        this.catalogoDao = Objects.requireNonNull(alimentoCatalogoDao, "alimentoCatalogoDao");
        this.aliasDao = Objects.requireNonNull(alimentoCatalogoAliasDao, "alimentoCatalogoAliasDao");
        this.sync = null;
    }

    public void setSyncEmitter(SyncEmitter syncEmitter) {
        this.sync = syncEmitter;
    }

    /* =================== Lectura =================== */

    public List<Alimento> findByIngestaIdVisiblePara(String ingestaId, Usuario actor) throws SQLException {
        requireActor(actor);
        String broteId = broteDeIngesta(ingestaId);
        String rol = resp(actor.getRolUsuario());

        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) {
            return alimentoDao.findByIngestaId(ingestaId);
        }
        if (rol.equals("ENCUESTADOR")) {
            return estaAsignado(actor.getIdUsuario(), broteId)
                    ? alimentoDao.findByIngestaId(ingestaId)
                    : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /* =================== Escritura =================== */

    public Alimento create(Alimento a, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(a, "Alimento requerido");
        validar(a);

        String broteId = broteDeIngesta(a.getIdIngesta());
        Brote b = broteDao.findById(broteId)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        assertPermisoEscritura(actor, b.getIdBrote());

        // normalización y dulicados
        String nombreNorm = normalizarNombre(a.getNombre());
        assertNoDuplicadoEnIngesta(a.getIdIngesta(), nombreNorm, null);
        a.setNombre(nombreNorm);

        if (isBlank(a.getIdAlimento())) {
            a.setIdAlimento(UUID.randomUUID().toString());
        }
        a.setIdCatalogo(resolverCatalogoId(a.getNombre()).orElse(null));

        a.setNombre(normalizarDesdeCatalogo(a.getNombre())); // NUEVO

        alimentoDao.insert(a);
        emitInsert("ALIMENTO", a.getIdAlimento(), toMap(a));
        return a;
    }

    public void update(Alimento a, Usuario actor) throws SQLException {
        requireActor(actor);
        Objects.requireNonNull(a, "Alimento requerido");
        if (isBlank(a.getIdAlimento())) throw new IllegalArgumentException("id_alimento requerido");
        validar(a);

        String broteId = broteDeIngesta(a.getIdIngesta());
        Brote b = broteDao.findById(broteId)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));
        assertBroteActivo(b);
        assertPermisoEscritura(actor, b.getIdBrote());

        String nombreNorm = normalizarNombre(a.getNombre());
        assertNoDuplicadoEnIngesta(a.getIdIngesta(), nombreNorm, a.getIdAlimento());
        a.setNombre(nombreNorm);
        a.setIdCatalogo(resolverCatalogoId(a.getNombre()).orElse(null));
        a.setNombre(normalizarDesdeCatalogo(a.getNombre())); // NUEVO
        var prevOpt = alimentoDao.findById(a.getIdAlimento());
        Map<String,Object> prevMap = prevOpt.isPresent() ? toMap(prevOpt.get()) : null;

        alimentoDao.update(a);
        emitUpdate("ALIMENTO",
                a.getIdAlimento(),
                prevMap,
                toMap(a));
    }

    public void delete(String idAlimento, Usuario actor) throws SQLException {
        requireActor(actor);
        if (isBlank(idAlimento)) throw new IllegalArgumentException("id_alimento requerido");

        // Cargar para conocer su ingesta y, por ende, el brote
        Alimento a = alimentoDao.findById(idAlimento)
                .orElseThrow(() -> new IllegalArgumentException("Alimento no encontrado"));

        String broteId = broteDeIngesta(a.getIdIngesta());
        Brote b = broteDao.findById(broteId)
                .orElseThrow(() -> new IllegalArgumentException("Brote no encontrado"));

        assertBroteActivo(b);
        assertPermisoEscritura(actor, b.getIdBrote());

        var antiguosOpt = alimentoDao.findById(idAlimento);
        alimentoDao.delete(idAlimento);
        antiguosOpt.ifPresent(prev ->emitDelete("ALIMENTO", a.getIdAlimento(), toMap(a)));

    }

    /* =================== Validaciones =================== */

    private void validar(Alimento a) {
        if (isBlank(a.getIdIngesta())) throw new IllegalArgumentException("id_ingesta obligatorio");
        if (isBlank(a.getNombre())) throw new IllegalArgumentException("El nombre del alimento es obligatorio");
        if (a.getNombre().trim().length() > 120)
            throw new IllegalArgumentException("El nombre del alimento es demasiado largo (máx. 120)");
    }

    /** Evita duplicados de nombre (case/espacios-insensible) dentro de la misma ingesta. */
    private void assertNoDuplicadoEnIngesta(String idIngesta, String nombreNormalizado, String excluirId) throws SQLException {
        List<Alimento> existentes = alimentoDao.findByIngestaId(idIngesta);
        boolean dup = existentes.stream().anyMatch(x -> {
            if (excluirId != null && excluirId.equals(x.getIdAlimento())) return false;
            String nx = normalizarNombre(x.getNombre());
            return nx.equalsIgnoreCase(nombreNormalizado);
        });
        if (dup) throw new IllegalArgumentException("Ya existe un alimento con ese nombre en esta ingesta.");
    }

    private static String normalizarNombre(String s) {
        String t = (s == null ? "" : s.trim());
        // colapsa espacios internos
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    /* =================== Permisos / Estado =================== */

    private void assertPermisoEscritura(Usuario actor, String broteId) throws SQLException {
        String rol = resp(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) return;
        if (rol.equals("ENCUESTADOR") && estaAsignado(actor.getIdUsuario(), broteId)) return;
        throw new SecurityException("No tiene permisos para modificar alimentos en este brote.");
    }

    private boolean estaAsignado(String usuarioId, String broteId) throws SQLException {
        return broteEncuestadorService.findByUsuarioId(usuarioId).stream()
                .anyMatch(be -> broteId.equals(be.getIdBrote()));
    }

    private void assertBroteActivo(Brote b) {
        if ("CERRADO".equalsIgnoreCase(b.getEstadoBrote()))
            throw new IllegalStateException("No se pueden modificar alimentos de un brote cerrado.");
    }

    /* =================== Helpers =================== */

    private String broteDeIngesta(String ingestaId) throws SQLException {
        if (isBlank(ingestaId)) throw new IllegalArgumentException("id_ingesta obligatorio");
        return ingestaLookup.findBroteIdByIngestaId(ingestaId)
                .orElseThrow(() -> new IllegalArgumentException("Ingesta no encontrada o sin brote asociado"));
    }

    private void requireActor(Usuario actor) {
        if (actor == null || isBlank(actor.getIdUsuario()))
            throw new SecurityException("Sesión no válida.");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String resp(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    // ====== Normalización y resolución de catálogo ======

    private static String norm(String s) {
        if (s == null) return null;
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")     // sin acentos
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");     // colapsar espacios
        return t;
    }

    // SUGERENCIAS para poblar el combo (usa catálogo + alias). Si no hay DAOs, devuelve vacío.
    public List<String> catalogoSugerencias(String query) throws SQLException {
        if (catalogoDao == null || aliasDao == null) return Collections.emptyList();
        String q = (query == null ? "" : query.trim());
        if (q.isEmpty()) return catalogoDao.findAllNombres();

        // union: nombres que empiezan por q + canónicos de alias que empiezan por q
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.addAll(catalogoDao.findByPrefix(q));
        out.addAll(aliasDao.findCanonicosByAliasPrefix(q)); // devuelve el canónico
        return new ArrayList<>(out);
    }

    // Normaliza entrada: si coincide con nombre canónico => ese; si coincide con alias => devuelve canónico; si no, deja tal cual.
    private String normalizarDesdeCatalogo(String entrada) throws SQLException {
        if (entrada == null) return null;
        String in = entrada.trim();
        if (in.isEmpty() || catalogoDao == null || aliasDao == null) return in;

        // Exacto en catálogo
        if (catalogoDao.existsNombreExacto(in)) return in;

        // Alias -> canónico
        Optional<String> can = aliasDao.canonicoDeAlias(in);
        return can.orElse(in);
    }


    private Optional<String> resolverCatalogoId(String nombreLibre) throws SQLException {
        String n = norm(nombreLibre);
        if (isBlank(n)) return Optional.empty();

        // 1) Alias exacto normalizado
        var alias = aliasDao.findByAliasNorm(n);
        if (alias.isPresent()) return Optional.of(alias.get().getIdCatalogo());

        // 2) Coincidencia por nombre canónico normalizado
        var cat = catalogoDao.findByNombreNorm(n);
        return cat.map(c -> c.getIdCatalogo());
    }

    private static Map<String,Object> toMap(Alimento a) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id_alimento", a.getIdAlimento());
        m.put("id_ingesta",  a.getIdIngesta());
        m.put("nombre",      a.getNombre());
        m.put("id_catalogo", a.getIdCatalogo());
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
