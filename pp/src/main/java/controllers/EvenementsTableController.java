package controllers;

import entities.Evenement;
import entities.Participation;
import javafx.animation.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Duration;
import services.ServiceEvenement;
import services.ServiceParticipation;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║   NEUROWELL — Admin Dashboard Controller             ║
 * ║   Dark Futuristic UI · Sky Blue & Neon Green         ║
 * ╚══════════════════════════════════════════════════════╝
 */
public class EvenementsTableController {

    // ═══════════════════════════════════════════════════════
    //  PALETTE & DESIGN TOKENS
    // ═══════════════════════════════════════════════════════
    private static final String C_CYAN       = "#00D9FF";
    private static final String C_GREEN      = "#00FF88";
    private static final String C_LIME       = "#7FFF00";
    private static final String C_DARK_BG    = "#050C07";
    private static final String C_CARD       = "#0D1F12";
    private static final String C_CARD2      = "#0A1A0F";
    private static final String C_SURFACE    = "#112016";
    private static final String C_TEXT       = "#E8FFF0";
    private static final String C_TEXT_DIM   = "rgba(200,255,220,0.55)";
    private static final String C_DANGER     = "#FF4D6D";
    private static final String C_WARN       = "#FFB347";
    private static final String C_BORDER     = "rgba(0,217,255,0.18)";

    // ═══════════════════════════════════════════════════════
    //  FXML — ÉVÉNEMENTS TABLE
    // ═══════════════════════════════════════════════════════
    @FXML private TableView<Evenement>           tableEvenements;
    @FXML private TableColumn<Evenement, String>    colTitre;
    @FXML private TableColumn<Evenement, String>    colImage;
    @FXML private TableColumn<Evenement, String>    colDescription;
    @FXML private TableColumn<Evenement, Timestamp> colDate;
    @FXML private TableColumn<Evenement, String>    colLocalisation;
    @FXML private TableColumn<Evenement, String>    colType;
    @FXML private TableColumn<Evenement, Integer>   colCapacite;
    @FXML private TableColumn<Evenement, String>    colStatut;
    @FXML private TableColumn<Evenement, String>    colPrix_e;
    @FXML private TableColumn<Evenement, Void>      colActions;

    // ═══════════════════════════════════════════════════════
    //  FXML — PARTICIPATIONS TABLE
    // ═══════════════════════════════════════════════════════
    @FXML private TableView<Participation>              tableParticipations;
    @FXML private TableColumn<Participation, String>    colObjectif;
    @FXML private TableColumn<Participation, String>    colModeParticipation;
    @FXML private TableColumn<Participation, Void>      colActionsParticipation;

    // ═══════════════════════════════════════════════════════
    //  FXML — CONTROLS
    // ═══════════════════════════════════════════════════════
    @FXML private Button    btnRefresh;
    @FXML private Button    btnRefreshParticipations;
    @FXML private Button    btnClose;
    @FXML private TextField searchField;
    @FXML private Button    btnSearch;

    // ═══════════════════════════════════════════════════════
    //  SERVICES
    // ═══════════════════════════════════════════════════════
    private final ServiceEvenement    serviceEvenement    = new ServiceEvenement();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Style tables ────────────────────────────────────
        styleTable(tableEvenements);
        if (tableParticipations != null) styleTable(tableParticipations);

        // ── Setup columns ───────────────────────────────────
        setupEventColumns();
        setupImageColumn();
        setupStatutColumn();
        setupActionsColumn();
        tableEvenements.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        if (tableParticipations != null) {
            setupParticipationColumns();
            tableParticipations.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }

        // ── Load data ────────────────────────────────────────
        loadEvenements();
        if (tableParticipations != null) loadParticipations();

        // ── Style & wire buttons ─────────────────────────────
        styleButton(btnRefresh,              C_CYAN,  "↻  Actualiser");
        styleButton(btnRefreshParticipations, C_GREEN, "↻  Actualiser");
        styleButton(btnClose,                C_DANGER, "✕  Fermer");
        styleButton(btnSearch,               C_CYAN,  "⌕  Rechercher");
        styleSearchField(searchField);

