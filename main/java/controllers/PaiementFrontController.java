package controllers;

import entities.Facture;
import entities.Paiement;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.FactureService;
import services.MailService;
import services.PaiementService;

import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PaiementFrontController {

    // ══════════════ STRIPE CONFIG ══════════════
    private static final String STRIPE_SECRET_KEY =
            "sk_test_51T46O54fvd8D4XfbUJhXegPNc9X2CvJ74" +
                    "Vp9keW8mPh6vtWDKuGiiY3bsAIjs3PxWhUFWliQO78" +
                    "lmkYmq9dtkwqA00elOLzzaV";

    // ══════════════ DONNÉES SESSION ══════════════
    private static double montantConsultation = 0.0;
    private static int    consultationId      = -1;
    private static String patientNomSession   = "";
    private static String psyNomSession       = "";

    public static void setMontantConsultation(double montant) { montantConsultation = montant; }
    public static void setConsultationId(int id)              { consultationId = id; }
    public static void setPatientNom(String nom)              { patientNomSession = nom; }
    public static void setPsyNom(String nom)                  { psyNomSession = nom; }

    // ── FXML — Formulaire ──
    @FXML private VBox cardStripe, cardPaypal, cardFlouci;
    @FXML private VBox stripeSection, paypalSection, flouciSection;
    @FXML private TextField cardNumber, cardExpiry, cardCvv, cardName;
    @FXML private TextField paypalEmail;
    @FXML private TextField flouciPhone;
    @FXML private TextField billingPrenom, billingNom, billingEmail, billingPhone;
    @FXML private TextField promoCode;
    @FXML private VBox progressSection;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel, progressStep;
    @FXML private Button payButton;
    @FXML private VBox resultSection, successView, failureView;
    @FXML private Label successRef, successAmount, failureMessage;
    @FXML private Button downloadBtn;
    @FXML private Label selectedMethodLabel;
    @FXML private VBox step2, step3;

    // ✅ Labels de prix dynamiques (fx:id dans le FXML)
    @FXML private Label lblConsultation;
    @FXML private Label lblFrais;
    @FXML private Label lblTva;
    @FXML private Label lblTotal;

    // ── STATE ──
    private String selectedMethod = "Stripe";
    private double MONTANT_TOTAL  = 0.0;
    private String lastPdfPath    = null;

    // ── SERVICES ──
    private final PaiementService paiementService = new PaiementService();
    private final FactureService  factureService  = new FactureService();
    private final MailService     mailService     = new MailService();

    // ════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        // ✅ Lire le montant réel de la consultation
        MONTANT_TOTAL = montantConsultation > 0 ? montantConsultation : 0.0;
        System.out.println("💰 Montant chargé : " + MONTANT_TOTAL + " DT");

        // ✅ Mettre à jour l'affichage du récapitulatif avec le vrai prix
        mettreAJourRecapitulatif();

        Stripe.apiKey = STRIPE_SECRET_KEY;
        setupCardNumberFormat();
        setupExpiryFormat();
    }

    /**
     * ✅ Affiche le montant RÉEL dans le panneau récapitulatif
     * Sans frais de plateforme ni TVA ajoutés
     */
    private void mettreAJourRecapitulatif() {
        if (lblConsultation != null)
            lblConsultation.setText(String.format("%.2f DT", MONTANT_TOTAL));
        if (lblFrais != null)
            lblFrais.setText("0,00 DT");
        if (lblTva != null)
            lblTva.setText("0,00 DT");
        if (lblTotal != null)
            lblTotal.setText(String.format("%.2f DT", MONTANT_TOTAL));
    }

    private void setupCardNumberFormat() {
        if (cardNumber == null) return;
        cardNumber.textProperty().addListener((obs, old, neu) -> {
            String digits = neu.replaceAll("[^0-9]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append(' ');
                sb.append(digits.charAt(i));
            }
            String result = sb.toString();
            if (!result.equals(neu)) {
                cardNumber.setText(result);
                cardNumber.positionCaret(result.length());
            }
        });
    }

    private void setupExpiryFormat() {
        if (cardExpiry == null) return;
        cardExpiry.textProperty().addListener((obs, old, neu) -> {
            String digits = neu.replaceAll("[^0-9]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            String result = digits.length() > 2
                    ? digits.substring(0, 2) + "/" + digits.substring(2)
                    : digits;
            if (!result.equals(neu)) {
                cardExpiry.setText(result);
                cardExpiry.positionCaret(result.length());
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  METHOD SELECTION
    // ════════════════════════════════════════════════════════
    @FXML private void selectStripe() { setMethod("Stripe", "💳  Stripe sélectionné", stripeSection); }
    @FXML private void selectPayPal()  { setMethod("PayPal", "🅿️  PayPal sélectionné", paypalSection); }
    @FXML private void selectFlouci()  { setMethod("Flouci", "🇹🇳  Flouci sélectionné", flouciSection); }

    private void setMethod(String method, String badgeText, VBox activeSection) {
        selectedMethod = method;
        if (selectedMethodLabel != null) selectedMethodLabel.setText(badgeText);
        showHide(stripeSection, activeSection == stripeSection);
        showHide(paypalSection, activeSection == paypalSection);
        showHide(flouciSection, activeSection == flouciSection);
        updateCardStyle(cardStripe, method.equals("Stripe"));
        updateCardStyle(cardPaypal, method.equals("PayPal"));
        updateCardStyle(cardFlouci, method.equals("Flouci"));
        FadeTransition ft = new FadeTransition(Duration.millis(300), activeSection);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void showHide(VBox node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateCardStyle(VBox card, boolean selected) {
        if (card == null) return;
        if (selected) {
            if (!card.getStyleClass().contains("pf-method-selected"))
                card.getStyleClass().add("pf-method-selected");
        } else {
            card.getStyleClass().remove("pf-method-selected");
        }
    }

    // ════════════════════════════════════════════════════════
    //  PROMO
    // ════════════════════════════════════════════════════════
    @FXML
    private void handlePromo() {
        if (promoCode == null) return;
        String code = promoCode.getText().trim().toUpperCase();
        if (code.equals("NEUROWELL10") || code.equals("WELCOME10") || code.equals("NW2026")) {
            showAlert("✅ Code appliqué!", "Réduction de 10% appliquée.", Alert.AlertType.INFORMATION);
        } else if (!code.isEmpty()) {
            showAlert("❌ Code invalide", "Ce code promo n'est pas valide ou a expiré.", Alert.AlertType.WARNING);
        }
    }

    // ════════════════════════════════════════════════════════
    //  📷 SCAN CARTE
    // ════════════════════════════════════════════════════════
    @FXML
    private void handleScanCard() {
        Stage stagePrincipal = getStage();
        if (stagePrincipal == null) return;
        CardScannerDialog.show(stagePrincipal, result -> {
            if (result.success) {
                if (!result.numero.isEmpty()    && cardNumber != null) { cardNumber.setText(result.numero);       animerFlashVert(cardNumber); }
                if (!result.expiration.isEmpty() && cardExpiry != null) { cardExpiry.setText(result.expiration); animerFlashVert(cardExpiry); }
                if (!result.titulaire.isEmpty()  && cardName   != null) { cardName.setText(result.titulaire);    animerFlashVert(cardName);   }
                afficherToastScanReussi();
            } else {
                showAlert("⚠️ Scan non réussi", result.erreur + "\n\n💡 Conseils :\n  • Éclairez bien la carte\n  • Posez-la bien à plat", Alert.AlertType.WARNING);
            }
        });
    }

    private void animerFlashVert(TextField champ) {
        String s = champ.getStyle();
        champ.setStyle(s + "-fx-border-color:#00FF88;-fx-border-width:2;-fx-effect:dropshadow(gaussian,#00FF88,12,0.5,0,0);");
        new Timeline(new KeyFrame(Duration.millis(1500), e -> champ.setStyle(s))).play();
    }

    private void afficherToastScanReussi() {
        Alert toast = new Alert(Alert.AlertType.INFORMATION);
        toast.setTitle("✓ Scan réussi"); toast.setHeaderText(null);
        toast.setContentText("✓ Carte scannée !\n\nIl vous reste à saisir le CVV.\n\nCe message se ferme automatiquement...");
        toast.show();
        new Timeline(new KeyFrame(Duration.seconds(3), e -> { if (toast.isShowing()) toast.close(); })).play();
    }

    // ════════════════════════════════════════════════════════
    //  PAYMENT HANDLER
    // ════════════════════════════════════════════════════════
    @FXML
    private void handlePay() {
        String error = validateForm();
        if (error != null) { showAlert("⚠️ Validation", error, Alert.AlertType.WARNING); return; }
        payButton.setDisable(true);
        show(progressSection);
        activateStep(step2);
        CompletableFuture.runAsync(() -> {
            try { processPayment(); }
            catch (Exception e) { Platform.runLater(() -> showFailure("Erreur système: " + e.getMessage())); }
        });
    }

    private void processPayment() throws Exception {
        updateProgress("🔐 Connexion sécurisée...", 0.15); sleep(600);
        updateProgress("💳 Validation " + selectedMethod + "...", 0.35); sleep(800);
        updateProgress("🏦 Traitement bancaire...", 0.55);

        boolean success;
        String stripeChargeId = null;

        if (selectedMethod.equals("Stripe")) {
            StripeResult result = chargeWithStripe();
            success = result.success;
            stripeChargeId = result.chargeId;
            if (!success) { final String msg = result.errorMessage; Platform.runLater(() -> showFailure(msg)); return; }
        } else {
            sleep(1200);
            success = Math.random() > 0.05;
            if (!success) { Platform.runLater(() -> showFailure("Transaction refusée.")); return; }
        }

        updateProgress("💾 Enregistrement...", 0.72); sleep(500);
        String reference = stripeChargeId != null
                ? "PAY-" + stripeChargeId.substring(3, 11).toUpperCase()
                : "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Paiement paiement = savePaiement(reference);

        updateProgress("📄 Génération facture PDF...", 0.85); sleep(700);
        Facture facture = createAndSaveFacture(reference);

        String pdfPath = null;
        try {
            pdfPath = factureService.genererFacturePDF(
                    facture, paiement,
                    billingPrenom.getText().trim(),
                    billingNom.getText().trim(),
                    billingEmail.getText().trim()
            );
        } catch (Exception e) { System.err.println("⚠️ PDF: " + e.getMessage()); }

        // ✅ Email dans un thread séparé — ne bloque plus la progression
        updateProgress("📧 Envoi email confirmation...", 0.92);
        final String   emailTo  = billingEmail.getText().trim();
        final String   prenom   = billingPrenom.getText().trim();
        final String   nom      = billingNom.getText().trim();
        final Paiement pFinal   = paiement;
        final Facture  fFinal   = facture;
        final String   pdfFinal = pdfPath;

        Thread emailThread = new Thread(() -> {
            try {
                mailService.sendPaymentConfirmation(emailTo, prenom, nom, pFinal, fFinal, pdfFinal);
                System.out.println("✅ Email envoyé à : " + emailTo);
            } catch (Exception e) {
                System.err.println("⚠️ Email non envoyé : " + e.getMessage());
            }
        });
        emailThread.setDaemon(true);
        emailThread.start();

        sleep(600);
        updateProgress("✅ Terminé !", 1.0); sleep(400);
        final String finalRef = reference;
        final String finalPdf = pdfPath;
        Platform.runLater(() -> showSuccess(finalRef, finalPdf));
    }

    // ════════════════════════════════════════════════════════
    //  STRIPE
    // ════════════════════════════════════════════════════════
    private static class StripeResult {
        boolean success; String chargeId; String errorMessage;
        StripeResult(boolean s, String c, String e) { success=s; chargeId=c; errorMessage=e; }
    }

    private StripeResult chargeWithStripe() {
        try {
            String rawCard = cardNumber.getText().replaceAll(" ", "").trim();
            String token   = resolveStripeTestToken(rawCard);
            long   centimes = Math.round(MONTANT_TOTAL * 100);

            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(centimes).setCurrency("usd").setSource(token)
                    .setDescription("Consultation NeuroWell - " + billingPrenom.getText().trim() + " " + billingNom.getText().trim())
                    .setReceiptEmail(billingEmail.getText().trim())
                    .putMetadata("patient_prenom", billingPrenom.getText().trim())
                    .putMetadata("patient_nom",    billingNom.getText().trim())
                    .putMetadata("methode", "Stripe")
                    .build();

            Charge charge = Charge.create(params);
            boolean ok = Boolean.TRUE.equals(charge.getPaid()) && "succeeded".equals(charge.getStatus());
            return new StripeResult(ok, charge.getId(), ok ? null : "Paiement non abouti : " + charge.getStatus());

        } catch (CardException e) {
            String msg = switch (e.getCode() != null ? e.getCode() : "") {
                case "card_declined"      -> "Carte refusée par votre banque.";
                case "insufficient_funds" -> "Fonds insuffisants.";
                case "expired_card"       -> "Carte expirée.";
                case "incorrect_cvc"      -> "CVV incorrect.";
                case "incorrect_number"   -> "Numéro de carte invalide.";
                default                   -> "Carte refusée : " + e.getUserMessage();
            };
            return new StripeResult(false, null, msg);
        } catch (StripeException e) {
            return new StripeResult(false, null, "Erreur Stripe : " + e.getMessage());
        } catch (Exception e) {
            return new StripeResult(false, null, "Erreur inattendue : " + e.getMessage());
        }
    }

    private String resolveStripeTestToken(String cardNum) {
        return switch (cardNum) {
            case "4242424242424242" -> "tok_visa";
            case "4000056655550002" -> "tok_visa_debit";
            case "5555555555554444" -> "tok_mastercard";
            case "5200828282828210" -> "tok_mastercard_debit";
            case "378282246310005"  -> "tok_amex";
            case "4000000000000002" -> "tok_chargeDeclined";
            case "4000000000009995" -> "tok_chargeDeclinedInsufficientFunds";
            case "4000000000000069" -> "tok_chargeDeclinedExpiredCard";
            case "4000000000000127" -> "tok_chargeDeclinedIncorrectCvc";
            case "4000000000000101" -> "tok_chargeDeclinedProcessingError";
            default                 -> "tok_visa";
        };
    }

    // ════════════════════════════════════════════════════════
    //  SAVE TO DB
    // ════════════════════════════════════════════════════════
    private Paiement savePaiement(String reference) throws SQLException {
        Paiement p = new Paiement(MONTANT_TOTAL, LocalDate.now(), selectedMethod, "REUSSI", reference, 1);
        paiementService.add(p);
        return p;
    }

    private Facture createAndSaveFacture(String reference) {
        String numFact = "NW-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + reference.substring(reference.length() - 8);
        Facture f = new Facture(numFact, LocalDate.now(), MONTANT_TOTAL, "Consultation psychologique · Réf: " + reference);
        try { factureService.ajouterFacture(f); }
        catch (Exception e) { System.err.println("⚠️ Facture DB: " + e.getMessage()); }
        return f;
    }

    // ════════════════════════════════════════════════════════
    //  PROGRESS / RESULT
    // ════════════════════════════════════════════════════════
    private void updateProgress(String text, double val) {
        Platform.runLater(() -> {
            if (progressStep != null) progressStep.setText(text);
            if (progressBar  != null) new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(progressBar.progressProperty(), val))).play();
        });
    }

    private void sleep(long ms) throws InterruptedException { Thread.sleep(ms); }

    private void showSuccess(String reference, String pdfPath) {
        this.lastPdfPath = pdfPath;
        hide(progressSection); activateStep(step3);
        show(resultSection); show(successView); hide(failureView);
        if (successRef    != null) successRef.setText("Référence: " + reference);
        if (successAmount != null) successAmount.setText(String.format("Montant payé: %.2f DT", MONTANT_TOTAL));
        new ParallelTransition(fade(successView, 0, 1, 600), scale(successView, 0.8, 1.0, 600)).play();
    }

    private void showFailure(String message) {
        hide(progressSection); payButton.setDisable(false);
        show(resultSection); hide(successView); show(failureView);
        if (failureMessage != null) failureMessage.setText(message);
        fade(failureView, 0, 1, 400).play();
    }

    @FXML private void handleRetry() {
        hide(resultSection); payButton.setDisable(false);
        if (progressBar != null) progressBar.setProgress(0);
    }

    // ════════════════════════════════════════════════════════
    //  DOWNLOAD / NAVIGATION
    // ════════════════════════════════════════════════════════
    @FXML
    private void handleDownloadPDF() {
        if (lastPdfPath == null) { showAlert("⚠️", "Aucun PDF disponible.", Alert.AlertType.WARNING); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la facture");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName(new File(lastPdfPath).getName());
        Stage stage = getStage(); if (stage == null) return;
        File dest = fc.showSaveDialog(stage);
        if (dest != null) {
            try {
                Files.copy(new File(lastPdfPath).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert("✅ Succès", "Facture enregistrée:\n" + dest.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("❌ Erreur", "Impossible d'enregistrer: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = getStage(); if (stage == null) return;
            Parent root = FXMLLoader.load(getClass().getResource("/views/front.fxml"));
            FadeTransition ft = new FadeTransition(Duration.millis(250), stage.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> { stage.getScene().setRoot(root); fade(root, 0, 1, 250).play(); });
            ft.play();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════════
    private String validateForm() {
        if (isEmpty(billingPrenom)) return "Veuillez entrer votre prénom.";
        if (isEmpty(billingNom))    return "Veuillez entrer votre nom.";
        if (billingEmail == null || !isValidEmail(billingEmail.getText().trim())) return "Veuillez entrer un email valide.";
        switch (selectedMethod) {
            case "Stripe":
                if (cardNumber == null || cardNumber.getText().replaceAll(" ", "").length() < 16) return "Numéro de carte invalide (16 chiffres).";
                if (cardExpiry == null || !cardExpiry.getText().matches("\\d{2}/\\d{2}"))          return "Date d'expiration invalide (MM/AA).";
                if (cardCvv    == null || cardCvv.getText().length() < 3)                          return "CVV invalide (3 chiffres).";
                if (isEmpty(cardName)) return "Veuillez entrer le nom du titulaire.";
                break;
            case "PayPal":
                if (paypalEmail == null || !isValidEmail(paypalEmail.getText().trim())) return "Email PayPal invalide.";
                break;
            case "Flouci":
                if (flouciPhone == null || flouciPhone.getText().trim().length() < 8) return "Numéro de téléphone invalide.";
                break;
        }
        return null;
    }

    // ════════════════════════════════════════════════════════
    //  UTILS
    // ════════════════════════════════════════════════════════
    private void show(Node n)  { if (n != null) { n.setVisible(true);  n.setManaged(true);  } }
    private void hide(Node n)  { if (n != null) { n.setVisible(false); n.setManaged(false); } }
    private boolean isEmpty(TextField f) { return f == null || f.getText().trim().isEmpty(); }
    private boolean isValidEmail(String e) { return e != null && e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"); }
    private void activateStep(VBox s) { if (s != null && !s.getStyleClass().contains("pf-step-active")) s.getStyleClass().add("pf-step-active"); }
    private Stage getStage() { return (payButton != null && payButton.getScene() != null) ? (Stage) payButton.getScene().getWindow() : null; }
    private FadeTransition  fade(Node n, double f, double t, int ms)  { FadeTransition  ft = new FadeTransition(Duration.millis(ms), n);  ft.setFromValue(f); ft.setToValue(t); return ft; }
    private ScaleTransition scale(Node n, double f, double t, int ms) { ScaleTransition st = new ScaleTransition(Duration.millis(ms), n); st.setFromX(f); st.setToX(t); st.setFromY(f); st.setToY(t); return st; }
    private void showAlert(String title, String msg, Alert.AlertType type) { Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
}