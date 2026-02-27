package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvenement;
import services.ServiceParticipation;
import services.ServiceEvaluation;
import services.ServiceRessource;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    // ═══════════════════════════════════════════════════════
    //  DESIGN TOKENS
    // ═══════════════════════════════════════════════════════
    private static final String C_CYAN    = "#00D9FF";
    private static final String C_GREEN   = "#00FF88";
    private static final String C_DARK    = "#050C07";
    private static final String C_CARD    = "#0D1F12";
    private static final String C_SURFACE = "#112016";
    private static final String C_DANGER  = "#FF4D6D";
    private static final String C_TEXT    = "#E8FFF0";

    // ═══════════════════════════════════════════════════════
    //  FXML INJECTED
    // ═══════════════════════════════════════════════════════
    @FXML private StackPane contentPane;
    @FXML private VBox      sidebar;

    // Nav buttons — Événements
    @FXML private Button btnEvenements;
    @FXML private VBox   evenementSubMenu;
    @FXML private Button btnAfficherEvenements;
    @FXML private Button btnAjouterEvenement;

    // Nav buttons — Évaluation (nouveau)
    @FXML private Button btnEvaluation;
    @FXML private VBox   evaluationSubMenu;
    @FXML private Button btnAjouterEvaluation;
    @FXML private Button btnAfficherEvaluation;

    // Nav buttons — Ressources
    @FXML private Button btnRessources;
    @FXML private VBox   ressourceSubMenu;
    @FXML private Button btnAjouterRessource;
    @FXML private Button btnAfficherRessource;

    // Nav buttons — Congés
    @FXML private Button btnConges;
    @FXML private VBox   congeSubMenu;
    @FXML private Button btnAjouterConge;
    @FXML private Button btnAfficherConge;

    @FXML private Button btnFront;
    @FXML private Button btnDashboard;  // ← AJOUT : bouton Tableau de bord

    // ═══════════════════════════════════════════════════════
    //  SERVICES
    // ═══════════════════════════════════════════════════════
    private final ServiceEvenement     serviceEvenement     = new ServiceEvenement();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private final ServiceEvaluation    serviceEvaluation    = new ServiceEvaluation();
    private final ServiceRessource     serviceRessource     = new ServiceRessource();

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        // ── Sidebar style ────────────────────────────────────
        styleSidebar();

        // ── Build dynamic header in sidebar ──────────────────
        buildSidebarHeader();

        // ── Style all nav buttons ─────────────────────────────
        styleNavButton(btnDashboard,         "⚡", "Tableau de bord");  // ← AJOUT
        styleNavButton(btnEvenements,        "🗓", "Événements");
        styleNavButton(btnEvaluation,        "📊", "Évaluation");
        styleNavButton(btnRessources,        "📚", "Ressources");
        styleNavButton(btnConges,            "🏖", "Congés");
        styleNavButton(btnFront,             "🌐", "Front Office");

        styleSubButton(btnAfficherEvenements, "📋", "Afficher");
        styleSubButton(btnAjouterEvenement,   "➕", "Ajouter");
        styleSubButton(btnAfficherEvaluation, "📋", "Afficher");
        styleSubButton(btnAjouterEvaluation,  "➕", "Ajouter");
        styleSubButton(btnAfficherRessource,  "📋", "Afficher");
        styleSubButton(btnAjouterRessource,   "➕", "Ajouter");
        styleSubButton(btnAfficherConge,      "📋", "Afficher");
        styleSubButton(btnAjouterConge,       "➕", "Ajouter");

        // ── Submenus hidden ───────────────────────────────────
        hideSubMenu(evenementSubMenu);
        hideSubMenu(evaluationSubMenu);
        hideSubMenu(ressourceSubMenu);
        hideSubMenu(congeSubMenu);

        // ── Wire actions ──────────────────────────────────────
        btnFront.setOnAction(this::openFrontPage);
        if (btnDashboard != null) btnDashboard.setOnAction(e -> showWelcomeDashboard()); // ← AJOUT

        btnEvenements.setOnAction(e -> {
            toggleMenu(evenementSubMenu);
            hideSubMenu(evaluationSubMenu);
            hideSubMenu(congeSubMenu);
            pulseButton(btnEvenements);
        });

        btnEvaluation.setOnAction(e -> {
            toggleMenu(evaluationSubMenu);
            hideSubMenu(evenementSubMenu);
            hideSubMenu(ressourceSubMenu);
            hideSubMenu(congeSubMenu);
            pulseButton(btnEvaluation);
        });

        btnRessources.setOnAction(e -> {
            toggleMenu(ressourceSubMenu);
            hideSubMenu(evenementSubMenu);
            hideSubMenu(evaluationSubMenu);
            hideSubMenu(congeSubMenu);
            pulseButton(btnRessources);
        });

        btnConges.setOnAction(e -> {
            toggleMenu(congeSubMenu);
            hideSubMenu(evenementSubMenu);
            hideSubMenu(evaluationSubMenu);
            hideSubMenu(ressourceSubMenu);
            pulseButton(btnConges);
        });

        // Événements actions
        btnAfficherEvenements.setOnAction(e -> loadView("/views/EvenementsTable.fxml"));
        btnAjouterEvenement.setOnAction(e -> loadView("/views/evenement.fxml"));

        // Évaluation actions
        btnAfficherEvaluation.setOnAction(e -> loadView("/views/AfficherEvaluation.fxml"));
        btnAjouterEvaluation.setOnAction(e -> loadView("/views/AjoutEvaluation.fxml"));

        // Ressources actions
        btnAfficherRessource.setOnAction(e -> loadView("/views/AfficherRessource.fxml"));
        btnAjouterRessource.setOnAction(e -> loadView("/views/AjoutRessource.fxml"));

        // Congés actions
        btnAfficherConge.setOnAction(e -> loadView("/views/ReponseCongeTable.fxml"));
        btnAjouterConge.setOnAction(e -> loadView("/views/ReponseConge.fxml"));

        // ── Show welcome dashboard ────────────────────────────
        showWelcomeDashboard();

        // ── Animate sidebar entrance ──────────────────────────
        animateSidebarEntrance();
    }

    // ═══════════════════════════════════════════════════════
    //  SIDEBAR STYLING
    // ═══════════════════════════════════════════════════════
    private void styleSidebar() {
        if (sidebar == null) return;
        sidebar.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #071A10, #050C07, #071A10); " +
                        "-fx-border-color: rgba(0,217,255,0.18) transparent rgba(0,217,255,0.18) transparent; " +
                        "-fx-border-width: 0 1 0 0; " +
                        "-fx-padding: 0 0 20 0;"
        );
        VBox.setVgrow(sidebar, Priority.ALWAYS);

        DropShadow sideGlow = new DropShadow();
        sideGlow.setColor(Color.web(C_CYAN, 0.12));
        sideGlow.setRadius(25);
        sideGlow.setOffsetX(6);
        sidebar.setEffect(sideGlow);
    }

    // ═══════════════════════════════════════════════════════
    //  SIDEBAR HEADER  (logo + clock + status dot)
    // ═══════════════════════════════════════════════════════
    private void buildSidebarHeader() {
        if (sidebar == null) return;

        // ── Logo block ────────────────────────────────────────
        VBox logoBlock = new VBox(4);
        logoBlock.setAlignment(Pos.CENTER);
        logoBlock.setPadding(new Insets(28, 16, 20, 16));
        logoBlock.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(0,217,255,0.06), transparent); " +
                        "-fx-border-color: transparent transparent rgba(0,217,255,0.12) transparent; " +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Animated logo circle
        StackPane logoCircle = new StackPane();
        Circle outerRing = new Circle(30);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web(C_CYAN, 0.35));
        outerRing.setStrokeWidth(1.5);

        Circle innerCircle = new Circle(24);
        innerCircle.setFill(Color.web(C_CYAN, 0.08));
        innerCircle.setStroke(Color.web(C_CYAN, 0.6));
        innerCircle.setStrokeWidth(2);

        Label logoIcon = new Label("⚡");
        logoIcon.setStyle("-fx-font-size: 22px;");

        logoCircle.getChildren().addAll(outerRing, innerCircle, logoIcon);

        // Rotate outer ring
        RotateTransition ringRotate = new RotateTransition(Duration.seconds(12), outerRing);
        ringRotate.setByAngle(360);
        ringRotate.setCycleCount(Animation.INDEFINITE);
        ringRotate.setInterpolator(Interpolator.LINEAR);
        ringRotate.play();

        // Pulse glow on inner circle
        Glow glow = new Glow(0.0);
        innerCircle.setEffect(glow);
        Timeline glowPulse = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(glow.levelProperty(), 0.1)),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(glow.levelProperty(), 0.7))
        );
        glowPulse.setAutoReverse(true);
        glowPulse.setCycleCount(Animation.INDEFINITE);
        glowPulse.play();

        Label appName = new Label("NEUROWELL");
        appName.setStyle(
                "-fx-text-fill: " + C_CYAN + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-letter-spacing: 3px;"
        );

        Label appTagline = new Label("Admin Panel");
        appTagline.setStyle("-fx-text-fill: rgba(0,217,255,0.40); -fx-font-size: 10px;");

        // Live clock
        Label clockLabel = new Label();
        clockLabel.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 11px;");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), ev ->
                clockLabel.setText(LocalDateTime.now().format(fmt))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        clockLabel.setText(LocalDateTime.now().format(fmt));

        // Status dot
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER);
        Circle statusDot = new Circle(4, Color.web(C_GREEN));
        DropShadow dotGlow = new DropShadow();
        dotGlow.setColor(Color.web(C_GREEN, 0.8)); dotGlow.setRadius(6);
        statusDot.setEffect(dotGlow);
        Label statusLabel = new Label("Système actif");
        statusLabel.setStyle("-fx-text-fill: rgba(0,255,136,0.60); -fx-font-size: 10px;");
        statusRow.getChildren().addAll(statusDot, statusLabel);

        logoBlock.getChildren().addAll(logoCircle, appName, appTagline, clockLabel, statusRow);
        sidebar.getChildren().add(0, logoBlock);
    }

    // ═══════════════════════════════════════════════════════
    //  WELCOME DASHBOARD (KPI cards + activity feed)
    // ═══════════════════════════════════════════════════════
    private void showWelcomeDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(28);
        content.setPadding(new Insets(30, 30, 30, 30));
        content.setStyle("-fx-background-color: transparent;");

        // ── Greeting ──────────────────────────────────────────
        HBox greeting = new HBox(12);
        greeting.setAlignment(Pos.CENTER_LEFT);
        Label greetIcon = new Label("👋");
        greetIcon.setStyle("-fx-font-size: 28px;");
        VBox greetText = new VBox(2);
        Label greetTitle = new Label("Bienvenue sur NEUROWELL");
        greetTitle.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label greetSub = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")));
        greetSub.setStyle("-fx-text-fill: rgba(0,217,255,0.50); -fx-font-size: 12px;");
        greetText.getChildren().addAll(greetTitle, greetSub);
        greeting.getChildren().addAll(greetIcon, greetText);

        // ── KPI Cards ─────────────────────────────────────────
        HBox kpiRow = new HBox(20);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        // ── CORRECTION : chargement réel des 3 compteurs ──────
        int nbEvenements = 0, nbParticipations = 0, nbEvaluations = 0, nbRessources = 0;
        try {
            nbEvenements     = serviceEvenement.recuperer().size();
            nbParticipations = serviceParticipation.recuperer().size();
            nbEvaluations    = serviceEvaluation.recuperer().size();
            nbRessources     = serviceRessource.recuperer().size();
        } catch (SQLException ignored) {}

        kpiRow.getChildren().addAll(
                buildKpiCard("🗓", "Événements",    String.valueOf(nbEvenements),    C_CYAN,    "+2 ce mois"),
                buildKpiCard("👥", "Participations", String.valueOf(nbParticipations), C_GREEN,  "actives"),
                buildKpiCard("📊", "Évaluations",   String.valueOf(nbEvaluations),   "#A78BFA", "en cours"),
                buildKpiCard("📚", "Ressources",     String.valueOf(nbRessources),    "#F59E0B", "publiées"),
                buildKpiCard("⭐", "Satisfaction",  "4.9/5",                          "#FFD700", "excellent")
        );

        // ── Quick Actions ─────────────────────────────────────
        Label qaTitle = new Label("⚡  Actions rapides");
        qaTitle.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        HBox qaRow = new HBox(16);
        qaRow.getChildren().addAll(
                buildQuickCard("➕", "Nouvel événement",   C_CYAN,    () -> loadView("/views/evenement.fxml")),
                buildQuickCard("📊", "Ajouter évaluation", "#A78BFA", () -> loadView("/views/AjoutEvaluation.fxml")),
                buildQuickCard("📚", "Ajouter ressource",  "#F59E0B", () -> loadView("/views/AjoutRessource.fxml")),
                buildQuickCard("📋", "Voir événements",    C_GREEN,   () -> loadView("/views/EvenementsTable.fxml")),
                buildQuickCard("📑", "Voir évaluations",  "#FFD700",  () -> loadView("/views/AfficherEvaluation.fxml")),
                buildQuickCard("📖", "Voir ressources",   "#F59E0B",  () -> loadView("/views/AfficherRessource.fxml"))
        );

        // ── Activity Feed ─────────────────────────────────────
        Label feedTitle = new Label("📡  Activité récente");
        feedTitle.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        VBox feed = buildActivityFeed();

        content.getChildren().addAll(greeting, kpiRow, qaTitle, qaRow, feedTitle, feed);
        scroll.setContent(content);
        contentPane.getChildren().setAll(scroll);
    }

    private VBox buildKpiCard(String emoji, String label, String value, String color, String sub) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setPrefWidth(220);
        card.setStyle(
                "-fx-background-color: #0D1F12; " +
                        "-fx-border-color: " + color + "44; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18;"
        );

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 24px;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.80); -fx-font-size: 13px; -fx-font-weight: 600;");

        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-text-fill: rgba(0,217,255,0.40); -fx-font-size: 11px;");

        card.getChildren().addAll(emojiLbl, valLbl, nameLbl, subLbl);

        // Hover glow
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #112016; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, " + color + "44, 20, 0.2, 0, 0);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #0D1F12; " +
                        "-fx-border-color: " + color + "44; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18;"
        ));
        return card;
    }

    private VBox buildQuickCard(String emoji, String label, String color, Runnable action) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22, 28, 22, 28));
        card.setStyle(
                "-fx-background-color: #112016; " +
                        "-fx-border-color: rgba(0,217,255,0.12); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18; " +
                        "-fx-cursor: hand;"
        );

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 26px;");

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.80); -fx-font-size: 12px; -fx-font-weight: 600;");

        card.getChildren().addAll(emojiLbl, nameLbl);
        card.setOnMouseClicked(e -> action.run());
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #0D1F12; " +
                        "-fx-border-color: " + color + "; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, " + color + "44, 22, 0.2, 0, 0);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #112016; " +
                        "-fx-border-color: rgba(0,217,255,0.12); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18; " +
                        "-fx-cursor: hand;"
        ));
        return card;
    }

    private VBox buildActivityFeed() {
        VBox feed = new VBox(0);
        feed.setStyle(
                "-fx-background-color: #0D1F12; " +
                        "-fx-border-color: rgba(0,217,255,0.13); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 16; " +
                        "-fx-background-radius: 16;"
        );

        String[][] entries = {
                {"🗓", C_CYAN,    "Nouvel événement créé",             "il y a 2 min"},
                {"📊", "#A78BFA", "Évaluation ajoutée — Stress",        "il y a 10 min"},
                {"👥", C_GREEN,   "Nouvelle participation enregistrée",  "il y a 25 min"},
                {"📊", "#A78BFA", "Évaluation modifiée — Anxiété",       "il y a 1 h"},
                {"🗓", C_CYAN,    "Événement mis à jour",               "il y a 2 h"},
        };

        for (int i = 0; i < entries.length; i++) {
            HBox row = buildFeedRow(entries[i][0], entries[i][1], entries[i][2], entries[i][3]);
            if (i < entries.length - 1) {
                row.setStyle(
                        "-fx-border-color: transparent transparent rgba(0,217,255,0.07) transparent; " +
                                "-fx-border-width: 0 0 1 0;"
                );
            }
            feed.getChildren().add(row);

            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), row);
            ft.setDelay(Duration.millis(200L * i));
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }

        return feed;
    }

    private HBox buildFeedRow(String emoji, String color, String text, String time) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));

        Circle dot = new Circle(5, Color.web(color));
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(color, 0.6)); ds.setRadius(6);
        dot.setEffect(ds);

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 15px;");

        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.80); -fx-font-size: 13px;");
        HBox.setHgrow(textLbl, Priority.ALWAYS);

        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-text-fill: rgba(0,217,255,0.40); -fx-font-size: 11px;");

        row.getChildren().addAll(dot, emojiLbl, textLbl, timeLbl);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //  NAV BUTTON STYLING
    // ═══════════════════════════════════════════════════════
    private void styleNavButton(Button btn, String emoji, String label) {
        if (btn == null) return;
        btn.setText(emoji + "   " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        String base =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: rgba(200,255,220,0.75); " +
                        "-fx-font-size: 13px; -fx-font-weight: 600; " +
                        "-fx-padding: 12 20; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-color: transparent;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(0,217,255,0.08); " +
                        "-fx-text-fill: " + C_CYAN + "; " +
                        "-fx-font-size: 13px; -fx-font-weight: 600; " +
                        "-fx-padding: 12 20; -fx-cursor: hand; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-color: transparent transparent transparent " + C_CYAN + "; " +
                        "-fx-border-width: 0 0 0 3;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleSubButton(Button btn, String emoji, String label) {
        if (btn == null) return;
        btn.setText("     " + emoji + "  " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        String base =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: rgba(200,255,220,0.50); " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 9 20; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(0,255,136,0.06); " +
                        "-fx-text-fill: " + C_GREEN + "; " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 9 20; -fx-cursor: hand; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-color: transparent transparent transparent " + C_GREEN + "; " +
                        "-fx-border-width: 0 0 0 2;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    // ═══════════════════════════════════════════════════════
    //  SUBMENU TOGGLE
    // ═══════════════════════════════════════════════════════
    private void hideSubMenu(VBox menu) {
        if (menu == null) return;
        menu.setVisible(false);
        menu.setManaged(false);
        menu.setOpacity(0);
    }

    private void toggleMenu(VBox menu) {
        if (menu == null) return;
        boolean isVisible = menu.isVisible();
        if (!isVisible) {
            menu.setVisible(true);
            menu.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), menu);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), menu);
            tt.setFromY(-8); tt.setToY(0); tt.play();
        } else {
            FadeTransition ft = new FadeTransition(Duration.millis(150), menu);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> { menu.setVisible(false); menu.setManaged(false); });
            ft.play();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════
    private void animateSidebarEntrance() {
        if (sidebar == null) return;
        sidebar.setTranslateX(-220);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), sidebar);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        tt.play();
    }

    private void pulseButton(Button btn) {
        if (btn == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
        st.setToX(0.95); st.setToY(0.95);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════
    private void loadView(String fxmlPath) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(page);

            FadeTransition ft = new FadeTransition(Duration.millis(350), page);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            TranslateTransition tt = new TranslateTransition(Duration.millis(350), page);
            tt.setFromY(15); tt.setToY(0); tt.play();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorBanner("❌  Impossible de charger : " + fxmlPath);
        }
    }

    private void showErrorBanner(String message) {
        Label banner = new Label(message);
        banner.setStyle(
                "-fx-background-color: rgba(255,77,109,0.15); " +
                        "-fx-border-color: " + C_DANGER + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-text-fill: " + C_DANGER + "; " +
                        "-fx-font-size: 13px; -fx-padding: 14 22;"
        );
        contentPane.getChildren().setAll(banner);
    }

    @FXML
    private void openFrontPage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/front.fxml"));
            Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300),
                    stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setScene(new Scene(root));
                stage.setTitle("Front Office");
                stage.show();
            });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorBanner("❌  Erreur chargement Front Office");
        }
    }
}
