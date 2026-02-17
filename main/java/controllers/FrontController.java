package controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.io.IOException;

public class FrontController implements Initializable {

    // ================= MAIN CONTAINER =================
    @FXML
    private VBox mainContainer;

    // ================= NAVIGATION =================
    @FXML private Button btnHome;
    @FXML private Button btnServices;
    @FXML private Button btnMethode;
    @FXML private Button btnEvents;
    @FXML private Button btnBlog;
    @FXML private Button btnContact;

    // ================= HERO =================
    @FXML private ImageView heroImage;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;
    @FXML private Button btnGetStarted;
    @FXML private Button btnExplore;

    // ================= SERVICES =================
    @FXML private GridPane servicesGrid;

    // ================= FOOTER =================
    @FXML private TextField newsletterEmail;
    @FXML private Button btnNewsletter;

    // ================= BACKGROUND =================
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeAnimations();
    }

    // ================= CONSULTATION =================
    @FXML
    private void handleConsultation() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/userConsultation.fxml")
            );

            Parent view = loader.load();

            if (mainContainer != null) {
                mainContainer.getChildren().clear();
                mainContainer.getChildren().add(view);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de charger la page Consultation.",
                    AlertType.ERROR);
        }
    }

    // ================= EVENTS =================
    @FXML
    private void handleEvents(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/showEvent.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de charger les événements.",
                    AlertType.ERROR);
        }
    }

    // ================= CONTACT =================
    @FXML
    private void handleContact() {
        showAlert("Contact",
                "Formulaire de contact à venir.",
                AlertType.INFORMATION);
    }

    // ================= NEWSLETTER =================
    @FXML
    private void handleNewsletter() {

        if (newsletterEmail == null) return;

        String email = newsletterEmail.getText().trim();

        if (email.isEmpty()) {
            showAlert("Erreur",
                    "Veuillez entrer votre email.",
                    AlertType.WARNING);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showAlert("Erreur",
                    "Email invalide.",
                    AlertType.WARNING);
            return;
        }

        showAlert("Succès",
                "Inscription réussie.",
                AlertType.INFORMATION);

        newsletterEmail.clear();
    }

    // ================= ANIMATIONS =================
    private void initializeAnimations() {

        if (orb1 != null)
            animateOrb(orb1, 30, -20, Duration.seconds(15));

        if (orb2 != null)
            animateOrb(orb2, -30, 20, Duration.seconds(18));

        if (orb3 != null)
            animateOrb(orb3, 20, -15, Duration.seconds(20));
    }

    private void animateOrb(Circle orb,
                            double x,
                            double y,
                            Duration duration) {

        TranslateTransition transition =
                new TranslateTransition(duration, orb);

        transition.setByX(x);
        transition.setByY(y);
        transition.setAutoReverse(true);
        transition.setCycleCount(Animation.INDEFINITE);
        transition.play();
    }

    // ================= ALERT =================
    private void showAlert(String title,
                           String message,
                           AlertType type) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
