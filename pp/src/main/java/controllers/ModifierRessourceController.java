package controllers;

import entities.Ressource;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ServiceRessource;

import java.io.File;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ModifierRessourceController {

    @FXML private TextField titreField;
    @FXML private Label titreError;

    @FXML private TextArea descriptionField;
    @FXML private Label descriptionError;

    @FXML private ComboBox<String> typeField;
    @FXML private Label typeError;

    @FXML private TextField cheminField;
    @FXML private Label cheminError;

    @FXML private TextField tailleField;
    @FXML private Label tailleError;

    @FXML private TextField formatField;
    @FXML private Label formatError;

    @FXML private DatePicker dateField;
    @FXML private Label dateError;

    @FXML private ComboBox<String> statutField;
    @FXML private Label statutError;

    @FXML private Button btnChooseFile;
    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;

    private ServiceRessource serviceRessource;
    private Ressource currentRessource;
    
    private final List<String> formatsAcceptes = Arrays.asList(
            "pdf", "jpg", "png", "doc", "docx", "ppt", "pptx", "mp4", "mp3"
    );
    private final double MAX_SIZE_KO = 5000;

    @FXML
    public void initialize() {
        serviceRessource = new ServiceRessource();

        typeField.getItems().addAll("PDF", "Vidéo", "Image", "Audio", "Article");
        statutField.getItems().addAll("brouillon", "publié", "archivé");

        tailleField.setEditable(false);
        formatField.setEditable(false);

        clearErrors();

        btnChooseFile.setOnAction(event -> handleChooseFile());
        submitBtn.setOnAction(event -> handleSubmit());
        cancelBtn.setOnAction(event -> handleCancel());
    }

    public void setRessource(Ressource r) {
        this.currentRessource = r;
        
        titreField.setText(r.getTitre());
        descriptionField.setText(r.getDescription());
        typeField.setValue(r.getType());
        cheminField.setText(r.getCheminFichier());
        tailleField.setText(String.valueOf(r.getTailleFichier()));
        formatField.setText(r.getFormat());
        dateField.setValue(r.getDatePublication().toLocalDate());
        statutField.setValue(r.getStatut());
    }

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier");
        
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3"),
                new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx", "*.ppt", "*.pptx")
        );

        File file = fileChooser.showOpenDialog(btnChooseFile.getScene().getWindow());

        if (file != null) {
            cheminField.setText(file.getAbsolutePath());
            
            double tailleKo = file.length() / 1024.0;
            tailleField.setText(String.format("%.2f", tailleKo).replace(",", "."));

            String fileName = file.getName();
            String extension = "";
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                extension = fileName.substring(dotIndex + 1).toLowerCase();
            }
            formatField.setText(extension);

            autoSelectType(extension);
        }
    }

    private void autoSelectType(String extension) {
        switch (extension) {
            case "pdf":
                typeField.setValue("PDF");
                break;
            case "jpg":
            case "jpeg":
            case "png":
                typeField.setValue("Image");
                break;
            case "mp4":
                typeField.setValue("Vidéo");
                break;
            case "mp3":
                typeField.setValue("Audio");
                break;
            case "doc":
            case "docx":
            case "ppt":
            case "pptx":
                typeField.setValue("Article");
                break;
        }
    }

    private void handleSubmit() {
        clearErrors();
        boolean valid = true;

        String titre = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        String type = typeField.getValue();
        String chemin = cheminField.getText().trim();
        String tailleText = tailleField.getText().trim();
        String format = formatField.getText().trim();
        LocalDate dateLocal = dateField.getValue();
        String statut = statutField.getValue();

        // Validation
        if (titre.isEmpty()) {
            titreError.setText("Le titre est obligatoire.");
            valid = false;
        } else if (titre.length() < 3) {
            titreError.setText("Le titre doit contenir au moins 3 caractères.");
            valid = false;
        }

        if (type == null || type.isEmpty()) {
            typeError.setText("Le type est obligatoire.");
            valid = false;
        }

        if (chemin.isEmpty()) {
            cheminError.setText("Veuillez sélectionner un fichier.");
            valid = false;
        }

        double taille = 0;
        if (tailleText.isEmpty()) {
            tailleError.setText("La taille du fichier est requise.");
            valid = false;
        } else {
            try {
                taille = Double.parseDouble(tailleText);
                if (taille > MAX_SIZE_KO) {
                    tailleError.setText("Fichier trop volumineux (max " + MAX_SIZE_KO + " Ko).");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                tailleError.setText("Taille invalide.");
                valid = false;
            }
        }

        if (format.isEmpty()) {
            formatError.setText("Le format est obligatoire.");
            valid = false;
        } else if (!formatsAcceptes.contains(format.toLowerCase())) {
            formatError.setText("Format non accepté.");
            valid = false;
        }

        if (type != null && !format.isEmpty() && !isTypeCompatible(type, format)) {
            formatError.setText("Le format ne correspond pas au type sélectionné.");
            valid = false;
        }

        if (dateLocal == null) {
            dateError.setText("La date est obligatoire.");
            valid = false;
        }

        if (statut == null || statut.isEmpty()) {
            statutError.setText("Le statut est obligatoire.");
            valid = false;
        }

        if (!valid) {
            showErrorMessage("Veuillez corriger les erreurs dans le formulaire.");
            return;
        }

        // Mise à jour
        currentRessource.setTitre(titre);
        currentRessource.setDescription(description);
        currentRessource.setType(type);
        currentRessource.setCheminFichier(chemin);
        currentRessource.setTailleFichier(taille);
        currentRessource.setFormat(format);
        currentRessource.setDatePublication(Date.valueOf(dateLocal));
        currentRessource.setStatut(statut);

        try {
            serviceRessource.modifier(currentRessource);
            showSuccessMessage("Ressource modifiée avec succès !");
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Impossible de modifier la ressource : " + e.getMessage());
        }
    }

    private boolean isTypeCompatible(String type, String format) {
        format = format.toLowerCase();

        switch (type) {
            case "PDF":
                return format.equals("pdf");
            case "Image":
                return format.equals("jpg") || format.equals("jpeg") || format.equals("png");
            case "Audio":
                return format.equals("mp3");
            case "Vidéo":
                return format.equals("mp4");
            case "Article":
                return format.equals("doc") || format.equals("docx") || 
                       format.equals("ppt") || format.equals("pptx");
            default:
                return false;
        }
    }

    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    private void clearErrors() {
        titreError.setText("");
        descriptionError.setText("");
        typeError.setText("");
        cheminError.setText("");
        tailleError.setText("");
        formatError.setText("");
        dateError.setText("");
        statutError.setText("");
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
