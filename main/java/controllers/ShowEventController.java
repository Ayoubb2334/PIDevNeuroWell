package controllers;

import entities.Evenement;
import entities.Participation;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvenement;
import services.ServiceParticipation;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modern Controller for Event Display Page
 * Updated with futuristic design and animations
 */
public class ShowEventController {

    // ==================== FXML INJECTED ELEMENTS ====================

    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;
    @FXML private VBox eventsContainer;

    // Filtres Type
    @FXML private CheckBox filterConference;
    @FXML private CheckBox filterFormation;
    @FXML private CheckBox filterWorkshop;
    @FXML private CheckBox filterAutre;

    // Filtres Prix
    @FXML private CheckBox filterPrixMoins40;
    @FXML private CheckBox filterPrixPlus40;
    @FXML private CheckBox filterGratuit;

    // Navigation Buttons
    @FXML private Button btnBackToFront;
    @FXML private Button btnParticipation;
    @FXML private Button btnSearch;
    @FXML private Button btnReset;

    // Results counter
    @FXML private Label resultsCount;

    // ==================== SERVICES ====================

    private ServiceEvenement serviceEvenement;
    private ServiceParticipation serviceParticipation;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== MODERN COLORS ====================

    private static final String PRIMARY_COLOR = "#00D9FF";   // Sky Blue / Bleu Ciel
    private static final String SECONDARY_COLOR = "#00FF88"; // Green / Vert
    private static final String ACCENT_COLOR = "#7FFF00";    // Lime Green / Vert Citron
    private static final String DARK_BG = "#0A0F0A";         // Dark Green-tinted
    private static final String CARD_BG = "#132418";         // Dark Card Background

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        serviceEvenement = new ServiceEvenement();
        serviceParticipation = new ServiceParticipation();

        // Apply modern styling
        applyModernStyling();

        // Load events with animation
        loadEvents();

