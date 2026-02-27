package controllers;

import entities.Evenement;
import entities.Participation;
import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvenement;
import services.ServiceParticipation;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import services.googlemeetservice;

/**
 * Modern Controller for Participation Page
 * With green and sky blue palette, animations and modern effects
 */
public class ParticipationController {

    // ==================== FXML INJECTED ELEMENTS ====================

    // Animated Background Orbs
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    // Main Container
    @FXML private VBox participationContainer;

    // Search and Filters
    @FXML private TextField searchField;
    @FXML private RadioButton filterPresentiel;
    @FXML private RadioButton filterEnLigne;
    @FXML private RadioButton filterTous;

    // Stats
    @FXML private Label totalParticipations;
    @FXML private Label resultsCount;

    // Toggle Group
    @FXML private ToggleGroup modeGroup;

    // ==================== SERVICES ====================

    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private final ServiceEvenement serviceEvenement = new ServiceEvenement();
    private final googlemeetservice googleMeetService = new googlemeetservice();

    // ==================== MODERN COLORS ====================

    private static final String PRIMARY_COLOR = "#00D9FF";   // Sky Blue
    private static final String SECONDARY_COLOR = "#00FF88"; // Green
    private static final String ACCENT_COLOR = "#7FFF00";    // Lime Green
    private static final String DARK_BG = "#0A0F0A";
    private static final String CARD_BG = "#132418";

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("ParticipationController initialized");

        // Initialize toggle group
        if (modeGroup == null) {
            modeGroup = new ToggleGroup();
        }

        // Assign radio buttons to toggle group
        if (filterPresentiel != null) filterPresentiel.setToggleGroup(modeGroup);
        if (filterEnLigne != null) filterEnLigne.setToggleGroup(modeGroup);
        if (filterTous != null) {
            filterTous.setToggleGroup(modeGroup);
            filterTous.setSelected(true); // Select "Tous" by default
        }

        // Initialize animations
        initializeAnimations();

        // Load participations
        loadParticipations();

