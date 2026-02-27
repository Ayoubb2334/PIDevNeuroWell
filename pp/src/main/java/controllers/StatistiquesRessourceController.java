package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import services.ServiceRessource;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard de statistiques des consultations de ressources.
 * Affiché via StatistiquesRessource.fxml
 */
public class StatistiquesRessourceController {

    // ── KPI cards ─────────────────────────────────────────────
    @FXML private Label kpiTotalVues;
    @FXML private Label kpiVues7j;
    @FXML private Label kpiVues30j;
    @FXML private Label kpiTopRessource;

    // ── Graphiques ────────────────────────────────────────────
    @FXML private BarChart<String, Number>  chartParJour;
    @FXML private PieChart                  chartParType;
    @FXML private BarChart<String, Number>  chartTopRessources;

    // ── Labels de chargement ──────────────────────────────────
    @FXML private VBox loadingPane;

    private ServiceRessource serviceRessource;

    private static final String CYAN   = "#00D9FF";
    private static final String GREEN  = "#00FF88";
    private static final String DARK   = "#050C07";

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        serviceRessource = new ServiceRessource();
        loadStatsAsync();
    }

    // ── Chargement asynchrone pour ne pas bloquer l'UI ────────
    private void loadStatsAsync() {
        new Thread(() -> {
            try {
                // 1. KPIs globaux
                int total   = serviceRessource.getTotalConsultations();
                int vues7j  = sumVuesDerniersJours(7);
                int vues30j = sumVuesDerniersJours(30);

                // 2. Top ressources (seulement celles avec au moins 1 vue)
                List<Object[]> top = serviceRessource.getTopRessources(7);
                // Filtrer les ressources sans vue (total = 0 car LEFT JOIN)
                List<Object[]> topAvecVues = top.stream()
                        .filter(row -> (int) row[3] > 0)
                        .collect(java.util.stream.Collectors.toList());
                String topTitre = topAvecVues.isEmpty() ? "Aucune consultation"
                        : (String) topAvecVues.get(0)[1] + " (" + topAvecVues.get(0)[3] + " vues)";

                // 3. Données graphiques
                List<Object[]> parJour = serviceRessource.getVuesParJour(30);
                List<Object[]> parType = serviceRessource.getVuesParType();

                // Mise à jour sur le thread JavaFX
                Platform.runLater(() -> {
                    // KPIs
                    kpiTotalVues.setText(String.valueOf(total));
                    kpiVues7j.setText(String.valueOf(vues7j));
                    kpiVues30j.setText(String.valueOf(vues30j));
                    kpiTopRessource.setText(topTitre);

                    // Graphique 1 — vues par jour (30 derniers jours)
                    buildBarChartParJour(parJour);

                    // Graphique 2 — répartition par type (PieChart)
                    buildPieChartParType(parType);

                    // Graphique 3 — top 7 ressources (avec vues uniquement)
                    buildBarChartTop(topAvecVues);

                    if (loadingPane != null) loadingPane.setVisible(false);
                });

            } catch (SQLException e) {
                Platform.runLater(() ->
                    kpiTotalVues.setText("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    /** Somme des consultations des N derniers jours (toutes ressources). */
    private int sumVuesDerniersJours(int jours) throws SQLException {
        List<Object[]> rows = serviceRessource.getVuesParJour(jours);
        return rows.stream().mapToInt(r -> (int) r[1]).sum();
    }

    // ── Graphique : vues par jour ─────────────────────────────
    private void buildBarChartParJour(List<Object[]> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");
        for (Object[] row : data) {
            String date = (String) row[0];
            int    cnt  = (int)   row[1];
            // Afficher juste MM-dd pour lisibilité
            String label = date.length() >= 10 ? date.substring(5) : date;
            series.getData().add(new XYChart.Data<>(label, cnt));
        }
        chartParJour.getData().clear();
        chartParJour.getData().add(series);
        styleBarChart(chartParJour);
    }

    // ── PieChart : vues par type ──────────────────────────────
    private void buildPieChartParType(List<Object[]> data) {
        chartParType.getData().clear();
        for (Object[] row : data) {
            String type = (String) row[0];
            int    cnt  = (int)   row[1];
            if (cnt > 0) {
                chartParType.getData().add(new PieChart.Data(type + " (" + cnt + ")", cnt));
            }
        }
        chartParType.setStyle("-fx-background-color: #071A10;");
        chartParType.setLabelsVisible(true);
        chartParType.setLegendVisible(true);
    }

    // ── Graphique : top 7 ressources ──────────────────────────
    private void buildBarChartTop(List<Object[]> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vues totales");
        for (Object[] row : data) {
            String titre = (String) row[1];
            int    cnt   = (int)   row[3];
            // Tronquer les titres longs
            String label = titre.length() > 18 ? titre.substring(0, 16) + "…" : titre;
            series.getData().add(new XYChart.Data<>(label, cnt));
        }
        chartTopRessources.getData().clear();
        chartTopRessources.getData().add(series);
        styleBarChart(chartTopRessources);
    }

    private void styleBarChart(BarChart<String, Number> chart) {
        chart.setStyle(
            "-fx-background-color: #071A10; " +
            "-fx-plot-background-color: #050C07;"
        );
        chart.setLegendVisible(false);
        chart.setAnimated(false);
    }

    // ── Bouton Actualiser ─────────────────────────────────────
    @FXML
    private void handleRefresh() {
        kpiTotalVues.setText("…");
        kpiVues7j.setText("…");
        kpiVues30j.setText("…");
        kpiTopRessource.setText("…");
        loadStatsAsync();
    }
}