        // Add listeners on filters with debounce
        setupFilterListeners();
    }

    /**
     * Apply modern styling to components
     */
    private void applyModernStyling() {
        // Style search field
        if (searchField != null) {
            searchField.setStyle(
                    "-fx-background-color: rgba(26, 26, 36, 0.6); " +
                            "-fx-text-fill: white; " +
                            "-fx-prompt-text-fill: rgba(248, 249, 250, 0.4); " +
                            "-fx-font-size: 14px; " +
                            "-fx-padding: 12; " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 15;"
            );

            searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    searchField.setStyle(
                            "-fx-background-color: rgba(26, 26, 36, 0.8); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-prompt-text-fill: rgba(248, 249, 250, 0.4); " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-padding: 12; " +
                                    "-fx-background-radius: 15; " +
                                    "-fx-border-color: " + PRIMARY_COLOR + "; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 15;"
                    );
                } else {
                    searchField.setStyle(
                            "-fx-background-color: rgba(26, 26, 36, 0.6); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-prompt-text-fill: rgba(248, 249, 250, 0.4); " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-padding: 12; " +
                                    "-fx-background-radius: 15; " +
                                    "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 15;"
                    );
                }
            });
        }

        // Style date picker
        if (dateFilter != null) {
            dateFilter.setStyle(
                    "-fx-background-color: rgba(26, 26, 36, 0.6); " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 15;"
            );
        }

        // Style checkboxes with modern look
        styleCheckBox(filterConference);
        styleCheckBox(filterFormation);
        styleCheckBox(filterWorkshop);
        styleCheckBox(filterAutre);
        styleCheckBox(filterPrixMoins40);
        styleCheckBox(filterPrixPlus40);
        styleCheckBox(filterGratuit);
    }

    /**
     * Style individual checkbox with modern design
     */
    private void styleCheckBox(CheckBox checkBox) {
        if (checkBox != null) {
            checkBox.setStyle(
                    "-fx-text-fill: rgba(248, 249, 250, 0.8); " +
                            "-fx-font-size: 13px; " +
                            "-fx-font-weight: 600;"
            );
        }
    }

    /**
     * Setup filter listeners with debounce effect
     */
    private void setupFilterListeners() {
        if (filterConference != null)
            filterConference.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterFormation != null)
            filterFormation.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterWorkshop != null)
            filterWorkshop.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterAutre != null)
            filterAutre.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        if (filterPrixMoins40 != null)
            filterPrixMoins40.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterPrixPlus40 != null)
            filterPrixPlus40.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        if (filterGratuit != null)
            filterGratuit.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    // ==================== NAVIGATION ====================

    /**
     * Return to front page with transition
     */
    @FXML
    private void handleBackToFront() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Front.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) eventsContainer.getScene().getWindow();

            // Fade transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), eventsContainer.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.setTitle("PSYCHÉ - Accueil");
                Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
                stage.setScene(scene);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();

        } catch (IOException e) {
            e.printStackTrace();
            showModernAlert("Erreur", "Impossible de retourner à l'accueil", Alert.AlertType.ERROR);
        }
    }

    /**
     * Open participation page
     */
    @FXML
    private void handleParticipation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/participation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) eventsContainer.getScene().getWindow();
            stage.setTitle("PSYCHÉ - Participation");
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showModernAlert("Erreur", "Impossible d'ouvrir la page de participation", Alert.AlertType.ERROR);
        }
    }

    // ==================== EVENT LOADING ====================

    /**
     * Load events with animation
     */
    private void loadEvents() {
        eventsContainer.getChildren().clear();

        try {
            List<Evenement> events = serviceEvenement.recuperer();

            // Filtrer uniquement les événements avec statut "Valide"
            List<Evenement> validEvents = events.stream()
                    .filter(ev -> "Validé".equalsIgnoreCase(ev.getStatut_e()))
                    .toList();

            if (validEvents.isEmpty()) {
                Label noEvents = new Label("Aucun événement valide disponible pour le moment.");
                noEvents.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-text-fill: rgba(248, 249, 250, 0.6); " +
                                "-fx-padding: 50; " +
                                "-fx-font-weight: 600;"
                );
                eventsContainer.getChildren().add(noEvents);

                if (resultsCount != null) {
                    resultsCount.setText("0 événement");
                }
                return;
            }

            // Update results counter
            if (resultsCount != null) {
                resultsCount.setText(validEvents.size() + " événement" + (validEvents.size() > 1 ? "s" : ""));
            }

            // Add events with staggered animation
            for (int i = 0; i < validEvents.size(); i++) {
                Evenement event = validEvents.get(i);
                VBox eventCard = createModernEventCard(event);
                eventsContainer.getChildren().add(eventCard);

                // Animate card appearance
                animateCardAppearance(eventCard, i);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showModernAlert("Erreur", "Impossible de charger les événements", Alert.AlertType.ERROR);
        }
    }


    /**
     * Animate card appearance with fade and slide
     */
    private void animateCardAppearance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(30);

        FadeTransition fade = new FadeTransition(Duration.millis(500), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(index * 100));

        TranslateTransition slide = new TranslateTransition(Duration.millis(500), card);
        slide.setFromY(30);
        slide.setToY(0);
        slide.setDelay(Duration.millis(index * 100));

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();
    }

    /**
     * Create modern event card with gradient and effects
     */
    private VBox createModernEventCard(Evenement event) {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(26, 26, 36, 0.6), rgba(26, 26, 36, 0.3)); " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-color: rgba(255, 255, 255, 0.05); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 20; " +
                        "-fx-padding: 5;"
        );

        // Add shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(PRIMARY_COLOR, 0.2));
        shadow.setRadius(15);
        shadow.setSpread(0.2);
        card.setEffect(shadow);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, rgba(26, 26, 36, 0.8), rgba(26, 26, 36, 0.5)); " +
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

        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, rgba(26, 26, 36, 0.6), rgba(26, 26, 36, 0.3)); " +
                            "-fx-background-radius: 20; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.05); " +
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

        HBox mainContent = new HBox(15);
        mainContent.setAlignment(Pos.CENTER_LEFT);
        mainContent.setPadding(new Insets(15));

        // Image with modern styling
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(false);
        imageView.setStyle("-fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");

        if (event.getImage() != null && !event.getImage().isEmpty()) {
            String[] paths = {
                    "src/main/resources/" + event.getImage(),
                    System.getProperty("user.dir") + "/src/main/resources/" + event.getImage(),
                    "target/classes/" + event.getImage()
            };
            for (String path : paths) {
                File imgFile = new File(path);
                if (imgFile.exists()) {
                    imageView.setImage(new Image(imgFile.toURI().toString()));
                    break;
                }
            }
        }

        // Content section
        VBox contentBox = new VBox(8);
        contentBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label titleLabel = new Label(event.getTitre_e());
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(450);
        titleLabel.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );

        Label descLabel = new Label(event.getDescription_e());
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(450);
        descLabel.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-text-fill: rgba(248, 249, 250, 0.6); " +
                        "-fx-line-spacing: 2px;"
        );

        // Info badges
        HBox infoBox = new HBox(12);
        infoBox.getChildren().addAll(
                createModernInfoBadge("📅", event.getDate_e().toLocalDateTime().format(dateFormatter)),
                createModernInfoBadge("📍", event.getLocalisation_e()),
                createModernInfoBadge("🎯", event.getType_e()),
                createModernInfoBadge("👥", event.getCapacitemax_e() + " places")
        );

        contentBox.getChildren().addAll(titleLabel, descLabel, infoBox);

        // Right section: Price and button
        VBox rightBox = new VBox(12);
        rightBox.setAlignment(Pos.CENTER);
        rightBox.setPadding(new Insets(10));

        Label prixLabel = new Label(event.getPrix_e());
        prixLabel.setStyle(
                "-fx-font-size: 22px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: linear-gradient(to right, " + PRIMARY_COLOR + ", " + SECONDARY_COLOR + ");"
        );

        Button btnParticiper = new Button("Participer");
        btnParticiper.setStyle(
                "-fx-background-color: linear-gradient(to right, " + PRIMARY_COLOR + ", " + SECONDARY_COLOR + "); " +
                        "-fx-text-fill: " + DARK_BG + "; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12 25; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand;"
        );

        // Button hover effect
        btnParticiper.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnParticiper);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        btnParticiper.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btnParticiper);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        btnParticiper.setOnAction(e -> openModernParticipationForm(event));

        rightBox.getChildren().addAll(prixLabel, btnParticiper);

        mainContent.getChildren().addAll(imageView, contentBox, rightBox);
        card.getChildren().add(mainContent);

        return card;
    }

    /**
     * Create modern info badge
     */
    private Label createModernInfoBadge(String emoji, String text) {
        Label label = new Label(emoji + " " + text);
        label.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: rgba(248, 249, 250, 0.7); " +
                        "-fx-font-weight: 600; " +
                        "-fx-background-color: rgba(0, 240, 255, 0.1); " +
                        "-fx-padding: 6 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: rgba(0, 240, 255, 0.3); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 15;"
        );
        label.setMaxWidth(160);
        return label;
    }

    // ==================== PARTICIPATION FORM ====================

    /**
     * Open modern participation form dialog
     */
    private void openModernParticipationForm(Evenement event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Participation à l'événement");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
                "-fx-background-color: " + CARD_BG + "; " +
                        "-fx-border-color: " + PRIMARY_COLOR + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-background-radius: 15;"
        );

        ButtonType btnConfirm = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnConfirm, btnCancel);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 15;");

        Label lblTitle = new Label("🎫 " + event.getTitre_e());
        lblTitle.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );

        Label lblSubtitle = new Label("Réservez votre place maintenant");
        lblSubtitle.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: rgba(248, 249, 250, 0.6);"
        );

        TextArea objectifField = new TextArea();
        objectifField.setPromptText("Pourquoi souhaitez-vous participer à cet événement ?");
        objectifField.setPrefRowCount(4);
        objectifField.setWrapText(true);
        objectifField.setStyle(
                "-fx-control-inner-background: rgba(26, 26, 36, 0.6); " +
                        "-fx-text-fill: white; " +
                        "-fx-prompt-text-fill: rgba(248, 249, 250, 0.4); " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10;"
        );

        Label lblMode = new Label("Mode de participation");
        lblMode.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: white;"
        );

        ToggleGroup modeGroup = new ToggleGroup();

        RadioButton rbEnLigne = new RadioButton("💻 En ligne");
        rbEnLigne.setToggleGroup(modeGroup);
        rbEnLigne.setSelected(true);
        rbEnLigne.setStyle("-fx-text-fill: rgba(248, 249, 250, 0.8); -fx-font-size: 13px;");

        RadioButton rbPresentiel = new RadioButton("🏢 Présentiel");
        rbPresentiel.setToggleGroup(modeGroup);
        rbPresentiel.setStyle("-fx-text-fill: rgba(248, 249, 250, 0.8); -fx-font-size: 13px;");

        HBox modeBox = new HBox(40, rbEnLigne, rbPresentiel);
        modeBox.setPadding(new Insets(10, 0, 0, 0));

        form.getChildren().addAll(lblTitle, lblSubtitle, objectifField, lblMode, modeBox);
        pane.setContent(form);

        // Style buttons
        dialog.getDialogPane().lookupButton(btnConfirm).setStyle(
                "-fx-background-color: linear-gradient(to right, " + PRIMARY_COLOR + ", " + SECONDARY_COLOR + "); " +
                        "-fx-text-fill: " + DARK_BG + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20; " +
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
                        "-fx-padding: 10 20; " +
                        "-fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(res -> {
            if (res == btnConfirm) {
                String objectif = objectifField.getText().trim();
                String mode = rbEnLigne.isSelected() ? "distanciel" : "presentiel";

                if (objectif.isEmpty() || objectif.length() < 10) {
                    showModernAlert("Erreur", "L'objectif doit contenir au moins 10 caractères", Alert.AlertType.ERROR);
                    return;
                }

                try {
                    int idUtilisateur = 1; // TODO: Replace with actual logged-in user ID
                    Participation p = new Participation(event.getId_e(), idUtilisateur, mode, objectif);
                    serviceParticipation.ajouter(p);

                    showModernAlert("Succès", "Votre participation a été enregistrée avec succès !", Alert.AlertType.INFORMATION);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showModernAlert("Erreur", "Une erreur est survenue lors de l'enregistrement", Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ==================== SEARCH AND FILTERS ====================

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (dateFilter != null) dateFilter.setValue(null);
        if (filterConference != null) filterConference.setSelected(false);
        if (filterFormation != null) filterFormation.setSelected(false);
        if (filterWorkshop != null) filterWorkshop.setSelected(false);
        if (filterAutre != null) filterAutre.setSelected(false);
        if (filterPrixMoins40 != null) filterPrixMoins40.setSelected(false);
        if (filterPrixPlus40 != null) filterPrixPlus40.setSelected(false);
        if (filterGratuit != null) filterGratuit.setSelected(false);

        loadEvents();
    }

    /**
     * Apply all active filters
     */
    private void applyFilters() {
        eventsContainer.getChildren().clear();

        String searchText = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        LocalDate selectedDate = dateFilter != null ? dateFilter.getValue() : null;

        try {
            List<Evenement> events = serviceEvenement.recuperer();
            int displayedCount = 0;

            for (Evenement e : events) {
                boolean matchesSearch = searchText.isEmpty() ||
                        e.getTitre_e().toLowerCase().contains(searchText) ||
                        e.getLocalisation_e().toLowerCase().contains(searchText) ||
                        e.getDescription_e().toLowerCase().contains(searchText);

                boolean matchesDate = selectedDate == null ||
                        e.getDate_e().toLocalDateTime().toLocalDate().equals(selectedDate);

                boolean matchesType = (!filterConference.isSelected() && !filterFormation.isSelected() &&
                        !filterWorkshop.isSelected() && !filterAutre.isSelected()) ||
                        (filterConference.isSelected() && e.getType_e().equalsIgnoreCase("Conférence")) ||
                        (filterFormation.isSelected() && e.getType_e().equalsIgnoreCase("Formation")) ||
                        (filterWorkshop.isSelected() && e.getType_e().equalsIgnoreCase("Workshop")) ||
                        (filterAutre.isSelected() && !e.getType_e().equalsIgnoreCase("Conférence") &&
                                !e.getType_e().equalsIgnoreCase("Formation") && !e.getType_e().equalsIgnoreCase("Workshop"));

                double prix = extractPrix(e.getPrix_e());
                boolean matchesPrix = (!filterPrixMoins40.isSelected() && !filterPrixPlus40.isSelected() && !filterGratuit.isSelected()) ||
                        (filterGratuit.isSelected() && prix == 0) ||
                        (filterPrixMoins40.isSelected() && prix > 0 && prix < 40) ||
                        (filterPrixPlus40.isSelected() && prix >= 40);

                if (matchesSearch && matchesDate && matchesType && matchesPrix) {
                    VBox eventCard = createModernEventCard(e);
                    eventsContainer.getChildren().add(eventCard);
                    animateCardAppearance(eventCard, displayedCount);
                    displayedCount++;
                }
            }

            // Update results counter
            if (resultsCount != null) {
                resultsCount.setText(displayedCount + " événement" + (displayedCount > 1 ? "s" : ""));
            }

            if (eventsContainer.getChildren().isEmpty()) {
                VBox noResults = new VBox(20);
                noResults.setAlignment(Pos.CENTER);
                noResults.setPadding(new Insets(60));

                Label icon = new Label("🔍");
                icon.setStyle("-fx-font-size: 64px;");

                Label message = new Label("Aucun événement trouvé");
                message.setStyle(
                        "-fx-font-size: 20px; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold;"
                );

                Label subtitle = new Label("Essayez de modifier vos critères de recherche");
                subtitle.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-text-fill: rgba(248, 249, 250, 0.6);"
                );

                noResults.getChildren().addAll(icon, message, subtitle);
                eventsContainer.getChildren().add(noResults);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showModernAlert("Erreur", "Erreur lors du filtrage des événements", Alert.AlertType.ERROR);
        }
    }

    /**
     * Extract price from string
     */
    private double extractPrix(String prixStr) {
        try {
            return Double.parseDouble(prixStr.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Show modern styled alert
     */
    private void showModernAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: " + CARD_BG + "; " +
                        "-fx-border-color: " + PRIMARY_COLOR + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-background-radius: 15;"
        );

        dialogPane.lookup(".content.label").setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 14px;"
        );

        alert.showAndWait();
    }
}
