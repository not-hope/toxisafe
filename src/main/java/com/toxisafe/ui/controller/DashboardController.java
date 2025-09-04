package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.BroteEncuestador;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.BroteEncuestadorService;
import com.toxisafe.service.BroteService;
import com.toxisafe.service.PersonaExpuestaService;
import com.toxisafe.service.UsuarioService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.sql.SQLException;
import java.util.*;

public class DashboardController {

    // Header

    // Cards
    @FXML private Label lblAsignadosActivos;
    @FXML private Label lblAsignadosCerrados;
    @FXML private Label lblCreados;
    @FXML private Label lblResponsable;
    @FXML private StackPane cardCreados;
    @FXML private StackPane cardResponsable;

    // Tabla
    @FXML private TableView<BroteRow> tblAsignados;
    @FXML private TableColumn<BroteRow,String> colNombre;
    @FXML private TableColumn<BroteRow, String> colFechaIni;
    @FXML private TableColumn<BroteRow, String> colResponsable;
    @FXML private TableColumn<BroteRow, String> colCreador;
    @FXML private TableColumn<BroteRow,String> colEstado;
    @FXML private TableColumn<BroteRow,Number> colExpuestos;

    // Services
    private BroteService broteService;
    private BroteEncuestadorService broteEncuestadorService;
    private PersonaExpuestaService personaExpuestaService;
    private UsuarioService usuarioService;
    private Usuario actor;
    private final Map<String, Usuario> usuariosCache = new HashMap<>();

    /** Carga el FXML y devuelve el root ya inicializado para incrustarlo en MainController. */

    public void init(Usuario actor,
                     BroteService broteService,
                     BroteEncuestadorService broteEncuestadorService,
                     PersonaExpuestaService personaExpuestaService,
                     UsuarioService usuarioService) {
        this.actor = Objects.requireNonNull(actor);
        this.broteService = Objects.requireNonNull(broteService);
        this.broteEncuestadorService = Objects.requireNonNull(broteEncuestadorService);
        this.personaExpuestaService = Objects.requireNonNull(personaExpuestaService);
        this.usuarioService = Objects.requireNonNull(usuarioService);

        configurarTabla();
        refrescar();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre()));
        colFechaIni.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fechaIniBrote()));
        colResponsable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().responsable()));
        colCreador.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().creador()));
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().estado()));
        colExpuestos.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().expuestos()));
    }

    private String resolveNombreUsuario(String id) {
        if (id == null || id.isBlank()) return "";
        Usuario u = usuariosCache.get(id);
        if (u == null) {
            try {
                u = usuarioService.findById(id).orElse(null);
                if (u != null) {
                    usuariosCache.put(id, u);
                }
            } catch (Exception ignore) {
                // deja pasar y usa el fallback
            }
        }
        if (u == null) return id; // fallback: aún no resuelto
        String nombre = safe(u.getNombreUsuario());
        String user   = safe(u.getUsernameUsuario());
        String full   = (nombre).trim();
        return full.isEmpty() ? user : full + (user.isEmpty() ? "" : " (" + user + ")");
    }

    private void refrescar() {
        try {
            String r = normalizeRol(actor.getRolUsuario());

            // Asignados
            List<Brote> asignados = brotesAsignados(actor.getIdUsuario());
            long activos  = asignados.stream().filter(this::esActivo).count();
            long cerrados = asignados.size() - activos;
            lblAsignadosActivos.setText(String.valueOf(activos));
            lblAsignadosCerrados.setText(String.valueOf(cerrados));

            // Visibilidad de tarjetas extra
            boolean showExtra = r.equals("ADMIN") || r.equals("EPIDEMIOLOGO") || r.equals("MIR_SALUD_PUBLICA");
            cardCreados.setVisible(showExtra);     cardCreados.setManaged(showExtra);
            cardResponsable.setVisible(showExtra); cardResponsable.setManaged(showExtra);

            if (showExtra) {
                lblCreados.setText(String.valueOf(brotesCreadosPor(actor.getIdUsuario()).size()));
                lblResponsable.setText(String.valueOf(brotesResponsableDe(actor.getIdUsuario()).size()));
            }

            // Detalle tabla
            List<BroteRow> rows = new ArrayList<>();
            for (Brote b : asignados) {
                int n = 0;
                try { n = personaExpuestaService.findByBroteIdVisiblePara(b.getIdBrote(), actor).size(); }
                catch (Exception ignore) {}
                rows.add(new BroteRow(nombreBrote(b), formateaFecha(b.getFechIniBrote()), resolveNombreUsuario(b.getResponsableBrote()), resolveNombreUsuario(b.getCreadorBrote()), safe(b.getEstadoBrote()), n));
            }
            rows.sort(Comparator.comparing(BroteRow::nombre));
            tblAsignados.setItems(FXCollections.observableArrayList(rows));

        } catch (Exception e) {
            e.printStackTrace();
            lblAsignadosActivos.setText("0");
            lblAsignadosCerrados.setText("0");
            lblCreados.setText("0");
            lblResponsable.setText("0");
            tblAsignados.setItems(FXCollections.observableArrayList());
        }
    }

    /* ===== Helpers ===== */

    private List<Brote> brotesAsignados(String idUsuario) throws SQLException {
        List<BroteEncuestador> asign = broteEncuestadorService.findByUsuarioId(idUsuario);
        List<String> ids = asign.stream().map(BroteEncuestador::getIdBrote).distinct().toList();
        List<Brote> out = new ArrayList<>();
        for (String id : ids) { broteService.findById(id).ifPresent(out::add); }
        return out;
    }

    private List<Brote> brotesCreadosPor(String idUsuario) throws SQLException {
        return broteService.findByCreador(idUsuario);
    }

    private List<Brote> brotesResponsableDe(String idUsuario) throws SQLException {
        return broteService.findByResponsable(idUsuario);
    }

    private boolean esActivo(Brote b) {
        return b != null && b.getEstadoBrote() != null &&
                b.getEstadoBrote().trim().equalsIgnoreCase("ACTIVO");
    }

    private static String nombreBrote(Brote b) {
        if (b == null) return "";
        String n = b.getNombreBrote();
        return (n == null || n.isBlank()) ? b.getIdBrote() : n;
    }
    private static String safe(String s){ return s==null ? "" : s; }

    private static String formateaFecha(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            var in = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            var dt = java.time.LocalDateTime.parse(raw, in);
            return dt.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignore) {
            // Si ya viene como yyyy-MM-dd, corta a 10
            return raw.length() >= 10 ? raw.substring(0, 10) : raw;
        }
    }

    private static String normalizeRol(String r){
        if (r==null) return "";
        String up = java.text.Normalizer.normalize(r, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    /* ===== DTO fila tabla ===== */
    public static final class BroteRow {
        private final String nombre, estado, fechaIniBrote, creador, responsable;
        private final int expuestos;
        public BroteRow(String nombre, String fechaIniBrote, String responsable, String creador, String estado, int expuestos){
            this.nombre = nombre;
            this.creador = creador;
            this.responsable = responsable;
            this.estado = estado;
            this.expuestos = expuestos;
            this.fechaIniBrote = fechaIniBrote;
        }
        public String nombre(){ return nombre; }
        public String estado(){ return estado; }
        public int expuestos(){ return expuestos; }
        public String fechaIniBrote(){ return fechaIniBrote; }
        public String responsable(){ return responsable; }
        public String creador(){ return creador; }
    }
}

