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
import javafx.stage.Stage;
import services.ServiceRessource;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class AfficherRessourceController {

    @FXML private TableView<Ressource>           tableRessources;
    @FXML private TableColumn<Ressource, String> titreCol;
    @FXML private TableColumn<Ressource, String> descriptionCol;
    @FXML private TableColumn<Ressource, String> typeCol;
    @FXML private TableColumn<Ressource, String> formatCol;
    @FXML private TableColumn<Ressource, Double> tailleCol;
    @FXML private TableColumn<Ressource, String> statutCol;
    @FXML private TableColumn<Ressource, java.sql.Date> dateCol;
    @FXML private TableColumn<Ressource, Void>   actionCol;
    @FXML private TableColumn<Ressource, Void>   vuesCol;   // ★ nouvelle colonne "Vues"

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label            totalCount;

    private ServiceRessource          serviceRessource;
    private ObservableList<Ressource> allRessources;

    /** Cache des vues : idRessource → totalVues, rechargé avec les données. */
    private Map<Integer, Integer> vuesCache = Map.of();

    // ── Palette identique au reste de l'application ──────────
    private static final String CYAN    = "#00D9FF";
    private static final String GREEN   = "#00FF88";
    private static final String DARK    = "#050C07";
    private static final String DANGER  = "#FF4D6D";
    private static final String SURFACE = "#071A10";
    private static final String GOLD    = "#FFD700";

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        serviceRessource = new ServiceRessource();

        applyTableStyle();
        setupColumns();
        setupFilters();
        setupRowClickTracking();   // ★ tracking au clic de ligne
        addActionButtonsToTable();
        loadData();
    }

    // ── TABLE STYLE ──────────────────────────────────────────
    private void applyTableStyle() {
        tableRessources.setEditable(false);
        tableRessources.setStyle(
            "-fx-background-color: " + DARK + ";" +
            "-fx-control-inner-background: " + SURFACE + ";" +
            "-fx-control-inner-background-alt: " + SURFACE + ";" +
            "-fx-table-cell-border-color: rgba(0,217,255,0.07);" +
            "-fx-border-color: transparent;"
        );

        // ── Style inline CSS injecté directement dans le tableau ──────
        // Contourne le problème JavaFX où les stylesheets externes
        // n'affectent pas les column-header sur certaines versions.
        String headerCss =
            ".table-view .column-header-background {" +
            "    -fx-background-color: #071A10;" +
            "}" +
            ".table-view .column-header," +
            ".table-view .filler {" +
            "    -fx-background-color: #071A10;" +
            "    -fx-border-color: rgba(0,217,255,0.20);" +
            "    -fx-border-width: 0 1 1 0;" +
            "    -fx-size: 36px;" +
            "}" +
            ".table-view .column-header .label {" +
            "    -fx-text-fill: #00D9FF;" +
            "    -fx-font-size: 12px;" +
            "    -fx-font-weight: bold;" +
            "    -fx-font-family: 'Courier New';" +
            "    -fx-alignment: CENTER;" +
            "}" +
            ".table-view .column-header .arrow {" +
            "    -fx-background-color: rgba(0,217,255,0.55);" +
            "}" +
            ".table-row-cell {" +
            "    -fx-background-color: #071A10;" +
            "    -fx-border-color: transparent;" +
            "    -fx-cell-size: 38px;" +
            "}" +
            ".table-row-cell:odd {" +
            "    -fx-background-color: #050C07;" +
            "}" +
            ".table-row-cell:hover {" +
            "    -fx-background-color: rgba(0,217,255,0.07);" +
            "}" +
            ".table-row-cell:selected," +
            ".table-row-cell:selected:hover {" +
            "    -fx-background-color: rgba(0,217,255,0.13);" +
            "}" +
            ".table-row-cell:selected .table-cell {" +
            "    -fx-text-fill: #E8FFF0;" +
            "}" +
            ".table-view .scroll-bar:vertical," +
            ".table-view .scroll-bar:horizontal {" +
            "    -fx-background-color: #050C07;" +
            "}" +
            ".table-view .scroll-bar .thumb {" +
            "    -fx-background-color: rgba(0,217,255,0.28);" +
            "    -fx-background-radius: 4;" +
            "}" +
            ".table-view .scroll-bar .track {" +
            "    -fx-background-color: #071A10;" +
            "}" +
            ".table-view .corner {" +
            "    -fx-background-color: #050C07;" +
            "}";

        // Écrire dans un fichier temporaire et le charger comme stylesheet
        try {
            java.io.File tmp = java.io.File.createTempFile("table-dark-", ".css");
            tmp.deleteOnExit();
            java.nio.file.Files.writeString(tmp.toPath(), headerCss);
            tableRessources.getStylesheets().add(tmp.toURI().toString());
        } catch (java.io.IOException ex) {
            System.err.println("[CSS] Impossible de créer le CSS temporaire : " + ex.getMessage());
        }
    }

    // ── COLONNES ─────────────────────────────────────────────
    /**
     * Style de fond appliqué sur CHAQUE cellule pour écraser le fond
     * clair par défaut de JavaFX et garantir la lisibilité du texte.
     */
    private static final String CELL_BG =
        "-fx-background-color: " + "#071A10" + ";";

    private void setupColumns() {
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        formatCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        tailleCol.setCellValueFactory(new PropertyValueFactory<>("tailleFichier"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePublication"));

        // ── Titre ─────────────────────────────────────────────
        titreCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle(
                    "-fx-text-fill: #E8FFF0;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-family: 'Courier New';"
                );
                lbl.setWrapText(false);
                setGraphic(lbl);
            }
        });

        // ── Description ───────────────────────────────────────
        descriptionCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle(
                    "-fx-text-fill: rgba(200,255,220,0.80);" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Courier New';"
                );
                lbl.setWrapText(false);
                setGraphic(lbl);
            }
        });

        // ── Format ────────────────────────────────────────────
        formatCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item.toUpperCase());
                lbl.setStyle(
                    "-fx-text-fill: rgba(0,217,255,0.75);" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Courier New';"
                );
                setGraphic(lbl);
            }
        });

        // ── Taille ────────────────────────────────────────────
        tailleCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(String.format("%.2f", item));
                lbl.setStyle(
                    "-fx-background-color: rgba(0,217,255,0.12);" +
                    "-fx-text-fill: #00D9FF;" +
                    "-fx-padding: 4 10;" +
                    "-fx-background-radius: 10;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-family: 'Courier New';"
                );
                setGraphic(lbl);
            }
        });

        // ── Type — icône + label ──────────────────────────────
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String icon = switch (item) {
                    case "PDF"     -> "📕 ";
                    case "Vidéo"   -> "🎥 ";
                    case "Image"   -> "🖼 ";
                    case "Audio"   -> "🎵 ";
                    case "Article" -> "📝 ";
                    default        -> "📄 ";
                };
                Label lbl = new Label(icon + item);
                lbl.setStyle(
                    "-fx-text-fill: #00D9FF;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-family: 'Courier New';"
                );
                setGraphic(lbl);
            }
        });

        // ── Statut — badge coloré ─────────────────────────────
        statutCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String bg, fg, icon;
                switch (item) {
                    case "publié"  -> { bg = "rgba(0,255,136,0.15)";  fg = GREEN; icon = "✔ "; }
                    case "archivé" -> { bg = "rgba(0,217,255,0.10)";  fg = CYAN;  icon = "⊟ "; }
                    default             -> { bg = "rgba(255,193,7,0.15)";  fg = GOLD;  icon = "⏳ "; }
                }
                Label badge = new Label(icon + item);
                badge.setStyle(
                    "-fx-background-color: " + bg + ";" +
                    "-fx-text-fill: " + fg + ";" +
                    "-fx-padding: 4 12;" +
                    "-fx-background-radius: 20;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-family: 'Courier New';"
                );
                setGraphic(badge);
            }
        });

        // ── Date ─────────────────────────────────────────────
        dateCol.setCellFactory(col -> new TableCell<>() {
            final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM. yyyy");
            @Override protected void updateItem(java.sql.Date item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CELL_BG);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label("📋 " + fmt.format(item));
                lbl.setStyle(
                    "-fx-text-fill: rgba(200,255,220,0.65);" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-family: 'Courier New';"
                );
                setGraphic(lbl);
            }
        });

        // ★ Colonne Vues — affiche le compteur depuis le cache
        if (vuesCol != null) {
            vuesCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setStyle(CELL_BG);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null); return;
                    }
                    Ressource r = (Ressource) getTableRow().getItem();
                    int count = vuesCache.getOrDefault(r.getIdRessource(), 0);

                    String color = count == 0 ? "rgba(200,255,220,0.30)"
                                 : count < 10  ? CYAN
                                 :               GREEN;
                    Label lbl = new Label("👁 " + count);
                    lbl.setStyle(
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Courier New';"
                    );
                    setGraphic(lbl);
                    setStyle("-fx-background-color: " + SURFACE + ";");
                }
            });
        }
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

    // ── ★ ROW CLICK → TRACKING ────────────────────────────────
    /**
     * Chaque fois que l'utilisateur sélectionne une ligne dans la table,
     * un événement de consultation est enregistré en base pour cette ressource.
     */
    private void setupRowClickTracking() {
        tableRessources.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    // Enregistrement asynchrone pour ne pas bloquer l'UI
                    new Thread(() ->
                        serviceRessource.enregistrerConsultation(
                            newVal.getIdRessource(), "backoffice")
                    ).start();
                }
            }
        );
    }

    // ── DATA ──────────────────────────────────────────────────
    private void loadData() {
        try {
            allRessources = FXCollections.observableArrayList(serviceRessource.recuperer());

            // ★ Charger le cache des vues en même temps
            try {
                vuesCache = serviceRessource.getTotalVuesParRessource();
            } catch (SQLException e) {
                System.err.println("[TRACKING] Impossible de charger les vues : " + e.getMessage());
                vuesCache = Map.of();
            }

            tableRessources.setItems(allRessources);
            updateCount(allRessources.size());
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les ressources : " + e.getMessage(),
                      Alert.AlertType.ERROR);
        }
    }

    private void updateCount(int count) {
        if (totalCount != null) totalCount.setText(String.valueOf(count));
    }

    // ── SEARCH / FILTER ───────────────────────────────────────
    @FXML private void handleSearch() { applyFilters(); }

    @FXML
    private void handleReset() {
        searchField.clear();
        filterType.setValue("Tous");
        filterStatut.setValue("Tous");
        tableRessources.setItems(allRessources);
        updateCount(allRessources.size());
    }

    // ★ Ouvrir le dashboard de stats
    @FXML
    private void handleOpenStats() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/StatistiquesRessource.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("📊 Statistiques des consultations");
            stage.setScene(new Scene(root, 820, 600));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les statistiques.", Alert.AlertType.ERROR);
        }
    }

    private void applyFilters() {
        String search  = searchField.getText().toLowerCase().trim();
        String selType = filterType.getValue();
        String selStat = filterStatut.getValue();

        ObservableList<Ressource> filtered = allRessources.filtered(r -> {
            boolean ms  = search.isEmpty()
                || r.getTitre().toLowerCase().contains(search)
                || (r.getDescription() != null && r.getDescription().toLowerCase().contains(search));
            boolean mt  = selType.equals("Tous") || r.getType().equals(selType);
            boolean mst = selStat.equals("Tous") || r.getStatut().equals(selStat);
            return ms && mt && mst;
        });
        tableRessources.setItems(filtered);
        updateCount(filtered.size());
    }

    // ── ACTION BUTTONS ────────────────────────────────────────
    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit   = new Button("⇄  Mod...");
            private final Button btnDelete = new Button("⊟  Supp...");
            private final HBox   pane      = new HBox(6, btnEdit, btnDelete);

            {
                String styleEdit =
                    "-fx-background-color: rgba(0,217,255,0.12); " +
                    "-fx-text-fill: " + CYAN + "; " +
                    "-fx-border-color: rgba(0,217,255,0.35); -fx-border-width: 1; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 5 10; -fx-font-size: 11px; " +
                    "-fx-font-family: 'Courier New'; -fx-cursor: hand;";

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
                setStyle(CELL_BG);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ── OPEN MODIFIER ─────────────────────────────────────────
    private void openModificationDialog(Ressource r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/ModifierRessource.fxml"));
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
