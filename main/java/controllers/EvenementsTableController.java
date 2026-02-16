package controllers;

import entities.Evenement;
import entities.Participation;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import services.ServiceEvenement;
import services.ServiceParticipation;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller pour la gestion des événements et participations
 * Avec design moderne vert et bleu ciel
 */
public class EvenementsTableController {

    // ==================== TABLE ÉVÉNEMENTS ====================
    @FXML private TableView<Evenement> tableEvenements;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colImage;
    @FXML private TableColumn<Evenement, String> colDescription;
    @FXML private TableColumn<Evenement, Timestamp> colDate;
    @FXML private TableColumn<Evenement, String> colLocalisation;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, Integer> colCapacite;
    @FXML private TableColumn<Evenement, String> colStatut;
    @FXML private TableColumn<Evenement, String> colPrix_e;
    @FXML private TableColumn<Evenement, Void> colActions;

    // ==================== TABLE PARTICIPATIONS ====================
    @FXML private TableView<Participation> tableParticipations;

    @FXML private TableColumn<Participation, String> colObjectif;
    @FXML private TableColumn<Participation, String> colModeParticipation;

    @FXML private TableColumn<Participation, Void> colActionsParticipation;

    // ==================== BOUTONS ET CHAMPS ====================
    @FXML private Button btnRefresh;
    @FXML private Button btnRefreshParticipations;
    @FXML private Button btnClose;
    @FXML private TextField searchField;
    @FXML private Button btnSearch;

    // ==================== SERVICES ====================
    private final ServiceEvenement serviceEvenement = new ServiceEvenement();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("EvenementsTableController initialized");

        // Setup événements
        setupEventColumns();
        setupImageColumn();
        setupActionsColumn();
        tableEvenements.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Setup participations SEULEMENT si la table existe
        if (tableParticipations != null) {
            setupParticipationColumns();
            tableParticipations.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }

        // Charger les données
        loadEvenements();
        if (tableParticipations != null) {
            loadParticipations();
        }

