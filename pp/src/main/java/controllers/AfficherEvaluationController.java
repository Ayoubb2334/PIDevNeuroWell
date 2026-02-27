package controllers;

import entities.Evaluation;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import services.ServiceEvaluation;

import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║   NEUROWELL — Afficher Évaluations (version avancée)        ║
 * ║   📊 PieChart   — Répartition par type de test              ║
 * ║   📊 BarChart   — Distribution des niveaux                  ║
 * ║   📊 LineChart  — Évolution du score dans le temps          ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class AfficherEvaluationController {

    // ── Palette ──
    private static final String C_CYAN     = "#00D9FF";
    private static final String C_GREEN    = "#00FF88";
    private static final String C_DARK_BG  = "#050C07";
    private static final String C_CARD     = "#0D1F12";
    private static final String C_CARD2    = "#0A1A0F";
    private static final String C_SURFACE  = "#112016";
    private static final String C_TEXT     = "#E8FFF0";
    private static final String C_TEXT_DIM = "rgba(200,255,220,0.55)";
    private static final String C_DANGER   = "#FF4D6D";
    private static final String C_WARN     = "#FFB347";
    private static final String C_BORDER   = "rgba(0,217,255,0.18)";

    // ── FXML — Tableau ──
    @FXML private TableView<Evaluation>            tableEvaluations;
    @FXML private TableColumn<Evaluation, String>  typeTestCol;
    @FXML private TableColumn<Evaluation, Integer> scoreCol;
    @FXML private TableColumn<Evaluation, String>  niveauCol;
    @FXML private TableColumn<Evaluation, Date>    dateCol;
    @FXML private TableColumn<Evaluation, Void>    actionCol;

    @FXML private Button    btnRefresh;
    @FXML private Button    btnSearch;
    @FXML private Button    btnStats;       // ← bouton "Voir Statistiques"
    @FXML private TextField searchField;
    @FXML private Label     totalLabel;

    // ── Service ──
    private final ServiceEvaluation serviceEvaluation = new ServiceEvaluation();

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        addTableGlow();
        setupColumns();
        setupNiveauColumn();
        setupScoreColumn();
        setupDateColumn();
        setupActionsColumn();
        tableEvaluations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadData();

        if (btnRefresh != null) btnRefresh.setOnAction(e -> { loadData(); pulseButton(btnRefresh); });
        if (btnSearch  != null) btnSearch.setOnAction(e -> filterEvaluations());
        if (searchField != null) searchField.setOnAction(e -> filterEvaluations());

        // ── Bouton Statistiques ──────────────────────────────
        if (btnStats != null) {
            btnStats.setOnAction(e -> { pulseButton(btnStats); showStatisticsDialog(); });
        }
    }

    // ── Glow table ──────────────────────────────────────────
    private void addTableGlow() {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(C_CYAN, 0.12));
        glow.setRadius(30);
        glow.setSpread(0.05);
        tableEvaluations.setEffect(glow);
    }

    // ═══════════════════════════════════════════════════════
    //  COLONNES
    // ═══════════════════════════════════════════════════════
    private void setupColumns() {
        typeTestCol.setCellValueFactory(new PropertyValueFactory<>("typeTest"));
        typeTestCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px; " +
                         "-fx-font-weight: bold; -fx-padding: 6 12;");
            }
        });
    }

    private void setupScoreColumn() {
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                String color, bg;
                if      (item < 8)  { color = C_GREEN;  bg = "rgba(0,255,136,0.12)"; }
                else if (item <= 14){ color = C_WARN;   bg = "rgba(255,179,71,0.12)"; }
                else                { color = C_DANGER; bg = "rgba(255,77,109,0.12)"; }

                Label lbl = new Label(String.valueOf(item));
                lbl.setStyle(
                    "-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px; " +
                    "-fx-background-color: " + bg + "; " +
                    "-fx-padding: 4 14; -fx-background-radius: 20;"
                );
                setGraphic(lbl); setText(null);
            }
        });
    }

    private void setupNiveauColumn() {
        niveauCol.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        niveauCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                String emoji, color, bg;
                switch (item) {
                    case "Faible" -> { emoji = "✔"; color = C_GREEN;  bg = "rgba(0,255,136,0.14)"; }
                    case "Moyen"  -> { emoji = "⏳"; color = C_WARN;   bg = "rgba(255,179,71,0.14)"; }
                    case "Élevé"  -> { emoji = "✖"; color = C_DANGER; bg = "rgba(255,77,109,0.14)"; }
                    default       -> { emoji = "•";  color = C_CYAN;   bg = "rgba(0,217,255,0.10)"; }
                }

                Label badge = new Label(emoji + "  " + item);
                badge.setStyle(
                    "-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-background-color: " + bg + "; " +
                    "-fx-padding: 5 14; -fx-background-radius: 30; " +
                    "-fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 30;"
                );

                if ("Faible".equals(item)) {
                    FadeTransition pulse = new FadeTransition(Duration.millis(900), badge);
                    pulse.setFromValue(0.7); pulse.setToValue(1.0);
                    pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
                    pulse.play();
                }
                setGraphic(badge); setText(null);
            }
        });
    }

    private void setupDateColumn() {
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateEvaluation"));
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy");
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label("📅 " + fmt.format(item));
                lbl.setStyle("-fx-text-fill: rgba(200,255,220,0.70); -fx-font-size: 12px;");
                setGraphic(lbl); setText(null);
            }
        });
    }

    private void setupActionsColumn() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit   = makeIconBtn("✏", C_CYAN,   "Modifier");
            private final Button btnDelete = makeIconBtn("🗑", C_DANGER, "Supprimer");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> openEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> {
                    Evaluation ev = getTableView().getItems().get(getIndex());
                    if (confirmDialog("Supprimer l'évaluation",
                            "Voulez-vous supprimer l'évaluation « " + ev.getTypeTest() + " » ?")) {
                        try {
                            serviceEvaluation.supprimer(ev);
                            tableEvaluations.getItems().remove(ev);
                            updateTotal();
                            showModernAlert("Succès ✔", "Évaluation supprimée avec succès !", Alert.AlertType.INFORMATION);
                        } catch (SQLException ex) {
                            showModernAlert("Erreur", "Impossible de supprimer l'évaluation", Alert.AlertType.ERROR);
                        }
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    //  DONNÉES
    // ═══════════════════════════════════════════════════════
    private void loadData() {
        try {
            ObservableList<Evaluation> list =
                    FXCollections.observableArrayList(serviceEvaluation.recuperer());
            tableEvaluations.setItems(list);
            animateTableLoad();
            updateTotal();
        } catch (SQLException e) {
            showModernAlert("Erreur", "Impossible de charger les évaluations", Alert.AlertType.ERROR);
        }
    }

    private void filterEvaluations() {
        if (searchField == null) return;
        String q = searchField.getText().trim().toLowerCase();
        try {
            ObservableList<Evaluation> filtered = FXCollections.observableArrayList(
                serviceEvaluation.recuperer().stream()
                    .filter(ev -> (ev.getTypeTest() != null && ev.getTypeTest().toLowerCase().contains(q))
                               || (ev.getNiveau()   != null && ev.getNiveau().toLowerCase().contains(q)))
                    .collect(Collectors.toList())
            );
            tableEvaluations.setItems(filtered);
            animateTableLoad();
            updateTotal();
        } catch (SQLException ex) {
            showModernAlert("Erreur", "Erreur lors de la recherche", Alert.AlertType.ERROR);
        }
    }

    private void updateTotal() {
        if (totalLabel != null)
            totalLabel.setText(String.valueOf(tableEvaluations.getItems().size()));
    }

    // ═══════════════════════════════════════════════════════
    //  STATISTIQUES VISUELLES  (Idée 1)
    //  ─ PieChart   : répartition par type
    //  ─ BarChart   : niveaux Faible / Moyen / Élevé
    //  ─ LineChart  : évolution score dans le temps
    // ═══════════════════════════════════════════════════════
    private void showStatisticsDialog() {
        List<Evaluation> data;
        try {
            data = serviceEvaluation.recuperer();
        } catch (SQLException e) {
            showModernAlert("Erreur", "Impossible de charger les données.", Alert.AlertType.ERROR);
            return;
        }
        if (data.isEmpty()) {
            showModernAlert("Info", "Aucune évaluation disponible pour les statistiques.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📊  Statistiques des évaluations");

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefSize(820, 680);
        pane.setStyle(
            "-fx-background-color: " + C_DARK_BG + "; " +
            "-fx-border-color: " + C_CYAN + "; " +
            "-fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20;"
        );
        pane.getButtonTypes().add(new ButtonType("✓  Fermer", ButtonBar.ButtonData.OK_DONE));

        // ── En-tête ──────────────────────────────────────────
        Label header = new Label("📊  Tableau de bord statistique");
        header.setStyle("-fx-text-fill: " + C_CYAN + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subHeader = new Label(data.size() + " évaluations analysées");
        subHeader.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 12px;");

        VBox headerBox = new VBox(4, header, subHeader);
        headerBox.setPadding(new Insets(20, 20, 10, 20));

        // ── Ligne 1 : PieChart + BarChart ──────────────────
        HBox row1 = new HBox(16);
        row1.setPadding(new Insets(0, 16, 0, 16));

        VBox pieBox  = buildPieChartBox(data);
        VBox barBox  = buildBarChartBox(data);
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        HBox.setHgrow(barBox, Priority.ALWAYS);
        row1.getChildren().addAll(pieBox, barBox);

        // ── Ligne 2 : LineChart ──────────────────────────────
        VBox lineBox = buildLineChartBox(data);
        lineBox.setPadding(new Insets(0, 16, 16, 16));

        // ── Résumé KPI ───────────────────────────────────────
        HBox kpiRow = buildKpiRow(data);
        kpiRow.setPadding(new Insets(0, 16, 16, 16));

        VBox content = new VBox(12, headerBox, makeSeparator(), kpiRow, row1, lineBox);
        content.setStyle("-fx-background-color: transparent;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setContent(scroll);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right, " + C_CYAN + ", " + C_GREEN + "); " +
            "-fx-text-fill: " + C_DARK_BG + "; -fx-font-weight: bold; " +
            "-fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        // Fade-in
        pane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), pane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        dialog.showAndWait();
    }

    // ── 1. PieChart — Répartition par type ──────────────────
    private VBox buildPieChartBox(List<Evaluation> data) {
        Map<String, Long> countByType = data.stream()
            .collect(Collectors.groupingBy(e -> e.getTypeTest() != null ? e.getTypeTest() : "Autre",
                     Collectors.counting()));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        countByType.forEach((type, count) ->
            pieData.add(new PieChart.Data(getTypeEmoji(type) + " " + type + " (" + count + ")", count))
        );

        PieChart pie = new PieChart(pieData);
        pie.setTitle("Par type de test");
        pie.setLegendVisible(true);
        pie.setLabelsVisible(true);
        pie.setPrefSize(340, 280);
        pie.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-chart-label-paint: #E8FFF0; " +
            "-fx-text-fill: #E8FFF0;"
        );

        // Couleurs personnalisées sur les tranches
        String[] pieColors = {"#00D9FF", "#00FF88", "#FFD700", "#FF4D6D", "#A78BFA"};
        for (int i = 0; i < pieData.size(); i++) {
            final int idx = i;
            pieData.get(i).nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + pieColors[idx % pieColors.length] + ";");
                    // Animation hover
                    node.setOnMouseEntered(e -> {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
                        st.setToX(1.08); st.setToY(1.08); st.play();
                    });
                    node.setOnMouseExited(e -> {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
                        st.setToX(1.0); st.setToY(1.0); st.play();
                    });
                }
            });
        }

        Label title = chartTitle("🥧  Répartition par type");
        VBox box = styledChartBox(title, pie);
        return box;
    }

    // ── 2. BarChart — Distribution des niveaux ──────────────
    private VBox buildBarChartBox(List<Evaluation> data) {
        Map<String, Long> countByNiveau = data.stream()
            .collect(Collectors.groupingBy(e -> e.getNiveau() != null ? e.getNiveau() : "Inconnu",
                     Collectors.counting()));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setStyle("-fx-tick-label-fill: " + C_TEXT + ";");
        yAxis.setStyle("-fx-tick-label-fill: " + C_TEXT + ";");
        yAxis.setLabel("Nombre");
        yAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.60); -fx-font-size: 10px;");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setTitle("");
        bar.setLegendVisible(false);
        bar.setPrefSize(340, 280);
        bar.setStyle("-fx-background-color: transparent;");
        bar.setBarGap(4);
        bar.setCategoryGap(20);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[]   niveaux = {"Faible", "Moyen", "Élevé"};
        String[]   colors  = {C_GREEN, C_WARN, C_DANGER};

        for (int i = 0; i < niveaux.length; i++) {
            final int idx = i;
            long val = countByNiveau.getOrDefault(niveaux[i], 0L);
            XYChart.Data<String, Number> d = new XYChart.Data<>(niveaux[i], val);
            series.getData().add(d);
            final String col = colors[i];
            final long finalVal = val;
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + col + ";");
                    // Animation de montée
                    node.setScaleY(0);
                    node.setTranslateY(node.getBoundsInParent().getHeight());
                    Timeline anim = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(node.scaleYProperty(), 0)),
                        new KeyFrame(Duration.millis(600 + 150L * idx),
                                     new KeyValue(node.scaleYProperty(), 1, Interpolator.EASE_OUT))
                    );
                    anim.play();
                    // Tooltip
                    Tooltip.install(node, new Tooltip(niveaux[idx] + " : " + finalVal));
                }
            });
        }

        bar.getData().add(series);

        Label title = chartTitle("📊  Distribution des niveaux");
        return styledChartBox(title, bar);
    }

    // ── 3. LineChart — Évolution du score dans le temps ─────
    private VBox buildLineChartBox(List<Evaluation> data) {
        // Trier par date
        List<Evaluation> sorted = data.stream()
            .filter(e -> e.getDateEvaluation() != null)
            .sorted(Comparator.comparing(e -> e.getDateEvaluation().toLocalDate()))
            .collect(Collectors.toList());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis(0, 100, 10);
        xAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.55); -fx-font-size: 9px;");
        yAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.55); -fx-font-size: 9px;");
        yAxis.setLabel("Score");

        LineChart<String, Number> line = new LineChart<>(xAxis, yAxis);
        line.setTitle("");
        line.setLegendVisible(true);
        line.setPrefHeight(220);
        line.setCreateSymbols(true);
        line.setStyle("-fx-background-color: transparent;");

        // Grouper par type de test
        Map<String, List<Evaluation>> byType = sorted.stream()
            .collect(Collectors.groupingBy(e -> e.getTypeTest() != null ? e.getTypeTest() : "Autre"));

        String[] lineColors = {C_CYAN, C_GREEN, "#FFD700", C_DANGER, "#A78BFA"};
        int colorIdx = 0;

        for (Map.Entry<String, List<Evaluation>> entry : byType.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());

            for (Evaluation ev : entry.getValue()) {
                String dateLabel = ev.getDateEvaluation().toLocalDate().toString();
                XYChart.Data<String, Number> point = new XYChart.Data<>(dateLabel, ev.getScore());
                series.getData().add(point);
                point.nodeProperty().addListener((obs, old, node) -> {
                    if (node != null) {
                        Tooltip.install(node, new Tooltip(
                            entry.getKey() + "\nScore : " + ev.getScore()
                            + "\nNiveau : " + ev.getNiveau()
                            + "\nDate : " + dateLabel
                        ));
                    }
                });
            }

            line.getData().add(series);

            // Couleur de la ligne
            final String col = lineColors[colorIdx % lineColors.length];
            for (XYChart.Data<?, ?> d : series.getData()) {
                d.nodeProperty().addListener((obs, old, node) -> {
                    if (node != null) node.setStyle("-fx-background-color: " + col + ", white;");
                });
            }
            // Couleur du trait
            series.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.lookup(".chart-series-line").setStyle(
                        "-fx-stroke: " + col + "; -fx-stroke-width: 2.5px;"
                    );
                }
            });
            colorIdx++;
        }

        Label title = chartTitle("📈  Évolution du score dans le temps");
        return styledChartBox(title, line);
    }

    // ── KPI résumé ──────────────────────────────────────────
    private HBox buildKpiRow(List<Evaluation> data) {
        double avg = data.stream().mapToInt(Evaluation::getScore).average().orElse(0);
        int max = data.stream().mapToInt(Evaluation::getScore).max().orElse(0);
        int min = data.stream().mapToInt(Evaluation::getScore).min().orElse(0);
        long elevated = data.stream().filter(e -> "Élevé".equals(e.getNiveau())).count();

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
            buildKpiCard("📊", "Moyenne", String.format("%.1f", avg), C_CYAN),
            buildKpiCard("🔺", "Maximum", String.valueOf(max), C_DANGER),
            buildKpiCard("🔻", "Minimum", String.valueOf(min), C_GREEN),
            buildKpiCard("⚠️", "Élevés", String.valueOf(elevated), "#FFD700")
        );
        return row;
    }

    private VBox buildKpiCard(String emoji, String label, String value, String color) {
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 20px;");
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.60); -fx-font-size: 11px;");

        VBox card = new VBox(4, emojiLbl, valLbl, nameLbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 24, 14, 24));
        card.setStyle(
            "-fx-background-color: " + C_CARD + "; " +
            "-fx-border-color: " + color + "44; " +
            "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;"
        );
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: " + C_SURFACE + "; " +
            "-fx-border-color: " + color + "; " +
            "-fx-border-width: 1.5; -fx-border-radius: 14; -fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, " + color + "44, 16, 0.2, 0, 0);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: " + C_CARD + "; " +
            "-fx-border-color: " + color + "44; " +
            "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;"
        ));
        return card;
    }

    // ── Helpers visuels ──────────────────────────────────────
    private Label chartTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + C_CYAN + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox styledChartBox(Label title, Node chart) {
        VBox box = new VBox(8, title, chart);
        box.setPadding(new Insets(14));
        box.setStyle(
            "-fx-background-color: " + C_CARD + "; " +
            "-fx-border-color: rgba(0,217,255,0.15); " +
            "-fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;"
        );
        VBox.setVgrow(chart, Priority.ALWAYS);
        return box;
    }

    private Line makeSeparator() {
        Line line = new Line(0, 0, 760, 0);
        line.setStroke(Color.web(C_CYAN, 0.15));
        line.setStrokeWidth(1);
        StackPane sp = new StackPane(line);
        sp.setPadding(new Insets(0, 16, 0, 16));
        // Return as a Line trick — use HBox wrapper instead
        Line sep = new Line(0, 0, 760, 0);
        sep.setStroke(Color.web(C_CYAN, 0.15));
        sep.setStrokeWidth(1);
        return sep;
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "📊";
        return switch (type) {
            case "Stress"     -> "🧘";
            case "Anxiété"    -> "😰";
            case "Dépression" -> "💙";
            case "Bien-être"  -> "🌿";
            case "Burnout"    -> "⚡";
            default           -> "📊";
        };
    }

    // ═══════════════════════════════════════════════════════
    //  DIALOG MODIFIER
    // ═══════════════════════════════════════════════════════
    private void openEditDialog(Evaluation ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏  Modifier l'évaluation");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
            "-fx-background-color: " + C_CARD + "; " +
            "-fx-border-color: " + C_CYAN + "; " +
            "-fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;"
        );

        ButtonType btnOK     = new ButtonType("💾  Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✕  Annuler",      ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnOK, btnCancel);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setPrefWidth(520);

        Label header = new Label("✏  Modification de l'évaluation");
        header.setStyle("-fx-text-fill: " + C_CYAN + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        form.getChildren().addAll(header, makeSeparator());

        ComboBox<String> typeTestField = styledCombo(
            new String[]{"Stress", "Anxiété", "Dépression", "Bien-être", "Burnout"}, ev.getTypeTest());
        TextField scoreField = styledField(String.valueOf(ev.getScore()), "Score (0–100)");
        ComboBox<String> niveauField = styledCombo(new String[]{"Faible", "Moyen", "Élevé"}, ev.getNiveau());
        DatePicker datePicker = styledDatePicker(ev.getDateEvaluation().toLocalDate());

        scoreField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int s = Integer.parseInt(newVal.trim());
                if      (s < 8)  niveauField.setValue("Faible");
                else if (s <= 14) niveauField.setValue("Moyen");
                else              niveauField.setValue("Élevé");
            } catch (NumberFormatException ignored) {}
        });

        form.getChildren().addAll(
            formRow("🎯 Type de test", typeTestField),
            formRow("🔢 Score (0–100)", scoreField),
            formRow("📈 Niveau", niveauField),
            formRow("📅 Date", datePicker)
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setContent(scroll);

        pane.lookupButton(btnOK).setStyle(
            "-fx-background-color: linear-gradient(to right, " + C_CYAN + ", " + C_GREEN + "); " +
            "-fx-text-fill: " + C_DARK_BG + "; -fx-font-weight: bold; " +
            "-fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;"
        );
        pane.lookupButton(btnCancel).setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: " + C_DANGER + "; -fx-border-width: 1.5; " +
            "-fx-border-radius: 20; -fx-background-radius: 20; " +
            "-fx-text-fill: " + C_DANGER + "; -fx-font-weight: bold; " +
            "-fx-padding: 10 24; -fx-cursor: hand;"
        );

        dialog.setResultConverter(btn -> {
            if (btn == btnOK) {
                String type = typeTestField.getValue();
                if (type == null || type.isEmpty()) {
                    showModernAlert("Validation", "Le type de test est obligatoire.", Alert.AlertType.WARNING);
                    return null;
                }
                int score;
                try {
                    score = Integer.parseInt(scoreField.getText().trim());
                    if (score < 0 || score > 100) {
                        showModernAlert("Validation", "Le score doit être entre 0 et 100.", Alert.AlertType.WARNING);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showModernAlert("Validation", "Le score doit être un nombre entier.", Alert.AlertType.WARNING);
                    return null;
                }
                String niveau = niveauField.getValue();
                LocalDate date = datePicker.getValue();
                if (niveau == null || date == null || date.isAfter(LocalDate.now())) {
                    showModernAlert("Validation", "Niveau ou date invalide.", Alert.AlertType.WARNING);
                    return null;
                }
                ev.setTypeTest(type); ev.setScore(score);
                ev.setNiveau(niveau); ev.setDateEvaluation(Date.valueOf(date));
                try {
                    serviceEvaluation.modifier(ev);
                    loadData();
                    showModernAlert("Succès ✔", "Évaluation mise à jour !", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showModernAlert("Erreur", "Impossible de modifier.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════
    private Button makeIconBtn(String icon, String color, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        String base = "-fx-background-color: transparent; -fx-text-fill: " + color + "; " +
                      "-fx-font-size: 15px; -fx-padding: 4 8; -fx-cursor: hand; " +
                      "-fx-border-color: " + color + "; -fx-border-width: 1; " +
                      "-fx-border-radius: 8; -fx-background-radius: 8;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace("-fx-background-color: transparent;", "-fx-background-color: " + color + "22;")));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private TextField styledField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        String base = "-fx-background-color: " + C_SURFACE + "; -fx-text-fill: " + C_TEXT + "; " +
                      "-fx-prompt-text-fill: rgba(200,255,220,0.35); -fx-font-size: 13px; " +
                      "-fx-padding: 10 14; -fx-background-radius: 10; " +
                      "-fx-border-color: " + C_BORDER + "; -fx-border-width: 1; -fx-border-radius: 10;";
        tf.setStyle(base);
        return tf;
    }

    private ComboBox<String> styledCombo(String[] options, String selected) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(options);
        cb.setValue(selected);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-text-fill: " + C_TEXT + "; " +
                    "-fx-border-color: " + C_BORDER + "; -fx-border-width: 1; " +
                    "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 4 10; -fx-font-size: 13px;");
        return cb;
    }

    private DatePicker styledDatePicker(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-text-fill: " + C_TEXT + "; " +
                    "-fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; " +
                    "-fx-border-width: 1; -fx-border-radius: 10;");
        return dp;
    }

    private HBox formRow(String labelText, javafx.scene.Node control) {
        Label lbl = new Label(labelText);
        lbl.setMinWidth(160);
        lbl.setStyle("-fx-text-fill: " + C_TEXT_DIM + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        HBox row = new HBox(12, lbl, control);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //  ANIMATIONS & ALERTES
    // ═══════════════════════════════════════════════════════
    private void animateTableLoad() {
        FadeTransition ft = new FadeTransition(Duration.millis(350), tableEvaluations);
        ft.setFromValue(0.4); ft.setToValue(1.0); ft.play();
    }

    private void pulseButton(Button btn) {
        if (btn == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
        st.setToX(0.92); st.setToY(0.92);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    private boolean confirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlertPane(alert);
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    private void showModernAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlertPane(alert);
        alert.showAndWait();
    }

    private void styleAlertPane(Alert alert) {
        alert.getDialogPane().setStyle(
            "-fx-background-color: " + C_CARD + "; " +
            "-fx-border-color: " + C_CYAN + "; " +
            "-fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;"
        );
        alert.getDialogPane().lookup(".content.label")
             .setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px;");
    }
}
