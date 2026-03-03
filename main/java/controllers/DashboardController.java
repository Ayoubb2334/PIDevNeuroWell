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
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvenement;
import services.ServiceParticipation;
import services.ServiceEvaluation;
import services.ServiceRessource;
import services.ServiceConsultation;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    private static final String C_CYAN    = "#00D9FF";
    private static final String C_GREEN   = "#00FF88";
    private static final String C_DARK    = "#050C07";
    private static final String C_CARD    = "#0D1F12";
    private static final String C_DANGER  = "#FF4D6D";
    private static final String C_TEXT    = "#E8FFF0";

    @FXML private StackPane contentPane;
    @FXML private VBox      sidebar;

    // Événements
    @FXML private Button btnEvenements;
    @FXML private VBox   evenementSubMenu;
    @FXML private Button btnAfficherEvenements;
    @FXML private Button btnAjouterEvenement;

    // Évaluation
    @FXML private Button btnEvaluation;
    @FXML private VBox   evaluationSubMenu;
    @FXML private Button btnAjouterEvaluation;
    @FXML private Button btnAfficherEvaluation;

    // Ressources
    @FXML private Button btnRessources;
    @FXML private VBox   ressourceSubMenu;
    @FXML private Button btnAjouterRessource;
    @FXML private Button btnAfficherRessource;

    // Congés
    @FXML private Button btnConges;
    @FXML private VBox   congeSubMenu;
    @FXML private Button btnAjouterConge;
    @FXML private Button btnAfficherConge;

    // ── Consultations (NOUVEAU) ───────────────────────────
    @FXML private Button btnConsultations;
    @FXML private VBox   consultationSubMenu;
    @FXML private Button btnAfficherConsultations;
    @FXML private Button btnComptesRendus;
    // ──────────────────────────────────────────────────────

    @FXML private Button btnFront;
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;

    private final ServiceEvenement     serviceEvenement     = new ServiceEvenement();
    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private final ServiceEvaluation    serviceEvaluation    = new ServiceEvaluation();
    private final ServiceRessource     serviceRessource     = new ServiceRessource();
    private final ServiceConsultation  serviceConsultation  = new ServiceConsultation(); // NOUVEAU

    @FXML
    public void initialize() {
        styleSidebar();
        buildSidebarHeader();

        styleNavButton(btnDashboard,     "⚡", "Tableau de bord");
        styleNavButton(btnEvenements,    "🗓", "Événements");
        styleNavButton(btnEvaluation,    "📊", "Évaluation");
        styleNavButton(btnRessources,    "📚", "Ressources");
        styleNavButton(btnConges,        "🏖", "Congés");
        styleNavButton(btnConsultations, "🧠", "Consultations");  // NOUVEAU
        styleNavButton(btnFront,         "🌐", "Front Office");
        styleNavButton(btnUsers,         "👤", "Utilisateurs");

        styleSubButton(btnAfficherEvenements,    "📋", "Afficher");
        styleSubButton(btnAjouterEvenement,      "➕", "Ajouter");
        styleSubButton(btnAfficherEvaluation,    "📋", "Afficher");
        styleSubButton(btnAjouterEvaluation,     "➕", "Ajouter");
        styleSubButton(btnAfficherRessource,     "📋", "Afficher");
        styleSubButton(btnAjouterRessource,      "➕", "Ajouter");
        styleSubButton(btnAfficherConge,         "📋", "Afficher");
        styleSubButton(btnAjouterConge,          "➕", "Ajouter");
        styleSubButton(btnAfficherConsultations, "📋", "Toutes les consultations"); // NOUVEAU
        styleSubButton(btnComptesRendus,         "📝", "Comptes rendus");           // NOUVEAU

        hideSubMenu(evenementSubMenu);
        hideSubMenu(evaluationSubMenu);
        hideSubMenu(ressourceSubMenu);
        hideSubMenu(congeSubMenu);
        hideSubMenu(consultationSubMenu); // NOUVEAU

        btnFront.setOnAction(this::openFrontPage);
        if (btnDashboard != null) btnDashboard.setOnAction(e -> showWelcomeDashboard());
        if (btnUsers     != null) btnUsers.setOnAction(e -> loadView("/views/UsersAdmin.fxml"));

        btnEvenements.setOnAction(e -> {
            toggleMenu(evenementSubMenu);
            hideAllSubMenusExcept(evenementSubMenu);
            pulseButton(btnEvenements);
        });
        btnEvaluation.setOnAction(e -> {
            toggleMenu(evaluationSubMenu);
            hideAllSubMenusExcept(evaluationSubMenu);
            pulseButton(btnEvaluation);
        });
        btnRessources.setOnAction(e -> {
            toggleMenu(ressourceSubMenu);
            hideAllSubMenusExcept(ressourceSubMenu);
            pulseButton(btnRessources);
        });
        btnConges.setOnAction(e -> {
            toggleMenu(congeSubMenu);
            hideAllSubMenusExcept(congeSubMenu);
            pulseButton(btnConges);
        });
        if (btnConsultations != null) btnConsultations.setOnAction(e -> { // NOUVEAU
            toggleMenu(consultationSubMenu);
            hideAllSubMenusExcept(consultationSubMenu);
            pulseButton(btnConsultations);
        });

        btnAfficherEvenements.setOnAction(e -> loadView("/views/EvenementsTable.fxml"));
        btnAjouterEvenement.setOnAction(e   -> loadView("/views/evenement.fxml"));
        btnAfficherEvaluation.setOnAction(e -> loadView("/views/AfficherEvaluation.fxml"));
        btnAjouterEvaluation.setOnAction(e  -> loadView("/views/AjoutEvaluation.fxml"));
        btnAfficherRessource.setOnAction(e  -> loadView("/views/AfficherRessource.fxml"));
        btnAjouterRessource.setOnAction(e   -> loadView("/views/AjoutRessource.fxml"));
        btnAfficherConge.setOnAction(e      -> loadView("/views/ReponseCongeTable.fxml"));
        btnAjouterConge.setOnAction(e       -> loadView("/views/ReponseConge.fxml"));

        // NOUVEAU
        if (btnAfficherConsultations != null)
            btnAfficherConsultations.setOnAction(e -> loadView("/views/ConsultationAdmin.fxml"));
        if (btnComptesRendus != null)
            btnComptesRendus.setOnAction(e -> loadView("/views/ConsultationAdmin.fxml"));

        showWelcomeDashboard();
        animateSidebarEntrance();
    }

    // Helper: ferme tous les sous-menus sauf celui ciblé
    private void hideAllSubMenusExcept(VBox except) {
        VBox[] all = {evenementSubMenu, evaluationSubMenu, ressourceSubMenu, congeSubMenu, consultationSubMenu};
        for (VBox m : all) if (m != except) hideSubMenu(m);
    }

    // ─── Sidebar ──────────────────────────────────────────
    private void styleSidebar() {
        if (sidebar == null) return;
        sidebar.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #071A10, #050C07, #071A10); " +
                        "-fx-border-color: rgba(0,217,255,0.18) transparent rgba(0,217,255,0.18) transparent; " +
                        "-fx-border-width: 0 1 0 0; -fx-padding: 0 0 20 0;"
        );
        VBox.setVgrow(sidebar, Priority.ALWAYS);
        DropShadow sg = new DropShadow();
        sg.setColor(Color.web(C_CYAN, 0.12)); sg.setRadius(25); sg.setOffsetX(6);
        sidebar.setEffect(sg);
    }

    private void buildSidebarHeader() {
        if (sidebar == null) return;
        VBox logoBlock = new VBox(4);
        logoBlock.setAlignment(Pos.CENTER);
        logoBlock.setPadding(new Insets(28, 16, 20, 16));
        logoBlock.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(0,217,255,0.06), rgba(0,0,0,0)); " +
                        "-fx-border-color: transparent transparent rgba(0,217,255,0.12) transparent; -fx-border-width: 0 0 1 0;"
        );
        StackPane logoCircle = new StackPane();
        Circle outerRing = new Circle(30); outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web(C_CYAN, 0.35)); outerRing.setStrokeWidth(1.5);
        Circle innerCircle = new Circle(24); innerCircle.setFill(Color.web(C_CYAN, 0.08));
        innerCircle.setStroke(Color.web(C_CYAN, 0.6)); innerCircle.setStrokeWidth(2);
        Label logoIcon = new Label("⚡"); logoIcon.setStyle("-fx-font-size: 22px;");
        logoCircle.getChildren().addAll(outerRing, innerCircle, logoIcon);
        RotateTransition rr = new RotateTransition(Duration.seconds(12), outerRing);
        rr.setByAngle(360); rr.setCycleCount(Animation.INDEFINITE); rr.setInterpolator(Interpolator.LINEAR); rr.play();
        Glow glow = new Glow(0.0); innerCircle.setEffect(glow);
        Timeline gp = new Timeline(
                new KeyFrame(Duration.ZERO,         new KeyValue(glow.levelProperty(), 0.1)),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(glow.levelProperty(), 0.7))
        );
        gp.setAutoReverse(true); gp.setCycleCount(Animation.INDEFINITE); gp.play();
        Label appName = new Label("NEUROWELL");
        appName.setStyle("-fx-text-fill:"+C_CYAN+"; -fx-font-size:15px; -fx-font-weight:bold; -fx-letter-spacing:3px;");
        Label appTagline = new Label("Admin Panel");
        appTagline.setStyle("-fx-text-fill: rgba(0,217,255,0.40); -fx-font-size: 10px;");
        Label clockLabel = new Label();
        clockLabel.setStyle("-fx-text-fill: rgba(200,255,220,0.55); -fx-font-size: 11px;");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), ev -> clockLabel.setText(LocalDateTime.now().format(fmt))));
        clock.setCycleCount(Animation.INDEFINITE); clock.play();
        clockLabel.setText(LocalDateTime.now().format(fmt));
        HBox statusRow = new HBox(6); statusRow.setAlignment(Pos.CENTER);
        Circle statusDot = new Circle(4, Color.web(C_GREEN));
        DropShadow dg = new DropShadow(); dg.setColor(Color.web(C_GREEN, 0.8)); dg.setRadius(6); statusDot.setEffect(dg);
        Label statusLabel = new Label("Système actif");
        statusLabel.setStyle("-fx-text-fill: rgba(0,255,136,0.60); -fx-font-size: 10px;");
        statusRow.getChildren().addAll(statusDot, statusLabel);
        logoBlock.getChildren().addAll(logoCircle, appName, appTagline, clockLabel, statusRow);
        sidebar.getChildren().add(0, logoBlock);
    }

    // ─── Welcome Dashboard ────────────────────────────────
    private void showWelcomeDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox content = new VBox(28);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: transparent;");

        // Greeting
        HBox greeting = new HBox(12); greeting.setAlignment(Pos.CENTER_LEFT);
        Label greetIcon = new Label("👋"); greetIcon.setStyle("-fx-font-size: 28px;");
        VBox greetText = new VBox(2);
        Label greetTitle = new Label("Bienvenue sur NEUROWELL");
        greetTitle.setStyle("-fx-text-fill:"+C_TEXT+"; -fx-font-size:22px; -fx-font-weight:bold;");
        Label greetSub = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")));
        greetSub.setStyle("-fx-text-fill: rgba(0,217,255,0.50); -fx-font-size: 12px;");
        greetText.getChildren().addAll(greetTitle, greetSub);
        greeting.getChildren().addAll(greetIcon, greetText);

        // KPI
        HBox kpiRow = new HBox(16); kpiRow.setAlignment(Pos.CENTER_LEFT);
        int nbEv = 0, nbPart = 0, nbEval = 0, nbRes = 0, nbConsult = 0;
        try {
            nbEv      = serviceEvenement.recuperer().size();
            nbPart    = serviceParticipation.recuperer().size();
            nbEval    = serviceEvaluation.recuperer().size();
            nbRes     = serviceRessource.recuperer().size();
            nbConsult = serviceConsultation.countTotal();
        } catch (SQLException ignored) {}
        kpiRow.getChildren().addAll(
                buildKpiCard("🗓", "Événements",    String.valueOf(nbEv),      C_CYAN,    "+2 ce mois"),
                buildKpiCard("👥", "Participations", String.valueOf(nbPart),    C_GREEN,   "actives"),
                buildKpiCard("📊", "Évaluations",   String.valueOf(nbEval),    "#A78BFA", "en cours"),
                buildKpiCard("📚", "Ressources",     String.valueOf(nbRes),     "#F59E0B", "publiées"),
                buildKpiCard("🧠", "Consultations",  String.valueOf(nbConsult), C_CYAN,    "total")
        );

        // Quick Actions
        Label qaTitle = new Label("⚡  Actions rapides");
        qaTitle.setStyle("-fx-text-fill:"+C_TEXT+"; -fx-font-size:15px; -fx-font-weight:bold;");
        HBox qaRow = new HBox(14);
        qaRow.getChildren().addAll(
                buildQuickCard("➕", "Nouvel événement",    C_CYAN,    () -> loadView("/views/evenement.fxml")),
                buildQuickCard("📊", "Ajouter évaluation", "#A78BFA", () -> loadView("/views/AjoutEvaluation.fxml")),
                buildQuickCard("📚", "Ajouter ressource",  "#F59E0B", () -> loadView("/views/AjoutRessource.fxml")),
                buildQuickCard("📋", "Voir événements",    C_GREEN,   () -> loadView("/views/EvenementsTable.fxml")),
                buildQuickCard("🧠", "Consultations",      C_CYAN,    () -> loadView("/views/ConsultationAdmin.fxml")),
                buildQuickCard("👤", "Utilisateurs",       "#FF4D6D", () -> loadView("/views/UsersAdmin.fxml"))
        );

        // ── Consultations Stats Section (NOUVEAU) ─────────
        Label consultTitle = new Label("🧠  Consultations — aperçu");
        consultTitle.setStyle("-fx-text-fill:"+C_TEXT+"; -fx-font-size:15px; -fx-font-weight:bold;");
        HBox consultRow = new HBox(14);
        try {
            consultRow.getChildren().addAll(
                    buildConsultMiniCard("📅", "Planifiées",  serviceConsultation.countByStatut("planifiee"), C_CYAN),
                    buildConsultMiniCard("⏳", "En cours",    serviceConsultation.countByStatut("en_cours"),  "#FFB347"),
                    buildConsultMiniCard("✅", "Terminées",   serviceConsultation.countByStatut("terminee"),  C_GREEN),
                    buildConsultMiniCard("❌", "Annulées",    serviceConsultation.countByStatut("annulee"),   "#FF4D6D")
            );
        } catch (SQLException ignored) {}

        // Activity Feed
        Label feedTitle = new Label("📡  Activité récente");
        feedTitle.setStyle("-fx-text-fill:"+C_TEXT+"; -fx-font-size:15px; -fx-font-weight:bold;");

        content.getChildren().addAll(greeting, kpiRow, qaTitle, qaRow, consultTitle, consultRow, feedTitle, buildActivityFeed());
        scroll.setContent(content);
        contentPane.getChildren().setAll(scroll);
    }

    private VBox buildConsultMiniCard(String emoji, String label, int count, String color) {
        VBox card = new VBox(6); card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 22, 16, 22)); card.setPrefWidth(190);
        card.setStyle("-fx-background-color: #0D1F12; -fx-border-color:"+color+"44; -fx-border-width:1; " +
                "-fx-border-radius:16; -fx-background-radius:16; -fx-cursor:hand;");
        HBox top = new HBox(10); top.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(emoji); ico.setStyle("-fx-font-size:18px;");
        Label cnt = new Label(String.valueOf(count));
        cnt.setStyle("-fx-text-fill:"+color+"; -fx-font-size:26px; -fx-font-weight:bold;");
        top.getChildren().addAll(ico, cnt);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: rgba(200,255,220,0.65); -fx-font-size:12px; -fx-font-weight:600;");
        card.getChildren().addAll(top, lbl);
        card.setOnMouseClicked(e -> loadView("/views/ConsultationAdmin.fxml"));
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#112016; -fx-border-color:"+color+"; " +
                "-fx-border-width:1.5; -fx-border-radius:16; -fx-background-radius:16; -fx-cursor:hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#0D1F12; -fx-border-color:"+color+"44; " +
                "-fx-border-width:1; -fx-border-radius:16; -fx-background-radius:16; -fx-cursor:hand;"));
        return card;
    }

    private VBox buildKpiCard(String emoji, String label, String value, String color, String sub) {
        VBox card = new VBox(8); card.setPadding(new Insets(20, 22, 20, 22)); card.setPrefWidth(200);
        card.setStyle("-fx-background-color:#0D1F12; -fx-border-color:"+color+"44; -fx-border-width:1; -fx-border-radius:18; -fx-background-radius:18;");
        Label e1 = new Label(emoji); e1.setStyle("-fx-font-size:24px;");
        Label v  = new Label(value); v.setStyle("-fx-text-fill:"+color+"; -fx-font-size:30px; -fx-font-weight:bold;");
        Label n  = new Label(label); n.setStyle("-fx-text-fill:rgba(200,255,220,0.80); -fx-font-size:13px; -fx-font-weight:600;");
        Label s  = new Label(sub);   s.setStyle("-fx-text-fill:rgba(0,217,255,0.40); -fx-font-size:11px;");
        card.getChildren().addAll(e1, v, n, s);
        card.setOnMouseEntered(ev -> card.setStyle("-fx-background-color:#112016; -fx-border-color:"+color+"; -fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18;"));
        card.setOnMouseExited(ev  -> card.setStyle("-fx-background-color:#0D1F12; -fx-border-color:"+color+"44; -fx-border-width:1; -fx-border-radius:18; -fx-background-radius:18;"));
        return card;
    }

    private VBox buildQuickCard(String emoji, String label, String color, Runnable action) {
        VBox card = new VBox(10); card.setAlignment(Pos.CENTER); card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle("-fx-background-color:#112016; -fx-border-color:rgba(0,217,255,0.12); -fx-border-width:1; -fx-border-radius:18; -fx-background-radius:18; -fx-cursor:hand;");
        Label e1 = new Label(emoji); e1.setStyle("-fx-font-size:26px;");
        Label n  = new Label(label); n.setStyle("-fx-text-fill:rgba(200,255,220,0.80); -fx-font-size:12px; -fx-font-weight:600;");
        card.getChildren().addAll(e1, n);
        card.setOnMouseClicked(e -> action.run());
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#0D1F12; -fx-border-color:"+color+"; -fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18; -fx-cursor:hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#112016; -fx-border-color:rgba(0,217,255,0.12); -fx-border-width:1; -fx-border-radius:18; -fx-background-radius:18; -fx-cursor:hand;"));
        return card;
    }

    private VBox buildActivityFeed() {
        VBox feed = new VBox(0);
        feed.setStyle("-fx-background-color:#0D1F12; -fx-border-color:rgba(0,217,255,0.13); -fx-border-width:1; -fx-border-radius:16; -fx-background-radius:16;");
        String[][] entries = {
                {"🗓",  C_CYAN,    "Nouvel événement créé",                "il y a 2 min"},
                {"🧠",  "#00D9FF", "Nouvelle demande de consultation",      "il y a 5 min"},
                {"📊",  "#A78BFA", "Évaluation ajoutée — Stress",           "il y a 10 min"},
                {"🧠",  C_GREEN,   "Consultation terminée — compte rendu",  "il y a 18 min"},
                {"👥",  C_GREEN,   "Nouvelle participation enregistrée",    "il y a 25 min"},
                {"📊",  "#A78BFA", "Évaluation modifiée — Anxiété",         "il y a 1 h"},
        };
        for (int i = 0; i < entries.length; i++) {
            HBox row = buildFeedRow(entries[i][0], entries[i][1], entries[i][2], entries[i][3]);
            if (i < entries.length - 1)
                row.setStyle("-fx-border-color:transparent transparent rgba(0,217,255,0.07) transparent; -fx-border-width:0 0 1 0;");
            feed.getChildren().add(row);
            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), row);
            ft.setDelay(Duration.millis(200L * i)); ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
        return feed;
    }

    private HBox buildFeedRow(String emoji, String color, String text, String time) {
        HBox row = new HBox(14); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(14, 20, 14, 20));
        Circle dot = new Circle(5, Color.web(color));
        DropShadow ds = new DropShadow(); ds.setColor(Color.web(color, 0.6)); ds.setRadius(6); dot.setEffect(ds);
        Label emojiLbl = new Label(emoji); emojiLbl.setStyle("-fx-font-size:15px;");
        Label textLbl  = new Label(text);  textLbl.setStyle("-fx-text-fill:rgba(200,255,220,0.80); -fx-font-size:13px;");
        HBox.setHgrow(textLbl, Priority.ALWAYS);
        Label timeLbl = new Label(time); timeLbl.setStyle("-fx-text-fill:rgba(0,217,255,0.40); -fx-font-size:11px;");
        row.getChildren().addAll(dot, emojiLbl, textLbl, timeLbl);
        return row;
    }

    // ─── Button styling ───────────────────────────────────
    private void styleNavButton(Button btn, String emoji, String label) {
        if (btn == null) return;
        btn.setText(emoji + "   " + label); btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        String base = "-fx-background-color:transparent; -fx-text-fill:rgba(200,255,220,0.75); " +
                "-fx-font-size:13px; -fx-font-weight:600; -fx-padding:12 20; -fx-cursor:hand; " +
                "-fx-background-radius:0; -fx-border-color:transparent;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:rgba(0,217,255,0.08); -fx-text-fill:"+C_CYAN+"; " +
                "-fx-font-size:13px; -fx-font-weight:600; -fx-padding:12 20; -fx-cursor:hand; " +
                "-fx-background-radius:0; -fx-border-color:transparent transparent transparent "+C_CYAN+"; -fx-border-width:0 0 0 3;"));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
    }

    private void styleSubButton(Button btn, String emoji, String label) {
        if (btn == null) return;
        btn.setText("     " + emoji + "  " + label); btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        String base = "-fx-background-color:transparent; -fx-text-fill:rgba(200,255,220,0.50); " +
                "-fx-font-size:12px; -fx-padding:9 20; -fx-cursor:hand; -fx-background-radius:0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:rgba(0,255,136,0.06); -fx-text-fill:"+C_GREEN+"; " +
                "-fx-font-size:12px; -fx-padding:9 20; -fx-cursor:hand; -fx-background-radius:0; " +
                "-fx-border-color:transparent transparent transparent "+C_GREEN+"; -fx-border-width:0 0 0 2;"));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
    }

    // ─── SubMenu toggle ───────────────────────────────────
    private void hideSubMenu(VBox menu) {
        if (menu == null) return;
        menu.setVisible(false); menu.setManaged(false); menu.setOpacity(0);
    }
    private void toggleMenu(VBox menu) {
        if (menu == null) return;
        if (!menu.isVisible()) {
            menu.setVisible(true); menu.setManaged(true);
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

    // ─── Animations ───────────────────────────────────────
    private void animateSidebarEntrance() {
        if (sidebar == null) return;
        sidebar.setTranslateX(-220);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), sidebar);
        tt.setToX(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }
    private void pulseButton(Button btn) {
        if (btn == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
        st.setToX(0.95); st.setToY(0.95); st.setAutoReverse(true); st.setCycleCount(2); st.play();
    }

    // ─── Navigation ───────────────────────────────────────
    void loadView(String fxmlPath) {
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
        banner.setStyle("-fx-background-color:rgba(255,77,109,0.15); -fx-border-color:"+C_DANGER+"; " +
                "-fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10; " +
                "-fx-text-fill:"+C_DANGER+"; -fx-font-size:13px; -fx-padding:14 22;");
        contentPane.getChildren().setAll(banner);
    }
    @FXML
    private void openFrontPage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/front.fxml"));
            Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> { stage.setScene(new Scene(root)); stage.setTitle("Front Office"); stage.show(); });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorBanner("❌  Erreur chargement Front Office");
        }
    }
}