        // Setup filter listeners
        setupFilterListeners();
    }

    /**
     * Initialize background animations
     */
    private void initializeAnimations() {
        if (orb1 != null && orb2 != null && orb3 != null) {
            animateOrb(orb1, 30, -20, Duration.seconds(15));
            animateOrb(orb2, -35, 25, Duration.seconds(18));
            animateOrb(orb3, 20, -15, Duration.seconds(20));
        }
    }

    /**
     * Animate background orb
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
     * Setup filter listeners for dynamic filtering
     */
    private void setupFilterListeners() {
        if (filterPresentiel != null)
            filterPresentiel.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterEnLigne != null)
            filterEnLigne.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterTous != null)
            filterTous.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (searchField != null)
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    // ==================== NAVIGATION ====================

    /**
     * Navigate back to front page
     */
    @FXML
    private void handleBackToFront() {
        navigateTo("/views/dashboard.fxml", "NeuroWell - Accueil");
    }

    /**
     * Navigate to events page
     */
    @FXML
    private VBox mainAnchorPane;

    @FXML
    private void handleEvenement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/showEvent.fxml"));
            Parent newRoot = loader.load();
            mainAnchorPane.getChildren().setAll(newRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Navigate to a specific page
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) participationContainer.getScene().getWindow();

            // Fade transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), participationContainer.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.setTitle(title);
                stage.getScene().setRoot(root);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();

        } catch (IOException e) {
            e.printStackTrace();
            showModernAlert("Erreur", "Impossible de charger la page", Alert.AlertType.ERROR);
        }
    }

    // ==================== LOAD PARTICIPATIONS ====================

    /**
     * Load all participations with animation
     */
    private void loadParticipations() {
        participationContainer.getChildren().clear();

        try {
            List<Participation> participations = serviceParticipation.recuperer();

            if (participations.isEmpty()) {
                showEmptyState("Aucune participation disponible pour le moment.");
                updateStats(0, 0);
                return;
            }

            // Update total stats
            if (totalParticipations != null) {
                totalParticipations.setText(String.valueOf(participations.size()));
            }

            if (resultsCount != null) {
                resultsCount.setText(participations.size() + " participation" + (participations.size() > 1 ? "s" : ""));
            }

            // Create cards with staggered animation
            for (int i = 0; i < participations.size(); i++) {
                Participation p = participations.get(i);
                VBox card = createModernParticipationCard(p);
                participationContainer.getChildren().add(card);
                animateCardAppearance(card, i);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showModernAlert("Erreur", "Impossible de charger les participations", Alert.AlertType.ERROR);
        }
    }

    /**
     * Create modern participation card
     */
    private VBox createModernParticipationCard(Participation p) throws SQLException {
        // Get associated event
        Evenement e = serviceEvenement.recuperer().stream()
                .filter(ev -> ev.getId_e() == p.getIdEvenement())
                .findFirst()
                .orElse(null);

        String titre = e != null ? e.getTitre_e() : "Événement supprimé";
        String type = e != null ? e.getType_e() : "-";
        String prix = e != null ? String.valueOf(e.getPrix_e()) : "-";
        String localisation = e != null ? e.getLocalisation_e() : "-";

        // Create card
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(19, 36, 24, 0.6), rgba(19, 36, 24, 0.3)); " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-color: rgba(0, 217, 255, 0.1); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 20; " +
                        "-fx-padding: 5;"
        );

        // Add shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(PRIMARY_COLOR, 0.2));
        shadow.setRadius(15);
        shadow.setSpread(0.2);
        card.setEffect(shadow);

        // Hover effect
        card.setOnMouseEntered(ev -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, rgba(19, 36, 24, 0.8), rgba(19, 36, 24, 0.5)); " +
                            "-fx-background-radius: 20; " +
                            "-fx-border-color: " + PRIMARY_COLOR + "; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 20; " +
                            "-fx-padding: 5;"
            );

            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(-8);
            tt.play();

            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setColor(Color.web(PRIMARY_COLOR, 0.4));
            hoverShadow.setRadius(25);
            hoverShadow.setSpread(0.3);
            card.setEffect(hoverShadow);
        });

        card.setOnMouseExited(ev -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, rgba(19, 36, 24, 0.6), rgba(19, 36, 24, 0.3)); " +
                            "-fx-background-radius: 20; " +
                            "-fx-border-color: rgba(0, 217, 255, 0.1); " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 20; " +
                            "-fx-padding: 5;"
            );

            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(0);
            tt.play();

            DropShadow normalShadow = new DropShadow();
            normalShadow.setColor(Color.web(PRIMARY_COLOR, 0.2));
            normalShadow.setRadius(15);
            normalShadow.setSpread(0.2);
            card.setEffect(normalShadow);
        });

        // Main content
        HBox mainContent = new HBox(15);
        mainContent.setAlignment(Pos.CENTER_LEFT);
        mainContent.setPadding(new Insets(20));

        // Left: Event info
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Title
        Label lblTitre = new Label("🎫 " + titre);
        lblTitre.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(450);

        // Info badges
        HBox badgesBox = new HBox(10);
        badgesBox.getChildren().addAll(
                createInfoBadge("🎯", type),
                createInfoBadge("📍", localisation),
                createInfoBadge("💰", prix + " DT")
        );

        // Mode badge
        Label modeLabel = createModeBadge(p.getModeparticipation());

        // Objectif
        Label lblObjectif = new Label("Objectif: " + p.getObjectif());
        lblObjectif.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-text-fill: rgba(248, 249, 250, 0.7); " +
                        "-fx-line-spacing: 2px;"
        );
        lblObjectif.setWrapText(true);
        lblObjectif.setMaxWidth(450);

        infoBox.getChildren().addAll(lblTitre, badgesBox, modeLabel, lblObjectif);

        // Right: Action buttons
        VBox actionBox = new VBox(12);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(10));

        Button btnEdit = createActionButton("✎ Modifier", PRIMARY_COLOR);
        btnEdit.setOnAction(ev -> openModernEditDialog(p));

        Button btnDelete = createActionButton("✖ Supprimer", "#FF6B6B");
        btnDelete.setOnAction(ev -> handleDelete(p));

        actionBox.getChildren().addAll(btnEdit, btnDelete);

        // ── Google Meet API button (uniquement pour les participants en ligne) ──
        if (p.getModeparticipation().equalsIgnoreCase("distanciel")) {
            // On récupère le titre et la date de l'événement associé
            final String titreFinal = titre;
            final java.sql.Timestamp dateFinal = e != null ? e.getDate_e() : null;

            Button btnMeet = createMeetButton();
            btnMeet.setOnAction(ev -> creerEtOuvrirMeet(titreFinal, dateFinal));
            actionBox.getChildren().add(btnMeet);
        }
        // ─────────────────────────────────────────────────────────────────────

        mainContent.getChildren().addAll(infoBox, actionBox);
        card.getChildren().add(mainContent);

        return card;
    }

    /**
     * Create info badge
     */
    private Label createInfoBadge(String emoji, String text) {
        Label badge = new Label(emoji + " " + text);
        badge.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: rgba(248, 249, 250, 0.7); " +
                        "-fx-background-color: rgba(0, 217, 255, 0.1); " +
                        "-fx-padding: 6 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: rgba(0, 217, 255, 0.3); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 15;"
        );
        return badge;
    }

    /**
     * Create mode badge (Présentiel/En ligne)
     */
    private Label createModeBadge(String mode) {
        String emoji = mode.equalsIgnoreCase("presentiel") ? "🏢" : "💻";
        String text = mode.equalsIgnoreCase("presentiel") ? "Présentiel" : "En ligne";
        String color = mode.equalsIgnoreCase("presentiel") ? SECONDARY_COLOR : PRIMARY_COLOR;

        Label badge = new Label(emoji + " " + text);
        badge.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-background-color: rgba(0, 255, 136, 0.15); " +
                        "-fx-padding: 8 16; " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 20;"
        );
        return badge;
    }

    /**
     * Create action button
     */
    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 25; " +
                        "-fx-background-radius: 15; " +
                        "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return btn;
    }

    /**
     * Animate card appearance
     */
    private void animateCardAppearance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(30);

        FadeTransition fade = new FadeTransition(Duration.millis(500), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(index * 80));

        TranslateTransition slide = new TranslateTransition(Duration.millis(500), card);
        slide.setFromY(30);
        slide.setToY(0);
        slide.setDelay(Duration.millis(index * 80));

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }

    /**
     * Show empty state
     */
    private void showEmptyState(String message) {
        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(80));

        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 72px;");

        Label msg = new Label(message);
        msg.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-text-fill: rgba(248, 249, 250, 0.6); " +
                        "-fx-font-weight: 600;"
        );

        emptyState.getChildren().addAll(icon, msg);
        participationContainer.getChildren().add(emptyState);
    }

    /**
     * Update statistics
     */
    private void updateStats(int total, int filtered) {
        if (totalParticipations != null) {
            totalParticipations.setText(String.valueOf(total));
        }
        if (resultsCount != null) {
            resultsCount.setText(filtered + " participation" + (filtered > 1 ? "s" : ""));
        }
    }

    // ==================== FILTERING ====================

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (filterTous != null) filterTous.setSelected(true);
        loadParticipations();
    }

    /**
     * Apply filters to participations
     */
    private void applyFilters() {
        participationContainer.getChildren().clear();

        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
        String selectedMode = "";

        if (filterPresentiel != null && filterPresentiel.isSelected()) {
            selectedMode = "presentiel";
        } else if (filterEnLigne != null && filterEnLigne.isSelected()) {
            selectedMode = "distanciel";
        }

        try {
            List<Participation> participations = serviceParticipation.recuperer();
            int displayedCount = 0;

            for (Participation p : participations) {
                Evenement e = serviceEvenement.recuperer().stream()
                        .filter(ev -> ev.getId_e() == p.getIdEvenement())
                        .findFirst()
                        .orElse(null);

                String titre = e != null ? e.getTitre_e() : "";
                String type = e != null ? e.getType_e() : "";
                String prix = e != null ? String.valueOf(e.getPrix_e()) : "";

                boolean matchesSearch = searchText.isEmpty() ||
                        p.getObjectif().toLowerCase().contains(searchText) ||
                        titre.toLowerCase().contains(searchText) ||
                        type.toLowerCase().contains(searchText) ||
                        prix.toLowerCase().contains(searchText);

                boolean matchesMode = selectedMode.isEmpty() ||
                        p.getModeparticipation().equalsIgnoreCase(selectedMode);

                if (matchesSearch && matchesMode) {
                    VBox card = createModernParticipationCard(p);
                    participationContainer.getChildren().add(card);
                    animateCardAppearance(card, displayedCount);
                    displayedCount++;
                }
            }

            // Update stats
            updateStats(participations.size(), displayedCount);

            if (displayedCount == 0) {
                showEmptyState("Aucune participation ne correspond à vos critères.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showModernAlert("Erreur", "Impossible de filtrer les participations", Alert.AlertType.ERROR);
        }
    }

    // ==================== ACTIONS ====================

    /**
     * Handle delete participation
     */
    private void handleDelete(Participation p) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la participation");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette participation ?");

        styleDialog(confirmation.getDialogPane());

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceParticipation.supprimer(p);
                    loadParticipations();
                    showModernAlert("Succès", "Participation supprimée avec succès", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showModernAlert("Erreur", "Impossible de supprimer la participation", Alert.AlertType.ERROR);
                }
            }
        });
    }

    /**
     * Open modern edit dialog
     */
    private void openModernEditDialog(Participation p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la participation");

        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);

        ButtonType btnSave = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnSave, btnCancel);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));

        // Title
        Label lblTitle = new Label("✎ Modifier la participation");
        lblTitle.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );

        // Objectif field
        Label lblObjectif = new Label("Objectif de participation");
        lblObjectif.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: white;");

        TextArea objectifField = new TextArea(p.getObjectif());
        objectifField.setWrapText(true);
        objectifField.setPrefRowCount(4);
        objectifField.setPromptText("Décrivez votre objectif (minimum 10 caractères)");
        objectifField.setStyle(
                "-fx-control-inner-background: rgba(19, 36, 24, 0.6); " +
                        "-fx-text-fill: white; " +
                        "-fx-prompt-text-fill: rgba(160, 224, 192, 0.4); " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: rgba(0, 217, 255, 0.3); " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10;"
        );

        // Mode selection
        Label lblMode = new Label("Mode de participation");
        lblMode.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: white;");

        RadioButton radioDistanciel = new RadioButton("💻 En ligne");
        RadioButton radioPresentiel = new RadioButton("🏢 Présentiel");

        radioDistanciel.setStyle("-fx-text-fill: rgba(248, 249, 250, 0.8); -fx-font-size: 13px;");
        radioPresentiel.setStyle("-fx-text-fill: rgba(248, 249, 250, 0.8); -fx-font-size: 13px;");

        ToggleGroup modeToggle = new ToggleGroup();
        radioDistanciel.setToggleGroup(modeToggle);
        radioPresentiel.setToggleGroup(modeToggle);

        if (p.getModeparticipation().equalsIgnoreCase("presentiel")) {
            radioPresentiel.setSelected(true);
        } else {
            radioDistanciel.setSelected(true);
        }

        HBox modeBox = new HBox(30, radioDistanciel, radioPresentiel);
        modeBox.setPadding(new Insets(10, 0, 0, 0));

        form.getChildren().addAll(lblTitle, lblObjectif, objectifField, lblMode, modeBox);
        pane.setContent(form);

        // Style buttons
        dialog.getDialogPane().lookupButton(btnSave).setStyle(
                "-fx-background-color: linear-gradient(to right, " + PRIMARY_COLOR + ", " + SECONDARY_COLOR + "); " +
                        "-fx-text-fill: " + DARK_BG + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 25; " +
                        "-fx-background-radius: 20; " +
                        "-fx-cursor: hand;"
        );

        dialog.getDialogPane().lookupButton(btnCancel).setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: " + PRIMARY_COLOR + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 20; " +
                        "-fx-background-radius: 20; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 25; " +
                        "-fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response == btnSave) {
                String objectif = objectifField.getText().trim();
                String mode = radioDistanciel.isSelected() ? "distanciel" : "presentiel";

                if (objectif.length() < 10) {
                    showModernAlert("Erreur", "L'objectif doit contenir au moins 10 caractères", Alert.AlertType.WARNING);
                    return;
                }

                p.setObjectif(objectif);
                p.setModeparticipation(mode);

                try {
                    serviceParticipation.modifier(p);
                    loadParticipations();
                    showModernAlert("Succès", "Participation modifiée avec succès", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showModernAlert("Erreur", "Impossible de modifier la participation", Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ==================== GOOGLE MEET API ====================

    /**
     * Crée un bouton "📹 Rejoindre Meet" stylisé en bleu Google
     */
    private Button createMeetButton() {
        Button btn = new Button("📹 Rejoindre Meet");
        btn.setStyle(
                "-fx-background-color: #1a73e8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 10 18; " +
                        "-fx-background-radius: 15; " +
                        "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                    "-fx-background-color: #1557b0; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 13px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 10 18; " +
                            "-fx-background-radius: 15; " +
                            "-fx-cursor: hand;"
            );
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(
                    "-fx-background-color: #1a73e8; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 13px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 10 18; " +
                            "-fx-background-radius: 15; " +
                            "-fx-cursor: hand;"
            );
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return btn;
    }

    /**
     * Appelle la vraie Google Meet REST API pour créer une réunion,
     * puis ouvre le lien Meet généré dans le navigateur par défaut.
     *
     * @param titreEvenement  Titre de l'événement (sera le nom de la réunion Meet)
     * @param dateEvenement   Date/heure de début (utilisée pour planifier la réunion)
     */
    private void creerEtOuvrirMeet(String titreEvenement, java.sql.Timestamp dateEvenement) {
        // Lancer dans un thread séparé pour ne pas bloquer l'UI JavaFX
        // (l'auth OAuth peut prendre quelques secondes la première fois)
        new Thread(() -> {
            try {
                // ── Appel réel à la Google Calendar/Meet API ──────────────────
                String meetUrl = googleMeetService.creerReunionMeet(titreEvenement, dateEvenement);
                // ─────────────────────────────────────────────────────────────

                // Retour sur le thread JavaFX pour ouvrir le navigateur
                javafx.application.Platform.runLater(() -> {
                    try {
                        URI uri = new URI(meetUrl);
                        if (Desktop.isDesktopSupported()
                                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(uri);
                        } else {
                            String os = System.getProperty("os.name").toLowerCase();
                            if (os.contains("linux")) {
                                new ProcessBuilder("xdg-open", meetUrl).start();
                            } else if (os.contains("mac")) {
                                new ProcessBuilder("open", meetUrl).start();
                            }
                        }
                        showModernAlert("✅ Réunion créée",
                                "Lien Meet généré :\n" + meetUrl,
                                Alert.AlertType.INFORMATION);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showModernAlert("Erreur", "Impossible d'ouvrir le lien Meet : " + ex.getMessage(),
                                Alert.AlertType.ERROR);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        showModernAlert("Erreur API Meet",
                                "Impossible de créer la réunion Google Meet :\n" + ex.getMessage(),
                                Alert.AlertType.ERROR));
            }
        }, "GoogleMeet-Thread").start();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Style dialog pane with modern design
     */
    private void styleDialog(DialogPane pane) {
        pane.setStyle(
                "-fx-background-color: " + CARD_BG + "; " +
                        "-fx-border-color: " + PRIMARY_COLOR + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 20; " +
                        "-fx-background-radius: 20;"
        );

        pane.lookup(".content.label").setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 14px;"
        );
    }

    /**
     * Show modern alert
     */
    private void showModernAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        styleDialog(alert.getDialogPane());

        alert.showAndWait();
    }
}
