package com.toxisafe.service;

import com.toxisafe.model.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio de Estadística para brotes.
 * - Line list
 * - Curva epidémica por día (casos)
 * - Tasas de ataque y medidas 2x2 por alimento (RR, OR, IC95%, Fisher bilateral)
 *
 * Depende SOLO de servicios de dominio existentes (no DAOs):
 *   - PersonaExpuestaService
 *   - SintomasGeneralesExpuestoService
 *   - IngestaService
 *   - AlimentoService
 */
public class EstadisticaService {

    private final PersonaExpuestaService personaExpuestaService;
    private final SintomasGeneralesExpuestoService sintomasService;
    private final IngestaService ingestaService;
    private final AlimentoService alimentoService;
    private final SintomaService sintomaService;


    public EstadisticaService(PersonaExpuestaService personaExpuestaService,
                              SintomasGeneralesExpuestoService sintomasService,
                              IngestaService ingestaService,
                              AlimentoService alimentoService,
                              SintomaService sintomaService) {
        this.personaExpuestaService = personaExpuestaService;
        this.sintomasService = sintomasService;
        this.ingestaService = ingestaService;
        this.alimentoService = alimentoService;
        this.sintomaService = sintomaService;

    }

    /* ===================== DTOs de salida ===================== */

    /** Fila de line list básica. Amplía si quieres más columnas. */
    public static final class LineListRow {
        public final String idExpuesto;
        public final String nombre;
        public final String apellido;
        public final String fechaInicio; // "yyyy-MM-dd HH:mm:ss" o null
        public final String fechaFin;    // idem
        public final int numSintomas;
        public final boolean esCaso;

        public LineListRow(String idExpuesto, String nombre, String apellido,
                           String fechaInicio, String fechaFin, int numSintomas, boolean esCaso) {
            this.idExpuesto = idExpuesto;
            this.nombre = nombre;
            this.apellido = apellido;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.numSintomas = numSintomas;
            this.esCaso = esCaso;
        }
    }

    /** Recuento por día para curva epidémica. */
    public static final class EpiBin {
        public final LocalDate date;
        public final int cases;
        public EpiBin(LocalDate date, int cases) {
            this.date = date; this.cases = cases;
        }
    }

    /** Resultado 2x2 por alimento con métricas epidemiológicas. */
    public static final class FoodAttackRow {
        public final String alimentoKey;   // clave normalizada del alimento (nombre o catálogo)
        public final String alimentoLabel; // etiqueta visible
        public final int a; // expuestos & caso
        public final int b; // expuestos & no-caso
        public final int c; // no expuestos & caso
        public final int d; // no expuestos & no-caso
        public final double arExpuestos;   // ataque expuestos
        public final double arNoExpuestos; // ataque no expuestos
        public final Double rr;  public final Double rrL;  public final Double rrH;
        public final Double or;  public final Double orL;  public final Double orH;
        public final Double pValueFisherTwoSided;

        public FoodAttackRow(String alimentoKey, String alimentoLabel,
                             int a, int b, int c, int d,
                             double arExpuestos, double arNoExpuestos,
                             Double rr, Double rrL, Double rrH,
                             Double or, Double orL, Double orH,
                             Double pValueFisherTwoSided) {
            this.alimentoKey = alimentoKey;
            this.alimentoLabel = alimentoLabel;
            this.a = a; this.b = b; this.c = c; this.d = d;
            this.arExpuestos = arExpuestos;
            this.arNoExpuestos = arNoExpuestos;
            this.rr = rr; this.rrL = rrL; this.rrH = rrH;
            this.or = or; this.orL = orL; this.orH = orH;
            this.pValueFisherTwoSided = pValueFisherTwoSided;
        }
    }

    /* ===================== API principal ===================== */

    /** Line list visible para el actor de todos los expuestos del brote. */
    public List<LineListRow> buildLineList(String idBrote, Usuario actor) throws SQLException {
        Objects.requireNonNull(actor, "actor requerido");
        List<PersonaExpuesta> expuestos = personaExpuestaService.findByBroteIdVisiblePara(idBrote, actor);

        List<LineListRow> out = new ArrayList<>();
        for (PersonaExpuesta pe : expuestos) {
            var optG = sintomasService.findGeneralesByExpuestoVisiblePara(pe.getIdExpuesto(), actor);
            String ini = null, fin = null;
            int numSintomas = 0;
            boolean caso = false;

            if (optG.isPresent()) {
                var g = optG.get();
                ini = g.getFechaInicioConjunto();
                fin = g.getFechaFinConjunto();
                var expos = sintomasService.listExposiciones(g.getIdSintomasGenerales(), actor);
                numSintomas = expos == null ? 0 : expos.size();
                caso = esCaso(pe.getIdExpuesto(), actor);
            }

            out.add(new LineListRow(
                    pe.getIdExpuesto(),
                    nz(pe.getNombreExpuesto()),
                    nz(pe.getApellidoExpuesto()),
                    ini, fin, numSintomas, caso
            ));
        }
        return out;
    }

