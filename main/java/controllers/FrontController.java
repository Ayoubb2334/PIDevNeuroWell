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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import services.PaiementService;
import entities.Paiement;
import java.sql.Date;
import java.util.List;
import java.sql.SQLException;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import java.io.IOException;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


/**
 * Controller for the Front page of the Psychology & Personal Development App
 * Handles all UI interactions, animations, and event management
 */
public class FrontController implements Initializable {
    private PaiementService paiementService = new PaiementService();

    // ==================== FXML INJECTED ELEMENTS ====================

    // Animated Background Orbs
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    // Navigation Buttons
    @FXML private Button btnHome;
    @FXML private Button btnServices;
    @FXML private Button btnMethode;
    @FXML private Button btnEvents;
    @FXML private Button btnBlog;
    @FXML private Button btnContact;

    // Hero Section
    @FXML private ImageView heroImage;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;
    @FXML private Button btnGetStarted;
    @FXML private Button btnExplore;

    // About Section
    @FXML private ImageView aboutImage;

    // Services Section
    @FXML private GridPane servicesGrid;

    // Team Section
    @FXML private ImageView team1Image;
    @FXML private ImageView team2Image;
    @FXML private ImageView team3Image;

    // Footer
    @FXML private Label footerHome;
    @FXML private Label footerServices;
    @FXML private Label footerMethode;
    @FXML private Label footerBlog;
    @FXML private TextField newsletterEmail;
    @FXML private Button btnNewsletter;
    // ==================== PAYMENT SECTION ====================

    @FXML private VBox paymentSection;
    @FXML private ScrollPane rootScrollPane;
    @FXML private ComboBox<String> modeCombo;
    @FXML private TextField montantField;
    @FXML private DatePicker datePicker;
    @FXML private Label paymentStatusLabel;


    // ==================== INITIALIZATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("FrontController initialized");

        initializeAnimations();
        setupEventHandlers();
        loadImages();
        setupHoverEffects();

