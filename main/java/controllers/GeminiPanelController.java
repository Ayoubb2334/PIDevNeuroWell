package controllers;

import entities.Participation;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.GeminiService;
import services.ServiceEvenement;
import services.ServiceParticipation;
import entities.Evenement;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panneau IA Gemini integre dans la vue Participation
 * A inclure dans ParticipationController via fx:include
 */
public class GeminiPanelController {

    private static final String C_CYAN    = "#00D9FF";
    private static final String C_GREEN   = "#00FF88";
    private static final String C_DARK    = "#050C07";
    private static final String C_CARD    = "#0D1F12";
    private static final String C_DANGER  = "#FF4D6D";
    private static final String C_WARN    = "#FFB347";
    private static final String C_PURPLE  = "#B066FF";

    @FXML private VBox      geminiPanel;
    @FXML private Label     lblAiStatus;
    @FXML private Circle    aiStatusDot;
    @FXML private VBox      resultsBox;
    @FXML private ProgressIndicator aiLoader;

    @FXML private Button btnAnalyseSentiment;
    @FXML private Button btnRecommandations;
    @FXML private Button btnResume;
    @FXML private Button btnRisqueAbandon;

    private final GeminiService        geminiService     = new GeminiService();
    private final ServiceParticipation serviceParticip   = new ServiceParticipation();
    private final ServiceEvenement     serviceEvenement  = new ServiceEvenement();

    private List<Participation> participations = new ArrayList<>();

    @FXML
    public void initialize() {
        styleButtons();
        loadParticipations();
        setStatus("IA Gemini prête", C_GREEN);
    }

    public void setParticipations(List<Participation> list) {
        this.participations = list;
    }

    // ================================================================
    //  1. ANALYSE SENTIMENT — tous les objectifs
    // ================================================================
    @FXML
    private void handleAnalyseSentiment() {
        if (participations.isEmpty()) { showError("Aucune participation chargee."); return; }

        setLoading(true);
        resultsBox.getChildren().clear();
        setStatus("Analyse des sentiments...", C_CYAN);

        runAsync(() -> {
            List<VBox> cards = new ArrayList<>();
            for (Participation p : participations) {
                String sentiment = geminiService.analyserSentimentObjectif(p.getObjectif());
                String color     = sentimentColor(sentiment);
                VBox card = buildResultCard(
                        "Participant #" + p.getId_p(),
                        "\"" + truncate(p.getObjectif(), 60) + "\"",
                        sentiment, color, sentimentEmoji(sentiment)
                );
                cards.add(card);
            }
            Platform.runLater(() -> {
                addHeader("Analyse de sentiment des objectifs", "🧠");
                cards.forEach(c -> {
                    resultsBox.getChildren().add(c);
                    animateCard(c, resultsBox.getChildren().size());
                });
                setLoading(false);
                setStatus("Analyse terminee — " + cards.size() + " participants", C_GREEN);
            });
        });
    }

    // ================================================================
    //  2. RECOMMANDATIONS PERSONNALISEES
    // ================================================================
    @FXML
    private void handleRecommandations() {
        if (participations.isEmpty()) { showError("Aucune participation chargee."); return; }

        setLoading(true);
        resultsBox.getChildren().clear();
        setStatus("Generation des recommandations...", C_CYAN);

        runAsync(() -> {
            List<String> types = getTypesEvenements();
            List<VBox> cards   = new ArrayList<>();

            for (Participation p : participations) {
                String reco = geminiService.recommanderEvenements(
                        p.getObjectif(), p.getModeparticipation(), types);
                VBox card = buildResultCard(
                        "Recommandation — Participant #" + p.getId_p(),
                        "Mode actuel : " + p.getModeparticipation(),
                        reco, C_CYAN, "🎯"
                );
                cards.add(card);
            }
            Platform.runLater(() -> {
                addHeader("Recommandations personnalisees", "🎯");
                cards.forEach(c -> {
                    resultsBox.getChildren().add(c);
                    animateCard(c, resultsBox.getChildren().size());
                });
                setLoading(false);
                setStatus("Recommandations generees", C_GREEN);
            });
        });
    }

