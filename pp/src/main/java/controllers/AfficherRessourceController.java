package controllers;

import entities.Ressource;
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
import services.ServiceRessource;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class AfficherRessourceController {

    @FXML private TableView<Ressource> tableRessources;
    @FXML private TableColumn<Ressource, String> titreCol;
    @FXML private TableColumn<Ressource, String> descriptionCol;
    @FXML private TableColumn<Ressource, String> typeCol;
    @FXML private TableColumn<Ressource, String> formatCol;
    @FXML private TableColumn<Ressource, Double> tailleCol;
    @FXML private TableColumn<Ressource, String> statutCol;
    @FXML private TableColumn<Ressource, Date> dateCol;
    @FXML private TableColumn<Ressource, Void> actionCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatut;

    private ServiceRessource serviceRessource;
    private ObservableList<Ressource> allRessources;

    @FXML
    public void initialize() {
        serviceRessource = new ServiceRessource();
        tableRessources.setEditable(false);

        // Configuration des colonnes (lecture seule)
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        formatCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        tailleCol.setCellValueFactory(new PropertyValueFactory<>("tailleFichier"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        dateCol.setCellFactory(column -> new TableCell<Ressource, Date>() {
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

        // Configuration des filtres
        filterType.getItems().addAll("Tous", "PDF", "Vidéo", "Image", "Audio", "Article");
        filterType.setValue("Tous");
        filterType.setOnAction(e -> applyFilters());

        filterStatut.getItems().addAll("Tous", "brouillon", "publié", "archivé");
        filterStatut.setValue("Tous");
        filterStatut.setOnAction(e -> applyFilters());

        addActionButtonsToTable();
        loadData();
    }

    private void loadData() {
        try {
            allRessources = FXCollections.observableArrayList(serviceRessource.recuperer());
            tableRessources.setItems(allRessources);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les ressources : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        filterType.setValue("Tous");
        filterStatut.setValue("Tous");
        tableRessources.setItems(allRessources);
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedType = filterType.getValue();
        String selectedStatut = filterStatut.getValue();

        ObservableList<Ressource> filtered = allRessources.filtered(r -> {
            boolean matchSearch = searchText.isEmpty() ||
                    r.getTitre().toLowerCase().contains(searchText) ||
                    (r.getDescription() != null && r.getDescription().toLowerCase().contains(searchText));

            boolean matchType = selectedType.equals("Tous") || r.getType().equals(selectedType);

            boolean matchStatut = selectedStatut.equals("Tous") || r.getStatut().equals(selectedStatut);

            return matchSearch && matchType && matchStatut;
        });

        tableRessources.setItems(filtered);
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
                    Ressource r = getTableView().getItems().get(getIndex());
                    openModificationDialog(r);
                });

                btnDelete.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Voulez-vous vraiment supprimer cette ressource ?");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                serviceRessource.supprimer(r);
                                getTableView().getItems().remove(r);
                                allRessources.remove(r);
                                showAlert("Succès", "Ressource supprimée !", Alert.AlertType.INFORMATION);
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

    private void openModificationDialog(Ressource r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ModifierRessource.fxml"));
            Parent root = loader.load();

            ModifierRessourceController controller = loader.getController();
            controller.setRessource(r);

            Stage stage = new Stage();
            stage.setTitle("Modifier la Ressource");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Rafraîchir après modification
            loadData();
            applyFilters();

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