        // ================= PAYMENT INIT =================
        if (modeCombo != null) {
            modeCombo.getItems().addAll("Carte Bancaire", "Paypal", "Espèce");
        }
        chargerStatutPaiement();
    }

    // ==================== ANIMATION SETUP ====================

    /**
     * Initialize all animations for the page
     */
    private void initializeAnimations() {
        // Animate background orbs if they exist
        if (orb1 != null && orb2 != null && orb3 != null) {
            animateOrb(orb1, 30, -20, Duration.seconds(15));
            animateOrb(orb2, -35, 25, Duration.seconds(18));
            animateOrb(orb3, 20, -15, Duration.seconds(20));
        }

        // Animate hero title with fade-in effect
        if (heroTitle != null) {
            FadeTransition fadeTitle = new FadeTransition(Duration.seconds(1.5), heroTitle);
            fadeTitle.setFromValue(0);
            fadeTitle.setToValue(1);
            fadeTitle.setDelay(Duration.millis(300));
            fadeTitle.play();
        }

        // Animate hero subtitle
        if (heroSubtitle != null) {
            FadeTransition fadeSubtitle = new FadeTransition(Duration.seconds(1.5), heroSubtitle);
            fadeSubtitle.setFromValue(0);
            fadeSubtitle.setToValue(1);
            fadeSubtitle.setDelay(Duration.millis(600));
            fadeSubtitle.play();
        }

        // Animate service cards if grid exists
        if (servicesGrid != null) {
            animateServiceCards();
        }
    }

    /**
     * Animate a background orb with floating effect
     */
    private void animateOrb(Circle orb, double deltaX, double deltaY, Duration duration) {
        TranslateTransition transition = new TranslateTransition(duration, orb);
        transition.setByX(deltaX);
        transition.setByY(deltaY);
        transition.setCycleCount(Animation.INDEFINITE);
        transition.setAutoReverse(true);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    /**
     * Animate service cards with staggered fade-in
     */
    private void animateServiceCards() {
        servicesGrid.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                int index = servicesGrid.getChildren().indexOf(node);

                // Fade in animation
                FadeTransition fade = new FadeTransition(Duration.millis(800), node);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.setDelay(Duration.millis(100 * index));

                // Slide up animation
                TranslateTransition slide = new TranslateTransition(Duration.millis(800), node);
                slide.setFromY(30);
                slide.setToY(0);
                slide.setDelay(Duration.millis(100 * index));

                ParallelTransition parallel = new ParallelTransition(fade, slide);
                parallel.play();
            }
        });
    }

    // ==================== EVENT HANDLERS SETUP ====================

    /**
     * Setup all event handlers for interactive elements
     */
    private void setupEventHandlers() {
        // Navigation buttons
        if (btnHome != null) btnHome.setOnAction(e -> handleNavigation("Home"));
        if (btnServices != null) btnServices.setOnAction(e -> handleNavigation("Services"));
        if (btnMethode != null) btnMethode.setOnAction(e -> handleNavigation("Méthode"));
        if (btnBlog != null) btnBlog.setOnAction(e -> handleNavigation("Blog"));

        // Hero buttons
        if (btnGetStarted != null) btnGetStarted.setOnAction(e -> handleGetStarted());
        if (btnExplore != null) btnExplore.setOnAction(e -> handleExplore());

        // Footer links
        if (footerHome != null) footerHome.setOnMouseClicked(e -> handleNavigation("Home"));
        if (footerServices != null) footerServices.setOnMouseClicked(e -> handleNavigation("Services"));
        if (footerMethode != null) footerMethode.setOnMouseClicked(e -> handleNavigation("Méthode"));
        if (footerBlog != null) footerBlog.setOnMouseClicked(e -> handleNavigation("Blog"));
    }

    /**
     * Setup hover effects for interactive elements
     */
    private void setupHoverEffects() {
        // Add pulse effect to CTA buttons
        if (btnGetStarted != null) {
            addPulseEffect(btnGetStarted);
        }

        if (btnContact != null) {
            addPulseEffect(btnContact);
        }
    }

    /**
     * Add subtle pulse animation on hover
     */
    private void addPulseEffect(Button button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }
    @FXML
    private void handlePaiement() {
        System.out.println("Scroll vers paiement");

        if (rootScrollPane != null) {
            rootScrollPane.setVvalue(1.0);
        }
    }

    @FXML
    private void handlePaiementSubmit() {

        try {

            double montant = Double.parseDouble(montantField.getText());
            String mode = modeCombo.getValue();
            Date date = Date.valueOf(datePicker.getValue());

            /*Paiement p = new Paiement(
                    montant,
                    mode,
                    "EN_ATTENTE",
                    date,
                    1 // id user fixe pour test
            );*/

            //paiementService.ajouter(p);

            paymentStatusLabel.setText(
                    "✅ Dernier paiement : "
                            + montant + " DT | Statut : EN_ATTENTE"
            );

            montantField.clear();
            modeCombo.setValue(null);
            datePicker.setValue(null);

        } catch (Exception e) {
            paymentStatusLabel.setText("❌ Erreur lors du paiement !");
            e.printStackTrace();
        }
    }



    // ==================== IMAGE LOADING ====================

    /**
     * Load all images with error handling
     */
    private void loadImages() {
        try {
            // Hero image
            if (heroImage != null) {
                loadImageSafely(heroImage, "/images/1.jpg");
            }

            // About image
            if (aboutImage != null) {
                loadImageSafely(aboutImage, "/images/2.jpeg");
            }

            // Team images
            if (team1Image != null) {
                loadImageSafely(team1Image, "/images/team1.jpg");
            }
            if (team2Image != null) {
                loadImageSafely(team2Image, "/images/team2.jpg");
            }
            if (team3Image != null) {
                loadImageSafely(team3Image, "/images/team3.jpg");
            }
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
        }
    }

    /**
     * Safely load an image with fallback
     */
    private void loadImageSafely(ImageView imageView, String path) {
        try {
            Image image = new Image(getClass().getResourceAsStream(path));
            if (!image.isError()) {
                imageView.setImage(image);
            } else {
                System.err.println("Failed to load image: " + path);
            }
        } catch (Exception e) {
            System.err.println("Exception loading image " + path + ": " + e.getMessage());
        }
    }

    // ==================== EVENT HANDLER METHODS ====================

    /**
     * Handle navigation to different sections
     */
    private void handleNavigation(String section) {
        System.out.println("Navigating to: " + section);

        // Add smooth scroll or page transition here
        showNotification("Navigation", "Navigation vers " + section);

        // TODO: Implement actual navigation logic
        // This could involve changing scenes, scrolling to sections, etc.
    }

    /**
     * Handle Events button click (from FXML)
     */
    @FXML
    private void handleEvents(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/showEvent.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page des événements.", Alert.AlertType.ERROR);
        }
    }


    /**
     * Handle Contact button click (from FXML)
     */
    @FXML
    private void handleContact() {
        System.out.println("Contact button clicked");
        showNotification("Contact", "Formulaire de contact à venir");

        // TODO: Show contact form or navigate to contact page
        // Example: showContactDialog();
    }

    /**
     * Handle Get Started button
     */
    private void handleGetStarted() {
        System.out.println("Get Started clicked");

        // Animate button
        if (btnGetStarted != null) {
            animateButtonClick(btnGetStarted);
        }

        showNotification("Bienvenue", "Commençons votre transformation mentale !");

        // TODO: Navigate to onboarding or registration
    }

    /**
     * Handle Explore button
     */
    private void handleExplore() {
        System.out.println("Explore clicked");

        if (btnExplore != null) {
            animateButtonClick(btnExplore);
        }

        // TODO: Scroll to services section or show more info
        showNotification("Explorer", "Découvrez nos services");
    }

    /**
     * Handle Newsletter subscription (from FXML)
     */
    @FXML
    private void handleNewsletter() {
        if (newsletterEmail != null) {
            String email = newsletterEmail.getText().trim();

            if (email.isEmpty()) {
                showAlert("Erreur", "Veuillez entrer votre adresse email", Alert.AlertType.WARNING);
                return;
            }

            if (!isValidEmail(email)) {
                showAlert("Erreur", "Adresse email invalide", Alert.AlertType.WARNING);
                return;
            }

            // Animate success
            if (btnNewsletter != null) {
                animateButtonClick(btnNewsletter);
            }

            // TODO: Send email to backend
            showAlert("Succès", "Merci ! Vous êtes maintenant abonné à notre newsletter.", Alert.AlertType.INFORMATION);

            // Clear field
            newsletterEmail.clear();
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Animate button click with scale effect
     */
    private void animateButtonClick(Button button) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(scaleDown, scaleUp);
        sequence.play();
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Show notification dialog
     */
    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the dialog
        alert.getDialogPane().setStyle(
                "-fx-background-color: #1A1A24; " +
                        "-fx-text-fill: white;"
        );

        alert.showAndWait();
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the dialog
        alert.getDialogPane().setStyle(
                "-fx-background-color: #1A1A24; " +
                        "-fx-text-fill: white;"
        );

        alert.showAndWait();
    }

    // ==================== PUBLIC METHODS FOR EXTERNAL ACCESS ====================

    /**
     * Refresh the page content
     */
    public void refresh() {
        System.out.println("Refreshing front page");
        loadImages();
        initializeAnimations();
    }

    /**
     * Scroll to a specific section (to be implemented)
     */
    public void scrollToSection(String sectionId) {
        System.out.println("Scrolling to section: " + sectionId);
        // TODO: Implement smooth scrolling to section
    }

    /**
     * Update hero content dynamically
     */
    public void updateHeroContent(String title, String subtitle) {
        if (heroTitle != null) {
            heroTitle.setText(title);
        }
        if (heroSubtitle != null) {
            heroSubtitle.setText(subtitle);
        }
    }
    private void chargerStatutPaiement() {

        try {

            int idUserConnecte = 2; // ⚠️ Mets un ID existant dans ta base

            List<Paiement> paiements = paiementService.afficherParUser(idUserConnecte);

            if (paiements.isEmpty()) {
                paymentStatusLabel.setText("Aucun paiement trouvé.");
                return;
            }

            Paiement dernier = paiements.get(paiements.size() - 1);

            paymentStatusLabel.setText("Statut : " + dernier.getStatut());

        } catch (Exception e) {
            e.printStackTrace();
            paymentStatusLabel.setText("Erreur chargement paiement !");
        }
    }
}
