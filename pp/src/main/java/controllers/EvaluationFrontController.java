package controllers;

import entities.Evaluation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import services.ServiceEvaluation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EvaluationFrontController {

    @FXML private VBox       evaluationsContainer;
    @FXML private Label      resultsCount;
    @FXML private Label      statTotal;
    @FXML private Label      statMoyenne;
    @FXML private Label      statNiveauFrequent;
    @FXML private TextField  searchField;
    @FXML private DatePicker dateFilter;

    @FXML private CheckBox filterStress;
    @FXML private CheckBox filterAnxiete;
    @FXML private CheckBox filterDepression;
    @FXML private CheckBox filterBienEtre;
    @FXML private CheckBox filterBurnout;
    @FXML private CheckBox filterFaible;
    @FXML private CheckBox filterMoyen;
    @FXML private CheckBox filterEleve;

    private ServiceEvaluation service;
    private List<Evaluation>  allEvaluations;

    @FXML
    public void initialize() {
        service = new ServiceEvaluation();
        loadData();
    }

    private void loadData() {
        try {
            allEvaluations = service.recuperer();
            updateStats(allEvaluations);
            displayEvaluations(allEvaluations);
        } catch (SQLException e) {
            showError("Impossible de charger : " + e.getMessage());
        }
    }

    // ============================================================
    // MÉTIER AVANCÉ 1 — Statistiques dynamiques
    // ============================================================
    private void updateStats(List<Evaluation> list) {
        statTotal.setText(String.valueOf(list.size()));

        if (list.isEmpty()) {
            statMoyenne.setText("0");
            statNiveauFrequent.setText("-");
            return;
        }

        double moyenne = list.stream().mapToInt(Evaluation::getScore).average().orElse(0);
        statMoyenne.setText(String.format("%.1f", moyenne));

        String niveauFrequent = list.stream()
                .collect(Collectors.groupingBy(Evaluation::getNiveau, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        statNiveauFrequent.setText(niveauFrequent);
    }

    // ============================================================
    // AFFICHAGE CARTES — Style sombre cyan comme showEvent
    // ============================================================
    private void displayEvaluations(List<Evaluation> list) {
        evaluationsContainer.getChildren().clear();
        resultsCount.setText(list.size() + " évaluation(s)");

        if (list.isEmpty()) {
            Label empty = new Label("Aucune évaluation trouvée.");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 16px; -fx-padding: 30;");
            evaluationsContainer.getChildren().add(empty);
            return;
        }

        for (Evaluation ev : list) {
            evaluationsContainer.getChildren().add(buildCard(ev));
        }
    }

    private HBox buildCard(Evaluation ev) {
        // Carte principale — style sombre avec bordure cyan
        HBox card = new HBox(25);
        card.setStyle(
            "-fx-background-color: rgba(10, 25, 41, 0.85);" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #00e5ff33;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-padding: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,229,255,0.15), 15, 0, 0, 5);"
        );

        // Emoji selon type
        String emoji = switch (ev.getTypeTest() == null ? "" : ev.getTypeTest()) {
            case "Stress"     -> "😰";
            case "Anxiété"    -> "😟";
            case "Dépression" -> "😔";
            case "Bien-être"  -> "😊";
            case "Burnout"    -> "🔥";
            default           -> "🧪";
        };

        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 38px; -fx-min-width: 55px;");

        // Infos centre
        VBox infos = new VBox(8);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label typeLabel = new Label(ev.getTypeTest());
        typeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label dateLabel = new Label("📅 " + (ev.getDateEvaluation() != null ? ev.getDateEvaluation().toString() : "-"));
        dateLabel.setStyle("-fx-text-fill: #8899aa; -fx-font-size: 13px;");

        // Badge niveau coloré
        String bgColor = switch (ev.getNiveau() == null ? "" : ev.getNiveau()) {
            case "Faible" -> "#00b894";
            case "Moyen"  -> "#fdcb6e";
            case "Élevé"  -> "#d63031";
            default       -> "#00e5ff";
        };
        Label niveauBadge = new Label("  " + ev.getNiveau() + "  ");
        niveauBadge.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 4 12;"
        );

        // Tags style showEvent
        HBox tags = new HBox(10);
        tags.getChildren().addAll(
            makeTag("🎯 " + ev.getTypeTest()),
            makeTag("📊 Score: " + ev.getScore()),
            niveauBadge
        );

        infos.getChildren().addAll(typeLabel, dateLabel, tags);

        // Score cyan (comme le prix dans showEvent)
        VBox scoreBox = new VBox(3);
        scoreBox.setStyle("-fx-alignment: CENTER; -fx-min-width: 90px;");

        Label scoreVal = new Label(String.valueOf(ev.getScore()));
        scoreVal.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label scoreSur = new Label("/ 100");
        scoreSur.setStyle("-fx-text-fill: #556677; -fx-font-size: 13px;");

        scoreBox.getChildren().addAll(scoreVal, scoreSur);

        // Bouton Rapport — style "Participer"
        Button btnRapport = new Button("📄 Rapport");
        btnRapport.setStyle(
            "-fx-background-color: #00e5ff;" +
            "-fx-text-fill: #0a1929;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 10 22;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 13px;"
        );
        btnRapport.setOnAction(e -> afficherRapport(ev));

        // Hover effect
        btnRapport.setOnMouseEntered(e -> btnRapport.setStyle(
            "-fx-background-color: #00b8d4;" +
            "-fx-text-fill: #0a1929;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 10 22;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 13px;"
        ));
        btnRapport.setOnMouseExited(e -> btnRapport.setStyle(
            "-fx-background-color: #00e5ff;" +
            "-fx-text-fill: #0a1929;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 10 22;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 13px;"
        ));

        VBox btnBox = new VBox(btnRapport);
        btnBox.setStyle("-fx-alignment: CENTER;");

        card.getChildren().addAll(iconLabel, infos, scoreBox, btnBox);
        return card;
    }

    private Label makeTag(String text) {
        Label tag = new Label(text);
        tag.setStyle(
            "-fx-background-color: rgba(0,229,255,0.1);" +
            "-fx-border-color: rgba(0,229,255,0.3);" +
            "-fx-border-radius: 20;" +
            "-fx-background-radius: 20;" +
            "-fx-text-fill: #00e5ff;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 4 12;"
        );
        return tag;
    }

    // ============================================================
    // MÉTIER AVANCÉ 2 — Rapport personnalisé
    // ============================================================
    private void afficherRapport(Evaluation ev) {
        Stage stage = new Stage();
        stage.setTitle("Rapport d'évaluation — " + ev.getTypeTest());

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #0a1929; -fx-padding: 35;");

        Label titre = new Label("📋 Rapport Psychologique");
        titre.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label infoLine = new Label("Test : " + ev.getTypeTest() + "   |   Score : " + ev.getScore() + "/100   |   Date : " + ev.getDateEvaluation());
        infoLine.setStyle("-fx-text-fill: #8899aa; -fx-font-size: 13px;");

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #00e5ff33;");

        // Interprétation
        Label interTitre = new Label("📊 Interprétation");
        interTitre.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label interText = new Label(genererInterpretation(ev));
        interText.setWrapText(true);
        interText.setStyle("-fx-text-fill: #ccd6f6; -fx-font-size: 14px; -fx-line-spacing: 4;");
        interText.setMaxWidth(480);

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #00e5ff33;");

        // Recommandations
        Label recoTitre = new Label("💡 Recommandations");
        recoTitre.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label recoText = new Label(genererRecommandation(ev));
        recoText.setWrapText(true);
        recoText.setStyle("-fx-text-fill: #ccd6f6; -fx-font-size: 14px; -fx-line-spacing: 4;");
        recoText.setMaxWidth(480);

        // Bouton fermer
        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
            "-fx-background-color: #00e5ff; -fx-text-fill: #0a1929;" +
            "-fx-font-weight: bold; -fx-background-radius: 20;" +
            "-fx-padding: 10 25; -fx-cursor: hand;"
        );
        btnFermer.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(btnFermer);
        btnRow.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 10 0 0 0;");

        root.getChildren().addAll(titre, infoLine, sep1, interTitre, interText, sep2, recoTitre, recoText, btnRow);

        Scene scene = new Scene(root, 560, 480);
        stage.setScene(scene);
        stage.show();
    }

    private String genererInterpretation(Evaluation ev) {
        int score = ev.getScore();
        String type = ev.getTypeTest() == null ? "ce test" : ev.getTypeTest().toLowerCase();
        return switch (ev.getNiveau() == null ? "" : ev.getNiveau()) {
            case "Faible" -> "Votre score de " + score + "/100 indique un niveau faible de " + type +
                    ".\nVous gérez bien cette dimension de votre santé mentale. Continuez vos bonnes habitudes.";
            case "Moyen"  -> "Votre score de " + score + "/100 indique un niveau modéré de " + type +
                    ".\nUne attention particulière est conseillée pour éviter une aggravation.";
            case "Élevé"  -> "Votre score de " + score + "/100 indique un niveau élevé de " + type +
                    ".\nIl est fortement conseillé de consulter un professionnel de santé mentale rapidement.";
            default       -> "Score enregistré : " + score + "/100. Consultez un professionnel pour une analyse complète.";
        };
    }

    private String genererRecommandation(Evaluation ev) {
        String type   = ev.getTypeTest() == null ? "" : ev.getTypeTest();
        String niveau = ev.getNiveau()   == null ? "" : ev.getNiveau();
        return switch (type) {
            case "Stress" -> niveau.equals("Élevé")
                ? "• Pratiquez 20 min de respiration profonde par jour\n• Réduisez les sources de stress professionnelles\n• Consultez un psychologue spécialisé en gestion du stress"
                : "• Maintenez une activité physique régulière\n• Pratiquez la méditation 10 min/jour\n• Adoptez une hygiène de sommeil de 7-8h";
            case "Anxiété" -> niveau.equals("Élevé")
                ? "• Thérapie cognitivo-comportementale (TCC) recommandée\n• Évitez la caféine et l'alcool\n• Pratiquez la cohérence cardiaque 3x/jour"
                : "• Exercice physique modéré 3x/semaine\n• Journaling quotidien de vos émotions\n• Techniques de relaxation progressive";
            case "Dépression" -> niveau.equals("Élevé")
                ? "• Consultation psychiatrique urgente recommandée\n• Maintenez le lien social, ne restez pas isolé(e)\n• Activités plaisantes quotidiennes même minimes"
                : "• Exposition à la lumière naturelle chaque matin\n• Activité physique douce (marche, yoga)\n• Parlez à un proche de confiance";
            case "Bien-être" ->
                "• Continuez vos pratiques de bien-être actuelles\n• Essayez la pleine conscience (mindfulness)\n• Planifiez des activités joyeuses chaque semaine";
            case "Burnout" -> niveau.equals("Élevé")
                ? "• Prenez une pause professionnelle si possible\n• Consultez un médecin du travail\n• Déconnectez-vous des écrans le soir"
                : "• Établissez des limites claires travail/vie privée\n• Pratiquez des activités de récupération\n• Dormez minimum 7h par nuit";
            default -> "• Maintenez un mode de vie sain et équilibré\n• Pratiquez une activité relaxante régulièrement\n• Consultez un professionnel si besoin";
        };
    }

    // ============================================================
    // FILTRES
    // ============================================================
    @FXML
    private void handleSearch() { appliquerFiltres(); }

    @FXML
    private void resetFilters() {
        searchField.clear();
        dateFilter.setValue(null);
        filterStress.setSelected(false);
        filterAnxiete.setSelected(false);
        filterDepression.setSelected(false);
        filterBienEtre.setSelected(false);
        filterBurnout.setSelected(false);
        filterFaible.setSelected(false);
        filterMoyen.setSelected(false);
        filterEleve.setSelected(false);
        displayEvaluations(allEvaluations);
        updateStats(allEvaluations);
    }

    private void appliquerFiltres() {
        List<Evaluation> filtered = allEvaluations.stream().filter(ev -> {
            // Recherche texte
            String s = searchField.getText().toLowerCase().trim();
            if (!s.isEmpty() && ev.getTypeTest() != null && !ev.getTypeTest().toLowerCase().contains(s))
                return false;
            // Filtre date
            if (dateFilter.getValue() != null && ev.getDateEvaluation() != null)
                if (!ev.getDateEvaluation().toLocalDate().equals(dateFilter.getValue())) return false;
            // Filtre type
            boolean typeOk = !filterStress.isSelected() && !filterAnxiete.isSelected()
                    && !filterDepression.isSelected() && !filterBienEtre.isSelected() && !filterBurnout.isSelected();
            if (!typeOk) {
                typeOk = (filterStress.isSelected()     && "Stress".equals(ev.getTypeTest()))
                      || (filterAnxiete.isSelected()    && "Anxiété".equals(ev.getTypeTest()))
                      || (filterDepression.isSelected() && "Dépression".equals(ev.getTypeTest()))
                      || (filterBienEtre.isSelected()   && "Bien-être".equals(ev.getTypeTest()))
                      || (filterBurnout.isSelected()    && "Burnout".equals(ev.getTypeTest()));
                if (!typeOk) return false;
            }
            // Filtre niveau
            boolean niveauOk = !filterFaible.isSelected() && !filterMoyen.isSelected() && !filterEleve.isSelected();
            if (!niveauOk) {
                niveauOk = (filterFaible.isSelected() && "Faible".equals(ev.getNiveau()))
                        || (filterMoyen.isSelected()  && "Moyen".equals(ev.getNiveau()))
                        || (filterEleve.isSelected()  && "Élevé".equals(ev.getNiveau()));
                if (!niveauOk) return false;
            }
            return true;
        }).collect(Collectors.toList());

        updateStats(filtered);
        displayEvaluations(filtered);
    }

    // ============================================================
    // NAVIGATION
    // ============================================================
    @FXML private void handleAccueil()    { naviguer("/views/front.fxml"); }
    @FXML private void handleEvents()     { naviguer("/views/showEvent.fxml"); }
    @FXML private void handlePasserTest() { naviguer("/views/AjoutEvaluation.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) evaluationsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showError("Navigation impossible : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur"); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }
}