    /** Curva epidémica (recuento de CASOS por día de inicio). */
    public List<EpiBin> buildEpiCurveDaily(String idBrote, Usuario actor) throws SQLException {
        Objects.requireNonNull(actor, "actor requerido");
        DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<LocalDate, Integer> counts = new HashMap<>();

        for (PersonaExpuesta pe : personaExpuestaService.findByBroteIdVisiblePara(idBrote, actor)) {
            if (!esCaso(pe.getIdExpuesto(), actor)) continue;

            var optG = sintomasService.findGeneralesByExpuestoVisiblePara(pe.getIdExpuesto(), actor);
            if (optG.isEmpty()) continue;
            var g = optG.get();
            String ini = g.getFechaInicioConjunto();
            if (isBlank(ini)) continue;

            try {
                LocalDateTime ldt = LocalDateTime.parse(ini, DF);
                LocalDate d = ldt.toLocalDate();
                counts.merge(d, 1, Integer::sum);
            } catch (Exception ignore) {
                // formato incorrecto -> ignora
            }
        }

        List<LocalDate> fechas = new ArrayList<>(counts.keySet());
        Collections.sort(fechas);
        List<EpiBin> out = new ArrayList<>();
        for (LocalDate d : fechas) out.add(new EpiBin(d, counts.getOrDefault(d, 0)));
        return out;
    }

    /**
     * Tasas de ataque, RR/OR/IC y p-valor Fisher por ALIMENTO.
     * Clave de alimento = id_catalogo si existe; si no, nombre normalizado.
     */
    public List<FoodAttackRow> computeAttackByFood(String idBrote, Usuario actor) throws SQLException {
        Objects.requireNonNull(actor, "actor requerido");

        // 1) Universo de expuestos visibles y condición de caso
        List<PersonaExpuesta> expuestos = personaExpuestaService.findByBroteIdVisiblePara(idBrote, actor);
        Map<String, Boolean> esCasoPorExpuesto = new HashMap<>();
        for (PersonaExpuesta pe : expuestos) {
            esCasoPorExpuesto.put(pe.getIdExpuesto(), esCaso(pe.getIdExpuesto(), actor));
        }

        // 2) Para cada expuesto, determinar alimentos consumidos (set por persona para evitar duplicados)
        Map<String, String> alimentoLabel = new HashMap<>();            // clave -> etiqueta visible
        Map<String, Set<String>> expuestosPorAlimento = new HashMap<>();// clave -> set idExpuesto
        Set<String> universoExpuestos = new HashSet<>();

        for (PersonaExpuesta pe : expuestos) {
            universoExpuestos.add(pe.getIdExpuesto());

            Set<String> alimentosPersona = new HashSet<>();
            for (Ingesta ing : ingestaService.findByExpuestoVisiblePara(pe.getIdExpuesto(), actor)) {
                for (Alimento al : alimentoService.findByIngestaIdVisiblePara(ing.getIdIngesta(), actor)) {
                    String key;
                    String label;

                    String idCat = null;
                    try { idCat = al.getIdCatalogo(); } catch (Throwable ignore) {}
                    if (idCat != null && !idCat.isBlank()) {
                        key = "CAT#" + idCat.trim();
                        label = nz(al.getNombre());
                    } else {
                        label = nz(al.getNombre());
                        key = norm(label);
                    }

                    alimentosPersona.add(key);
                    alimentoLabel.putIfAbsent(key, label);
                }
            }
            for (String key : alimentosPersona) {
                expuestosPorAlimento.computeIfAbsent(key, k -> new HashSet<>()).add(pe.getIdExpuesto());
            }
        }

        // 3) Tablas 2x2 por alimento
        int N = universoExpuestos.size();
        List<FoodAttackRow> out = new ArrayList<>();

        for (Map.Entry<String, Set<String>> e : expuestosPorAlimento.entrySet()) {
            String key = e.getKey();
            Set<String> expAl = e.getValue();
            String label = alimentoLabel.getOrDefault(key, key);

            int a = 0, b = 0, c = 0, d = 0;
            for (String idExp : universoExpuestos) {
                boolean expuesto = expAl.contains(idExp);
                boolean caso = esCasoPorExpuesto.getOrDefault(idExp, false);
                if (expuesto && caso) a++;
                else if (expuesto) b++;
                else if (!expuesto && caso) c++;
                else d++;
            }

            double arExp = ratio(a, a + b);
            double arNoExp = ratio(c, c + d);
            RROR rrOr = calcRROR(a, b, c, d);
            Double pFisher = fisherTwoSidedP(a, b, c, d);

            out.add(new FoodAttackRow(
                    key, label, a, b, c, d,
                    arExp, arNoExp,
                    rrOr.rr, rrOr.rrL, rrOr.rrH,
                    rrOr.or, rrOr.orL, rrOr.orH,
                    pFisher
            ));
        }

        // Orden sugerida: RR desc
        out.sort((r1, r2) -> Double.compare(safeForSort(r2.rr), safeForSort(r1.rr)));
        return out;
    }