    // ================================================================
    //  3. RESUME GLOBAL
    // ================================================================
    @FXML
    private void handleResume() {
        if (participations.isEmpty()) { showError("Aucune participation chargee."); return; }

        setLoading(true);
        resultsBox.getChildren().clear();
        setStatus("Generation du resume...", C_CYAN);

        runAsync(() -> {
            List<String> objectifs = participations.stream()
                    .map(Participation::getObjectif)
                    .collect(Collectors.toList());

            long presentiel  = participations.stream()
                    .filter(p -> "presentiel".equalsIgnoreCase(p.getModeparticipation())).count();
            long distanciel  = participations.size() - presentiel;

            String resume = geminiService.genererResume(
                    objectifs, participations.size(), (int) presentiel, (int) distanciel);

            Platform.runLater(() -> {
                addHeader("Resume analytique IA", "📊");
                // Stats visuelles
                HBox stats = buildStatsRow(participations.size(), (int) presentiel, (int) distanciel);
                resultsBox.getChildren().add(stats);
                // Texte resume
                VBox card = buildResultCard("Analyse globale", "", resume, C_GREEN, "📝");
                resultsBox.getChildren().add(card);
                animateCard(card, 1);
                setLoading(false);
                setStatus("Resume genere avec succes", C_GREEN);
            });
        });
    }

    // ================================================================
    //  4. RISQUE D'ABANDON
    // ================================================================
    @FXML
    private void handleRisqueAbandon() {
        if (participations.isEmpty()) { showError("Aucune participation chargee."); return; }

        setLoading(true);
        resultsBox.getChildren().clear();
        setStatus("Evaluation des risques...", C_CYAN);

        runAsync(() -> {
            List<VBox> cardsHaut  = new ArrayList<>();
            List<VBox> cardsMoyen = new ArrayList<>();
            List<VBox> cardsFaible = new ArrayList<>();

            for (Participation p : participations) {
                String risque = geminiService.evaluerRisqueAbandon(
                        p.getObjectif(), p.getModeparticipation());
                String color = risqueColor(risque);
                String emoji = risqueEmoji(risque);
                VBox card = buildResultCard(
                        "Participant #" + p.getId_p() + " — Risque " + risque,
                        "\"" + truncate(p.getObjectif(), 60) + "\"",
                        "Mode : " + p.getModeparticipation(), color, emoji
                );
                if ("Eleve".equalsIgnoreCase(risque))       cardsHaut.add(card);
                else if ("Moyen".equalsIgnoreCase(risque))  cardsMoyen.add(card);
                else                                         cardsFaible.add(card);
            }

            Platform.runLater(() -> {
                addHeader("Evaluation du risque d'abandon", "⚠️");
                if (!cardsHaut.isEmpty()) {
                    resultsBox.getChildren().add(sectionLabel("Risque ELEVE", C_DANGER));
                    cardsHaut.forEach(c -> { resultsBox.getChildren().add(c); animateCard(c, resultsBox.getChildren().size()); });
                }
                if (!cardsMoyen.isEmpty()) {
                    resultsBox.getChildren().add(sectionLabel("Risque MOYEN", C_WARN));
                    cardsMoyen.forEach(c -> { resultsBox.getChildren().add(c); animateCard(c, resultsBox.getChildren().size()); });
                }
                if (!cardsFaible.isEmpty()) {
                    resultsBox.getChildren().add(sectionLabel("Risque FAIBLE", C_GREEN));
                    cardsFaible.forEach(c -> { resultsBox.getChildren().add(c); animateCard(c, resultsBox.getChildren().size()); });
                }
                setLoading(false);
                setStatus("Evaluation terminee", C_GREEN);
            });
        });
    }

    // ================================================================
    //  HELPERS UI
    // ================================================================

