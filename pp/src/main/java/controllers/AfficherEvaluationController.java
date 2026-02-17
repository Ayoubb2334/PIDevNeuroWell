package controllers;

import entities.Evaluation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import services.ServiceEvaluation;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class AfficherEvaluationController {

    @FXML private TableView<Evaluation>            tableEvaluations;
    @FXML private TableColumn<Evaluation, String>  typeTestCol;
    @FXML private TableColumn<Evaluation, Integer> scoreCol;
    @FXML private TableColumn<Evaluation, String>  niveauCol;
    @FXML private TableColumn<Evaluation, Date>    dateCol;
    @FXML private TableColumn<Evaluation, Void>    actionCol;

    private ServiceEvaluation serviceEvaluation;

    @FXML
    public void initialize() {
        serviceEvaluation = new ServiceEvaluation();
        tableEvaluations.setEditable(false);

        // Configuration des colonnes (lecture seule)
        typeTestCol.setCellValueFactory(new PropertyValueFactory<>("typeTest"));
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        niveauCol.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateEvaluation"));
        dateCol.setCellFactory(column -> new TableCell<Evaluation, Date>() {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(fmt.format(item));
                }
            }
        });

        addActionButtonsToTable();
        loadData();
    }

    private void loadData() {
        try {
            ObservableList<Evaluation> list = FXCollections.observableArrayList(
                    serviceEvaluation.recuperer()
            );
            tableEvaluations.setItems(list);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addActionButtonsToTable() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️ Modifier");
            private final Button btnDelete = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(5, btnEdit, btnDelete);
            
            {
                btnEdit.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                btnDelete.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                
                btnEdit.setOnAction(e -> {
                    Evaluation ev = getTableView().getItems().get(getIndex());
                    openModificationDialog(ev);
                });
                
                btnDelete.setOnAction(e -> {
                    Evaluation ev = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Voulez-vous vraiment supprimer cette évaluation ?");
                    
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                serviceEvaluation.supprimer(ev);
                                getTableView().getItems().remove(ev);
                                showAlert("Succès", "Évaluation supprimée !", Alert.AlertType.INFORMATION);
                            } catch (SQLException ex) {
                                showAlert("Erreur", ex.getMessage(), Alert.AlertType.ERROR);
                            }
                        }
                    });
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void openModificationDialog(Evaluation ev) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ModifierEvaluation.fxml"));
            Parent root = loader.load();

            ModifierEvaluationController controller = loader.getController();
            controller.setEvaluation(ev);

            Stage stage = new Stage();
            stage.setTitle("Modifier l'Évaluation");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Rafraîchir après modification
            loadData();

        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
