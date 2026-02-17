package controllers;

import javafx.scene.layout.Priority;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class DashboardController {

    @FXML private StackPane contentPane;

    // ===== EVENEMENTS =====
    @FXML private Button btnEvenements;
    @FXML private VBox   evenementSubMenu;
    @FXML private Button btnAfficherEvenements;
    @FXML private Button btnAjouterEvenement;

    // ===== CONGES =====
    @FXML private Button btnConges;
    @FXML private VBox   congeSubMenu;
    @FXML private Button btnAjouterConge;
    @FXML private Button btnAfficherConge;

    // ===== EVALUATION =====
    @FXML private Button btnEvaluation;
    @FXML private VBox   evaluationSubMenu;
    @FXML private Button btnAjouterEvaluation;
    @FXML private Button btnAfficherEvaluation;
    @FXML private Button btnFrontEvaluation;

    // ===== RESSOURCES (NOUVEAU) =====
    @FXML private Button btnRessources;
    @FXML private VBox   ressourceSubMenu;
    @FXML private Button btnAjouterRessource;
    @FXML private Button btnAfficherRessources;
    @FXML private Button btnFrontRessources;

    // ===== FRONT =====
    @FXML private Button btnFront;
    @FXML private VBox   sidebar;

    @FXML
    public void initialize() {
        if (sidebar != null) {
            VBox.setVgrow(sidebar, Priority.ALWAYS);
        }

        // -------- FRONT --------
        btnFront.setOnAction(this::openFrontPage);

        // -------- EVENEMENTS --------
        evenementSubMenu.setVisible(false);
        evenementSubMenu.setManaged(false);
        btnEvenements.setOnAction(e -> toggleMenu(evenementSubMenu));
        btnAfficherEvenements.setOnAction(e -> loadView("/views/EvenementsTable.fxml"));
        btnAjouterEvenement.setOnAction(e -> loadView("/views/evenement.fxml"));

        // -------- CONGES --------
        congeSubMenu.setVisible(false);
        congeSubMenu.setManaged(false);
        btnConges.setOnAction(e -> toggleMenu(congeSubMenu));
        btnAfficherConge.setOnAction(e -> loadView("/views/ReponseCongeTable.fxml"));
        btnAjouterConge.setOnAction(e -> loadView("/views/ReponseConge.fxml"));

        // -------- EVALUATION --------
        evaluationSubMenu.setVisible(false);
        evaluationSubMenu.setManaged(false);
        btnEvaluation.setOnAction(e -> toggleMenu(evaluationSubMenu));
        btnAjouterEvaluation.setOnAction(e -> loadView("/views/AjoutEvaluation.fxml"));
        btnAfficherEvaluation.setOnAction(e -> loadView("/views/AfficherEvaluation.fxml"));
        btnFrontEvaluation.setOnAction(e -> openEvaluationFront());

        // -------- RESSOURCES (NOUVEAU) --------
        ressourceSubMenu.setVisible(false);
        ressourceSubMenu.setManaged(false);
        btnRessources.setOnAction(e -> toggleMenu(ressourceSubMenu));
        btnAjouterRessource.setOnAction(e -> loadView("/views/AjoutRessource.fxml"));
        btnAfficherRessources.setOnAction(e -> loadView("/views/AfficherRessource.fxml"));
        btnFrontRessources.setOnAction(e -> openRessourcesFront());
    }

    // ================= EVALUATION FRONT =================
    private void openEvaluationFront() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/EvaluationFront.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Evaluations - Front Office");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= RESSOURCES FRONT =================
    private void openRessourcesFront() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/RessourcesFront.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ressources - Front Office");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= OUVERTURE PAGE FRONT =================
    @FXML
    private void openFrontPage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/front.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Front Office");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= TOGGLE MENU =================
    private void toggleMenu(VBox menu) {
        boolean visible = menu.isVisible();
        menu.setVisible(!visible);
        menu.setManaged(!visible);
    }

    // ================= CHARGEMENT DANS CONTENT =================
    private void loadView(String fxmlPath) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(page);
            FadeTransition ft = new FadeTransition(Duration.millis(300), page);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
