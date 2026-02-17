package controllers;

import entities.Evenement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import services.ServiceEvenement;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;


public class EvenementsTableController {

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

    @FXML private Button btnRefresh;
    @FXML private Button btnClose;
    @FXML private TextField searchField;
    @FXML private Button btnSearch;

    private final ServiceEvenement service = new ServiceEvenement();

    @FXML
    public void initialize() {
        setupColumns();
        setupImageColumn();
        setupActionsColumn();

        // Nouvelle syntaxe pour resize auto avec JavaFX 20+
        tableEvenements.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        loadEvenements();

        btnRefresh.setOnAction(e -> loadEvenements());
        btnClose.setOnAction(e -> tableEvenements.getScene().getWindow().hide());

        // Recherche
        btnSearch.setOnAction(e -> filterEvenements());
    }

    private void setupColumns() {
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
        // On dit à la colonne où trouver la donnée
        colImage.setCellValueFactory(new PropertyValueFactory<>("image")); // <-- très important

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

                        // ⚡ Edit
                        btnEdit.setOnAction(e -> {
                            Evenement ev = getTableView().getItems().get(getIndex());
                            openEditDialog(ev);  // dialog modifie l'objet
                            tableEvenements.refresh(); // <-- Important pour mettre à jour la vue
                        });

                        // Delete
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


    private void loadEvenements() {
        ObservableList<Evenement> list = FXCollections.observableArrayList();
        try {
            list.addAll(service.recuperer());
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des événements : " + e.getMessage());
        }
        tableEvenements.setItems(list);
    }

    private void filterEvenements() {
        String query = searchField.getText().toLowerCase();
        ObservableList<Evenement> filteredList = FXCollections.observableArrayList();
        try {
            for (Evenement e : service.recuperer()) {
                if (e.getTitre_e().toLowerCase().contains(query) ||
                        e.getDescription_e().toLowerCase().contains(query)) {
                    filteredList.add(e);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Erreur recherche : " + ex.getMessage());
        }
        tableEvenements.setItems(filteredList);
    }

    private void deleteEvenement(Evenement ev) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer cet événement ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Supprimer l'événement");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.supprimer(ev);
                    tableEvenements.getItems().remove(ev);
                } catch (SQLException ex) {
                    System.err.println("Erreur suppression : " + ex.getMessage());
                }
            }
        });
    }

    private void validateEvenement(Evenement ev) {
        ev.setStatut_e("Validé");
        try {
            service.modifier(ev);
            loadEvenements();
        } catch (SQLException ex) {
            System.err.println("Erreur validation : " + ex.getMessage());
        }
    }private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // On peut laisser vide si pas besoin de header
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void openEditDialog(Evenement ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Événement");
        dialog.setHeaderText("Modification de l'événement : " + ev.getTitre_e());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/style/table.css").toExternalForm());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // VBox avec formulaire
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
                    // Validation Titre
                    String titre = txtTitre.getText().trim();
                    if (titre.length() < 5 || titre.length() > 100) {
                        showAlert("Erreur", "Le titre doit contenir entre 5 et 100 caractères.", Alert.AlertType.ERROR);
                        return null;
                    }

                    // Validation Description
                    String desc = txtDesc.getText().trim();
                    if (desc.length() < 8 || desc.length() > 200) {
                        showAlert("Erreur", "La description doit contenir entre 8 et 200 caractères.", Alert.AlertType.ERROR);
                        return null;
                    }

                    // Validation Date
                    LocalDate date = datePicker.getValue();
                    if (date == null || date.isBefore(LocalDate.now())) {
                        showAlert("Erreur", "La date doit être aujourd'hui ou dans le futur.", Alert.AlertType.ERROR);
                        return null;
                    }

                    // Validation Capacité
                    int capacite;
                    try {
                        capacite = Integer.parseInt(txtCap.getText().trim());
                        if (capacite <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        showAlert("Erreur", "La capacité doit être un nombre positif.", Alert.AlertType.ERROR);
                        return null;
                    }

                    // Validation Prix
                    String prixStr = txtPrix.getText().trim();
                    if (!prixStr.matches("\\d+(\\.\\d+)?DT")) {
                        showAlert("Erreur", "Le prix doit être un nombre suivi de 'DT' (ex: 100DT).", Alert.AlertType.ERROR);
                        return null;
                    }

                    // Mise à jour des données
                    ev.setTitre_e(titre);
                    ev.setImage(txtImage.getText());
                    ev.setDescription_e(desc);
                    ev.setDate_e(Timestamp.valueOf(date.atStartOfDay()));
                    ev.setLocalisation_e(txtLocalisation.getText());
                    ev.setType_e(txtType.getText());
                    ev.setCapacitemax_e(capacite);
                    ev.setStatut_e(txtStatut.getText());
                    ev.setPrix_e(prixStr);

                    service.modifier(ev);
                    loadEvenements();
                    showAlert("Succès", "Événement modifié !", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Vérifiez les champs du formulaire.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }}

