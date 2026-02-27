package controllers;

import entities.Evaluation;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvaluation;
import services.GamificationService;
import services.GamificationService.Badge;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import javafx.animation.ScaleTransition;
import javafx.scene.layout.GridPane;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.ArrayList;
import java.time.LocalDate;

public class ShowEvaluationController implements Initializable {

    // ── Palette ──
    private static final String C_CYAN    = "#00D9FF";
    private static final String C_GREEN   = "#00FF88";
    private static final String C_DARK    = "#050C07";
    private static final String C_CARD    = "#0D1F12";
    private static final String C_SURFACE = "#112016";
    private static final String C_TEXT    = "#E8FFF0";
    private static final String C_DANGER  = "#FF4D6D";
    private static final String C_WARN    = "#FFD700";

    // ── Animated background orbs ──
    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;

    // ── Search / Filters ──
    @FXML private TextField searchField;
    @FXML private CheckBox  filterStress;
    @FXML private CheckBox  filterAnxiete;
    @FXML private CheckBox  filterDepression;
    @FXML private CheckBox  filterBienetre;
    @FXML private CheckBox  filterBurnout;

    // ── Results ──
    @FXML private VBox  evaluationsContainer;
    @FXML private Label resultsCount;

    // ── Stats button ──
    @FXML private Button btnStats;

    // ── Badges button ──
    @FXML private Button btnBadges;

    private ServiceEvaluation  serviceEvaluation;
    private List<Evaluation>   allEvaluations;
    private GamificationService gamificationService;

