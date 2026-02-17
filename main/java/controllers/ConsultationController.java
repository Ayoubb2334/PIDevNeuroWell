package controllers;

import entities.Consultation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.DateCell;
import services.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class ConsultationController {

    @FXML
    private TextField idUserField;

    @FXML
    private TextField idPsychologueField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField heureField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private ComboBox<String> statutComboBox;

    @FXML
    private Button btnAjouter;

    private ConsultationService consultationService;

    public ConsultationController() {
        consultationService = new ConsultationService();
    }

    @FXML
    public void initialize() {

        // Bloquer dates passées
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                }
            }
        });

        // Remplir ComboBox type
        typeComboBox.getItems().addAll(
                "presentiel",
                "en_ligne"
        );

        // Remplir ComboBox statut
        statutComboBox.getItems().addAll(
                "planifiee",
                "en_cours",
                "terminee",
                "annulee"
        );

        btnAjouter.setOnAction(event -> ajouterConsultation());
    }

    private void ajouterConsultation() {

        // ================= CHAMPS VIDES =================
        if (idUserField.getText().trim().isEmpty() ||
                idPsychologueField.getText().trim().isEmpty() ||
                datePicker.getValue() == null ||
                heureField.getText().trim().isEmpty() ||
                typeComboBox.getValue() == null ||
                statutComboBox.getValue() == null) {

            showAlert(Alert.AlertType.ERROR, "Erreur", "Tous les champs sont obligatoires !");
            return;
        }

        int idUser;
        int idPsychologue;

        // ================= ID NUMÉRIQUES =================
        try {
            idUser = Integer.parseInt(idUserField.getText().trim());
            idPsychologue = Integer.parseInt(idPsychologueField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les ID doivent être numériques !");
            return;
        }

        if (idUser <= 0 || idPsychologue <= 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les ID doivent être positifs !");
            return;
        }

        LocalDate date = datePicker.getValue();
        LocalDate today = LocalDate.now();

        // ================= DATE PASSÉE =================
        if (date.isBefore(today)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "La date ne peut pas être passée !");
            return;
        }

        LocalTime heure;

        // ================= FORMAT HEURE =================
        try {
            heure = LocalTime.parse(heureField.getText().trim());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format heure invalide (HH:mm)");
            return;
        }

        // ================= HEURE PASSÉE AUJOURD’HUI =================
        if (date.equals(today) && heure.isBefore(LocalTime.now())) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'heure est déjà passée !");
            return;
        }

        // ================= HORAIRES CABINET =================
        LocalTime ouverture = LocalTime.of(8, 0);
        LocalTime fermeture = LocalTime.of(18, 0);

        if (heure.isBefore(ouverture) || heure.isAfter(fermeture)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Heure autorisée entre 08:00 et 18:00 !");
            return;
        }

        try {

            // ================= DOUBLE RÉSERVATION =================
            if (consultationService.existeConsultation(idPsychologue, date, heure)) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Ce psychologue a déjà une consultation à cette heure !");
                return;
            }

            Consultation consultation = new Consultation(
                    idUser,
                    idPsychologue,
                    date,
                    heure,
                    typeComboBox.getValue(),
                    statutComboBox.getValue()
            );

            consultationService.ajouter(consultation);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Consultation ajoutée avec succès !");

            clearFields();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur Base de Données",
                    "Utilisateur ou Psychologue inexistant !");
        }
    }

    private void clearFields() {
        idUserField.clear();
        idPsychologueField.clear();
        datePicker.setValue(null);
        heureField.clear();
        typeComboBox.setValue(null);
        statutComboBox.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