    /* ===================== Lógica de "es caso" (ajústala si quieres) ===================== */

    private boolean esCaso(String idExpuesto, Usuario actor) throws SQLException {
        var optG = sintomasService.findGeneralesByExpuestoVisiblePara(idExpuesto, actor);
        if (optG.isEmpty()) return false;
        var g = optG.get();

        boolean tieneInicio = !isBlank(g.getFechaInicioConjunto());
        int numSintomas = 0;
        try {
            var expos = sintomasService.listExposiciones(g.getIdSintomasGenerales(), actor);
            numSintomas = (expos == null) ? 0 : expos.size();
        } catch (Exception ignore) {}
        boolean tieneAlguno = numSintomas > 0;

        // Regla: caso = tiene fecha de inicio y ≥1 síntoma
        return tieneInicio && tieneAlguno;
    }

    /* ===================== Utilidades epidemiológicas ===================== */

    private static final class RROR {
        final Double rr, rrL, rrH;
        final Double or, orL, orH;
        RROR(Double rr, Double rrL, Double rrH, Double or, Double orL, Double orH) {
            this.rr = rr; this.rrL = rrL; this.rrH = rrH;
            this.or = or; this.orL = orL; this.orH = orH;
        }
    }

    /** RR/OR + IC95% con corrección de Haldane-Anscombe cuando hay ceros. */
    private static RROR calcRROR(int a, int b, int c, int d) {
        double aa = a, bb = b, cc = c, dd = d;
        boolean anyZero = (a == 0 || b == 0 || c == 0 || d == 0);
        if (anyZero) { aa += 0.5; bb += 0.5; cc += 0.5; dd += 0.5; }

        double rr = (aa / (aa + bb)) / (cc / (cc + dd));
        double seLogRR = Math.sqrt( (1.0/aa) - (1.0/(aa+bb)) + (1.0/cc) - (1.0/(cc+dd)) );
        double rrL = Math.exp(Math.log(rr) - 1.96 * seLogRR);
        double rrH = Math.exp(Math.log(rr) + 1.96 * seLogRR);

        double or = (aa * dd) / (bb * cc);
        double seLogOR = Math.sqrt(1.0/aa + 1.0/bb + 1.0/cc + 1.0/dd);
        double orL = Math.exp(Math.log(or) - 1.96 * seLogOR);
        double orH = Math.exp(Math.log(or) + 1.96 * seLogOR);

        return new RROR(rr, rrL, rrH, or, orL, orH);
    }

    /** Fisher exacta bilateral para 2x2 sin librerías externas. */
    private static Double fisherTwoSidedP(int a, int b, int c, int d) {
        int r1 = a + b, r2 = c + d, c1 = a + c, c2 = b + d, n = r1 + r2;
        if (n == 0) return null;

        int minA = Math.max(0, c1 - r2);
        int maxA = Math.min(c1, r1);

        double pObs = hypergeomProb(a, r1, c1, n);
        double pTwo = 0.0;

        for (int k = minA; k <= maxA; k++) {
            double p = hypergeomProb(k, r1, c1, n);
            if (p <= pObs + 1e-12) pTwo += p; // bilateral "clásico"
        }
        return Math.min(1.0, pTwo);
    }