        if (btnRefresh               != null) btnRefresh.setOnAction(e -> { loadEvenements();   pulseButton(btnRefresh); });
        if (btnRefreshParticipations != null) btnRefreshParticipations.setOnAction(e -> { loadParticipations(); pulseButton(btnRefreshParticipations); });
        if (btnClose                 != null) btnClose.setOnAction(e -> tableEvenements.getScene().getWindow().hide());
        if (btnSearch                != null) btnSearch.setOnAction(e -> filterEvenements());
        if (searchField              != null) searchField.setOnAction(e -> filterEvenements());
    }

    // ═══════════════════════════════════════════════════════
    //  GLOBAL TABLE STYLE
    // ═══════════════════════════════════════════════════════
    private <T> void styleTable(TableView<T> table) {
        if (table == null) return;
        table.setStyle(
                "-fx-background-color: " + C_CARD + "; " +
                        "-fx-border-color: " + C_BORDER + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 16; " +
                        "-fx-background-radius: 16; " +
                        "-fx-table-cell-border-color: rgba(0,217,255,0.07);"
        );

        // Glow effect on table
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(C_CYAN, 0.12));
        glow.setRadius(30);
        glow.setSpread(0.05);
        table.setEffect(glow);

        // Row factory — zebra + hover glow
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");

            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("-fx-background-color: transparent;");
                } else {
                    int idx = row.getIndex();
                    String base = (idx % 2 == 0)
                            ? "-fx-background-color: " + C_CARD + ";"
                            : "-fx-background-color: " + C_CARD2 + ";";
                    row.setStyle(base);
                }
            });

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle(
                            "-fx-background-color: rgba(0,217,255,0.07); " +
                                    "-fx-border-color: rgba(0,217,255,0.25); " +
                                    "-fx-border-width: 0 0 1 0;"
                    );
                    ScaleTransition st = new ScaleTransition(Duration.millis(120), row);
                    st.setToX(1.005); st.setToY(1.005);
                    st.play();
                }
            });

            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    int idx = row.getIndex();
                    row.setStyle((idx % 2 == 0)
                            ? "-fx-background-color: " + C_CARD + ";"
                            : "-fx-background-color: " + C_CARD2 + ";");
                    ScaleTransition st = new ScaleTransition(Duration.millis(120), row);
                    st.setToX(1.0); st.setToY(1.0);
                    st.play();
                }
            });
            return row;
        });
    }

    // ═══════════════════════════════════════════════════════
    //  COLUMN SETUP — ÉVÉNEMENTS
    // ═══════════════════════════════════════════════════════
    private void setupEventColumns() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre_e"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description_e"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_e"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation_e"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_e"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacitemax_e"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut_e"));
        colPrix_e.setCellValueFactory(new PropertyValueFactory<>("prix_e"));

        // Style all text columns
        styleCellFactory(colTitre,       C_TEXT,    true);
        styleCellFactory(colDescription, C_TEXT_DIM, false);
        styleCellFactory(colLocalisation,C_TEXT_DIM, false);
        styleCellFactory(colType,        C_CYAN,    false);
        styleCellFactory(colPrix_e,      C_GREEN,   true);

        // Price with "DT" badge
        colPrix_e.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle(
                        "-fx-text-fill: " + C_GREEN + "; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 13px; " +
                                "-fx-background-color: rgba(0,255,136,0.12); " +
                                "-fx-padding: 4 10; " +
                                "-fx-background-radius: 20;"
                );
                setGraphic(lbl); setText(null);
            }
        });

        // Capacite with progress-pill
        colCapacite.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label("👥 " + item);
                lbl.setStyle(
                        "-fx-text-fill: " + C_CYAN + "; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-background-color: rgba(0,217,255,0.10); " +
                                "-fx-padding: 4 10; " +
                                "-fx-background-radius: 20;"
                );
                setGraphic(lbl); setText(null);
            }
        });

        // Date formatted
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String formatted = item.toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm"));
                Label lbl = new Label("📅 " + formatted);
                lbl.setStyle("-fx-text-fill: rgba(200,255,220,0.7); -fx-font-size: 12px;");
                setGraphic(lbl); setText(null);
            }
        });
    }

    // ── Statut column with colored badge ──────────────────
    private void setupStatutColumn() {
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                String emoji, color, bg;
                switch (item.toLowerCase()) {
                    case "validé"   -> { emoji = "✔"; color = C_GREEN;  bg = "rgba(0,255,136,0.14)"; }
                    case "en cours" -> { emoji = "⏳"; color = C_WARN;   bg = "rgba(255,179,71,0.14)"; }
                    case "annulé"   -> { emoji = "✖"; color = C_DANGER; bg = "rgba(255,77,109,0.14)"; }
                    default         -> { emoji = "•";  color = C_CYAN;   bg = "rgba(0,217,255,0.10)"; }
                }

                Label badge = new Label(emoji + "  " + item);
                badge.setStyle(
                        "-fx-text-fill: " + color + "; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-color: " + bg + "; " +
                                "-fx-padding: 5 14; " +
                                "-fx-background-radius: 30; " +
                                "-fx-border-color: " + color + "; " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 30;"
                );

                // Pulse glow on "Validé"
                if (item.equalsIgnoreCase("validé")) {
                    FadeTransition pulse = new FadeTransition(Duration.millis(900), badge);
                    pulse.setFromValue(0.7); pulse.setToValue(1.0);
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.setAutoReverse(true);
                    pulse.play();
                }

                setGraphic(badge); setText(null);
            }
        });
    }

    // ── Image column ──────────────────────────────────────
    private void setupImageColumn() {
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(52); iv.setFitHeight(44);
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-background-radius: 10;");
                // Rounded clip
                Rectangle clip = new Rectangle(52, 44);
                clip.setArcWidth(10); clip.setArcHeight(10);
                iv.setClip(clip);
            }
            @Override protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isEmpty()) { setGraphic(null); return; }
                File f = new File("src/main/resources/" + path);
                if (f.exists()) {
                    iv.setImage(new Image(f.toURI().toString()));
                    DropShadow ds = new DropShadow();
                    ds.setColor(Color.web(C_CYAN, 0.3));
                    ds.setRadius(8);
                    iv.setEffect(ds);
                    setGraphic(iv);
                } else {
                    Label ph = new Label("🖼");
                    ph.setStyle("-fx-font-size: 22px; -fx-opacity: 0.4;");
                    setGraphic(ph);
                }
            }
        });
    }

    // ── Actions column — Événements ───────────────────────
    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit     = makeIconBtn("✏", C_CYAN,   "Modifier");
            private final Button btnDelete   = makeIconBtn("🗑", C_DANGER, "Supprimer");
            private final Button btnValidate = makeIconBtn("✔", C_GREEN,  "Valider");
            private final HBox   box         = new HBox(6, btnEdit, btnDelete, btnValidate);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    openEditDialog(ev);
                });
                btnDelete.setOnAction(e -> deleteEvenement(getTableView().getItems().get(getIndex())));
                btnValidate.setOnAction(e -> validateEvenement(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    //  COLUMN SETUP — PARTICIPATIONS
    // ═══════════════════════════════════════════════════════
    private void setupParticipationColumns() {

        // Objectif
        if (colObjectif != null) {
            colObjectif.setCellValueFactory(cell -> {
                String obj = cell.getValue().getObjectif();
                return new SimpleStringProperty(obj != null ? obj : "—");
            });
            colObjectif.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setGraphic(null); return; }
                    Label lbl = new Label(item);
                    lbl.setWrapText(true);
                    lbl.setMaxWidth(260);
                    lbl.setStyle(
                            "-fx-text-fill: " + C_TEXT + "; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-line-spacing: 2;"
                    );
                    setGraphic(lbl); setText(null);
                }
            });
        }

        // Mode participation — animated badge
        if (colModeParticipation != null) {
            colModeParticipation.setCellValueFactory(cell -> {
                String mode = cell.getValue().getModeparticipation();
                return new SimpleStringProperty(mode != null ? mode : "");
            });
            colModeParticipation.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setGraphic(null); return; }

                    boolean isPresentiel = item.equalsIgnoreCase("presentiel");
                    String emoji  = isPresentiel ? "🏢" : "💻";
                    String label  = isPresentiel ? "Présentiel" : "En ligne";
                    String color  = isPresentiel ? C_GREEN : C_CYAN;
                    String bg     = isPresentiel ? "rgba(0,255,136,0.12)" : "rgba(0,217,255,0.12)";

                    Label badge = new Label(emoji + "  " + label);
                    badge.setStyle(
                            "-fx-text-fill: " + color + "; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-color: " + bg + "; " +
                                    "-fx-padding: 5 14; " +
                                    "-fx-background-radius: 30; " +
                                    "-fx-border-color: " + color + "; " +
                                    "-fx-border-width: 1; " +
                                    "-fx-border-radius: 30;"
                    );

                    // Live dot for "En ligne"
                    if (!isPresentiel) {
                        Circle dot = new Circle(4, Color.web(C_CYAN));
                        FadeTransition blink = new FadeTransition(Duration.millis(700), dot);
                        blink.setFromValue(0.2); blink.setToValue(1.0);
                        blink.setCycleCount(Animation.INDEFINITE); blink.setAutoReverse(true);
                        blink.play();
                        HBox pill = new HBox(6, dot, badge);
                        pill.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(pill);
                    } else {
                        setGraphic(badge);
                    }
                    setText(null);
                }
            });
        }

        // Actions — Participations
        if (colActionsParticipation != null) {
            colActionsParticipation.setCellFactory(param -> new TableCell<>() {
                private final Button btnEdit   = makeIconBtn("✎", C_CYAN,   "Modifier");
                private final Button btnDelete = makeIconBtn("✖", C_DANGER, "Supprimer");
                private final HBox   box       = new HBox(6, btnEdit, btnDelete);
                {
                    box.setAlignment(Pos.CENTER);
                    btnEdit.setOnAction(e -> editParticipation(getTableView().getItems().get(getIndex())));
                    btnDelete.setOnAction(e -> deleteParticipation(getTableView().getItems().get(getIndex())));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    // ═══════════════════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════════════════
    private void loadEvenements() {
        try {
            ObservableList<Evenement> list = FXCollections.observableArrayList(serviceEvenement.recuperer());
            tableEvenements.setItems(list);
            animateTableLoad(tableEvenements);
        } catch (SQLException e) {
            showModernAlert("Erreur", "Impossible de charger les événements", Alert.AlertType.ERROR);
        }
    }

    private void filterEvenements() {
        if (searchField == null) return;
        String q = searchField.getText().trim().toLowerCase();
        try {
            ObservableList<Evenement> filtered = FXCollections.observableArrayList();
            for (Evenement ev : serviceEvenement.recuperer()) {
                if (ev.getTitre_e().toLowerCase().contains(q) ||
                        ev.getDescription_e().toLowerCase().contains(q) ||
                        ev.getType_e().toLowerCase().contains(q)) {
                    filtered.add(ev);
                }
            }
            tableEvenements.setItems(filtered);
            animateTableLoad(tableEvenements);
        } catch (SQLException ex) {
            showModernAlert("Erreur", "Erreur lors de la recherche", Alert.AlertType.ERROR);
        }
    }

    private void loadParticipations() {
        if (tableParticipations == null) return;
        try {
            List<Participation> list = serviceParticipation.recuperer();
            tableParticipations.getItems().setAll(list);
            animateTableLoad(tableParticipations);
        } catch (SQLException e) {
            showModernAlert("Erreur", "Impossible de charger les participations", Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  CRUD — ÉVÉNEMENTS
    // ═══════════════════════════════════════════════════════
    private void deleteEvenement(Evenement ev) {
        if (!confirmDialog("Supprimer l'événement",
                "Cette action est irréversible.\nVoulez-vous supprimer « " + ev.getTitre_e() + " » ?")) return;
        try {
            serviceEvenement.supprimer(ev);
            tableEvenements.getItems().remove(ev);
            showModernAlert("Succès", "Événement supprimé avec succès ✔", Alert.AlertType.INFORMATION);
        } catch (SQLException ex) {
            showModernAlert("Erreur", "Impossible de supprimer l'événement", Alert.AlertType.ERROR);
        }
    }

    private void validateEvenement(Evenement ev) {
        ev.setStatut_e("Validé");
        try {
            serviceEvenement.modifier(ev);
            loadEvenements();
            showModernAlert("Validé ✔", "L'événement « " + ev.getTitre_e() + " » est maintenant actif.", Alert.AlertType.INFORMATION);
        } catch (SQLException ex) {
            showModernAlert("Erreur", "Impossible de valider l'événement", Alert.AlertType.ERROR);
        }
    }

    private void openEditDialog(Evenement ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏  Modifier l'événement");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
                "-fx-background-color: " + C_CARD + "; " +
                        "-fx-border-color: " + C_CYAN + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18;"
        );

        ButtonType btnOK     = new ButtonType("💾  Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✕  Annuler",      ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnOK, btnCancel);

        // ── Form ──────────────────────────────────────────────
        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setPrefWidth(580);

        // Header
        Label header = new Label("✏  Modification de l'événement");
        header.setStyle(
                "-fx-text-fill: " + C_CYAN + "; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold;"
        );
        form.getChildren().add(header);
        form.getChildren().add(separator());

        TextField txtTitre      = styledField(ev.getTitre_e(), "Titre de l'événement");
        TextField txtImage      = styledField(ev.getImage() != null ? ev.getImage() : "", "Chemin de l'image");
        TextArea  txtDesc       = styledArea(ev.getDescription_e(), "Description");
        DatePicker datePicker   = styledDatePicker(ev.getDate_e().toLocalDateTime().toLocalDate());
        TextField txtLoc        = styledField(ev.getLocalisation_e(), "Localisation");
        TextField txtType       = styledField(ev.getType_e(), "Type d'événement");
        TextField txtCap        = styledField(String.valueOf(ev.getCapacitemax_e()), "Capacité maximale");
        TextField txtStatut     = styledField(ev.getStatut_e(), "Statut");
        TextField txtPrix       = styledField(ev.getPrix_e(), "Prix (ex: 100DT)");

        form.getChildren().addAll(
                formRow("📌 Titre",        txtTitre),
                formRow("🖼 Image",         txtImage),
                formRow("📝 Description",   txtDesc),
                formRow("📅 Date",          datePicker),
                formRow("📍 Localisation",  txtLoc),
                formRow("🎯 Type",          txtType),
                formRow("👥 Capacité",      txtCap),
                formRow("📊 Statut",        txtStatut),
                formRow("💰 Prix",          txtPrix)
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setContent(scroll);

        // Style dialog buttons
        pane.lookupButton(btnOK).setStyle(
                "-fx-background-color: linear-gradient(to right, " + C_CYAN + ", " + C_GREEN + "); " +
                        "-fx-text-fill: " + C_DARK_BG + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 20; " +
                        "-fx-cursor: hand;"
        );
        pane.lookupButton(btnCancel).setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: " + C_DANGER + "; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 20; " +
                        "-fx-background-radius: 20; " +
                        "-fx-text-fill: " + C_DANGER + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 24; " +
                        "-fx-cursor: hand;"
        );

        dialog.setResultConverter(btn -> {
            if (btn == btnOK) {
                try {
                    String titre = txtTitre.getText().trim();
                    if (titre.length() < 5 || titre.length() > 100) {
                        showModernAlert("Validation", "Le titre doit contenir entre 5 et 100 caractères.", Alert.AlertType.WARNING);
                        return null;
                    }
                    String desc = txtDesc.getText().trim();
                    if (desc.length() < 8 || desc.length() > 200) {
                        showModernAlert("Validation", "La description doit contenir entre 8 et 200 caractères.", Alert.AlertType.WARNING);
                        return null;
                    }
                    LocalDate date = datePicker.getValue();
                    if (date == null || date.isBefore(LocalDate.now())) {
                        showModernAlert("Validation", "La date doit être aujourd'hui ou dans le futur.", Alert.AlertType.WARNING);
                        return null;
                    }
                    int cap;
                    try { cap = Integer.parseInt(txtCap.getText().trim()); if (cap <= 0) throw new NumberFormatException(); }
                    catch (NumberFormatException e) {
                        showModernAlert("Validation", "La capacité doit être un entier positif.", Alert.AlertType.WARNING);
                        return null;
                    }
                    String prix = txtPrix.getText().trim();
                    if (!prix.matches("\\d+(\\.\\d+)?DT")) {
                        showModernAlert("Validation", "Format prix invalide. Exemple : 100DT", Alert.AlertType.WARNING);
                        return null;
                    }
                    ev.setTitre_e(titre);
                    ev.setImage(txtImage.getText().trim());
                    ev.setDescription_e(desc);
                    ev.setDate_e(Timestamp.valueOf(date.atStartOfDay()));
                    ev.setLocalisation_e(txtLoc.getText().trim());
                    ev.setType_e(txtType.getText().trim());
                    ev.setCapacitemax_e(cap);
                    ev.setStatut_e(txtStatut.getText().trim());
                    ev.setPrix_e(prix);
                    serviceEvenement.modifier(ev);
                    loadEvenements();
                    showModernAlert("Succès ✔", "Événement mis à jour avec succès !", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showModernAlert("Erreur", "Vérifiez les champs du formulaire.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    //  CRUD — PARTICIPATIONS
    // ═══════════════════════════════════════════════════════
    private void editParticipation(Participation p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✎  Modifier la participation");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
                "-fx-background-color: " + C_CARD + "; " +
                        "-fx-border-color: " + C_GREEN + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18;"
        );

        ButtonType btnSave   = new ButtonType("💾  Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✕  Annuler",      ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnSave, btnCancel);

        VBox form = new VBox(18);
        form.setPadding(new Insets(28));

        Label header = new Label("✎  Modifier la participation");
        header.setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        form.getChildren().addAll(header, separator());

        Label lblObj = new Label("Objectif de participation");
        lblObj.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-weight: 600; -fx-font-size: 13px;");

        TextArea objectifField = styledArea(p.getObjectif(), "Décrivez votre objectif (min. 10 caractères)");

        Label lblMode = new Label("Mode de participation");
        lblMode.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-weight: 600; -fx-font-size: 13px;");

        ToggleGroup modeGroup = new ToggleGroup();

        RadioButton rbPresentiel = new RadioButton("🏢  Présentiel");
        RadioButton rbEnLigne    = new RadioButton("💻  En ligne");
        rbPresentiel.setToggleGroup(modeGroup);
        rbEnLigne.setToggleGroup(modeGroup);
        rbPresentiel.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px;");
        rbEnLigne.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px;");

        if (p.getModeparticipation().equalsIgnoreCase("presentiel")) rbPresentiel.setSelected(true);
        else rbEnLigne.setSelected(true);

        HBox modeBox = new HBox(30, rbPresentiel, rbEnLigne);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        modeBox.setPadding(new Insets(6, 0, 0, 0));

        form.getChildren().addAll(lblObj, objectifField, lblMode, modeBox);
        pane.setContent(form);

        pane.lookupButton(btnSave).setStyle(
                "-fx-background-color: linear-gradient(to right, " + C_CYAN + ", " + C_GREEN + "); " +
                        "-fx-text-fill: " + C_DARK_BG + "; -fx-font-weight: bold; " +
                        "-fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;"
        );
        pane.lookupButton(btnCancel).setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + C_DANGER + "; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 20; -fx-background-radius: 20; " +
                        "-fx-text-fill: " + C_DANGER + "; -fx-font-weight: bold; " +
                        "-fx-padding: 10 24; -fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(res -> {
            if (res == btnSave) {
                String obj  = objectifField.getText().trim();
                String mode = rbPresentiel.isSelected() ? "presentiel" : "distanciel";
                if (obj.length() < 10) {
                    showModernAlert("Validation", "L'objectif doit contenir au moins 10 caractères.", Alert.AlertType.WARNING);
                    return;
                }
                p.setObjectif(obj);
                p.setModeparticipation(mode);
                try {
                    serviceParticipation.modifier(p);
                    loadParticipations();
                    showModernAlert("Succès ✔", "Participation mise à jour avec succès !", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showModernAlert("Erreur", "Impossible de modifier la participation.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deleteParticipation(Participation p) {
        if (!confirmDialog("Supprimer la participation", "Voulez-vous vraiment supprimer cette participation ?")) return;
        try {
            serviceParticipation.supprimer(p);
            loadParticipations();
            showModernAlert("Succès", "Participation supprimée ✔", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showModernAlert("Erreur", "Impossible de supprimer la participation.", Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  UI HELPERS — FORM WIDGETS
    // ═══════════════════════════════════════════════════════
    private TextField styledField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-prompt-text-fill: rgba(200,255,220,0.35); " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 10 14; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: " + C_BORDER + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 10;"
        );
        tf.focusedProperty().addListener((obs, o, focused) -> tf.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-prompt-text-fill: rgba(200,255,220,0.35); " +
                        "-fx-font-size: 13px; -fx-padding: 10 14; -fx-background-radius: 10; " +
                        "-fx-border-color: " + (focused ? C_CYAN : C_BORDER) + "; " +
                        "-fx-border-width: " + (focused ? "1.5" : "1") + "; -fx-border-radius: 10;"
        ));
        return tf;
    }

    private TextArea styledArea(String value, String prompt) {
        TextArea ta = new TextArea(value);
        ta.setPromptText(prompt);
        ta.setWrapText(true);
        ta.setPrefRowCount(3);
        ta.setStyle(
                "-fx-control-inner-background: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-prompt-text-fill: rgba(200,255,220,0.35); " +
                        "-fx-font-size: 13px; -fx-padding: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: " + C_BORDER + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 10;"
        );
        return ta;
    }

    private DatePicker styledDatePicker(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: " + C_BORDER + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 10;"
        );
        return dp;
    }

    /** Two-column form row: label left, control right */
    private HBox formRow(String labelText, javafx.scene.Node control) {
        Label lbl = new Label(labelText);
        lbl.setMinWidth(140);
        lbl.setStyle("-fx-text-fill: " + C_TEXT_DIM + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        HBox row = new HBox(12, lbl, control);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    private javafx.scene.shape.Line separator() {
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, 520, 0);
        line.setStroke(Color.web(C_CYAN, 0.2));
        line.setStrokeWidth(1);
        return line;
    }

    // ═══════════════════════════════════════════════════════
    //  UI HELPERS — BUTTONS
    // ═══════════════════════════════════════════════════════
    /** Small icon button for table action cells */
    private Button makeIconBtn(String icon, String color, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        String base =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-padding: 4 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace("transparent",
                color.replace("#", "rgba(") + ",0.15)")));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    /** Full-width navigation/action buttons */
    private void styleButton(Button btn, String color, String text) {
        if (btn == null) return;
        btn.setText(text);
        String base =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 13px; -fx-font-weight: 600; " +
                        "-fx-padding: 9 20; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 20; -fx-background-radius: 20; " +
                        "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + color + "; -fx-text-fill: " + C_DARK_BG + "; " +
                        "-fx-font-size: 13px; -fx-font-weight: 600; " +
                        "-fx-padding: 9 20; -fx-border-color: " + color + "; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleSearchField(TextField tf) {
        if (tf == null) return;
        tf.setPromptText("🔍  Rechercher un événement…");
        tf.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-prompt-text-fill: rgba(200,255,220,0.4); " +
                        "-fx-font-size: 13px; -fx-padding: 10 16; " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-color: " + C_BORDER + "; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 20;"
        );
        tf.focusedProperty().addListener((obs, o, focused) -> tf.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-prompt-text-fill: rgba(200,255,220,0.4); " +
                        "-fx-font-size: 13px; -fx-padding: 10 16; " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-color: " + (focused ? C_CYAN : C_BORDER) + "; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 20;"
        ));
    }

    // ═══════════════════════════════════════════════════════
    //  UI HELPERS — GENERIC CELL FACTORY
    // ═══════════════════════════════════════════════════════
    private void styleCellFactory(TableColumn<Evenement, String> col, String color, boolean bold) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setStyle(
                        "-fx-text-fill: " + color + "; " +
                                "-fx-font-size: 12px; " +
                                (bold ? "-fx-font-weight: bold; " : "") +
                                "-fx-padding: 6 8;"
                );
                setText(item);
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════
    private <T> void animateTableLoad(TableView<T> table) {
        FadeTransition ft = new FadeTransition(Duration.millis(350), table);
        ft.setFromValue(0.4); ft.setToValue(1.0);
        ft.play();
    }

    private void pulseButton(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), btn);
        st.setToX(0.92); st.setToY(0.92);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    // ═══════════════════════════════════════════════════════
    //  DIALOGS
    // ═══════════════════════════════════════════════════════
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
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 16; " +
                        "-fx-background-radius: 16;"
        );
        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px;"
        );
    }
}
