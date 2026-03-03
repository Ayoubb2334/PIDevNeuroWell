package services;

import entities.Facture;
import entities.Paiement;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.io.File;
import java.util.Properties;

public class MailService {

    // ══════════════ CONFIGURATION SMTP ══════════════
    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final int    SMTP_PORT      = 587;
    private static final String EMAIL_FROM     = "khalilfelhi8@gmail.com";
    private static final String EMAIL_PASSWORD = "ehtfxujtsubucicn";
    private static final String APP_NAME       = "NeuroWell";

    // ══════════════════════════════════════════════════════
    //  Envoi avec PDF en pièce jointe
    // ══════════════════════════════════════════════════════
    public void sendPaymentConfirmation(String toEmail, String prenom, String nom,
                                        Paiement paiement, Facture facture,
                                        String pdfPath) {
        System.out.println("🚀 sendPaymentConfirmation CALLED → " + toEmail);
        if (pdfPath != null) {
            System.out.println("📎 PDF path = " + pdfPath);
            System.out.println("📎 PDF exists = " + new File(pdfPath).exists());
        }

        try {
            Session session = createMailSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, APP_NAME, "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("✅ Confirmation de paiement - " + facture.getNumeroFacture(), "UTF-8");

            MimeMultipart multipart = new MimeMultipart("mixed");

            // ─── HTML Body ───
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildHtmlEmail(prenom, nom, paiement, facture), "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // ─── PDF Attachment — ✅ FIX : utilise addHeader au lieu de setFileName ───
            if (pdfPath != null && new File(pdfPath).exists()) {
                MimeBodyPart pdfPart = new MimeBodyPart();

                // ✅ FIX : on attache le fichier sans appeler setFileName() qui cause le crash
                FileDataSource fds = new FileDataSource(pdfPath);
                pdfPart.setDataHandler(new DataHandler(fds));

                // ✅ On définit le nom du fichier via l'en-tête directement (évite MimeUtil.cleanContentType)
                String nomFichier = "Facture_" + facture.getNumeroFacture().replace("/", "-") + ".pdf";
                pdfPart.addHeader("Content-Disposition", "attachment; filename=\"" + nomFichier + "\"");
                pdfPart.addHeader("Content-Type", "application/pdf; name=\"" + nomFichier + "\"");

                multipart.addBodyPart(pdfPart);
                System.out.println("📎 PDF attaché avec succès : " + nomFichier);
            } else {
                System.out.println("⚠️ PDF non trouvé, email envoyé sans pièce jointe.");
            }

            message.setContent(multipart);

            Transport transport = session.getTransport("smtp");
            transport.connect(SMTP_HOST, SMTP_PORT, EMAIL_FROM, EMAIL_PASSWORD);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            System.out.println("✅ Email envoyé avec succès à : " + toEmail);

        } catch (AuthenticationFailedException e) {
            System.err.println("❌ Authentification Gmail échouée.");
            System.err.println("   → Vérifiez que la validation en 2 étapes est activée.");
            System.err.println("   → Vérifiez que EMAIL_PASSWORD est bien l'App Password (16 car, sans espaces).");
            System.err.println("   → Détail : " + e.getMessage());
        } catch (MessagingException e) {
            System.err.println("⚠️ Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("⚠️ Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  Envoi simple (test sans PDF)
    // ══════════════════════════════════════════════════════
    public void sendSimpleConfirmation(String toEmail, String prenom, Paiement paiement) {
        System.out.println("🚀 sendSimpleConfirmation → " + toEmail);
        try {
            Session session = createMailSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, APP_NAME, "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("✅ Paiement reçu - NeuroWell", "UTF-8");
            message.setText(
                    "Bonjour " + prenom + ",\n\n"
                            + "Votre paiement de " + String.format("%.2f", paiement.getMontant())
                            + " DT a été reçu avec succès.\n"
                            + "Référence : " + paiement.getReference() + "\n\n"
                            + "Merci de votre confiance,\nL'équipe NeuroWell",
                    "UTF-8"
            );

            Transport transport = session.getTransport("smtp");
            transport.connect(SMTP_HOST, SMTP_PORT, EMAIL_FROM, EMAIL_PASSWORD);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            System.out.println("✅ Email simple envoyé à : " + toEmail);

        } catch (AuthenticationFailedException e) {
            System.err.println("❌ Authentification Gmail échouée : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("⚠️ Erreur envoi email simple : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  PRIVATE : Session SMTP Gmail
    // ══════════════════════════════════════════════════════
    private Session createMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",              SMTP_HOST);
        props.put("mail.smtp.port",              String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust",         SMTP_HOST);
        props.put("mail.smtp.ssl.protocols",     "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout",           "10000");
        props.put("mail.smtp.writetimeout",      "10000");

        Session session = Session.getInstance(props);
        session.setDebug(false);
        return session;
    }

    // ══════════════════════════════════════════════════════
    //  PRIVATE : Template HTML
    // ══════════════════════════════════════════════════════
    private String buildHtmlEmail(String prenom, String nom,
                                  Paiement paiement, Facture facture) {

        String dateStr = paiement.getDatePaiement()
                .format(java.time.format.DateTimeFormatter.ofPattern(
                        "dd MMMM yyyy", java.util.Locale.FRENCH));

        String montantStr = String.format("%.2f DT", paiement.getMontant());

        return String.format("""
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Confirmation de paiement – NeuroWell</title>
</head>
<body style="margin:0;padding:0;background:#0a0e1a;font-family:Arial,sans-serif;color:#ffffff;">

  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a0e1a;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0"
               style="background:#131929;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.5);">

          <tr>
            <td style="background:linear-gradient(135deg,#6c63ff,#48cfad);padding:32px;text-align:center;">
              <h1 style="margin:0;font-size:28px;color:#ffffff;letter-spacing:1px;">✅ Paiement confirmé</h1>
              <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:15px;">NeuroWell – Plateforme de santé mentale</p>
            </td>
          </tr>

          <tr>
            <td style="padding:32px;">
              <p style="font-size:16px;margin:0 0 24px;">
                Bonjour <strong>%s %s</strong>,<br><br>
                Nous avons bien reçu votre paiement. Voici le récapitulatif :
              </p>

              <table width="100%%" cellpadding="12" cellspacing="0"
                     style="background:#1e2a42;border-radius:8px;font-size:14px;">
                <tr>
                  <td style="color:#a0aec0;border-bottom:1px solid #2d3748;">Référence paiement</td>
                  <td style="text-align:right;font-weight:bold;border-bottom:1px solid #2d3748;">%s</td>
                </tr>
                <tr>
                  <td style="color:#a0aec0;border-bottom:1px solid #2d3748;">Numéro de facture</td>
                  <td style="text-align:right;font-weight:bold;border-bottom:1px solid #2d3748;">%s</td>
                </tr>
                <tr>
                  <td style="color:#a0aec0;border-bottom:1px solid #2d3748;">Méthode de paiement</td>
                  <td style="text-align:right;font-weight:bold;border-bottom:1px solid #2d3748;">%s</td>
                </tr>
                <tr>
                  <td style="color:#a0aec0;border-bottom:1px solid #2d3748;">Date</td>
                  <td style="text-align:right;font-weight:bold;border-bottom:1px solid #2d3748;">%s</td>
                </tr>
                <tr>
                  <td style="color:#a0aec0;font-size:16px;"><strong>Montant total</strong></td>
                  <td style="text-align:right;font-size:20px;font-weight:bold;color:#48cfad;">%s</td>
                </tr>
              </table>

              <p style="margin:24px 0 0;font-size:14px;color:#a0aec0;">
                Votre facture est disponible en pièce jointe (PDF).<br>
                Conservez ce document pour vos archives.
              </p>
            </td>
          </tr>

          <tr>
            <td style="background:#0d1321;padding:20px;text-align:center;">
              <p style="margin:0;font-size:12px;color:#4a5568;">
                © 2026 NeuroWell · Tous droits réservés<br>
                Cet email a été envoyé automatiquement, merci de ne pas y répondre.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>

</body>
</html>
""",
                prenom, nom,
                paiement.getReference(),
                facture.getNumeroFacture(),
                paiement.getModePaiement(),
                dateStr,
                montantStr
        );
    }
}