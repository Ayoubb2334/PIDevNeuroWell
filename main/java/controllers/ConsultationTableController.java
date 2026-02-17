package controllers;

import entities.Consultation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import services.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ConsultationTableController {

    @FXML
    private TableView<Consultation> tableConsultation;

    @FXML
    private TableColumn<Consultation, Integer> colId;

    @FXML
    private TableColumn<Consultation, Integer> colIdUser;

    @FXML
    private TableColumn<Consultation, Integer> colIdPsychologue;

    @FXML
    private TableColumn<Consultation, Object> colDate;

    @FXML
    private TableColumn<Consultation, Object> colHeure;

    @FXML
    private TableColumn<Consultation, String> colType;

    @FXML
    private TableColumn<Consultation, String> colStatut;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnSupprimer;

    private ConsultationService consultationService;

    public ConsultationTableController() {
        consultationService = new ConsultationService();
    }

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("idConsultation"));
        colIdUser.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        colIdPsychologue.setCellValueFactory(new PropertyValueFactory<>("idPsychologue"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateConsultation"));
        colHeure.setCellValueFactory(new PropertyValueFactory<>("heureConsultation"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeConsultation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        loadData();

        btnSupprimer.setOnAction(e -> supprimerConsultation());
        btnModifier.setOnAction(e -> modifierConsultation());
    }

    // ================= CHARGER TABLE =================
    private void loadData() {

        try {
            List<Consultation> list = consultationService.afficher();
            ObservableList<Consultation> observableList =
                    FXCollections.observableArrayList(list);
            tableConsultation.setItems(observableList);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur",
                    "Impossible de charger les données.");
        }
    }

    // ================= SUPPRIMER =================
    private void supprimerConsultation() {

        Consultation selected =
                tableConsultation.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Attention",
                    "Veuillez sélectionner une consultation.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer cette consultation ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {

            try {
                consultationService.supprimer(selected.getIdConsultation());

                showAlert(Alert.AlertType.INFORMATION,
                        "Succès",
                        "Consultation supprimée !");

                loadData();

            } catch (SQLException e) {

                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "Impossible de supprimer.");
            }
        }
    }

    // ================= MODIFIER (flexible) =================
    private void modifierConsultation() {

        Consultation selected =
                tableConsultation.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Attention",
                    "Veuillez sélectionner une consultation.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier Consultation");

        TextField psychologueField =
                new TextField(String.valueOf(selected.getIdPsychologue()));

        DatePicker datePicker =
                new DatePicker(selected.getDateConsultation());

        TextField heureField =
                new TextField(selected.getHeureConsultation().toString());

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("presentiel", "en_ligne");
        typeBox.setValue(selected.getTypeConsultation());

        ComboBox<String> statutBox = new ComboBox<>();
        statutBox.getItems().addAll("planifiee", "en_cours", "terminee", "annulee");
        statutBox.setValue(selected.getStatut());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Psychologue ID:"), 0, 0);
        grid.add(psychologueField, 1, 0);

        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);

        grid.add(new Label("Heure:"), 0, 2);
        grid.add(heureField, 1, 2);

        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeBox, 1, 3);

        grid.add(new Label("Statut:"), 0, 4);
        grid.add(statutBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {

                try {

                    int newPsychologue =
                            Integer.parseInt(psychologueField.getText());

                    LocalDate newDate =
                            datePicker.getValue();

                    LocalTime newHeure =
                            LocalTime.parse(heureField.getText());

                    String newType = typeBox.getValue();
                    String newStatut = statutBox.getValue();

                    // Vérification règle 15 minutes
                    if (consultationService.existeConsultation(
                            newPsychologue, newDate, newHeure)
                            &&
                            !(newPsychologue == selected.getIdPsychologue()
                                    && newDate.equals(selected.getDateConsultation())
                                    && newHeure.equals(selected.getHeureConsultation()))) {

                        showAlert(Alert.AlertType.ERROR,
                                "Erreur",
                                "Psychologue déjà occupé à cette heure.");
                        return;
                    }

                    selected.setIdPsychologue(newPsychologue);
                    selected.setDateConsultation(newDate);
                    selected.setHeureConsultation(newHeure);
                    selected.setTypeConsultation(newType);
                    selected.setStatut(newStatut);

                    consultationService.modifier(selected);

                    showAlert(Alert.AlertType.INFORMATION,
                            "Succès",
                            "Consultation modifiée !");

                    loadData();

                } catch (Exception e) {

                    showAlert(Alert.AlertType.ERROR,
                            "Erreur",
                            "Données invalides.");
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type,
                           String title,
                           String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
