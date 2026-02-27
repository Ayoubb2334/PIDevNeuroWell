package controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import java.io.IOException;

public class FrontController implements Initializable {

    // ── Background Orbs ──
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    // ── Navbar ──
    @FXML private Button btnHome;
    @FXML private Button btnServices;
    @FXML private Button btnMethode;
    @FXML private Button btnEvents;
    @FXML private Button btnBlog;
    @FXML private Button btnContact;
    @FXML private Button btnAdmin;

    // ── Hero ──
    @FXML private ImageView heroImage;
    @FXML private Label     heroTitle;
    @FXML private Label     heroSubtitle;
    @FXML private Button    btnGetStarted;
    @FXML private Button    btnExplore;

    // ── About ──
    @FXML private ImageView aboutImage;

    // ── Services ──
    @FXML private GridPane servicesGrid;

    // ── Team ──
    @FXML private ImageView team1Image;
    @FXML private ImageView team2Image;
    @FXML private ImageView team3Image;

    // ── Footer ──
    @FXML private Label     footerHome;
    @FXML private Label     footerServices;
    @FXML private Label     footerMethode;
    @FXML private Label     footerBlog;
    @FXML private TextField newsletterEmail;
    @FXML private Button    btnNewsletter;

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeAnimations();
        setupEventHandlers();
        loadImages();
        setupHoverEffects();
    }

    // ═══════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════
    private void initializeAnimations() {
        if (orb1 != null && orb2 != null && orb3 != null) {
            animateOrb(orb1,  30, -20, Duration.seconds(15));
            animateOrb(orb2, -35,  25, Duration.seconds(18));
            animateOrb(orb3,  20, -15, Duration.seconds(20));
        }
        if (heroTitle != null) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1.5), heroTitle);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(300)); ft.play();
        }
        if (heroSubtitle != null) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1.5), heroSubtitle);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(600)); ft.play();
        }
        if (servicesGrid != null) animateServiceCards();
    }

    private void animateOrb(Circle orb, double dx, double dy, Duration dur) {
        TranslateTransition t = new TranslateTransition(dur, orb);
        t.setByX(dx); t.setByY(dy);
        t.setCycleCount(Animation.INDEFINITE);
        t.setAutoReverse(true);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.play();
    }

    private void animateServiceCards() {
        servicesGrid.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                int idx = servicesGrid.getChildren().indexOf(node);
                FadeTransition fade = new FadeTransition(Duration.millis(800), node);
                fade.setFromValue(0); fade.setToValue(1);
                fade.setDelay(Duration.millis(100L * idx));

                TranslateTransition slide = new TranslateTransition(Duration.millis(800), node);
                slide.setFromY(30); slide.setToY(0);
                slide.setDelay(Duration.millis(100L * idx));

                new ParallelTransition(fade, slide).play();
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    //  EVENT HANDLERS SETUP
    // ═══════════════════════════════════════════════════════
    private void setupEventHandlers() {
        if (btnHome     != null) btnHome.setOnAction(e -> handleNavigation("Home"));
        if (btnServices != null) btnServices.setOnAction(e -> handleNavigation("Services"));
        if (btnMethode  != null) btnMethode.setOnAction(e -> handleNavigation("Méthode"));
        if (btnBlog     != null) btnBlog.setOnAction(e -> handleNavigation("Blog"));
        if (btnGetStarted != null) btnGetStarted.setOnAction(e -> handleGetStarted());
        if (btnExplore    != null) btnExplore.setOnAction(e -> handleExplore());
        if (footerHome     != null) footerHome.setOnMouseClicked(e -> handleNavigation("Home"));
        if (footerServices != null) footerServices.setOnMouseClicked(e -> handleNavigation("Services"));
        if (footerMethode  != null) footerMethode.setOnMouseClicked(e -> handleNavigation("Méthode"));
        if (footerBlog     != null) footerBlog.setOnMouseClicked(e -> handleNavigation("Blog"));
    }

    private void setupHoverEffects() {
        if (btnGetStarted != null) addPulseEffect(btnGetStarted);
        if (btnContact    != null) addPulseEffect(btnContact);
    }

    private void addPulseEffect(Button button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(200), button);
            s.setToX(1.05); s.setToY(1.05); s.play();
        });
        button.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(200), button);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
    }

    // ═══════════════════════════════════════════════════════
    //  IMAGE LOADING
    // ═══════════════════════════════════════════════════════
    private void loadImages() {
        try {
            if (heroImage   != null) loadImageSafely(heroImage,   "/images/1.jpg");
            if (aboutImage  != null) loadImageSafely(aboutImage,  "/images/2.jpeg");
            if (team1Image  != null) loadImageSafely(team1Image,  "/images/team1.jpg");
            if (team2Image  != null) loadImageSafely(team2Image,  "/images/team2.jpg");
            if (team3Image  != null) loadImageSafely(team3Image,  "/images/team3.jpg");
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
        }
    }

    private void loadImageSafely(ImageView iv, String path) {
        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            if (!img.isError()) iv.setImage(img);
        } catch (Exception e) {
            System.err.println("Exception loading image " + path + ": " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FXML ACTIONS
    // ═══════════════════════════════════════════════════════

    /** Navigate back to Admin Dashboard */
    @FXML
    private void handleBackToAdmin(ActionEvent event) {
        navigateTo(event, "/views/dashboard.fxml");
    }

    /** Navigate to the Events front page */
    @FXML
    private void handleEvents(ActionEvent event) {
        navigateTo(event, "/views/showEvent.fxml");
    }

    /** Navigate to the Evaluations front page */
    @FXML
    private void handleEvaluations(ActionEvent event) {
        navigateTo(event, "/views/showEvaluation.fxml");
    }

    /** Navigate to the Ressources front page */
    @FXML
    private void handleRessources(ActionEvent event) {
        navigateTo(event, "/views/RessourcesFront.fxml");
    }

    /** Contact button */
    @FXML
    private void handleContact() {
        showNotification("Contact", "Formulaire de contact à venir");
    }

    /** Newsletter subscription */
    @FXML
    private void handleNewsletter() {
        if (newsletterEmail == null) return;
        String email = newsletterEmail.getText().trim();
        if (email.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer votre adresse email", Alert.AlertType.WARNING);
            return;
        }
        if (!isValidEmail(email)) {
            showAlert("Erreur", "Adresse email invalide", Alert.AlertType.WARNING);
            return;
        }
        if (btnNewsletter != null) animateButtonClick(btnNewsletter);
        showAlert("Succès", "Merci ! Vous êtes maintenant abonné à notre newsletter.", Alert.AlertType.INFORMATION);
        newsletterEmail.clear();
    }

    // ═══════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════
    private void handleNavigation(String section) {
        showNotification("Navigation", "Navigation vers " + section);
    }

    private void handleGetStarted() {
        if (btnGetStarted != null) animateButtonClick(btnGetStarted);
        showNotification("Bienvenue", "Commençons votre transformation mentale !");
    }

    private void handleExplore() {
        if (btnExplore != null) animateButtonClick(btnExplore);
        showNotification("Explorer", "Découvrez nos services");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setScene(new Scene(root));
                stage.show();
            });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page.", Alert.AlertType.ERROR);
        }
    }

    private void animateButtonClick(Button button) {
        ScaleTransition down = new ScaleTransition(Duration.millis(100), button);
        down.setToX(0.95); down.setToY(0.95);
        ScaleTransition up = new ScaleTransition(Duration.millis(100), button);
        up.setToX(1.0); up.setToY(1.0);
        new SequentialTransition(down, up).play();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    //  PUBLIC
    // ═══════════════════════════════════════════════════════
    public void refresh() {
        loadImages();
        initializeAnimations();
    }

    public void updateHeroContent(String title, String subtitle) {
        if (heroTitle    != null) heroTitle.setText(title);
        if (heroSubtitle != null) heroSubtitle.setText(subtitle);
    }
}
