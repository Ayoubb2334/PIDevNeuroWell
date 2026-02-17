package controllers;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.ServiceEvenement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public class EvenementController {

    private Runnable onEventAdded;

    public void setOnEventAdded(Runnable onEventAdded) {
        this.onEventAdded = onEventAdded;
    }

    @FXML private TextField titreField;
    @FXML private TextField imageField;
    @FXML private Button btnChooseImage;
    @FXML private ImageView imagePreview;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dateField;
    @FXML private TextField localisationField;
    @FXML private TextField typeField;
    @FXML private TextField capaciteField;
    @FXML private TextField statutField;
    @FXML private TextField prixField;
    @FXML private Button btnAjouter;

    // Labels d'erreur
    @FXML private Label titreError;
    @FXML private Label imageError;
    @FXML private Label descriptionError;
    @FXML private Label dateError;
    @FXML private Label localisationError;
    @FXML private Label typeError;
    @FXML private Label capaciteError;
    @FXML private Label statutError;
    @FXML private Label prixError;

    private ServiceEvenement service;
    private String selectedImagePath = "";

    @FXML
    public void initialize() {
        service = new ServiceEvenement();

        btnAjouter.setOnAction(e -> ajouterEvenement());
        btnChooseImage.setOnAction(e -> choisirImage());

        addTextListeners();
    }

    private void addTextListeners() {
        titreField.textProperty().addListener((obs, o, n) -> titreError.setVisible(false));
        imageField.textProperty().addListener((obs, o, n) -> imageError.setVisible(false));
        descriptionField.textProperty().addListener((obs, o, n) -> descriptionError.setVisible(false));
        dateField.valueProperty().addListener((obs, o, n) -> dateError.setVisible(false));
        localisationField.textProperty().addListener((obs, o, n) -> localisationError.setVisible(false));
        typeField.textProperty().addListener((obs, o, n) -> typeError.setVisible(false));
        capaciteField.textProperty().addListener((obs, o, n) -> capaciteError.setVisible(false));
        statutField.textProperty().addListener((obs, o, n) -> statutError.setVisible(false));
        prixField.textProperty().addListener((obs, o, n) -> prixError.setVisible(false));
    }

    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image pour l'événement");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(btnChooseImage.getScene().getWindow());

        if (selectedFile != null) {
            try {
                String destFolder = "src/main/resources/images/evenements/";
                File destDir = new File(destFolder);
                if (!destDir.exists()) destDir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                File destFile = new File(destFolder + fileName);

                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                selectedImagePath = "images/evenements/" + fileName;
                imageField.setText(selectedImagePath);

                imagePreview.setImage(new Image(selectedFile.toURI().toString()));
                imageError.setVisible(false);

            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de copier l'image", Alert.AlertType.ERROR);
            }
        }
    }

    private void ajouterEvenement() {
        boolean valid = true;

        // Cacher tous les messages
        titreError.setVisible(false);
        imageError.setVisible(false);
        descriptionError.setVisible(false);
        dateError.setVisible(false);
        localisationError.setVisible(false);
        typeError.setVisible(false);
        capaciteError.setVisible(false);
        statutError.setVisible(false);
        prixError.setVisible(false);

        String titre = titreField.getText().trim();
        String imagePath = imageField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate date = dateField.getValue();
        String localisation = localisationField.getText().trim();
        String type = typeField.getText().trim();
        String capaciteStr = capaciteField.getText().trim();
        String statut = statutField.getText().trim();
        String prixStr = prixField.getText().trim();

        if (titre.isEmpty() || titre.length() < 5 || titre.length() > 100) {
            titreError.setText("Titre entre 5 et 100 caractères");
            titreError.setVisible(true);
            valid = false;
        }

        if (description.isEmpty() || description.length() < 8 || description.length() > 200) {
            descriptionError.setText("Description entre 8 et 200 caractères");
            descriptionError.setVisible(true);
            valid = false;
        }

        if (date == null) {
            dateError.setText("Date requise");
            dateError.setVisible(true);
            valid = false;
        } else if (date.isBefore(LocalDate.now())) {
            dateError.setText("La date ne peut pas être passée");
            dateError.setVisible(true);
            valid = false;
        }

        int capacite = 0;
        try {
            capacite = Integer.parseInt(capaciteStr);
            if (capacite <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            capaciteError.setText("Capacité invalide");
            capaciteError.setVisible(true);
            valid = false;
        }

        if (!prixStr.matches("\\d+(\\.\\d+)?DT")) {
            prixError.setText("Prix invalide (ex: 50DT)");
            prixError.setVisible(true);
            valid = false;
        }

        if (!valid) {
            showAlert("Erreur", "Certains champs sont invalides.", Alert.AlertType.ERROR);
            return;
        }

        try {
            Evenement e = new Evenement();
            e.setTitre_e(titre);
            e.setImage(imagePath.isEmpty() ? null : imagePath);
            e.setDescription_e(description);
            e.setDate_e(Timestamp.valueOf(date.atStartOfDay()));
            e.setLocalisation_e(localisation);
            e.setType_e(type);
            e.setCapacitemax_e(capacite);
            e.setStatut_e(statut.isEmpty() ? "En cours" : statut);
            e.setPrix_e(prixStr);

            service.ajouter(e);

            clearForm();
            showAlert("Succès", "Ajout effectué avec succès !", Alert.AlertType.INFORMATION);

            if (onEventAdded != null) onEventAdded.run();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'ajout à la base.", Alert.AlertType.ERROR);
        }
    }

    private void clearForm() {
        titreField.clear();
        imageField.clear();
        imagePreview.setImage(null);
        selectedImagePath = "";
        descriptionField.clear();
        dateField.setValue(null);
        localisationField.clear();
        typeField.clear();
        capaciteField.clear();
        statutField.clear();
        prixField.clear();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
