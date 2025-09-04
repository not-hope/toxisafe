package com.toxisafe.ui.controller;

import com.toxisafe.model.Brote;
import com.toxisafe.model.Usuario;
import com.toxisafe.service.*;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class EstadisticaController {

    // --- UI ---
    @FXML private Label lblTitulo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnExportarCsv;
    @FXML private Button btnInforme;
    @FXML private TabPane tabPane;

    // Line list
    @FXML private TableView<EstadisticaService.LineListRow> tvLineList;
    @FXML private TableColumn<EstadisticaService.LineListRow, String> colLLNombre;
    @FXML private TableColumn<EstadisticaService.LineListRow, String> colLLApellido;
    @FXML private TableColumn<EstadisticaService.LineListRow, String> colLLIni;
    @FXML private TableColumn<EstadisticaService.LineListRow, String> colLLFin;
    @FXML private TableColumn<EstadisticaService.LineListRow, String> colLLNum;
    @FXML private TableColumn<EstadisticaService.LineListRow, String> colLLCaso;

    // Curva epidémica
    @FXML private BarChart<String, Number> chartEpi;
    @FXML private CategoryAxis epiXAxis;
    @FXML private NumberAxis epiYAxis;

    // Ataque por alimento
    @FXML private TableView<EstadisticaService.FoodAttackRow> tvFood;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFAlimento;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFA;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFB;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFC;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFD;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFARExp;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFARNoExp;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFRR;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFRRL;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFRRH;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFOR;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFORL;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFORH;
    @FXML private TableColumn<EstadisticaService.FoodAttackRow, String> colFP;

    // --- Estado / servicios ---
    private Usuario actor;
    private Brote brote;

    private EstadisticaService estadisticaService;
    private InformeService informeService;
    private BroteEncuestadorService broteEncuestadorService;

    private static final DecimalFormat DF3 = new DecimalFormat("#.###");

    public void init(Usuario actor,
                     Brote brote,
                     EstadisticaService estadisticaService,
                     InformeService informeService, BroteEncuestadorService broteEncuestadorService) {
        this.actor = actor;
        this.brote = brote;
        this. estadisticaService = estadisticaService;
        this.informeService = informeService;
        this.broteEncuestadorService = broteEncuestadorService;


        lblTitulo.setText("Estadística del brote: " + (brote.getNombreBrote() == null));
        configurarTablas();
        refrescarTodo();
    }

    @FXML
    private void initialize() {
        // Configuración estática del chart
        if (epiXAxis != null) epiXAxis.setLabel("Fecha de inicio (caso)");
        if (epiYAxis != null) epiYAxis.setLabel("Casos");
    }

    private void configurarTablas() {
        // Line list
        colLLNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().nombre)));
        colLLApellido.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().apellido)));
        colLLIni.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().fechaInicio)));
        colLLFin.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().fechaFin)));
        colLLNum.setCellValueFactory(c -> new ReadOnlyStringWrapper(Integer.toString(c.getValue().numSintomas)));
        colLLCaso.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().esCaso ? "Sí" : "No"));

        // Ataque por alimento
        colFAlimento.setCellValueFactory(c -> new ReadOnlyStringWrapper(nz(c.getValue().alimentoLabel)));
        colFA.setCellValueFactory(c -> new ReadOnlyStringWrapper(Integer.toString(c.getValue().a)));
        colFB.setCellValueFactory(c -> new ReadOnlyStringWrapper(Integer.toString(c.getValue().b)));
        colFC.setCellValueFactory(c -> new ReadOnlyStringWrapper(Integer.toString(c.getValue().c)));
        colFD.setCellValueFactory(c -> new ReadOnlyStringWrapper(Integer.toString(c.getValue().d)));

        colFARExp.setCellValueFactory(c -> new SimpleStringProperty(fmtRatio(c.getValue().arExpuestos)));
        colFARNoExp.setCellValueFactory(c -> new SimpleStringProperty(fmtRatio(c.getValue().arNoExpuestos)));

        colFRR.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().rr)));
        colFRRL.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().rrL)));
        colFRRH.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().rrH)));

        colFOR.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().or)));
        colFORL.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().orL)));
        colFORH.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().orH)));

        colFP.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue().pValueFisherTwoSided)));
    }

    /* ===================== Acciones ===================== */

    @FXML
    private void handleRefrescar() {
        refrescarTodo();
    }

    @FXML
    private void handleExportarCsv() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));

        String sugerido = switch (tab.getText().toLowerCase(Locale.ROOT)) {
            case "line list" -> "linelist.csv";
            case "curva epidémica" -> "curva_epidemica.csv";
            case "alimentos" -> "alimentos_ataque.csv";
            default -> "estadistica.csv";
        };
        fc.setInitialFileName(sugerido);
        File f = fc.showSaveDialog(tabPane.getScene().getWindow());
        if (f == null) return;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            if (tab.getText().equalsIgnoreCase("Line list")) {
                exportLineListCsv(bw, tvLineList.getItems());
            } else if (tab.getText().toLowerCase(Locale.ROOT).contains("curva")) {
                // reconstruir bins desde el chart
                exportCurveCsv(bw, chartEpi);
            } else {
                exportFoodCsv(bw, tvFood.getItems());
            }
            info("Exportado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            error("No se pudo exportar: " + ex.getMessage());
        }
    }

    @FXML
    private void handleAbrirInforme() {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/com/toxisafe/ui/view/informe.fxml"));
            Parent root = fx.load();
            InformeController ctrl = fx.getController();

            // Pásale exactamente lo que tu init espera
            ctrl.init(actor, brote, informeService, estadisticaService, broteEncuestadorService);

            Stage dlg = new Stage();
            dlg.setTitle("Informe — " + (brote.getNombreBrote() == null ? brote.getIdBrote() : brote.getNombreBrote()));
            dlg.initModality(Modality.APPLICATION_MODAL);
            // opcional: dlg.initOwner(miBoton.getScene().getWindow());
            dlg.setScene(new Scene(root));
            dlg.showAndWait();

        } catch (Exception e) {
            error("No se pudo abrir Informe: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void refrescarTodo() {
        try {
            // Line list
            List<EstadisticaService.LineListRow> ll = estadisticaService.buildLineList(brote.getIdBrote(), actor);
            tvLineList.getItems().setAll(ll);

            // Curva epidémica
            List<EstadisticaService.EpiBin> bins = estadisticaService.buildEpiCurveDaily(brote.getIdBrote(), actor);
            renderEpiChart(bins);

            // Alimentos
            List<EstadisticaService.FoodAttackRow> foods = estadisticaService.computeAttackByFood(brote.getIdBrote(), actor);
            tvFood.getItems().setAll(foods);

        } catch (SQLException e) {
            error("Error al calcular estadísticas: " + e.getMessage());
            tvLineList.getItems().clear();
            tvFood.getItems().clear();
            chartEpi.getData().clear();
        }
    }

    /* ===================== Render / Export helpers ===================== */

    private void renderEpiChart(List<EstadisticaService.EpiBin> bins) {
        chartEpi.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Casos por día");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (var b : bins) {
            s.getData().add(new XYChart.Data<>(df.format(b.date), b.cases));
        }
        chartEpi.getData().add(s);
    }

    private void exportLineListCsv(BufferedWriter bw, List<EstadisticaService.LineListRow> rows) throws Exception {
        bw.write("id_expuesto;nombre;apellido;fecha_inicio;fecha_fin;num_sintomas;es_caso");
        bw.newLine();
        for (var r : rows) {
            bw.write(escape(r.idExpuesto) + ";" +
                    escape(r.nombre) + ";" +
                    escape(r.apellido) + ";" +
                    escape(nz(r.fechaInicio)) + ";" +
                    escape(nz(r.fechaFin)) + ";" +
                    r.numSintomas + ";" +
                    (r.esCaso ? "1" : "0"));
            bw.newLine();
        }
    }

    private void exportCurveCsv(BufferedWriter bw, BarChart<String, Number> chart) throws Exception {
        bw.write("fecha;casi");
        bw.newLine();
        if (chart.getData().isEmpty()) return;
        for (var d : chart.getData().get(0).getData()) {
            bw.write(escape(d.getXValue()) + ";" + d.getYValue());
            bw.newLine();
        }
    }

    private void exportFoodCsv(BufferedWriter bw, List<EstadisticaService.FoodAttackRow> rows) throws Exception {
        bw.write("alimento;a;b;c;d;AR_expuestos;AR_no_expuestos;RR;RR_L;RR_H;OR;OR_L;OR_H;p_fisher");
        bw.newLine();
        for (var r : rows) {
            bw.write(escape(r.alimentoLabel) + ";" +
                    r.a + ";" + r.b + ";" + r.c + ";" + r.d + ";" +
                    fmtCsvRatio(r.arExpuestos) + ";" + fmtCsvRatio(r.arNoExpuestos) + ";" +
                    fmtCsv(r.rr) + ";" + fmtCsv(r.rrL) + ";" + fmtCsv(r.rrH) + ";" +
                    fmtCsv(r.or) + ";" + fmtCsv(r.orL) + ";" + fmtCsv(r.orH) + ";" +
                    fmtCsv(r.pValueFisherTwoSided));
            bw.newLine();
        }
    }

    /* ===================== Utils ===================== */

    private static String nz(String s) { return s == null ? "" : s; }

    private static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private static void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }

    private static String escape(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(";") || t.contains("\"") || t.contains("\n")) {
            return "\"" + t + "\"";
        }
        return t;
    }

    private static String fmt(Double d) {
        if (d == null || d.isNaN() || d.isInfinite()) return "";
        return DF3.format(d);
    }
    private static String fmtCsv(Double d) { return fmt(d); }

    private static String fmtRatio(double r) {
        if (Double.isNaN(r) || Double.isInfinite(r)) return "";
        return DF3.format(r); // si prefieres %, usa `DF3.format(r*100) + "%"`
    }
    private static String fmtCsvRatio(double r) { return fmtRatio(r); }
}
