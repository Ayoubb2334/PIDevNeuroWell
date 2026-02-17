package controllers;

import entities.Evaluation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ServiceEvaluation;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

public class AjoutEvaluationController {

    // PAS de idUserField — id_evaluation est AUTO_INCREMENT

    @FXML private ComboBox<String> typeTestField;
    @FXML private Label typeTestError;

    @FXML private TextField scoreField;
    @FXML private Label scoreError;

    @FXML private ComboBox<String> niveauField;
    @FXML private Label niveauError;

    @FXML private DatePicker dateField;
    @FXML private Label dateError;

    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;

    private ServiceEvaluation serviceEvaluation;

    @FXML
    public void initialize() {
        serviceEvaluation = new ServiceEvaluation();

        typeTestField.getItems().addAll("Stress", "Anxiété", "Dépression", "Bien-être", "Burnout");
        niveauField.getItems().addAll("Faible", "Moyen", "Élevé");

        clearErrors();

        submitBtn.setOnAction(event -> handleSubmit());
        cancelBtn.setOnAction(event -> handleCancel());

        // Auto-calcul du niveau selon le score
        scoreField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int s = Integer.parseInt(newVal.trim());
                if (s < 8)        niveauField.setValue("Faible");
                else if (s <= 14) niveauField.setValue("Moyen");
                else              niveauField.setValue("Élevé");
            } catch (NumberFormatException ignored) {}
        });
    }

    private void handleSubmit() {
        clearErrors();
        boolean valid = true;

        String typeTest  = typeTestField.getValue();
        String scoreText = scoreField.getText().trim();
        String niveau    = niveauField.getValue();
        LocalDate date   = dateField.getValue();

        // --- Validation Type Test ---
        if (typeTest == null || typeTest.isEmpty()) {
            typeTestError.setText("Le type de test est obligatoire.");
            valid = false;
        }

        // --- Validation Score ---
        int score = 0;
        if (scoreText.isEmpty()) {
            scoreError.setText("Le score est obligatoire.");
            valid = false;
        } else {
            try {
                score = Integer.parseInt(scoreText);
                if (score < 0 || score > 100) {
                    scoreError.setText("Le score doit être entre 0 et 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                scoreError.setText("Le score doit être un nombre entier.");
                valid = false;
            }
        }

        // --- Validation Niveau ---
        if (niveau == null || niveau.isEmpty()) {
            niveauError.setText("Le niveau est obligatoire.");
            valid = false;
        }

        // --- Validation Date ---
        if (date == null) {
            dateError.setText("La date est obligatoire.");
            valid = false;
        } else if (date.isAfter(LocalDate.now())) {
            dateError.setText("La date ne peut pas être dans le futur.");
            valid = false;
        }

        if (!valid) {
            showErrorMessage("Veuillez remplir correctement tous les champs.");
            return;
        }

        // --- Création sans id_user (auto-increment gère id_evaluation) ---
        Evaluation evaluation = new Evaluation();
        evaluation.setTypeTest(typeTest);
        evaluation.setScore(score);
        evaluation.setNiveau(niveau);
        evaluation.setDateEvaluation(Date.valueOf(date));
        // id_user mis à 0 ou une valeur par défaut si pas nécessaire
        evaluation.setIdUser(0);

        try {
            serviceEvaluation.ajouter(evaluation);
            showSuccessMessage("Évaluation ajoutée avec succès !");
            clearForm();
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Impossible d'ajouter l'évaluation : " + e.getMessage());
        }
    }

    private void handleCancel() {
        clearForm();
        clearErrors();
    }

    private void clearForm() {
        typeTestField.setValue(null);
        scoreField.clear();
        niveauField.setValue(null);
        dateField.setValue(null);
    }

    private void clearErrors() {
        typeTestError.setText("");
        scoreError.setText("");
        niveauError.setText("");
        dateError.setText("");
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
