package controllers;

import entities.Ressource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import services.ServiceRessource;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class AfficherRessourceController {

    @FXML private TableView<Ressource>           tableRessources;
    @FXML private TableColumn<Ressource, String> titreCol;
    @FXML private TableColumn<Ressource, String> descriptionCol;
    @FXML private TableColumn<Ressource, String> typeCol;
    @FXML private TableColumn<Ressource, String> formatCol;
    @FXML private TableColumn<Ressource, Double> tailleCol;
    @FXML private TableColumn<Ressource, String> statutCol;
    @FXML private TableColumn<Ressource, Date>   dateCol;
    @FXML private TableColumn<Ressource, Void>   actionCol;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label            totalCount;

    private ServiceRessource          serviceRessource;
    private ObservableList<Ressource> allRessources;

    // ── Couleurs identiques à AfficherEvaluation ──────────────
    private static final String CYAN     = "#00D9FF";
    private static final String GREEN    = "#00FF88";
    private static final String DARK     = "#050C07";
    private static final String DANGER   = "#FF4D6D";
    private static final String SURFACE  = "#071A10";

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        serviceRessource = new ServiceRessource();

        applyTableStyle();
        setupColumns();
        setupFilters();
        addActionButtonsToTable();
        loadData();
    }

    // ── TABLE STYLE ──────────────────────────────────────────
    private void applyTableStyle() {
        tableRessources.setEditable(false);
        tableRessources.setStyle(
            "-fx-background-color: " + DARK + "; " +
            "-fx-control-inner-background: " + SURFACE + "; " +
            "-fx-control-inner-background-alt: " + SURFACE + "; " +
            "-fx-table-cell-border-color: rgba(0,217,255,0.07); " +
            "-fx-border-color: transparent;"
        );
    }

    // ── COLUMNS ──────────────────────────────────────────────
    private void setupColumns() {
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        formatCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        tailleCol.setCellValueFactory(new PropertyValueFactory<>("tailleFichier"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePublication"));

        // Date formatée
        dateCol.setCellFactory(col -> new TableCell<>() {
            final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM. yyyy");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label("📋 " + fmt.format(item));
                lbl.setStyle("-fx-text-fill: rgba(200,255,220,0.65); -fx-font-size: 12px; -fx-font-family: 'Courier New';");
                setGraphic(lbl);
            }
        });

        // Statut — badge coloré comme AfficherEvaluation
        statutCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String bg, fg, icon;
                switch (item) {
                    case "publié"  -> { bg = "rgba(0,255,136,0.15)";  fg = GREEN;  icon = "✔ "; }
                    case "archivé" -> { bg = "rgba(0,217,255,0.10)";  fg = CYAN;   icon = "⊟ "; }
                    default        -> { bg = "rgba(255,193,7,0.15)";  fg = "#FFD700"; icon = "⏳ "; }
                }
                Label badge = new Label(icon + item);
                badge.setStyle(
                    "-fx-background-color: " + bg + "; " +
                    "-fx-text-fill: " + fg + "; " +
                    "-fx-padding: 4 12; -fx-background-radius: 20; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Courier New';"
                );
                setGraphic(badge);
            }
        });

        // Type — badge avec icône
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String icon = switch (item) {
                    case "PDF"     -> "📕 ";
                    case "Vidéo"   -> "🎥 ";
                    case "Image"   -> "🖼️ ";
                    case "Audio"   -> "🎵 ";
                    case "Article" -> "📝 ";
                    default        -> "📄 ";
                };
                Label lbl = new Label(icon + item);
                lbl.setStyle(
                    "-fx-text-fill: " + CYAN + "; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Courier New';"
                );
                setGraphic(lbl);
            }
        });

        // Titre en blanc
        titreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
                setGraphic(lbl);
            }
        });

        // Score / taille en couleur
        tailleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(String.format("%.2f", item));
                lbl.setStyle(
                    "-fx-background-color: rgba(0,217,255,0.12); " +
                    "-fx-text-fill: " + CYAN + "; " +
                    "-fx-padding: 4 10; -fx-background-radius: 10; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Courier New';"
                );
                setGraphic(lbl);
            }
        });
    }

    // ── FILTERS ──────────────────────────────────────────────
    private void setupFilters() {
        filterType.getItems().addAll("Tous", "PDF", "Vidéo", "Image", "Audio", "Article");
        filterType.setValue("Tous");
        filterType.setOnAction(e -> applyFilters());

        filterStatut.getItems().addAll("Tous", "brouillon", "publié", "archivé");
        filterStatut.setValue("Tous");
        filterStatut.setOnAction(e -> applyFilters());
    }

    // ── DATA ──────────────────────────────────────────────────
    private void loadData() {
        try {
            allRessources = FXCollections.observableArrayList(serviceRessource.recuperer());
            tableRessources.setItems(allRessources);
            updateCount(allRessources.size());
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les ressources : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateCount(int count) {
        if (totalCount != null) totalCount.setText(String.valueOf(count));
    }

    // ── SEARCH / FILTER ───────────────────────────────────────
    @FXML
    private void handleSearch() { applyFilters(); }

    @FXML
    private void handleReset() {
        searchField.clear();
        filterType.setValue("Tous");
        filterStatut.setValue("Tous");
        tableRessources.setItems(allRessources);
        updateCount(allRessources.size());
    }

    private void applyFilters() {
        String search   = searchField.getText().toLowerCase().trim();
        String selType  = filterType.getValue();
        String selStat  = filterStatut.getValue();

        ObservableList<Ressource> filtered = allRessources.filtered(r -> {
            boolean ms = search.isEmpty() ||
                r.getTitre().toLowerCase().contains(search) ||
                (r.getDescription() != null && r.getDescription().toLowerCase().contains(search));
            boolean mt = selType.equals("Tous") || r.getType().equals(selType);
            boolean mst = selStat.equals("Tous") || r.getStatut().equals(selStat);
            return ms && mt && mst;
        });
        tableRessources.setItems(filtered);
        updateCount(filtered.size());
    }

    // ── ACTION BUTTONS (style identique à AfficherEvaluation) ─
    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit   = new Button("⇄  Mod...");
            private final Button btnDelete = new Button("⊟  Supp...");
            private final HBox   pane      = new HBox(6, btnEdit, btnDelete);

            {
                // Modifier — style cyan comme AfficherEvaluation
                String styleEdit =
                    "-fx-background-color: rgba(0,217,255,0.12); " +
                    "-fx-text-fill: " + CYAN + "; " +
                    "-fx-border-color: rgba(0,217,255,0.35); -fx-border-width: 1; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 5 10; -fx-font-size: 11px; " +
                    "-fx-font-family: 'Courier New'; -fx-cursor: hand;";

                // Supprimer — style rouge comme AfficherEvaluation
                String styleDelete =
                    "-fx-background-color: rgba(255,77,109,0.12); " +
                    "-fx-text-fill: " + DANGER + "; " +
                    "-fx-border-color: rgba(255,77,109,0.35); -fx-border-width: 1; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 5 10; -fx-font-size: 11px; " +
                    "-fx-font-family: 'Courier New'; -fx-cursor: hand;";

                btnEdit.setStyle(styleEdit);
                btnDelete.setStyle(styleDelete);

                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(styleEdit
                    .replace("rgba(0,217,255,0.12)", "rgba(0,217,255,0.25)")
                    .replace("rgba(0,217,255,0.35)", CYAN)));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(styleEdit));

                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(styleDelete
                    .replace("rgba(255,77,109,0.12)", "rgba(255,77,109,0.25)")
                    .replace("rgba(255,77,109,0.35)", DANGER)));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(styleDelete));

                btnEdit.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    openModificationDialog(r);
                });

                btnDelete.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer « " + r.getTitre() + " » ?");
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.OK) {
                            try {
                                serviceRessource.supprimer(r);
                                allRessources.remove(r);
                                tableRessources.refresh();
                                updateCount(tableRessources.getItems().size());
                                showAlert("Succès", "Ressource supprimée.", Alert.AlertType.INFORMATION);
                            } catch (SQLException ex) {
                                showAlert("Erreur", ex.getMessage(), Alert.AlertType.ERROR);
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
                setStyle("-fx-background-color: " + SURFACE + ";");
            }
        });
    }

    // ── OPEN MODIFIER ─────────────────────────────────────────
    private void openModificationDialog(Ressource r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ModifierRessource.fxml"));
            Parent root = loader.load();
            ModifierRessourceController ctrl = loader.getController();
            ctrl.setRessource(r);
            Stage stage = new Stage();
            stage.setTitle("Modifier la Ressource");
            stage.setScene(new Scene(root, 560, 640));
            stage.showAndWait();
            loadData();
            applyFilters();
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
