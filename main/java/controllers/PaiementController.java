package controllers;

import entities.Paiement;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.PaiementService;

import java.sql.Date;
import java.sql.SQLException;

public class PaiementController {

    @FXML private TextField montantField;
    @FXML private TextField modeField;
    @FXML private TextField statutField;
    @FXML private TextField userField;
    @FXML private DatePicker datePicker;

    @FXML private TableView<Paiement> tablePaiement;
    @FXML private TableColumn<Paiement, Integer> colId;
    @FXML private TableColumn<Paiement, Double> colMontant;
    @FXML private TableColumn<Paiement, String> colMode;
    @FXML private TableColumn<Paiement, String> colStatut;
    @FXML private TableColumn<Paiement, Date> colDate;
    @FXML private TableColumn<Paiement, Integer> colUser;

    private final PaiementService service = new PaiementService();
    private final ObservableList<Paiement> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        colMontant.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getMontant()).asObject());

        colMode.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getModePaiement()));

        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatut()));

        colDate.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getDatePaiement()));

        colUser.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getIdUser()).asObject());

        loadPaiements();
    }

    @FXML
    public void ajouterPaiement() {

        if (!valider()) return;

        try {
            Paiement p = new Paiement(
                    Double.parseDouble(montantField.getText()),
                    modeField.getText(),
                    statutField.getText(),
                    Date.valueOf(datePicker.getValue()),
                    Integer.parseInt(userField.getText())
            );

            service.ajouter(p);
            showAlert("Paiement ajouté !");
            clearFields();
            loadPaiements();

        } catch (SQLException e) {
            showAlert("Erreur base de données !");
            e.printStackTrace();
        }
    }

    @FXML
    public void loadPaiements() {
        try {
            list.setAll(service.afficher());
            tablePaiement.setItems(list);
        } catch (SQLException e) {
            showAlert("Erreur chargement données !");
        }
    }

    @FXML
    public void supprimerPaiement() {

        Paiement selected = tablePaiement.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Sélectionnez un paiement !");
            return;
        }

        try {
            service.supprimer(selected.getId());
            showAlert("Paiement supprimé !");
            loadPaiements();
        } catch (SQLException e) {
            showAlert("Erreur suppression !");
        }
    }

    @FXML
    public void modifierPaiement() {

        Paiement selected = tablePaiement.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Sélectionnez un paiement !");
            return;
        }

        if (!valider()) return;

        try {
            selected.setMontant(Double.parseDouble(montantField.getText()));
            selected.setModePaiement(modeField.getText());
            selected.setStatut(statutField.getText());
            selected.setDatePaiement(Date.valueOf(datePicker.getValue()));
            selected.setIdUser(Integer.parseInt(userField.getText()));

            service.modifier(selected);
            showAlert("Paiement modifié !");
            clearFields();
            loadPaiements();

        } catch (SQLException e) {
            showAlert("Erreur modification !");
        }
    }

    private boolean valider() {

        if (montantField.getText().isEmpty()
                || modeField.getText().isEmpty()
                || statutField.getText().isEmpty()
                || userField.getText().isEmpty()
                || datePicker.getValue() == null) {

            showAlert("Tous les champs sont obligatoires !");
            return false;
        }

        try {
            Double.parseDouble(montantField.getText());
        } catch (NumberFormatException e) {
            showAlert("Montant doit être un nombre !");
            return false;
        }

        try {
            Integer.parseInt(userField.getText());
        } catch (NumberFormatException e) {
            showAlert("ID User doit être un entier !");
            return false;
        }

        return true;
    }

    private void clearFields() {
        montantField.clear();
        modeField.clear();
        statutField.clear();
        userField.clear();
        datePicker.setValue(null);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
