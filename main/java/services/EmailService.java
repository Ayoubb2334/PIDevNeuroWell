package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;

public class EmailService {

    // ✅ Plus static, plus final → peut être assigné dynamiquement
    private String fromEmail;
    private String fromPassword;
    private static final String FROM_NAME = "NeuroWell";

    public void sendMeetInvitations(List<String> recipientEmails,
                                    String eventTitle,
                                    String meetUrl,
                                    LocalDateTime meetDateTime) {

        // ── Tableau pour récupérer les valeurs depuis le thread JavaFX ──
        final String[] credentials = new String[2]; // [0]=email, [1]=password
        final boolean[] confirmed  = {false};
        final CountDownLatch latch  = new CountDownLatch(1);

        // ── Afficher le dialog sur le thread JavaFX ──────────────────────
        Platform.runLater(() -> {
            EmailConfigDialog dialog = new EmailConfigDialog();
            boolean ok = dialog.show();
            if (ok) {
                credentials[0] = dialog.getEmail();
                credentials[1] = dialog.getPassword();
                confirmed[0]   = true;
            }
            latch.countDown(); // signaler que le dialog est fermé
        });

        // ── Attendre que l'utilisateur ferme le dialog ───────────────────
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (!confirmed[0]) {
            System.out.println("Envoi annulé par l'utilisateur.");
            return;
        }

        this.fromEmail    = credentials[0];
        this.fromPassword = credentials[1];

        // ── Envoi des emails ─────────────────────────────────────────────
        Session session = createSession();
        for (String email : recipientEmails) {
            if (email == null || !email.contains("@")) continue;
            try {
                Message msg = buildMessage(session, email, eventTitle, meetUrl, meetDateTime);
                Transport.send(msg);
                System.out.println("Email envoye a : " + email);
            } catch (MessagingException | UnsupportedEncodingException e) {
                System.err.println("Echec envoi a " + email + " : " + e.getMessage());
            }
        }
    }
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        final String email    = this.fromEmail;
        final String password = this.fromPassword;

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    private Message buildMessage(Session session,
                                 String toEmail,
                                 String eventTitle,
                                 String meetUrl,
                                 LocalDateTime dateTime)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromEmail, FROM_NAME, "UTF-8"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("Invitation : " + eventTitle, "UTF-8");

        String dateStr = dateTime != null
                ? dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'a' HH:mm"))
                : "a confirmer";

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(buildHtmlBody(eventTitle, meetUrl, dateStr, toEmail),
                "text/html; charset=UTF-8");

        Multipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(htmlPart);
        msg.setContent(multipart);
        return msg;
    }

    private String buildHtmlBody(String eventTitle, String meetUrl,
                                 String dateStr, String toEmail) {
        String safeUrl   = meetUrl    != null ? meetUrl    : "#";
        String safeTitle = eventTitle != null ? eventTitle : "";

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>"
                + "body{margin:0;padding:0;background:#050C07;font-family:Arial,sans-serif;}"
                + ".wrapper{max-width:600px;margin:0 auto;background:#0D1F12;border-radius:16px;"
                + "overflow:hidden;border:1px solid rgba(0,217,255,0.20);}"
                + ".header{background:linear-gradient(135deg,#00D9FF22,#00FF8822);"
                + "padding:36px 40px;text-align:center;border-bottom:1px solid rgba(0,217,255,0.15);}"
                + ".header h1{margin:0;color:#00D9FF;font-size:28px;font-weight:900;letter-spacing:2px;}"
                + ".header p{margin:8px 0 0;color:rgba(255,255,255,0.55);font-size:13px;}"
                + ".body{padding:36px 40px;}"
                + ".event-title{color:white;font-size:22px;font-weight:700;margin:0 0 6px;}"
                + ".date-row{color:#00FF88;font-size:14px;margin:0 0 28px;}"
                + ".info-box{background:rgba(0,217,255,0.06);border:1px solid rgba(0,217,255,0.18);"
                + "border-radius:12px;padding:20px 24px;margin-bottom:28px;}"
                + ".info-box p{margin:0 0 8px;color:rgba(255,255,255,0.65);font-size:13px;}"
                + ".info-box p:last-child{margin:0;}"
                + ".link-box{background:rgba(0,255,136,0.06);border:1px solid rgba(0,255,136,0.25);"
                + "border-radius:12px;padding:16px 20px;word-break:break-all;margin-bottom:28px;}"
                + ".link-box a{color:#00FF88;font-size:13px;text-decoration:none;}"
                + ".btn{display:block;width:fit-content;margin:0 auto 28px;"
                + "background:linear-gradient(to right,#00D9FF,#00FF88);"
                + "color:#050C07;font-weight:700;font-size:15px;"
                + "padding:14px 40px;border-radius:50px;text-decoration:none;letter-spacing:0.5px;}"
                + ".footer{text-align:center;padding:20px 40px;"
                + "border-top:1px solid rgba(0,217,255,0.10);"
                + "color:rgba(255,255,255,0.25);font-size:11px;}"
                + "</style></head><body><div class=\"wrapper\">"
                + "<div class=\"header\"><h1>&#129504; NEUROWELL</h1>"
                + "<p>Invitation a une reunion en ligne</p></div>"
                + "<div class=\"body\">"
                + "<p class=\"event-title\">" + safeTitle + "</p>"
                + "<p class=\"date-row\">&#128197; " + dateStr + "</p>"
                + "<div class=\"info-box\">"
                + "<p>&#128100; Vous avez ete invite(e) a rejoindre cette reunion.</p>"
                + "<p>&#128205; Mode : Session en ligne (Google Meet)</p>"
                + "<p>&#128231; Invite(e) : " + toEmail + "</p>"
                + "</div>"
                + "<p style=\"color:rgba(255,255,255,0.55);font-size:13px;margin:0 0 10px;\">Lien de la reunion :</p>"
                + "<div class=\"link-box\"><a href=\"" + safeUrl + "\">" + safeUrl + "</a></div>"
                + "<a class=\"btn\" href=\"" + safeUrl + "\">&#128640; Rejoindre la reunion</a>"
                + "<p style=\"color:rgba(255,255,255,0.35);font-size:12px;text-align:center;margin:0;\">"
                + "Cliquez sur le bouton ou copiez le lien dans votre navigateur.</p>"
                + "</div>"
                + "<div class=\"footer\">NeuroWell &mdash; Plateforme de bien-etre mental<br/>"
                + "Cet email a ete envoye automatiquement, merci de ne pas repondre.</div>"
                + "</div></body></html>";
    }
}