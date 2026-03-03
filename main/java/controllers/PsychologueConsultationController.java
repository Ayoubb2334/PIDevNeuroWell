package controllers;

import entities.Consultation;
import entities.CompteRendu;
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
import services.GeminiService;
import services.ServiceConsultation;
import services.SessionManager;
import services.SmsService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PsychologueConsultationController {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DARK   = "#050C07";
    private static final String C_CARD   = "#0D1F12";
    private static final String C_DANGER = "#FF4D6D";
    private static final String C_ORANGE = "#FFB347";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Circle  orb1, orb2;
    @FXML private FlowPane demandesPane;
    @FXML private ScrollPane demandesScroll;
    @FXML private VBox    crListPane;
    @FXML private Label   lblDemandes, lblCR, lblStatsCR;
    @FXML private Button  btnShowDemandes, btnShowCR, btnShowCalendrier;
    @FXML private StackPane viewDemandes, viewCR, viewCalendrier;
    @FXML private Button  btnAddCR;
    @FXML private Button  btnRetour;
    @FXML private ComboBox<String> cmbFilterStatut;
    @FXML private GridPane calendarGrid;
    @FXML private Label   lblCalendarMonth;

    private final ServiceConsultation service = new ServiceConsultation();
    private final GeminiService        gemini  = new GeminiService();
    private final SmsService           smsSvc  = new SmsService();
    private int idPsy;
    private YearMonth currentMonth = YearMonth.now();

    @FXML
    public void initialize() {
        animateOrbs();
        idPsy = SessionManager.isLoggedIn() ? SessionManager.getId() : 1;
        setupNavTabs();
        setupRetourBtn();
        loadDemandes();
        loadComptesRendus();
    }

    private void setupRetourBtn() {
        if (btnRetour == null) return;
        btnRetour.setText("← Retour");
        btnRetour.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(0,217,255,0.65); " +
                "-fx-font-size: 11px; -fx-font-weight: 600; " +
                "-fx-border-color: rgba(0,217,255,0.22); -fx-border-width: 1; " +
                "-fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 6 14; -fx-cursor: hand;");
        btnRetour.setOnAction(e -> {
            try {
                Stage stage = (Stage) btnRetour.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/views/Front.fxml"));
                FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
                ft.setFromValue(1); ft.setToValue(0);
                ft.setOnFinished(ev -> { stage.setScene(new Scene(root)); stage.show(); });
                ft.play();
            } catch (IOException ex) { ex.printStackTrace(); }
        });
    }

    private void setupNavTabs() {
        String on  = "-fx-background-color: rgba(0,217,255,0.12); -fx-text-fill: #00D9FF; " +
                "-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 11 28; " +
                "-fx-border-color: #00D9FF; -fx-border-width: 0 0 2 0; -fx-background-radius: 0; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-text-fill: rgba(200,255,220,0.45); " +
                "-fx-font-size: 12px; -fx-padding: 11 28; -fx-border-color: transparent; " +
                "-fx-border-width: 0 0 2 0; -fx-background-radius: 0; -fx-cursor: hand;";
        if (btnShowDemandes != null) {
            btnShowDemandes.setStyle(on);
            btnShowDemandes.setOnAction(e -> {
                btnShowDemandes.setStyle(on);
                if (btnShowCR != null) btnShowCR.setStyle(off);
                if (btnShowCalendrier != null) btnShowCalendrier.setStyle(off);
                if (viewDemandes != null) viewDemandes.setVisible(true);
                if (viewCR      != null) viewCR.setVisible(false);
                if (viewCalendrier != null) viewCalendrier.setVisible(false);
            });
        }
        if (btnShowCR != null) {
            btnShowCR.setStyle(off);
            btnShowCR.setOnAction(e -> {
                btnShowCR.setStyle(on);
                if (btnShowDemandes != null) btnShowDemandes.setStyle(off);
                if (btnShowCalendrier != null) btnShowCalendrier.setStyle(off);
                if (viewCR      != null) viewCR.setVisible(true);
                if (viewDemandes!= null) viewDemandes.setVisible(false);
                if (viewCalendrier != null) viewCalendrier.setVisible(false);
                loadComptesRendus();
            });
        }
        // ✅ Onglet Calendrier
        if (btnShowCalendrier != null) {
            btnShowCalendrier.setStyle(off);
            btnShowCalendrier.setOnAction(e -> {
                btnShowCalendrier.setStyle(on);
                if (btnShowDemandes != null) btnShowDemandes.setStyle(off);
                if (btnShowCR != null) btnShowCR.setStyle(off);
                if (viewCalendrier != null) viewCalendrier.setVisible(true);
                if (viewDemandes   != null) viewDemandes.setVisible(false);
                if (viewCR         != null) viewCR.setVisible(false);
                buildCalendar();
            });
        }
    }

    // ─── DEMANDES (Card View) ─────────────────────────────────
    private void loadDemandes() {
        if (demandesPane == null) return;
        demandesPane.getChildren().clear();
        try {
            List<Consultation> list = service.getConsultationsParPsychologue(idPsy);
            if (lblDemandes != null) lblDemandes.setText(list.size() + " demande" + (list.size() > 1 ? "s" : ""));

            if (list.isEmpty()) {
                Label empty = new Label("Aucune demande pour le moment.");
                empty.setStyle("-fx-text-fill: rgba(200,255,220,0.40); -fx-font-size: 13px; -fx-padding: 30;");
                demandesPane.getChildren().add(empty);
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                VBox card = buildDemandeCard(list.get(i));
                demandesPane.getChildren().add(card);
                animateCard(card, i);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private VBox buildDemandeCard(Consultation c) {
        VBox card = new VBox(12);
        card.setPrefWidth(280); card.setMaxWidth(280);
        card.setPadding(new Insets(18, 16, 16, 16));
        card.setStyle("-fx-background-color: #0D1F12; " +
                "-fx-border-color: rgba(0,217,255,0.14); -fx-border-width: 1.5; " +
                "-fx-border-radius: 18; -fx-background-radius: 18;");

        // Status indicator top
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5);
        dot.setStyle(statutDotStyle(c.getStatut()));
        Label lblStatut = new Label(c.getStatutDisplay());
        lblStatut.setStyle("-fx-text-fill:" + statutColor(c.getStatut()) + "; -fx-font-size:10px; -fx-font-weight:bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblType = new Label(c.getTypeDisplay());
        lblType.setStyle("-fx-text-fill:rgba(0,217,255,0.60); -fx-font-size:10px;");
        topRow.getChildren().addAll(dot, lblStatut, sp, lblType);

        // Patient avatar + name
        HBox patRow = new HBox(12); patRow.setAlignment(Pos.CENTER_LEFT);
        StackPane av = new StackPane();
        Circle avc = new Circle(22);
        avc.setStyle("-fx-fill: rgba(0,217,255,0.10); -fx-stroke: rgba(0,217,255,0.25); -fx-stroke-width:1;");
        Label avL = new Label(initials(
                c.getPatient() != null ? c.getPatient().getNom() : "",
                c.getPatient() != null ? c.getPatient().getPrenom() : ""
        ));
        avL.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:13px; -fx-font-weight:bold;");
        av.getChildren().addAll(avc, avL);
        VBox patInfo = new VBox(2);
        Label lblPat = new Label(c.getPatientFullName());
        lblPat.setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:13px; -fx-font-weight:bold;");
        Label lblDate = new Label(c.getDateConsultation() != null ? c.getDateConsultation().format(FMT) : "");
        lblDate.setStyle("-fx-text-fill:rgba(200,255,220,0.45); -fx-font-size:11px;");
        patInfo.getChildren().addAll(lblPat, lblDate);
        patRow.getChildren().addAll(av, patInfo);

        // Actions
        HBox actions = new HBox(8);
        Button btnAccept  = cardBtn("Accepter",   C_GREEN);
        Button btnRefuse  = cardBtn("Refuser",    C_DANGER);
        Button btnEnCours = cardBtn("En cours",   C_ORANGE);
        Button btnCR      = cardBtn("Compte rendu", C_CYAN);
        btnAccept.setOnAction(e  -> changeStatut(c, "planifiee",  actions));
        btnRefuse.setOnAction(e  -> changeStatut(c, "annulee",    actions));
        btnEnCours.setOnAction(e -> changeStatut(c, "en_cours",   actions));
        btnCR.setOnAction(e      -> openCRDialog(c));
        actions.getChildren().addAll(btnAccept, btnRefuse);

        HBox actRow2 = new HBox(8, btnEnCours, btnCR);

        card.getChildren().addAll(topRow, patRow, actions, actRow2);

        // Hover
        DropShadow sh = new DropShadow(); sh.setColor(Color.web(C_CYAN, 0)); sh.setRadius(20);
        card.setEffect(sh);
        card.setOnMouseEntered(ev -> {
            card.setStyle("-fx-background-color: rgba(0,217,255,0.04); " +
                    "-fx-border-color: rgba(0,217,255,0.40); -fx-border-width: 1.5; " +
                    "-fx-border-radius: 18; -fx-background-radius: 18;");
            sh.setColor(Color.web(C_CYAN, 0.15));
        });
        card.setOnMouseExited(ev -> {
            card.setStyle("-fx-background-color: #0D1F12; " +
                    "-fx-border-color: rgba(0,217,255,0.14); -fx-border-width: 1.5; " +
                    "-fx-border-radius: 18; -fx-background-radius: 18;");
            sh.setColor(Color.web(C_CYAN, 0));
        });

        return card;
    }

    private void changeStatut(Consultation c, String statut, HBox actionsBox) {
        try {
            service.mettreAJourStatut(c.getId(), statut);
            c.setStatut(statut);

            // ✅ SMS notification au patient
            if (c.getPatient() != null && c.getPatient().getTelephone() != null) {
                String tel = c.getPatient().getTelephone();
                String patNom = c.getPatientFullName();
                String psyNom = c.getPsyFullName();
                new Thread(() -> {
                    if ("annulee".equals(statut)) {
                        smsSvc.envoyerAnnulation(tel, patNom, psyNom);
                    } else if ("planifiee".equals(statut)) {
                        smsSvc.envoyerConfirmationConsultation(tel, patNom, psyNom,
                                c.getDateConsultation(),
                                "presentiel".equals(c.getType()) ? "Présentiel" : "En ligne",
                                c.getPrix());
                    }
                }).start();
            }

            loadDemandes();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════
    //  ✅ CALENDRIER DES CONSULTATIONS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleCalendarPrev() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleCalendarNext() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    private void buildCalendar() {
        if (calendarGrid == null) return;

        // Mise à jour du titre
        if (lblCalendarMonth != null) {
            String mois = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            lblCalendarMonth.setText(mois.substring(0,1).toUpperCase() + mois.substring(1) + " " + currentMonth.getYear());
        }

        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // Colonnes égales
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS); cc.setPercentWidth(100.0/7);
            calendarGrid.getColumnConstraints().add(cc);
        }

        // En-têtes jours
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(jours[i]);
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: rgba(0,217,255,0.70); -fx-font-size: 10px; " +
                    "-fx-font-weight: bold; -fx-padding: 8 4; " +
                    "-fx-border-color: transparent transparent rgba(0,217,255,0.15) transparent; " +
                    "-fx-border-width: 0 0 1 0;");
            calendarGrid.add(l, i, 0);
        }

        // Récupérer les consultations du mois
        List<Consultation> consultations;
        try {
            consultations = service.getConsultationsParPsychologue(idPsy);
        } catch (SQLException e) {
            consultations = List.of();
        }
        final List<Consultation> finalConsultations = consultations;

        LocalDate firstDay = currentMonth.atDay(1);
        int startDow = firstDay.getDayOfWeek().getValue() - 1; // 0=Lun
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 1; int col = startDow;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            LocalDate today = LocalDate.now();

            // Consultations ce jour
            List<Consultation> consultsDuJour = finalConsultations.stream()
                    .filter(c -> c.getDateConsultation() != null &&
                            c.getDateConsultation().toLocalDate().equals(date))
                    .toList();

            VBox cell = new VBox(2);
            cell.setPadding(new Insets(4));
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setMinHeight(60);

            // Style
            String cellBg = date.equals(today) ? "rgba(0,217,255,0.10)" : "transparent";
            String border = date.equals(today) ? "rgba(0,217,255,0.50)" : "rgba(0,217,255,0.08)";
            cell.setStyle("-fx-background-color: " + cellBg + "; " +
                    "-fx-border-color: " + border + "; " +
                    "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-cursor: " + (consultsDuJour.isEmpty() ? "default" : "hand") + ";");

            Label lblDay = new Label(String.valueOf(day));
            lblDay.setStyle("-fx-text-fill: " + (date.equals(today) ? "#00D9FF" : "#E8FFF0") +
                    "; -fx-font-size: 12px; -fx-font-weight: " + (date.equals(today) ? "bold" : "normal") + ";");
            cell.getChildren().add(lblDay);

            // Points de consultations
            for (Consultation c : consultsDuJour.stream().limit(3).toList()) {
                String dotColor = statutColor(c.getStatut());
                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 8px;");
                cell.getChildren().add(dot);
            }
            if (consultsDuJour.size() > 3) {
                Label more = new Label("+" + (consultsDuJour.size() - 3));
                more.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 8px;");
                cell.getChildren().add(more);
            }

            // Tooltip on hover
            if (!consultsDuJour.isEmpty()) {
                cell.setOnMouseClicked(ev -> showDayConsultations(date, consultsDuJour));
                cell.setOnMouseEntered(ev -> cell.setStyle("-fx-background-color: rgba(0,217,255,0.08); " +
                        "-fx-border-color: rgba(0,217,255,0.40); " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"));
                cell.setOnMouseExited(ev -> cell.setStyle("-fx-background-color: " + cellBg + "; " +
                        "-fx-border-color: " + border + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"));
            }

            calendarGrid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private void showDayConsultations(LocalDate date, List<Consultation> consults) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Consultations du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        pane.getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        pane.setPrefWidth(420);

        VBox box = new VBox(10); box.setPadding(new Insets(16));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (Consultation c : consults) {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #112016; -fx-border-color: rgba(0,217,255,0.15); " +
                    "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
            Label lblTime = new Label(c.getDateConsultation() != null ? c.getDateConsultation().format(fmt) : "");
            lblTime.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 45;");
            VBox info = new VBox(2);
            Label lblPat = new Label(c.getPatientFullName());
            lblPat.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label lblType = new Label(c.getTypeDisplay() + " · " + c.getStatutDisplay());
            lblType.setStyle("-fx-text-fill: rgba(200,255,220,0.50); -fx-font-size: 10px;");
            info.getChildren().addAll(lblPat, lblType);
            row.getChildren().addAll(lblTime, info);
            box.getChildren().add(row);
        }
        pane.setContent(box);
        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    //  ✅ GÉNÉRATION IA DU COMPTE RENDU (Groq/Llama)
    // ═══════════════════════════════════════════════════════

    private void genererCompteRenduIA(Consultation c, TextArea targetArea) {
        if (targetArea == null) return;
        targetArea.setText("⏳ Génération en cours...");
        targetArea.setDisable(true);

        new Thread(() -> {
            try {
                String prompt = String.format(
                    "Tu es un psychologue clinicien. Génère un compte rendu professionnel et structuré " +
                    "pour une consultation psychologique avec les informations suivantes :\n" +
                    "- Patient : %s\n" +
                    "- Psychologue : Dr. %s\n" +
                    "- Date : %s\n" +
                    "- Type : %s\n" +
                    "- Statut : %s\n\n" +
                    "Le compte rendu doit inclure :\n" +
                    "1. Résumé de la séance\n" +
                    "2. Observations cliniques\n" +
                    "3. Objectifs thérapeutiques\n" +
                    "4. Recommandations pour la prochaine séance\n\n" +
                    "Réponds en français, de manière professionnelle et concise (300-400 mots).",
                    c.getPatientFullName(),
                    c.getPsyFullName(),
                    c.getDateConsultation() != null ? c.getDateConsultation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A",
                    c.getTypeDisplay(),
                    c.getStatutDisplay()
                );

                // Appel à GeminiService (Groq)
                String generated = callGeminiForCR(prompt);

                Platform.runLater(() -> {
                    targetArea.setText(generated);
                    targetArea.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    targetArea.setText("Erreur lors de la génération : " + ex.getMessage());
                    targetArea.setDisable(false);
                });
            }
        }).start();
    }

    private String callGeminiForCR(String prompt) {
        // Utilise GeminiService via reflection ou appel direct
        // Puisque GeminiService n'expose pas une méthode générique, on utilise
        // le même pattern HTTP
        try {
            var httpClient = java.net.http.HttpClient.newHttpClient();
            var gson = new com.google.gson.Gson();
            String apiKey = "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";
            String apiUrl = "https://api.groq.com/openai/v1/chat/completions";
            String model  = "llama-3.3-70b-versatile";

            String body = """
                {
                    "model": "%s",
                    "messages": [{"role": "user", "content": %s}],
                    "max_tokens": 700
                }
                """.formatted(model, gson.toJson(prompt));

            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();

            var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var obj = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
                return obj.getAsJsonArray("choices").get(0).getAsJsonObject()
                        .getAsJsonObject("message").get("content").getAsString().trim();
            }
        } catch (Exception e) {
            System.err.println("AI CR generation error: " + e.getMessage());
        }
        return "Compte rendu non disponible. Veuillez rédiger manuellement.";
    }

    // ─── COMPTES RENDUS — CRUD ────────────────────────────────
    private void loadComptesRendus() {
        if (crListPane == null) return;
        crListPane.getChildren().clear();
        try {
            List<CompteRendu> list = service.getComptesRendusParPsy(idPsy);
            if (lblCR != null) lblCR.setText(list.size() + " compte" + (list.size() > 1 ? "s" : "") + " rendu" + (list.size() > 1 ? "s" : ""));

            if (list.isEmpty()) {
                Label empty = new Label("Aucun compte rendu.");
                empty.setStyle("-fx-text-fill: rgba(200,255,220,0.40); -fx-font-size: 13px; -fx-padding: 20;");
                crListPane.getChildren().add(empty);
                return;
            }
            for (CompteRendu cr : list) {
                VBox card = buildCRCard(cr);
                crListPane.getChildren().add(card);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private VBox buildCRCard(CompteRendu cr) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #0D1F12; -fx-border-color: rgba(0,217,255,0.12); " +
                "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");

        // Header
        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        VBox info = new VBox(2);
        Label lblPat = new Label(cr.getPatientFullName());
        lblPat.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label lblDate = new Label(cr.getDateRedaction() != null ? cr.getDateRedaction().format(FMT) : "");
        lblDate.setStyle("-fx-text-fill: rgba(0,217,255,0.55); -fx-font-size: 10px;");
        info.getChildren().addAll(lblPat, lblDate);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnEdit = cardBtn("Modifier",   C_CYAN);
        Button btnDel  = cardBtn("Supprimer",  C_DANGER);
        btnEdit.setOnAction(e -> editCR(cr));
        btnDel.setOnAction(e  -> deleteCR(cr));
        header.getChildren().addAll(info, sp, btnEdit, btnDel);

        // Contenu
        Label contenu = new Label(cr.getContenuResume());
        contenu.setWrapText(true);
        contenu.setStyle("-fx-text-fill: rgba(200,255,220,0.65); -fx-font-size: 12px;");

        card.getChildren().addAll(header, contenu);
        return card;
    }

    @FXML
    private void handleAddCR() {
        // Select consultation first
        try {
            List<Consultation> consults = service.getConsultationsParPsychologue(idPsy);
            if (consults.isEmpty()) {
                alert("Info", "Aucune consultation disponible.", Alert.AlertType.INFORMATION);
                return;
            }
            openCRDialogNew(consults);
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void openCRDialog(Consultation c) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau compte rendu");
        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        ButtonType save   = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler",     ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        VBox form = new VBox(14); form.setPadding(new Insets(22));
        Label title = new Label("Compte rendu — " + c.getPatientFullName());
        title.setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 16px; -fx-font-weight: bold;");

        TextArea ta = new TextArea();
        ta.setPromptText("Redigez le compte rendu de la consultation...");
        ta.setPrefRowCount(7); ta.setWrapText(true);
        styleTextArea(ta);

        // ✅ Bouton génération IA
        Button btnIA = new Button("🤖 Générer avec l'IA");
        btnIA.setStyle("-fx-background-color: rgba(0,217,255,0.12); -fx-text-fill: #00D9FF; " +
                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 7 16; " +
                "-fx-border-color: rgba(0,217,255,0.30); -fx-border-width: 1; " +
                "-fx-border-radius: 14; -fx-background-radius: 14; -fx-cursor: hand;");
        btnIA.setOnAction(e -> genererCompteRenduIA(c, ta));

        form.getChildren().addAll(title, btnIA, labeled("Contenu", ta));
        pane.setContent(form);
        styleDialogBtn(pane.lookupButton(save),   C_GREEN);
        styleDialogBtn(pane.lookupButton(cancel), C_DANGER);

        dialog.showAndWait().ifPresent(r -> {
            if (r != save) return;
            String contenu = ta.getText().trim();
            if (contenu.isEmpty()) { alert("Erreur", "Le contenu ne peut pas etre vide.", Alert.AlertType.WARNING); return; }
            try {
                CompteRendu cr = new CompteRendu(c.getId(), contenu, LocalDateTime.now());
                service.ajouterCompteRendu(cr);
                // Marquer consultation terminee
                service.mettreAJourStatut(c.getId(), "terminee");
                loadComptesRendus(); loadDemandes();
            } catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });
    }

    private void openCRDialogNew(List<Consultation> consults) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau compte rendu");
        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        ButtonType save   = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler",     ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        VBox form = new VBox(14); form.setPadding(new Insets(22));
        Label title = new Label("Nouveau compte rendu");
        title.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:16px; -fx-font-weight:bold;");

        ComboBox<String> cmbC = new ComboBox<>();
        for (Consultation c : consults)
            cmbC.getItems().add(c.getId() + " — " + c.getPatientFullName() + " (" + (c.getDateConsultation()!=null?c.getDateConsultation().format(FMT):"") + ")");
        cmbC.getSelectionModel().selectFirst();
        cmbC.setStyle("-fx-background-color:#112016; -fx-text-fill:#E8FFF0; " +
                "-fx-border-color:rgba(0,217,255,0.20); -fx-border-width:1.5; " +
                "-fx-border-radius:10; -fx-background-radius:10;");
        cmbC.setMaxWidth(Double.MAX_VALUE);

        TextArea ta = new TextArea();
        ta.setPromptText("Redigez le compte rendu..."); ta.setPrefRowCount(6); ta.setWrapText(true);
        styleTextArea(ta);

        form.getChildren().addAll(title, labeled("Consultation", cmbC), labeled("Contenu", ta));
        pane.setContent(form);
        styleDialogBtn(pane.lookupButton(save),   C_GREEN);
        styleDialogBtn(pane.lookupButton(cancel), C_DANGER);

        dialog.showAndWait().ifPresent(r -> {
            if (r != save) return;
            int idx = cmbC.getSelectionModel().getSelectedIndex();
            if (idx < 0) return;
            String contenu = ta.getText().trim();
            if (contenu.isEmpty()) { alert("Erreur", "Contenu vide.", Alert.AlertType.WARNING); return; }
            try {
                CompteRendu cr = new CompteRendu(consults.get(idx).getId(), contenu, LocalDateTime.now());
                service.ajouterCompteRendu(cr);
                loadComptesRendus();
            } catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });
    }

    private void editCR(CompteRendu cr) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le compte rendu");
        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        ButtonType save   = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler",     ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        VBox form = new VBox(14); form.setPadding(new Insets(22));
        Label title = new Label("Modifier — " + cr.getPatientFullName());
        title.setStyle("-fx-text-fill:#00D9FF; -fx-font-size:16px; -fx-font-weight:bold;");
        TextArea ta = new TextArea(cr.getContenu());
        ta.setPrefRowCount(7); ta.setWrapText(true);
        styleTextArea(ta);
        form.getChildren().addAll(title, labeled("Contenu", ta));
        pane.setContent(form);
        styleDialogBtn(pane.lookupButton(save),   C_CYAN);
        styleDialogBtn(pane.lookupButton(cancel), C_DANGER);

        dialog.showAndWait().ifPresent(r -> {
            if (r != save) return;
            String contenu = ta.getText().trim();
            if (contenu.isEmpty()) { alert("Erreur", "Contenu vide.", Alert.AlertType.WARNING); return; }
            cr.setContenu(contenu);
            try { service.modifierCompteRendu(cr); loadComptesRendus(); }
            catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
        });
    }

    private void deleteCR(CompteRendu cr) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Supprimer"); conf.setHeaderText(null);
        conf.setContentText("Supprimer ce compte rendu definitvement ?");
        styleDialog(conf.getDialogPane());
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try { service.supprimerCompteRendu(cr.getId()); loadComptesRendus(); }
                catch (SQLException ex) { alert("Erreur", ex.getMessage(), Alert.AlertType.ERROR); }
            }
        });
    }

    // ─── Helpers ──────────────────────────────────────────────
    private String statutColor(String s) {
        if (s == null) return "#E8FFF0";
        switch (s) {
            case "planifiee": return C_CYAN;
            case "en_cours":  return C_ORANGE;
            case "terminee":  return C_GREEN;
            case "annulee":   return C_DANGER;
            default:          return "#E8FFF0";
        }
    }
    private String statutDotStyle(String s) {
        return "-fx-fill:" + statutColor(s) + "; " +
                "-fx-effect: dropshadow(gaussian," + statutColor(s) + ",8,0.6,0,0);";
    }
    private String initials(String n, String p) {
        return (n!=null&&!n.isEmpty()?String.valueOf(n.charAt(0)).toUpperCase():"") +
                (p!=null&&!p.isEmpty()?String.valueOf(p.charAt(0)).toUpperCase():"");
    }
    private Button cardBtn(String text, String color) {
        Button b = new Button(text);
        String base = "-fx-background-color:transparent; -fx-text-fill:"+color+"; " +
                "-fx-font-size:10px; -fx-font-weight:600; -fx-padding:5 10; " +
                "-fx-border-color:"+color+"; -fx-border-width:1; " +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;";
        String hov  = "-fx-background-color:"+color+"; -fx-text-fill:"+C_DARK+"; " +
                "-fx-font-size:10px; -fx-font-weight:600; -fx-padding:5 10; " +
                "-fx-border-color:"+color+"; -fx-border-width:1; " +
                "-fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hov));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }
    private void styleTextArea(TextArea ta) {
        ta.setStyle("-fx-control-inner-background:#112016; -fx-text-fill:#E8FFF0; " +
                "-fx-prompt-text-fill:rgba(200,255,220,0.25); -fx-font-size:12px; " +
                "-fx-border-color:rgba(0,217,255,0.18); -fx-border-width:1.5; " +
                "-fx-border-radius:12; -fx-background-radius:12;");
    }
    private VBox labeled(String lbl, javafx.scene.Node f) {
        Label l = new Label(lbl);
        l.setStyle("-fx-text-fill:rgba(200,255,220,0.55); -fx-font-size:10px; -fx-font-weight:bold;");
        VBox b = new VBox(4, l, f); return b;
    }
    private void styleDialog(DialogPane p) {
        p.setStyle("-fx-background-color:#0D1F12; -fx-border-color:rgba(0,217,255,0.25); " +
                "-fx-border-width:1.5; -fx-border-radius:18; -fx-background-radius:18;");
        try { p.lookup(".content.label").setStyle("-fx-text-fill:#E8FFF0; -fx-font-size:13px;"); }
        catch (Exception ignored) {}
    }
    private void styleDialogBtn(javafx.scene.Node btn, String color) {
        if (btn != null) btn.setStyle("-fx-background-color:"+color+"; -fx-text-fill:"+C_DARK+"; " +
                "-fx-font-weight:bold; -fx-padding:9 22; -fx-background-radius:18; -fx-cursor:hand;");
    }
    private void alert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleDialog(a.getDialogPane()); Platform.runLater(a::showAndWait);
    }
    private void animateCard(VBox card, int i) {
        card.setOpacity(0); card.setTranslateY(16);
        PauseTransition d = new PauseTransition(Duration.millis(i * 70));
        d.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(320), card);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(320), card);
            tt.setFromY(16); tt.setToY(0);
            new ParallelTransition(ft, tt).play();
        });
        d.play();
    }
    private void animateOrbs() {
        animateOrb(orb1, 40, -25, 14); animateOrb(orb2, -50, 35, 18);
    }
    private void animateOrb(Circle o, double dx, double dy, double s) {
        if (o == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.seconds(s), o);
        tt.setByX(dx); tt.setByY(dy); tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true); tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
    }
}