    // P(A=k) ~ Hypergeom: C(c1, k) * C(c2, r1-k) / C(n, r1)
    private static double hypergeomProb(int k, int r1, int c1, int n) {
        int c2 = n - c1;
        if (k < 0 || k > c1 || (r1 - k) < 0 || (r1 - k) > c2) return 0.0;
        double logP = logChoose(c1, k) + logChoose(c2, r1 - k) - logChoose(n, r1);
        return Math.exp(logP);
    }

    // log(C(n,k)) con log-factoriales (cache simple).
    private static final Map<Integer, Double> LOGFACT = new HashMap<>();
    private static double logChoose(int n, int k) {
        if (k < 0 || k > n) return Double.NEGATIVE_INFINITY;
        return logFact(n) - logFact(k) - logFact(n - k);
    }
    private static double logFact(int n) {
        if (n < 0) return Double.NaN;
        return LOGFACT.computeIfAbsent(n, EstadisticaService::lf);
    }
    private static double lf(int n) {
        if (n < 2) return 0.0;
        double x = 0.0;
        for (int i = 2; i <= n; i++) x += Math.log(i);
        return x;
    }

    /* ===================== Helpers ===================== */

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static double ratio(int num, int den) { return den == 0 ? Double.NaN : (double) num / den; }
    private static double safeForSort(Double d) { return (d == null || Double.isNaN(d)) ? Double.NEGATIVE_INFINITY : d; }
    private static String norm(String s) {
        if (s == null) return "";
        String t = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return t.trim().toUpperCase(Locale.ROOT);
    }

    /** Texto plano para insertar en el contenido del Informe (RF6). */
    public String resumenTextoParaInforme(String idBrote, Usuario actor) throws SQLException {
        ResumenBrote r = calcularResumenBrote(idBrote, actor);

        StringBuilder sb = new StringBuilder();
        sb.append("Expuestos totales: ").append(r.totalExpuestos).append('\n');
        sb.append("Expuestos con síntomas: ").append(r.expuestosConSintomas).append('\n');
        sb.append("Casos (definición por defecto): ").append(r.casosDefinidos).append('\n');

        if (!r.conteoSintomas.isEmpty()) {
            sb.append("\nSíntomas más frecuentes:\n");
            r.conteoSintomas.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .forEach(e -> sb.append(" - ")
                            .append(e.getKey())
                            .append(": ")
                            .append(e.getValue())
                            .append('\n'));
        } else {
            sb.append("\nNo hay síntomas registrados.\n");
        }
        return sb.toString();
    }


    private ResumenBrote calcularResumenBrote(String idBrote, Usuario actor) throws SQLException {
        ResumenBrote out = new ResumenBrote();

        // 1) Expuestos visibles en el brote para el actor
        List<PersonaExpuesta> expuestos =
                personaExpuestaService.findByBroteIdVisiblePara(idBrote, actor);
        out.totalExpuestos = expuestos.size();

        // 2) Cache de nombres de síntoma por id para no ir a BD en bucle
        Map<String, String> nombreSintomaCache = new HashMap<>();
        java.util.function.Function<String, String> nombreSintoma = id -> nombreSintomaCache.computeIfAbsent(id, k -> {
            try {
                return sintomaService.findById(k)
                        .map(Sintoma::getNombreSintoma)
                        .orElse("(síntoma " + k + ")");
            } catch (SQLException e) {
                return "(síntoma " + k + ")";
            }
        });

        // 3) Recorrido por expuestos
        for (PersonaExpuesta pe : expuestos) {
            var genOpt = sintomasService.findGeneralesByExpuestoVisiblePara(pe.getIdExpuesto(), actor);
            if (genOpt.isEmpty()) continue;

            out.expuestosConSintomas++;

            var gen = genOpt.get();
            var expos = sintomasService.listExposiciones(gen.getIdSintomasGenerales(), actor);

            for (ExposicionSintoma e : expos) {
                String nombre = nombreSintoma.apply(e.getIdSintoma());
                out.conteoSintomas.merge(nombre, 1, Integer::sum);
            }

            // ¿Cumple definición de caso por defecto?
            if (sintomasService.esCaso(
                    pe.getIdExpuesto(),
                    SintomasGeneralesExpuestoService.CasoDef.porDefecto(),
                    actor)) {
                out.casosDefinidos++;
            }
        }

        return out;
    }



    private static class ResumenBrote {
        int totalExpuestos;
        int expuestosConSintomas;
        int casosDefinidos;
        java.util.Map<String, Integer> conteoSintomas = new java.util.HashMap<>();
    }
}
