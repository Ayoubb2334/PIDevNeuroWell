package controllers;

import entities.Evaluation;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvaluation;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  NEUROWELL — Questionnaire Interactif                        ║
 * ║  Reproduction fidèle de la démo HTML                        ║
 * ║  10 questions · Score auto · Date auto · Sauvegarde auto    ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class QuestionnaireController {

    // ── Palette identique à la démo ──
    private static final String C_CYAN    = "#00D9FF";
    private static final String C_GREEN   = "#00FF88";
    private static final String C_DARK    = "#050C07";
    private static final String C_CARD    = "#0D1F12";
    private static final String C_SURFACE = "#112016";
    private static final String C_TEXT    = "#E8FFF0";
    private static final String C_DANGER  = "#FF4D6D";
    private static final String C_WARN    = "#FFD700";
    private static final String C_PURPLE  = "#A78BFA";

    // ── Scale colors exactly like the demo (1→5) ──
    private static final String[] SCALE_COLORS = {
        "#00FF88", "#80FF88", "#FFD700", "#FF8C00", "#FF4D6D"
    };
    private static final String[] SCALE_LABELS = {
        "Jamais", "Rarement", "Parfois", "Souvent", "Toujours"
    };

    // ── FXML — Root & Navbar ──
    @FXML private StackPane rootPane;
    @FXML private Label     lblTopType;

    // ── FXML — Orbs ──
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    // ── FXML — Phases ──
    @FXML private VBox  phaseIntro;
    @FXML private VBox  phaseQuiz;
    @FXML private VBox  phaseResult;

    // ── FXML — Type cards ──
    @FXML private VBox btnStress;
    @FXML private VBox btnAnxiete;
    @FXML private VBox btnDepression;
    @FXML private VBox btnBienetre;
    @FXML private VBox btnBurnout;
    @FXML private Button btnStart;

    // ── FXML — Progress ──
    @FXML private Label     lblCounter;
    @FXML private Label     lblPct;
    @FXML private Rectangle progressFill;
    @FXML private Pane      progressPane;
    @FXML private HBox      dotsRow;

    // ── FXML — Question ──
    @FXML private VBox  questionCard;
    @FXML private Label lblQNumber;
    @FXML private Label lblQText;
    @FXML private Label lblScaleMin;
    @FXML private Label lblScaleMax;
    @FXML private HBox  scaleRow;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    // ── FXML — Result ──
    @FXML private StackPane scoreCirclePane;
    @FXML private Label     lblScoreType;
    @FXML private Label     lblScoreDate;
    @FXML private Label     lblNiveauBadge;
    @FXML private Label     lblInterp;
    @FXML private Label     lblRecomm;
    @FXML private VBox      answersContainer;

    // ── State ──
    private String   selectedType = null;
    private int      currentQ     = 0;
    private int[]    answers;
    private String[][] questions;

    private final ServiceEvaluation serviceEvaluation = new ServiceEvaluation();

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE — Fullscreen + orb animations
    // ═══════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    rootPane.prefWidthProperty().bind(newScene.widthProperty());
                    rootPane.prefHeightProperty().bind(newScene.heightProperty());
                    newScene.widthProperty().addListener((o, ov, nv) -> positionOrbs(nv.doubleValue(), newScene.getHeight()));
                    newScene.heightProperty().addListener((o, ov, nv) -> positionOrbs(newScene.getWidth(), nv.doubleValue()));
                    positionOrbs(newScene.getWidth(), newScene.getHeight());
                }
            });
        }
        animateOrbs();
        initTypeCards();

        // Bind progressFill to grow inside progressPane
        if (progressPane != null) {
            progressPane.sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) {
                    // progressFill height matches pane height, width animated
                    progressFill.heightProperty().bind(progressPane.heightProperty());
                }
            });
        }
    }

    private void positionOrbs(double w, double h) {
        if (orb1 != null) { orb1.setTranslateX(-w * 0.25); orb1.setTranslateY(-h * 0.20); }
        if (orb2 != null) { orb2.setTranslateX( w * 0.35); orb2.setTranslateY( h * 0.40); }
        if (orb3 != null) { orb3.setTranslateX( w * 0.05); orb3.setTranslateY( h * 0.10); }
    }

    private void animateOrbs() {
        animateOrb(orb1,  30, -20, 14);
        animateOrb(orb2, -25,  18, 18);
        animateOrb(orb3,  18, -14, 20);
    }

    private void animateOrb(Circle orb, double dx, double dy, double secs) {
        if (orb == null) return;
        TranslateTransition t = new TranslateTransition(Duration.seconds(secs), orb);
        t.setByX(dx); t.setByY(dy);
        t.setCycleCount(Animation.INDEFINITE);
        t.setAutoReverse(true);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.play();
    }

    private void initTypeCards() {
        String[] baseColors = {C_CYAN, C_GREEN, C_PURPLE, C_GREEN, C_WARN};
        VBox[] cards = {btnStress, btnAnxiete, btnDepression, btnBienetre, btnBurnout};
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) cards[i].setStyle(typeCardStyle(baseColors[i], false));
        }
    }

    @FXML
    private void quitTest() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/showEvaluation.fxml"));
            Stage stage = (Stage) phaseIntro.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            FadeTransition ft = new FadeTransition(Duration.millis(300), phaseIntro.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> { stage.setScene(scene); stage.show(); });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  PHASE 1 — Sélection du type
    // ═══════════════════════════════════════════════════════
    @FXML private void selectStress()     { selectType("Stress",     btnStress);     }
    @FXML private void selectAnxiete()    { selectType("Anxiété",    btnAnxiete);    }
    @FXML private void selectDepression() { selectType("Dépression", btnDepression); }
    @FXML private void selectBienetre()   { selectType("Bien-être",  btnBienetre);   }
    @FXML private void selectBurnout()    { selectType("Burnout",    btnBurnout);    }

    private void selectType(String type, VBox selectedCard) {
        selectedType = type;
        if (lblTopType != null) lblTopType.setText(getTypeEmoji(type) + "  " + type);

        // Reset all cards
        VBox[] allCards = {btnStress, btnAnxiete, btnDepression, btnBienetre, btnBurnout};
        String[] cardColors = {C_CYAN, C_GREEN, C_PURPLE, C_GREEN, C_WARN};
        for (int i = 0; i < allCards.length; i++) {
            allCards[i].setStyle(typeCardStyle(cardColors[i], false));
            // Reset text color
            allCards[i].getChildren().forEach(node -> {
                if (node instanceof Label lbl && !lbl.getText().contains("🧘")
                        && !lbl.getText().contains("😰") && !lbl.getText().contains("💙")
                        && !lbl.getText().contains("🌿") && !lbl.getText().contains("⚡")) {
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: rgba(200,255,220,0.70);");
                }
            });
        }

        // Highlight selected card
        int idx = Arrays.asList(allCards).indexOf(selectedCard);
        String color = cardColors[idx];
        selectedCard.setStyle(typeCardStyle(color, true));
        selectedCard.getChildren().forEach(node -> {
            if (node instanceof Label lbl && !lbl.getText().contains("🧘")
                    && !lbl.getText().contains("😰") && !lbl.getText().contains("💙")
                    && !lbl.getText().contains("🌿") && !lbl.getText().contains("⚡")) {
                lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            }
        });

        // Scale animation on card — bounce
        ScaleTransition bounce = new ScaleTransition(Duration.millis(150), selectedCard);
        bounce.setFromX(1.0); bounce.setToX(1.08);
        bounce.setFromY(1.0); bounce.setToY(1.08);
        bounce.setAutoReverse(true); bounce.setCycleCount(2); bounce.play();

        // Enable start button
        btnStart.setStyle(
            "-fx-background-color: linear-gradient(to right," + C_CYAN + "," + C_GREEN + "); " +
            "-fx-text-fill: #050C07; -fx-font-weight: bold; -fx-font-size: 14px; " +
            "-fx-padding: 14 40; -fx-background-radius: 25; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,217,255,0.45),18,0.4,0,4);"
        );
        btnStart.setOpacity(1.0);
    }

    private String typeCardStyle(String color, boolean selected) {
        if (selected) return
            "-fx-background-color: " + color + "22; " +
            "-fx-border-color: " + color + "; -fx-border-width: 2; " +
            "-fx-border-radius: 16; -fx-background-radius: 16; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian," + color + "55,18,0.3,0,0);";
        return
            "-fx-background-color: #0D1F12; " +
            "-fx-border-color: " + color + "44; -fx-border-width: 1.5; " +
            "-fx-border-radius: 16; -fx-background-radius: 16; -fx-cursor: hand;";
    }

    // ═══════════════════════════════════════════════════════
    //  PHASE 2 — Quiz
    // ═══════════════════════════════════════════════════════
    @FXML
    private void startQuiz() {
        if (selectedType == null) return;

        questions = getQuestions(selectedType);
        answers   = new int[10];
        Arrays.fill(answers, -1);
        currentQ  = 0;

        // Build progress dots (like demo)
        dotsRow.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            Label dot = new Label("●");
            dot.setId("dot" + i);
            dot.setStyle("-fx-text-fill: rgba(0,217,255,0.20); -fx-font-size: 8px;");
            dotsRow.getChildren().add(dot);
        }

        // Switch phase with fade
        switchPhase(phaseIntro, phaseQuiz);
        renderQuestion();
    }

    private void renderQuestion() {
        // ── Card slide animation ──
        questionCard.setTranslateY(20);
        questionCard.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), questionCard);
        tt.setToY(0); tt.play();
        FadeTransition ft = new FadeTransition(Duration.millis(350), questionCard);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        String[] q = questions[currentQ];

        // Progress
        int pct = (int) Math.round((currentQ / 10.0) * 100);
        lblCounter.setText("Question " + (currentQ + 1) + " / 10");
        lblPct.setText(pct + "%");

        // Animate progress fill — use progressPane width
        double trackW = progressPane != null ? progressPane.getWidth() : 700;
        if (trackW <= 0) trackW = 700;
        final double totalWidth = trackW;
        final double targetWidth = (pct / 100.0) * totalWidth;
        final double fromWidth   = progressFill.getWidth();
        Timeline progressAnim = new Timeline(
            new KeyFrame(Duration.ZERO,       new KeyValue(progressFill.widthProperty(), fromWidth)),
            new KeyFrame(Duration.millis(500), new KeyValue(progressFill.widthProperty(), targetWidth, Interpolator.EASE_OUT))
        );
        progressAnim.play();

        // Dots
        for (int i = 0; i < 10; i++) {
            Label dot = (Label) dotsRow.getChildren().get(i);
            if (i < currentQ)
                dot.setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-size: 8px;");
            else if (i == currentQ)
                dot.setStyle("-fx-text-fill: " + C_CYAN + "; -fx-font-size: 11px;");
            else
                dot.setStyle("-fx-text-fill: rgba(0,217,255,0.20); -fx-font-size: 8px;");
        }

        // Question text
        lblQNumber.setText("QUESTION " + String.format("%02d", currentQ + 1));
        lblQText.setText(q[0]);
        lblScaleMin.setText(q[1]);
        lblScaleMax.setText(q[2]);

        // Build 5 scale buttons — exactly like demo
        scaleRow.getChildren().clear();
        for (int v = 1; v <= 5; v++) {
            final int val = v;
            boolean selected = answers[currentQ] == val;

            VBox scaleBtn = buildScaleButton(val, selected);
            scaleBtn.setOnMouseEntered(e -> scaleBtn.setStyle(scaleBtnStyle(SCALE_COLORS[val-1], true)));
            scaleBtn.setOnMouseExited(e  -> scaleBtn.setStyle(scaleBtnStyle(SCALE_COLORS[val-1], answers[currentQ] == val)));
            scaleBtn.setOnMouseClicked(e -> {
                answers[currentQ] = val;

                // Refresh all 5 scale buttons
                for (int i = 0; i < scaleRow.getChildren().size(); i++) {
                    VBox btn = (VBox) scaleRow.getChildren().get(i);
                    int btnVal = i + 1;
                    boolean sel = answers[currentQ] == btnVal;
                    btn.setStyle(scaleBtnStyle(SCALE_COLORS[i], sel));
                    // Update number label color
                    Label numLbl = (Label) btn.getChildren().get(0);
                    numLbl.setStyle(scaleLblStyle(SCALE_COLORS[i], sel));
                    // Pop animation
                    if (sel) {
                        ScaleTransition pop = new ScaleTransition(Duration.millis(150), btn);
                        pop.setFromX(1.0); pop.setToX(1.1);
                        pop.setFromY(1.0); pop.setToY(1.1);
                        pop.setAutoReverse(true); pop.setCycleCount(2); pop.play();
                    }
                }

                // Enable Next button
                activateNextBtn();
            });

            scaleRow.getChildren().add(scaleBtn);
        }

        // Nav buttons state
        btnPrev.setOpacity(currentQ == 0 ? 0.35 : 1.0);
        btnPrev.setDisable(currentQ == 0);

        if (answers[currentQ] != -1) activateNextBtn();
        else                         deactivateNextBtn();

        btnNext.setText(currentQ == 9 ? "✨  Voir mon résultat" : "Suivant →");
    }

    private VBox buildScaleButton(int val, boolean selected) {
        String color = SCALE_COLORS[val - 1];

        Label numLbl  = new Label(String.valueOf(val));
        numLbl.setStyle(scaleLblStyle(color, selected));

        Label descLbl = new Label(SCALE_LABELS[val - 1]);
        descLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(200,255,220,0.40); -fx-text-alignment: center;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(80);

        VBox box = new VBox(6, numLbl, descLbl);
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(80);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(scaleBtnStyle(color, selected));
        box.setCursor(javafx.scene.Cursor.HAND);
        return box;
    }

    private String scaleBtnStyle(String color, boolean selected) {
        if (selected) return
            "-fx-background-color: " + color + "25; " +
            "-fx-border-color: " + color + "; -fx-border-width: 2; " +
            "-fx-border-radius: 14; -fx-background-radius: 14; " +
            "-fx-effect: dropshadow(gaussian," + color + "66,14,0.3,0,0);";
        return
            "-fx-background-color: #112016; " +
            "-fx-border-color: rgba(0,217,255,0.18); -fx-border-width: 1.5; " +
            "-fx-border-radius: 14; -fx-background-radius: 14;";
    }

    private String scaleLblStyle(String color, boolean selected) {
        return "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: "
               + (selected ? color : "rgba(200,255,220,0.60)") + ";";
    }

    private void activateNextBtn() {
        btnNext.setStyle(
            "-fx-background-color: linear-gradient(to right," + C_CYAN + "," + C_GREEN + "); " +
            "-fx-text-fill: #050C07; -fx-font-weight: bold; -fx-font-size: 13px; " +
            "-fx-padding: 12 24; -fx-border-radius: 14; -fx-background-radius: 14; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(0,217,255,0.40),14,0.3,0,3);"
        );
        btnNext.setDisable(false);
    }

    private void deactivateNextBtn() {
        btnNext.setStyle(
            "-fx-background-color: rgba(0,217,255,0.10); " +
            "-fx-text-fill: rgba(200,255,220,0.35); -fx-font-size: 13px; " +
            "-fx-padding: 12 24; -fx-border-radius: 14; -fx-background-radius: 14;"
        );
        btnNext.setDisable(true);
    }

    @FXML
    private void prevQuestion() {
        if (currentQ > 0) {
            currentQ--;
            renderQuestion();
        }
    }

    @FXML
    private void nextQuestion() {
        if (currentQ < 9) {
            currentQ++;
            renderQuestion();
        } else {
            showResult();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  PHASE 3 — Résultat + Sauvegarde automatique
    // ═══════════════════════════════════════════════════════
    private void showResult() {
        // Calcul du score — identique à la démo HTML
        int total  = Arrays.stream(answers).sum();
        // total entre 10 (toutes à 1) et 50 (toutes à 5)
        // Normalisé 0–100
        int score100 = Math.round(((float)(total - 10) / 40) * 100);
        score100 = Math.max(0, Math.min(100, score100));

        String niveau;
        String scoreColor;
        String niveauEmoji;
        if (score100 <= 20) {
            niveau = "Faible"; scoreColor = C_GREEN;  niveauEmoji = "✅";
        } else if (score100 <= 50) {
            niveau = "Moyen";  scoreColor = C_WARN;   niveauEmoji = "⚠️";
        } else {
            niveau = "Élevé";  scoreColor = C_DANGER; niveauEmoji = "🔴";
        }

        // ── 1. Sauvegarde automatique ────────────────────────
        Date today = Date.valueOf(LocalDate.now());
        Evaluation ev = new Evaluation();
        ev.setTypeTest(selectedType);
        ev.setScore(score100);
        ev.setNiveau(niveau);
        ev.setDateEvaluation(today);
        ev.setIdUser(0);

        boolean saved = false;
        try {
            serviceEvaluation.ajouter(ev);
            saved = true;
            // Vérifier alerte niveau élevé consécutif
            verifierAlerteConsecutive(selectedType);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // ── 2. Switch vers résultat ──────────────────────────
        switchPhase(phaseQuiz, phaseResult);

        // ── 3. Cercle score animé ────────────────────────────
        scoreCirclePane.getChildren().clear();
        Circle ringBg = new Circle(60, Color.web(C_SURFACE));
        ringBg.setStroke(Color.web(scoreColor, 0.85));
        ringBg.setStrokeWidth(4);

        Label scoreNumLbl = new Label("0");
        scoreNumLbl.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");

        StackPane ring = new StackPane(ringBg, scoreNumLbl);

        // Pulse animation — exact comme la démo
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), ring);
        pulse.setFromX(1.0); pulse.setToX(1.06);
        pulse.setFromY(1.0); pulse.setToY(1.06);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        scoreCirclePane.getChildren().add(ring);

        // Animate score counter like the demo
        final int finalScore = score100;
        int[] counter = {0};
        Timeline countUp = new Timeline(
            new KeyFrame(Duration.millis(40), e -> {
                counter[0] = Math.min(counter[0] + 2, finalScore);
                scoreNumLbl.setText(String.valueOf(counter[0]));
            })
        );
        countUp.setCycleCount((int) Math.ceil(finalScore / 2.0) + 5);
        countUp.play();

        // Labels
        lblScoreType.setText(getTypeEmoji(selectedType) + "  " + selectedType);
        lblScoreType.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        lblScoreDate.setText("📅  " + today + (saved ? "  ✅ Sauvegardé automatiquement" : ""));
        lblScoreDate.setStyle("-fx-text-fill: " + (saved ? "#00FF88" : "rgba(200,255,220,0.45)") + "; -fx-font-size: 12px;");

        lblNiveauBadge.setText(niveauEmoji + "  " + niveau);
        lblNiveauBadge.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8 24; " +
            "-fx-border-color: " + scoreColor + "; -fx-border-width: 2; " +
            "-fx-border-radius: 30; -fx-background-radius: 30; " +
            "-fx-background-color: " + scoreColor + "22; " +
            "-fx-text-fill: " + scoreColor + ";"
        );

        // ── 4. Interprétation + Recommandation ───────────────
        lblInterp.setText(getInterpretation(selectedType, niveau));
        lblRecomm.setText(getRecommandation(selectedType, niveau));

        // ── 5. Détail des réponses — comme la démo ───────────
        answersContainer.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            int val = answers[i];
            String col = SCALE_COLORS[Math.max(0, Math.min(val - 1, 4))];
            String shortQ = questions[i][0].length() > 58
                ? questions[i][0].substring(0, 58) + "…"
                : questions[i][0];

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7, 12, 7, 12));
            row.setStyle("-fx-background-color: rgba(17,32,22,0.80); -fx-background-radius: 8;");

            Label qLbl = new Label("Q" + (i + 1) + "  " + shortQ);
            qLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 11px;");
            qLbl.setMaxWidth(430);
            HBox.setHgrow(qLbl, Priority.ALWAYS);

            Label aLbl = new Label(val + "/5");
            aLbl.setStyle(
                "-fx-text-fill: " + col + "; -fx-font-weight: bold; -fx-font-size: 12px; " +
                "-fx-background-color: " + col + "20; -fx-padding: 3 10; -fx-background-radius: 8;"
            );

            row.getChildren().addAll(qLbl, aLbl);
            answersContainer.getChildren().add(row);

            // Staggered fade-in
            row.setOpacity(0);
            FadeTransition rowFade = new FadeTransition(Duration.millis(300), row);
            rowFade.setDelay(Duration.millis(50L * i + 400));
            rowFade.setFromValue(0); rowFade.setToValue(1); rowFade.play();
        }
    }

    // ── Restart — back to phase 1 like the demo ──────────────
    @FXML
    private void restart() {
        selectedType = null;
        currentQ     = 0;
        answers      = null;
        questions    = null;

        // Reset type cards
        VBox[] allCards = {btnStress, btnAnxiete, btnDepression, btnBienetre, btnBurnout};
        String[] cardColors = {C_CYAN, C_GREEN, C_PURPLE, C_GREEN, C_WARN};
        for (int i = 0; i < allCards.length; i++) {
            final int idx = i;
            allCards[i].setStyle(typeCardStyle(cardColors[i], false));
            allCards[i].getChildren().forEach(node -> {
                if (node instanceof Label lbl && !lbl.getText().matches(".*[🧘😰💙🌿⚡].*")) {
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: rgba(200,255,220,0.70);");
                }
            });
        }

        // Reset start button
        btnStart.setStyle(
            "-fx-background-color: linear-gradient(to right," + C_CYAN + "," + C_GREEN + "); " +
            "-fx-text-fill: #050C07; -fx-font-weight: bold; -fx-font-size: 14px; " +
            "-fx-padding: 14 40; -fx-background-radius: 25; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,217,255,0.45),18,0.4,0,4); -fx-opacity: 0.4;"
        );
        btnStart.setOpacity(0.4);

        switchPhase(phaseResult, phaseIntro);
    }

    // ── Smooth phase transition ───────────────────────────────
    private void switchPhase(VBox from, VBox to) {
        FadeTransition out = new FadeTransition(Duration.millis(200), from);
        out.setFromValue(1); out.setToValue(0);
        out.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            to.setOpacity(0);
            to.setVisible(true);
            to.setManaged(true);
            FadeTransition in = new FadeTransition(Duration.millis(350), to);
            in.setFromValue(0); in.setToValue(1); in.play();
        });
        out.play();
    }

    // ═══════════════════════════════════════════════════════
    //  10 QUESTIONS PAR TYPE
    // ═══════════════════════════════════════════════════════
    private String[][] getQuestions(String type) {
        return switch (type) {
            case "Stress" -> new String[][]{
                {"Au cours de la semaine passée, à quelle fréquence vous êtes-vous senti(e) dépassé(e) par les événements ?", "Jamais", "Très souvent"},
                {"Avez-vous du mal à vous détendre même pendant votre temps libre ?", "Pas du tout", "Constamment"},
                {"Votre cœur s'emballe-t-il ou ressentez-vous des tensions musculaires sous pression ?", "Rarement", "Tout le temps"},
                {"Les responsabilités du quotidien vous semblent-elles lourdes à porter ?", "Légères", "Écrasantes"},
                {"Avez-vous des difficultés à dormir à cause de pensées qui tournent en boucle ?", "Jamais", "Chaque nuit"},
                {"Votre appétit ou digestion est-il perturbé en période de pression ?", "Non", "Oui, beaucoup"},
                {"Ressentez-vous de l'irritabilité ou de l'impatience inhabituelle ?", "Rarement", "Constamment"},
                {"Avez-vous du mal à vous concentrer ou finir vos tâches habituelles ?", "Non", "Tout le temps"},
                {"Ressentez-vous des maux de tête ou douleurs corporelles liés à la pression ?", "Jamais", "Souvent"},
                {"Votre stress affecte-t-il votre qualité de vie globale ?", "Pas du tout", "Énormément"},
            };
            case "Anxiété" -> new String[][]{
                {"Ressentez-vous une inquiétude persistante pour des situations qui ne sont pas encore arrivées ?", "Jamais", "En permanence"},
                {"Évitez-vous des situations ou lieux par crainte de ce qui pourrait arriver ?", "Jamais", "Très souvent"},
                {"Votre anxiété provoque-t-elle des symptômes physiques (tremblements, nausées, sueurs) ?", "Non", "Oui, fréquemment"},
                {"Vos pensées anxieuses interfèrent-elles avec votre concentration ?", "Rarement", "Constamment"},
                {"Avez-vous l'impression de perdre le contrôle face à vos peurs ?", "Jamais", "Souvent"},
                {"Êtes-vous souvent en attente d'un événement catastrophique ?", "Non", "Presque toujours"},
                {"L'anxiété affecte-t-elle vos relations sociales ou professionnelles ?", "Pas du tout", "Beaucoup"},
                {"Avez-vous du mal à décider par peur de faire le mauvais choix ?", "Rarement", "Tout le temps"},
                {"Vous sentez-vous constamment tendu(e) ou sur vos gardes sans raison ?", "Non", "Oui, souvent"},
                {"Votre anxiété vous empêche-t-elle de profiter des moments agréables ?", "Jamais", "Toujours"},
            };
            case "Dépression" -> new String[][]{
                {"Avez-vous perdu de l'intérêt pour des activités qui vous plaisaient avant ?", "Pas du tout", "Complètement"},
                {"Vous sentez-vous fatigué(e) ou sans énergie, même après une bonne nuit de sommeil ?", "Rarement", "Presque toujours"},
                {"Avez-vous des pensées négatives récurrentes sur vous-même ou votre avenir ?", "Jamais", "Très fréquemment"},
                {"Votre appétit a-t-il changé significativement (perte ou augmentation) ?", "Non", "Oui, beaucoup"},
                {"Avez-vous du mal à vous concentrer ou prendre des décisions simples ?", "Pas du tout", "Toujours"},
                {"Vous sentez-vous souvent triste, vide ou sans espoir ?", "Rarement", "Presque tous les jours"},
                {"Vous isolez-vous ou évitez-vous vos proches ?", "Non", "Très souvent"},
                {"Vos problèmes semblent-ils insurmontables ou sans solution ?", "Jamais", "Constamment"},
                {"Votre estime de vous-même a-t-elle baissé récemment ?", "Non", "Beaucoup"},
                {"Avez-vous du mal à trouver un sens ou plaisir dans votre quotidien ?", "Non", "Oui, totalement"},
            };
            case "Bien-être" -> new String[][]{
                {"Dans quelle mesure vous sentez-vous épanoui(e) dans votre vie actuelle ?", "Pas du tout", "Totalement"},
                {"Avez-vous des relations sociales satisfaisantes et un sentiment d'appartenance ?", "Non", "Oui, pleinement"},
                {"Trouvez-vous un sens dans vos activités quotidiennes ?", "Rarement", "Toujours"},
                {"Votre niveau d'énergie vous permet-il de profiter de vos journées ?", "Non", "Oui, totalement"},
                {"Vous sentez-vous en paix avec vous-même et vos choix de vie ?", "Rarement", "Souvent"},
                {"Votre sommeil est-il réparateur et suffisant ?", "Rarement", "Toujours"},
                {"Pratiquez-vous régulièrement des activités qui vous apportent du plaisir ?", "Jamais", "Très souvent"},
                {"Vous sentez-vous reconnu(e) et valorisé(e) dans votre entourage ?", "Non", "Oui, pleinement"},
                {"Gérez-vous bien l'équilibre vie professionnelle / vie personnelle ?", "Pas du tout", "Très bien"},
                {"Avez-vous un sentiment d'optimisme pour votre avenir ?", "Non", "Oui, vraiment"},
            };
            default -> new String[][]{ // Burnout
                {"Ressentez-vous un épuisement profond lié au travail, même après le week-end ?", "Jamais", "Constamment"},
                {"Avez-vous l'impression que votre travail perd de son sens ?", "Non", "Absolument"},
                {"Votre efficacité au travail a-t-elle diminué malgré plus d'efforts ?", "Non", "Beaucoup"},
                {"Ressentez-vous du cynisme ou de l'irritabilité envers vos collègues ?", "Jamais", "Très souvent"},
                {"Avez-vous du mal à décrocher mentalement de votre travail ?", "Non", "Impossible"},
                {"Vous sentez-vous vidé(e) émotionnellement après une journée de travail ?", "Rarement", "Chaque jour"},
                {"Votre motivation professionnelle a-t-elle fortement diminué ?", "Non", "Totalement"},
                {"Négligez-vous votre santé (sommeil, repas, sport) à cause du travail ?", "Non", "Souvent"},
                {"Avez-vous le sentiment que rien de ce que vous faites ne suffit jamais ?", "Jamais", "Constamment"},
                {"Envisagez-vous de quitter votre poste à cause de l'épuisement ?", "Non", "Sérieusement"},
            };
        };
    }

    // ═══════════════════════════════════════════════════════
    //  IDÉE 7 — ALERTE NIVEAUX ÉLEVÉS CONSÉCUTIFS
    // ═══════════════════════════════════════════════════════
    private void verifierAlerteConsecutive(String typeTest) {
        try {
            java.util.List<entities.Evaluation> toutes = serviceEvaluation.recuperer();
            java.util.List<entities.Evaluation> parType = toutes.stream()
                .filter(e -> typeTest.equals(e.getTypeTest()) && e.getDateEvaluation() != null)
                .sorted((a, b) -> b.getDateEvaluation().compareTo(a.getDateEvaluation()))
                .collect(java.util.stream.Collectors.toList());

            int consecutifs = 0;
            for (entities.Evaluation e : parType) {
                if ("Élevé".equals(e.getNiveau())) consecutifs++;
                else break;
            }
            if (consecutifs >= 3) {
                showAlerteDialog(typeTest, consecutifs);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlerteDialog(String typeTest, int nb) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("🚨  Alerte — Niveau élevé répété");

        // Son d'alerte — alert.wav joué 3 fois (javax.sound, aucune dépendance)
        new Thread(() -> {
            try {
                java.net.URL soundUrl = getClass().getResource("/sounds/alert.wav");
                if (soundUrl != null) {
                    for (int i = 0; i < 3; i++) {
                        javax.sound.sampled.AudioInputStream ais =
                            javax.sound.sampled.AudioSystem.getAudioInputStream(soundUrl);
                        javax.sound.sampled.Clip clip =
                            javax.sound.sampled.AudioSystem.getClip();
                        clip.open(ais);
                        clip.start();
                        // Attendre la fin du son avant de rejouer
                        Thread.sleep(clip.getMicrosecondLength() / 1000 + 300);
                        clip.close();
                    }
                }
            } catch (Exception ignored) {}
        }).start();

        javafx.scene.control.DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(500);
        pane.setStyle(
            "-fx-background-color: #1A0508;" +
            "-fx-border-color: #FF4D6D; -fx-border-width: 2.5;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        pane.getButtonTypes().add(new javafx.scene.control.ButtonType("J'ai compris", javafx.scene.control.ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(18);
        content.setPadding(new Insets(28));

        // Icône pulsante
        Label alertIcon = new Label("🚨");
        alertIcon.setStyle("-fx-font-size: 52px;");
        alertIcon.setAlignment(Pos.CENTER);
        alertIcon.setMaxWidth(Double.MAX_VALUE);
        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), alertIcon);
        pulse.setFromX(1.0); pulse.setToX(1.15);
        pulse.setFromY(1.0); pulse.setToY(1.15);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        // Titre
        Label titleLbl = new Label("Niveau Élevé détecté " + nb + " fois de suite !");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(Double.MAX_VALUE);
        titleLbl.setAlignment(Pos.CENTER);
        titleLbl.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Type badge
        Label typeLbl = new Label(getTypeEmoji(typeTest) + "  " + typeTest);
        typeLbl.setMaxWidth(Double.MAX_VALUE);
        typeLbl.setAlignment(Pos.CENTER);
        typeLbl.setStyle(
            "-fx-text-fill: #FF4D6D; -fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-background-color: rgba(255,77,109,0.12);" +
            "-fx-border-color: rgba(255,77,109,0.40); -fx-border-width: 1;" +
            "-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 20;"
        );

        // Message
        Label msgLbl = new Label(
            "Vos " + nb + " dernières évaluations de type \"" + typeTest +
            "\" sont toutes au niveau Élevé.\n\n" +
            "Cette tendance répétée peut indiquer un besoin d'accompagnement professionnel."
        );
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: rgba(255,180,180,0.90); -fx-font-size: 13px; -fx-line-spacing: 4;");

        // Recommandation
        VBox recommBox = new VBox(8);
        recommBox.setPadding(new Insets(14, 16, 14, 16));
        recommBox.setStyle(
            "-fx-background-color: rgba(255,77,109,0.10);" +
            "-fx-border-color: rgba(255,77,109,0.35); -fx-border-width: 1;" +
            "-fx-border-radius: 14; -fx-background-radius: 14;"
        );
        Label recommTitle = new Label("💡  Recommandation urgente");
        recommTitle.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label recommText = new Label(getRecommandationUrgente(typeTest));
        recommText.setWrapText(true);
        recommText.setStyle("-fx-text-fill: rgba(255,200,200,0.85); -fx-font-size: 12px; -fx-line-spacing: 3;");
        recommBox.getChildren().addAll(recommTitle, recommText);

        content.getChildren().addAll(alertIcon, titleLbl, typeLbl, msgLbl, recommBox);
        pane.setContent(content);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: #FF4D6D;" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-padding: 12 40; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    private String getRecommandationUrgente(String type) {
        return switch (type) {
            case "Stress"     -> "Consultez un médecin ou un psychologue. Pratiquez la cohérence cardiaque 3×/jour et réduisez les sources de stress non essentielles.";
            case "Anxiété"    -> "Une thérapie cognitivo-comportementale (TCC) est fortement recommandée. Contactez un professionnel dès que possible.";
            case "Dépression" -> "Consultez un médecin ou un psychiatre rapidement. Vous n'êtes pas seul(e) — l'aide professionnelle fait une vraie différence.";
            case "Bien-être"  -> "Un accompagnement en développement personnel peut être bénéfique. Parlez-en à un professionnel de santé.";
            case "Burnout"    -> "Consultez votre médecin pour un arrêt si nécessaire. Le burnout non traité a des conséquences graves sur la santé.";
            default           -> "Consultez un professionnel de santé mentale pour un suivi adapté à votre situation.";
        };
    }

    // ═══════════════════════════════════════════════════════
    //  INTERPRÉTATIONS & RECOMMANDATIONS
    // ═══════════════════════════════════════════════════════
    private String getInterpretation(String type, String niveau) {
        return switch (type) {
            case "Stress" -> switch (niveau) {
                case "Faible" -> "Votre niveau de stress est bien maîtrisé. Votre organisme répond efficacement aux sollicitations quotidiennes.";
                case "Moyen"  -> "Un stress modéré est détecté. Votre corps est sous pression mais reste capable de s'adapter.";
                default       -> "Niveau de stress important. Votre système nerveux est en état d'alerte prolongé, ce qui peut impacter votre santé.";
            };
            case "Anxiété" -> switch (niveau) {
                case "Faible" -> "Votre anxiété est à un niveau normal. Vous gérez bien l'incertitude et les situations nouvelles.";
                case "Moyen"  -> "Une anxiété modérée est présente. Des pensées intrusives peuvent affecter votre quotidien.";
                default       -> "Niveau d'anxiété élevé. Des manifestations physiques et cognitives sont probablement présentes.";
            };
            case "Dépression" -> switch (niveau) {
                case "Faible" -> "Votre humeur est globalement stable. Aucun signe significatif de dépression n'est détecté.";
                case "Moyen"  -> "Des symptômes dépressifs modérés sont identifiés : baisse de motivation ou perte d'intérêt possible.";
                default       -> "Symptômes dépressifs importants. Une prise en charge professionnelle est fortement recommandée.";
            };
            case "Bien-être" -> switch (niveau) {
                case "Faible" -> "Excellent niveau de bien-être ! Vous êtes épanoui(e) et en harmonie avec votre environnement.";
                case "Moyen"  -> "Bien-être satisfaisant mais perfectible. Quelques domaines de vie méritent attention.";
                default       -> "Votre bien-être global est dégradé. Un rééquilibrage de vos sphères de vie est nécessaire.";
            };
            default -> switch (niveau) { // Burnout
                case "Faible" -> "Pas de signe d'épuisement professionnel. Vous maintenez un bon équilibre travail-repos.";
                case "Moyen"  -> "Signes précoces de burnout détectés. Un rythme trop soutenu peut mener à l'épuisement.";
                default       -> "Burnout avancé détecté. Votre réservoir d'énergie est épuisé — une pause urgente est nécessaire.";
            };
        };
    }

    private String getRecommandation(String type, String niveau) {
        return switch (type) {
            case "Stress" -> switch (niveau) {
                case "Faible" -> "✅  Continuez vos bonnes habitudes ! Exercice régulier et sommeil de qualité.";
                case "Moyen"  -> "🧘  Pratiquez 15 min de respiration profonde chaque matin. Identifiez vos sources de stress principales.";
                default       -> "🚨  Consultez un professionnel. Cohérence cardiaque 3×/jour, réduction de la caféine, marche 30 min/jour.";
            };
            case "Anxiété" -> switch (niveau) {
                case "Faible" -> "✅  Maintenez vos habitudes. Un mindfulness de 10 min/jour renforcera votre sérénité.";
                case "Moyen"  -> "💬  Tenez un journal de vos pensées. La technique STOP (Stop, Respirer, Observer, Agir) peut vous aider.";
                default       -> "🚨  Une thérapie cognitivo-comportementale (TCC) est recommandée. Évitez les situations anxiogènes non essentielles.";
            };
            case "Dépression" -> switch (niveau) {
                case "Faible" -> "✅  Cultivez vos relations et activités plaisantes. L'activité physique protège la santé mentale.";
                case "Moyen"  -> "🌿  Exposez-vous à la lumière naturelle chaque jour. Fixez-vous de petits objectifs quotidiens.";
                default       -> "🚨  Consultez un médecin ou psychologue. Vous n'êtes pas seul(e) — l'aide fait une vraie différence.";
            };
            case "Bien-être" -> switch (niveau) {
                case "Faible" -> "✅  Partagez vos pratiques positives. Fixez-vous un nouveau défi personnel stimulant.";
                case "Moyen"  -> "🌱  Identifiez les 3 domaines de vie déficitaires et agissez en priorité sur le plus impactant.";
                default       -> "🔄  Bilan complet de vos besoins fondamentaux. Envisagez un accompagnement en développement personnel.";
            };
            default -> switch (niveau) { // Burnout
                case "Faible" -> "✅  Protégez votre équilibre. Apprenez à dire non aux demandes non essentielles.";
                case "Moyen"  -> "⏸️  Planifiez des pauses régulières. Déconnectez les écrans professionnels le soir et le week-end.";
                default       -> "🚨  Consultez votre médecin pour un arrêt si nécessaire. Le burnout non traité a des conséquences graves.";
            };
        };
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "📊";
        return switch (type) {
            case "Stress"     -> "🧘";
            case "Anxiété"    -> "😰";
            case "Dépression" -> "💙";
            case "Bien-être"  -> "🌿";
            case "Burnout"    -> "⚡";
            default           -> "📊";
        };
    }
}