        // Event handlers
        if (btnRefresh != null) btnRefresh.setOnAction(e -> loadEvenements());
        if (btnRefreshParticipations != null) btnRefreshParticipations.setOnAction(e -> loadParticipations());
        if (btnClose != null) btnClose.setOnAction(e -> {
            if (tableEvenements.getScene() != null && tableEvenements.getScene().getWindow() != null) {
                tableEvenements.getScene().getWindow().hide();
            }
        });
        if (btnSearch != null) btnSearch.setOnAction(e -> filterEvenements());
    }

    // ==================== SETUP ÉVÉNEMENTS COLUMNS ====================

    private void setupEventColumns() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre_e"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description_e"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_e"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation_e"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_e"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacitemax_e"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut_e"));
        colPrix_e.setCellValueFactory(new PropertyValueFactory<>("prix_e"));
    }

    private void setupImageColumn() {
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));

        colImage.setCellFactory(col -> new TableCell<Evenement, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(50);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    File f = new File("src/main/resources/" + imagePath);
                    if (f.exists()) {
                        imageView.setImage(new Image(f.toURI().toString()));
                        setGraphic(imageView);
                    } else {
                        setGraphic(new Label("❌"));
                    }
                }
            }
        });
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Evenement, Void>, TableCell<Evenement, Void>> cellFactory = param ->
                new TableCell<>() {
                    private final Button btnEdit = new Button("✏");
                    private final Button btnDelete = new Button("🗑");
                    private final Button btnValidate = new Button("✔");
                    private final HBox pane = new HBox(5, btnEdit, btnDelete, btnValidate);

                    {
                        btnEdit.getStyleClass().add("form-btn");
                        btnDelete.getStyleClass().add("form-btn");
                        btnValidate.getStyleClass().add("form-btn");

                        btnEdit.setOnAction(e -> {
                            Evenement ev = getTableView().getItems().get(getIndex());
                            openEditDialog(ev);
                            tableEvenements.refresh();
                        });

                        btnDelete.setOnAction(e -> deleteEvenement(getTableView().getItems().get(getIndex())));
                        btnValidate.setOnAction(e -> validateEvenement(getTableView().getItems().get(getIndex())));
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
        colActions.setCellFactory(cellFactory);
    }

    // ==================== SETUP PARTICIPATIONS COLUMNS ====================

    private void setupParticipationColumns() {


        // Colonne Nom du participant (récupéré via id_u)


        // ✅ Colonne OBJECTIF - DIRECTEMENT depuis la base de données
        if (colObjectif != null) {
            colObjectif.setCellValueFactory(cellData -> {
                // Récupère directement l'objectif depuis l'entité Participation
                String objectif = cellData.getValue().getObjectif();
                return new SimpleStringProperty(objectif != null ? objectif : "Aucun objectif");
            });

            // Style avec wrap text pour les longs textes
            colObjectif.setCellFactory(column -> new TableCell<Participation, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                        setMaxWidth(250);
                        setPrefHeight(Control.USE_COMPUTED_SIZE);
                        setStyle("-fx-font-size: 12px; -fx-padding: 5;");
                    }
                }
            });
        }

        // ✅ Colonne MODE DE PARTICIPATION - DIRECTEMENT depuis la base de données
        if (colModeParticipation != null) {
            colModeParticipation.setCellValueFactory(cellData -> {
                // Récupère directement le mode depuis l'entité Participation
                String mode = cellData.getValue().getModeparticipation();

                // Formatage pour affichage avec emoji
                String displayMode;
                if (mode != null && mode.equalsIgnoreCase("presentiel")) {
                    displayMode = "🏢 Présentiel";
                } else if (mode != null && (mode.equalsIgnoreCase("distanciel") || mode.equalsIgnoreCase("en_ligne"))) {
                    displayMode = "💻 En ligne";
                } else {
                    displayMode = "❓ Non défini";
                }

                return new SimpleStringProperty(displayMode);
            });

            // Style avec couleur selon le mode
            colModeParticipation.setCellFactory(column -> new TableCell<Participation, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        // Couleur dynamique selon le mode
                        if (item.contains("Présentiel")) {
                            setStyle("-fx-text-fill: #00FF88; -fx-font-weight: bold; -fx-font-size: 13px;");
                        } else if (item.contains("En ligne")) {
                            setStyle("-fx-text-fill: #00D9FF; -fx-font-weight: bold; -fx-font-size: 13px;");
                        } else {
                            setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold; -fx-font-size: 13px;");
                        }
                    }
                }
            });
        }

        // Colonne Date d'inscription


        // Colonne ACTIONS Participation
        if (colActionsParticipation != null) {
            colActionsParticipation.setCellFactory(param -> new TableCell<>() {
                private final Button btnEdit = new Button("✎");
                private final Button btnDelete = new Button("✖");
                private final HBox actionBox = new HBox(5, btnEdit, btnDelete);

                {
                    actionBox.setAlignment(Pos.CENTER);

                    // Style bouton Modifier
                    btnEdit.setStyle(
                            "-fx-background-color: #00D9FF; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-padding: 5 10; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-background-radius: 5;"
                    );

                    // Style bouton Supprimer
                    btnDelete.setStyle(
                            "-fx-background-color: #FF6B6B; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-padding: 5 10; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-background-radius: 5;"
                    );

                    // Actions
                    btnEdit.setOnAction(event -> {
                        Participation participation = getTableView().getItems().get(getIndex());
                        editParticipation(participation);
                    });

                    btnDelete.setOnAction(event -> {
                        Participation participation = getTableView().getItems().get(getIndex());
                        deleteParticipation(participation);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : actionBox);
                }
            });
        }
    }

    // ==================== LOAD ÉVÉNEMENTS ====================

    private void loadEvenements() {
        ObservableList<Evenement> list = FXCollections.observableArrayList();
        try {
            list.addAll(serviceEvenement.recuperer());
            tableEvenements.setItems(list);
            System.out.println("✅ Événements chargés : " + list.size());
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du chargement des événements : " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements", Alert.AlertType.ERROR);
        }
    }

    private void filterEvenements() {
        if (searchField == null) return;

        String query = searchField.getText().toLowerCase();
        ObservableList<Evenement> filteredList = FXCollections.observableArrayList();
        try {
            for (Evenement e : serviceEvenement.recuperer()) {
                if (e.getTitre_e().toLowerCase().contains(query) ||
                        e.getDescription_e().toLowerCase().contains(query)) {
                    filteredList.add(e);
                }
            }
            tableEvenements.setItems(filteredList);
            System.out.println("🔍 Résultats filtrés : " + filteredList.size());
        } catch (SQLException ex) {
            System.err.println("❌ Erreur recherche : " + ex.getMessage());
        }
    }

    // ==================== LOAD PARTICIPATIONS ====================

    private void loadParticipations() {
        if (tableParticipations == null) {
            System.out.println("⚠️ Table participations non initialisée dans le FXML");
            return;
        }

        try {
            List<Participation> participations = serviceParticipation.recuperer();
            tableParticipations.getItems().clear();
            tableParticipations.getItems().addAll(participations);

            System.out.println("✅ Participations chargées : " + participations.size());

            // Debug : afficher les données chargées
            for (Participation p : participations) {
                System.out.println("  - ID: " + p.getId_p() +
                        ", Événement: " + p.getId_e() +
                        ", Mode: " + p.getModeparticipation() +
                        ", Objectif: " + (p.getObjectif() != null ? p.getObjectif().substring(0, Math.min(30, p.getObjectif().length())) + "..." : "null"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur SQL : " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les participations: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ==================== ÉVÉNEMENTS CRUD ====================

    private void deleteEvenement(Evenement ev) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer cet événement ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Supprimer l'événement");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceEvenement.supprimer(ev);
                    tableEvenements.getItems().remove(ev);
                    showAlert("Succès", "Événement supprimé", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    System.err.println("Erreur suppression : " + ex.getMessage());
                    showAlert("Erreur", "Impossible de supprimer l'événement", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void validateEvenement(Evenement ev) {
        ev.setStatut_e("Validé");
        try {
            serviceEvenement.modifier(ev);
            loadEvenements();
            showAlert("Succès", "Événement validé", Alert.AlertType.INFORMATION);
        } catch (SQLException ex) {
            System.err.println("Erreur validation : " + ex.getMessage());
            showAlert("Erreur", "Impossible de valider l'événement", Alert.AlertType.ERROR);
        }
    }

    private void openEditDialog(Evenement ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Événement");
        dialog.setHeaderText("Modification de l'événement : " + ev.getTitre_e());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/style/table.css").toExternalForm());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setPrefWidth(600);

        TextField txtTitre = new TextField(ev.getTitre_e());
        TextField txtImage = new TextField(ev.getImage() != null ? ev.getImage() : "");
        TextArea txtDesc = new TextArea(ev.getDescription_e());
        DatePicker datePicker = new DatePicker(ev.getDate_e().toLocalDateTime().toLocalDate());
        TextField txtLocalisation = new TextField(ev.getLocalisation_e());
        TextField txtType = new TextField(ev.getType_e());
        TextField txtCap = new TextField(String.valueOf(ev.getCapacitemax_e()));
        TextField txtStatut = new TextField(ev.getStatut_e());
        TextField txtPrix = new TextField(ev.getPrix_e());

        form.getChildren().addAll(
                new Label("Titre:"), txtTitre,
                new Label("Image:"), txtImage,
                new Label("Description:"), txtDesc,
                new Label("Date:"), datePicker,
                new Label("Localisation:"), txtLocalisation,
                new Label("Type:"), txtType,
                new Label("Capacité:"), txtCap,
                new Label("Statut:"), txtStatut,
                new Label("Prix:"), txtPrix
        );

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");

        dialogPane.setContent(scrollPane);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    String titre = txtTitre.getText().trim();
                    if (titre.length() < 5 || titre.length() > 100) {
                        showAlert("Erreur", "Le titre doit contenir entre 5 et 100 caractères.", Alert.AlertType.ERROR);
                        return null;
                    }

                    String desc = txtDesc.getText().trim();
                    if (desc.length() < 8 || desc.length() > 200) {
                        showAlert("Erreur", "La description doit contenir entre 8 et 200 caractères.", Alert.AlertType.ERROR);
                        return null;
                    }

                    LocalDate date = datePicker.getValue();
                    if (date == null || date.isBefore(LocalDate.now())) {
                        showAlert("Erreur", "La date doit être aujourd'hui ou dans le futur.", Alert.AlertType.ERROR);
                        return null;
                    }

                    int capacite;
                    try {
                        capacite = Integer.parseInt(txtCap.getText().trim());
                        if (capacite <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        showAlert("Erreur", "La capacité doit être un nombre positif.", Alert.AlertType.ERROR);
                        return null;
                    }

                    String prixStr = txtPrix.getText().trim();
                    if (!prixStr.matches("\\d+(\\.\\d+)?DT")) {
                        showAlert("Erreur", "Le prix doit être un nombre suivi de 'DT' (ex: 100DT).", Alert.AlertType.ERROR);
                        return null;
                    }

                    ev.setTitre_e(titre);
                    ev.setImage(txtImage.getText());
                    ev.setDescription_e(desc);
                    ev.setDate_e(Timestamp.valueOf(date.atStartOfDay()));
                    ev.setLocalisation_e(txtLocalisation.getText());
                    ev.setType_e(txtType.getText());
                    ev.setCapacitemax_e(capacite);
                    ev.setStatut_e(txtStatut.getText());
                    ev.setPrix_e(prixStr);

                    serviceEvenement.modifier(ev);
                    loadEvenements();
                    showAlert("Succès", "Événement modifié !", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Vérifiez les champs du formulaire.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ==================== PARTICIPATIONS CRUD ====================

    private void editParticipation(Participation participation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la participation");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
                "-fx-background-color: #132418; " +
                        "-fx-border-color: #00D9FF; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-background-radius: 15;"
        );

        ButtonType btnSave = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnSave, btnCancel);

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));

        Label lblObjectif = new Label("Objectif de participation");
        lblObjectif.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        TextArea objectifField = new TextArea(participation.getObjectif());
        objectifField.setWrapText(true);
        objectifField.setPrefRowCount(4);
        objectifField.setStyle(
                "-fx-control-inner-background: rgba(19, 36, 24, 0.6); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px;"
        );

        Label lblMode = new Label("Mode de participation");
        lblMode.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton rbPresentiel = new RadioButton("🏢 Présentiel");
        RadioButton rbEnLigne = new RadioButton("💻 En ligne");

        rbPresentiel.setToggleGroup(modeGroup);
        rbEnLigne.setToggleGroup(modeGroup);

        rbPresentiel.setStyle("-fx-text-fill: white;");
        rbEnLigne.setStyle("-fx-text-fill: white;");

        if (participation.getModeparticipation().equalsIgnoreCase("presentiel")) {
            rbPresentiel.setSelected(true);
        } else {
            rbEnLigne.setSelected(true);
        }

        HBox modeBox = new HBox(20, rbPresentiel, rbEnLigne);

        form.getChildren().addAll(lblObjectif, objectifField, lblMode, modeBox);
        pane.setContent(form);

        dialog.getDialogPane().lookupButton(btnSave).setStyle(
                "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88); " +
                        "-fx-text-fill: #0A0F0A; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 15;"
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnSave) {
                String newObjectif = objectifField.getText().trim();
                String newMode = rbPresentiel.isSelected() ? "presentiel" : "distanciel";

                if (newObjectif.length() < 10) {
                    showAlert("Erreur", "L'objectif doit contenir au moins 10 caractères", Alert.AlertType.WARNING);
                    return;
                }

                participation.setObjectif(newObjectif);
                participation.setModeparticipation(newMode);

                try {
                    serviceParticipation.modifier(participation);
                    loadParticipations();
                    showAlert("Succès", "Participation modifiée avec succès", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de modifier la participation", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deleteParticipation(Participation participation) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la participation");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette participation ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceParticipation.supprimer(participation);
                    loadParticipations();
                    showAlert("Succès", "Participation supprimée avec succès", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer la participation", Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ==================== UTILITY ====================

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
