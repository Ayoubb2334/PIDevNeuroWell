package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.EmailService;

import java.awt.Desktop;
import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MeetRoomController {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DARK   = "#050C07";
    private static final String C_DANGER = "#FF4D6D";

    @FXML private StackPane rootPane;
    @FXML private VBox      mainCard;

    @FXML private Circle orb1, orb2, orb3, orb4;

    @FXML private Label  lblEventTitle;
    @FXML private Label  lblMode;
    @FXML private Label  lblDate;
    @FXML private Label  lblLiveIndicator;
    @FXML private Label  lblMeetUrl;
    @FXML private Label  lblCountdown;
    @FXML private Label  lblCountdownSub;
    @FXML private Label  lblStatus;
    @FXML private Circle statusDot;

    @FXML private TextField  txtParticipantName;
    @FXML private Button     btnAddParticipant;
    @FXML private Button     btnSendInvitations;
    @FXML private VBox       participantsListBox;
    @FXML private Label      lblParticipantCount;

    @FXML private Spinner<Integer> spinnerHours;
    @FXML private Spinner<Integer> spinnerMinutes;
    @FXML private Button     btnSetTime;

    @FXML private Button btnCopyLink;
    @FXML private Button btnJoinNow;
    @FXML private Button btnCancel;

    private String             meetUrl;
    private String             eventTitle;
    private LocalDateTime      meetDateTime;
    private Timeline           countdownTimeline;
    private final List<String> emailList   = new ArrayList<>();
    private final EmailService emailService = new EmailService();

    // =========================================================================
    @FXML
    public void initialize() {
        animateOrbs();
        animateCard();
        animateLiveIndicator();
        setupSpinners();
        wireButtons();
    }

    public void initMeet(String titre, String meetLink, Timestamp date, String mode) {
        this.meetUrl    = meetLink;
        this.eventTitle = titre;
        this.meetDateTime = date != null ? date.toLocalDateTime() : null;

        if (lblEventTitle != null) {
            lblEventTitle.setText(titre);
            lblEventTitle.setOpacity(0);
            FadeTransition ft0 = new FadeTransition(Duration.millis(400), lblEventTitle);
            ft0.setToValue(1);
            ft0.play();
        }

        if (lblMode != null) {
            boolean online = "distanciel".equalsIgnoreCase(mode);
            String color   = online ? C_CYAN : C_GREEN;
            String bg      = online ? "rgba(0,217,255,0.12)" : "rgba(0,255,136,0.12)";
            lblMode.setText(online ? "Session en ligne" : "Présentiel");
            lblMode.setStyle(
                    "-fx-text-fill:" + color + ";-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-background-color:" + bg + ";-fx-padding:5 14;" +
                            "-fx-background-radius:20;-fx-border-color:" + color + ";" +
                            "-fx-border-width:1;-fx-border-radius:20;");
        }

        if (lblDate != null && date != null)
            lblDate.setText(date.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy  -  HH:mm")));

        if (lblMeetUrl != null)
            lblMeetUrl.setText(meetLink != null ? meetLink : "Génération en cours...");

        setStatus(
                meetLink != null && !meetLink.isEmpty() ? "Réunion prête"       : "Génération du lien...",
                meetLink != null && !meetLink.isEmpty() ? C_GREEN               : C_CYAN
        );

        if (countdownTimeline != null) countdownTimeline.stop();
        startCountdown(date);
    }

    // =========================================================================

    private void setupParticipantInput() {
        if (txtParticipantName == null) return;

        txtParticipantName.setStyle(
                "-fx-background-color:rgba(0,217,255,0.07);-fx-text-fill:white;" +
                        "-fx-prompt-text-fill:rgba(255,255,255,0.35);-fx-font-size:13px;" +
                        "-fx-padding:10 14;-fx-background-radius:12;" +
                        "-fx-border-color:rgba(0,217,255,0.25);-fx-border-width:1.5;" +
                        "-fx-border-radius:12;");

        txtParticipantName.setOnAction(e -> handleAddParticipant());
    }

    @FXML
    private void handleAddParticipant() {
        if (txtParticipantName == null || participantsListBox == null) return;

        String email = txtParticipantName.getText().trim();

        if (email.isEmpty() || !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            txtParticipantName.setStyle(
                    "-fx-background-color:rgba(255,77,109,0.12);-fx-text-fill:white;" +
                            "-fx-prompt-text-fill:rgba(255,255,255,0.35);-fx-font-size:13px;" +
                            "-fx-padding:10 14;-fx-background-radius:12;" +
                            "-fx-border-color:" + C_DANGER + ";-fx-border-width:1.5;-fx-border-radius:12;");
            shake(txtParticipantName);
            setStatus("Email invalide", C_DANGER);
            return;
        }

        if (emailList.contains(email)) {
            setStatus("Email deja ajoute", C_DANGER);
            shake(txtParticipantName);
            return;
        }

        txtParticipantName.setStyle(
                "-fx-background-color:rgba(0,217,255,0.07);-fx-text-fill:white;" +
                        "-fx-prompt-text-fill:rgba(255,255,255,0.35);-fx-font-size:13px;" +
                        "-fx-padding:10 14;-fx-background-radius:12;" +
                        "-fx-border-color:rgba(0,217,255,0.25);-fx-border-width:1.5;-fx-border-radius:12;");

        emailList.add(email);
        txtParticipantName.clear();

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color:rgba(0,217,255,0.07);-fx-padding:8 14;" +
                        "-fx-background-radius:10;-fx-border-color:rgba(0,217,255,0.18);" +
                        "-fx-border-width:1;-fx-border-radius:10;");

        Label icon = new Label("✉");
        icon.setStyle("-fx-text-fill:" + C_CYAN + ";-fx-font-size:14px;");

        Label emailLbl = new Label(email);
        emailLbl.setStyle("-fx-text-fill:white;-fx-font-size:13px;");
        HBox.setHgrow(emailLbl, Priority.ALWAYS);

        Label badge = new Label("En attente");
        badge.setStyle(
                "-fx-text-fill:" + C_CYAN + ";-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-color:rgba(0,217,255,0.12);-fx-padding:3 8;" +
                        "-fx-background-radius:8;-fx-border-color:rgba(0,217,255,0.25);" +
                        "-fx-border-width:1;-fx-border-radius:8;");

        Button btnDel = new Button("✕");
        btnDel.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + C_DANGER + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:2 6;");
        btnDel.setOnAction(e -> {
            emailList.remove(email);
            participantsListBox.getChildren().remove(row);
            updateParticipantCount();
        });

        row.getChildren().addAll(icon, emailLbl, badge, btnDel);

        row.setOpacity(0);
        row.setTranslateX(-15);
        participantsListBox.getChildren().add(row);

        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setToValue(1); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), row);
        tt.setToX(0); tt.play();

        updateParticipantCount();
        setStatus(emailList.size() + " invite(s)", C_CYAN);
    }

    private void updateParticipantCount() {
        if (lblParticipantCount != null)
            lblParticipantCount.setText(emailList.size() + " invite" + (emailList.size() > 1 ? "s" : ""));
    }

    @FXML
    private void handleSendInvitations() {
        if (emailList.isEmpty()) {
            setStatus("Aucun email ajoute", C_DANGER);
            return;
        }
        if (meetUrl == null || meetUrl.isEmpty()) {
            setStatus("Lien Meet non disponible", C_DANGER);
            return;
        }

        if (btnSendInvitations != null) {
            btnSendInvitations.setDisable(true);
            btnSendInvitations.setText("Envoi en cours...");
        }
        setStatus("Envoi des invitations...", C_CYAN);

        List<String> snapshot = new ArrayList<>(emailList);
        String       url      = meetUrl;
        String       title    = eventTitle != null ? eventTitle : "Reunion";
        LocalDateTime dt      = meetDateTime;

        Thread worker = new Thread(() -> {
            try {
                emailService.sendMeetInvitations(snapshot, title, url, dt);

                Platform.runLater(() -> {
                    setStatus(snapshot.size() + " invitation(s) envoyees !", C_GREEN);
                    updateBadgesToSent();
                    if (btnSendInvitations != null) {
                        btnSendInvitations.setText("Envoye !");
                        btnSendInvitations.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setStatus("Erreur envoi : " + ex.getMessage(), C_DANGER);
                    if (btnSendInvitations != null) {
                        btnSendInvitations.setText("Reessayer");
                        btnSendInvitations.setDisable(false);
                    }
                });
            }
        }, "EmailSender-Thread");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateBadgesToSent() {
        if (participantsListBox == null) return;
        for (javafx.scene.Node node : participantsListBox.getChildren()) {
            if (node instanceof HBox row) {
                row.getChildren().stream()
                        .filter(n -> n instanceof Label lbl && "En attente".equals(lbl.getText()))
                        .forEach(n -> {
                            Label lbl = (Label) n;
                            lbl.setText("Envoye");
                            lbl.setStyle(
                                    "-fx-text-fill:" + C_GREEN + ";-fx-font-size:10px;-fx-font-weight:bold;" +
                                            "-fx-background-color:rgba(0,255,136,0.12);-fx-padding:3 8;" +
                                            "-fx-background-radius:8;-fx-border-color:rgba(0,255,136,0.25);" +
                                            "-fx-border-width:1;-fx-border-radius:8;");
                        });
            }
        }
    }

    // =========================================================================

    private void setupSpinners() {
        if (spinnerHours == null || spinnerMinutes == null) return;

        spinnerHours.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalDateTime.now().getHour()));
        spinnerMinutes.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, LocalDateTime.now().getMinute()));

        String spinStyle =
                "-fx-background-color:rgba(0,217,255,0.07);-fx-text-fill:white;" +
                        "-fx-border-color:rgba(0,217,255,0.25);-fx-border-width:1.5;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-pref-width:80;";
        spinnerHours.setStyle(spinStyle);
        spinnerMinutes.setStyle(spinStyle);

        spinnerHours.getEditor().setStyle("-fx-text-fill:white;-fx-background-color:transparent;-fx-alignment:center;");
        spinnerMinutes.getEditor().setStyle("-fx-text-fill:white;-fx-background-color:transparent;-fx-alignment:center;");
    }

    @FXML
    private void handleSetTime() {
        if (spinnerHours == null || spinnerMinutes == null) return;

        int h = spinnerHours.getValue();
        int m = spinnerMinutes.getValue();

        LocalDateTime chosen = LocalDateTime.now()
                .withHour(h).withMinute(m).withSecond(0).withNano(0);
        Timestamp ts = Timestamp.valueOf(chosen);

        if (lblDate != null)
            lblDate.setText(chosen.format(DateTimeFormatter.ofPattern("dd MMMM yyyy  -  HH:mm")));

        if (countdownTimeline != null) countdownTimeline.stop();
        startCountdown(ts);

        setStatus("Heure définie : " + String.format("%02d:%02d", h, m), C_GREEN);
        pulseLabel(lblDate);
    }

    // =========================================================================

    private void startCountdown(Timestamp start) {
        if (lblCountdown == null) return;

        if (start == null) {
            lblCountdown.setText("En direct");
            if (lblCountdownSub != null) lblCountdownSub.setText("La réunion est disponible");
            return;
        }

        Runnable tick = () -> {
            long diff = start.getTime() - System.currentTimeMillis();
            if (diff <= 0) {
                lblCountdown.setText("EN DIRECT");
                lblCountdown.setStyle("-fx-text-fill:" + C_GREEN + ";-fx-font-size:36px;-fx-font-weight:bold;");
                if (lblCountdownSub != null) lblCountdownSub.setText("La réunion a commencé !");
                if (countdownTimeline != null) countdownTimeline.stop();
                return;
            }
            long hrs = diff / 3_600_000;
            long min = (diff % 3_600_000) / 60_000;
            long sec = (diff % 60_000)    / 1_000;
            lblCountdown.setText(hrs > 0
                    ? String.format("%02d:%02d:%02d", hrs, min, sec)
                    : String.format("%02d:%02d", min, sec));
            if (lblCountdownSub != null) {
                if (hrs > 0)      lblCountdownSub.setText("Début dans " + hrs + "h " + min + "min");
                else if (min > 0) lblCountdownSub.setText("Début dans " + min + " minute" + (min > 1 ? "s" : ""));
                else              lblCountdownSub.setText("Début dans quelques secondes !");
            }
        };

        tick.run();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick.run()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    // =========================================================================

    private void wireButtons() {
        setupParticipantInput();

        if (btnSendInvitations != null) {
            styleBtn(btnSendInvitations, C_GREEN);
            btnSendInvitations.setOnAction(e -> handleSendInvitations());
        }
        if (btnAddParticipant != null) {
            styleBtn(btnAddParticipant, C_CYAN);
            btnAddParticipant.setOnAction(e -> handleAddParticipant());
        }
        if (btnSetTime != null) {
            styleBtn(btnSetTime, C_GREEN);
            btnSetTime.setOnAction(e -> handleSetTime());
        }
        if (btnJoinNow != null) {
            styleJoinButton(btnJoinNow);
            btnJoinNow.setOnAction(e -> joinMeet());
        }
        if (btnCopyLink != null) btnCopyLink.setOnAction(e -> copyLink());
        if (btnCancel != null) {
            btnCancel.setOnAction(e -> {
                if (countdownTimeline != null) countdownTimeline.stop();
                rootPane.getScene().getWindow().hide();
            });
        }
    }

    private void joinMeet() {
        if (meetUrl == null || meetUrl.isEmpty()) { setStatus("Lien non disponible", C_DANGER); return; }
        ScaleTransition sc = new ScaleTransition(Duration.millis(120), btnJoinNow);
        sc.setToX(0.94); sc.setToY(0.94); sc.setAutoReverse(true); sc.setCycleCount(2);
        sc.setOnFinished(e -> openBrowser());
        sc.play();
    }

    private void openBrowser() {
        try {
            URI uri = new URI(meetUrl);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(uri);
            else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux"))    new ProcessBuilder("xdg-open", meetUrl).start();
                else if (os.contains("mac")) new ProcessBuilder("open",     meetUrl).start();
            }
            setStatus("Navigateur ouvert", C_GREEN);
        } catch (Exception ex) { System.err.println("Erreur ouverture navigateur: " + ex.getMessage()); setStatus("Erreur ouverture lien", C_DANGER); }
    }

    private void copyLink() {
        if (meetUrl == null) return;
        Clipboard cb = Clipboard.getSystemClipboard();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(meetUrl);
        cb.setContent(cc);
        if (btnCopyLink != null) {
            String orig = btnCopyLink.getText();
            btnCopyLink.setText("✅ Copié !");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> btnCopyLink.setText(orig));
            pause.play();
        }
        setStatus("Lien copié", C_GREEN);
    }

    // =========================================================================

    private void setStatus(String msg, String color) {
        if (lblStatus  != null) { lblStatus.setText(msg); lblStatus.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;"); }
        if (statusDot  != null) statusDot.setFill(Color.web(color));
    }

    private void styleBtn(Button btn, String color) {
        String base = "-fx-background-color:" + color + ";-fx-text-fill:" + C_DARK + ";" +
                "-fx-font-weight:bold;-fx-padding:9 20;-fx-background-radius:12;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> { ScaleTransition s = new ScaleTransition(Duration.millis(120), btn); s.setToX(1.05); s.setToY(1.05); s.play(); });
        btn.setOnMouseExited(e ->  { ScaleTransition s = new ScaleTransition(Duration.millis(120), btn); s.setToX(1.0);  s.setToY(1.0);  s.play(); });
    }

    private void styleJoinButton(Button btn) {
        String base = "-fx-background-color:linear-gradient(to right," + C_CYAN + "," + C_GREEN + ");" +
                "-fx-text-fill:" + C_DARK + ";-fx-font-size:16px;-fx-font-weight:bold;" +
                "-fx-padding:16 48;-fx-background-radius:40;-fx-cursor:hand;";
        btn.setStyle(base);
        DropShadow glow = new DropShadow(); glow.setColor(Color.web(C_CYAN, 0.45)); glow.setRadius(24); btn.setEffect(glow);
        btn.setOnMouseEntered(e -> { btn.setStyle(base.replace(C_CYAN+","+C_GREEN, C_GREEN+","+C_CYAN)); ScaleTransition s = new ScaleTransition(Duration.millis(140), btn); s.setToX(1.04); s.setToY(1.04); s.play(); });
        btn.setOnMouseExited(e ->  { btn.setStyle(base); btn.setEffect(glow); ScaleTransition s = new ScaleTransition(Duration.millis(140), btn); s.setToX(1.0); s.setToY(1.0); s.play(); });
    }

    private void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    private void pulseLabel(Label lbl) {
        if (lbl == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(200), lbl);
        st.setFromX(1.0); st.setToX(1.08); st.setAutoReverse(true); st.setCycleCount(2); st.play();
    }

    private void animateOrbs() {
        Circle[] orbs = { orb1, orb2, orb3, orb4 };
        double[] dx   = { 40,  -50,  30, -35 };
        double[] dy   = { -30,  40, -50,  30 };
        double[] secs = { 14,   18,  20,  16 };
        for (int i = 0; i < orbs.length; i++) {
            if (orbs[i] == null) continue;
            TranslateTransition tt = new TranslateTransition(Duration.seconds(secs[i]), orbs[i]);
            tt.setByX(dx[i]); tt.setByY(dy[i]); tt.setCycleCount(Animation.INDEFINITE); tt.setAutoReverse(true);
            tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
            Glow glow = new Glow(0.1); orbs[i].setEffect(glow);
            Timeline gl = new Timeline(
                    new KeyFrame(Duration.ZERO,                   new KeyValue(glow.levelProperty(), 0.1)),
                    new KeyFrame(Duration.seconds(secs[i] / 2.0), new KeyValue(glow.levelProperty(), 0.6)));
            gl.setAutoReverse(true); gl.setCycleCount(Animation.INDEFINITE); gl.play();
        }
    }

    private void animateCard() {
        if (mainCard == null) return;
        mainCard.setOpacity(0); mainCard.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(600), mainCard);
        ft.setToValue(1);
        ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(600), mainCard);
        tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT); tt.play();
    }

    private void animateLiveIndicator() {
        if (lblLiveIndicator == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(700), lblLiveIndicator);
        ft.setFromValue(0.3); ft.setToValue(1.0); ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true); ft.play();
    }

}