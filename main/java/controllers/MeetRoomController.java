package controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
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

import java.awt.Desktop;
import java.net.URI;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class MeetRoomController {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DARK   = "#050C07";
    private static final String C_DANGER = "#FF4D6D";

    @FXML private StackPane rootPane;
    @FXML private VBox      mainCard;

    @FXML private Circle orb1;
    @FXML private Circle orb2;
    @FXML private Circle orb3;
    @FXML private Circle orb4;

    @FXML private Label  lblEventTitle;
    @FXML private Label  lblMode;
    @FXML private Label  lblDate;
    @FXML private Label  lblLiveIndicator;
    @FXML private Label  lblMeetUrl;
    @FXML private Label  lblCountdown;
    @FXML private Label  lblCountdownSub;
    @FXML private Label  lblStatus;
    @FXML private Label  lblParticipantCount;

    @FXML private Button btnCopyLink;
    @FXML private Button btnJoinNow;
    @FXML private Button btnCancel;

    @FXML private HBox   avatarsBox;
    @FXML private Circle statusDot;

    private String   meetUrl;
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        animateOrbs();
        animateCard();
        animateLiveIndicator();
        buildAvatars();
        wireButtons();
    }

    public void initMeet(String titre, String meetLink, Timestamp date, String mode) {
        this.meetUrl = meetLink;

        if (lblEventTitle != null) {
            lblEventTitle.setText(titre);
            fadeIn(lblEventTitle, 0);
        }

        if (lblMode != null) {
            boolean online = "distanciel".equalsIgnoreCase(mode);
            String color   = online ? C_CYAN : C_GREEN;
            String bg      = online ? "rgba(0,217,255,0.12)" : "rgba(0,255,136,0.12)";
            lblMode.setText(online ? "Session en ligne" : "Presentiel");
            lblMode.setStyle(
                    "-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold; " +
                            "-fx-background-color: " + bg + "; -fx-padding: 5 14; " +
                            "-fx-background-radius: 20; -fx-border-color: " + color + "; " +
                            "-fx-border-width: 1; -fx-border-radius: 20;"
            );
        }

        if (lblDate != null && date != null) {
            lblDate.setText(date.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy  -  HH:mm")));
        }

        if (lblMeetUrl != null) {
            lblMeetUrl.setText(meetLink != null ? meetLink : "Generation en cours...");
        }

        setStatus(
                meetLink != null && !meetLink.isEmpty() ? "Reunion prete" : "Generation du lien...",
                meetLink != null && !meetLink.isEmpty() ? C_GREEN : C_CYAN
        );

        if (countdownTimeline != null) countdownTimeline.stop();
        startCountdown(date);
    }

    private void startCountdown(Timestamp start) {
        if (lblCountdown == null) return;

        if (start == null) {
            lblCountdown.setText("En direct");
            if (lblCountdownSub != null) lblCountdownSub.setText("La reunion est disponible");
            return;
        }

        Runnable tick = () -> {
            long diff = start.getTime() - System.currentTimeMillis();
            if (diff <= 0) {
                lblCountdown.setText("EN DIRECT");
                lblCountdown.setStyle(
                        "-fx-text-fill: " + C_GREEN + "; -fx-font-size: 36px; -fx-font-weight: bold;"
                );
                if (lblCountdownSub != null) lblCountdownSub.setText("La reunion a commence !");
                if (countdownTimeline != null) countdownTimeline.stop();
                return;
            }
            long h = diff / 3_600_000;
            long m = (diff % 3_600_000) / 60_000;
            long s = (diff % 60_000)    / 1_000;
            lblCountdown.setText(h > 0
                    ? String.format("%02d:%02d:%02d", h, m, s)
                    : String.format("%02d:%02d", m, s));
            if (lblCountdownSub != null) {
                if (h > 0)      lblCountdownSub.setText("Debut dans " + h + "h " + m + "min");
                else if (m > 0) lblCountdownSub.setText("Debut dans " + m + " minute" + (m > 1 ? "s" : ""));
                else            lblCountdownSub.setText("Debut dans quelques secondes !");
            }
        };

        tick.run();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick.run()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void buildAvatars() {
        if (avatarsBox == null) return;
        avatarsBox.getChildren().clear();

        String[] colors   = { C_CYAN, C_GREEN, "#FFB347", "#FF4D6D", "#7FFF00" };
        String[] initials = { "AY",   "ME",    "SO",      "KA",      "YO"      };

        for (int i = 0; i < 5; i++) {
            StackPane avatar = new StackPane();

            Circle bg = new Circle(18);
            bg.setFill(Color.web(colors[i], 0.25));
            bg.setStroke(Color.web(colors[i], 0.80));
            bg.setStrokeWidth(1.5);

            Label lbl = new Label(initials[i]);
            lbl.setStyle("-fx-text-fill: " + colors[i] + "; -fx-font-size: 10px; -fx-font-weight: bold;");

            DropShadow ds = new DropShadow();
            ds.setColor(Color.web(colors[i], 0.40));
            ds.setRadius(8);
            avatar.setEffect(ds);

            avatar.getChildren().addAll(bg, lbl);
            avatar.setTranslateX(i * -8.0);
            avatarsBox.getChildren().add(avatar);
            fadeIn(avatar, i * 80L);
        }

        if (lblParticipantCount != null) lblParticipantCount.setText("+5 participants");
    }

    private void wireButtons() {
        if (btnJoinNow != null) {
            styleJoinButton(btnJoinNow);
            btnJoinNow.setOnAction(e -> joinMeet());
        }
        if (btnCopyLink != null) {
            btnCopyLink.setOnAction(e -> copyLink());
        }
        if (btnCancel != null) {
            btnCancel.setOnAction(e -> {
                if (countdownTimeline != null) countdownTimeline.stop();
                rootPane.getScene().getWindow().hide();
            });
        }
    }

    private void joinMeet() {
        if (meetUrl == null || meetUrl.isEmpty()) {
            setStatus("Lien non disponible", C_DANGER);
            return;
        }
        ScaleTransition sc = new ScaleTransition(Duration.millis(120), btnJoinNow);
        sc.setToX(0.94);
        sc.setToY(0.94);
        sc.setAutoReverse(true);
        sc.setCycleCount(2);
        sc.setOnFinished(e -> openBrowser());
        sc.play();
    }

    private void openBrowser() {
        try {
            URI uri = new URI(meetUrl);
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux"))    new ProcessBuilder("xdg-open", meetUrl).start();
                else if (os.contains("mac")) new ProcessBuilder("open",     meetUrl).start();
            }
            setStatus("Navigateur ouvert", C_GREEN);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Erreur ouverture lien", C_DANGER);
        }
    }

    private void copyLink() {
        if (meetUrl == null) return;
        Clipboard        cb = Clipboard.getSystemClipboard();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(meetUrl);
        cb.setContent(cc);

        if (btnCopyLink != null) {
            String orig = btnCopyLink.getText();
            btnCopyLink.setText("Copie !");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> btnCopyLink.setText(orig));
            pause.play();
        }
        setStatus("Lien copie", C_GREEN);
    }

    private void setStatus(String msg, String color) {
        if (lblStatus != null) {
            lblStatus.setText(msg);
            lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        }
        if (statusDot != null) statusDot.setFill(Color.web(color));
    }

    private void animateOrbs() {
        Circle[] orbs = { orb1,  orb2,  orb3,  orb4  };
        double[] dx   = {  40,   -50,    30,   -35    };
        double[] dy   = { -30,    40,   -50,    30    };
        double[] secs = {  14,    18,    20,    16    };

        for (int i = 0; i < orbs.length; i++) {
            if (orbs[i] == null) continue;

            TranslateTransition tt = new TranslateTransition(Duration.seconds(secs[i]), orbs[i]);
            tt.setByX(dx[i]);
            tt.setByY(dy[i]);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();

            Glow glow = new Glow(0.1);
            orbs[i].setEffect(glow);
            Timeline gl = new Timeline(
                    new KeyFrame(Duration.ZERO,                   new KeyValue(glow.levelProperty(), 0.1)),
                    new KeyFrame(Duration.seconds(secs[i] / 2.0), new KeyValue(glow.levelProperty(), 0.6))
            );
            gl.setAutoReverse(true);
            gl.setCycleCount(Animation.INDEFINITE);
            gl.play();
        }
    }

    private void animateCard() {
        if (mainCard == null) return;
        mainCard.setOpacity(0);
        mainCard.setTranslateY(30);

        FadeTransition ft = new FadeTransition(Duration.millis(600), mainCard);
        ft.setToValue(1);
        ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(600), mainCard);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        tt.play();
    }

    private void animateLiveIndicator() {
        if (lblLiveIndicator == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(700), lblLiveIndicator);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void styleJoinButton(Button btn) {
        String base =
                "-fx-background-color: linear-gradient(to right, " + C_CYAN + ", " + C_GREEN + "); " +
                        "-fx-text-fill: " + C_DARK + "; -fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-padding: 16 48; -fx-background-radius: 40; -fx-cursor: hand;";
        btn.setStyle(base);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(C_CYAN, 0.45));
        glow.setRadius(24);
        btn.setEffect(glow);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(base.replace(C_CYAN + ", " + C_GREEN, C_GREEN + ", " + C_CYAN));
            DropShadow hg = new DropShadow();
            hg.setColor(Color.web(C_GREEN, 0.60));
            hg.setRadius(30);
            btn.setEffect(hg);
            ScaleTransition st = new ScaleTransition(Duration.millis(140), btn);
            st.setToX(1.04);
            st.setToY(1.04);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(base);
            btn.setEffect(glow);
            ScaleTransition st = new ScaleTransition(Duration.millis(140), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void fadeIn(Node node, long delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setDelay(Duration.millis(delayMs));
        ft.setToValue(1);
        ft.play();
    }
}