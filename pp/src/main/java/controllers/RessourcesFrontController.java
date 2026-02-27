package controllers;

import entities.Ressource;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceRessource;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RessourcesFrontController implements Initializable {

    // ==================== FXML INJECTED ELEMENTS ====================

    // Background orbs
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    // Navigation
    @FXML private Button btnBackToFront;

    // Search
    @FXML private TextField searchField;
    @FXML private Button btnSearch;
    @FXML private Button btnReset;

    // Type filters
    @FXML private CheckBox filterPDF;
    @FXML private CheckBox filterVideo;
    @FXML private CheckBox filterImage;
    @FXML private CheckBox filterAudio;
    @FXML private CheckBox filterArticle;

    // Results
    @FXML private Label resultsCount;
    @FXML private VBox ressourcesContainer;

    // Service
    private ServiceRessource serviceRessource;
    private List<Ressource> allRessources;
    private List<Ressource> filteredRessources;

    // ==================== INITIALIZATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("RessourcesFrontController initialized");
        serviceRessource = new ServiceRessource();

        // Initialize animations
        initializeAnimations();

        // Load resources
        loadRessources();

        // Setup event handlers
        setupEventHandlers();

        // Initial display
        displayRessources(allRessources);
    }

    // ==================== ANIMATION SETUP ====================

    private void initializeAnimations() {
        if (orb1 != null && orb2 != null && orb3 != null) {
            animateOrb(orb1, 30, -20, Duration.seconds(15));
            animateOrb(orb2, -35, 25, Duration.seconds(18));
            animateOrb(orb3, 20, -15, Duration.seconds(20));
        }
    }

    private void animateOrb(Circle orb, double deltaX, double deltaY, Duration duration) {
        TranslateTransition transition = new TranslateTransition(duration, orb);
        transition.setByX(deltaX);
        transition.setByY(deltaY);
        transition.setCycleCount(Animation.INDEFINITE);
        transition.setAutoReverse(true);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    // ==================== DATA LOADING ====================

    private void loadRessources() {
        try {
            // Charger seulement les ressources publiées
            allRessources = serviceRessource.recuperer().stream()
                    .filter(r -> "publié".equals(r.getStatut()))
                    .collect(Collectors.toList());
            filteredRessources = allRessources;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les ressources", Alert.AlertType.ERROR);
        }
    }

    // ==================== EVENT HANDLERS SETUP ====================

    private void setupEventHandlers() {
        // Search
        if (btnSearch != null) {
            btnSearch.setOnAction(e -> handleSearch());
        }
        if (searchField != null) {
            searchField.setOnAction(e -> handleSearch());
        }

        // Reset
        if (btnReset != null) {
            btnReset.setOnAction(e -> resetFilters());
        }

        // Back to front
        if (btnBackToFront != null) {
            btnBackToFront.setOnAction(e -> handleBackToFront());
        }

        // Filter change listeners
        setupFilterListeners();
    }

    private void setupFilterListeners() {
        if (filterPDF != null) {
            filterPDF.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        }
        if (filterVideo != null) {
            filterVideo.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        }
        if (filterImage != null) {
            filterImage.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        }
        if (filterAudio != null) {
            filterAudio.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        }
        if (filterArticle != null) {
            filterArticle.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        }
    }

    // ==================== SEARCH AND FILTER ====================

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void resetFilters() {
        // Clear search
        if (searchField != null) {
            searchField.clear();
        }

        // Uncheck all filters
        if (filterPDF != null) filterPDF.setSelected(false);
        if (filterVideo != null) filterVideo.setSelected(false);
        if (filterImage != null) filterImage.setSelected(false);
        if (filterAudio != null) filterAudio.setSelected(false);
        if (filterArticle != null) filterArticle.setSelected(false);

        // Show all
        filteredRessources = allRessources;
        displayRessources(filteredRessources);
    }

    private void applyFilters() {
        String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";

        filteredRessources = allRessources.stream()
                .filter(r -> matchesSearch(r, searchText))
                .filter(this::matchesTypeFilter)
                .collect(Collectors.toList());

        displayRessources(filteredRessources);
    }

    private boolean matchesSearch(Ressource r, String search) {
        if (search.isEmpty()) return true;
        return r.getTitre().toLowerCase().contains(search) ||
                (r.getDescription() != null && r.getDescription().toLowerCase().contains(search));
    }

    private boolean matchesTypeFilter(Ressource r) {
        boolean noneSelected = (filterPDF == null || !filterPDF.isSelected()) &&
                (filterVideo == null || !filterVideo.isSelected()) &&
                (filterImage == null || !filterImage.isSelected()) &&
                (filterAudio == null || !filterAudio.isSelected()) &&
                (filterArticle == null || !filterArticle.isSelected());

        if (noneSelected) {
            return true;
        }

        String type = r.getType();
        return (filterPDF != null && filterPDF.isSelected() && "PDF".equals(type)) ||
                (filterVideo != null && filterVideo.isSelected() && "Vidéo".equals(type)) ||
                (filterImage != null && filterImage.isSelected() && "Image".equals(type)) ||
                (filterAudio != null && filterAudio.isSelected() && "Audio".equals(type)) ||
                (filterArticle != null && filterArticle.isSelected() && "Article".equals(type));
    }

    // ==================== DISPLAY ====================

    private void displayRessources(List<Ressource> ressources) {
        if (ressourcesContainer == null) return;

        ressourcesContainer.getChildren().clear();

        if (ressources.isEmpty()) {
            Label emptyLabel = new Label("Aucune ressource trouvée");
            emptyLabel.setStyle("-fx-text-fill: #B8B8D1; -fx-font-size: 18px; -fx-padding: 40px;");
            ressourcesContainer.getChildren().add(emptyLabel);
        } else {
            int index = 0;
            for (Ressource r : ressources) {
                VBox card = createRessourceCard(r);
                ressourcesContainer.getChildren().add(card);
                animateCardEntry(card, index++);
            }
        }

        // Update count
        if (resultsCount != null) {
            resultsCount.setText(ressources.size() + " ressource(s) disponible(s)");
        }
    }

    private VBox createRessourceCard(Ressource r) {
        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color: rgba(37, 37, 54, 0.9); " +
                        "-fx-background-radius: 15px; " +
                        "-fx-padding: 25px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(124, 58, 237, 0.3), 10, 0, 0, 0); " +
                        "-fx-border-color: rgba(124, 58, 237, 0.2); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 15px;"
        );
        card.setMaxWidth(Double.MAX_VALUE);

        // Header with icon and type
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(getIconForType(r.getType()));
        icon.setStyle("-fx-font-size: 40px;");

        VBox titleBox = new VBox(5);
        Label title = new Label(r.getTitre());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        title.setWrapText(true);

        Label typeLabel = new Label(r.getType());
        typeLabel.setStyle(
                "-fx-background-color: rgba(124, 58, 237, 0.3); " +
                        "-fx-text-fill: #A78BFA; " +
                        "-fx-padding: 5 12; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold;"
        );

        titleBox.getChildren().addAll(title, typeLabel);
        header.getChildren().addAll(icon, titleBox);

        // Description
        Label description = new Label(
                r.getDescription() != null && !r.getDescription().isEmpty()
                        ? r.getDescription()
                        : "Aucune description disponible"
        );
        description.setStyle("-fx-text-fill: #B8B8D1; -fx-font-size: 14px;");
        description.setWrapText(true);
        description.setMaxWidth(Double.MAX_VALUE);

        // File info
        HBox fileInfo = new HBox(20);
        fileInfo.setAlignment(Pos.CENTER_LEFT);

        Label formatLabel = new Label("📄 " + r.getFormat().toUpperCase());
        formatLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 13px;");

        Label sizeLabel = new Label("💾 " + String.format("%.2f Ko", r.getTailleFichier()));
        sizeLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 13px;");

        Label dateLabel = new Label("📅 " + r.getDatePublication().toString());
        dateLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 13px;");

        fileInfo.getChildren().addAll(formatLabel, sizeLabel, dateLabel);

        // Action button
        Button btnOpen = new Button("👁️ Ouvrir la ressource");
        btnOpen.setStyle(
                "-fx-background-color: linear-gradient(to right, #7C3AED, #A78BFA); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 12 24; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
        );
        btnOpen.setMaxWidth(Double.MAX_VALUE);
        btnOpen.setOnAction(e -> openFile(r.getCheminFichier()));

        // Hover effect
        btnOpen.setOnMouseEntered(e -> {
            btnOpen.setStyle(
                    "-fx-background-color: linear-gradient(to right, #6D28D9, #8B5CF6); " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 10px; " +
                            "-fx-padding: 12 24; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-cursor: hand; " +
                            "-fx-scale-x: 1.02; " +
                            "-fx-scale-y: 1.02;"
            );
        });

        btnOpen.setOnMouseExited(e -> {
            btnOpen.setStyle(
                    "-fx-background-color: linear-gradient(to right, #7C3AED, #A78BFA); " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 10px; " +
                            "-fx-padding: 12 24; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-cursor: hand;"
            );
        });

        card.getChildren().addAll(header, description, fileInfo, btnOpen);

        return card;
    }

    private String getIconForType(String type) {
        switch (type) {
            case "PDF":
                return "📕";
            case "Vidéo":
                return "🎥";
            case "Image":
                return "🖼️";
            case "Audio":
                return "🎵";
            case "Article":
                return "📝";
            default:
                return "📄";
        }
    }

    private void animateCardEntry(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(30);

        FadeTransition fade = new FadeTransition(Duration.millis(500), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(50 * index));

        TranslateTransition slide = new TranslateTransition(Duration.millis(500), card);
        slide.setFromY(30);
        slide.setToY(0);
        slide.setDelay(Duration.millis(50 * index));

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }

    // ==================== ACTION HANDLERS ====================

    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Erreur", "Fichier introuvable : " + path, Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le fichier", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleBackToFront() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/front.fxml"));
            Stage stage = (Stage) btnBackToFront.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Front Office");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page d'accueil", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEvents() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/showEvent.fxml"));
            Stage stage = (Stage) btnBackToFront.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Événements");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEvaluations() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/showEvaluation.fxml"));
            Stage stage = (Stage) btnBackToFront.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Évaluations");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les évaluations", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
