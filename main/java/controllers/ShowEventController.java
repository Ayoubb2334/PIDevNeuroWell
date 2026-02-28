package controllers;

import entities.Evenement;
import entities.Participation;
import entities.UserUnified;
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
import services.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ShowEventController {

    // ==================== FXML ====================

    @FXML private TextField  searchField;
    @FXML private DatePicker dateFilter;
    @FXML private VBox       eventsContainer;

    @FXML private CheckBox filterConference;
    @FXML private CheckBox filterFormation;
    @FXML private CheckBox filterWorkshop;
    @FXML private CheckBox filterAutre;
    @FXML private CheckBox filterPrixMoins40;
    @FXML private CheckBox filterPrixPlus40;
    @FXML private CheckBox filterGratuit;

    // Ces champs sont injectés par FXML mais utilisés uniquement via CSS/FXML
    @FXML private Button btnBackToFront;
    @FXML private Button btnParticipation;
    @FXML private Button btnSearch;
    @FXML private Button btnReset;

    @FXML private Label resultsCount;

    // ==================== SERVICES ====================

    private ServiceEvenement     serviceEvenement;
    private ServiceParticipation serviceParticipation;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== COULEURS ====================

    private static final String PRIMARY_COLOR = "#00D9FF";
    private static final String DARK_BG       = "#0A0F0A";
    private static final String CARD_BG       = "#132418";

    // ==================== INIT ====================

    @FXML
    public void initialize() {
        serviceEvenement     = new ServiceEvenement();
        serviceParticipation = new ServiceParticipation();
        applyModernStyling();
        loadEvents();
        setupFilterListeners();
    }

    // ==================== STYLE ====================

    private void applyModernStyling() {
        if (searchField != null) {
            String baseStyle =
                    "-fx-background-color: rgba(26,26,36,0.6); -fx-text-fill: white;" +
                            "-fx-prompt-text-fill: rgba(248,249,250,0.4); -fx-font-size: 14px;" +
                            "-fx-padding: 12; -fx-background-radius: 15;" +
                            "-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 2; -fx-border-radius: 15;";
            String focusStyle =
                    "-fx-background-color: rgba(26,26,36,0.8); -fx-text-fill: white;" +
                            "-fx-prompt-text-fill: rgba(248,249,250,0.4); -fx-font-size: 14px;" +
                            "-fx-padding: 12; -fx-background-radius: 15;" +
                            "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-width: 2; -fx-border-radius: 15;";
            searchField.setStyle(baseStyle);
            searchField.focusedProperty().addListener((obs, o, focused) ->
                    searchField.setStyle(focused ? focusStyle : baseStyle));
        }
        if (dateFilter != null) {
            dateFilter.setStyle(
                    "-fx-background-color: rgba(26,26,36,0.6); -fx-text-fill: white;" +
                            "-fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1);" +
                            "-fx-border-width: 2; -fx-border-radius: 15;");
        }
        styleCheckBox(filterConference); styleCheckBox(filterFormation);
        styleCheckBox(filterWorkshop);   styleCheckBox(filterAutre);
        styleCheckBox(filterPrixMoins40); styleCheckBox(filterPrixPlus40);
        styleCheckBox(filterGratuit);
    }

    private void styleCheckBox(CheckBox cb) {
        if (cb != null)
            cb.setStyle("-fx-text-fill: rgba(248,249,250,0.8); -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void setupFilterListeners() {
        if (filterConference  != null) filterConference.selectedProperty().addListener((o, v, n)  -> applyFilters());
        if (filterFormation   != null) filterFormation.selectedProperty().addListener((o, v, n)   -> applyFilters());
        if (filterWorkshop    != null) filterWorkshop.selectedProperty().addListener((o, v, n)    -> applyFilters());
        if (filterAutre       != null) filterAutre.selectedProperty().addListener((o, v, n)       -> applyFilters());
        if (filterPrixMoins40 != null) filterPrixMoins40.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (filterPrixPlus40  != null) filterPrixPlus40.selectedProperty().addListener((o, v, n)  -> applyFilters());
        if (filterGratuit     != null) filterGratuit.selectedProperty().addListener((o, v, n)     -> applyFilters());
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleBackToFront() {
        navigateWithFade("/views/front.fxml", "NeuroWell - Accueil");
    }

    @FXML
    private void handleParticipation() {
        navigateWithFade("/views/participation.fxml", "PSYCHE - Participation");
    }

    @FXML
    private void handleGoToEvaluations() {
        navigateWithFade("/views/showEvaluation.fxml", "NeuroWell - Evaluations");
    }

    // ── Méthode de navigation réutilisable ────────────────────────────────────
    private void navigateWithFade(String fxmlPath, String title) {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = (Stage) eventsContainer.getScene().getWindow();
            FadeTransition fo = new FadeTransition(Duration.millis(300), eventsContainer.getScene().getRoot());
            fo.setFromValue(1.0); fo.setToValue(0.0);
            fo.setOnFinished(ev -> {
                stage.setTitle(title);
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
                FadeTransition fi = new FadeTransition(Duration.millis(300), root);
                fi.setFromValue(0.0); fi.setToValue(1.0); fi.play();
            });
            fo.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page : " + fxmlPath, Alert.AlertType.ERROR);
        }
    }

    // ==================== CHARGEMENT EVENEMENTS ====================

    private void loadEvents() {
        eventsContainer.getChildren().clear();
        try {
            List<Evenement> validEvents = serviceEvenement.recuperer().stream()
                    .filter(ev -> "Valide".equalsIgnoreCase(ev.getStatut_e())
                            || "Validé".equalsIgnoreCase(ev.getStatut_e()))
                    .toList();

            if (validEvents.isEmpty()) {
                eventsContainer.getChildren().add(emptyLabel("Aucun evenement valide disponible."));
                if (resultsCount != null) resultsCount.setText("0 evenement");
                return;
            }
            if (resultsCount != null)
                resultsCount.setText(validEvents.size() + " evenement" + (validEvents.size() > 1 ? "s" : ""));

            for (int i = 0; i < validEvents.size(); i++) {
                VBox card = createModernEventCard(validEvents.get(i));
                eventsContainer.getChildren().add(card);
                animateCard(card, i);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les evenements", Alert.AlertType.ERROR);
        }
    }

    private void animateCard(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(30);
        FadeTransition      ft = new FadeTransition(Duration.millis(500), card);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
        ft.setToValue(1); ft.setDelay(Duration.millis(index * 100L));
        tt.setFromY(30); tt.setToY(0); tt.setDelay(Duration.millis(index * 100L));
        new ParallelTransition(ft, tt).play();
    }

    // ==================== CARTE EVENEMENT ====================

    // ── Remplacer createModernEventCard — supprimer les {{ }} ─────────────────

    private VBox createModernEventCard(Evenement event) {
        VBox card = new VBox(0);
        card.setStyle(cardStyle(false));
        DropShadow shadow = cardShadow(false);
        card.setEffect(shadow);

        card.setOnMouseEntered(e -> {
            card.setStyle(cardStyle(true));
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(-8); tt.play();
            card.setEffect(cardShadow(true));
        });
        card.setOnMouseExited(e -> {
            card.setStyle(cardStyle(false));
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(0); tt.play();
            card.setEffect(shadow);
        });

        HBox mainContent = new HBox(15);
        mainContent.setAlignment(Pos.CENTER_LEFT);
        mainContent.setPadding(new Insets(15));

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120); imageView.setFitHeight(100); imageView.setPreserveRatio(false);
        if (event.getImage() != null && !event.getImage().isEmpty()) {
            for (String path : new String[]{
                    "src/main/resources/" + event.getImage(),
                    System.getProperty("user.dir") + "/src/main/resources/" + event.getImage(),
                    "target/classes/" + event.getImage()}) {
                File f = new File(path);
                if (f.exists()) { imageView.setImage(new Image(f.toURI().toString())); break; }
            }
        }

        // Contenu
        VBox contentBox = new VBox(8);
        contentBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label titleLabel = new Label(event.getTitre_e());
        titleLabel.setWrapText(true); titleLabel.setMaxWidth(450);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label descLabel = new Label(event.getDescription_e());
        descLabel.setWrapText(true); descLabel.setMaxWidth(450);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(248,249,250,0.6); -fx-line-spacing: 2px;");

        HBox infoBox = new HBox(12);
        infoBox.getChildren().addAll(
                infoBadge("📅", event.getDate_e().toLocalDateTime().format(DATE_FORMATTER)),
                infoBadge("📍", event.getLocalisation_e()),
                infoBadge("🎯", event.getType_e()),
                infoBadge("👥", event.getCapacitemax_e() + " places"));
        contentBox.getChildren().addAll(titleLabel, descLabel, infoBox);

        // Colonne droite
        VBox rightBox = new VBox(12);
        rightBox.setAlignment(Pos.CENTER);
        rightBox.setPadding(new Insets(10));

        Label prixLabel = new Label(event.getPrix_e());
        prixLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #00FF88;");

        Button btnParticiper = buildGradientBtn("Participer", "#00D9FF", "#00FF88");
        btnParticiper.setOnAction(e -> openParticipationForm(event));

        Button btnMaps = buildOutlineBtn("Voir sur Maps", PRIMARY_COLOR);
        btnMaps.setOnAction(e -> openMapsPage(event.getLocalisation_e(), event.getTitre_e()));

        Button btnPayer = buildGradientBtn("Payer en ligne", "#FFB300", "#FF6F00");
        btnPayer.setOnAction(e -> openPaymentPage(event));

        rightBox.getChildren().addAll(prixLabel, btnParticiper, btnMaps, btnPayer);
        mainContent.getChildren().addAll(imageView, contentBox, rightBox);
        card.getChildren().add(mainContent);
        return card;
    }

    private Label infoBadge(String emoji, String text) {
        Label label = new Label(emoji + " " + text);
        label.setMaxWidth(160);
        label.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: rgba(248,249,250,0.7); -fx-font-weight: 600;" +
                        "-fx-background-color: rgba(0,240,255,0.1); -fx-padding: 6 12; -fx-background-radius: 15;" +
                        "-fx-border-color: rgba(0,240,255,0.3); -fx-border-width: 1; -fx-border-radius: 15;");
        return label;
    }

    // ==================== FORMULAIRE PARTICIPATION ====================

    // ── Remplacer openParticipationForm — corriger SessionManager ─────────────

    private void openParticipationForm(Evenement event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Participation a l'evenement");
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color:" + CARD_BG + ";-fx-border-color:" + PRIMARY_COLOR + ";" +
                "-fx-border-width:2;-fx-border-radius:15;-fx-background-radius:15;");

        ButtonType btnConfirm = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnConfirm, btnCancel);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color:" + CARD_BG + ";-fx-background-radius:15;");

        Label lblTitle    = new Label("🎫 " + event.getTitre_e());
        lblTitle.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label lblSubtitle = new Label("Reservez votre place maintenant");
        lblSubtitle.setStyle("-fx-font-size:14px;-fx-text-fill:rgba(248,249,250,0.6);");

        TextArea objectifField = new TextArea();
        objectifField.setPromptText("Pourquoi souhaitez-vous participer a cet evenement ?");
        objectifField.setPrefRowCount(4); objectifField.setWrapText(true);
        objectifField.setStyle(
                "-fx-control-inner-background:rgba(26,26,36,0.6);-fx-text-fill:white;" +
                        "-fx-prompt-text-fill:rgba(248,249,250,0.4);-fx-font-size:13px;" +
                        "-fx-padding:12;-fx-background-radius:10;" +
                        "-fx-border-color:rgba(255,255,255,0.1);-fx-border-width:2;-fx-border-radius:10;");

        Label lblMode = new Label("Mode de participation");
        lblMode.setStyle("-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:white;");

        ToggleGroup modeGroup    = new ToggleGroup();
        RadioButton rbEnLigne    = new RadioButton("En ligne");
        RadioButton rbPresentiel = new RadioButton("Presentiel");
        rbEnLigne.setToggleGroup(modeGroup); rbEnLigne.setSelected(true);
        rbPresentiel.setToggleGroup(modeGroup);
        rbEnLigne.setStyle("-fx-text-fill:rgba(248,249,250,0.8);-fx-font-size:13px;");
        rbPresentiel.setStyle("-fx-text-fill:rgba(248,249,250,0.8);-fx-font-size:13px;");

        form.getChildren().addAll(lblTitle, lblSubtitle, objectifField, lblMode,
                new HBox(40, rbEnLigne, rbPresentiel));
        pane.setContent(form);
        pane.lookupButton(btnConfirm).setStyle(
                "-fx-background-color:linear-gradient(to right,#00D9FF,#00FF88);" +
                        "-fx-text-fill:" + DARK_BG + ";-fx-font-weight:bold;" +
                        "-fx-padding:10 20;-fx-background-radius:20;-fx-cursor:hand;");
        pane.lookupButton(btnCancel).setStyle(
                "-fx-background-color:transparent;-fx-border-color:" + PRIMARY_COLOR + ";" +
                        "-fx-border-width:2;-fx-border-radius:20;-fx-background-radius:20;" +
                        "-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:10 20;-fx-cursor:hand;");

        dialog.showAndWait().ifPresent(res -> {
            if (res != btnConfirm) return;
            String objectif = objectifField.getText().trim();
            String mode     = rbEnLigne.isSelected() ? "distanciel" : "presentiel";
            if (objectif.length() < 10) {
                showAlert("Erreur", "L'objectif doit contenir au moins 10 caracteres.", Alert.AlertType.ERROR);
                return;
            }
            try {
                int idUtilisateur = SessionManager.getCurrentUserId();
                if (idUtilisateur == -1) {
                    showAlert("Erreur", "Vous devez etre connecte.", Alert.AlertType.ERROR);
                    return;
                }
                if (serviceParticipation.countByEvenement(event.getId_e()) >= event.getCapacitemax_e()) {
                    showAlert("Complet", "La capacite maximale est atteinte.", Alert.AlertType.WARNING);
                    return;
                }

                // ✅ On construit la Participation avec les IDs directement
                //    sans dépendre d'un objet User de SessionManager
                Participation p = new Participation(
                        event,                              // objet Evenement
                        buildUserFromId(idUtilisateur),     // objet UserUnified minimal
                        mode,
                        objectif);
                serviceParticipation.ajouter(p);
                showAlert("Succes", "Votre participation a ete enregistree !", Alert.AlertType.INFORMATION);

            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Erreur lors de l'enregistrement.", Alert.AlertType.ERROR);
            }
        });
    }

    // ── Helper : construire un UserUnified minimal à partir d'un id ───────────
    private UserUnified buildUserFromId(int id) {
        UserUnified u = new UserUnified();
        u.setId(id);
        return u;
    }

    // ==================== FILTRES ====================

    @FXML private void handleSearch() { applyFilters(); }

    @FXML
    private void resetFilters() {
        if (searchField       != null) searchField.clear();
        if (dateFilter        != null) dateFilter.setValue(null);
        if (filterConference  != null) filterConference.setSelected(false);
        if (filterFormation   != null) filterFormation.setSelected(false);
        if (filterWorkshop    != null) filterWorkshop.setSelected(false);
        if (filterAutre       != null) filterAutre.setSelected(false);
        if (filterPrixMoins40 != null) filterPrixMoins40.setSelected(false);
        if (filterPrixPlus40  != null) filterPrixPlus40.setSelected(false);
        if (filterGratuit     != null) filterGratuit.setSelected(false);
        loadEvents();
    }

    private void applyFilters() {
        eventsContainer.getChildren().clear();
        String    searchText   = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        LocalDate selectedDate = dateFilter  != null ? dateFilter.getValue() : null;

        try {
            List<Evenement> events = serviceEvenement.recuperer();
            int count = 0;
            for (Evenement e : events) {
                if (matchesFilters(e, searchText, selectedDate)) {
                    VBox card = createModernEventCard(e);
                    eventsContainer.getChildren().add(card);
                    animateCard(card, count++);
                }
            }
            if (resultsCount != null)
                resultsCount.setText(count + " evenement" + (count > 1 ? "s" : ""));

            if (count == 0) {
                VBox noResults = new VBox(20); noResults.setAlignment(Pos.CENTER); noResults.setPadding(new Insets(60));
                Label icon = new Label("🔍"); icon.setStyle("-fx-font-size: 64px;");
                Label msg  = new Label("Aucun evenement trouve");
                msg.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
                Label sub  = new Label("Essayez de modifier vos criteres de recherche");
                sub.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(248,249,250,0.6);");
                noResults.getChildren().addAll(icon, msg, sub);
                eventsContainer.getChildren().add(noResults);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Erreur lors du filtrage des evenements", Alert.AlertType.ERROR);
        }
    }

    private boolean matchesFilters(Evenement e, String searchText, LocalDate selectedDate) {
        boolean okSearch = searchText.isEmpty()
                || e.getTitre_e().toLowerCase().contains(searchText)
                || e.getLocalisation_e().toLowerCase().contains(searchText)
                || e.getDescription_e().toLowerCase().contains(searchText);

        boolean okDate = selectedDate == null
                || e.getDate_e().toLocalDateTime().toLocalDate().equals(selectedDate);

        boolean noTypeFilter = filterConference == null || (!filterConference.isSelected()
                && !filterFormation.isSelected() && !filterWorkshop.isSelected() && !filterAutre.isSelected());
        boolean okType = noTypeFilter
                || (filterConference != null && filterConference.isSelected() && e.getType_e().equalsIgnoreCase("Conference"))
                || (filterFormation  != null && filterFormation.isSelected()  && e.getType_e().equalsIgnoreCase("Formation"))
                || (filterWorkshop   != null && filterWorkshop.isSelected()   && e.getType_e().equalsIgnoreCase("Workshop"))
                || (filterAutre      != null && filterAutre.isSelected()
                && !e.getType_e().equalsIgnoreCase("Conference")
                && !e.getType_e().equalsIgnoreCase("Formation")
                && !e.getType_e().equalsIgnoreCase("Workshop"));

        double prix = extractPrix(e.getPrix_e());
        boolean noPrixFilter = filterGratuit == null || (!filterGratuit.isSelected()
                && !filterPrixMoins40.isSelected() && !filterPrixPlus40.isSelected());
        boolean okPrix = noPrixFilter
                || (filterGratuit     != null && filterGratuit.isSelected()     && prix == 0)
                || (filterPrixMoins40 != null && filterPrixMoins40.isSelected() && prix > 0 && prix < 40)
                || (filterPrixPlus40  != null && filterPrixPlus40.isSelected()  && prix >= 40);

        return okSearch && okDate && okType && okPrix;
    }

    private double extractPrix(String prixStr) {
        try { return Double.parseDouble(prixStr.replaceAll("[^0-9.]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    // ==================== PAGES ANNEXES ====================

    private void openPaymentPage(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Payment.fxml"));
            Parent root = loader.load();
            loader.<PaymentController>getController().initPayment(event, "/views/showEvent.fxml");
            navigateToRoot(root, "NeuroWell - Paiement");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page de paiement.", Alert.AlertType.ERROR);
        }
    }

    private void openMapsPage(String localisation, String titre) {
        if (localisation == null || localisation.isBlank()) {
            showAlert("Localisation manquante", "Aucune adresse disponible.", Alert.AlertType.WARNING);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapsView.fxml"));
            Parent root = loader.load();
            loader.<MapsController>getController().initMap(localisation, titre);
            navigateToRoot(root, "NeuroWell - Maps");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la carte.", Alert.AlertType.ERROR);
        }
    }

    private void navigateToRoot(Parent root, String title) {
        Stage stage = (Stage) eventsContainer.getScene().getWindow();
        FadeTransition fo = new FadeTransition(Duration.millis(300), eventsContainer.getScene().getRoot());
        fo.setFromValue(1.0); fo.setToValue(0.0);
        fo.setOnFinished(ev -> {
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle(title);
            FadeTransition fi = new FadeTransition(Duration.millis(300), root);
            fi.setFromValue(0.0); fi.setToValue(1.0); fi.play();
        });
        fo.play();
    }

    // ==================== UI HELPERS ====================

    private String cardStyle(boolean hover) {
        return "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%," +
                (hover ? "rgba(26,26,36,0.8),rgba(26,26,36,0.5)" : "rgba(26,26,36,0.6),rgba(26,26,36,0.3)") + ");" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: " + (hover ? PRIMARY_COLOR : "rgba(255,255,255,0.05)") + ";" +
                "-fx-border-width: 1; -fx-border-radius: 20; -fx-padding: 5;";
    }

    private DropShadow cardShadow(boolean hover) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(PRIMARY_COLOR, hover ? 0.4 : 0.2));
        ds.setRadius(hover ? 25 : 15); ds.setSpread(hover ? 0.3 : 0.2);
        return ds;
    }

    private Button buildGradientBtn(String text, String from, String to) {
        Button btn = new Button(text);
        String base = "-fx-background-color:linear-gradient(to right," + from + "," + to + ");" +
                "-fx-text-fill:" + DARK_BG + ";-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-padding:10 18;-fx-background-radius:25;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> { ScaleTransition s = new ScaleTransition(Duration.millis(150), btn); s.setToX(1.05); s.setToY(1.05); s.play(); });
        btn.setOnMouseExited(e  -> { ScaleTransition s = new ScaleTransition(Duration.millis(150), btn); s.setToX(1.0);  s.setToY(1.0);  s.play(); });
        return btn;
    }

    private Button buildOutlineBtn(String text, String color) {
        Button btn = new Button(text);
        String base  = "-fx-background-color:transparent;-fx-border-color:" + color + ";-fx-border-width:2;" +
                "-fx-border-radius:25;-fx-background-radius:25;-fx-text-fill:" + color + ";" +
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 18;-fx-cursor:hand;";
        String hover = "-fx-background-color:" + color + ";-fx-border-color:" + color + ";-fx-border-width:2;" +
                "-fx-border-radius:25;-fx-background-radius:25;-fx-text-fill:" + DARK_BG + ";" +
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 18;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> { btn.setStyle(hover); ScaleTransition s = new ScaleTransition(Duration.millis(150), btn); s.setToX(1.05); s.setToY(1.05); s.play(); });
        btn.setOnMouseExited(e  -> { btn.setStyle(base);  ScaleTransition s = new ScaleTransition(Duration.millis(150), btn); s.setToX(1.0);  s.setToY(1.0);  s.play(); });
        return btn;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:18px;-fx-text-fill:rgba(248,249,250,0.6);-fx-padding:50;-fx-font-weight:600;");
        return l;
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color:" + CARD_BG + ";-fx-border-color:" + PRIMARY_COLOR + ";" +
                "-fx-border-width:2;-fx-border-radius:15;-fx-background-radius:15;");
        try { dp.lookup(".content.label").setStyle("-fx-text-fill:white;-fx-font-size:14px;"); }
        catch (Exception ignored) {}
        alert.showAndWait();
    }
}