    private VBox buildResultCard(String title, String subtitle, String content,
                                 String accentColor, String emoji) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color:rgba(13,31,18,0.85);" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + accentColor + ";" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:14;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:16px;");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;");
        header.getChildren().addAll(emojiLbl, titleLbl);

        card.getChildren().add(header);

        if (!subtitle.isEmpty()) {
            Label subLbl = new Label(subtitle);
            subLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.45);-fx-font-size:11px;");
            subLbl.setWrapText(true);
            card.getChildren().add(subLbl);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + accentColor + ";-fx-opacity:0.3;");
        card.getChildren().add(sep);

        Label contentLbl = new Label(content);
        contentLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.80);-fx-font-size:12px;-fx-line-spacing:3;");
        contentLbl.setWrapText(true);
        card.getChildren().add(contentLbl);

        return card;
    }

    private void addHeader(String title, String emoji) {
        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(0, 0, 8, 0));
        Label e  = new Label(emoji); e.setStyle("-fx-font-size:20px;");
        Label t  = new Label(title); t.setStyle("-fx-text-fill:" + C_CYAN + ";-fx-font-size:15px;-fx-font-weight:bold;");
        hdr.getChildren().addAll(e, t);
        resultsBox.getChildren().add(hdr);
    }

    private Label sectionLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-color:rgba(0,0,0,0.3);-fx-padding:4 12;" +
                "-fx-background-radius:8;");
        return l;
    }

    private HBox buildStatsRow(int total, int presentiel, int distanciel) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(0, 0, 12, 0));
        row.getChildren().addAll(
                statBox("Total", String.valueOf(total), C_CYAN),
                statBox("Presentiel", String.valueOf(presentiel), C_GREEN),
                statBox("En ligne", String.valueOf(distanciel), C_PURPLE)
        );
        return row;
    }

    private VBox statBox(String label, String value, String color) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color:rgba(0,0,0,0.3);" +
                "-fx-background-radius:12;" +
                "-fx-border-color:" + color + ";" +
                "-fx-border-width:1;-fx-border-radius:12;");
        HBox.setHgrow(box, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + color + ";-fx-font-size:24px;-fx-font-weight:bold;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:rgba(255,255,255,0.55);-fx-font-size:11px;");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private void showError(String msg) {
        resultsBox.getChildren().clear();
        Label l = new Label("⚠ " + msg);
        l.setStyle("-fx-text-fill:" + C_DANGER + ";-fx-font-size:13px;");
        resultsBox.getChildren().add(l);
    }

    private void setStatus(String msg, String color) {
        if (lblAiStatus  != null) { lblAiStatus.setText(msg); lblAiStatus.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;"); }
        if (aiStatusDot  != null) aiStatusDot.setFill(Color.web(color));
    }

    private void setLoading(boolean loading) {
        if (aiLoader != null) aiLoader.setVisible(loading);
        if (btnAnalyseSentiment != null) btnAnalyseSentiment.setDisable(loading);
        if (btnRecommandations  != null) btnRecommandations.setDisable(loading);
        if (btnResume           != null) btnResume.setDisable(loading);
        if (btnRisqueAbandon    != null) btnRisqueAbandon.setDisable(loading);
    }

    private void animateCard(VBox card, int idx) {
        card.setOpacity(0); card.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setToValue(1); ft.setDelay(Duration.millis(idx * 80L)); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setToY(0); tt.setDelay(Duration.millis(idx * 80L)); tt.play();
    }

    private void styleButtons() {
        styleBtn(btnAnalyseSentiment, C_PURPLE, "🧠 Analyser sentiments");
        styleBtn(btnRecommandations,  C_CYAN,   "🎯 Recommandations");
        styleBtn(btnResume,           C_GREEN,  "📊 Resume global");
        styleBtn(btnRisqueAbandon,    C_WARN,   "⚠️ Risque abandon");
    }

    private void styleBtn(Button btn, String color, String text) {
        if (btn == null) return;
        btn.setText(text);
        String base = "-fx-background-color:" + color + ";-fx-text-fill:" + C_DARK + ";" +
                "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:10 16;" +
                "-fx-background-radius:12;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> { ScaleTransition s = new ScaleTransition(Duration.millis(120), btn); s.setToX(1.03); s.setToY(1.03); s.play(); });
        btn.setOnMouseExited(e ->  { ScaleTransition s = new ScaleTransition(Duration.millis(120), btn); s.setToX(1.0);  s.setToY(1.0);  s.play(); });
    }

    private void loadParticipations() {
        try {
            int userId = services.SessionManager.getCurrentUserId();
            this.participations = userId != -1
                    ? serviceParticip.recupererParUser(userId)
                    : serviceParticip.recuperer();
        } catch (SQLException e) {
            System.err.println("Erreur chargement participations : " + e.getMessage());
        }
    }

    private List<String> getTypesEvenements() {
        try {
            return serviceEvenement.recuperer().stream()
                    .map(Evenement::getType_e).distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            return List.of("Conference", "Formation", "Workshop");
        }
    }

    private void runAsync(Runnable task) {
        Thread t = new Thread(task, "Gemini-Thread");
        t.setDaemon(true);
        t.start();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String sentimentColor(String s) {
        return switch (s) {
            case "Tres motive" -> C_GREEN;
            case "Motive"      -> C_CYAN;
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
            case "Faible" -> C_GREEN;
            case "Moyen"  -> C_WARN;
            default       -> C_DANGER;
        };
    }

    private String risqueEmoji(String r) {
        return switch (r) {
            case "Faible" -> "✅";
            case "Moyen"  -> "⚠️";
            default       -> "🚨";
        };
    }
}