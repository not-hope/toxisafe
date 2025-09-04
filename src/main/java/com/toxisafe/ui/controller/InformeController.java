package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.Informe;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.BroteEncuestadorService;
import com.toxisafe.service.EstadisticaService;
import com.toxisafe.service.InformeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InformeController {

    // Header
    @FXML private Label lblTitulo;
    @FXML private Label lblEstadoBrote;

    // Listado de informes previos y editor
    @FXML private ListView<Informe> lvInformes;
    @FXML private TextArea txtContenido;

    // Botonera
    @FXML private Button btnGenerar;
    @FXML private Button btnGuardarNuevo;
    @FXML private Button btnActualizarSel;
    @FXML private Button btnEliminarSel;
    @FXML private Button btnExportarTxt;
    @FXML private Button btnCerrar;

    // Estado/Servicios
    private Usuario actor;
    private Brote brote;
    private InformeService informeService;
    private EstadisticaService estadisticaService;
    private BroteEncuestadorService broteEncuestadorService;

    private final ObservableList<Informe> informes = FXCollections.observableArrayList();
    private boolean puedeGestionar = false; // según rol/asignación

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    /* =====================  Inyección de contexto  ===================== */

    /**
     * Llamar justo tras cargar el FXML.
     */
    public void init(Usuario actor,
                     Brote brote,
                     InformeService informeService,
                     EstadisticaService estadisticaService,
                     BroteEncuestadorService broteEncuestadorService) {
        this.actor = Objects.requireNonNull(actor);
        this.brote = Objects.requireNonNull(brote);
        this.informeService = Objects.requireNonNull(informeService);
        this.estadisticaService = Objects.requireNonNull(estadisticaService);
        this.broteEncuestadorService = Objects.requireNonNull(broteEncuestadorService);

        lblTitulo.setText("Informe — Brote: " + brote.getNombreBrote());
        lblEstadoBrote.setText(brote.getEstadoBrote());

        // Permisos: ADMIN/EPIDEMIOLOGO/MIR -> sí; ENCUESTADOR sólo si está asignado
        String rol = safe(actor.getRolUsuario());
        if (rol.equals("ADMIN") || rol.equals("EPIDEMIOLOGO") || rol.equals("MIR_SALUD_PUBLICA")) {
            puedeGestionar = true;
        } else if (rol.equals("ENCUESTADOR")) {
            try {
                puedeGestionar = broteEncuestadorService.findByUsuarioId(actor.getIdUsuario())
                        .stream().anyMatch(be -> brote.getIdBrote().equals(be.getIdBrote()));
            } catch (Exception e) {
                puedeGestionar = false;
            }
        }

        configurarLista();
        cargarInformes();
        refrescarBotones();
    }

    /* =====================  Setup UI  ===================== */

    private void configurarLista() {
        lvInformes.setItems(informes);
        lvInformes.setPlaceholder(new Label("Sin informes previos"));
        lvInformes.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Informe it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) { setText(null); return; }
                // Primera línea del contenido como título
                String first = Optional.ofNullable(it.getContenidoInforme())
                        .map(s -> s.replace("\r",""))
                        .map(s -> s.contains("\n") ? s.substring(0, s.indexOf('\n')) : s)
                        .orElse("(sin contenido)");
                setText(first);
            }
        });
        lvInformes.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) txtContenido.setText(Optional.ofNullable(b.getContenidoInforme()).orElse(""));
            refrescarBotones();
        });
    }

    private void cargarInformes() {
        try {
            informes.setAll(informeService.findByBroteIdVisiblePara(brote.getIdBrote(), actor));
        } catch (Exception e) {
            error("No se pudieron cargar los informes: " + e.getMessage());
            informes.clear();
        }
    }

    private void refrescarBotones() {
        boolean haySel = lvInformes.getSelectionModel().getSelectedItem() != null;
        btnGenerar.setDisable(!puedeGestionar);
        btnGuardarNuevo.setDisable(!puedeGestionar);
        btnActualizarSel.setDisable(!puedeGestionar || !haySel);
        btnEliminarSel.setDisable(!puedeGestionar || !haySel);
        // Exportar y Cerrar siempre disponibles
    }

    /* =====================  Acciones  ===================== */

    @FXML
    private void handleGenerarResumen() {
        try {
            String resumen = estadisticaService.resumenTextoParaInforme(brote.getIdBrote(), actor);
            String encabezado = "Informe epidemiológico — " + brote.getNombreBrote()
                    + " (" + TS.format(LocalDateTime.now()) + ")\n\n";
            txtContenido.setText(encabezado + resumen);
        } catch (Exception e) {
            error("No se pudo generar el resumen: " + e.getMessage());
        }
    }

    @FXML
    private void handleGuardarNuevo() {
        if (!puedeGestionar) return;
        String contenido = Optional.ofNullable(txtContenido.getText()).orElse("").trim();
        if (contenido.isEmpty()) { info("No hay contenido que guardar."); return; }
        Informe inf = new Informe(UUID.randomUUID().toString(), brote.getIdBrote(), contenido);
        try {
            informeService.create(inf, actor);
            cargarInformes();
            seleccionar(inf.getIdInforme());
            info("Informe guardado.");
        } catch (Exception e) {
            error("No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    private void handleActualizarSeleccionado() {
        if (!puedeGestionar) return;
        Informe sel = lvInformes.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        sel.setContenidoInforme(Optional.ofNullable(txtContenido.getText()).orElse(""));
        try {
            informeService.update(sel, actor);
            cargarInformes();
            seleccionar(sel.getIdInforme());
            info("Informe actualizado.");
        } catch (Exception e) {
            error("No se pudo actualizar: " + e.getMessage());
        }
    }

    @FXML
    private void handleEliminarSeleccionado() {
        if (!puedeGestionar) return;
        Informe sel = lvInformes.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el informe seleccionado?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    informeService.delete(sel.getIdInforme(), actor);
                    cargarInformes();
                    txtContenido.clear();
                } catch (Exception e) {
                    error("No se pudo eliminar: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleExportarTxt() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar informe (TXT)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
        fc.setInitialFileName("informe_" + brote.getNombreBrote() + "_" + TS.format(LocalDateTime.now()) + ".txt");
        File f = fc.showSaveDialog(txtContenido.getScene().getWindow());
        if (f != null) {
            try {
                Files.writeString(f.toPath(), Optional.ofNullable(txtContenido.getText()).orElse(""),
                        StandardCharsets.UTF_8);
                info("Exportado en: " + f.getAbsolutePath());
            } catch (Exception e) {
                error("No se pudo exportar: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportar() {
        String contenido = Optional.ofNullable(txtContenido.getText()).orElse("");
        if (contenido.isEmpty()) {
            info("No hay contenido para exportar");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar informe");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("Word (*.docx)", "*.docx"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv")
        );
        fc.setInitialFileName("Informe_" + brote.getNombreBrote() + ".pdf");

        File f = fc.showSaveDialog(txtContenido.getScene().getWindow());
        if (f == null) return;

        // Asegura extensión acorde al filtro escogido si el usuario no la puso
        String name = f.getName().toLowerCase();
        String path = f.getAbsolutePath();
        if (!name.contains(".")) {
            FileChooser.ExtensionFilter sel = fc.getSelectedExtensionFilter();
            if (sel != null && !sel.getExtensions().isEmpty()) {
                String pattern = sel.getExtensions().get(0); // p.ej. "*.pdf"
                String ext = pattern.replace("*", "");       // ".pdf"
                path += ext;
                f = new File(path);
            }
        }

        try {
            if (path.endsWith(".pdf")) {
                exportPdf(f.toPath(), contenido);
            } else if (path.endsWith(".txt")) {
                exportTxt(f.toPath(), contenido);
            } else if (path.endsWith(".docx")) {
                exportDocx(f.toPath(), contenido);
            } else if (path.endsWith(".csv")) {
                exportCsv(f.toPath(), contenido);
            } else {
                // Por defecto, texto
                exportTxt(f.toPath(), contenido);
            }
            info("Exportado en: " + f.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            error("No se pudo exportar: " + e.getMessage());
        }
    }


    @FXML
    private void handleCerrar() {
        // Cierra la ventana actual
        txtContenido.getScene().getWindow().hide();
    }

    /* =====================  Utilidades  ===================== */

    private void seleccionar(String idInforme) {
        if (idInforme == null) return;
        for (Informe i : lvInformes.getItems()) {
            if (idInforme.equals(i.getIdInforme())) {
                lvInformes.getSelectionModel().select(i);
                lvInformes.scrollTo(i);
                break;
            }
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        String up = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return up.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }

    // --- TXT ---
    private static void exportTxt(java.nio.file.Path p, String contenido) throws java.io.IOException {
        java.nio.file.Files.writeString(p, contenido, java.nio.charset.StandardCharsets.UTF_8);
    }

    // --- PDF (PDFBox) ---
    private static void exportPdf(java.nio.file.Path p, String contenido) throws Exception {
        // Dependencia: org.apache.pdfbox:pdfbox
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
        doc.addPage(page);

        float margin = 50f;
        float yStart = page.getMediaBox().getHeight() - margin;
        float width = page.getMediaBox().getWidth() - 2 * margin;
        float leading = 1.4f;

        // Fuente: incrusta DejaVuSans si la tienes en resources, si no usa Helvetica
        org.apache.pdfbox.pdmodel.font.PDFont font;
        float fontSize = 12f;
        try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/DejaVuSans.ttf")) {
            if (is != null) {
                font = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, is, true);
            } else {
                font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
            }
        }

        java.util.List<String> lines = wrapForWidth(contenido, font, fontSize, width);

        org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(margin, yStart);

        float lineHeight = fontSize * leading;
        float y = yStart;
        for (String ln : lines) {
            if (y - lineHeight < margin) {
                cs.endText(); cs.close();
                page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
                doc.addPage(page);
                cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
                cs.beginText();
                cs.setFont(font, fontSize);
                y = page.getMediaBox().getHeight() - margin;
                cs.newLineAtOffset(margin, y);
            }
            cs.showText(ln.replace("\t", "    "));
            cs.newLineAtOffset(0, -lineHeight);
            y -= lineHeight;
        }

        cs.endText();
        cs.close();
        doc.save(p.toFile());
        doc.close();
    }

    private static java.util.List<String> wrapForWidth(String text,
                                                       org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float width) throws java.io.IOException {

        java.util.List<String> out = new java.util.ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isEmpty()) { out.add(""); continue; }
            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                String test = line.length()==0 ? w : line + " " + w;
                float sz = font.getStringWidth(test) / 1000 * fontSize;
                if (sz > width && line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                } else {
                    line.setLength(0);
                    line.append(test);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    // --- DOCX (Apache POI) ---
    private static void exportDocx(java.nio.file.Path p, String contenido) throws Exception {
        // Dependencia: org.apache.poi:poi-ooxml
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            for (String line : contenido.split("\\R", -1)) {
                org.apache.poi.xwpf.usermodel.XWPFParagraph para = doc.createParagraph();
                org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
                run.setFontFamily("Calibri");
                run.setFontSize(11);
                if (line.isEmpty()) run.addCarriageReturn(); else run.setText(line);
            }
            try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(p)) {
                doc.write(os);
            }
        }
    }

    private static void exportCsv(java.nio.file.Path p, String contenido) throws java.io.IOException {
        // Cada línea del área de texto será una fila del CSV
        // con una única columna llamada "linea"
        StringBuilder sb = new StringBuilder();
        sb.append("linea").append("\r\n"); // cabecera

        for (String line : contenido.split("\\R", -1)) {
            sb.append('"')
                    .append(line.replace("\"", "\"\"")) // escapar comillas
                    .append('"')
                    .append("\r\n");
        }
        java.nio.file.Files.writeString(p, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

}
