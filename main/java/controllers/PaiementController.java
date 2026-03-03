package controllers;

import entities.Paiement;
import services.PaiementService;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PaiementController {

    @FXML private TextField montantField;
    @FXML private ComboBox<String> modeCombo;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField searchField;
    @FXML private VBox cardContainer;
    @FXML private Label totalRevenueLabel;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private Button addBtn;

    private PaiementService service = new PaiementService();

    @FXML
    public void initialize() {

        modeCombo.getItems().addAll("Stripe", "PayPal", "Paymee", "Flouci");
        statutCombo.getItems().addAll("REUSSI", "ECHOUE", "EN_ATTENTE");

        refreshCards();
        loadRevenue();
        loadChart();

        addBtn.setOnAction(e -> addPaiement());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                displayCards(service.search(newVal));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void addPaiement() {

        if (!validateInput()) return;

        String reference = "PAY-" + UUID.randomUUID().toString().substring(0,8);

        Paiement p = new Paiement(
                Double.parseDouble(montantField.getText()),
                LocalDate.now(),
                modeCombo.getValue(),
                statutCombo.getValue(),
                reference,
                1   // user connecté
        );

        try {
            service.add(p);
            clearForm();
            refreshCards();
            loadRevenue();
            loadChart();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private boolean validateInput() {

        if (montantField.getText().isEmpty()) return false;
        if (modeCombo.getValue() == null) return false;
        if (statutCombo.getValue() == null) return false;

        try {
            double m = Double.parseDouble(montantField.getText());
            if (m <= 0) return false;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void refreshCards() {
        try {
            displayCards(service.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayCards(List<Paiement> list) {

        cardContainer.getChildren().clear();

        for (Paiement p : list) {

            VBox card = new VBox(6);
            card.setStyle("-fx-background-color:#1e293b; -fx-padding:15; -fx-background-radius:10;");

            Label ref = new Label("Référence : " + p.getReference());
            ref.setStyle("-fx-text-fill:white;");

            Label user = new Label("Utilisateur : " + p.getUserName());
            user.setStyle("-fx-text-fill:#38bdf8;");

            Label montant = new Label("Montant : " + p.getMontant() + " DT");
            montant.setStyle("-fx-text-fill:#22c55e;");

            Label statut = new Label("Statut : " + p.getStatut());

            Button delete = new Button("Supprimer");

            delete.setOnAction(e -> {
                try {
                    service.delete(p.getId());
                    refreshCards();
                    loadRevenue();
                    loadChart();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            card.getChildren().addAll(ref, user, montant, statut, delete);
            cardContainer.getChildren().add(card);
        }
    }

    private void loadRevenue() {
        try {
            totalRevenueLabel.setText(service.getTotalRevenue() + " DT");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadChart() {

        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        try {
            for (int i = 1; i <= 12; i++) {
                series.getData().add(
                        new XYChart.Data<>("M" + i, service.getMonthlyRevenue(i))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        revenueChart.getData().add(series);
    }


    private void clearForm() {
        montantField.clear();
        modeCombo.setValue(null);
        statutCombo.setValue(null);
    }
}