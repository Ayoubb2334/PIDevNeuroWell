package controllers;

import entities.Paiement;
import services.PaiementService;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AfficherPaiementController {

    // ══════════════ FXML ══════════════
    @FXML private VBox        cardContainer;
    @FXML private TextField   searchField;
    @FXML private Label       totalLabel;
    @FXML private Label       successLabel;
    @FXML private Label       failLabel;
    @FXML private Label       avgLabel;        // ✅ NOUVEAU : montant moyen
    @FXML private BarChart<String, Number> chart;
    @FXML private PieChart    pieChart;        // ✅ NOUVEAU : camembert par méthode

    // ✅ NOUVEAUX FILTRES
    @FXML private ComboBox<String> filterMethode;
    @FXML private ComboBox<String> filterStatut;
    @FXML private DatePicker       filterDateDebut;
    @FXML private DatePicker       filterDateFin;
    @FXML private Button           btnReset;

    private final PaiementService service = new PaiementService();
    private List<Paiement> allPaiements;   // cache complet

    // ════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        setupFilters();
        loadData();

        // Recherche textuelle en temps réel
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ════════════════════════════════════════════════════════
    //  ✅ SETUP FILTRES
    // ════════════════════════════════════════════════════════
    private void setupFilters() {
        // ComboBox Méthode
        if (filterMethode != null) {
            filterMethode.getItems().addAll("Toutes", "Stripe", "PayPal", "Flouci");
            filterMethode.setValue("Toutes");
            filterMethode.setStyle(comboStyle());
            filterMethode.valueProperty().addListener((obs, o, n) -> applyFilters());
        }

        // ComboBox Statut
        if (filterStatut != null) {
            filterStatut.getItems().addAll("Tous", "REUSSI", "ECHOUE", "EN_ATTENTE");
            filterStatut.setValue("Tous");
            filterStatut.setStyle(comboStyle());
            filterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
        }

        // DatePickers
        if (filterDateDebut != null) {
            filterDateDebut.setStyle(datePickerStyle());
            filterDateDebut.valueProperty().addListener((obs, o, n) -> applyFilters());
        }
        if (filterDateFin != null) {
            filterDateFin.setStyle(datePickerStyle());
            filterDateFin.valueProperty().addListener((obs, o, n) -> applyFilters());
        }

        // Bouton Reset
        if (btnReset != null) {
            btnReset.setStyle(
                    "-fx-background-color:rgba(255,75,110,0.12);" +
                            "-fx-text-fill:#FF4B6E;" +
                            "-fx-font-size:11px;-fx-font-weight:bold;" +
                            "-fx-border-color:rgba(255,75,110,0.30);-fx-border-width:1;" +
                            "-fx-border-radius:10;-fx-background-radius:10;" +
                            "-fx-padding:6 14;-fx-cursor:hand;"
            );
            btnReset.setOnAction(e -> resetFilters());
        }
    }

    // ════════════════════════════════════════════════════════
    //  ✅ APPLIQUER LES FILTRES
    // ════════════════════════════════════════════════════════
    private void applyFilters() {
        if (allPaiements == null) return;

        List<Paiement> filtered = allPaiements.stream()
                .filter(p -> matchSearch(p))
                .filter(p -> matchMethode(p))
                .filter(p -> matchStatut(p))
                .filter(p -> matchDate(p))
                .collect(Collectors.toList());

        displayCards(filtered);
        updateKPIs(filtered);
    }

    private boolean matchSearch(Paiement p) {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) return true;
        return (p.getReference()    != null && p.getReference().toLowerCase().contains(q))
                || (p.getModePaiement() != null && p.getModePaiement().toLowerCase().contains(q))
                || String.format("%.2f", p.getMontant()).contains(q);
    }

    private boolean matchMethode(Paiement p) {
        if (filterMethode == null) return true;
        String val = filterMethode.getValue();
        if (val == null || val.equals("Toutes")) return true;
        return val.equalsIgnoreCase(p.getModePaiement());
    }

    private boolean matchStatut(Paiement p) {
        if (filterStatut == null) return true;
        String val = filterStatut.getValue();
        if (val == null || val.equals("Tous")) return true;
        return val.equalsIgnoreCase(p.getStatut());
    }

    private boolean matchDate(Paiement p) {
        if (p.getDatePaiement() == null) return true;
        LocalDate date = p.getDatePaiement();
        if (filterDateDebut != null && filterDateDebut.getValue() != null
                && date.isBefore(filterDateDebut.getValue())) return false;
        if (filterDateFin   != null && filterDateFin.getValue()   != null
                && date.isAfter(filterDateFin.getValue()))   return false;
        return true;
    }

    private void resetFilters() {
        searchField.clear();
        if (filterMethode  != null) filterMethode.setValue("Toutes");
        if (filterStatut   != null) filterStatut.setValue("Tous");
        if (filterDateDebut != null) filterDateDebut.setValue(null);
        if (filterDateFin   != null) filterDateFin.setValue(null);
        applyFilters();
    }

    // ════════════════════════════════════════════════════════
    //  LOAD DATA
    // ════════════════════════════════════════════════════════
    private void loadData() {
        try {
            allPaiements = service.getAll();
            displayCards(allPaiements);
            updateKPIs(allPaiements);
            loadBarChart();
            loadPieChart();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  ✅ KPIs MIS À JOUR DYNAMIQUEMENT
    // ════════════════════════════════════════════════════════
    private void updateKPIs(List<Paiement> list) {
        double total = list.stream().mapToDouble(Paiement::getMontant).sum();
        long   reussi = list.stream().filter(p -> "REUSSI".equalsIgnoreCase(p.getStatut())).count();
        long   echoue = list.stream().filter(p -> "ECHOUE".equalsIgnoreCase(p.getStatut())).count();
        double avg    = list.isEmpty() ? 0 : total / list.size();

        if (totalLabel   != null) totalLabel.setText(String.format("%.2f DT", total));
        if (successLabel != null) successLabel.setText(String.valueOf(reussi));
        if (failLabel    != null) failLabel.setText(String.valueOf(echoue));
        if (avgLabel     != null) avgLabel.setText(String.format("%.2f DT", avg));
    }

    // ════════════════════════════════════════════════════════
    //  DISPLAY CARDS
    // ════════════════════════════════════════════════════════
    private void displayCards(List<Paiement> list) {
        cardContainer.getChildren().clear();

        if (list.isEmpty()) {
            Label empty = new Label("◌   Aucune transaction trouvée");
            empty.setStyle("-fx-text-fill:rgba(0,217,255,0.30);-fx-font-family:'Consolas';"
                    + "-fx-font-size:14px;-fx-padding:40 0;");
            cardContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            HBox card = buildCreativeCard(list.get(i));
            card.setOpacity(0);
            card.setTranslateY(12);

            FadeTransition ft = new FadeTransition(Duration.millis(280), card);
            ft.setDelay(Duration.millis(i * 50L));
            ft.setToValue(1);

            TranslateTransition tt = new TranslateTransition(Duration.millis(280), card);
            tt.setDelay(Duration.millis(i * 50L));
            tt.setToY(0);

            ft.play();
            tt.play();
            cardContainer.getChildren().add(card);
        }
    }

    private HBox buildCreativeCard(Paiement p) {
        String statut = p.getStatut() != null ? p.getStatut().toUpperCase() : "";
        String accentColor = switch (statut) {
            case "REUSSI", "RÉUSSI" -> "#00FF88";
            case "ECHOUE", "ÉCHOUÉ" -> "#FF4B6E";
            default                  -> "#FFC400";
        };

        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(72);
        card.setStyle(
                "-fx-background-color:#0A1A0D;" +
                        "-fx-border-color:rgba(0,217,255,0.08);" +
                        "-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;"
        );

        // Barre colorée gauche
        Rectangle colorBar = new Rectangle(4, 72);
        colorBar.setFill(javafx.scene.paint.Color.web(accentColor));
        colorBar.setArcWidth(4); colorBar.setArcHeight(4);

        // Icône méthode
        StackPane iconBox = new StackPane();
        iconBox.setMinWidth(52); iconBox.setMaxWidth(52);
        iconBox.setPadding(new Insets(0, 0, 0, 14));
        Label icon = new Label(getMethodIcon(p.getModePaiement()));
        icon.setStyle("-fx-font-size:20px;");
        iconBox.getChildren().add(icon);

        // Infos centre
        VBox infoBlock = new VBox(4);
        infoBlock.setAlignment(Pos.CENTER_LEFT);
        infoBlock.setPadding(new Insets(12, 0, 12, 6));
        HBox.setHgrow(infoBlock, Priority.ALWAYS);

        HBox line1 = new HBox(10);
        line1.setAlignment(Pos.CENTER_LEFT);
        Label ref   = new Label(p.getReference());
        ref.setStyle("-fx-text-fill:#00D9FF;-fx-font-family:'Consolas';-fx-font-size:13px;-fx-font-weight:bold;");
        Label badge = buildBadge(statut, accentColor);
        line1.getChildren().addAll(ref, badge);

        HBox line2 = new HBox(8);
        line2.setAlignment(Pos.CENTER_LEFT);
        Label montant = new Label(String.format("%.2f DT", p.getMontant()));
        montant.setStyle("-fx-text-fill:" + accentColor + ";-fx-font-family:'Consolas';-fx-font-size:14px;-fx-font-weight:bold;");
        Label s1 = sep(); Label s2 = sep();
        Label methode = new Label(p.getModePaiement() != null ? p.getModePaiement() : "—");
        methode.setStyle("-fx-text-fill:rgba(200,255,220,0.55);-fx-font-family:'Consolas';-fx-font-size:12px;");
        String dateStr = p.getDatePaiement() != null
                ? p.getDatePaiement().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
        Label date = new Label(dateStr);
        date.setStyle("-fx-text-fill:rgba(0,217,255,0.40);-fx-font-family:'Consolas';-fx-font-size:11px;");
        line2.getChildren().addAll(montant, s1, methode, s2, date);
        infoBlock.getChildren().addAll(line1, line2);

        // Boutons actions
        VBox actionsBox = new VBox(6);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setPadding(new Insets(0, 16, 0, 12));
        Button btnEdit = buildActionBtn("✎  Modifier", "#00D9FF");
        btnEdit.setOnAction(e -> openEditDialog(p));
        Button btnDel  = buildActionBtn("✕  Supprimer", "#FF4B6E");
        btnDel.setOnAction(e -> {
            try { service.delete(p.getId()); loadData(); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
        actionsBox.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(colorBar, iconBox, infoBlock, actionsBox);

        // Hover
        String baseStyle = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#0D2214;" +
                        "-fx-border-color:" + accentColor + "44;" +
                        "-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian," + accentColor + "22,16,0.1,0,2);"
        ));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    // ════════════════════════════════════════════════════════
    //  ✅ GRAPHIQUE BAR — Données RÉELLES par mois
    // ════════════════════════════════════════════════════════
    private void loadBarChart() {
        if (chart == null || allPaiements == null) return;
        chart.getData().clear();
        chart.setLegendVisible(false);
        chart.setAnimated(true);

        // Regrouper par mois (données réelles)
        Map<String, Double> parMois = allPaiements.stream()
                .filter(p -> p.getDatePaiement() != null && "REUSSI".equalsIgnoreCase(p.getStatut()))
                .collect(Collectors.groupingBy(
                        p -> p.getDatePaiement().format(DateTimeFormatter.ofPattern("MMM yy")),
                        Collectors.summingDouble(Paiement::getMontant)
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Afficher les 6 derniers mois dans l'ordre
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");
        for (int i = 5; i >= 0; i--) {
            String label = now.minusMonths(i).format(fmt);
            double val   = parMois.getOrDefault(label, 0.0);
            series.getData().add(new XYChart.Data<>(label, val));
        }

        chart.getData().add(series);

        // Couleur neon vert
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-bar-fill:#00FF88;");
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  ✅ CAMEMBERT — Répartition par méthode de paiement
    // ════════════════════════════════════════════════════════
    private void loadPieChart() {
        if (pieChart == null || allPaiements == null) return;
        pieChart.getData().clear();
        pieChart.setLegendVisible(true);
        pieChart.setAnimated(true);

        Map<String, Long> parMethode = allPaiements.stream()
                .filter(p -> p.getModePaiement() != null)
                .collect(Collectors.groupingBy(Paiement::getModePaiement, Collectors.counting()));

        parMethode.forEach((methode, count) ->
                pieChart.getData().add(new PieChart.Data(methode + " (" + count + ")", count))
        );

        // Couleurs personnalisées
        Platform.runLater(() -> {
            String[] colors = {"#00FF88", "#00D9FF", "#FF9F43", "#FF4B6E"};
            int idx = 0;
            for (PieChart.Data d : pieChart.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-pie-color:" + colors[idx % colors.length] + ";");
                idx++;
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  EDIT DIALOG
    // ════════════════════════════════════════════════════════
    private void openEditDialog(Paiement p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/AjouterPaiement.fxml"));
            Parent root = loader.load();
            AjouterPaiementController ctrl = loader.getController();
            ctrl.setPaiementToEdit(p);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Paiement");
            stage.setScene(new Scene(root, 520, 620));
            stage.showAndWait();
            loadData();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════
    private Label buildBadge(String statut, String color) {
        Label badge = new Label();
        String text = switch (statut) {
            case "REUSSI", "RÉUSSI" -> "✓ RÉUSSI";
            case "ECHOUE", "ÉCHOUÉ" -> "✕ ÉCHOUÉ";
            default                  -> "◌ EN ATTENTE";
        };
        badge.setText(text);
        badge.setStyle(
                "-fx-background-color:" + color + "18;-fx-text-fill:" + color + ";" +
                        "-fx-font-family:'Consolas';-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-border-color:" + color + "44;" +
                        "-fx-border-width:1;-fx-border-radius:20;-fx-padding:3 10;"
        );
        return badge;
    }

    private Button buildActionBtn(String text, String color) {
        Button btn = new Button(text);
        String base =
                "-fx-background-color:" + color + "15;-fx-text-fill:" + color + ";" +
                        "-fx-font-family:'Consolas';-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:7;-fx-border-color:" + color + "30;" +
                        "-fx-border-width:1;-fx-border-radius:7;-fx-padding:5 12;-fx-cursor:hand;-fx-min-width:100;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + color + ";-fx-text-fill:" +
                        (color.equals("#FF4B6E") ? "white" : "#050C07") + ";" +
                        "-fx-font-family:'Consolas';-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:7;-fx-border-color:" + color + ";" +
                        "-fx-border-width:1;-fx-border-radius:7;-fx-padding:5 12;-fx-cursor:hand;-fx-min-width:100;" +
                        "-fx-effect:dropshadow(gaussian," + color + "66,10,0.3,0,0);"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Label sep() {
        Label s = new Label("·");
        s.setStyle("-fx-text-fill:rgba(0,217,255,0.25);-fx-font-size:12px;");
        return s;
    }

    private String getMethodIcon(String method) {
        if (method == null) return "💳";
        return switch (method.toLowerCase()) {
            case "stripe" -> "💳";
            case "paypal" -> "🅿";
            case "flouci" -> "🇹🇳";
            case "paymee" -> "💰";
            default       -> "💳";
        };
    }

    private String comboStyle() {
        return "-fx-background-color:#0A1A0D;-fx-text-fill:#00D9FF;" +
                "-fx-border-color:rgba(0,217,255,0.25);-fx-border-radius:8;-fx-background-radius:8;";
    }

    private String datePickerStyle() {
        return "-fx-background-color:#0A1A0D;-fx-text-fill:#00D9FF;" +
                "-fx-border-color:rgba(0,217,255,0.25);-fx-border-radius:8;-fx-background-radius:8;";
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null);
        a.setContentText(msg); a.showAndWait();
    }
}