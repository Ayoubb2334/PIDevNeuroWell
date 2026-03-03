package controllers;
import entities.Consultation;
import entities.UserUnified;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import services.ServiceConsultation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CompteRendu {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DANGER = "#FF4D6D";
    private static final String C_ORANGE = "#FFB347";

    @FXML private Circle orb1, orb2, orb3;
    @FXML private Label lblTotal, lblPlanifiee, lblEnCours, lblTerminee;
    @FXML private TextField searchField;
    @FXML private Button btnFilterAll, btnFilterPlanifiee, btnFilterEnCours, btnFilterTerminee;

    @FXML private TableView<Consultation>         tableConsultations;
    @FXML private TableColumn<Consultation, Void> colPsy;
    @FXML private TableColumn<Consultation, Void> colDate;
    @FXML private TableColumn<Consultation, Void> colType;
    @FXML private TableColumn<Consultation, Void> colNotes;    // affiche le statut
    @FXML private TableColumn<Consultation, Void> colActions;

    private final ServiceConsultation service = new ServiceConsultation();
    private ObservableList<Consultation> allData = FXCollections.observableArrayList();
    private FilteredList<Consultation>   filtered;
    private String activeFilter = "tous";

    // ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        animateOrbs();
        styleFilterButtons();
        setupTable();
        loadData();
        setupSearch();
    }

    // ── Chargement ──────────────────────────────────────────────
    private void loadData() {
        try {
            // ✅ getToutesConsultations() — nom exact dans ServiceConsultation
            List<Consultation> list = service.getToutesConsultations();
            allData.setAll(list);
        } catch (SQLException e) {
            e.printStackTrace();
            allData.clear();
        }

        if (filtered == null) {
            filtered = new FilteredList<>(allData, c -> true);
            tableConsultations.setItems(filtered);
        }
        updateStats();
        applyFilter();
    }

    private void updateStats() {
        long total     = allData.size();
        long planifiee = allData.stream().filter(c -> "planifiee".equals(c.getStatut())).count();
        long enCours   = allData.stream().filter(c -> "en_cours".equals(c.getStatut())).count();
        long terminee  = allData.stream().filter(c -> "terminee".equals(c.getStatut())).count();
        if (lblTotal     != null) lblTotal.setText(String.valueOf(total));
        if (lblPlanifiee != null) lblPlanifiee.setText(String.valueOf(planifiee));
        if (lblEnCours   != null) lblEnCours.setText(String.valueOf(enCours));
        if (lblTerminee  != null) lblTerminee.setText(String.valueOf(terminee));
    }

    // ── Table ───────────────────────────────────────────────────
    private void setupTable() {
        tableConsultations.setStyle(
                "-fx-background-color: #0D1F12; -fx-border-color: rgba(0,217,255,0.15); " +
                        "-fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;");
        tableConsultations.setRowFactory(tv -> {
            TableRow<Consultation> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isEmpty())
                    row.setStyle(is ? "-fx-background-color: rgba(0,217,255,0.05);"
                            : "-fx-background-color: transparent;");
            });
            return row;
        });
        setupColPsy();
        setupColDate();
        setupColType();
        setupColNotes();
        setupColActions();
    }

    // Colonne : Psychologue
    private void setupColPsy() {
        colPsy.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                UserUnified psy = getTableRow().getItem().getPsychologue();

                StackPane avatar = new StackPane();
                Circle bg = new Circle(18);
                bg.setStyle("-fx-fill: rgba(0,217,255,0.10); -fx-stroke: rgba(0,217,255,0.30); -fx-stroke-width: 1.5;");
                Label lblInit = new Label(psy != null ? initials(psy.getNom(), psy.getPrenom()) : "?");
                lblInit.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 11px; -fx-font-weight: bold;");
                avatar.getChildren().addAll(bg, lblInit);

                Label lblName = new Label(psy != null ? "Dr. " + psy.getNom() + " " + psy.getPrenom() : "—");
                lblName.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 12px; -fx-font-weight: bold;");
                String spec = (psy != null && psy.getSpecialite() != null) ? psy.getSpecialite() : "Psychologie générale";
                Label lblSpec = new Label(spec);
                lblSpec.setStyle("-fx-text-fill: rgba(0,217,255,0.55); -fx-font-size: 10px;");

                VBox info = new VBox(2, lblName, lblSpec);
                HBox box  = new HBox(10, avatar, info);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
    }

    // Colonne : Date souhaitée
    private void setupColDate() {
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                LocalDateTime date = getTableRow().getItem().getDateConsultation();
                VBox box = new VBox(2);
                box.setAlignment(Pos.CENTER_LEFT);
                if (date != null) {
                    Label d = new Label(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    d.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 12px; -fx-font-weight: bold;");
                    Label h = new Label(date.format(DateTimeFormatter.ofPattern("HH:mm")));
                    h.setStyle("-fx-text-fill: rgba(0,217,255,0.65); -fx-font-size: 10px;");
                    box.getChildren().addAll(d, h);
                } else {
                    Label l = new Label("—"); l.setStyle("-fx-text-fill: rgba(200,255,220,0.35);");
                    box.getChildren().add(l);
                }
                setGraphic(box);
            }
        });
    }

    // Colonne : Type
    private void setupColType() {
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                boolean enLigne = "en_ligne".equals(getTableRow().getItem().getType());
                Label badge = new Label(enLigne ? "🌐 En ligne" : "🏥 Présentiel");
                badge.setStyle(
                        "-fx-background-color: " + (enLigne ? "rgba(0,217,255,0.12)" : "rgba(0,255,136,0.10)") + "; " +
                                "-fx-text-fill: " + (enLigne ? C_CYAN : C_GREEN) + "; " +
                                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4 12; " +
                                "-fx-background-radius: 20; " +
                                "-fx-border-color: " + (enLigne ? "rgba(0,217,255,0.30)" : "rgba(0,255,136,0.25)") + "; " +
                                "-fx-border-width: 1; -fx-border-radius: 20;");
                setGraphic(badge);
            }
        });
    }

    // Colonne : Notes → affiche le statut (Consultation n'a pas de champ notes)
    private void setupColNotes() {
        colNotes.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                String statut = getTableRow().getItem().getStatut();
                String color, bg, border, label;
                switch (statut != null ? statut : "") {
                    case "planifiee" -> { color = C_CYAN;   bg = "rgba(0,217,255,0.10)"; border = "rgba(0,217,255,0.30)";  label = "📅 Planifiée"; }
                    case "en_cours"  -> { color = C_ORANGE; bg = "rgba(255,179,71,0.10)"; border = "rgba(255,179,71,0.30)"; label = "⏳ En cours"; }
                    case "terminee"  -> { color = C_GREEN;  bg = "rgba(0,255,136,0.10)"; border = "rgba(0,255,136,0.30)";  label = "✅ Terminée"; }
                    case "annulee"   -> { color = C_DANGER; bg = "rgba(255,77,109,0.10)"; border = "rgba(255,77,109,0.30)"; label = "❌ Annulée"; }
                    default          -> { color = "rgba(200,255,220,0.40)"; bg = "rgba(200,255,220,0.05)"; border = "rgba(200,255,220,0.15)"; label = statut != null ? statut : "—"; }
                }
                Label badge = new Label(label);
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; " +
                        "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4 14; " +
                        "-fx-background-radius: 20; -fx-border-color: " + border + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 20;");
                setGraphic(badge);
            }
        });
    }

    // Colonne : Actions
    private void setupColActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setStyle(
                        "-fx-background-color: rgba(0,217,255,0.12); -fx-text-fill: #00D9FF; " +
                                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 14; " +
                                "-fx-border-color: rgba(0,217,255,0.30); -fx-border-width: 1; " +
                                "-fx-border-radius: 16; -fx-background-radius: 16; -fx-cursor: hand;");
                btnDelete.setStyle(
                        "-fx-background-color: rgba(255,77,109,0.10); -fx-text-fill: #FF4D6D; " +
                                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 14; " +
                                "-fx-border-color: rgba(255,77,109,0.30); -fx-border-width: 1; " +
                                "-fx-border-radius: 16; -fx-background-radius: 16; -fx-cursor: hand;");
                btnEdit.setOnAction(e   -> handleEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ── Recherche & Filtres ──────────────────────────────────────
    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void applyFilter() {
        if (filtered == null) return;
        String search = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        filtered.setPredicate(c -> {
            if (!"tous".equals(activeFilter) && !activeFilter.equals(c.getStatut())) return false;
            if (!search.isEmpty()) return c.getPsyFullName().toLowerCase().contains(search);
            return true;
        });
    }

    @FXML private void handleFilterAll()       { activeFilter = "tous";      applyFilter(); styleFilterButtons(); }
    @FXML private void handleFilterPlanifiee() { activeFilter = "planifiee"; applyFilter(); styleFilterButtons(); }
    @FXML private void handleFilterEnCours()   { activeFilter = "en_cours";  applyFilter(); styleFilterButtons(); }
    @FXML private void handleFilterTerminee()  { activeFilter = "terminee";  applyFilter(); styleFilterButtons(); }

    private void styleFilterButtons() {
        String on  = "-fx-background-color: linear-gradient(to right,#00D9FF,#00FF88); " +
                "-fx-text-fill: #050C07; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 7 18; -fx-background-radius: 18; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-text-fill: rgba(200,255,220,0.50); " +
                "-fx-font-size: 11px; -fx-padding: 7 18; -fx-border-color: rgba(200,255,220,0.18); " +
                "-fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18; -fx-cursor: hand;";
        if (btnFilterAll      != null) btnFilterAll.setStyle("tous".equals(activeFilter)           ? on : off);
        if (btnFilterPlanifiee!= null) btnFilterPlanifiee.setStyle("planifiee".equals(activeFilter) ? on : off);
        if (btnFilterEnCours  != null) btnFilterEnCours.setStyle("en_cours".equals(activeFilter)   ? on : off);
        if (btnFilterTerminee != null) btnFilterTerminee.setStyle("terminee".equals(activeFilter)  ? on : off);
    }

    // ── Actions ─────────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        openFormDialog(null);
    }

    private void handleEdit(Consultation c) {
        openFormDialog(c);
    }

    private void handleDelete(Consultation c) {
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32, 40, 32, 40));
        root.setStyle("-fx-background-color: #0D1F12; -fx-border-color: rgba(255,77,109,0.40); " +
                "-fx-border-width: 1.5; -fx-border-radius: 20; -fx-background-radius: 20;");
        root.setEffect(new DropShadow(30, Color.web(C_DANGER, 0.25)));

        Label title = new Label("Supprimer la consultation ?");
        title.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 15px; -fx-font-weight: bold;");

        String psyName = c.getPsychologue() != null
                ? "Dr. " + c.getPsychologue().getNom() + " " + c.getPsychologue().getPrenom() : "—";
        String dateStr = c.getDateConsultation() != null
                ? c.getDateConsultation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        Label detail = new Label(psyName + "\n" + dateStr);
        detail.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 12px;");
        detail.setAlignment(Pos.CENTER);

        Button btnCancel  = new Button("Annuler");
        Button btnConfirm = new Button("Supprimer");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(200,255,220,0.55); " +
                "-fx-font-size: 12px; -fx-padding: 9 24; -fx-border-color: rgba(200,255,220,0.20); " +
                "-fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18; -fx-cursor: hand;");
        btnConfirm.setStyle("-fx-background-color: #FF4D6D; -fx-text-fill: #fff; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 9 24; " +
                "-fx-border-radius: 18; -fx-background-radius: 18; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> popup.close());
        btnConfirm.setOnAction(e -> {
            try {
                // ✅ supprimerConsultation(int id) — méthode exacte du service
                service.supprimerConsultation(c.getId());
                loadData();
            } catch (SQLException ex) { ex.printStackTrace(); }
            popup.close();
        });

        HBox btns = new HBox(12, btnCancel, btnConfirm);
        btns.setAlignment(Pos.CENTER);
        root.getChildren().addAll(title, detail, btns);
        popup.setScene(new Scene(root, Color.TRANSPARENT));
        popup.showAndWait();
    }

    // ── Formulaire Modifier ──────────────────────────────────────
    private void openFormDialog(Consultation existing) {
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(32, 36, 32, 36));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #0D1F12; -fx-border-color: rgba(0,217,255,0.25); " +
                "-fx-border-width: 1.5; -fx-border-radius: 22; -fx-background-radius: 22;");
        root.setEffect(new DropShadow(40, Color.web(C_CYAN, 0.20)));

        boolean isEdit = existing != null;
        Label titleLbl = new Label(isEdit ? "✏  Modifier la consultation" : "+ Ajouter une consultation");
        titleLbl.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 16px; -fx-font-weight: bold;");
        root.getChildren().add(titleLbl);

        // Date
        root.getChildren().add(fieldLabel("Date souhaitée (yyyy-MM-dd HH:mm)"));
        TextField tfDate = styledTextField("yyyy-MM-dd HH:mm");
        tfDate.setText(isEdit && existing.getDateConsultation() != null
                ? existing.getDateConsultation().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        root.getChildren().add(tfDate);

        // Type
        root.getChildren().add(fieldLabel("Type de consultation"));
        ToggleButton tbPres = new ToggleButton("🏥 Présentiel");
        ToggleButton tbLine = new ToggleButton("🌐 En ligne");
        ToggleGroup  tg     = new ToggleGroup();
        tbPres.setToggleGroup(tg); tbLine.setToggleGroup(tg);
        if (isEdit && "en_ligne".equals(existing.getType())) tbLine.setSelected(true);
        else tbPres.setSelected(true);
        styleToggle(tbPres, tbLine);
        tbPres.selectedProperty().addListener((o, ov, nv) -> styleToggle(tbPres, tbLine));
        tbLine.selectedProperty().addListener((o, ov, nv) -> styleToggle(tbPres, tbLine));
        root.getChildren().add(new HBox(10, tbPres, tbLine));

        // Statut (uniquement en modification)
        ComboBox<String> cbStatut = null;
        if (isEdit) {
            root.getChildren().add(fieldLabel("Statut"));
            cbStatut = new ComboBox<>();
            cbStatut.getItems().addAll("planifiee", "en_cours", "terminee", "annulee");
            cbStatut.setValue(existing.getStatut() != null ? existing.getStatut() : "planifiee");
            cbStatut.setMaxWidth(Double.MAX_VALUE);
            cbStatut.setStyle("-fx-background-color: #112016; -fx-text-fill: #E8FFF0; " +
                    "-fx-border-color: rgba(0,217,255,0.20); -fx-border-width: 1.5; " +
                    "-fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 12px;");
            root.getChildren().add(cbStatut);
        }

        // Erreur
        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 11px;");
        lblErr.setVisible(false);

        // Boutons
        Button btnCancel = new Button("Annuler");
        Button btnSave   = new Button(isEdit ? "Enregistrer" : "Ajouter");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(200,255,220,0.55); " +
                "-fx-font-size: 12px; -fx-padding: 10 22; -fx-border-color: rgba(200,255,220,0.20); " +
                "-fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18; -fx-cursor: hand;");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right,#00D9FF,#00FF88); " +
                "-fx-text-fill: #050C07; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 10 22; -fx-border-radius: 18; -fx-background-radius: 18; -fx-cursor: hand;");

        final ComboBox<String> cbFinal = cbStatut;
        btnCancel.setOnAction(e -> popup.close());
        btnSave.setOnAction(e -> {
            LocalDateTime date;
            try {
                date = LocalDateTime.parse(tfDate.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception ex) {
                lblErr.setText("Format invalide. Utilisez : yyyy-MM-dd HH:mm"); lblErr.setVisible(true); return;
            }
            if (!isEdit && date.isBefore(LocalDateTime.now())) {
                lblErr.setText("La date doit être dans le futur."); lblErr.setVisible(true); return;
            }
            String type   = tbLine.isSelected() ? "en_ligne" : "presentiel";
            String statut = (cbFinal != null && cbFinal.getValue() != null) ? cbFinal.getValue() : "planifiee";
            try {
                if (isEdit) {
                    // ✅ modifierConsultation(Consultation) — méthode exacte du service
                    existing.setDateConsultation(date);
                    existing.setType(type);
                    existing.setStatut(statut);
                    service.modifierConsultation(existing);
                } else {
                    lblErr.setText("Utilisez le formulaire patient pour ajouter une consultation.");
                    lblErr.setVisible(true); return;
                }
                loadData();
                popup.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                lblErr.setText("Erreur : " + ex.getMessage()); lblErr.setVisible(true);
            }
        });

        root.getChildren().addAll(lblErr, new HBox(12, btnCancel, btnSave) {{ setAlignment(Pos.CENTER_RIGHT); }});
        popup.setScene(new Scene(root, Color.TRANSPARENT));
        popup.showAndWait();
    }

    // ── Helpers ─────────────────────────────────────────────────
    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 11px; -fx-font-weight: 600;");
        return l;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #112016; -fx-text-fill: #E8FFF0; " +
                "-fx-prompt-text-fill: rgba(200,255,220,0.25); -fx-font-size: 12px; " +
                "-fx-padding: 10 14; -fx-border-color: rgba(0,217,255,0.20); " +
                "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;");
        return tf;
    }

    private void styleToggle(ToggleButton tbPres, ToggleButton tbLine) {
        String on  = "-fx-background-color: #00D9FF; -fx-text-fill: #050C07; -fx-font-weight: bold; " +
                "-fx-font-size: 11px; -fx-padding: 8 20; -fx-background-radius: 18; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-text-fill: rgba(200,255,220,0.50); " +
                "-fx-font-size: 11px; -fx-padding: 8 20; -fx-border-color: rgba(200,255,220,0.18); " +
                "-fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18; -fx-cursor: hand;";
        tbPres.setStyle(tbPres.isSelected() ? on : off);
        tbLine.setStyle(tbLine.isSelected() ? on : off);
    }

    private String initials(String nom, String prenom) {
        String n = nom    != null && !nom.isEmpty()    ? String.valueOf(nom.charAt(0)).toUpperCase() : "";
        String p = prenom != null && !prenom.isEmpty() ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        return n + p;
    }

    // ── Orbes ───────────────────────────────────────────────────
    private void animateOrbs() {
        animateOrb(orb1,  40, -25, 14);
        animateOrb(orb2, -50,  35, 18);
        animateOrb(orb3,  25,  40, 22);
    }

    private void animateOrb(Circle o, double dx, double dy, double s) {
        if (o == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.seconds(s), o);
        tt.setByX(dx); tt.setByY(dy);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }
}