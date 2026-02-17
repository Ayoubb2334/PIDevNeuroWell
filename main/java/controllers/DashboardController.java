package controllers;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.io.IOException;

public class DashboardController {

    @FXML private StackPane contentPane;
    @FXML private VBox sidebar;

    // EVENEMENTS
    @FXML private Button btnEvenements;
    @FXML private VBox evenementSubMenu;
    @FXML private Button btnAfficherEvenements;
    @FXML private Button btnAjouterEvenement;

    // CONGES
    @FXML private Button btnConges;
    @FXML private VBox congeSubMenu;
    @FXML private Button btnAjouterConge;
    @FXML private Button btnAfficherConge;

    // PAIEMENT
    @FXML private Button btnPaiement;

    // FRONT
    @FXML private Button btnFront;

    @FXML
    public void initialize() {

        if (sidebar != null) {
            VBox.setVgrow(sidebar, Priority.ALWAYS);
        }

        // FRONT
        btnFront.setOnAction(this::openFrontPage);

        // EVENEMENTS
        evenementSubMenu.setVisible(false);
        evenementSubMenu.setManaged(false);

        btnEvenements.setOnAction(e ->
                toggleMenu(evenementSubMenu));

        btnAfficherEvenements.setOnAction(e ->
                loadView("/views/EvenementsTable.fxml"));

        btnAjouterEvenement.setOnAction(e ->
                loadView("/views/evenement.fxml"));

        // CONGES
        congeSubMenu.setVisible(false);
        congeSubMenu.setManaged(false);

        btnConges.setOnAction(e ->
                toggleMenu(congeSubMenu));

        btnAfficherConge.setOnAction(e ->
                loadView("/views/ReponseCongeTable.fxml"));

        btnAjouterConge.setOnAction(e ->
                loadView("/views/ReponseConge.fxml"));

        // PAIEMENT (double sécurité)
        btnPaiement.setOnAction(this::openPaiement);
    }

    // ================= PAIEMENT =================
    @FXML
    private void openPaiement(ActionEvent event) {
        loadView("/views/Paiement.fxml");
    }

    // ================= FRONT =================
    private void openFrontPage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/views/front.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Front Office");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Erreur chargement Front.fxml");
        }
    }

    // ================= TOGGLE MENU =================
    private void toggleMenu(VBox menu) {
        boolean visible = menu.isVisible();
        menu.setVisible(!visible);
        menu.setManaged(!visible);
    }

    // ================= LOAD VIEW =================
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );

            Node page = loader.load();

            contentPane.getChildren().setAll(page);

            FadeTransition ft =
                    new FadeTransition(Duration.millis(300), page);

            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Erreur chargement : " + fxmlPath);
        }
    }
}
