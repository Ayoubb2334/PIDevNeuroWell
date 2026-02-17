package controllers;

import entities.Consultation;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class UserConsultationController {

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField txtHeure;

    @FXML
    private ComboBox<String> comboType;

    @FXML
    private TableView<Consultation> tableConsultations;

    @FXML
    private TableColumn<Consultation, LocalDate> colDate;

    @FXML
    private TableColumn<Consultation, LocalTime> colHeure;

    @FXML
    private TableColumn<Consultation, String> colPsychologue;

    @FXML
    private TableColumn<Consultation, String> colType;

    @FXML
    private TableColumn<Consultation, String> colStatut;

    private final ConsultationService service = new ConsultationService();

    private final int idUserConnecte = 11;   // 🔥 user existant
    private final int idPsychologueFixe = 10; // 🔥 psychologue existant

    @FXML
    public void initialize() {

        // 🔥 Bloquer dates passées
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (!empty && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        });

        // Remplir combo type
        comboType.setItems(FXCollections.observableArrayList(
                "presentiel",
                "en_ligne"
        ));

        // Lier colonnes table
        colDate.setCellValueFactory(data ->
                new SimpleObjectProperty<>(
                        data.getValue().getDateConsultation()
                ));

        colHeure.setCellValueFactory(data ->
                new SimpleObjectProperty<>(
                        data.getValue().getHeureConsultation()
                ));

        colType.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTypeConsultation()
                ));

        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getStatut()
                ));

        colPsychologue.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.valueOf(data.getValue().getIdPsychologue())
                ));

        loadConsultations();
    }

    @FXML
    public void ajouterConsultation() {

        try {

            if (datePicker.getValue() == null ||
                    txtHeure.getText().isEmpty() ||
                    comboType.getValue() == null) {

                showAlert("Veuillez remplir tous les champs !");
                return;
            }

            if (datePicker.getValue().isBefore(LocalDate.now())) {
                showAlert("Impossible de choisir une date passée !");
                return;
            }

            Consultation c = new Consultation();

            c.setIdUser(idUserConnecte);
            c.setIdPsychologue(idPsychologueFixe);
            c.setDateConsultation(datePicker.getValue());
            c.setHeureConsultation(LocalTime.parse(txtHeure.getText()));
            c.setTypeConsultation(comboType.getValue());
            c.setStatut("planifiee");

            service.ajouter(c);

            showAlert("Consultation ajoutée avec succès !");
            loadConsultations();
            clearForm();

        } catch (Exception e) {
            showAlert("Erreur : Vérifiez le format de l'heure (HH:mm)");
        }
    }

    private void loadConsultations() {

        try {

            List<Consultation> all = service.afficher();

            List<Consultation> filtered = all.stream()
                    .filter(c -> c.getIdUser() == idUserConnecte)
                    .collect(Collectors.toList());

            tableConsultations.setItems(
                    FXCollections.observableArrayList(filtered)
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        datePicker.setValue(null);
        txtHeure.clear();
        comboType.setValue(null);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