    // ═══════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceEvaluation  = new ServiceEvaluation();
        gamificationService = new GamificationService();
        animateOrbs();
        loadEvaluations();
        // Rapport hebdomadaire automatique
        javafx.application.Platform.runLater(this::afficherRapportHebdomadaire);
    }

    private void animateOrbs() {
        animateOrb(orb1,  25, -15, Duration.seconds(14));
        animateOrb(orb2, -30,  20, Duration.seconds(17));
        animateOrb(orb3,  18, -12, Duration.seconds(20));
    }

    private void animateOrb(Circle orb, double dx, double dy, Duration dur) {
        if (orb == null) return;
        TranslateTransition t = new TranslateTransition(dur, orb);
        t.setByX(dx); t.setByY(dy);
        t.setCycleCount(Animation.INDEFINITE);
        t.setAutoReverse(true);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.play();
    }

    // ── Load data ────────────────────────────────────────────
    private void loadEvaluations() {
        try {
            allEvaluations = serviceEvaluation.recuperer();
            displayEvaluations(allEvaluations);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les évaluations : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Display cards ────────────────────────────────────────
    private void displayEvaluations(List<Evaluation> list) {
        evaluationsContainer.getChildren().clear();

        if (resultsCount != null) {
            resultsCount.setText(list.size() + " résultat" + (list.size() > 1 ? "s" : ""));
        }

        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 48px;");
            Label msg = new Label("Aucune évaluation disponible");
            msg.setStyle("-fx-text-fill: rgba(0,217,255,0.45); -fx-font-size: 16px;");
            empty.getChildren().addAll(icon, msg);
            evaluationsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            HBox card = buildEvaluationCard(list.get(i));
            evaluationsContainer.getChildren().add(card);

            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setDelay(Duration.millis(80L * i));
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
            tt.setDelay(Duration.millis(80L * i));
            tt.setFromY(20); tt.setToY(0); tt.play();
        }
    }

    private HBox buildEvaluationCard(Evaluation ev) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 28, 22, 28));
        card.setStyle(cardStyle(false));
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));

        Label icon = new Label(getTypeEmoji(ev.getTypeTest()));
        icon.setStyle("-fx-font-size: 36px;");

        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label typeLabel = new Label(ev.getTypeTest());
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label dateLabel = new Label("📅  " + (ev.getDateEvaluation() != null ? ev.getDateEvaluation().toString() : "—"));
        dateLabel.setStyle("-fx-text-fill: rgba(160,224,192,0.60); -fx-font-size: 13px;");

        // Hint cliquable
        Label hintLabel = new Label("📋  Cliquer pour voir le rapport");
        hintLabel.setStyle("-fx-text-fill: rgba(0,217,255,0.45); -fx-font-size: 11px; -fx-font-style: italic;");

        info.getChildren().addAll(typeLabel, dateLabel, hintLabel);

        VBox scoreBadge = buildScoreBadge(ev.getScore(), ev.getNiveau());

        // Bouton rapport
        Button btnRapport = new Button("📋  Rapport");
        btnRapport.setStyle(
            "-fx-background-color: rgba(0,217,255,0.12); " +
            "-fx-text-fill: #00D9FF; -fx-font-size: 12px; -fx-font-weight: bold; " +
            "-fx-border-color: rgba(0,217,255,0.40); -fx-border-width: 1; " +
            "-fx-border-radius: 16; -fx-background-radius: 16; " +
            "-fx-padding: 8 18; -fx-cursor: hand;"
        );
        btnRapport.setOnMouseEntered(e -> btnRapport.setStyle(
            "-fx-background-color: rgba(0,217,255,0.25); " +
            "-fx-text-fill: #00D9FF; -fx-font-size: 12px; -fx-font-weight: bold; " +
            "-fx-border-color: #00D9FF; -fx-border-width: 1.5; " +
            "-fx-border-radius: 16; -fx-background-radius: 16; " +
            "-fx-padding: 8 18; -fx-cursor: hand;"
        ));
        btnRapport.setOnMouseExited(e -> btnRapport.setStyle(
            "-fx-background-color: rgba(0,217,255,0.12); " +
            "-fx-text-fill: #00D9FF; -fx-font-size: 12px; -fx-font-weight: bold; " +
            "-fx-border-color: rgba(0,217,255,0.40); -fx-border-width: 1; " +
            "-fx-border-radius: 16; -fx-background-radius: 16; " +
            "-fx-padding: 8 18; -fx-cursor: hand;"
        ));
        btnRapport.setOnAction(e -> showRapportDialog(ev));

        // Clic sur toute la carte aussi
        card.setOnMouseClicked(e -> showRapportDialog(ev));

        card.getChildren().addAll(icon, info, scoreBadge, btnRapport);
        return card;
    }

    private String cardStyle(boolean hovered) {
        if (hovered) return "-fx-background-color: linear-gradient(to right, rgba(19,36,24,0.95), rgba(19,36,24,0.70));" +
                           "-fx-border-color: #00D9FF; -fx-border-width: 1.5; -fx-border-radius: 18; -fx-background-radius: 18;" +
                           "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,217,255,0.30), 22, 0.3, 0, 6);";
        return "-fx-background-color: linear-gradient(to right, rgba(19,36,24,0.80), rgba(19,36,24,0.50));" +
               "-fx-border-color: rgba(0,217,255,0.18); -fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18;" +
               "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0.3, 0, 4);";
    }

    private VBox buildScoreBadge(int score, String niveau) {
        String color, emoji;
        switch (niveau) {
            case "Faible" -> { color = "#00FF88"; emoji = "✅"; }
            case "Moyen"  -> { color = "#FFD700"; emoji = "⚠️"; }
            default       -> { color = "#FF4D6D"; emoji = "🔴"; }
        }

        VBox badge = new VBox(4);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(12, 20, 12, 20));
        badge.setStyle("-fx-background-color: rgba(19,36,24,0.80); -fx-border-color: " + color + "55;" +
                       "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");

        Label emojiLbl  = new Label(emoji);     emojiLbl.setStyle("-fx-font-size: 20px;");
        Label scoreLbl  = new Label(String.valueOf(score));
        scoreLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 26px; -fx-font-weight: bold;");
        Label niveauLbl = new Label(niveau);
        niveauLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        badge.getChildren().addAll(emojiLbl, scoreLbl, niveauLbl);
        return badge;
    }


    // ═══════════════════════════════════════════════════════
    //  RAPPORT PERSONNALISÉ PAR ÉVALUATION
    // ═══════════════════════════════════════════════════════
    private void showRapportDialog(Evaluation ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📋  Rapport — " + ev.getTypeTest());

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(540);
        pane.setStyle(
            "-fx-background-color: #071A10; " +
            "-fx-border-color: #00D9FF; " +
            "-fx-border-width: 2; -fx-border-radius: 22; -fx-background-radius: 22;"
        );
        pane.getButtonTypes().add(new ButtonType("✓  Fermer", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(18);
        content.setPadding(new Insets(28));

        // ── En-tête ──────────────────────────────────────────
        HBox headerRow = new HBox(14);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(getTypeEmoji(ev.getTypeTest()));
        iconLbl.setStyle("-fx-font-size: 40px;");

        VBox headerText = new VBox(4);
        Label titleLbl = new Label("Rapport d'évaluation");
        titleLbl.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label typeLbl = new Label(ev.getTypeTest()
            + "  •  " + (ev.getDateEvaluation() != null ? ev.getDateEvaluation().toString() : "—"));
        typeLbl.setStyle("-fx-text-fill: rgba(160,224,192,0.60); -fx-font-size: 13px;");
        headerText.getChildren().addAll(titleLbl, typeLbl);
        headerRow.getChildren().addAll(iconLbl, headerText);

        content.getChildren().addAll(headerRow, makeLine());

        // ── Score + Niveau côte à côte ────────────────────────
        String scoreColor = ev.getScore() <= 20 ? "#00FF88" : ev.getScore() <= 50 ? "#FFD700" : "#FF4D6D";
        String niveauColor;
        String niveauEmoji;
        switch (ev.getNiveau()) {
            case "Faible" -> { niveauColor = "#00FF88"; niveauEmoji = "✅"; }
            case "Moyen"  -> { niveauColor = "#FFD700"; niveauEmoji = "⚠️"; }
            default       -> { niveauColor = "#FF4D6D"; niveauEmoji = "🔴"; }
        }

        HBox scoreRow = new HBox(30);
        scoreRow.setAlignment(Pos.CENTER);

        // Cercle score
        StackPane scoreCircle = new StackPane();
        Circle bg = new Circle(46);
        bg.setFill(Color.web("#112016"));
        bg.setStroke(Color.web(scoreColor, 0.85));
        bg.setStrokeWidth(3);
        Label scoreNumLbl = new Label(String.valueOf(ev.getScore()));
        scoreNumLbl.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        scoreCircle.getChildren().addAll(bg, scoreNumLbl);

        // Pulse
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), scoreCircle);
        pulse.setFromX(1.0); pulse.setToX(1.05);
        pulse.setFromY(1.0); pulse.setToY(1.05);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        VBox scoreBox = new VBox(8, scoreCircle, new Label("Score") {{
            setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 11px;");
        }});
        scoreBox.setAlignment(Pos.CENTER);

        // Badge niveau
        Label emojiNiveauLbl = new Label(niveauEmoji);
        emojiNiveauLbl.setStyle("-fx-font-size: 34px;");
        Label niveauTextLbl = new Label(ev.getNiveau());
        niveauTextLbl.setStyle(
            "-fx-text-fill: " + niveauColor + "; -fx-font-size: 17px; -fx-font-weight: bold; " +
            "-fx-border-color: " + niveauColor + "; -fx-border-width: 1.5; -fx-border-radius: 18; " +
            "-fx-background-color: " + niveauColor + "22; -fx-background-radius: 18; -fx-padding: 5 18;"
        );
        VBox niveauBox = new VBox(8, emojiNiveauLbl, niveauTextLbl, new Label("Niveau") {{
            setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 11px;");
        }});
        niveauBox.setAlignment(Pos.CENTER);

        scoreRow.getChildren().addAll(scoreBox, niveauBox);
        content.getChildren().add(scoreRow);
        content.getChildren().add(makeLine());

        // ── Interprétation ───────────────────────────────────
        Label interpTitle = new Label("🔍  Interprétation");
        interpTitle.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label interpText = new Label(getInterpretation(ev.getTypeTest(), ev.getNiveau()));
        interpText.setWrapText(true);
        interpText.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px; -fx-line-spacing: 4;");

        content.getChildren().addAll(interpTitle, interpText);
        content.getChildren().add(makeLine());

        // ── Recommandation ───────────────────────────────────
        Label recommTitle = new Label("💡  Recommandation");
        recommTitle.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label recommText = new Label(getRecommandation(ev.getTypeTest(), ev.getNiveau()));
        recommText.setWrapText(true);
        recommText.setStyle(
            "-fx-text-fill: #E8FFF0; -fx-font-size: 13px; -fx-line-spacing: 4; " +
            "-fx-background-color: rgba(0,255,136,0.07); " +
            "-fx-padding: 14 18; -fx-background-radius: 14;"
        );

        content.getChildren().addAll(recommTitle, recommText);

        // ── Progression vs précédente ────────────────────────
        Evaluation precedente = getPrecedenteEvaluation(ev);
        if (precedente != null && precedente != ev) {
            content.getChildren().add(makeLine());
            content.getChildren().add(buildProgressionBlock(ev, precedente));
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setContent(scroll);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88); " +
            "-fx-text-fill: #050C07; -fx-font-weight: bold; " +
            "-fx-padding: 10 32; -fx-background-radius: 22; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    // ── Évaluation précédente du même type ───────────────────
    private Evaluation getPrecedenteEvaluation(Evaluation actuelle) {
        if (allEvaluations == null) return null;
        return allEvaluations.stream()
            .filter(e -> e != actuelle
                && actuelle.getTypeTest() != null
                && actuelle.getTypeTest().equals(e.getTypeTest())
                && e.getDateEvaluation() != null)
            .max(Comparator.comparing(e -> e.getDateEvaluation().toLocalDate()))
            .orElse(null);
    }

    // ── Bloc progression ─────────────────────────────────────
    private VBox buildProgressionBlock(Evaluation actuelle, Evaluation precedente) {
        int diff = actuelle.getScore() - precedente.getScore();
        String icon, message, color;
        if (diff < 0) {
            icon = "📈"; color = "#00FF88";
            message = "Amélioration — score passé de " + precedente.getScore() + " à " + actuelle.getScore() + " (" + diff + " pts)";
        } else if (diff > 0) {
            icon = "📉"; color = "#FF4D6D";
            message = "Aggravation — score passé de " + precedente.getScore() + " à " + actuelle.getScore() + " (+" + diff + " pts)";
        } else {
            icon = "➡️"; color = "#FFD700";
            message = "Stable — score identique à l\'évaluation précédente (" + actuelle.getScore() + " pts)";
        }

        Label title = new Label("📊  Suivi de progression");
        title.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Barres visuelles
        HBox bars = new HBox(24);
        bars.setAlignment(Pos.CENTER_LEFT);

        VBox prevBar = buildMiniBar("Avant", precedente.getScore(), "rgba(200,255,220,0.30)");
        VBox nowBar  = buildMiniBar("Maintenant", actuelle.getScore(), color);
        bars.getChildren().addAll(prevBar, nowBar);

        Label progLbl = new Label(icon + "  " + message);
        progLbl.setWrapText(true);
        progLbl.setStyle(
            "-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-background-color: " + color + "15; -fx-padding: 12 16; -fx-background-radius: 12; " +
            "-fx-border-color: " + color + "44; -fx-border-width: 1; -fx-border-radius: 12;"
        );

        Label prevInfo = new Label("Évaluation précédente : " + precedente.getNiveau()
            + "  •  " + (precedente.getDateEvaluation() != null ? precedente.getDateEvaluation().toString() : "—"));
        prevInfo.setStyle("-fx-text-fill: rgba(200,255,220,0.40); -fx-font-size: 11px;");

        VBox box = new VBox(10, title, bars, progLbl, prevInfo);
        box.setStyle(
            "-fx-background-color: rgba(0,217,255,0.04); " +
            "-fx-border-color: rgba(0,217,255,0.15); " +
            "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
        );
        return box;
    }

    private VBox buildMiniBar(String label, int score, String color) {
        double width = (score / 100.0) * 180;

        Label labelLbl = new Label(label + " : " + score);
        labelLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label bar = new Label();
        bar.setPrefHeight(8);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        bar.setPrefWidth(0);

        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bar.prefWidthProperty(), 0)),
            new KeyFrame(Duration.millis(700), new KeyValue(bar.prefWidthProperty(), width, Interpolator.EASE_OUT))
        );
        anim.setDelay(Duration.millis(200));
        anim.play();

        VBox box = new VBox(4, labelLbl, bar);
        return box;
    }

    // ── Interprétations ──────────────────────────────────────
    private String getInterpretation(String type, String niveau) {
        if (type == null) return "Évaluation complétée.";
        return switch (type) {
            case "Stress" -> switch (niveau) {
                case "Faible"  -> "Votre niveau de stress est bien maîtrisé. Votre organisme répond efficacement aux sollicitations quotidiennes.";
                case "Moyen"   -> "Un stress modéré est détecté. Votre corps est sous pression mais reste capable de s\'adapter.";
                default        -> "Niveau de stress important détecté. Votre système nerveux est en état d\'alerte prolongé.";
            };
            case "Anxiété" -> switch (niveau) {
                case "Faible"  -> "Votre anxiété est à un niveau normal. Vous gérez bien l\'incertitude et les situations nouvelles.";
                case "Moyen"   -> "Une anxiété modérée est présente. Des pensées intrusives peuvent affecter votre quotidien.";
                default        -> "Niveau d\'anxiété élevé. Des manifestations physiques et cognitives sont probablement présentes.";
            };
            case "Dépression" -> switch (niveau) {
                case "Faible"  -> "Votre humeur est globalement stable. Aucun signe significatif de dépression n\'est détecté.";
                case "Moyen"   -> "Des symptômes dépressifs modérés sont identifiés : baisse de motivation, fatigue possible.";
                default        -> "Symptômes dépressifs importants. Une prise en charge professionnelle est recommandée.";
            };
            case "Bien-être" -> switch (niveau) {
                case "Faible"  -> "Excellent niveau de bien-être ! Vous êtes épanoui(e) et en harmonie avec votre environnement.";
                case "Moyen"   -> "Bien-être satisfaisant mais perfectible. Quelques domaines de vie méritent attention.";
                default        -> "Votre bien-être global est dégradé. Un rééquilibrage de vos sphères de vie est nécessaire.";
            };
            case "Burnout" -> switch (niveau) {
                case "Faible"  -> "Pas de signe d\'épuisement professionnel. Vous maintenez un bon équilibre travail-repos.";
                case "Moyen"   -> "Signes précoces de burnout détectés. Un rythme trop soutenu peut mener à l\'épuisement.";
                default        -> "Burnout avancé détecté. Votre réservoir d\'énergie est épuisé, une pause urgente est nécessaire.";
            };
            default -> "Évaluation complétée. Consultez votre praticien pour une interprétation personnalisée.";
        };
    }

    private String getRecommandation(String type, String niveau) {
        if (type == null) return "Consultez un praticien NEUROWELL pour un programme personnalisé.";
        return switch (type) {
            case "Stress" -> switch (niveau) {
                case "Faible"  -> "✅  Continuez vos bonnes habitudes ! Exercice régulier et sommeil de qualité.";
                case "Moyen"   -> "🧘  Pratiquez 15 min de respiration profonde chaque matin. Identifiez vos sources de stress.";
                default        -> "🚨  Consultez un professionnel. En attendant : cohérence cardiaque 3×/jour, marche 30 min.";
            };
            case "Anxiété" -> switch (niveau) {
                case "Faible"  -> "✅  Maintenez vos habitudes. Un mindfulness de 10 min/jour renforcera votre sérénité.";
                case "Moyen"   -> "💬  Tenez un journal de vos pensées. La technique STOP peut vous aider au quotidien.";
                default        -> "🚨  Une thérapie cognitivo-comportementale (TCC) est recommandée. Évitez les situations anxiogènes.";
            };
            case "Dépression" -> switch (niveau) {
                case "Faible"  -> "✅  Cultivez vos relations et vos activités plaisantes. L\'activité physique protège la santé mentale.";
                case "Moyen"   -> "🌿  Exposez-vous à la lumière naturelle chaque jour. Fixez-vous de petits objectifs quotidiens.";
                default        -> "🚨  Consultez un médecin ou un psychologue. Vous n\'êtes pas seul(e) — l\'aide fait une vraie différence.";
            };
            case "Bien-être" -> switch (niveau) {
                case "Faible"  -> "✅  Partagez vos pratiques positives. Fixez-vous un nouveau défi personnel stimulant.";
                case "Moyen"   -> "🌱  Identifiez les 3 domaines de vie déficitaires et agissez en priorité sur le plus impactant.";
                default        -> "🔄  Bilan complet de vos besoins fondamentaux. Envisagez un accompagnement en développement personnel.";
            };
            case "Burnout" -> switch (niveau) {
                case "Faible"  -> "✅  Protégez votre équilibre. Apprenez à dire non aux demandes non essentielles.";
                case "Moyen"   -> "⏸️  Planifiez des pauses régulières. Déconnectez les écrans professionnels le soir.";
                default        -> "🚨  Consultez votre médecin pour un arrêt si nécessaire. Le burnout non traité a des conséquences graves.";
            };
            default -> "💡  Consultez un praticien NEUROWELL pour un programme personnalisé adapté à vos résultats.";
        };
    }

    // ═══════════════════════════════════════════════════════
    //  STATISTIQUES VISUELLES  — Idée 1 (version Front)
    // ═══════════════════════════════════════════════════════
    @FXML
    private void handleShowStats() {
        if (allEvaluations == null || allEvaluations.isEmpty()) {
            showAlert("Info", "Aucune évaluation disponible pour afficher les statistiques.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📊  Statistiques des évaluations");

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefSize(860, 700);
        pane.setStyle(
            "-fx-background-color: #071A10; " +
            "-fx-border-color: #00D9FF; " +
            "-fx-border-width: 2; -fx-border-radius: 22; -fx-background-radius: 22;"
        );
        pane.getButtonTypes().add(new ButtonType("✓  Fermer", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(20);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color: transparent;");

        // ── En-tête ──────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label("📊  Tableau de bord statistique");
        titleLbl.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 22px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLbl = new Label(allEvaluations.size() + " évaluations analysées");
        countLbl.setStyle(
            "-fx-text-fill: #00D9FF; -fx-font-size: 12px; " +
            "-fx-background-color: rgba(0,217,255,0.10); " +
            "-fx-border-color: rgba(0,217,255,0.30); -fx-border-width: 1; " +
            "-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 14;"
        );
        header.getChildren().addAll(titleLbl, spacer, countLbl);
        content.getChildren().add(header);
        content.getChildren().add(makeLine());

        // ── KPI Row ──────────────────────────────────────────
        content.getChildren().add(buildKpiRow(allEvaluations));
        content.getChildren().add(makeLine());

        // ── Charts Row 1 : PieChart + BarChart ───────────────
        HBox row1 = new HBox(16);
        VBox pieBox = buildPieChartBox(allEvaluations);
        VBox barBox = buildBarChartBox(allEvaluations);
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        HBox.setHgrow(barBox, Priority.ALWAYS);
        row1.getChildren().addAll(pieBox, barBox);
        content.getChildren().add(row1);

        // ── Chart Row 2 : LineChart ───────────────────────────
        content.getChildren().add(buildLineChartBox(allEvaluations));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setContent(scroll);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88); " +
            "-fx-text-fill: #050C07; -fx-font-weight: bold; " +
            "-fx-padding: 10 32; -fx-background-radius: 22; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    // ── KPI Cards ────────────────────────────────────────────
    private HBox buildKpiRow(List<Evaluation> data) {
        double avg     = data.stream().mapToInt(Evaluation::getScore).average().orElse(0);
        int    max     = data.stream().mapToInt(Evaluation::getScore).max().orElse(0);
        int    min     = data.stream().mapToInt(Evaluation::getScore).min().orElse(0);
        long   elevated = data.stream().filter(e -> "Élevé".equals(e.getNiveau())).count();

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
            buildKpiCard("📊", "Moyenne",  String.format("%.1f", avg), C_CYAN),
            buildKpiCard("🔺", "Maximum",  String.valueOf(max),         C_DANGER),
            buildKpiCard("🔻", "Minimum",  String.valueOf(min),         C_GREEN),
            buildKpiCard("⚠️", "Niveaux élevés", String.valueOf(elevated), C_WARN)
        );
        return row;
    }

    private VBox buildKpiCard(String emoji, String label, String value, String color) {
        Label emojiLbl = new Label(emoji);  emojiLbl.setStyle("-fx-font-size: 22px;");
        Label valLbl   = new Label(value);
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label nameLbl  = new Label(label);
        nameLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 11px;");

        VBox card = new VBox(4, emojiLbl, valLbl, nameLbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 28, 16, 28));
        card.setStyle(
            "-fx-background-color: #0D1F12; -fx-border-color: " + color + "44; " +
            "-fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;"
        );
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #112016; -fx-border-color: " + color + "; " +
            "-fx-border-width: 1.5; -fx-border-radius: 16; -fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, " + color + "44, 18, 0.2, 0, 0);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: #0D1F12; -fx-border-color: " + color + "44; " +
            "-fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;"
        ));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── 1. PieChart ──────────────────────────────────────────
    private VBox buildPieChartBox(List<Evaluation> data) {
        Map<String, Long> countByType = data.stream()
            .collect(Collectors.groupingBy(
                e -> e.getTypeTest() != null ? e.getTypeTest() : "Autre",
                Collectors.counting()
            ));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        countByType.forEach((type, count) ->
            pieData.add(new PieChart.Data(getTypeEmoji(type) + " " + type + " (" + count + ")", count))
        );

        PieChart pie = new PieChart(pieData);
        pie.setLegendVisible(true);
        pie.setLabelsVisible(true);
        pie.setPrefSize(360, 280);
        pie.setStyle("-fx-background-color: transparent;");

        String[] pieColors = {C_CYAN, C_GREEN, C_WARN, C_DANGER, "#A78BFA"};
        for (int i = 0; i < pieData.size(); i++) {
            final int idx = i;
            pieData.get(i).nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + pieColors[idx % pieColors.length] + ";");
                    node.setOnMouseEntered(e -> {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
                        st.setToX(1.08); st.setToY(1.08); st.play();
                    });
                    node.setOnMouseExited(e -> {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
                        st.setToX(1.0); st.setToY(1.0); st.play();
                    });
                }
            });
        }

        Label title = chartTitle("🥧  Répartition par type de test");
        return styledChartBox(title, pie);
    }

    // ── 2. BarChart ──────────────────────────────────────────
    private VBox buildBarChartBox(List<Evaluation> data) {
        Map<String, Long> countByNiveau = data.stream()
            .collect(Collectors.groupingBy(
                e -> e.getNiveau() != null ? e.getNiveau() : "Inconnu",
                Collectors.counting()
            ));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.60); -fx-font-size: 10px;");
        yAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.60); -fx-font-size: 10px;");
        yAxis.setLabel("Nombre");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setPrefSize(360, 280);
        bar.setStyle("-fx-background-color: transparent;");
        bar.setBarGap(4);
        bar.setCategoryGap(20);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] niveaux = {"Faible", "Moyen", "Élevé"};
        String[] colors  = {C_GREEN, C_WARN, C_DANGER};

        for (int i = 0; i < niveaux.length; i++) {
            final int    idx      = i;
            final long   val      = countByNiveau.getOrDefault(niveaux[i], 0L);
            final String col      = colors[i];
            final String niveauLbl = niveaux[i];

            XYChart.Data<String, Number> d = new XYChart.Data<>(niveaux[i], val);
            series.getData().add(d);

            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + col + ";");
                    node.setScaleY(0);
                    Timeline anim = new Timeline(
                        new KeyFrame(Duration.ZERO,
                            new KeyValue(node.scaleYProperty(), 0)),
                        new KeyFrame(Duration.millis(600 + 150L * idx),
                            new KeyValue(node.scaleYProperty(), 1, Interpolator.EASE_OUT))
                    );
                    anim.play();
                    Tooltip.install(node, new Tooltip(niveauLbl + " : " + val));
                }
            });
        }

        bar.getData().add(series);
        Label title = chartTitle("📊  Distribution des niveaux");
        return styledChartBox(title, bar);
    }

    // ── 3. LineChart ──────────────────────────────────────────
    private VBox buildLineChartBox(List<Evaluation> data) {
        List<Evaluation> sorted = data.stream()
            .filter(e -> e.getDateEvaluation() != null)
            .sorted(Comparator.comparing(e -> e.getDateEvaluation().toLocalDate()))
            .collect(Collectors.toList());

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis(0, 100, 10);
        xAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.55); -fx-font-size: 9px;");
        yAxis.setStyle("-fx-tick-label-fill: rgba(200,255,220,0.55); -fx-font-size: 9px;");
        yAxis.setLabel("Score");

        LineChart<String, Number> line = new LineChart<>(xAxis, yAxis);
        line.setLegendVisible(true);
        line.setPrefHeight(230);
        line.setCreateSymbols(true);
        line.setStyle("-fx-background-color: transparent;");

        Map<String, List<Evaluation>> byType = sorted.stream()
            .collect(Collectors.groupingBy(e -> e.getTypeTest() != null ? e.getTypeTest() : "Autre"));

        String[] lineColors = {C_CYAN, C_GREEN, C_WARN, C_DANGER, "#A78BFA"};
        int colorIdx = 0;

        for (Map.Entry<String, List<Evaluation>> entry : byType.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            final String col = lineColors[colorIdx % lineColors.length];

            for (Evaluation ev : entry.getValue()) {
                String dateLabel = ev.getDateEvaluation().toLocalDate().toString();
                XYChart.Data<String, Number> point = new XYChart.Data<>(dateLabel, ev.getScore());
                series.getData().add(point);
                point.nodeProperty().addListener((obs, old, node) -> {
                    if (node != null) {
                        node.setStyle("-fx-background-color: " + col + ", white;");
                        Tooltip.install(node, new Tooltip(
                            entry.getKey() + "\nScore : " + ev.getScore()
                            + "\nNiveau : " + ev.getNiveau()
                            + "\nDate : " + dateLabel
                        ));
                    }
                });
            }

            line.getData().add(series);

            series.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    Node lineNode = node.lookup(".chart-series-line");
                    if (lineNode != null)
                        lineNode.setStyle("-fx-stroke: " + col + "; -fx-stroke-width: 2.5px;");
                }
            });
            colorIdx++;
        }

        Label title = chartTitle("📈  Évolution du score dans le temps");
        return styledChartBox(title, line);
    }

    // ── Helpers ──────────────────────────────────────────────
    private Label chartTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + C_CYAN + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox styledChartBox(Label title, Node chart) {
        VBox box = new VBox(10, title, chart);
        box.setPadding(new Insets(16));
        box.setStyle(
            "-fx-background-color: #0D1F12; " +
            "-fx-border-color: rgba(0,217,255,0.15); " +
            "-fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18;"
        );
        VBox.setVgrow(chart, Priority.ALWAYS);
        return box;
    }

    private Line makeLine() {
        Line line = new Line(0, 0, 780, 0);
        line.setStroke(Color.web(C_CYAN, 0.15));
        line.setStrokeWidth(1);
        return line;
    }

    // ═══════════════════════════════════════════════════════
    //  SEARCH & FILTER
    // ═══════════════════════════════════════════════════════
    @FXML private void handleSearch()  { applyFilters(); }

    @FXML
    private void resetFilters() {
        if (searchField      != null) searchField.clear();
        if (filterStress     != null) filterStress.setSelected(false);
        if (filterAnxiete    != null) filterAnxiete.setSelected(false);
        if (filterDepression != null) filterDepression.setSelected(false);
        if (filterBienetre   != null) filterBienetre.setSelected(false);
        if (filterBurnout    != null) filterBurnout.setSelected(false);
        displayEvaluations(allEvaluations);
    }

    private void applyFilters() {
        if (allEvaluations == null) return;
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        boolean anyTypeChecked = (filterStress     != null && filterStress.isSelected())
                              || (filterAnxiete    != null && filterAnxiete.isSelected())
                              || (filterDepression != null && filterDepression.isSelected())
                              || (filterBienetre   != null && filterBienetre.isSelected())
                              || (filterBurnout    != null && filterBurnout.isSelected());

        List<Evaluation> filtered = allEvaluations.stream()
            .filter(ev -> {
                if (!query.isEmpty() && ev.getTypeTest() != null
                        && !ev.getTypeTest().toLowerCase().contains(query)) return false;
                if (anyTypeChecked) {
                    String t = ev.getTypeTest();
                    if (filterStress     != null && filterStress.isSelected()     && "Stress".equals(t))     return true;
                    if (filterAnxiete    != null && filterAnxiete.isSelected()    && "Anxiété".equals(t))    return true;
                    if (filterDepression != null && filterDepression.isSelected() && "Dépression".equals(t)) return true;
                    if (filterBienetre   != null && filterBienetre.isSelected()   && "Bien-être".equals(t))  return true;
                    if (filterBurnout    != null && filterBurnout.isSelected()    && "Burnout".equals(t))    return true;
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        displayEvaluations(filtered);
    }

    // ═══════════════════════════════════════════════════════
    //  RAPPORT HEBDOMADAIRE AUTOMATIQUE
    // ═══════════════════════════════════════════════════════
    private void afficherRapportHebdomadaire() {
        if (allEvaluations == null || allEvaluations.isEmpty()) return;

        // Semaine courante : lundi → dimanche
        java.time.LocalDate today    = java.time.LocalDate.now();
        java.time.LocalDate lundi    = today.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate dimanche = today.with(java.time.DayOfWeek.SUNDAY);

        // Semaine précédente
        java.time.LocalDate lundiPrec    = lundi.minusWeeks(1);
        java.time.LocalDate dimanchePrec = dimanche.minusWeeks(1);

        // Filtrer évaluations semaine courante
        List<Evaluation> semaineCourante = allEvaluations.stream()
            .filter(e -> e.getDateEvaluation() != null)
            .filter(e -> {
                java.time.LocalDate d = e.getDateEvaluation().toLocalDate();
                return !d.isBefore(lundi) && !d.isAfter(dimanche);
            })
            .collect(java.util.stream.Collectors.toList());

        if (semaineCourante.isEmpty()) return; // Rien cette semaine → pas de rapport

        // Filtrer semaine précédente
        List<Evaluation> semainePrecedente = allEvaluations.stream()
            .filter(e -> e.getDateEvaluation() != null)
            .filter(e -> {
                java.time.LocalDate d = e.getDateEvaluation().toLocalDate();
                return !d.isBefore(lundiPrec) && !d.isAfter(dimanchePrec);
            })
            .collect(java.util.stream.Collectors.toList());

        // Stats semaine courante
        int nbEvals      = semaineCourante.size();
        double scoreMoyen = semaineCourante.stream().mapToInt(Evaluation::getScore).average().orElse(0);
        int scoreMax     = semaineCourante.stream().mapToInt(Evaluation::getScore).max().orElse(0);
        int scoreMin     = semaineCourante.stream().mapToInt(Evaluation::getScore).min().orElse(0);

        // Type dominant
        String typeDominant = semaineCourante.stream()
            .collect(java.util.stream.Collectors.groupingBy(Evaluation::getTypeTest, java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse("—");

        // Tendance vs semaine précédente
        String tendance, tendanceCouleur, tendanceIcon;
        if (semainePrecedente.isEmpty()) {
            tendance = "Première semaine de suivi"; tendanceCouleur = "#00D9FF"; tendanceIcon = "🆕";
        } else {
            double scorePrecMoyen = semainePrecedente.stream().mapToInt(Evaluation::getScore).average().orElse(0);
            double diff = scoreMoyen - scorePrecMoyen;
            if (diff < -5)      { tendance = "Amélioration";  tendanceCouleur = "#00FF88"; tendanceIcon = "📈"; }
            else if (diff > 5)  { tendance = "Dégradation";   tendanceCouleur = "#FF4D6D"; tendanceIcon = "📉"; }
            else                { tendance = "Stable";         tendanceCouleur = "#FFD700"; tendanceIcon = "➡️"; }
        }

        // Niveau dominant
        long nbEleve  = semaineCourante.stream().filter(e -> "Élevé".equals(e.getNiveau())).count();
        long nbMoyen  = semaineCourante.stream().filter(e -> "Moyen".equals(e.getNiveau())).count();
        long nbFaible = semaineCourante.stream().filter(e -> "Faible".equals(e.getNiveau())).count();

        // Conseil de la semaine
        String conseil = getConseilSemaine(typeDominant, (int) Math.round(scoreMoyen));

        // ── Construire le dialog ──────────────────────────────
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📅  Rapport de la semaine");

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(540);
        pane.setStyle(
            "-fx-background-color: #071A10;" +
            "-fx-border-color: #00D9FF; -fx-border-width: 2;" +
            "-fx-border-radius: 22; -fx-background-radius: 22;"
        );
        pane.getButtonTypes().add(new ButtonType("✓  Fermer", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(16);
        content.setPadding(new Insets(26));

        // En-tête
        VBox header = new VBox(6);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label titleLbl = new Label("📅  Résumé de votre semaine");
        titleLbl.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label dateLbl = new Label("Du " + lundi + " au " + dimanche);
        dateLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 12px;");
        header.getChildren().addAll(titleLbl, dateLbl);
        content.getChildren().add(header);
        content.getChildren().add(makeLine());

        // KPIs — 4 cartes
        HBox kpis = new HBox(10);
        kpis.setAlignment(javafx.geometry.Pos.CENTER);
        kpis.getChildren().addAll(
            makeKpiCard("📊", String.valueOf(nbEvals),   "Évaluations",   "#00D9FF"),
            makeKpiCard("⭐", String.format("%.0f", scoreMoyen), "Score moyen", tendanceCouleur),
            makeKpiCard("🔼", String.valueOf(scoreMax),  "Meilleur score","#00FF88"),
            makeKpiCard("🔽", String.valueOf(scoreMin),  "Score le plus bas", "#FF4D6D")
        );
        content.getChildren().add(kpis);
        content.getChildren().add(makeLine());

        // Type dominant + tendance
        HBox middleRow = new HBox(12);
        middleRow.setAlignment(javafx.geometry.Pos.CENTER);

        VBox typeBox = new VBox(6);
        typeBox.setAlignment(javafx.geometry.Pos.CENTER);
        typeBox.setPadding(new Insets(14));
        typeBox.setStyle("-fx-background-color: rgba(0,217,255,0.08); -fx-border-color: rgba(0,217,255,0.25); -fx-border-width:1; -fx-border-radius:14; -fx-background-radius:14;");
        typeBox.setPrefWidth(220);
        Label typeTitle = new Label("Type dominant");
        typeTitle.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 11px;");
        Label typeVal = new Label(getTypeEmoji(typeDominant) + "  " + typeDominant);
        typeVal.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 16px; -fx-font-weight: bold;");
        typeBox.getChildren().addAll(typeTitle, typeVal);

        VBox tendBox = new VBox(6);
        tendBox.setAlignment(javafx.geometry.Pos.CENTER);
        tendBox.setPadding(new Insets(14));
        tendBox.setStyle("-fx-background-color: " + tendanceCouleur + "12; -fx-border-color: " + tendanceCouleur + "40; -fx-border-width:1; -fx-border-radius:14; -fx-background-radius:14;");
        tendBox.setPrefWidth(220);
        Label tendTitle = new Label("Tendance vs semaine passée");
        tendTitle.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 11px;");
        Label tendVal = new Label(tendanceIcon + "  " + tendance);
        tendVal.setStyle("-fx-text-fill: " + tendanceCouleur + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        tendBox.getChildren().addAll(tendTitle, tendVal);

        middleRow.getChildren().addAll(typeBox, tendBox);
        content.getChildren().add(middleRow);
        content.getChildren().add(makeLine());

        // Répartition niveaux
        Label repartTitle = new Label("Répartition des niveaux cette semaine");
        repartTitle.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 12px; -fx-font-weight: bold;");
        content.getChildren().add(repartTitle);

        content.getChildren().add(buildNiveauBar("✅  Faible", (int) nbFaible, nbEvals, "#00FF88"));
        content.getChildren().add(buildNiveauBar("⚠️  Moyen",  (int) nbMoyen,  nbEvals, "#FFD700"));
        content.getChildren().add(buildNiveauBar("🔴  Élevé",  (int) nbEleve,  nbEvals, "#FF4D6D"));

        content.getChildren().add(makeLine());

        // Conseil de la semaine
        VBox conseilBox = new VBox(8);
        conseilBox.setPadding(new Insets(14, 16, 14, 16));
        conseilBox.setStyle(
            "-fx-background-color: rgba(0,255,136,0.07);" +
            "-fx-border-color: rgba(0,255,136,0.25); -fx-border-width:1;" +
            "-fx-border-radius:14; -fx-background-radius:14;"
        );
        Label conseilTitle = new Label("💡  Conseil de la semaine");
        conseilTitle.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label conseilText = new Label(conseil);
        conseilText.setWrapText(true);
        conseilText.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 12px; -fx-line-spacing: 3;");
        conseilBox.getChildren().addAll(conseilTitle, conseilText);
        content.getChildren().add(conseilBox);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        pane.setContent(scroll);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right,#00D9FF,#00FF88);" +
            "-fx-text-fill: #050C07; -fx-font-weight: bold;" +
            "-fx-padding: 10 32; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    private VBox makeKpiCard(String emoji, String value, String label, String color) {
        VBox card = new VBox(6);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPadding(new Insets(14, 10, 14, 10));
        card.setPrefWidth(110);
        card.setStyle(
            "-fx-background-color: " + color + "12;" +
            "-fx-border-color: " + color + "40; -fx-border-width:1;" +
            "-fx-border-radius:14; -fx-background-radius:14;"
        );
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 22px;");
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label lblLbl = new Label(label);
        lblLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 10px;");
        lblLbl.setWrapText(true);
        lblLbl.setMaxWidth(100);
        lblLbl.setAlignment(javafx.geometry.Pos.CENTER);
        card.getChildren().addAll(emojiLbl, valLbl, lblLbl);
        return card;
    }

    private HBox buildNiveauBar(String label, int count, int total, String color) {
        Label nameLbl = new Label(label + "  (" + count + ")");
        nameLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLbl.setMinWidth(120);

        Pane track = new Pane();
        track.setPrefHeight(8);
        track.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(track, javafx.scene.layout.Priority.ALWAYS);
        track.setStyle("-fx-background-color: rgba(0,217,255,0.10); -fx-background-radius: 4;");

        Pane fill = new Pane();
        fill.setPrefHeight(8);
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        fill.setPrefWidth(0);
        track.getChildren().add(fill);

        track.widthProperty().addListener((obs, ov, nv) -> {
            if (nv.doubleValue() > 0 && total > 0) {
                double target = ((double) count / total) * nv.doubleValue();
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,        new KeyValue(fill.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(600),  new KeyValue(fill.prefWidthProperty(), target, Interpolator.EASE_OUT))
                );
                tl.play();
            }
        });

        HBox row = new HBox(12, nameLbl, track);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private String getConseilSemaine(String type, int scoreMoyen) {
        if (scoreMoyen <= 20) return "✅ Excellente semaine ! Continuez vos bonnes habitudes et maintenez votre routine bien-être.";
        if (scoreMoyen <= 50) return switch (type) {
            case "Stress"     -> "🧘 Semaine modérée en stress. Intégrez 10 min de respiration profonde chaque matin.";
            case "Anxiété"    -> "💬 Anxiété modérée cette semaine. Tenez un journal de vos pensées pour les objectiver.";
            case "Dépression" -> "🌿 Exposez-vous à la lumière naturelle chaque jour et fixez-vous de petits objectifs.";
            case "Bien-être"  -> "🌱 Bien-être satisfaisant. Identifiez un domaine à améliorer et agissez cette semaine.";
            case "Burnout"    -> "⏸️ Planifiez des pauses. Déconnectez les écrans professionnels le soir.";
            default           -> "💡 Continuez votre suivi régulier pour mieux comprendre vos tendances.";
        };
        return switch (type) {
            case "Stress"     -> "🚨 Semaine difficile. Consultez un professionnel et réduisez les obligations non essentielles.";
            case "Anxiété"    -> "🚨 Niveau d'anxiété élevé cette semaine. Une thérapie cognitivo-comportementale peut aider.";
            case "Dépression" -> "🚨 Semaine difficile. N'hésitez pas à consulter un médecin ou un psychologue.";
            case "Bien-être"  -> "🔄 Votre bien-être nécessite attention. Faites un bilan de vos besoins fondamentaux.";
            case "Burnout"    -> "🚨 Risque d'épuisement élevé. Consultez votre médecin pour évaluer la situation.";
            default           -> "🚨 Semaine difficile. Prenez soin de vous et cherchez un soutien professionnel si nécessaire.";
        };
    }


    // ═══════════════════════════════════════════════════════
    //  🏅 BOUTON BADGES — handler FXML
    // ═══════════════════════════════════════════════════════
    @FXML
    private void handleVoirBadges() {
        try {
            List<Evaluation> toutes = serviceEvaluation.recuperer();
            int xpTotal              = gamificationService.calculerXPTotal(toutes);
            GamificationService.Niveau niveau = gamificationService.getNiveau(xpTotal);
            int streak               = gamificationService.calculerStreak(toutes);
            Set<Badge> badgesGagnes  = gamificationService.getBadgesDebloques(toutes);
            showBadgesDialog(toutes.size(), xpTotal, niveau, streak, badgesGagnes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  🏅 DIALOG MES BADGES
    // ═══════════════════════════════════════════════════════
    private void showBadgesDialog(int nbEvals, int xpTotal,
                                   GamificationService.Niveau niveau,
                                   int streak, Set<Badge> badgesGagnes) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🏅  Mes Badges & Progression");

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(580);
        pane.setStyle(
            "-fx-background-color: #071A10;" +
            "-fx-border-color: #FFD700; -fx-border-width: 2;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        pane.getButtonTypes().add(new ButtonType("✓  Fermer", ButtonBar.ButtonData.OK_DONE));

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefHeight(540);

        VBox content = new VBox(18);
        content.setPadding(new Insets(26));

        // ── Titre ──────────────────────────────────────────
        Label titleLbl = new Label("🏅  Mes Badges & Progression");
        titleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 22px; -fx-font-weight: bold;");

        // ── Carte niveau + XP ──────────────────────────────
        VBox niveauCard = new VBox(10);
        niveauCard.setPadding(new Insets(16));
        niveauCard.setStyle(
            "-fx-background-color: rgba(0,217,255,0.07);" +
            "-fx-border-color: rgba(0,217,255,0.30); -fx-border-width: 1.5;" +
            "-fx-border-radius: 16; -fx-background-radius: 16;"
        );

        // Niveau label
        HBox niveauRow = new HBox(14);
        niveauRow.setAlignment(Pos.CENTER_LEFT);
        Label lvlEmoji = new Label(niveau.emoji);
        lvlEmoji.setStyle("-fx-font-size: 38px;");
        VBox lvlTexts = new VBox(4);
        Label lvlName = new Label("Niveau " + niveau.numero + " — " + niveau.titre);
        lvlName.setStyle("-fx-text-fill: " + niveau.couleur + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label xpLbl = new Label("XP total : " + xpTotal);
        xpLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 12px;");
        lvlTexts.getChildren().addAll(lvlName, xpLbl);
        niveauRow.getChildren().addAll(lvlEmoji, lvlTexts);

        // Barre XP
        int xpDansNiveau   = xpTotal - niveau.xpRequis;
        int xpPourProchain = niveau.xpProchain - niveau.xpRequis;
        double pct = Math.min(1.0, (double) xpDansNiveau / xpPourProchain);
        Label xpProgress = new Label(xpDansNiveau + " / " + xpPourProchain + " XP  →  Niveau " + (niveau.numero + 1));
        xpProgress.setStyle("-fx-text-fill: rgba(200,255,220,0.40); -fx-font-size: 11px;");

        Pane track = new Pane();
        track.setPrefHeight(16); track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 8;");
        Pane fill = new Pane();
        fill.setPrefHeight(16);
        fill.setStyle("-fx-background-color: " + niveau.couleur + "; -fx-background-radius: 8;");
        fill.setPrefWidth(0);
        track.getChildren().add(fill);
        track.widthProperty().addListener((ob, ov, nv) -> {
            if (nv.doubleValue() > 0) {
                double target = pct * nv.doubleValue();
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,        new KeyValue(fill.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(900), new KeyValue(fill.prefWidthProperty(), target, Interpolator.EASE_OUT))
                );
                tl.play();
            }
        });

        niveauCard.getChildren().addAll(niveauRow, track, xpProgress);

        // ── Streak ────────────────────────────────────────
        HBox streakBox = new HBox(12);
        streakBox.setAlignment(Pos.CENTER_LEFT);
        streakBox.setPadding(new Insets(12, 16, 12, 16));
        streakBox.setStyle(
            "-fx-background-color: rgba(255,140,0,0.10);" +
            "-fx-border-color: rgba(255,140,0,0.30); -fx-border-width: 1;" +
            "-fx-border-radius: 14; -fx-background-radius: 14;"
        );
        Label fire = new Label("🔥");
        fire.setStyle("-fx-font-size: 28px;");
        VBox streakTexts = new VBox(2);
        Label streakVal = new Label("Streak : " + streak + " jour" + (streak > 1 ? "s" : "") + " consécutif" + (streak > 1 ? "s" : ""));
        streakVal.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label streakSub = new Label(streak >= 7 ? "💎 Incroyable assiduité !" : streak >= 3 ? "🔥 Vous êtes en feu !" : "Continuez pour débloquer des badges !");
        streakSub.setStyle("-fx-text-fill: rgba(255,180,100,0.65); -fx-font-size: 11px;");
        streakTexts.getChildren().addAll(streakVal, streakSub);
        streakBox.getChildren().addAll(fire, streakTexts);

        // ── Stats globales ────────────────────────────────
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
            makeStatCard("📊", String.valueOf(nbEvals), "Évaluations"),
            makeStatCard("🏅", badgesGagnes.size() + "/" + Badge.values().length, "Badges"),
            makeStatCard("⚡", String.valueOf(xpTotal), "XP Total")
        );

        // ── Séparateurs ───────────────────────────────────
        content.getChildren().addAll(
            titleLbl,
            makeSepGold(),
            niveauCard,
            streakBox,
            statsRow,
            makeSepGold()
        );

        // ── Badges OBTENUS ────────────────────────────────
        Label gagnesTitle = new Label("✅  Badges obtenus (" + badgesGagnes.size() + "/" + Badge.values().length + ")");
        gagnesTitle.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 14px; -fx-font-weight: bold;");
        content.getChildren().add(gagnesTitle);

        if (badgesGagnes.isEmpty()) {
            Label noneLbl = new Label("Faites votre première évaluation pour débloquer des badges !");
            noneLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.40); -fx-font-size: 12px; -fx-font-style: italic;");
            content.getChildren().add(noneLbl);
        } else {
            FlowPane gagnesFlow = new FlowPane(12, 12);
            for (Badge b : badgesGagnes) {
                gagnesFlow.getChildren().add(makeBadgeCardBig(b, true));
            }
            content.getChildren().add(gagnesFlow);
        }

        // ── Badges VERROUILLÉS ────────────────────────────
        List<Badge> verrouilles = new ArrayList<>();
        for (Badge b : Badge.values()) {
            if (!badgesGagnes.contains(b)) verrouilles.add(b);
        }

        if (!verrouilles.isEmpty()) {
            content.getChildren().add(makeSepGold());
            Label verrouillesTitle = new Label("🔒  Badges à débloquer (" + verrouilles.size() + ")");
            verrouillesTitle.setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 14px; -fx-font-weight: bold;");
            content.getChildren().add(verrouillesTitle);

            FlowPane verrFlow = new FlowPane(12, 12);
            for (Badge b : verrouilles) {
                verrFlow.getChildren().add(makeBadgeCardBig(b, false));
            }
            content.getChildren().add(verrFlow);
        }

        scroll.setContent(content);
        pane.setContent(scroll);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right,#FFD700,#FF8C00);" +
            "-fx-text-fill: #050C07; -fx-font-weight: bold;" +
            "-fx-padding: 10 32; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    private VBox makeStatCard(String emoji, String val, String label) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(130);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color: rgba(0,217,255,0.07);" +
            "-fx-border-color: rgba(0,217,255,0.20); -fx-border-width: 1;" +
            "-fx-border-radius: 14; -fx-background-radius: 14;"
        );
        Label e = new Label(emoji); e.setStyle("-fx-font-size: 22px;");
        Label v = new Label(val);   v.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label l = new Label(label); l.setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 11px;");
        card.getChildren().addAll(e, v, l);
        return card;
    }

    private VBox makeBadgeCardBig(Badge b, boolean unlocked) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(125);
        card.setPadding(new Insets(14, 10, 14, 10));
        String bg     = unlocked ? "rgba(255,215,0,0.10)" : "rgba(255,255,255,0.03)";
        String border = unlocked ? "#FFD700" : "rgba(255,255,255,0.10)";
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: " + border + "; -fx-border-width: 1.5;" +
            "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        Label emoji = new Label(unlocked ? b.emoji : "🔒");
        emoji.setStyle("-fx-font-size: 32px;");
        Label name = new Label(b.nom);
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        name.setMaxWidth(110);
        name.setStyle("-fx-text-fill: " + (unlocked ? "#FFD700" : "rgba(255,255,255,0.22)") + "; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-alignment: center;");
        Label cond = new Label(b.condition);
        cond.setWrapText(true);
        cond.setAlignment(Pos.CENTER);
        cond.setMaxWidth(110);
        cond.setStyle("-fx-text-fill: rgba(200,255,220,0.30); -fx-font-size: 9.5px; -fx-text-alignment: center;");
        card.getChildren().addAll(emoji, name, cond);
        return card;
    }

    private Line makeSepGold() {
        Line l = new Line(0, 0, 510, 0);
        l.setStroke(Color.web("#FFD700", 0.18));
        return l;
    }

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════
    @FXML private void handleBackToFront()    { navigate("/views/front.fxml", "Accueil"); }
    @FXML private void handleGoToEvents()     { navigate("/views/showEvent.fxml", "Événements"); }
    @FXML private void handleGoToRessources() { navigate("/views/RessourcesFront.fxml", "Ressources"); }

    // ═══════════════════════════════════════════════════════
    //  RAPPORT PERSONNALISÉ PAR ÉVALUATION

    // ═══════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════
    @FXML
    private void handlePasserTest() {
        navigate("/views/questionnaire.fxml", "Évaluation — NeuroWell");
    }

    private void navigate(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) evaluationsContainer.getScene().getWindow();
            double w = stage.getScene().getWidth();
            double h = stage.getScene().getHeight();
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                Scene scene = new Scene(root, w, h);
                stage.setScene(scene);
                stage.setTitle(title);
                // Make root fill the whole scene
                if (root instanceof javafx.scene.layout.Region r) {
                    r.prefWidthProperty().bind(scene.widthProperty());
                    r.prefHeightProperty().bind(scene.heightProperty());
                }
                stage.show();
            });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page.", Alert.AlertType.ERROR);
        }
    }

    // ── Helpers ──────────────────────────────────────────────
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

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle(
            "-fx-background-color: #0D1F12; -fx-border-color: #00D9FF; " +
            "-fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;"
        );
        alert.showAndWait();
    }
}
