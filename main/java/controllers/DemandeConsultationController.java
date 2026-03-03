package controllers;

import services.AuthService;
import services.GoogleCalendarService;
import entities.Consultation;
import entities.CompteRendu;
import entities.UserUnified;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceConsultation;
import services.SessionManager;
import services.SmsService;
import services.UserService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DemandeConsultationController {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DARK   = "#050C07";
    private static final String C_DANGER = "#FF4D6D";
    private static final String C_ORANGE = "#FFB347";

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";

    private static final double PRIX_PRESENTIEL = 80.0;
    private static final double PRIX_EN_LIGNE   = 50.0;

    // ── FXML ──
    @FXML private Circle       orb1, orb2;
    @FXML private FlowPane     psyCardsPane;
    @FXML private TextField    dateField;
    @FXML private ToggleButton btnPresentiel, btnEnLigne;
    @FXML private TextArea     notesArea;
    @FXML private Button       btnSubmit, btnRetour, btnHistorique, btnCompteRenduIA;
    @FXML private Label        lblSelectedPsy, lblError, lblSuccess, lblPrix;
    @FXML private ScrollPane   psyScrollPane;
    @FXML private TextField    prixField;
    @FXML private TextField    phoneField;

    // ✅ Bouton Google Calendar créé dynamiquement après planification
    private Button btnGoogleCalendar = null;

    private final ServiceConsultation   service  = new ServiceConsultation();
    private final UserService           userSvc  = new UserService();
    private final SmsService            smsSvc   = new SmsService();
    private final GoogleCalendarService gcalSvc  = new GoogleCalendarService();

    private UserUnified selectedPsy  = null;
    private YearMonth   currentMonth = YearMonth.now();

    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        animateOrbs();
        styleToggleButtons();
        loadPsychologues();
        styleDateField();
        styleSubmitBtn();
        setupRetourBtn();
        setupHistoriqueBtn();
        setupIABtn();
        updatePrixDisplay();
        dateField.setText(
                LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        if (phoneField != null) {
            AuthService.AuthResult u = SessionManager.getCurrentUser();
            if (u != null && u.telephone != null && !u.telephone.isEmpty()) {
                phoneField.setText(u.telephone);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  RETOUR
    // ════════════════════════════════════════════════════════
    private void setupRetourBtn() {
        if (btnRetour == null) return;
        String base  = "-fx-background-color:transparent; -fx-text-fill:rgba(0,217,255,0.65); " +
                "-fx-font-size:12px; -fx-font-weight:600; -fx-border-color:rgba(0,217,255,0.22); " +
                "-fx-border-width:1; -fx-border-radius:20; -fx-background-radius:20; -fx-padding:7 16; -fx-cursor:hand;";
        String hover = "-fx-background-color:rgba(0,217,255,0.10); -fx-text-fill:#00D9FF; " +
                "-fx-font-size:12px; -fx-font-weight:600; -fx-border-color:#00D9FF; " +
                "-fx-border-width:1; -fx-border-radius:20; -fx-background-radius:20; -fx-padding:7 16; -fx-cursor:hand;";
        btnRetour.setStyle(base);
        btnRetour.setOnMouseEntered(e -> btnRetour.setStyle(hover));
        btnRetour.setOnMouseExited(e  -> btnRetour.setStyle(base));
        btnRetour.setOnAction(e -> naviguerVers(e, "/views/Front.fxml"));
    }

    // ════════════════════════════════════════════════════════
    //  HISTORIQUE
    // ════════════════════════════════════════════════════════
    private void setupHistoriqueBtn() {
        if (btnHistorique == null) return;
        btnHistorique.setOnAction(e -> ouvrirHistoriqueComplet());
    }

    private void ouvrirHistoriqueComplet() {
        AuthService.AuthResult currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) { showError("Vous devez être connecté."); return; }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mes Consultations");
        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane); pane.setPrefWidth(740); pane.setPrefHeight(580);
        pane.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color:transparent;");
        Tab t1 = new Tab("📋  Historique");
        Tab t2 = new Tab("📅  Calendrier");
        Tab t3 = new Tab("📝  Comptes Rendus");
        t1.setContent(buildHistoriqueTab(currentUser, dialog));
        t2.setContent(buildCalendrierTab(currentUser.id));
        t3.setContent(buildComptesRendusTab(currentUser.id));
        tabs.getTabs().addAll(t1, t2, t3);
        pane.setContent(tabs);
        styleDialogBtn(pane.lookupButton(pane.getButtonTypes().get(0)), C_CYAN);
        dialog.showAndWait();
    }

    private ScrollPane buildHistoriqueTab(AuthService.AuthResult cu, Dialog<?> parent) {
        VBox box = new VBox(10); box.setPadding(new Insets(16));
        try {
            List<Consultation> list = service.getConsultationsParPatient(cu.id);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (list.isEmpty()) {
                Label e = new Label("Aucune consultation pour le moment.");
                e.setStyle("-fx-text-fill:rgba(200,255,220,0.45); -fx-font-size:13px;");
                box.getChildren().add(e);
            } else {
                for (Consultation c : list) {
                    VBox card = new VBox(8); card.setPadding(new Insets(12,14,12,14));
                    card.setStyle("-fx-background-color:#112016; -fx-border-color:rgba(0,217,255,0.18); -fx-border-width:1; -fx-border-radius:12; -fx-background-radius:12;");
                    HBox r1 = new HBox(10); r1.setAlignment(Pos.CENTER_LEFT);
                    Label lp = new Label("Dr. "+c.getPsyFullName()); lp.setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:13px; -fx-font-weight:bold;");
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    r1.getChildren().addAll(lp, sp, statutBadge(c.getStatut()));
                    HBox r2 = new HBox(16);
                    Label ld = new Label("📅 "+(c.getDateConsultation()!=null?c.getDateConsultation().format(fmt):""));
                    ld.setStyle("-fx-text-fill:rgba(0,217,255,0.70); -fx-font-size:11px;");
                    Label lt = new Label("📍 "+c.getTypeDisplay()); lt.setStyle("-fx-text-fill:rgba(200,255,220,0.50); -fx-font-size:11px;");
                    Label lpx = new Label("💰 "+String.format("%.2f TND",c.getPrix())); lpx.setStyle("-fx-text-fill:#00FF88; -fx-font-size:11px;");
                    r2.getChildren().addAll(ld, lt, lpx);
                    card.getChildren().addAll(r1, r2);
                    if ("planifiee".equals(c.getStatut()) && c.getPrix() > 0) {
                        Button bp = new Button("💳 Payer maintenant");
                        bp.setStyle("-fx-background-color:linear-gradient(to right,#00D9FF,#00FF88); -fx-text-fill:#050C07; -fx-font-size:11px; -fx-font-weight:bold; -fx-padding:6 16; -fx-background-radius:14; -fx-cursor:hand;");
                        bp.setOnAction(ev -> { parent.close(); naviguerVersPaiement(c); });
                        card.getChildren().add(bp);
                    }
                    box.getChildren().add(card);
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        ScrollPane s = new ScrollPane(box); s.setFitToWidth(true);
        s.setStyle("-fx-background-color:transparent; -fx-background:transparent;"); return s;
    }

    private VBox buildCalendrierTab(int patId) {
        VBox root = new VBox(10); root.setPadding(new Insets(16));
        Button prev = navBtn("◀"); Button next = navBtn("▶");
        Label lblM = new Label();
        lblM.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:15px; -fx-font-weight:bold; -fx-min-width:200; -fx-alignment:center;");
        HBox nav = new HBox(16, prev, lblM, next); nav.setAlignment(Pos.CENTER);
        GridPane grid = new GridPane(); grid.setHgap(4); grid.setVgap(4); grid.setPadding(new Insets(8,0,0,0));
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS); cc.setPercentWidth(100.0/7);
            grid.getColumnConstraints().add(cc);
        }
        refreshCalendar(grid, lblM, patId);
        prev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); refreshCalendar(grid, lblM, patId); });
        next.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1);  refreshCalendar(grid, lblM, patId); });
        root.getChildren().addAll(nav, grid); return root;
    }

    private void refreshCalendar(GridPane grid, Label lblM, int patId) {
        grid.getChildren().clear();
        String m = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblM.setText(m.substring(0,1).toUpperCase()+m.substring(1)+" "+currentMonth.getYear());
        String[] jours = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(jours[i]); l.setMaxWidth(Double.MAX_VALUE); l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill:rgba(0,217,255,0.70); -fx-font-size:10px; -fx-font-weight:bold; -fx-padding:6 2;");
            grid.add(l, i, 0);
        }
        List<Consultation> all;
        try { all = service.getConsultationsParPatient(patId); } catch (SQLException e) { all = List.of(); }
        final List<Consultation> fc = all;
        LocalDate first = currentMonth.atDay(1); int startDow = first.getDayOfWeek().getValue()-1;
        int days = currentMonth.lengthOfMonth(); LocalDate today = LocalDate.now();
        int row = 1, col = startDow;
        for (int d = 1; d <= days; d++) {
            LocalDate date = currentMonth.atDay(d);
            List<Consultation> dj = fc.stream().filter(c -> c.getDateConsultation()!=null && c.getDateConsultation().toLocalDate().equals(date)).toList();
            VBox cell = new VBox(2); cell.setPadding(new Insets(4)); cell.setAlignment(Pos.TOP_CENTER); cell.setMinHeight(52);
            boolean isT = date.equals(today);
            String bg = isT?"rgba(0,217,255,0.10)":"transparent";
            String bo = isT?"rgba(0,217,255,0.50)":"rgba(0,217,255,0.08)";
            if (!dj.isEmpty()) { bg="rgba(0,255,136,0.05)"; bo="rgba(0,255,136,0.30)"; }
            final String fbg=bg, fbo=bo;
            cell.setStyle("-fx-background-color:"+bg+"; -fx-border-color:"+bo+"; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
            Label ld = new Label(String.valueOf(d));
            ld.setStyle("-fx-text-fill:"+(isT?"#00D9FF":"#E8FFF0")+"; -fx-font-size:11px; -fx-font-weight:"+(isT?"bold":"normal")+";");
            cell.getChildren().add(ld);
            for (Consultation c : dj.stream().limit(2).toList()) {
                Label dot = new Label("●"); dot.setStyle("-fx-text-fill:"+statutColor(c.getStatut())+"; -fx-font-size:7px;");
                cell.getChildren().add(dot);
            }
            if (dj.size()>2) { Label more=new Label("+"+(dj.size()-2)); more.setStyle("-fx-text-fill:rgba(200,255,220,0.50); -fx-font-size:8px;"); cell.getChildren().add(more); }
            if (!dj.isEmpty()) {
                cell.setOnMouseClicked(ev -> showDayPopup(date, dj));
                cell.setOnMouseEntered(ev -> cell.setStyle("-fx-background-color:rgba(0,217,255,0.08); -fx-border-color:rgba(0,217,255,0.40); -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;"));
                cell.setOnMouseExited(ev  -> cell.setStyle("-fx-background-color:"+fbg+"; -fx-border-color:"+fbo+"; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;"));
            }
            grid.add(cell, col, row); col++; if(col==7){col=0;row++;}
        }
    }

    private void showDayPopup(LocalDate date, List<Consultation> cs) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Consultations du "+date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DialogPane p = d.getDialogPane(); styleDialog(p);
        p.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        VBox box = new VBox(8); box.setPadding(new Insets(14));
        for (Consultation c : cs) {
            HBox rowBox = new HBox(10); rowBox.setAlignment(Pos.CENTER_LEFT); rowBox.setPadding(new Insets(8,12,8,12));
            rowBox.setStyle("-fx-background-color:#112016; -fx-border-color:rgba(0,217,255,0.15); -fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10;");
            Label lt = new Label(c.getDateConsultation()!=null?c.getDateConsultation().format(DateTimeFormatter.ofPattern("HH:mm")):"");
            lt.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:13px; -fx-font-weight:bold; -fx-min-width:40;");
            VBox info = new VBox(2);
            Label lp = new Label("Dr. "+c.getPsyFullName()); lp.setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:12px; -fx-font-weight:bold;");
            Label lm = new Label(c.getTypeDisplay()+" · "+c.getStatutDisplay()+" · "+String.format("%.2f TND",c.getPrix()));
            lm.setStyle("-fx-text-fill:rgba(200,255,220,0.50); -fx-font-size:10px;");
            info.getChildren().addAll(lp, lm); rowBox.getChildren().addAll(lt, info); box.getChildren().add(rowBox);
        }
        p.setContent(box); d.showAndWait();
    }

    private ScrollPane buildComptesRendusTab(int patId) {
        VBox box = new VBox(10); box.setPadding(new Insets(16));
        try {
            List<CompteRendu> crs = service.getComptesRendusParPatient(patId);
            if (crs.isEmpty()) {
                Label e = new Label("Aucun compte rendu disponible."); e.setStyle("-fx-text-fill:rgba(200,255,220,0.45); -fx-font-size:13px;"); box.getChildren().add(e);
            } else {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (CompteRendu cr : crs) {
                    VBox card = new VBox(10); card.setPadding(new Insets(14));
                    card.setStyle("-fx-background-color:#112016; -fx-border-color:rgba(0,217,255,0.18); -fx-border-width:1; -fx-border-radius:14; -fx-background-radius:14;");
                    HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
                    Label ld = new Label(cr.getDateRedaction()!=null?cr.getDateRedaction().format(fmt):""); ld.setStyle("-fx-text-fill:rgba(0,217,255,0.60); -fx-font-size:10px;");
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    Button btnIA = new Button("🤖 Résumé IA");
                    btnIA.setStyle("-fx-background-color:rgba(0,217,255,0.10); -fx-text-fill:#00D9FF; -fx-font-size:10px; -fx-font-weight:600; -fx-padding:5 12; -fx-border-color:rgba(0,217,255,0.30); -fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10; -fx-cursor:hand;");
                    header.getChildren().addAll(ld, sp, btnIA);
                    Label contenu = new Label(cr.getContenu()); contenu.setWrapText(true); contenu.setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:12px;");
                    Label lblResume = new Label(); lblResume.setWrapText(true); lblResume.setVisible(false); lblResume.setManaged(false);
                    lblResume.setStyle("-fx-text-fill:#00FF88; -fx-font-size:11px; -fx-background-color:rgba(0,255,136,0.06); -fx-padding:10; -fx-border-color:rgba(0,255,136,0.20); -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
                    btnIA.setOnAction(e -> {
                        if (lblResume.isVisible()) { lblResume.setVisible(false); lblResume.setManaged(false); btnIA.setText("🤖 Résumé IA"); }
                        else { lblResume.setText("⏳ Génération en cours..."); lblResume.setVisible(true); lblResume.setManaged(true); btnIA.setText("⏳ Chargement..."); btnIA.setDisable(true); genererResumeIA(cr.getContenu(), lblResume, btnIA); }
                    });
                    card.getChildren().addAll(header, contenu, lblResume); box.getChildren().add(card);
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        ScrollPane s = new ScrollPane(box); s.setFitToWidth(true);
        s.setStyle("-fx-background-color:transparent; -fx-background:transparent;"); return s;
    }

    // ════════════════════════════════════════════════════════
    //  BOUTON IA
    // ════════════════════════════════════════════════════════
    private void setupIABtn() {
        if (btnCompteRenduIA == null) return;
        String base  = "-fx-background-color:rgba(0,217,255,0.10); -fx-text-fill:#00D9FF; -fx-font-size:11px; -fx-font-weight:700; -fx-border-color:rgba(0,217,255,0.35); -fx-border-width:1; -fx-border-radius:16; -fx-background-radius:16; -fx-padding:8 16; -fx-cursor:hand;";
        String hover = "-fx-background-color:#00D9FF; -fx-text-fill:#050C07; -fx-font-size:11px; -fx-font-weight:700; -fx-border-color:#00D9FF; -fx-border-width:1; -fx-border-radius:16; -fx-background-radius:16; -fx-padding:8 16; -fx-cursor:hand;";
        btnCompteRenduIA.setStyle(base);
        btnCompteRenduIA.setOnMouseEntered(e -> btnCompteRenduIA.setStyle(hover));
        btnCompteRenduIA.setOnMouseExited(e  -> btnCompteRenduIA.setStyle(base));
    }

    @FXML
    public void handleCompteRenduIA() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Compte Rendu IA — NeuroWell");
        DialogPane pane = dialog.getDialogPane(); styleDialog(pane); pane.setPrefWidth(580);
        pane.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        VBox root = new VBox(16); root.setPadding(new Insets(22));
        Label title = new Label("🤖  Générateur de Compte Rendu IA"); title.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:17px; -fx-font-weight:bold;");
        Label lblInfo = new Label("Décrivez brièvement la consultation :"); lblInfo.setStyle("-fx-text-fill:rgba(200,255,220,0.55); -fx-font-size:11px;");
        TextArea taContext = new TextArea(); taContext.setPromptText("Ex: Patient anxieux, 1ère séance..."); taContext.setPrefRowCount(4); taContext.setWrapText(true);
        taContext.setStyle("-fx-control-inner-background:#112016; -fx-text-fill:#E8FFF0; -fx-prompt-text-fill:rgba(200,255,220,0.25); -fx-font-size:12px; -fx-border-color:rgba(0,217,255,0.18); -fx-border-width:1.5; -fx-border-radius:12; -fx-background-radius:12;");
        Button btnGen = new Button("✨  Générer le compte rendu");
        btnGen.setStyle("-fx-background-color:linear-gradient(to right,#00D9FF,#00FF88); -fx-text-fill:#050C07; -fx-font-size:12px; -fx-font-weight:bold; -fx-padding:10 24; -fx-background-radius:20; -fx-cursor:hand;");
        btnGen.setMaxWidth(Double.MAX_VALUE);
        TextArea taResult = new TextArea(); taResult.setPromptText("Le compte rendu généré apparaîtra ici..."); taResult.setPrefRowCount(8); taResult.setWrapText(true); taResult.setEditable(true);
        taResult.setStyle("-fx-control-inner-background:#0D1A0F; -fx-text-fill:#E8FFF0; -fx-prompt-text-fill:rgba(200,255,220,0.20); -fx-font-size:12px; -fx-border-color:rgba(0,255,136,0.20); -fx-border-width:1.5; -fx-border-radius:12; -fx-background-radius:12;");
        Button btnCopy = new Button("📋 Copier");
        btnCopy.setStyle("-fx-background-color:rgba(0,255,136,0.10); -fx-text-fill:#00FF88; -fx-font-size:11px; -fx-font-weight:600; -fx-padding:6 16; -fx-border-color:rgba(0,255,136,0.25); -fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10; -fx-cursor:hand;");
        btnCopy.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(taResult.getText()); cb.setContent(cc);
            PauseTransition resetCopy = new PauseTransition(Duration.seconds(2));
            resetCopy.setOnFinished(ev -> btnCopy.setText("📋 Copier"));
            resetCopy.play();

        });
        btnGen.setOnAction(e -> {
            String ctx = taContext.getText().trim();
            if (ctx.isEmpty()) { taResult.setText("⚠️ Veuillez décrire la consultation d'abord."); return; }
            taResult.setText("⏳ Génération en cours..."); btnGen.setDisable(true);
            String psyName = selectedPsy!=null?selectedPsy.getNom()+" "+selectedPsy.getPrenom():"Non sélectionné";
            AuthService.AuthResult cu = SessionManager.getCurrentUser();
            String patName = cu!=null?cu.nom+" "+cu.prenom:"Patient";
            String prompt = "Tu es un psychologue clinicien. Génère un compte rendu professionnel et structuré.\n\n" +
                    "Patient : "+patName+"\nPsychologue : Dr. "+psyName+"\nDate : "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+"\nContexte : "+ctx+
                    "\n\nLe compte rendu doit inclure :\n1. Motif de consultation\n2. Résumé de la séance\n3. Observations cliniques\n4. Objectifs thérapeutiques\n5. Recommandations\n\nRéponds en français (300-400 mots).";
            genererTexteIA(prompt, taResult, btnGen, "✨  Générer le compte rendu");
        });
        root.getChildren().addAll(title, lblInfo, taContext, btnGen, new Separator(), new Label("Résultat IA"){{setStyle("-fx-text-fill:rgba(0,217,255,0.55); -fx-font-size:9px; -fx-font-weight:bold;");}}, taResult, btnCopy);
        pane.setContent(root);
        styleDialogBtn(pane.lookupButton(pane.getButtonTypes().get(0)), C_CYAN);
        dialog.showAndWait();
    }

    // ════════════════════════════════════════════════════════
    //  IA GROQ
    // ════════════════════════════════════════════════════════
    private void genererTexteIA(String prompt, TextArea target, Button btn, String origText) {
        new Thread(() -> {
            try {
                var gson = new com.google.gson.Gson();
                String body = "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":%s}],\"max_tokens\":700}".formatted(GROQ_MODEL, gson.toJson(prompt));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GROQ_URL))
                        .header("Content-Type","application/json").header("Authorization","Bearer "+GROQ_API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                String result = resp.statusCode()==200
                        ? com.google.gson.JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim()
                        : "❌ Erreur IA ("+resp.statusCode()+")";
                Platform.runLater(() -> { target.setText(result); btn.setDisable(false); btn.setText(origText); });
            } catch (Exception ex) { Platform.runLater(() -> { target.setText("❌ Erreur : "+ex.getMessage()); btn.setDisable(false); btn.setText(origText); }); }
        }).start();
    }

    private void genererResumeIA(String contenu, Label lblTarget, Button btnIA) {
        new Thread(() -> {
            try {
                String prompt = "Résume en 3-4 phrases ce compte rendu psychologique, en français, de façon bienveillante pour le patient :\n\n"+contenu;
                var gson = new com.google.gson.Gson();
                String body = "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":%s}],\"max_tokens\":300}".formatted(GROQ_MODEL, gson.toJson(prompt));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GROQ_URL))
                        .header("Content-Type","application/json").header("Authorization","Bearer "+GROQ_API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                String result = resp.statusCode()==200
                        ? "🤖 "+com.google.gson.JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim()
                        : "❌ Erreur IA ("+resp.statusCode()+")";
                Platform.runLater(() -> { lblTarget.setText(result); btnIA.setText("✅ Masquer"); btnIA.setDisable(false); });
            } catch (Exception ex) { Platform.runLater(() -> { lblTarget.setText("❌ Erreur : "+ex.getMessage()); btnIA.setText("🤖 Résumé IA"); btnIA.setDisable(false); }); }
        }).start();
    }

    // ════════════════════════════════════════════════════════
    //  ✅ HANDLESUBMIT — avec bouton Google Calendar
    // ════════════════════════════════════════════════════════
    @FXML
    private void handleSubmit() {
        hideMessages();

        if (selectedPsy == null) { showError("Veuillez sélectionner un psychologue."); return; }
        if (dateField.getText().trim().isEmpty()) { showError("Veuillez entrer une date."); return; }

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(dateField.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ex) { showError("Format invalide. Utilisez : yyyy-MM-dd HH:mm"); return; }
        if (date.isBefore(LocalDateTime.now())) { showError("La date doit être dans le futur."); return; }

        String type = btnEnLigne != null && btnEnLigne.isSelected() ? "en_ligne" : "presentiel";
        double prixCalc = type.equals("en_ligne") ? PRIX_EN_LIGNE : PRIX_PRESENTIEL;
        if (prixField != null && !prixField.getText().trim().isEmpty()) {
            try { prixCalc = Double.parseDouble(prixField.getText().trim()); } catch (NumberFormatException ignored) {}
        }
        final double prixFinal = prixCalc;
        final String telSaisi = (phoneField != null && !phoneField.getText().trim().isEmpty()) ? phoneField.getText().trim() : null;
        String notes = notesArea != null ? notesArea.getText().trim() : "";

        try {
            AuthService.AuthResult cu = SessionManager.getCurrentUser();
            if (cu == null) { showError("Vous devez être connecté."); return; }

            UserUnified patient = new UserUnified();
            patient.setId(cu.id); patient.setNom(cu.nom); patient.setPrenom(cu.prenom);
            patient.setEmail(cu.email); patient.setTelephone(cu.telephone); patient.setRole(cu.role);

            Consultation c = new Consultation(patient, selectedPsy, date, type, "planifiee", prixFinal);
            c.setNotes(notes.isEmpty() ? null : notes);
            service.ajouterConsultation(c);

            // SMS
            final String patNom = cu.nom+" "+cu.prenom;
            final String psyNom = selectedPsy.getNom()+" "+selectedPsy.getPrenom();
            final String typeLbl = type.equals("en_ligne") ? "En ligne" : "Présentiel";
            final LocalDateTime df = date;
            if (telSaisi != null) {
                new Thread(() -> smsSvc.envoyerConfirmationConsultation(telSaisi, patNom, psyNom, df, typeLbl, prixFinal)).start();
            }

            // ✅ Google Calendar + bouton d'accès
            final Consultation consultationFinal = c;
            new Thread(() -> {
                try {
                    String googleEventId = gcalSvc.syncConsultation(consultationFinal);

                    // Construire le lien Google Calendar vers cet événement
                    String lienGoogleCal = "https://calendar.google.com/calendar/r";

                    System.out.println("📅 Événement créé : " + googleEventId);
                    Platform.runLater(() -> {
                        showSuccess("✅ Consultation planifiée et ajoutée à Google Calendar !"
                                + (telSaisi != null ? "  SMS envoyé." : ""));
                        // ✅ Afficher le bouton Google Calendar
                        afficherBoutonCalendrier(lienGoogleCal);
                    });
                } catch (Exception ex) {
                    System.err.println("⚠️ Google Calendar : " + ex.getMessage());
                    Platform.runLater(() -> showSuccess("✅ Consultation planifiée !"
                            + (telSaisi != null ? "  SMS envoyé." : "")));
                }
            }).start();

            // Reset UI
            selectedPsy = null;
            loadPsychologues();
            if (lblSelectedPsy != null) lblSelectedPsy.setText("Aucun psychologue sélectionné");
            dateField.setText(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            if (notesArea != null) notesArea.clear();
            updatePrixDisplay();

        } catch (Exception ex) { ex.printStackTrace(); showError("Erreur : " + ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════
    //  ✅ BOUTON GOOGLE CALENDAR — logique complète
    // ════════════════════════════════════════════════════════

    /**
     * Crée dynamiquement le bouton bleu "Ouvrir dans Google Calendar"
     * et l'insère juste sous le label de succès.
     * Disparaît automatiquement après 60 secondes.
     */
    private void afficherBoutonCalendrier(String lien) {
        retirerBoutonCalendrier(); // Nettoyer l'ancien

        btnGoogleCalendar = new Button("📅  Ouvrir dans Google Calendar");
        btnGoogleCalendar.setMaxWidth(Double.MAX_VALUE);

        String styleNormal = "-fx-background-color:#4285F4; -fx-text-fill:white; " +
                "-fx-font-size:13px; -fx-font-weight:bold; -fx-padding:11 0; " +
                "-fx-background-radius:22; -fx-cursor:hand;";
        String styleHover  = "-fx-background-color:#2a6de8; -fx-text-fill:white; " +
                "-fx-font-size:13px; -fx-font-weight:bold; -fx-padding:11 0; " +
                "-fx-background-radius:22; -fx-cursor:hand;";

        btnGoogleCalendar.setStyle(styleNormal);
        btnGoogleCalendar.setOnMouseEntered(e -> btnGoogleCalendar.setStyle(styleHover));
        btnGoogleCalendar.setOnMouseExited(e  -> btnGoogleCalendar.setStyle(styleNormal));

        // ── Clic → ouvrir le navigateur ──
        btnGoogleCalendar.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(lien));
            } catch (Exception ex) {
                // Fallback : copier dans le presse-papiers
                javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(lien); cb.setContent(cc);
                btnGoogleCalendar.setText("✅ Lien copié dans le presse-papiers !");
            }
        });

        // ── Insérer dans le parent de lblSuccess ──
        if (lblSuccess != null) {
            javafx.scene.Parent parent = lblSuccess.getParent();
            if (parent instanceof VBox vbox) {
                int idx = vbox.getChildren().indexOf(lblSuccess);
                vbox.getChildren().add(Math.min(idx + 1, vbox.getChildren().size()), btnGoogleCalendar);
            } else if (parent instanceof Pane pane) {
                pane.getChildren().add(btnGoogleCalendar);
            }
        }

        // ── Auto-disparition après 60 secondes ──
        new Timeline(new KeyFrame(Duration.seconds(60), ev -> {
            retirerBoutonCalendrier();
            if (lblSuccess != null) { lblSuccess.setVisible(false); lblSuccess.setManaged(false); }
        })).play();
    }

    /** Retire proprement le bouton du layout. */
    private void retirerBoutonCalendrier() {
        if (btnGoogleCalendar != null) {
            javafx.scene.Parent p = btnGoogleCalendar.getParent();
            if (p instanceof Pane pane) pane.getChildren().remove(btnGoogleCalendar);
            btnGoogleCalendar = null;
        }
    }

    // ════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════
    private void naviguerVersPaiement(Consultation c) {
        try {
            PaiementFrontController.setMontantConsultation(c.getPrix());
            PaiementFrontController.setConsultationId(c.getId());
            PaiementFrontController.setPatientNom(c.getPatient()!=null?c.getPatientFullName():SessionManager.getNom()+" "+SessionManager.getPrenom());
            PaiementFrontController.setPsyNom(c.getPsyFullName());
            Stage stage = (Stage)(psyCardsPane!=null?psyCardsPane.getScene().getWindow():btnHistorique.getScene().getWindow());
            Parent root = FXMLLoader.load(getClass().getResource("/views/PaiementFront.fxml"));
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0); ft.setOnFinished(e -> { stage.setScene(new Scene(root)); stage.show(); }); ft.play();
        } catch (IOException e) { e.printStackTrace(); showError("Impossible d'ouvrir le paiement."); }
    }

    private void naviguerVers(javafx.event.ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0); ft.setOnFinished(e -> { stage.setScene(new Scene(root)); stage.show(); }); ft.play();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════
    //  PRIX
    // ════════════════════════════════════════════════════════
    private void updatePrixDisplay() {
        boolean en = btnEnLigne != null && btnEnLigne.isSelected();
        double p = en ? PRIX_EN_LIGNE : PRIX_PRESENTIEL;
        if (lblPrix   != null) lblPrix.setText(String.format("Prix estimé : %.2f TND", p));
        if (prixField != null) prixField.setText(String.format("%.2f", p));
    }

    // ════════════════════════════════════════════════════════
    //  CHARGEMENT PSY
    // ════════════════════════════════════════════════════════
    private void loadPsychologues() {
        if (psyCardsPane == null) return;
        psyCardsPane.getChildren().clear();
        try {
            List<UserUnified> psys = userSvc.getAllUsers().stream().filter(u -> "psy".equals(u.getRole())).toList();
            if (psys.isEmpty()) {
                Label e = new Label("Aucun psychologue disponible."); e.setStyle("-fx-text-fill:rgba(200,255,220,0.45); -fx-font-size:13px; -fx-padding:20;");
                psyCardsPane.getChildren().add(e); return;
            }
            for (int i = 0; i < psys.size(); i++) {
                VBox card = buildPsyCard(psys.get(i)); psyCardsPane.getChildren().add(card);
                card.setOpacity(0); card.setTranslateY(20);
                PauseTransition delay = new PauseTransition(Duration.millis(i*80));
                delay.setOnFinished(e -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(350), card); ft.setFromValue(0); ft.setToValue(1);
                    TranslateTransition tt = new TranslateTransition(Duration.millis(350), card); tt.setFromY(20); tt.setToY(0);
                    new ParallelTransition(ft, tt).play();
                }); delay.play();
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private VBox buildPsyCard(UserUnified psy) {
        VBox card = new VBox(12); card.setPrefWidth(210); card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20,16,18,16)); card.setStyle(unselectedCardStyle());
        StackPane av = new StackPane();
        Circle bg = new Circle(36); bg.setStyle("-fx-fill:rgba(0,217,255,0.10); -fx-stroke:rgba(0,217,255,0.30); -fx-stroke-width:1.5;");
        Label li = new Label(initials(psy.getNom(), psy.getPrenom())); li.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:20px; -fx-font-weight:bold;");
        av.getChildren().addAll(bg, li);
        Label ln = new Label(psy.getNom()+" "+psy.getPrenom()); ln.setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:13px; -fx-font-weight:bold;"); ln.setWrapText(true); ln.setAlignment(Pos.CENTER);
        Label ls = new Label(psy.getSpecialite()!=null?psy.getSpecialite():"Psychologie générale"); ls.setStyle("-fx-text-fill:rgba(0,217,255,0.65); -fx-font-size:11px;"); ls.setWrapText(true); ls.setAlignment(Pos.CENTER);
        Label ld = new Label(psy.getDiplome()!=null?psy.getDiplome():""); ld.setStyle("-fx-text-fill:rgba(200,255,220,0.40); -fx-font-size:10px;"); ld.setWrapText(true); ld.setAlignment(Pos.CENTER);
        Button bs = new Button("Sélectionner"); bs.setMaxWidth(Double.MAX_VALUE);
        bs.setStyle("-fx-background-color:transparent; -fx-text-fill:#00D9FF; -fx-font-size:11px; -fx-font-weight:600; -fx-padding:7 0; -fx-border-color:rgba(0,217,255,0.35); -fx-border-width:1; -fx-border-radius:20; -fx-background-radius:20; -fx-cursor:hand;");
        bs.setOnAction(e -> selectPsy(psy, card, bs));
        DropShadow sh = new DropShadow(); sh.setColor(Color.web(C_CYAN,0)); sh.setRadius(20); card.setEffect(sh);
        card.setOnMouseEntered(ev -> { if(selectedPsy==null||selectedPsy.getId()!=psy.getId()){card.setStyle(hoverCardStyle());sh.setColor(Color.web(C_CYAN,0.18));} });
        card.setOnMouseExited(ev  -> { if(selectedPsy==null||selectedPsy.getId()!=psy.getId()){card.setStyle(unselectedCardStyle());sh.setColor(Color.web(C_CYAN,0));} });
        card.getChildren().addAll(av, ln, ls, ld, bs); return card;
    }

    private void selectPsy(UserUnified psy, VBox card, Button bs) {
        selectedPsy = psy;
        if (psyCardsPane!=null) psyCardsPane.getChildren().forEach(n -> {
            if(n instanceof VBox){n.setStyle(unselectedCardStyle());((VBox)n).getChildren().stream().filter(c->c instanceof Button).forEach(c->((Button)c).setText("Sélectionner"));}
        });
        card.setStyle(selectedCardStyle()); bs.setText("✓ Sélectionné");
        bs.setStyle("-fx-background-color:#00FF88; -fx-text-fill:#050C07; -fx-font-size:11px; -fx-font-weight:700; -fx-padding:7 0; -fx-border-color:#00FF88; -fx-border-width:1; -fx-border-radius:20; -fx-background-radius:20; -fx-cursor:hand;");
        if(lblSelectedPsy!=null) lblSelectedPsy.setText("Dr. "+psy.getNom()+" "+psy.getPrenom()+(psy.getSpecialite()!=null?" — "+psy.getSpecialite():""));
        hideMessages();
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════
    private Label statutBadge(String s) {
        Label l = new Label(getStatutLabel(s));
        l.setStyle("-fx-text-fill:"+statutColor(s)+"; -fx-font-size:10px; -fx-font-weight:bold; -fx-background-color:"+statutBg(s)+"; -fx-padding:3 10; -fx-background-radius:10;");
        return l;
    }
    private String getStatutLabel(String s) {
        if(s==null) return "—";
        return switch(s){case "planifiee"->"📅 Planifiée";case "en_cours"->"⏳ En cours";case "terminee"->"✅ Terminée";case "annulee"->"❌ Annulée";default->s;};
    }
    private String statutColor(String s) {
        if(s==null) return "#E8FFF0";
        return switch(s){case "planifiee"->C_CYAN;case "en_cours"->C_ORANGE;case "terminee"->C_GREEN;case "annulee"->C_DANGER;default->"#E8FFF0";};
    }
    private String statutBg(String s) {
        if(s==null) return "rgba(200,255,220,0.10)";
        return switch(s){case "planifiee"->"rgba(0,217,255,0.12)";case "en_cours"->"rgba(255,179,71,0.12)";case "terminee"->"rgba(0,255,136,0.12)";case "annulee"->"rgba(255,77,109,0.12)";default->"rgba(200,255,220,0.10)";};
    }
    private Button navBtn(String t) {
        Button b = new Button(t); b.setStyle("-fx-background-color:rgba(0,217,255,0.12); -fx-text-fill:#00D9FF; -fx-font-size:13px; -fx-font-weight:bold; -fx-padding:5 14; -fx-border-color:rgba(0,217,255,0.30); -fx-border-width:1; -fx-border-radius:12; -fx-background-radius:12; -fx-cursor:hand;"); return b;
    }
    private void styleDialog(DialogPane p) {
        p.setStyle("-fx-background-color:#0D1F12; -fx-border-color:rgba(0,217,255,0.25); -fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18;");
    }
    private void styleDialogBtn(javafx.scene.Node btn, String color) {
        if(btn!=null) btn.setStyle("-fx-background-color:"+color+"; -fx-text-fill:"+C_DARK+"; -fx-font-weight:bold; -fx-padding:9 22; -fx-background-radius:18; -fx-cursor:hand;");
    }
    private void styleToggleButtons() {
        String on  = "-fx-background-color:#00D9FF; -fx-text-fill:#050C07; -fx-font-size:12px; -fx-font-weight:bold; -fx-padding:10 24; -fx-background-radius:20; -fx-cursor:hand;";
        String off = "-fx-background-color:transparent; -fx-text-fill:rgba(200,255,220,0.50); -fx-font-size:12px; -fx-padding:10 24; -fx-border-color:rgba(200,255,220,0.18); -fx-border-width:1; -fx-border-radius:20; -fx-background-radius:20; -fx-cursor:hand;";
        if(btnPresentiel!=null){btnPresentiel.setStyle(on);btnPresentiel.setOnAction(e->{btnPresentiel.setStyle(on);if(btnEnLigne!=null)btnEnLigne.setStyle(off);updatePrixDisplay();});}
        if(btnEnLigne!=null){btnEnLigne.setStyle(off);btnEnLigne.setOnAction(e->{btnEnLigne.setStyle(on);if(btnPresentiel!=null)btnPresentiel.setStyle(off);updatePrixDisplay();});}
    }
    private void styleDateField() {
        if(dateField==null) return;
        dateField.setStyle("-fx-background-color:#0D1A0F; -fx-text-fill:#E8FFF0; -fx-prompt-text-fill:rgba(200,255,220,0.22); -fx-font-size:13px; -fx-padding:11 14; -fx-background-radius:11; -fx-border-color:rgba(0,217,255,0.20); -fx-border-width:1.5; -fx-border-radius:11;");
    }
    private void styleSubmitBtn() {
        if(btnSubmit==null) return;
        btnSubmit.setStyle("-fx-background-color:linear-gradient(to right,#00D9FF,#00FF88); -fx-text-fill:#050C07; -fx-font-size:14px; -fx-font-weight:bold; -fx-padding:14 0; -fx-background-radius:28; -fx-cursor:hand;");
        DropShadow g = new DropShadow(); g.setColor(Color.web(C_CYAN,0.40)); g.setRadius(20); btnSubmit.setEffect(g);
    }
    private void showError(String msg) {
        retirerBoutonCalendrier();
        if(lblError==null) return;
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
        if(lblSuccess!=null){ lblSuccess.setVisible(false); lblSuccess.setManaged(false); }
    }
    private void showSuccess(String msg) {
        if(lblSuccess==null) return;
        lblSuccess.setText(msg); lblSuccess.setVisible(true); lblSuccess.setManaged(true);
        if(lblError!=null){ lblError.setVisible(false); lblError.setManaged(false); }
    }
    private void hideMessages() {
        retirerBoutonCalendrier();
        if(lblError!=null){ lblError.setVisible(false); lblError.setManaged(false); }
        if(lblSuccess!=null){ lblSuccess.setVisible(false); lblSuccess.setManaged(false); }
    }
    private String initials(String n, String p) { return (n!=null&&!n.isEmpty()?String.valueOf(n.charAt(0)).toUpperCase():"")+(p!=null&&!p.isEmpty()?String.valueOf(p.charAt(0)).toUpperCase():""); }
    private String unselectedCardStyle() { return "-fx-background-color:#0D1F12; -fx-border-color:rgba(0,217,255,0.14); -fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18; -fx-cursor:hand;"; }
    private String hoverCardStyle()      { return "-fx-background-color:rgba(0,217,255,0.05); -fx-border-color:rgba(0,217,255,0.40); -fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18; -fx-cursor:hand;"; }
    private String selectedCardStyle()   { return "-fx-background-color:rgba(0,255,136,0.07); -fx-border-color:#00FF88; -fx-border-width:2; -fx-border-radius:18; -fx-background-radius:18; -fx-cursor:hand;"; }
    private void animateOrbs() { animateOrb(orb1,40,-25,14); animateOrb(orb2,-50,35,18); }
    private void animateOrb(Circle o, double dx, double dy, double s) {
        if(o==null) return;
        TranslateTransition tt = new TranslateTransition(Duration.seconds(s), o);
        tt.setByX(dx); tt.setByY(dy); tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true); tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
    }
}