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

    // Nav buttons
    @FXML private Button btnEvenements;
    @FXML private VBox   evenementSubMenu;
    @FXML private Button btnAfficherEvenements;
    @FXML private Button btnAjouterEvenement;

    @FXML private Button btnConges;
    @FXML private VBox   congeSubMenu;
    @FXML private Button btnAjouterConge;
    @FXML private Button btnAfficherConge;

    @FXML private Button btnFront;

    // ═══════════════════════════════════════════════════════
    //  SERVICES
    // ═══════════════════════════════════════════════════════
    private final ServiceEvenement     serviceEvenement     = new ServiceEvenement();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();

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
        styleNavButton(btnEvenements,        "🗓", "Événements");
        styleNavButton(btnConges,            "🏖", "Congés");
        styleNavButton(btnFront,             "🌐", "Front Office");
        styleSubButton(btnAfficherEvenements,"📋", "Afficher");
        styleSubButton(btnAjouterEvenement,  "➕", "Ajouter");
        styleSubButton(btnAfficherConge,     "📋", "Afficher");
        styleSubButton(btnAjouterConge,      "➕", "Ajouter");

        // ── Submenus hidden ───────────────────────────────────
        hideSubMenu(evenementSubMenu);
        hideSubMenu(congeSubMenu);

        // ── Wire actions ──────────────────────────────────────
        btnFront.setOnAction(this::openFrontPage);

        btnEvenements.setOnAction(e -> {
            toggleMenu(evenementSubMenu);
            hideSubMenu(congeSubMenu);
            pulseButton(btnEvenements);
        });
        btnConges.setOnAction(e -> {
            toggleMenu(congeSubMenu);
            hideSubMenu(evenementSubMenu);
            pulseButton(btnConges);
        });

        btnAfficherEvenements.setOnAction(e -> loadView("/views/EvenementsTable.fxml"));
        btnAjouterEvenement.setOnAction(e -> loadView("/views/evenement.fxml"));
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
                new KeyFrame(Duration.ZERO,       new KeyValue(glow.levelProperty(), 0.1)),
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

        Label appSub = new Label("Admin Dashboard");
        appSub.setStyle(
                "-fx-text-fill: rgba(0,217,255,0.45); " +
                        "-fx-font-size: 10px; " +
                        "-fx-letter-spacing: 1.5px;"
        );

        // Live clock
        Label clockLabel = new Label();
        clockLabel.setStyle(
                "-fx-text-fill: rgba(200,255,220,0.60); " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-family: 'Consolas', monospace;"
        );

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            String time = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss  |  dd/MM/yyyy"));
            clockLabel.setText(time);
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        clockLabel.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss  |  dd/MM/yyyy")));

        // Online status indicator
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER);
        Circle statusDot = new Circle(4, Color.web(C_GREEN));
        FadeTransition dotBlink = new FadeTransition(Duration.millis(800), statusDot);
        dotBlink.setFromValue(0.3); dotBlink.setToValue(1.0);
        dotBlink.setCycleCount(Animation.INDEFINITE); dotBlink.setAutoReverse(true);
        dotBlink.play();
        Label statusText = new Label("Système en ligne");
        statusText.setStyle("-fx-text-fill: rgba(0,255,136,0.55); -fx-font-size: 10px;");
        statusRow.getChildren().addAll(statusDot, statusText);

        logoBlock.getChildren().addAll(logoCircle, appName, appSub, clockLabel, statusRow);

        // Insert at top of sidebar
        sidebar.getChildren().add(0, logoBlock);

        // ── Stats strip ───────────────────────────────────────
        HBox statsStrip = buildMiniStatsStrip();
        statsStrip.setPadding(new Insets(14, 12, 14, 12));
        sidebar.getChildren().add(1, statsStrip);
    }

    /** Two mini KPI tiles in the sidebar */
    private HBox buildMiniStatsStrip() {
        int evCount = 0, partCount = 0;
        try { evCount   = serviceEvenement.recuperer().size(); }   catch (SQLException ignored) {}
        try { partCount = serviceParticipation.recuperer().size(); } catch (SQLException ignored) {}

        HBox strip = new HBox(8);
        strip.setAlignment(Pos.CENTER);
        strip.getChildren().addAll(
                miniKpi("🗓", String.valueOf(evCount),   "Événements"),
                miniKpi("👥", String.valueOf(partCount), "Participations")
        );
        return strip;
    }

    private VBox miniKpi(String emoji, String value, String label) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setStyle(
                "-fx-background-color: rgba(0,217,255,0.06); " +
                        "-fx-border-color: rgba(0,217,255,0.18); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-background-radius: 12;"
        );
        HBox.setHgrow(box, Priority.ALWAYS);

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 16px;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle(
                "-fx-text-fill: " + C_CYAN + "; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold;"
        );

        // Count-up animation
        try {
            int target = Integer.parseInt(value);
            Timeline countUp = new Timeline();
            for (int i = 0; i <= target; i++) {
                final int v = i;
                countUp.getKeyFrames().add(
                        new KeyFrame(Duration.millis(600.0 / Math.max(target, 1) * i),
                                e -> valueLbl.setText(String.valueOf(v)))
                );
            }
            countUp.play();
        } catch (NumberFormatException ignored) {}

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 9px;");

        box.getChildren().addAll(emojiLbl, valueLbl, labelLbl);
        return box;
    }

    // ═══════════════════════════════════════════════════════
    //  WELCOME DASHBOARD  (shown in contentPane on launch)
    // ═══════════════════════════════════════════════════════
    private void showWelcomeDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #050C07;");

        VBox dashboard = new VBox(28);
        dashboard.setPadding(new Insets(36, 40, 36, 40));
        dashboard.setStyle("-fx-background-color: #050C07;");

        // ── Greeting ─────────────────────────────────────────
        Label greeting = new Label("Bienvenue, Administrateur 👋");
        greeting.setStyle(
                "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-font-size: 28px; " +
                        "-fx-font-weight: bold;"
        );
        Label subGreeting = new Label(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"))
                        + "  —  Tableau de bord principal"
        );
        subGreeting.setStyle(
                "-fx-text-fill: rgba(200,255,220,0.45); " +
                        "-fx-font-size: 13px;"
        );

        // ── KPI Cards row ─────────────────────────────────────
        HBox kpiRow = buildKpiRow();

        // ── Section label ─────────────────────────────────────
        Label sectionLabel = new Label("⚡  Accès Rapide");
        sectionLabel.setStyle(
                "-fx-text-fill: " + C_CYAN + "; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-letter-spacing: 1px;"
        );

        // ── Quick action cards ────────────────────────────────
        HBox quickActions = buildQuickActions();

        // ── Activity feed ─────────────────────────────────────
        Label feedLabel = new Label("📡  Activité Récente");
        feedLabel.setStyle(
                "-fx-text-fill: " + C_CYAN + "; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-letter-spacing: 1px;"
        );
        VBox activityFeed = buildActivityFeed();

        dashboard.getChildren().addAll(
                greeting, subGreeting,
                kpiRow,
                sectionLabel, quickActions,
                feedLabel, activityFeed
        );

        scroll.setContent(dashboard);
        contentPane.getChildren().setAll(scroll);

        // Animate entrance
        FadeTransition ft = new FadeTransition(Duration.millis(600), dashboard);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), dashboard);
        tt.setFromY(20); tt.setToY(0); tt.play();
    }

    /** 4 KPI stat cards in a row */
    private HBox buildKpiRow() {
        int evCount = 0, partCount = 0;
        try { evCount   = serviceEvenement.recuperer().size(); }   catch (SQLException ignored) {}
        try { partCount = serviceParticipation.recuperer().size(); } catch (SQLException ignored) {}

        int validatedCount = 0;
        try {
            validatedCount = (int) serviceEvenement.recuperer().stream()
                    .filter(e -> "Validé".equalsIgnoreCase(e.getStatut_e()))
                    .count();
        } catch (SQLException ignored) {}

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(
                kpiCard("🗓", String.valueOf(evCount),      "Événements",        C_CYAN,   "total"),
                kpiCard("✔", String.valueOf(validatedCount),"Validés",           C_GREEN,  "actifs"),
                kpiCard("👥", String.valueOf(partCount),    "Participations",    C_CYAN,   "inscrits"),
                kpiCard("📊", "100%",                       "Système",           C_GREEN,  "opérationnel")
        );

        for (Node card : row.getChildren()) HBox.setHgrow(card, Priority.ALWAYS);
        return row;
    }

    private VBox kpiCard(String emoji, String value, String label, String color, String sub) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20, 22, 20, 22));
        card.setStyle(
                "-fx-background-color: " + C_CARD + "; " +
                        "-fx-border-color: rgba(0,217,255,0.14); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18;"
        );

        // Top row: emoji + colored bar
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 24px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Rectangle bar = new Rectangle(3, 36);
        bar.setArcWidth(3); bar.setArcHeight(3);
        bar.setFill(Color.web(color));
        topRow.getChildren().addAll(emojiLbl, spacer, bar);

        Label valueLbl = new Label(value);
        valueLbl.setStyle(
                "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 32px; " +
                        "-fx-font-weight: bold;"
        );

        // Count-up animation
        try {
            int target = Integer.parseInt(value);
            if (target > 0) {
                Timeline cu = new Timeline();
                int steps = Math.min(target, 30);
                for (int i = 0; i <= steps; i++) {
                    final int v = (int)((double)i / steps * target);
                    cu.getKeyFrames().add(new KeyFrame(Duration.millis(800.0 / steps * i),
                            e -> valueLbl.setText(String.valueOf(v))));
                }
                cu.getKeyFrames().add(new KeyFrame(Duration.millis(820),
                        e -> valueLbl.setText(value)));
                cu.play();
            }
        } catch (NumberFormatException ignored) {}

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 12px;");

        Label subLbl = new Label("● " + sub);
        subLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-opacity: 0.7;");

        card.getChildren().addAll(topRow, valueLbl, labelLbl, subLbl);

        // Hover glow
        DropShadow normalShadow = new DropShadow();
        normalShadow.setColor(Color.web(color, 0.10));
        normalShadow.setRadius(15);
        card.setEffect(normalShadow);

        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: " + C_CARD + "; " +
                            "-fx-border-color: " + color + "; " +
                            "-fx-border-width: 1.5; " +
                            "-fx-border-radius: 18; " +
                            "-fx-background-radius: 18;"
            );
            DropShadow hover = new DropShadow();
            hover.setColor(Color.web(color, 0.35));
            hover.setRadius(25);
            card.setEffect(hover);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: " + C_CARD + "; " +
                            "-fx-border-color: rgba(0,217,255,0.14); " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 18; " +
                            "-fx-background-radius: 18;"
            );
            card.setEffect(normalShadow);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    /** 3 large clickable shortcut cards */
    private HBox buildQuickActions() {
        HBox row = new HBox(14);

        VBox c1 = quickActionCard("📋", "Voir Événements",
                "Consulter et gérer\ntous les événements", C_CYAN,
                () -> loadView("/views/EvenementsTable.fxml"));

        VBox c2 = quickActionCard("➕", "Ajouter Événement",
                "Créer un nouvel\névénement", C_GREEN,
                () -> loadView("/views/evenement.fxml"));

        VBox c3 = quickActionCard("🌐", "Front Office",
                "Accéder à l'espace\nvisiteurs", "#7FFF00",
                () -> btnFront.fire());

        row.getChildren().addAll(c1, c2, c3);
        for (Node c : row.getChildren()) HBox.setHgrow(c, Priority.ALWAYS);
        return row;
    }

    private VBox quickActionCard(String emoji, String title, String desc,
                                 String color, Runnable action) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-border-color: rgba(0,217,255,0.12); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 18; " +
                        "-fx-background-radius: 18;"
        );

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 32px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: bold;"
        );

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 11px;");
        descLbl.setWrapText(true);

        // Arrow indicator
        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-opacity: 0.5;");

        card.getChildren().addAll(emojiLbl, titleLbl, descLbl, arrow);
        card.setOnMouseClicked(e -> action.run());

        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: " + C_CARD + "; " +
                            "-fx-border-color: " + color + "; " +
                            "-fx-border-width: 1.5; " +
                            "-fx-border-radius: 18; " +
                            "-fx-background-radius: 18;"
            );
            arrow.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-opacity: 1.0;");
            DropShadow ds = new DropShadow();
            ds.setColor(Color.web(color, 0.30)); ds.setRadius(22);
            card.setEffect(ds);
            ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: " + C_SURFACE + "; " +
                            "-fx-border-color: rgba(0,217,255,0.12); " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 18; " +
                            "-fx-background-radius: 18;"
            );
            arrow.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-opacity: 0.5;");
            card.setEffect(null);
            ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    /** Simulated activity feed */
    private VBox buildActivityFeed() {
        VBox feed = new VBox(0);
        feed.setStyle(
                "-fx-background-color: " + C_CARD + "; " +
                        "-fx-border-color: rgba(0,217,255,0.13); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 16; " +
                        "-fx-background-radius: 16;"
        );

        String[][] activities = {
                {"✔", C_GREEN,  "Système initialisé",             "maintenant"},
                {"🗓", C_CYAN,  "Événements chargés depuis la BDD","il y a 1s"},
                {"👥", C_CYAN,  "Participations récupérées",       "il y a 2s"},
                {"⚡", "#7FFF00","Dashboard prêt",                  "il y a 3s"}
        };

        for (int i = 0; i < activities.length; i++) {
            String[] a = activities[i];
            HBox row = buildFeedRow(a[0], a[1], a[2], a[3]);
            if (i < activities.length - 1) {
                row.setStyle(row.getStyle() +
                        "-fx-border-color: transparent transparent rgba(0,217,255,0.07) transparent; " +
                        "-fx-border-width: 0 0 1 0;"
                );
            }
            feed.getChildren().add(row);

            // Staggered fade-in
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

        // Dot indicator
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
    //  SUBMENU TOGGLE  (with slide animation)
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