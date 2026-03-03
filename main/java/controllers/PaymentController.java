package controllers;

import entities.Evenement;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class PaymentController {

    // ─── Stripe TEST public key ─────────────────────────────
    // Remplace par ta vraie cle sur https://dashboard.stripe.com
    private static final String STRIPE_PUBLIC_KEY = "pk_test_51SIxuHAYYBkRJKzo25aijM1ZxlrkhipJAaZHtWrp05frKhaANALVrNYvz7kzSHYkWpgP1B1XKQNiNwHdTFITlxyb00SEFIN488";

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DARK   = "#050C07";
    private static final String C_DANGER = "#FF4D6D";

    @FXML private StackPane  rootPane;
    @FXML private WebView    paymentWebView;
    @FXML private VBox       loadingOverlay;
    @FXML private ProgressBar loadingBar;
    @FXML private Label      lblEventName;
    @FXML private Label      lblEventDate;
    @FXML private Label      lblPrixBase;
    @FXML private Label      lblFrais;
    @FXML private Label      lblTotal;
    @FXML private Label      lblStatus;
    @FXML private Button     btnBack;
    @FXML private Circle     orb1, orb2, orb3;

    private Evenement evenement;
    private double    prixBase   = 0;
    private double    prixTotal  = 0;
    private String    previousFxml = "/views/showEvent.fxml";

    @FXML
    public void initialize() {
        animateOrbs();
        styleBackBtn();
        if (paymentWebView != null) {
            paymentWebView.getEngine().setJavaScriptEnabled(true);
        }
    }

    // ─── Init depuis ShowEventController ─────────────────────
    public void initPayment(Evenement event, String previousFxml) {
        this.evenement    = event;
        this.previousFxml = previousFxml;

        // Labels recap
        if (lblEventName != null) lblEventName.setText(event.getTitre_e());
        if (lblEventDate != null && event.getDate_e() != null) {
            lblEventDate.setText(event.getDate_e().toLocalDateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        // Prix
        prixBase = extractPrix(event.getPrix_e());
        double frais = Math.round(prixBase * 0.05 * 100.0) / 100.0;
        prixTotal    = Math.round((prixBase + frais) * 100.0) / 100.0;

        if (lblPrixBase != null) lblPrixBase.setText(String.format("%.2f DT", prixBase));
        if (lblFrais    != null) lblFrais.setText(String.format("%.2f DT", frais));
        if (lblTotal    != null) lblTotal.setText(String.format("%.2f DT", prixTotal));

        loadPaymentForm();
    }

    // ─── HTML Stripe Payment Form ─────────────────────────────
    private void loadPaymentForm() {
        if (paymentWebView == null) return;

        // Convertir en centimes pour Stripe
        long amountCents = (long)(prixTotal * 100);
        String eventName = evenement != null ? evenement.getTitre_e().replace("'", "\\'") : "Evenement";

        String html =
                "<!DOCTYPE html><html><head>" +
                        "<meta charset='utf-8'/>" +
                        "<script src='https://js.stripe.com/v3/'></script>" +
                        "<style>" +
                        "  @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&display=swap');" +
                        "  *{ margin:0; padding:0; box-sizing:border-box; font-family:'JetBrains Mono',monospace; }" +
                        "  body{ background:#050C07; display:flex; align-items:center; justify-content:center;" +
                        "        min-height:100vh; padding:30px; }" +

                        "  .card{" +
                        "    background: rgba(13,31,18,0.92);" +
                        "    border: 1.5px solid rgba(0,217,255,0.25);" +
                        "    border-radius: 24px;" +
                        "    padding: 40px;" +
                        "    width: 100%;" +
                        "    max-width: 480px;" +
                        "    box-shadow: 0 0 60px rgba(0,217,255,0.08), 0 20px 60px rgba(0,0,0,0.6);" +
                        "  }" +

                        "  h2{ color:#00D9FF; font-size:20px; font-weight:600; margin-bottom:6px; }" +
                        "  .subtitle{ color:rgba(200,255,220,0.45); font-size:11px; margin-bottom:30px; }" +

                        "  .field-label{" +
                        "    color:rgba(200,255,220,0.60); font-size:10px; font-weight:600;" +
                        "    letter-spacing:1px; margin-bottom:8px; margin-top:18px;" +
                        "  }" +

                        "  .stripe-element{" +
                        "    background: #112016;" +
                        "    border: 1.5px solid rgba(0,217,255,0.20);" +
                        "    border-radius: 12px;" +
                        "    padding: 14px 16px;" +
                        "    transition: border-color 0.2s;" +
                        "  }" +
                        "  .stripe-element.StripeElement--focus{" +
                        "    border-color: #00D9FF;" +
                        "    box-shadow: 0 0 0 3px rgba(0,217,255,0.12);" +
                        "  }" +
                        "  .stripe-element.StripeElement--invalid{ border-color: #FF4D6D; }" +

                        "  input[type=text]{" +
                        "    width:100%; background:#112016;" +
                        "    border:1.5px solid rgba(0,217,255,0.20);" +
                        "    border-radius:12px; padding:13px 16px;" +
                        "    color:#E8FFF0; font-size:13px; font-family:inherit;" +
                        "    outline:none; transition:border-color 0.2s;" +
                        "  }" +
                        "  input[type=text]:focus{ border-color:#00D9FF; box-shadow:0 0 0 3px rgba(0,217,255,0.12); }" +
                        "  input[type=text]::placeholder{ color:rgba(200,255,220,0.25); }" +

                        "  .row2{ display:grid; grid-template-columns:1fr 1fr; gap:12px; }" +

                        "  #pay-btn{" +
                        "    width:100%; margin-top:28px;" +
                        "    background: linear-gradient(to right, #00D9FF, #00FF88);" +
                        "    border:none; border-radius:28px; padding:16px;" +
                        "    color:#050C07; font-size:15px; font-weight:700;" +
                        "    cursor:pointer; letter-spacing:0.5px;" +
                        "    transition: transform 0.15s, box-shadow 0.15s;" +
                        "    font-family:inherit;" +
                        "  }" +
                        "  #pay-btn:hover{ transform:translateY(-2px);" +
                        "    box-shadow:0 8px 30px rgba(0,217,255,0.35); }" +
                        "  #pay-btn:active{ transform:translateY(0); }" +
                        "  #pay-btn:disabled{ opacity:0.5; cursor:not-allowed; transform:none; }" +

                        "  #error-msg{" +
                        "    color:#FF4D6D; font-size:12px; margin-top:14px;" +
                        "    padding:10px 14px; background:rgba(255,77,109,0.10);" +
                        "    border:1px solid rgba(255,77,109,0.30);" +
                        "    border-radius:10px; display:none;" +
                        "  }" +

                        "  #success-msg{" +
                        "    color:#00FF88; font-size:13px; margin-top:14px;" +
                        "    padding:14px; background:rgba(0,255,136,0.10);" +
                        "    border:1px solid rgba(0,255,136,0.30);" +
                        "    border-radius:10px; text-align:center; display:none;" +
                        "  }" +

                        "  .amount-badge{" +
                        "    background:rgba(0,217,255,0.08);" +
                        "    border:1px solid rgba(0,217,255,0.20);" +
                        "    border-radius:12px; padding:12px 16px;" +
                        "    display:flex; justify-content:space-between; align-items:center;" +
                        "    margin-bottom:24px;" +
                        "  }" +
                        "  .amount-label{ color:rgba(200,255,220,0.55); font-size:11px; }" +
                        "  .amount-value{ color:#00FF88; font-size:20px; font-weight:700; }" +

                        "  .card-logos{ display:flex; gap:8px; margin-bottom:18px; }" +
                        "  .card-logo{" +
                        "    background:rgba(255,255,255,0.06);" +
                        "    border:1px solid rgba(255,255,255,0.10);" +
                        "    border-radius:6px; padding:4px 10px;" +
                        "    color:rgba(200,255,220,0.40); font-size:10px; font-weight:600;" +
                        "  }" +

                        "  .spinner{" +
                        "    display:inline-block; width:16px; height:16px;" +
                        "    border:2px solid rgba(5,12,7,0.4);" +
                        "    border-top-color:#050C07;" +
                        "    border-radius:50%; animation:spin 0.7s linear infinite;" +
                        "    vertical-align:middle; margin-right:8px;" +
                        "  }" +
                        "  @keyframes spin{ to{ transform:rotate(360deg); } }" +
                        "</style></head><body>" +

                        "<div class='card'>" +
                        "  <h2>&#128179; Informations de paiement</h2>" +
                        "  <p class='subtitle'>Votre paiement est securise et chiffre</p>" +

                        "  <div class='amount-badge'>" +
                        "    <span class='amount-label'>Montant total</span>" +
                        "    <span class='amount-value'>" + String.format("%.2f DT", prixTotal) + "</span>" +
                        "  </div>" +

                        "  <div class='card-logos'>" +
                        "    <span class='card-logo'>VISA</span>" +
                        "    <span class='card-logo'>MASTERCARD</span>" +
                        "    <span class='card-logo'>AMEX</span>" +
                        "  </div>" +

                        "  <form id='payment-form'>" +

                        // Nom sur la carte
                        "    <p class='field-label'>NOM SUR LA CARTE</p>" +
                        "    <input type='text' id='card-name' placeholder='Jean Dupont' autocomplete='off'/>" +

                        // Email
                        "    <p class='field-label'>EMAIL</p>" +
                        "    <input type='text' id='card-email' placeholder='email@exemple.com' autocomplete='off'/>" +

                        // Numero de carte Stripe
                        "    <p class='field-label'>NUMERO DE CARTE</p>" +
                        "    <div id='card-number' class='stripe-element'></div>" +

                        // Expiry + CVC
                        "    <div class='row2'>" +
                        "      <div>" +
                        "        <p class='field-label'>EXPIRATION</p>" +
                        "        <div id='card-expiry' class='stripe-element'></div>" +
                        "      </div>" +
                        "      <div>" +
                        "        <p class='field-label'>CVC</p>" +
                        "        <div id='card-cvc' class='stripe-element'></div>" +
                        "      </div>" +
                        "    </div>" +

                        "    <button id='pay-btn' type='submit'>Payer " + String.format("%.2f DT", prixTotal) + "</button>" +
                        "    <div id='error-msg'></div>" +
                        "    <div id='success-msg'>&#9989; Paiement confirme ! Merci pour votre participation a <strong>" + eventName + "</strong>.</div>" +

                        "  </form>" +
                        "</div>" +

                        "<script>" +
                        // Initialisation Stripe
                        "var stripeKey = '" + STRIPE_PUBLIC_KEY + "';" +
                        "var stripe, elements, cardNumber, cardExpiry, cardCvc;" +
                        "var paid = false;" +

                        // Style elements Stripe
                        "var elStyle = {" +
                        "  base: {" +
                        "    color: '#E8FFF0'," +
                        "    fontFamily: 'JetBrains Mono, monospace'," +
                        "    fontSize: '14px'," +
                        "    '::placeholder': { color: 'rgba(200,255,220,0.25)' }," +
                        "    iconColor: '#00D9FF'" +
                        "  }," +
                        "  invalid: { color: '#FF4D6D', iconColor: '#FF4D6D' }" +
                        "};" +

                        // Attendre que Stripe JS charge
                        "function initStripe() {" +
                        "  if (typeof Stripe === 'undefined') {" +
                        "    setTimeout(initStripe, 200); return;" +
                        "  }" +
                        "  stripe   = Stripe(stripeKey);" +
                        "  elements = stripe.elements();" +
                        "  cardNumber = elements.create('cardNumber', {style: elStyle, showIcon: true});" +
                        "  cardExpiry = elements.create('cardExpiry', {style: elStyle});" +
                        "  cardCvc    = elements.create('cardCvc',    {style: elStyle});" +
                        "  cardNumber.mount('#card-number');" +
                        "  cardExpiry.mount('#card-expiry');" +
                        "  cardCvc.mount('#card-cvc');" +

                        // Signal pret pour JavaFX
                        "  document.title = 'PAYMENT_READY';" +
                        "}" +
                        "initStripe();" +

                        // Soumission du formulaire
                        "document.getElementById('payment-form').addEventListener('submit', async function(e) {" +
                        "  e.preventDefault();" +
                        "  if (paid) return;" +

                        "  var name  = document.getElementById('card-name').value.trim();" +
                        "  var email = document.getElementById('card-email').value.trim();" +
                        "  var btn   = document.getElementById('pay-btn');" +
                        "  var errDiv = document.getElementById('error-msg');" +

                        "  if (!name) { showError('Veuillez saisir le nom sur la carte.'); return; }" +
                        "  if (!email || !email.includes('@')) { showError('Email invalide.'); return; }" +

                        "  btn.disabled = true;" +
                        "  btn.innerHTML = '<span class=\"spinner\"></span> Traitement en cours...';" +
                        "  errDiv.style.display = 'none';" +

                        // Simuler un PaymentMethod (en mode TEST Stripe)
                        "  try {" +
                        "    var result = await stripe.createPaymentMethod({" +
                        "      type: 'card'," +
                        "      card: cardNumber," +
                        "      billing_details: { name: name, email: email }" +
                        "    });" +

                        "    if (result.error) {" +
                        "      showError(result.error.message);" +
                        "      btn.disabled = false;" +
                        "      btn.innerHTML = 'Payer " + String.format("%.2f DT", prixTotal) + "';" +
                        "    } else {" +
                        // Paiement reussi (en production, envoyer result.paymentMethod.id au backend)
                        "      paid = true;" +
                        "      document.getElementById('success-msg').style.display = 'block';" +
                        "      btn.style.display = 'none';" +
                        "      document.title = 'PAYMENT_SUCCESS|' + result.paymentMethod.id;" +
                        "    }" +
                        "  } catch(err) {" +
                        "    showError('Erreur reseau : ' + err.message);" +
                        "    btn.disabled = false;" +
                        "    btn.innerHTML = 'Payer " + String.format("%.2f DT", prixTotal) + "';" +
                        "  }" +
                        "});" +

                        "function showError(msg) {" +
                        "  var d = document.getElementById('error-msg');" +
                        "  d.textContent = msg;" +
                        "  d.style.display = 'block';" +
                        "}" +
                        "</script></body></html>";

        WebEngine engine = paymentWebView.getEngine();

        // Ecouter le chargement
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                PauseTransition pause = new PauseTransition(Duration.millis(1000));
                pause.setOnFinished(e -> {
                    hideLoading();
                    if (lblStatus != null) setStatus("Pret a payer", C_GREEN);
                });
                pause.play();
            } else if (state == Worker.State.FAILED) {
                hideLoading();
                setStatus("Erreur chargement", C_DANGER);
            }
        });

        // Ecouter le titre pour detecter PAYMENT_SUCCESS
        engine.titleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null && newT.startsWith("PAYMENT_SUCCESS|")) {
                String pmId = newT.replace("PAYMENT_SUCCESS|", "");
                Platform.runLater(() -> handlePaymentSuccess(pmId));
            } else if ("PAYMENT_READY".equals(newT)) {
                Platform.runLater(() -> setStatus("Formulaire pret", C_CYAN));
            }
        });

        engine.loadContent(html, "text/html");

        // Animer la barre de chargement
        if (loadingBar != null) {
            new Timeline(
                    new javafx.animation.KeyFrame(Duration.ZERO,
                            new javafx.animation.KeyValue(loadingBar.progressProperty(), 0.0)),
                    new javafx.animation.KeyFrame(Duration.seconds(2.5),
                            new javafx.animation.KeyValue(loadingBar.progressProperty(), 0.85))
            ).play();
        }
    }

    // ─── Succes paiement ──────────────────────────────────────
    private void handlePaymentSuccess(String paymentMethodId) {
        setStatus("Paiement confirme !", C_GREEN);

        // Ici tu peux appeler ton backend pour confirmer le PaymentIntent
        // Ex: new PaiementService().confirmer(evenement.getId_e(), paymentMethodId, prixTotal);

        // Afficher un alert de confirmation apres 2s
        // Platform.runLater obligatoire — showAndWait interdit pendant une animation
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> Platform.runLater(this::showSuccessAndReturn));
        pause.play();
    }

    private void showSuccessAndReturn() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paiement reussi !");
        alert.setHeaderText(null);
        alert.setContentText(
                "Votre paiement de " + String.format("%.2f DT", prixTotal) + " a ete confirme.\n" +
                        "Un email de confirmation a ete envoye.\n\n" +
                        "Merci pour votre participation a :\n" + (evenement != null ? evenement.getTitre_e() : "")
        );
        styleAlert(alert);
        alert.showAndWait();
        navigateBack();
    }

    @FXML
    private void handleBack() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler le paiement ?");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment annuler ce paiement ?\nVos informations ne seront pas enregistrees.");
        styleAlert(confirm);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) navigateBack();
        });
    }

    private void navigateBack() {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource(previousFxml));
            Stage  stage = (Stage) rootPane.getScene().getWindow();
            FadeTransition fo = new FadeTransition(Duration.millis(280), rootPane.getScene().getRoot());
            fo.setToValue(0);
            fo.setOnFinished(e -> {
                stage.getScene().setRoot(root);
                FadeTransition fi = new FadeTransition(Duration.millis(280), root);
                fi.setFromValue(0); fi.setToValue(1); fi.play();
            });
            fo.play();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    // ─── Helpers ──────────────────────────────────────────────
    private void hideLoading() {
        if (loadingOverlay == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(400), loadingOverlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> loadingOverlay.setVisible(false));
        ft.play();
    }

    private void setStatus(String msg, String color) {
        if (lblStatus != null) {
            lblStatus.setText(msg);
            lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 600;");
        }
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle(
                "-fx-background-color: #0D1F12; " +
                        "-fx-border-color: rgba(0,217,255,0.30); -fx-border-width: 1.5; " +
                        "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        try {
            alert.getDialogPane().lookup(".content.label")
                    .setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;");
        } catch (Exception ignored) {}
    }

    private void styleBackBtn() {
        if (btnBack == null) return;
        String base  = "-fx-background-color: transparent; -fx-border-color: rgba(255,77,109,0.40); " +
                "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14; " +
                "-fx-text-fill: rgba(255,77,109,0.70); -fx-font-size: 12px; -fx-padding: 11 0; -fx-cursor: hand;";
        String hover = "-fx-background-color: rgba(255,77,109,0.10); -fx-border-color: #FF4D6D; " +
                "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14; " +
                "-fx-text-fill: #FF4D6D; -fx-font-size: 12px; -fx-padding: 11 0; -fx-cursor: hand;";
        btnBack.setStyle(base);
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(hover));
        btnBack.setOnMouseExited(e  -> btnBack.setStyle(base));
    }

    private double extractPrix(String prixStr) {
        if (prixStr == null) return 0;
        try { return Double.parseDouble(prixStr.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return 0; }
    }

    private void animateOrbs() {
        animateOrb(orb1,  40, -25, 14);
        animateOrb(orb2, -50,  35, 18);
        animateOrb(orb3,  25, -40, 20);
    }

    private void animateOrb(Circle orb, double dx, double dy, double secs) {
        if (orb == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.seconds(secs), orb);
        tt.setByX(dx); tt.setByY(dy);
        tt.setCycleCount(Animation.INDEFINITE); tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
    }
}