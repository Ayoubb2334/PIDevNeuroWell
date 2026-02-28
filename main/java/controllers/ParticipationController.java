package controllers;

import entities.Evenement;
import entities.Participation;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import services.GeminiService;
import services.googlemeetservice;
import services.ServiceEvenement;
import services.ServiceParticipation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParticipationController {

    private static final String PRIMARY_COLOR   = "#00D9FF";
    private static final String SECONDARY_COLOR = "#00FF88";
    private static final String DARK_BG         = "#050C07";
    private static final String CARD_BG         = "#0D1F12";
    private static final String C_PURPLE        = "#B066FF";
    private static final String C_WARN          = "#FFB347";
    private static final String C_DANGER        = "#FF4D6D";

    @FXML private Circle    orb1, orb2, orb3;
    @FXML private VBox      participationContainer;
    @FXML private VBox      mainAnchorPane;
    @FXML private TextField searchField;
    @FXML private RadioButton filterPresentiel, filterEnLigne, filterTous;
    @FXML private Label     totalParticipations, resultsCount;
    @FXML private ToggleGroup modeGroup;

    @FXML private VBox              geminiSidebar;
    @FXML private VBox              aiResultsBox;
    @FXML private Circle            aiStatusDot;
    @FXML private Label             lblAiStatus;
    @FXML private ProgressIndicator aiLoader;
    @FXML private Button            btnAnalyseSentiment;
    @FXML private Button            btnRecommandations;
    @FXML private Button            btnResume;
    @FXML private Button            btnRisqueAbandon;

    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private final ServiceEvenement     serviceEvenement     = new ServiceEvenement();
    private final googlemeetservice    googleMeetService    = new googlemeetservice();
    private final GeminiService        geminiService        = new GeminiService();

    private List<Participation> currentParticipations = new ArrayList<>();

    // =========================================================================
    @FXML
    public void initialize() {
        if (modeGroup == null) modeGroup = new ToggleGroup();
        if (filterPresentiel != null) filterPresentiel.setToggleGroup(modeGroup);
        if (filterEnLigne    != null) filterEnLigne.setToggleGroup(modeGroup);
        if (filterTous       != null) { filterTous.setToggleGroup(modeGroup); filterTous.setSelected(true); }

        initializeAnimations();
        loadParticipations();
        setupFilterListeners();
        setupGeminiButtons();
        setAiStatus("IA Gemini prete", SECONDARY_COLOR);
    }

    // =========================================================================
    //  GEMINI
    // =========================================================================

    private void setupGeminiButtons() {
        styleAiBtn(btnAnalyseSentiment, C_PURPLE,        "🧠 Analyser sentiments");
        styleAiBtn(btnRecommandations,  PRIMARY_COLOR,   "🎯 Recommandations");
        styleAiBtn(btnResume,           SECONDARY_COLOR, "📊 Resume global");
        styleAiBtn(btnRisqueAbandon,    C_WARN,          "⚠ Risque abandon");
    }

    private void styleAiBtn(Button btn, String color, String text) {
        if (btn == null) return;
        btn.setText(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:" + DARK_BG + ";" +
                "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:10 14;" +
                "-fx-background-radius:12;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> { ScaleTransition s = new ScaleTransition(Duration.millis(120), btn); s.setToX(1.03); s.setToY(1.03); s.play(); });
        btn.setOnMouseExited(e  -> { ScaleTransition s = new ScaleTransition(Duration.millis(120), btn); s.setToX(1.0);  s.setToY(1.0);  s.play(); });
    }

    @FXML
    private void handleAnalyseSentiment() {
        if (currentParticipations.isEmpty()) { showAiError("Aucune participation chargee."); return; }
        setAiLoading(true);
        aiResultsBox.getChildren().clear();
        setAiStatus("Analyse des sentiments...", PRIMARY_COLOR);

        runAsync(() -> {
            List<VBox> cards = new ArrayList<>();
            for (Participation p : currentParticipations) {
                String sentiment = geminiService.analyserSentimentObjectif(p.getObjectif());
                String color     = sentimentColor(sentiment);
                cards.add(buildAiCard("Participant #" + p.getId_p(),
                        "\"" + truncate(p.getObjectif(), 55) + "\"",
                        sentiment, color, sentimentEmoji(sentiment)));
            }
            Platform.runLater(() -> {
                addAiHeader("Analyse sentiments", "🧠");
                for (int i = 0; i < cards.size(); i++) { aiResultsBox.getChildren().add(cards.get(i)); animateAiCard(cards.get(i), i); }
                setAiLoading(false);
                setAiStatus("Analyse terminee — " + cards.size() + " resultats", SECONDARY_COLOR);
            });
        });
    }

    @FXML
    private void handleRecommandations() {
        if (currentParticipations.isEmpty()) { showAiError("Aucune participation chargee."); return; }
        setAiLoading(true);
        aiResultsBox.getChildren().clear();
        setAiStatus("Generation recommandations...", PRIMARY_COLOR);

        runAsync(() -> {
            List<String> types = getTypesEvenements();
            List<VBox> cards   = new ArrayList<>();
            for (Participation p : currentParticipations) {
                String reco = geminiService.recommanderEvenements(p.getObjectif(), p.getModeparticipation(), types);
                cards.add(buildAiCard("Participant #" + p.getId_p(),
                        "Mode : " + p.getModeparticipation(),
                        reco, PRIMARY_COLOR, "🎯"));
            }
            Platform.runLater(() -> {
                addAiHeader("Recommandations", "🎯");
                for (int i = 0; i < cards.size(); i++) { aiResultsBox.getChildren().add(cards.get(i)); animateAiCard(cards.get(i), i); }
                setAiLoading(false);
                setAiStatus("Recommandations generees", SECONDARY_COLOR);
            });
        });
    }

    @FXML
    private void handleResume() {
        if (currentParticipations.isEmpty()) { showAiError("Aucune participation chargee."); return; }
        setAiLoading(true);
        aiResultsBox.getChildren().clear();
        setAiStatus("Generation du resume...", PRIMARY_COLOR);

        runAsync(() -> {
            List<String> objectifs = currentParticipations.stream()
                    .map(Participation::getObjectif).collect(Collectors.toList());
            long pres = currentParticipations.stream()
                    .filter(p -> "presentiel".equalsIgnoreCase(p.getModeparticipation())).count();
            long dist = currentParticipations.size() - pres;
            String resume = geminiService.genererResume(objectifs, currentParticipations.size(), (int) pres, (int) dist);

            Platform.runLater(() -> {
                addAiHeader("Resume analytique", "📊");
                aiResultsBox.getChildren().add(buildStatsRow(currentParticipations.size(), (int) pres, (int) dist));
                VBox card = buildAiCard("Analyse globale", "", resume, SECONDARY_COLOR, "📝");
                aiResultsBox.getChildren().add(card);
                animateAiCard(card, 0);
                setAiLoading(false);
                setAiStatus("Resume genere", SECONDARY_COLOR);
            });
        });
    }

    @FXML
    private void handleRisqueAbandon() {
        if (currentParticipations.isEmpty()) { showAiError("Aucune participation chargee."); return; }
        setAiLoading(true);
        aiResultsBox.getChildren().clear();
        setAiStatus("Evaluation des risques...", PRIMARY_COLOR);

        runAsync(() -> {
            List<VBox> haut = new ArrayList<>(), moyen = new ArrayList<>(), faible = new ArrayList<>();
            for (Participation p : currentParticipations) {
                String risque = geminiService.evaluerRisqueAbandon(p.getObjectif(), p.getModeparticipation());
                VBox card = buildAiCard(
                        "Participant #" + p.getId_p() + " — " + risque,
                        "\"" + truncate(p.getObjectif(), 55) + "\"",
                        "Mode : " + p.getModeparticipation(),
                        risqueColor(risque), risqueEmoji(risque));
                if      ("Eleve".equalsIgnoreCase(risque)) haut.add(card);
                else if ("Moyen".equalsIgnoreCase(risque)) moyen.add(card);
                else                                        faible.add(card);
            }
            Platform.runLater(() -> {
                addAiHeader("Risque d'abandon", "⚠");
                int idx = 0;
                if (!haut.isEmpty()) {
                    aiResultsBox.getChildren().add(sectionLabel("Risque ELEVE", C_DANGER));
                    for (VBox c : haut)   { aiResultsBox.getChildren().add(c); animateAiCard(c, idx++); }
                }
                if (!moyen.isEmpty()) {
                    aiResultsBox.getChildren().add(sectionLabel("Risque MOYEN", C_WARN));
                    for (VBox c : moyen)  { aiResultsBox.getChildren().add(c); animateAiCard(c, idx++); }
                }
                if (!faible.isEmpty()) {
                    aiResultsBox.getChildren().add(sectionLabel("Risque FAIBLE", SECONDARY_COLOR));
                    for (VBox c : faible) { aiResultsBox.getChildren().add(c); animateAiCard(c, idx++); }
                }
                setAiLoading(false);
                setAiStatus("Evaluation terminee", SECONDARY_COLOR);
            });
        });
    }

    // ── Gemini UI helpers ─────────────────────────────────────────────────────

    private VBox buildAiCard(String title, String subtitle, String content, String color, String emoji) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(13));
        card.setStyle("-fx-background-color:rgba(13,31,18,0.88);-fx-background-radius:13;" +
                "-fx-border-color:" + color + ";-fx-border-width:1;-fx-border-radius:13;");
        HBox hdr = new HBox(7); hdr.setAlignment(Pos.CENTER_LEFT);
        Label emojiL = new Label(emoji); emojiL.setStyle("-fx-font-size:14px;");
        Label titleL = new Label(title); titleL.setStyle("-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;");
        hdr.getChildren().addAll(emojiL, titleL);
        card.getChildren().add(hdr);
        if (!subtitle.isEmpty()) {
            Label sub = new Label(subtitle);
            sub.setStyle("-fx-text-fill:rgba(255,255,255,0.40);-fx-font-size:10px;");
            sub.setWrapText(true);
            card.getChildren().add(sub);
        }
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + color + ";-fx-opacity:0.3;");
        card.getChildren().add(sep);
        Label body = new Label(content);
        body.setStyle("-fx-text-fill:rgba(255,255,255,0.80);-fx-font-size:11px;-fx-line-spacing:3;");
        body.setWrapText(true);
        card.getChildren().add(body);
        return card;
    }

    private void addAiHeader(String title, String emoji) {
        HBox h = new HBox(8); h.setAlignment(Pos.CENTER_LEFT); h.setPadding(new Insets(0, 0, 6, 0));
        Label e = new Label(emoji); e.setStyle("-fx-font-size:16px;");
        Label t = new Label(title); t.setStyle("-fx-text-fill:" + C_PURPLE + ";-fx-font-size:13px;-fx-font-weight:bold;");
        h.getChildren().addAll(e, t);
        aiResultsBox.getChildren().add(h);
    }

    private Label sectionLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-color:rgba(0,0,0,0.3);-fx-padding:3 10;-fx-background-radius:7;");
        return l;
    }

    private HBox buildStatsRow(int total, int pres, int dist) {
        HBox row = new HBox(8); row.setPadding(new Insets(0, 0, 10, 0));
        row.getChildren().addAll(
                statBox("Total",      String.valueOf(total), PRIMARY_COLOR),
                statBox("Presentiel", String.valueOf(pres),  SECONDARY_COLOR),
                statBox("En ligne",   String.valueOf(dist),  C_PURPLE));
        return row;
    }

    private VBox statBox(String lbl, String val, String color) {
        VBox box = new VBox(3); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color:rgba(0,0,0,0.3);-fx-background-radius:10;" +
                "-fx-border-color:" + color + ";-fx-border-width:1;-fx-border-radius:10;");
        HBox.setHgrow(box, Priority.ALWAYS);
        Label v = new Label(val); v.setStyle("-fx-text-fill:" + color + ";-fx-font-size:20px;-fx-font-weight:bold;");
        Label l = new Label(lbl); l.setStyle("-fx-text-fill:rgba(255,255,255,0.50);-fx-font-size:10px;");
        box.getChildren().addAll(v, l);
        return box;
    }

    private void showAiError(String msg) {
        aiResultsBox.getChildren().clear();
        Label l = new Label("⚠ " + msg);
        l.setStyle("-fx-text-fill:" + C_DANGER + ";-fx-font-size:12px;");
        aiResultsBox.getChildren().add(l);
    }

    private void setAiStatus(String msg, String color) {
        if (lblAiStatus != null) { lblAiStatus.setText(msg); lblAiStatus.setStyle("-fx-text-fill:" + color + ";-fx-font-size:10px;"); }
        if (aiStatusDot != null) aiStatusDot.setFill(Color.web(color));
    }

    private void setAiLoading(boolean loading) {
        if (aiLoader            != null) aiLoader.setVisible(loading);
        if (btnAnalyseSentiment != null) btnAnalyseSentiment.setDisable(loading);
        if (btnRecommandations  != null) btnRecommandations.setDisable(loading);
        if (btnResume           != null) btnResume.setDisable(loading);
        if (btnRisqueAbandon    != null) btnRisqueAbandon.setDisable(loading);
    }

    private void animateAiCard(VBox card, int idx) {
        card.setOpacity(0); card.setTranslateX(20);
        FadeTransition ft = new FadeTransition(Duration.millis(350), card);
        ft.setToValue(1); ft.setDelay(Duration.millis(idx * 60L)); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
        tt.setToX(0); tt.setDelay(Duration.millis(idx * 60L)); tt.play();
    }

    private void runAsync(Runnable task) {
        Thread t = new Thread(task, "Gemini-Thread");
        t.setDaemon(true);
        t.start();
    }

    private List<String> getTypesEvenements() {
        try {
            return serviceEvenement.recuperer().stream()
                    .map(Evenement::getType_e).distinct().collect(Collectors.toList());
        } catch (SQLException e) {
            return List.of("Conference", "Formation", "Workshop");
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String sentimentColor(String s) {
        return switch (s) {
            case "Tres motive" -> SECONDARY_COLOR;
            case "Motive"      -> PRIMARY_COLOR;
            case "Neutre"      -> C_WARN;
            default            -> C_DANGER;
        };
    }

    private String sentimentEmoji(String s) {
        return switch (s) {
            case "Tres motive" -> "🔥";
            case "Motive"      -> "✅";
            case "Neutre"      -> "😐";
            default            -> "😔";
        };
    }

    private String risqueColor(String r) {
        return switch (r) {
            case "Faible" -> SECONDARY_COLOR;
            case "Moyen"  -> C_WARN;
            default       -> C_DANGER;
        };
    }

    private String risqueEmoji(String r) {
        return switch (r) {
            case "Faible" -> "✅";
            case "Moyen"  -> "⚠";
            default       -> "🚨";
        };
    }

    // =========================================================================
    //  PARTICIPATIONS — Chargement & navigation
    // =========================================================================

    private void initializeAnimations() {
        if (orb1 != null) animateOrb(orb1,  30, -20, Duration.seconds(15));
        if (orb2 != null) animateOrb(orb2, -35,  25, Duration.seconds(18));
        if (orb3 != null) animateOrb(orb3,  20, -15, Duration.seconds(20));
    }

    private void animateOrb(Circle orb, double dx, double dy, Duration dur) {
        TranslateTransition tt = new TranslateTransition(dur, orb);
        tt.setByX(dx); tt.setByY(dy);
        tt.setCycleCount(Animation.INDEFINITE); tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
    }

    private void setupFilterListeners() {
        if (filterPresentiel != null) filterPresentiel.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (filterEnLigne    != null) filterEnLigne.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (filterTous       != null) filterTous.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (searchField      != null) searchField.textProperty().addListener((o, v, n) -> applyFilters());
    }

    @FXML private void handleBackToFront() { navigateTo("/views/dashboard.fxml", "NeuroWell - Accueil"); }

    @FXML
    private void handleEvenement() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/showEvent.fxml"));
            mainAnchorPane.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) participationContainer.getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300), participationContainer.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setTitle(title);
                stage.getScene().setRoot(root);
                FadeTransition fi = new FadeTransition(Duration.millis(300), root);
                fi.setFromValue(0); fi.setToValue(1); fi.play();
            });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page", Alert.AlertType.ERROR);
        }
    }

    private void loadParticipations() {
        participationContainer.getChildren().clear();
        try {
            int userId = services.SessionManager.getCurrentUserId();
            List<Participation> list = userId != -1
                    ? serviceParticipation.recupererParUser(userId)
                    : serviceParticipation.recuperer();

            currentParticipations = list;

            if (list.isEmpty()) { showEmptyState("Aucune participation disponible."); updateStats(0, 0); return; }
            if (totalParticipations != null) totalParticipations.setText(String.valueOf(list.size()));
            if (resultsCount != null) resultsCount.setText(list.size() + " participation" + (list.size() > 1 ? "s" : ""));

            for (int i = 0; i < list.size(); i++) {
                VBox card = createCard(list.get(i));
                participationContainer.getChildren().add(card);
                animateCard(card, i);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les participations", Alert.AlertType.ERROR);
        }
    }

    // ✅ UNE SEULE méthode createCard — utilise directement p.getEvenement()
    private VBox createCard(Participation p) {
        // Accès direct à l'objet Evenement embarqué — pas de requête supplémentaire
        Evenement ev = p.getEvenement();

        String titre = ev != null ? ev.getTitre_e()        : "Evenement supprime";
        String type  = ev != null ? ev.getType_e()         : "-";
        String prix  = ev != null ? ev.getPrix_e()         : "-";
        String lieu  = ev != null ? ev.getLocalisation_e() : "-";

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

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20));

        VBox info = new VBox(10);
        info.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblTitre = new Label("🎫  " + titre);
        lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        lblTitre.setWrapText(true); lblTitre.setMaxWidth(460);

        HBox badges = new HBox(10);
        badges.getChildren().addAll(infoBadge("🎯", type), infoBadge("📍", lieu), infoBadge("💰", prix));

        Label lblObj = new Label("Objectif : " + p.getObjectif());
        lblObj.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(248,249,250,0.65);");
        lblObj.setWrapText(true); lblObj.setMaxWidth(460);

        info.getChildren().addAll(lblTitre, badges, modeBadge(p.getModeparticipation()), lblObj);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8));

        Button btnEdit   = actionBtn("✎  Modifier",  PRIMARY_COLOR);
        Button btnDelete = actionBtn("✖  Supprimer", "#FF6B6B");
        btnEdit.setOnAction(e -> openEditDialog(p));
        btnDelete.setOnAction(e -> handleDelete(p));
        actions.getChildren().addAll(btnEdit, btnDelete);

        // Bouton Meet uniquement si distanciel ET evenement disponible
        if ("distanciel".equalsIgnoreCase(p.getModeparticipation()) && ev != null) {
            Button btnMeet = meetBtn();
            btnMeet.setOnAction(e -> ouvrirSalleMeet(ev.getTitre_e(), ev.getDate_e(), p));
            actions.getChildren().add(btnMeet);
        }

        row.getChildren().addAll(info, actions);
        card.getChildren().add(row);
        return card;
    }

    private void ouvrirSalleMeet(String titre, java.sql.Timestamp date, Participation p) {
        Stage meetStage = new Stage();
        meetStage.initModality(Modality.APPLICATION_MODAL);
        meetStage.initStyle(StageStyle.TRANSPARENT);
        meetStage.setTitle("NeuroWell Meet — " + titre);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MeetRoom.fxml"));
            Parent root = loader.load();
            MeetRoomController ctrl = loader.getController();
            ctrl.initMeet(titre, null, date, p.getModeparticipation());
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            meetStage.setScene(scene);
            root.setOpacity(0);
            meetStage.show();
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setToValue(1); ft.play();
            new Thread(() -> {
                try {
                    String meetUrl = googleMeetService.creerReunionMeet(titre, date);
                    Platform.runLater(() -> ctrl.initMeet(titre, meetUrl, date, p.getModeparticipation()));
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        meetStage.close();
                        showAlert("Erreur Google Meet", "Impossible de creer la reunion :\n" + ex.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            }, "MeetGen-Thread").start();
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface Meet", Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleSearch() { applyFilters(); }

    @FXML
    private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (filterTous  != null) filterTous.setSelected(true);
        loadParticipations();
    }

    private void applyFilters() {
        participationContainer.getChildren().clear();
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        String mode   = filterPresentiel != null && filterPresentiel.isSelected() ? "presentiel"
                : filterEnLigne != null && filterEnLigne.isSelected() ? "distanciel" : "";
        try {
            int userId = services.SessionManager.getCurrentUserId();
            List<Participation> list = userId != -1
                    ? serviceParticipation.recupererParUser(userId)
                    : serviceParticipation.recuperer();
            currentParticipations = list;
            int count = 0;
            for (Participation p : list) {
                // ✅ Direct via l'objet embarqué — plus de stream/filter
                String titre = p.getEvenement() != null ? p.getEvenement().getTitre_e() : "";
                boolean okSearch = search.isEmpty()
                        || p.getObjectif().toLowerCase().contains(search)
                        || titre.toLowerCase().contains(search);
                boolean okMode = mode.isEmpty() || p.getModeparticipation().equalsIgnoreCase(mode);
                if (okSearch && okMode) {
                    VBox card = createCard(p);
                    participationContainer.getChildren().add(card);
                    animateCard(card, count++);
                }
            }
            updateStats(list.size(), count);
            if (count == 0) showEmptyState("Aucune participation ne correspond a vos criteres.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void handleDelete(Participation p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer la participation");
        confirm.setContentText("Cette action est irreversible. Confirmer ?");
        styleDialogPane(confirm.getDialogPane());
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    serviceParticipation.supprimer(p);
                    loadParticipations();
                    showAlert("Succes", "Participation supprimee", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void openEditDialog(Participation p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la participation");
        DialogPane pane = dialog.getDialogPane();
        styleDialogPane(pane);
        ButtonType save   = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler",     ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        VBox form = new VBox(18); form.setPadding(new Insets(28));
        Label titleLbl = new Label("Modifier la participation");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");
        Label lblObj = new Label("Objectif");
        lblObj.setStyle("-fx-text-fill: rgba(200,255,220,0.70); -fx-font-size: 13px; -fx-font-weight: 600;");
        TextArea taObj = new TextArea(p.getObjectif());
        taObj.setWrapText(true); taObj.setPrefRowCount(4);
        taObj.setStyle("-fx-control-inner-background: #112016; -fx-background-color: #112016; -fx-text-fill: #E8FFF0;" +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 12;" +
                "-fx-border-color: rgba(0,217,255,0.25); -fx-border-width: 1.5; -fx-border-radius: 12;");
        Label lblMode = new Label("Mode");
        lblMode.setStyle("-fx-text-fill: rgba(200,255,220,0.70); -fx-font-size: 13px; -fx-font-weight: 600;");
        ToggleGroup tg = new ToggleGroup();
        RadioButton rbDist = new RadioButton("En ligne (distanciel)"); rbDist.setToggleGroup(tg);
        rbDist.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;");
        RadioButton rbPres = new RadioButton("Presentiel"); rbPres.setToggleGroup(tg);
        rbPres.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;");
        if (p.getModeparticipation().equalsIgnoreCase("presentiel")) rbPres.setSelected(true);
        else rbDist.setSelected(true);
        form.getChildren().addAll(titleLbl, lblObj, taObj, lblMode, new HBox(24, rbDist, rbPres));
        pane.setContent(form);
        pane.lookupButton(save).setStyle(
                "-fx-background-color: linear-gradient(to right," + PRIMARY_COLOR + "," + SECONDARY_COLOR + ");" +
                        "-fx-text-fill:" + DARK_BG + ";-fx-font-weight:bold;-fx-padding:10 24;-fx-background-radius:20;-fx-cursor:hand;");
        pane.lookupButton(cancel).setStyle(
                "-fx-background-color:transparent;-fx-border-color:#FF4D6D;-fx-border-width:1.5;" +
                        "-fx-border-radius:20;-fx-background-radius:20;-fx-text-fill:#FF4D6D;" +
                        "-fx-font-weight:bold;-fx-padding:10 24;-fx-cursor:hand;");

        dialog.showAndWait().ifPresent(r -> {
            if (r == save) {
                String obj  = taObj.getText().trim();
                String mode = rbDist.isSelected() ? "distanciel" : "presentiel";
                if (obj.length() < 10) {
                    showAlert("Validation", "L'objectif doit contenir au moins 10 caracteres.", Alert.AlertType.WARNING);
                    return;
                }
                p.setObjectif(obj); p.setModeparticipation(mode);
                try {
                    serviceParticipation.modifier(p);
                    loadParticipations();
                    showAlert("Succes", "Participation modifiee", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible de modifier.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────────

    private String cardStyle(boolean hover) {
        return "-fx-background-color: linear-gradient(to bottom right," +
                (hover ? "rgba(19,36,24,0.85),rgba(19,36,24,0.55)" : "rgba(19,36,24,0.60),rgba(19,36,24,0.30)") + "); " +
                "-fx-background-radius: 20; -fx-border-color: " + (hover ? PRIMARY_COLOR : "rgba(0,217,255,0.10)") + "; " +
                "-fx-border-width: 1; -fx-border-radius: 20; -fx-padding: 5;";
    }

    private DropShadow cardShadow(boolean hover) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(PRIMARY_COLOR, hover ? 0.40 : 0.20));
        ds.setRadius(hover ? 25 : 15); ds.setSpread(hover ? 0.30 : 0.20);
        return ds;
    }

    private Label infoBadge(String emoji, String text) {
        Label l = new Label(emoji + " " + text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(248,249,250,0.65);" +
                "-fx-background-color: rgba(0,217,255,0.08); -fx-padding: 5 12;" +
                "-fx-background-radius: 14; -fx-border-color: rgba(0,217,255,0.25);" +
                "-fx-border-width: 1; -fx-border-radius: 14;");
        return l;
    }

    private Label modeBadge(String mode) {
        boolean pre = mode.equalsIgnoreCase("presentiel");
        String c    = pre ? SECONDARY_COLOR : PRIMARY_COLOR;
        Label l = new Label(pre ? "Presentiel" : "En ligne");
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + c + ";" +
                "-fx-background-color: rgba(0,217,255,0.08); -fx-padding: 6 16;" +
                "-fx-background-radius: 18; -fx-border-color: " + c + ";" +
                "-fx-border-width: 1; -fx-border-radius: 18;");
        return l;
    }

    private Button actionBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 12px;" +
                "-fx-font-weight: 600; -fx-padding: 9 22; -fx-background-radius: 14; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> { ScaleTransition s = new ScaleTransition(Duration.millis(130), btn); s.setToX(1.05); s.setToY(1.05); s.play(); });
        btn.setOnMouseExited(e  -> { ScaleTransition s = new ScaleTransition(Duration.millis(130), btn); s.setToX(1.0);  s.setToY(1.0);  s.play(); });
        return btn;
    }

    private Button meetBtn() {
        Button btn = new Button("Rejoindre Meet");
        String base = "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88);" +
                "-fx-text-fill: #050C07; -fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-padding: 9 18; -fx-background-radius: 14; -fx-cursor: hand;";
        btn.setStyle(base);
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00D9FF", 0.40)); glow.setRadius(16);
        btn.setEffect(glow);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(base.replace("#00D9FF, #00FF88", "#00FF88, #00D9FF"));
            ScaleTransition s = new ScaleTransition(Duration.millis(130), btn); s.setToX(1.05); s.setToY(1.05); s.play();
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(base);
            ScaleTransition s = new ScaleTransition(Duration.millis(130), btn); s.setToX(1.0); s.setToY(1.0); s.play();
        });
        return btn;
    }

    private void animateCard(VBox card, int idx) {
        card.setOpacity(0); card.setTranslateY(28);
        FadeTransition ft = new FadeTransition(Duration.millis(480), card);
        ft.setToValue(1); ft.setDelay(Duration.millis(idx * 75L)); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(480), card);
        tt.setToY(0); tt.setDelay(Duration.millis(idx * 75L)); tt.play();
        new ParallelTransition(ft, tt).play();
    }

    private void showEmptyState(String msg) {
        VBox box = new VBox(16); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(80));
        Label icon = new Label("📋"); icon.setStyle("-fx-font-size: 64px;");
        Label lbl  = new Label(msg);  lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(200,255,220,0.55);");
        box.getChildren().addAll(icon, lbl);
        participationContainer.getChildren().add(box);
    }

    private void updateStats(int total, int filtered) {
        if (totalParticipations != null) totalParticipations.setText(String.valueOf(total));
        if (resultsCount != null) resultsCount.setText(filtered + " participation" + (filtered > 1 ? "s" : ""));
    }

    private void styleDialogPane(DialogPane pane) {
        pane.setStyle("-fx-background-color: " + CARD_BG + "; -fx-border-color: " + PRIMARY_COLOR + ";" +
                "-fx-border-width: 2; -fx-border-radius: 18; -fx-background-radius: 18;");
        try { pane.lookup(".content.label").setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;"); }
        catch (Exception ignored) {}
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleDialogPane(a.getDialogPane()); a.showAndWait();
    }
}