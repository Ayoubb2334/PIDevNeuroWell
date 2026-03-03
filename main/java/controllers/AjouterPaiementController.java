package controllers;

import entities.Paiement;
import services.PaiementService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.UUID;

public class AjouterPaiementController {

    @FXML private TextField montantField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> modeCombo;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Button addBtn;

    private Paiement paiementToEdit;
    private final PaiementService service = new PaiementService();

    @FXML
    public void initialize() {
        modeCombo.getItems().addAll("Stripe", "PayPal", "Paymee", "Flouci");
        statutCombo.getItems().addAll("REUSSI", "ECHOUE", "EN_ATTENTE");
    }

    // ── Appelé par le bouton "ENREGISTRER" dans le FXML (onAction="#handleAjouter")
    @FXML
    private void handleAjouter() {
        handleSave();
    }

    // ── Appelé par le bouton "Annuler" dans le FXML (onAction="#handleAnnuler")
    @FXML
    private void handleAnnuler() {
        try {
            Stage stage = (Stage) addBtn.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            clear();
        }
    }

    private void handleSave() {
        try {
            if (montantField.getText().isEmpty()
                    || datePicker.getValue() == null
                    || modeCombo.getValue() == null
                    || statutCombo.getValue() == null) {
                showAlert("⚠️ Champs manquants", "Tous les champs sont obligatoires !", Alert.AlertType.WARNING);
                return;
            }

            double montant;
            try {
                montant = Double.parseDouble(montantField.getText().replace(",", "."));
            } catch (NumberFormatException ex) {
                showAlert("❌ Erreur", "Montant invalide. Entrez un nombre (ex: 108.90)", Alert.AlertType.ERROR);
                return;
            }

            Paiement p = new Paiement();
            p.setMontant(montant);
            p.setDatePaiement(datePicker.getValue());
            p.setModePaiement(modeCombo.getValue());
            p.setStatut(statutCombo.getValue());
            p.setUserId(1);

            if (paiementToEdit == null) {
                // ── MODE AJOUT ──
                p.setReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                service.add(p);
                showAlert("✅ Succès", "Paiement ajouté avec succès !", Alert.AlertType.INFORMATION);
                clear();
            } else {
                // ── MODE MODIFICATION ──
                p.setId(paiementToEdit.getId());
                p.setReference(paiementToEdit.getReference());
                service.update(p);
                showAlert("✅ Succès", "Paiement modifié avec succès !", Alert.AlertType.INFORMATION);
                Stage stage = (Stage) addBtn.getScene().getWindow();
                stage.close();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("❌ Erreur système", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Appelé depuis AfficherPaiementController pour pré-remplir le formulaire
    public void setPaiementToEdit(Paiement p) {
        this.paiementToEdit = p;
        montantField.setText(String.valueOf(p.getMontant()));
        datePicker.setValue(p.getDatePaiement());
        modeCombo.setValue(p.getModePaiement());
        statutCombo.setValue(p.getStatut());
        if (addBtn != null) addBtn.setText("💳  METTRE À JOUR");
    }

    private void clear() {
        montantField.clear();
        datePicker.setValue(null);
        modeCombo.setValue(null);
        statutCombo.setValue(null);
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}