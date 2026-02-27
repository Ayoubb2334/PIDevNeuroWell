package controllers;

import entities.Evaluation;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import services.ServiceEvaluation;
import services.GamificationService;
import services.GamificationService.Badge;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class AjoutEvaluationController {

    @FXML private ComboBox<String> typeTestField;
    @FXML private Label typeTestError;
    @FXML private TextField scoreField;
    @FXML private Label scoreError;
    @FXML private Label niveauDisplay;
    @FXML private Label niveauError;
    @FXML private DatePicker dateField;
    @FXML private Label dateError;
    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;

    private ServiceEvaluation serviceEvaluation;
    private GamificationService gamificationService;

    @FXML
    public void initialize() {
        serviceEvaluation = new ServiceEvaluation();
        gamificationService = new GamificationService();
        typeTestField.getItems().addAll("Stress", "Anxiété", "Dépression", "Bien-être", "Burnout");
        clearErrors();
        submitBtn.setOnAction(event -> handleSubmit());
        cancelBtn.setOnAction(event -> handleCancel());

        // Auto-calcul niveau selon le score — affiché en lecture seule
        scoreField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int s = Integer.parseInt(newVal.trim());
                if (s <= 20) {
                    niveauDisplay.setText("✅  Faible");
                    niveauDisplay.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 14;");
                } else if (s <= 50) {
                    niveauDisplay.setText("⚠️  Moyen");
                    niveauDisplay.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 14;");
                } else {
                    niveauDisplay.setText("🔴  Élevé");
                    niveauDisplay.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 14;");
                }
            } catch (NumberFormatException ignored) {
                niveauDisplay.setText("— Saisissez un score —");
                niveauDisplay.setStyle("-fx-text-fill: rgba(200,255,220,0.35); -fx-font-size: 13px; -fx-padding: 10 14;");
            }
        });
    }

    private void handleSubmit() {
        clearErrors();
        boolean valid = true;

        String typeTest  = typeTestField.getValue();
        String scoreText = scoreField.getText().trim();
        String niveau    = niveauDisplay.getText().replaceAll("[^A-Za-zÀ-ÿ]", "").trim();
        LocalDate date   = dateField.getValue();

        if (typeTest == null || typeTest.isEmpty()) {
            typeTestError.setText("Le type de test est obligatoire.");
            valid = false;
        }

        int score = 0;
        if (scoreText.isEmpty()) {
            scoreError.setText("Le score est obligatoire.");
            valid = false;
        } else {
            try {
                score = Integer.parseInt(scoreText);
                if (score < 0 || score > 100) {
                    scoreError.setText("Le score doit être entre 0 et 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                scoreError.setText("Le score doit être un nombre entier.");
                valid = false;
            }
        }

        // Niveau auto-calculé — pas de validation manuelle nécessaire
        if (niveau == null || niveau.isEmpty()) {
            // Cas très rare : score non encore saisi, on force Faible
            niveau = "Faible";
            niveauDisplay.setText("✅  Faible");
            niveauDisplay.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 14;");
        }

        if (date == null) {
            dateError.setText("La date est obligatoire.");
            valid = false;
        } else if (date.isAfter(LocalDate.now())) {
            dateError.setText("La date ne peut pas être dans le futur.");
            valid = false;
        }

        if (!valid) {
            showErrorMessage("Veuillez remplir correctement tous les champs.");
            return;
        }

        // Chercher évaluation précédente du même type AVANT d'ajouter
        Evaluation precedente = getPrecedenteEvaluation(typeTest);

        // Créer et sauvegarder la nouvelle évaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setTypeTest(typeTest);
        evaluation.setScore(score);
        evaluation.setNiveau(niveau);
        evaluation.setDateEvaluation(Date.valueOf(date));
        evaluation.setIdUser(0);

        try {
            serviceEvaluation.ajouter(evaluation);
            clearForm();

            // Afficher suivi de progression si une précédente existe
            if (precedente != null) {
                showProgressionDialog(evaluation, precedente);
            } else {
                showSuccessMessage("Évaluation ajoutée avec succès !\nAucune évaluation précédente de type \"" + typeTest + "\" trouvée.");
            }

            // Vérifier alerte niveau élevé consécutif
            verifierAlerteConsecutive(typeTest);

            // 🎮 Gamification — XP, Niveau, Badges
            try {
                List<Evaluation> toutesApres = serviceEvaluation.recuperer();
                GamificationService.ResultatGamification res =
                    gamificationService.calculer(toutesApres, evaluation);
                showGamificationDialog(res);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Impossible d'ajouter l'évaluation : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  RÉCUPÉRER LA DERNIÈRE ÉVALUATION DU MÊME TYPE
    // ═══════════════════════════════════════════════════════
    private Evaluation getPrecedenteEvaluation(String typeTest) {
        try {
            List<Evaluation> toutes = serviceEvaluation.recuperer();
            return toutes.stream()
                .filter(e -> typeTest.equals(e.getTypeTest()) && e.getDateEvaluation() != null)
                .max(Comparator.comparing(e -> e.getDateEvaluation().toLocalDate()))
                .orElse(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  DIALOG SUIVI DE PROGRESSION
    // ═══════════════════════════════════════════════════════
    private void showProgressionDialog(Evaluation nouvelle, Evaluation precedente) {
        int diff = nouvelle.getScore() - precedente.getScore();

        String icon, tendance, color, message;
        if (diff < 0) {
            icon = "📈"; tendance = "Amélioration"; color = "#00FF88";
            message = "Score passé de " + precedente.getScore() + " → " + nouvelle.getScore() + " (" + diff + " pts)";
        } else if (diff > 0) {
            icon = "📉"; tendance = "Aggravation"; color = "#FF4D6D";
            message = "Score passé de " + precedente.getScore() + " → " + nouvelle.getScore() + " (+" + diff + " pts)";
        } else {
            icon = "➡️"; tendance = "Stable"; color = "#FFD700";
            message = "Score identique à l'évaluation précédente (" + nouvelle.getScore() + " pts)";
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📊  Suivi de progression — " + nouvelle.getTypeTest());

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(500);
        pane.setStyle(
            "-fx-background-color: #071A10;" +
            "-fx-border-color: #00D9FF; -fx-border-width: 2;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        pane.getButtonTypes().add(new ButtonType("✓  Fermer", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(18);
        content.setPadding(new Insets(26));

        // En-tête
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        Label emojiLbl = new Label(getTypeEmoji(nouvelle.getTypeTest()));
        emojiLbl.setStyle("-fx-font-size: 38px;");
        VBox headerText = new VBox(4);
        Label titleLbl = new Label("Suivi de progression");
        titleLbl.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label subLbl = new Label(nouvelle.getTypeTest() + "  •  " + nouvelle.getDateEvaluation());
        subLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 12px;");
        headerText.getChildren().addAll(titleLbl, subLbl);
        header.getChildren().addAll(emojiLbl, headerText);
        content.getChildren().addAll(header, makeSeparator());

        // Barres comparatives
        Label barsTitle = new Label("Comparaison des scores");
        barsTitle.setStyle("-fx-text-fill: rgba(200,255,220,0.60); -fx-font-size: 12px; -fx-font-weight: bold;");
        content.getChildren().add(barsTitle);
        content.getChildren().add(buildBar("Précédent  (" + precedente.getNiveau() + ")", precedente.getScore(), "rgba(200,255,220,0.35)", false));
        content.getChildren().add(buildBar("Nouveau     (" + nouvelle.getNiveau() + ")",   nouvelle.getScore(),  color, true));
        content.getChildren().add(makeSeparator());

        // Badge tendance central
        VBox tendanceBox = new VBox(8);
        tendanceBox.setAlignment(Pos.CENTER);
        tendanceBox.setPadding(new Insets(16, 20, 16, 20));
        tendanceBox.setStyle(
            "-fx-background-color: " + color + "18;" +
            "-fx-border-color: " + color + "55; -fx-border-width: 1;" +
            "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        Label diffLbl = new Label((diff > 0 ? "+" : "") + diff + " pts");
        diffLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 38px; -fx-font-weight: bold;");
        Label tendanceLbl = new Label(icon + "  " + tendance);
        tendanceLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
        tendanceBox.getChildren().addAll(diffLbl, tendanceLbl, msgLbl);
        content.getChildren().add(tendanceBox);
        content.getChildren().add(makeSeparator());

        // Info précédente
        Label prevInfo = new Label("Évaluation précédente : Score " + precedente.getScore()
            + " / Niveau " + precedente.getNiveau()
            + " / Date " + precedente.getDateEvaluation());
        prevInfo.setWrapText(true);
        prevInfo.setStyle("-fx-text-fill: rgba(200,255,220,0.35); -fx-font-size: 11px;");

        // Succès
        Label savedLbl = new Label("✅  Nouvelle évaluation sauvegardée avec succès !");
        savedLbl.setStyle(
            "-fx-text-fill: #00FF88; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-color: rgba(0,255,136,0.10);" +
            "-fx-border-color: rgba(0,255,136,0.30); -fx-border-width: 1;" +
            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14;"
        );
        content.getChildren().addAll(prevInfo, savedLbl);

        pane.setContent(content);
        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right,#00D9FF,#00FF88);" +
            "-fx-text-fill: #050C07; -fx-font-weight: bold;" +
            "-fx-padding: 10 32; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    // Barre de progression animée
    private VBox buildBar(String label, int score, String color, boolean animate) {
        Label nameLbl = new Label(label + " :  " + score + " / 100");
        nameLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Pane track = new Pane();
        track.setPrefHeight(10);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color: rgba(0,217,255,0.10); -fx-background-radius: 5;");

        Pane fill = new Pane();
        fill.setPrefHeight(10);
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");
        fill.setPrefWidth(0);
        track.getChildren().add(fill);

        track.widthProperty().addListener((obs, ov, nv) -> {
            if (nv.doubleValue() > 0) {
                double target = (score / 100.0) * nv.doubleValue();
                if (animate) {
                    Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,        new KeyValue(fill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(700),  new KeyValue(fill.prefWidthProperty(), target, Interpolator.EASE_OUT))
                    );
                    tl.setDelay(Duration.millis(200));
                    tl.play();
                } else {
                    fill.setPrefWidth(target);
                }
            }
        });

        return new VBox(6, nameLbl, track);
    }

    private Line makeSeparator() {
        Line line = new Line(0, 0, 440, 0);
        line.setStroke(Color.web("#00D9FF", 0.15));
        return line;
    }

    // ═══════════════════════════════════════════════════════
    //  IDÉE 7 — ALERTE NIVEAUX ÉLEVÉS CONSÉCUTIFS
    // ═══════════════════════════════════════════════════════
    private void verifierAlerteConsecutive(String typeTest) {
        try {
            List<Evaluation> toutes = serviceEvaluation.recuperer();

            // Filtrer par type, trier par date décroissante (plus récent en premier)
            List<Evaluation> parType = toutes.stream()
                .filter(e -> typeTest.equals(e.getTypeTest()) && e.getDateEvaluation() != null)
                .sorted((a, b) -> b.getDateEvaluation().compareTo(a.getDateEvaluation()))
                .collect(java.util.stream.Collectors.toList());

            // Compter les "Élevé" consécutifs depuis la plus récente
            int consecutifs = 0;
            for (Evaluation e : parType) {
                if ("Élevé".equals(e.getNiveau())) {
                    consecutifs++;
                } else {
                    break; // dès qu'on tombe sur Faible ou Moyen, on arrête
                }
            }

            // Déclencher l'alerte si 3 ou plus
            if (consecutifs >= 3) {
                showAlerteDialog(typeTest, consecutifs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlerteDialog(String typeTest, int nbConsecutifs) {
        Dialog<ButtonType> dialog = new Dialog<>();
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

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(500);
        pane.setStyle(
            "-fx-background-color: #1A0508;" +
            "-fx-border-color: #FF4D6D; -fx-border-width: 2.5;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        pane.getButtonTypes().add(new ButtonType("J'ai compris", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(18);
        content.setPadding(new Insets(28));

        // Icône alerte pulsante
        Label alertIcon = new Label("🚨");
        alertIcon.setStyle("-fx-font-size: 52px;");
        alertIcon.setAlignment(javafx.geometry.Pos.CENTER);
        alertIcon.setMaxWidth(Double.MAX_VALUE);
        ScaleTransition pulse = new ScaleTransition(javafx.util.Duration.millis(800), alertIcon);
        pulse.setFromX(1.0); pulse.setToX(1.15);
        pulse.setFromY(1.0); pulse.setToY(1.15);
        pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        // Titre
        Label titleLbl = new Label("Niveau Élevé détecté " + nbConsecutifs + " fois de suite !");
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-alignment: center;");
        titleLbl.setMaxWidth(Double.MAX_VALUE);
        titleLbl.setAlignment(javafx.geometry.Pos.CENTER);

        // Séparateur rouge
        javafx.scene.shape.Line sep = new javafx.scene.shape.Line(0, 0, 440, 0);
        sep.setStroke(javafx.scene.paint.Color.web("#FF4D6D", 0.30));

        // Type + emoji
        Label typeLbl = new Label(getTypeEmoji(typeTest) + "  " + typeTest);
        typeLbl.setStyle(
            "-fx-text-fill: #FF4D6D; -fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-background-color: rgba(255,77,109,0.12);" +
            "-fx-border-color: rgba(255,77,109,0.40); -fx-border-width: 1;" +
            "-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 20;"
        );
        typeLbl.setMaxWidth(Double.MAX_VALUE);
        typeLbl.setAlignment(javafx.geometry.Pos.CENTER);

        // Message principal
        Label msgLbl = new Label(
            "Vos " + nbConsecutifs + " dernières évaluations de type \"" + typeTest +
            "\" sont toutes au niveau Élevé.\n\n" +
            "Cette tendance répétée peut indiquer un besoin d'accompagnement professionnel."
        );
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: rgba(255,180,180,0.90); -fx-font-size: 13px; -fx-line-spacing: 4;");

        // Recommandation urgente
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

        content.getChildren().addAll(alertIcon, titleLbl, sep, typeLbl, msgLbl, recommBox);
        pane.setContent(content);

        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: #FF4D6D;" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
            "-fx-padding: 12 40; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    private String getRecommandationUrgente(String type) {
        return switch (type) {
            case "Stress"     -> "Consultez un médecin généraliste ou un psychologue. Pratiquez la cohérence cardiaque 3×/jour et réduisez les sources de stress non essentielles.";
            case "Anxiété"    -> "Une thérapie cognitivo-comportementale (TCC) est fortement recommandée. Contactez un professionnel de santé mentale dès que possible.";
            case "Dépression" -> "Consultez un médecin ou un psychiatre rapidement. Vous n'êtes pas seul(e) — l'aide professionnelle fait une vraie différence.";
            case "Bien-être"  -> "Un accompagnement en développement personnel ou un bilan de vie avec un coach certifié peut être bénéfique. Parlez-en à un professionnel.";
            case "Burnout"    -> "Consultez votre médecin pour un arrêt maladie si nécessaire. Le burnout non traité a des conséquences graves sur la santé physique et mentale.";
            default           -> "Consultez un professionnel de santé mentale pour un suivi adapté à votre situation.";
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


    // ═══════════════════════════════════════════════════════
    //  🎮 DIALOG GAMIFICATION — XP + NIVEAU + BADGES
    // ═══════════════════════════════════════════════════════
    private void showGamificationDialog(GamificationService.ResultatGamification res) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🎮  Votre progression");

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(520);
        pane.setStyle(
            "-fx-background-color: #071A10;" +
            "-fx-border-color: #00D9FF; -fx-border-width: 2;" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        pane.getButtonTypes().add(new ButtonType("🚀  Continuer", ButtonBar.ButtonData.OK_DONE));

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));

        // ── Titre ─────────────────────────────────────────────
        Label titleLbl = new Label("🎮  Votre progression");
        titleLbl.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 20px; -fx-font-weight: bold;");

        // ── XP gagné cette évaluation ─────────────────────────
        HBox xpGagneBox = new HBox(10);
        xpGagneBox.setAlignment(Pos.CENTER_LEFT);
        xpGagneBox.setPadding(new Insets(10, 14, 10, 14));
        xpGagneBox.setStyle(
            "-fx-background-color: rgba(0,217,255,0.08);" +
            "-fx-border-color: rgba(0,217,255,0.25); -fx-border-width: 1;" +
            "-fx-border-radius: 14; -fx-background-radius: 14;"
        );
        Label xpIcon = new Label("⚡");
        xpIcon.setStyle("-fx-font-size: 22px;");
        VBox xpTexts = new VBox(2);
        Label xpVal = new Label("+" + res.xpGagne + " XP gagnés");
        xpVal.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 17px; -fx-font-weight: bold;");
        HBox bonusBox = new HBox(8);
        if (res.xpBonus_faible)       bonusBox.getChildren().add(makePill("+5 Faible",       "#00FF88"));
        if (res.xpBonus_amelioration) bonusBox.getChildren().add(makePill("+15 Amélioration", "#00D9FF"));
        if (res.xpBonus_streak)       bonusBox.getChildren().add(makePill("+20 Streak",       "#FFD700"));
        if (!res.nouveauxBadges.isEmpty()) bonusBox.getChildren().add(makePill("+25×" + res.nouveauxBadges.size() + " Badge", "#FF8C00"));
        xpTexts.getChildren().addAll(xpVal, bonusBox);
        xpGagneBox.getChildren().addAll(xpIcon, xpTexts);

        // ── Barre XP ──────────────────────────────────────────
        GamificationService.Niveau n = res.niveauActuel;
        int xpDansNiveau  = res.xpTotal - n.xpRequis;
        int xpPourProchain = n.xpProchain - n.xpRequis;
        double pct = Math.min(1.0, (double) xpDansNiveau / xpPourProchain);

        Label niveauLbl = new Label(n.emoji + "  Niveau " + n.numero + " — " + n.titre);
        niveauLbl.setStyle("-fx-text-fill: " + n.couleur + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Pane track = new Pane();
        track.setPrefHeight(14); track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 7;");
        Pane fill = new Pane();
        fill.setPrefHeight(14);
        fill.setStyle("-fx-background-color: " + n.couleur + "; -fx-background-radius: 7;");
        fill.setPrefWidth(0);
        track.getChildren().add(fill);
        track.widthProperty().addListener((ob, ov, nv) -> {
            if (nv.doubleValue() > 0) {
                double target = pct * nv.doubleValue();
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,       new KeyValue(fill.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(900), new KeyValue(fill.prefWidthProperty(), target, Interpolator.EASE_OUT))
                );
                // level-up: barre se remplit entièrement d'abord
                if (res.levelUp) {
                    tl = new Timeline(
                        new KeyFrame(Duration.ZERO,        new KeyValue(fill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(600),  new KeyValue(fill.prefWidthProperty(), nv.doubleValue(), Interpolator.EASE_OUT)),
                        new KeyFrame(Duration.millis(900),  new KeyValue(fill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(1500), new KeyValue(fill.prefWidthProperty(), target, Interpolator.EASE_OUT))
                    );
                }
                tl.play();
            }
        });

        Label xpProgressLbl = new Label(xpDansNiveau + " / " + xpPourProchain + " XP  →  Niveau " + (n.numero + 1));
        xpProgressLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 11px;");

        // ── Level Up banner ───────────────────────────────────
        VBox levelUpBanner = null;
        if (res.levelUp) {
            levelUpBanner = new VBox(4);
            levelUpBanner.setAlignment(Pos.CENTER);
            levelUpBanner.setPadding(new Insets(12, 16, 12, 16));
            levelUpBanner.setStyle(
                "-fx-background-color: rgba(255,215,0,0.12);" +
                "-fx-border-color: #FFD700; -fx-border-width: 2;" +
                "-fx-border-radius: 16; -fx-background-radius: 16;"
            );
            Label lvlIcon = new Label("🎉");
            lvlIcon.setStyle("-fx-font-size: 36px;");
            Label lvlTxt = new Label("LEVEL UP ! " + res.ancienNiveau.emoji + " → " + n.emoji + " " + n.titre);
            lvlTxt.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16px; -fx-font-weight: bold;");
            levelUpBanner.getChildren().addAll(lvlIcon, lvlTxt);
            ScaleTransition sc = new ScaleTransition(Duration.millis(600), levelUpBanner);
            sc.setFromX(0.8); sc.setToX(1.0);
            sc.setFromY(0.8); sc.setToY(1.0);
            sc.play();
        }

        // ── Streak ────────────────────────────────────────────
        HBox streakBox = new HBox(10);
        streakBox.setAlignment(Pos.CENTER_LEFT);
        streakBox.setPadding(new Insets(10, 14, 10, 14));
        streakBox.setStyle(
            "-fx-background-color: rgba(255,140,0,0.10);" +
            "-fx-border-color: rgba(255,140,0,0.30); -fx-border-width: 1;" +
            "-fx-border-radius: 14; -fx-background-radius: 14;"
        );
        Label fireIcon = new Label("🔥");
        fireIcon.setStyle("-fx-font-size: 24px;");
        Label streakLbl = new Label("Streak : " + res.streak + " jour" + (res.streak > 1 ? "s" : "") + " consécutif" + (res.streak > 1 ? "s" : ""));
        streakLbl.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 15px; -fx-font-weight: bold;");
        streakBox.getChildren().addAll(fireIcon, streakLbl);

        // ── Nouveaux badges ───────────────────────────────────
        content.getChildren().addAll(titleLbl, makeSep(), xpGagneBox, makeSep(), niveauLbl, track, xpProgressLbl);
        if (levelUpBanner != null) content.getChildren().add(levelUpBanner);
        content.getChildren().addAll(makeSep(), streakBox);

        if (!res.nouveauxBadges.isEmpty()) {
            content.getChildren().add(makeSep());
            Label badgeTitleLbl = new Label("🏅  Nouveau" + (res.nouveauxBadges.size() > 1 ? "x" : "") + " badge" + (res.nouveauxBadges.size() > 1 ? "s" : "") + " débloqué" + (res.nouveauxBadges.size() > 1 ? "s" : "") + " !");
            badgeTitleLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 15px; -fx-font-weight: bold;");
            content.getChildren().add(badgeTitleLbl);

            FlowPane badgesFlow = new FlowPane(10, 10);
            for (Badge b : res.nouveauxBadges) {
                VBox card = makeBadgeCard(b, true);
                badgesFlow.getChildren().add(card);
                FadeTransition ft = new FadeTransition(Duration.millis(500), card);
                ft.setFromValue(0); ft.setToValue(1);
                ft.setDelay(Duration.millis(300));
                ft.play();
            }
            content.getChildren().add(badgesFlow);
        }

        // ── XP total ─────────────────────────────────────────
        content.getChildren().add(makeSep());
        Label totalLbl = new Label("XP total : " + res.xpTotal + " XP");
        totalLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.40); -fx-font-size: 11px;");
        content.getChildren().add(totalLbl);

        pane.setContent(content);
        pane.lookupButton(pane.getButtonTypes().get(0)).setStyle(
            "-fx-background-color: linear-gradient(to right,#00D9FF,#00FF88);" +
            "-fx-text-fill: #050C07; -fx-font-weight: bold;" +
            "-fx-padding: 10 32; -fx-background-radius: 20; -fx-cursor: hand;"
        );

        pane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), pane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    private Label makePill(String text, String color) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-color: " + color + "22;" +
            "-fx-border-color: " + color + "66; -fx-border-width: 1;" +
            "-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 2 8;"
        );
        return l;
    }

    private VBox makeBadgeCard(Badge b, boolean unlocked) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(110);
        card.setPadding(new Insets(12, 8, 12, 8));
        String bg    = unlocked ? "rgba(255,215,0,0.10)" : "rgba(255,255,255,0.04)";
        String border= unlocked ? "#FFD700" : "rgba(255,255,255,0.10)";
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: " + border + "; -fx-border-width: 1.5;" +
            "-fx-border-radius: 14; -fx-background-radius: 14;"
        );
        Label emoji = new Label(unlocked ? b.emoji : "🔒");
        emoji.setStyle("-fx-font-size: 28px;");
        Label name  = new Label(b.nom);
        name.setWrapText(true);
        name.setStyle("-fx-text-fill: " + (unlocked ? "#FFD700" : "rgba(255,255,255,0.25)") + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-alignment: center;");
        name.setMaxWidth(95);
        Label cond = new Label(b.condition);
        cond.setWrapText(true);
        cond.setStyle("-fx-text-fill: rgba(200,255,220,0.35); -fx-font-size: 9px; -fx-text-alignment: center;");
        cond.setMaxWidth(95);
        card.getChildren().addAll(emoji, name, cond);
        return card;
    }

    private Line makeSep() {
        Line l = new Line(0, 0, 460, 0);
        l.setStroke(Color.web("#00D9FF", 0.12));
        return l;
    }

    private void handleCancel() { clearForm(); clearErrors(); }

    private void clearForm() {
        typeTestField.setValue(null);
        scoreField.clear();
        niveauDisplay.setText("— Saisissez un score —");
        niveauDisplay.setStyle("-fx-text-fill: rgba(200,255,220,0.35); -fx-font-size: 13px; -fx-padding: 10 14;");
        dateField.setValue(null);
    }

    private void clearErrors() {
        typeTestError.setText("");
        scoreError.setText("");
        niveauError.setText("");
        dateError.setText("");
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
