package controllers;

import entities.Consultation;
import entities.CompteRendu;
import entities.UserUnified;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.ServiceConsultation;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConsultationAdminController {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DARK   = "#050C07";
    private static final String C_DANGER = "#FF4D6D";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Circle orb1, orb2, orb3;
    @FXML private TextField searchField;
    @FXML private Label lblTotal, lblPlanifiee, lblEnCours, lblTerminee;
    @FXML private TableView<Consultation> tableConsultations;
    @FXML private TableColumn<Consultation, String> colPatient;
    @FXML private TableColumn<Consultation, String> colPsy;
    @FXML private TableColumn<Consultation, String> colDate;
    @FXML private TableColumn<Consultation, String> colType;
    @FXML private TableColumn<Consultation, String> colStatut;
    @FXML private TableColumn<Consultation, Void>   colActions;
    @FXML private Button btnFilterAll, btnFilterPlanifiee, btnFilterEnCours, btnFilterTerminee;

    private final ServiceConsultation service = new ServiceConsultation();
    private final ObservableList<Consultation> allData = FXCollections.observableArrayList();
    private FilteredList<Consultation> filteredData;
    private String currentFilter = "all";

    @FXML
    public void initialize() {
        animateOrbs();
        setupTable();
        setupSearch();
        setupFilterBtns();
        loadData();
    }

    private void setupTable() {
        tableConsultations.setStyle(
                "-fx-background-color: transparent; -fx-table-cell-border-color: transparent; " +
                        "-fx-border-color: rgba(0,217,255,0.12); -fx-border-width: 1; -fx-border-radius: 14;");
        tableConsultations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (colPatient != null) colPatient.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getPatientFullName()));
        if (colPsy != null) colPsy.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getPsyFullName()));
        if (colDate != null) colDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getDateConsultation() != null ?
                                d.getValue().getDateConsultation().format(FMT) : ""));
        if (colType != null) colType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getTypeDisplay()));

        if (colStatut != null) {
            colStatut.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(d.getValue().getStatut()));
            colStatut.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    setGraphic(statutBadge(item));
                    setText(null);
                }
            });
        }

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit = actionBtn("Modifier",  C_CYAN);
                private final Button btnDel  = actionBtn("Supprimer", C_DANGER);
                private final Button btnCR   = actionBtn("C.Rendus",  C_GREEN);
                private final HBox box = new HBox(6, btnEdit, btnDel, btnCR);
                {
                    box.setAlignment(Pos.CENTER);
                    btnEdit.setOnAction(e -> editConsultation(getTableView().getItems().get(getIndex())));
                    btnDel.setOnAction(e  -> deleteConsultation(getTableView().getItems().get(getIndex())));
                    btnCR.setOnAction(e   -> showComptesRendus(getTableView().getItems().get(getIndex())));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        tableConsultations.setRowFactory(tv -> {
            TableRow<Consultation> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            row.hoverProperty().addListener((o, old, h) ->
                    row.setStyle(h ? "-fx-background-color: rgba(0,217,255,0.05);"
                            : "-fx-background-color: transparent;"));
            return row;
        });
    }

    private void setupSearch() {
        filteredData = new FilteredList<>(allData, p -> true);
        tableConsultations.setItems(filteredData);
        if (searchField != null)
            searchField.textProperty().addListener((o, old, v) -> applyFilter());
    }

    private void setupFilterBtns() {
        String on  = "-fx-background-color:#00D9FF; -fx-text-fill:#050C07; -fx-font-size:11px; " +
                "-fx-font-weight:bold; -fx-padding:7 16; -fx-background-radius:20; -fx-cursor:hand;";
        String off = "-fx-background-color:transparent; -fx-text-fill:rgba(200,255,220,0.50); -fx-font-size:11px; " +
                "-fx-padding:7 16; -fx-border-color:rgba(200,255,220,0.18); -fx-border-width:1; " +
                "-fx-border-radius:20; -fx-background-radius:20; -fx-cursor:hand;";
        Button[] btns   = {btnFilterAll, btnFilterPlanifiee, btnFilterEnCours, btnFilterTerminee};
        String[] values = {"all", "planifiee", "en_cours", "terminee"};
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] == null) continue;
            btns[i].setStyle(i == 0 ? on : off);
            final String val = values[i];
            btns[i].setOnAction(e -> {
                currentFilter = val;
                for (Button b : btns) if (b != null) b.setStyle(off);
                ((Button) e.getSource()).setStyle(on);
                applyFilter();
            });
        }
    }

    private void applyFilter() {
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        filteredData.setPredicate(c -> {
            boolean statOk = "all".equals(currentFilter) || currentFilter.equals(c.getStatut());
            if (!statOk) return false;
            if (search.isEmpty()) return true;
            return (c.getPatientFullName() + " " + c.getPsyFullName()).toLowerCase().contains(search);
        });
        updateStats();
    }

    private void loadData() {
        try {
            allData.setAll(service.getToutesConsultations());
            applyFilter();
            updateStats();
        } catch (SQLException ex) {
            ex.printStackTrace();
            alert("Erreur", "Impossible de charger : " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateStats() {
        try {
            if (lblTotal     != null) lblTotal.setText(String.valueOf(service.countTotal()));
            if (lblPlanifiee != null) lblPlanifiee.setText(String.valueOf(service.countByStatut("planifiee")));
            if (lblEnCours   != null) lblEnCours.setText(String.valueOf(service.countByStatut("en_cours")));
            if (lblTerminee  != null) lblTerminee.setText(String.valueOf(service.countByStatut("terminee")));
        } catch (SQLException ignored) {}
    }

    @FXML private void handleAdd() { showConsultationDialog(null); }
    private void editConsultation(Consultation c)   { showConsultationDialog(c); }

    private void deleteConsultation(Consultation c) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Supprimer"); conf.setHeaderText(null);
        conf.setContentText("Supprimer cette consultation et ses comptes rendus ?");
        styleDialog(conf.getDialogPane());
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimerConsultation(c.getId()); loadData(); }
                catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  DIALOG AJOUTER / MODIFIER
    // ════════════════════════════════════════════════════════
    private void showConsultationDialog(Consultation existing) {
        boolean isNew = existing == null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouvelle consultation" : "Modifier la consultation");
        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);

        ButtonType save   = new ButtonType(isNew ? "Créer" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        VBox form = new VBox(14);
        form.setPadding(new Insets(22));
        form.setPrefWidth(500);

        Label title = new Label(isNew ? "➕ Nouvelle consultation" : "✏️ Modifier la consultation");
        title.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:17px; -fx-font-weight:bold;");

        // ── ✅ ComboBox Patient (objet UserUnified) ───────────
        ComboBox<UserUnified> cmbPatient = userComboBox();
        try {
            cmbPatient.getItems().addAll(service.getPatients());
        } catch (Exception ex) { ex.printStackTrace(); }

        // ── ✅ ComboBox Psychologue (objet UserUnified) ───────
        ComboBox<UserUnified> cmbPsy = userComboBoxPsy();
        try {
            cmbPsy.getItems().addAll(service.getPsychologues());
        } catch (Exception ex) { ex.printStackTrace(); }

        // ── Pré-sélection si modification ─────────────────────
        if (existing != null) {
            cmbPatient.getItems().stream()
                    .filter(u -> u.getId() == existing.getIdPatient())
                    .findFirst().ifPresent(cmbPatient::setValue);
            cmbPsy.getItems().stream()
                    .filter(u -> u.getId() == existing.getIdPsychologue())
                    .findFirst().ifPresent(cmbPsy::setValue);
        }

        // ── DatePicker ────────────────────────────────────────
        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setPromptText("Sélectionnez une date");
        datePicker.setStyle(
                "-fx-background-color:#112016; -fx-text-fill:#E8FFF0; " +
                        "-fx-border-color:rgba(0,217,255,0.20); -fx-border-width:1.5; " +
                        "-fx-border-radius:10; -fx-background-radius:10; -fx-font-size:12px;");

        // ✅ Bloquer dates passées
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(java.time.LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#1a0a0a; -fx-text-fill:#444;");
                }
            }
        });

        // ── Heure + Minutes ───────────────────────────────────
        ComboBox<String> cmbHeure  = styledComboItems("08","09","10","11","12","13","14","15","16","17","18","19","20");
        ComboBox<String> cmbMinute = styledComboItems("00","15","30","45");
        cmbHeure.setPrefWidth(90);
        cmbMinute.setPrefWidth(90);

        HBox heureBox = new HBox(10, new Label("h"), cmbHeure, new Label(":"), cmbMinute);
        heureBox.setAlignment(Pos.CENTER_LEFT);
        heureBox.getChildren().forEach(n -> {
            if (n instanceof Label)
                ((Label) n).setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:13px;");
        });

        // Pré-remplir si modification
        if (existing != null && existing.getDateConsultation() != null) {
            datePicker.setValue(existing.getDateConsultation().toLocalDate());
            cmbHeure.setValue(String.format("%02d", existing.getDateConsultation().getHour()));
            int m = existing.getDateConsultation().getMinute();
            cmbMinute.setValue(m < 8 ? "00" : m < 23 ? "15" : m < 38 ? "30" : "45");
        }

        // ── Type et Statut ─────────────────────────────────────
        ComboBox<String> cmbType   = styledComboItems("presentiel", "en_ligne");
        ComboBox<String> cmbStatut = styledComboItems("planifiee", "en_cours", "terminee", "annulee");
        if (existing != null) {
            if (existing.getType()   != null) cmbType.setValue(existing.getType());
            if (existing.getStatut() != null) cmbStatut.setValue(existing.getStatut());
        }

        // ── Labels d'erreur ────────────────────────────────────
        Label errPatient = errorLabel();
        Label errPsy     = errorLabel();
        Label errDate    = errorLabel();

        // ── Validation temps réel ──────────────────────────────
        cmbPatient.valueProperty().addListener((o, old, v) -> {
            if (v == null) showErr(errPatient, "Veuillez sélectionner un patient");
            else hideErr(errPatient);
        });
        cmbPsy.valueProperty().addListener((o, old, v) -> {
            if (v == null) showErr(errPsy, "Veuillez sélectionner un psychologue");
            else hideErr(errPsy);
        });
        datePicker.valueProperty().addListener((o, old, v) -> {
            if (v == null) showErr(errDate, "Veuillez sélectionner une date");
            else if (v.isBefore(java.time.LocalDate.now())) showErr(errDate, "❌ Date dans le passé");
            else hideErr(errDate);
        });

        // ── Assemblage ─────────────────────────────────────────
        form.getChildren().addAll(
                title,
                labeled("👤 Patient",     cmbPatient), errPatient,
                labeled("🧠 Psychologue", cmbPsy),     errPsy,
                labeled("📅 Date",        datePicker),  errDate,
                labeled("🕐 Heure",       heureBox),
                labeled("🎯 Type",        cmbType),
                labeled("📋 Statut",      cmbStatut)
        );
        pane.setContent(form);
        styleDialogBtn(pane.lookupButton(save),   isNew ? C_GREEN : C_CYAN);
        styleDialogBtn(pane.lookupButton(cancel), C_DANGER);

        // ── Validation finale ──────────────────────────────────
        pane.lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean valid = true;

            if (cmbPatient.getValue() == null) {
                showErr(errPatient, "⚠ Veuillez sélectionner un patient"); valid = false;
            }
            if (cmbPsy.getValue() == null) {
                showErr(errPsy, "⚠ Veuillez sélectionner un psychologue"); valid = false;
            }
            if (datePicker.getValue() == null) {
                showErr(errDate, "⚠ Veuillez sélectionner une date"); valid = false;
            } else if (datePicker.getValue().isBefore(java.time.LocalDate.now())) {
                showErr(errDate, "⚠ La date ne peut pas être dans le passé"); valid = false;
            } else {
                // ✅ Vérifier que l'heure n'est pas passée si c'est aujourd'hui
                java.time.LocalDateTime dt = java.time.LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.of(
                                Integer.parseInt(cmbHeure.getValue()),
                                Integer.parseInt(cmbMinute.getValue())));
                if (dt.isBefore(java.time.LocalDateTime.now())) {
                    showErr(errDate, "⚠ L'heure choisie est déjà passée"); valid = false;
                } else {
                    hideErr(errDate);
                }
            }
            if (!valid) event.consume();
        });

        // ── Sauvegarde ─────────────────────────────────────────
        dialog.showAndWait().ifPresent(r -> {
            if (r != save) return;
            try {
                Consultation c = existing != null ? existing : new Consultation();

                // ✅ Appel des objets UserUnified directement
                c.setPatient(cmbPatient.getValue());
                c.setPsychologue(cmbPsy.getValue());

                java.time.LocalDateTime dt = java.time.LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.of(
                                Integer.parseInt(cmbHeure.getValue()),
                                Integer.parseInt(cmbMinute.getValue())));
                c.setDateConsultation(dt);
                c.setType(cmbType.getValue());
                c.setStatut(cmbStatut.getValue());

                if (isNew) service.ajouterConsultation(c);
                else       service.modifierConsultation(c);
                loadData();
                alert("✅ Succès", isNew ? "Consultation créée !" : "Consultation modifiée !", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  COMPTES RENDUS
    // ════════════════════════════════════════════════════════
    private void showComptesRendus(Consultation c) {
        try {
            List<CompteRendu> crs = service.getComptesRendusParConsultation(c.getId());
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Comptes rendus — " + c.getPatientFullName());
            DialogPane pane = dialog.getDialogPane();
            styleDialog(pane);
            pane.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

            VBox box = new VBox(10);
            box.setPadding(new Insets(20));
            box.setPrefWidth(500);

            Label lbl = new Label("Comptes rendus (" + crs.size() + ")");
            lbl.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:16px; -fx-font-weight:bold;");
            box.getChildren().add(lbl);

            if (crs.isEmpty()) {
                Label empty = new Label("Aucun compte rendu.");
                empty.setStyle("-fx-text-fill:rgba(200,255,220,0.45); -fx-font-size:13px;");
                box.getChildren().add(empty);
            } else {
                for (CompteRendu cr : crs) {
                    VBox card = new VBox(6);
                    card.setStyle("-fx-background-color:#112016; -fx-border-color:rgba(0,217,255,0.20); " +
                            "-fx-border-width:1; -fx-border-radius:12; -fx-background-radius:12; -fx-padding:14;");
                    Label date = new Label(cr.getDateRedaction() != null ? cr.getDateRedaction().format(FMT) : "");
                    date.setStyle("-fx-text-fill:rgba(0,217,255,0.60); -fx-font-size:10px;");
                    Label txt = new Label(cr.getContenu());
                    txt.setWrapText(true);
                    txt.setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:12px;");
                    card.getChildren().addAll(date, txt);
                    box.getChildren().add(card);
                }
            }
            pane.setContent(box);
            dialog.showAndWait();
        } catch (SQLException ex) {
            alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    // ✅ ComboBox pour Patient — affiche nom + prénom
    private ComboBox<UserUnified> userComboBox() {
        ComboBox<UserUnified> c = new ComboBox<>();
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color:#112016; -fx-text-fill:#E8FFF0; " +
                "-fx-border-color:rgba(0,217,255,0.20); -fx-border-width:1.5; " +
                "-fx-border-radius:10; -fx-background-radius:10; -fx-font-size:12px;");
        c.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(UserUnified u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getNom() + " " + u.getPrenom());
            }
        });
        c.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(UserUnified u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getNom() + " " + u.getPrenom());
            }
        });
        return c;
    }

    // ✅ ComboBox pour Psychologue — affiche nom + prénom + spécialité
    private ComboBox<UserUnified> userComboBoxPsy() {
        ComboBox<UserUnified> c = new ComboBox<>();
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color:#112016; -fx-text-fill:#E8FFF0; " +
                "-fx-border-color:rgba(0,217,255,0.20); -fx-border-width:1.5; " +
                "-fx-border-radius:10; -fx-background-radius:10; -fx-font-size:12px;");
        c.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(UserUnified u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); return; }
                setText(u.getNom() + " " + u.getPrenom() +
                        (u.getSpecialite() != null ? " — " + u.getSpecialite() : ""));
            }
        });
        c.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(UserUnified u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getNom() + " " + u.getPrenom());
            }
        });
        return c;
    }

    private Label statutBadge(String statut) {
        Label l = new Label();
        String text, color, bg;
        switch (statut != null ? statut : "") {
            case "planifiee": text="Planifiée"; color="#00D9FF"; bg="rgba(0,217,255,0.12)"; break;
            case "en_cours":  text="En cours";  color="#FFB347"; bg="rgba(255,179,71,0.12)"; break;
            case "terminee":  text="Terminée";  color="#00FF88"; bg="rgba(0,255,136,0.12)"; break;
            case "annulee":   text="Annulée";   color="#FF4D6D"; bg="rgba(255,77,109,0.12)"; break;
            default:          text=statut;      color="#E8FFF0"; bg="rgba(200,255,220,0.10)"; break;
        }
        l.setText(text);
        l.setStyle("-fx-text-fill:"+color+"; -fx-font-size:10px; -fx-font-weight:bold; " +
                "-fx-background-color:"+bg+"; -fx-padding:3 10; " +
                "-fx-background-radius:12; -fx-border-color:"+color+"; " +
                "-fx-border-width:1; -fx-border-radius:12;");
        return l;
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        String base = "-fx-background-color:transparent; -fx-text-fill:"+color+"; " +
                "-fx-font-size:10px; -fx-font-weight:600; -fx-padding:4 10; " +
                "-fx-border-color:"+color+"; -fx-border-width:1; " +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;";
        String hov  = "-fx-background-color:"+color+"; -fx-text-fill:"+C_DARK+"; " +
                "-fx-font-size:10px; -fx-font-weight:600; -fx-padding:4 10; " +
                "-fx-border-color:"+color+"; -fx-border-width:1; " +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hov));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Label errorLabel() {
        Label l = new Label();
        l.setStyle("-fx-text-fill:#FF4D6D; -fx-font-size:10px; -fx-font-weight:bold;");
        l.setVisible(false); l.setManaged(false);
        return l;
    }
    private void showErr(Label l, String msg) { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void hideErr(Label l)             { l.setVisible(false); l.setManaged(false); }

    private ComboBox<String> styledComboItems(String... items) {
        ComboBox<String> c = new ComboBox<>();
        c.setMaxWidth(Double.MAX_VALUE);
        c.getItems().addAll(items);
        c.setValue(items[0]);
        c.setStyle("-fx-background-color:#112016; -fx-text-fill:#E8FFF0; " +
                "-fx-border-color:rgba(0,217,255,0.20); -fx-border-width:1.5; " +
                "-fx-border-radius:10; -fx-background-radius:10; -fx-font-size:12px;");
        return c;
    }

    private VBox labeled(String lbl, javafx.scene.Node f) {
        Label l = new Label(lbl);
        l.setStyle("-fx-text-fill:rgba(200,255,220,0.55); -fx-font-size:10px; -fx-font-weight:bold;");
        VBox b = new VBox(4, l, f);
        HBox.setHgrow(f, Priority.ALWAYS);
        return b;
    }

    private void styleDialog(DialogPane p) {
        p.setStyle("-fx-background-color:#0D1F12; -fx-border-color:rgba(0,217,255,0.25); " +
                "-fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18;");
        try { p.lookup(".content.label").setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:13px;"); }
        catch (Exception ignored) {}
    }

    private void styleDialogBtn(javafx.scene.Node btn, String color) {
        if (btn != null) btn.setStyle("-fx-background-color:"+color+"; -fx-text-fill:"+C_DARK+"; " +
                "-fx-font-weight:bold; -fx-padding:9 22; -fx-background-radius:18; -fx-cursor:hand;");
    }

    private void alert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleDialog(a.getDialogPane()); a.showAndWait();
    }

    private void animateOrbs() {
        animateOrb(orb1, 40, -25, 14);
        animateOrb(orb2, -50, 35, 18);
        animateOrb(orb3, 25, -40, 20);
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