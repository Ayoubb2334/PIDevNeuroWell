package controllers;

// ─── Imports du service IA ─────────────────────────────────────
import services.CardScannerService;

// ─── Imports JavaFX Animations ────────────────────────────────
import javafx.animation.*;
import javafx.util.Duration;

// ─── Imports JavaFX Thread ────────────────────────────────────
import javafx.application.Platform;    // Pour modifier l'UI depuis un autre thread

// ─── Imports JavaFX Layout ────────────────────────────────────
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;           // ImageView, WritableImage, PixelWriter
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

// ─── Imports Webcam ───────────────────────────────────────────
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

// ─── Imports Java standards ───────────────────────────────────
import java.awt.image.BufferedImage;          // Image Java AWT (webcam)
import java.util.concurrent.CompletableFuture;// Exécution en arrière-plan
import java.util.concurrent.atomic.AtomicBoolean; // Variable thread-safe
import java.util.function.Consumer;           // Callback lambda

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║         CardScannerDialog — ÉTAPE 3                         ║
 * ║   Fenêtre JavaFX avec webcam en temps réel + scan IA        ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * COMMENT UTILISER CETTE CLASSE :
 * ─────────────────────────────────
 *  Depuis PaiementFrontController, appeler :
 *
 *    CardScannerDialog.show(stage, result -> {
 *        if (result.success) {
 *            cardNumber.setText(result.numero);
 *            cardExpiry.setText(result.expiration);
 *            cardName.setText(result.titulaire);
 *        }
 *    });
 *
 * STRUCTURE DE LA FENÊTRE :
 * ─────────────────────────────
 *  ┌─────────────────────────────────────────┐
 *  │  📷 SCAN CARTE BANCAIRE        [IA] [✕] │  ← Barre de titre
 *  ├─────────────────────────────────────────┤
 *  │                                         │
 *  │     [  flux vidéo webcam en direct  ]   │  ← Caméra (300px)
 *  │     [  avec cadre animé style scan  ]   │
 *  │                                         │
 *  ├─────────────────────────────────────────┤
 *  │  💡 Lumière  📐 À plat  🔍 Net  ⚡ Vite │  ← Conseils
 *  ├─────────────────────────────────────────┤
 *  │  ● Caméra prête                         │  ← Status
 *  │  [  📷 CAPTURER ET ANALYSER AVEC L'IA ] │  ← Bouton
 *  ├─────────────────────────────────────────┤
 *  │  ✓ Carte détectée !  (après scan)       │  ← Résultat (caché au départ)
 *  │  💳 Numéro : 4242 4242 4242 4242        │
 *  │  📅 Expiration : 12/26                  │
 *  │  👤 Titulaire : KHALIL FELHI            │
 *  │  [✓ Utiliser]    [↩ Réessayer]          │
 *  └─────────────────────────────────────────┘
 */
public class CardScannerDialog {

    // ══════════════════════════════════════════════════════════════
    //  🎨 COULEURS — Thème NeuroWell (identique au reste de l'appli)
    // ══════════════════════════════════════════════════════════════
    private static final String CYAN    = "#00D9FF";  // Bleu cyan principal
    private static final String GREEN   = "#00FF88";  // Vert succès
    private static final String RED     = "#FF4B6E";  // Rouge erreur
    private static final String DARK    = "#050C07";  // Fond très sombre
    private static final String CARD    = "#0A1A0D";  // Fond carte
    private static final String SURFACE = "#0D2214";  // Fond surface
    private static final String TEXT    = "#E8FFF0";  // Texte principal
    private static final String MUTED   = "rgba(200,255,220,0.55)"; // Texte atténué
    private static final String DIM     = "rgba(0,217,255,0.38)";   // Texte très atténué

    // ══════════════════════════════════════════════════════════════
    //  🤖 SERVICE IA
    // ══════════════════════════════════════════════════════════════
    // Instance du service qui appelle Claude Vision API
    private final CardScannerService serviceIA = new CardScannerService();

    // ══════════════════════════════════════════════════════════════
    //  📷 ÉTAT DE LA WEBCAM
    // ══════════════════════════════════════════════════════════════

    /** L'objet webcam (null si pas de caméra détectée) */
    private Webcam webcam;

    /**
     * AtomicBoolean = variable booléenne "thread-safe".
     * Plusieurs threads (UI + webcam + IA) lisent/écrivent cette valeur.
     * AtomicBoolean évite les bugs de concurrence.
     *
     * streaming = true → le thread de stream est actif
     * streaming = false → le thread de stream doit s'arrêter
     */
    private final AtomicBoolean streaming = new AtomicBoolean(false);

    /**
     * scanning = true → l'IA est en train d'analyser (on pause le stream)
     * scanning = false → le stream continue normalement
     */
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    /** Thread qui récupère les frames de la webcam en continu */
    private Thread threadStream;

    // ══════════════════════════════════════════════════════════════
    //  🖼️ RÉFÉRENCES AUX ÉLÉMENTS DE L'UI
    // ══════════════════════════════════════════════════════════════
    private Stage     fenetreDialog;   // La fenêtre modale
    private ImageView vueCamera;       // Affichage du flux webcam
    private Label     lblStatus;       // "Caméra prête" / "Analyse en cours" / etc.
    private Button    btnCapturer;     // Bouton "📷 CAPTURER ET ANALYSER"
    private VBox      sectionResultat; // Zone résultat (cachée jusqu'au scan)
    private Label     lblNumero;       // Affiche le numéro détecté
    private Label     lblExpiration;   // Affiche l'expiration détectée
    private Label     lblTitulaire;    // Affiche le titulaire détecté
    private VBox      overlayAnalyse;  // Overlay sombre "Analyse en cours..."

    // ══════════════════════════════════════════════════════════════
    //  📦 DONNÉES
    // ══════════════════════════════════════════════════════════════
    /** Dernier résultat de scan (rempli après analyse IA) */
    private CardScannerService.ScanResult dernierResultat;

    /**
     * Callback = fonction qui sera appelée quand l'utilisateur
     * clique "✓ Utiliser ces informations".
     * C'est la lambda passée dans show().
     */
    private Consumer<CardScannerService.ScanResult> callback;

    // ══════════════════════════════════════════════════════════════
    //  🚪 POINT D'ENTRÉE STATIQUE — La seule méthode publique
    // ══════════════════════════════════════════════════════════════

    /**
     * Ouvre la fenêtre de scan de carte bancaire.
     *
     * @param fenetrePrincipale  La Stage parente (pour centrer la dialog)
     * @param resultatCallback   Appelé avec le ScanResult quand l'utilisateur confirme
     *
     * EXEMPLE :
     *   Stage stage = (Stage) cardNumber.getScene().getWindow();
     *   CardScannerDialog.show(stage, result -> {
     *       if (result.success) {
     *           cardNumber.setText(result.numero);
     *       }
     *   });
     */
    public static void show(Stage fenetrePrincipale,
                            Consumer<CardScannerService.ScanResult> resultatCallback) {
        // Crée une instance et lance tout automatiquement
        new CardScannerDialog(fenetrePrincipale, resultatCallback);
    }

    // ══════════════════════════════════════════════════════════════
    //  🏗️ CONSTRUCTEUR PRIVÉ
    // ══════════════════════════════════════════════════════════════

    private CardScannerDialog(Stage fenetrePrincipale,
                              Consumer<CardScannerService.ScanResult> resultatCallback) {
        this.callback = resultatCallback;
        construireFenetre(fenetrePrincipale);  // 1. Construire l'UI
        demarrerWebcam();                       // 2. Démarrer la caméra en arrière-plan
    }

    // ══════════════════════════════════════════════════════════════
    //  🏗️ CONSTRUCTION DE LA FENÊTRE
    // ══════════════════════════════════════════════════════════════

    private void construireFenetre(Stage fenetrePrincipale) {
        // Créer une nouvelle fenêtre
        fenetreDialog = new Stage();

        // initOwner : lie cette fenêtre à la fenêtre principale
        // (se centre dessus, se ferme avec elle)
        fenetreDialog.initOwner(fenetrePrincipale);

        // APPLICATION_MODAL : bloque les interactions avec la fenêtre principale
        // tant que cette dialog est ouverte
        fenetreDialog.initModality(Modality.APPLICATION_MODAL);

        // UNDECORATED : pas de barre de titre Windows (on fait notre propre)
        fenetreDialog.initStyle(StageStyle.UNDECORATED);

        fenetreDialog.setResizable(false);
        fenetreDialog.setTitle("Scanner carte bancaire — NeuroWell");

        // Conteneur racine de toute la fenêtre
        VBox racine = new VBox(0);
        racine.setStyle(
                "-fx-background-color:" + DARK + ";" +
                        "-fx-border-color:rgba(0,217,255,0.30);" +  // Bordure cyan subtile
                        "-fx-border-width:1;" +
                        "-fx-border-radius:18;" +
                        "-fx-background-radius:18;"
        );

        // Assembler les 5 sections de haut en bas
        racine.getChildren().addAll(
                creerBarreDeTitre(),     // Section 1 : En-tête
                creerZoneCamera(),       // Section 2 : Flux webcam
                creerConseilsUtilisation(), // Section 3 : Aide visuelle
                creerBarreControles(),   // Section 4 : Status + Bouton capturer
                creerSectionResultat()   // Section 5 : Résultat (caché au départ)
        );

        // Créer la scène avec fond transparent (pour les coins arrondis)
        Scene scene = new Scene(racine, 560, 640);
        scene.setFill(Color.TRANSPARENT);

        fenetreDialog.setScene(scene);
        fenetreDialog.show();

        // Nettoyer la caméra quand on ferme la fenêtre
        fenetreDialog.setOnCloseRequest(event -> nettoyerRessources());
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 1 : BARRE DE TITRE PERSONNALISÉE
    // ══════════════════════════════════════════════════════════════

    private HBox creerBarreDeTitre() {
        HBox barre = new HBox(12);
        barre.setAlignment(Pos.CENTER_LEFT);
        barre.setPadding(new Insets(16, 20, 16, 20));
        barre.setStyle(
                // Dégradé léger de gauche à droite
                "-fx-background-color:linear-gradient(to right, #071A10, " + DARK + ");" +
                        // Séparateur fin en bas
                        "-fx-border-color:transparent transparent rgba(0,217,255,0.12) transparent;" +
                        "-fx-border-width:0 0 1 0;" +
                        // Coins arrondis uniquement en haut
                        "-fx-background-radius:18 18 0 0;"
        );

        // Icône caméra
        Label icone = new Label("📷");
        icone.setStyle("-fx-font-size:22px;");

        // Bloc titre + sous-titre
        VBox textesTitre = new VBox(3);
        Label titre = new Label("SCAN CARTE BANCAIRE");
        titre.setStyle(
                "-fx-text-fill:" + CYAN + ";" +
                        "-fx-font-size:16px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-letter-spacing:2px;"
        );
        Label sousTitre = new Label("IA Vision · Claude Anthropic");
        sousTitre.setStyle("-fx-text-fill:" + DIM + ";-fx-font-size:10px;");
        textesTitre.getChildren().addAll(titre, sousTitre);

        // Espace flexible qui pousse les boutons à droite
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        // Badge "IA"
        Label badgeIA = new Label("✦ IA");
        badgeIA.setStyle(
                "-fx-background-color:rgba(0,217,255,0.10);" +
                        "-fx-text-fill:" + CYAN + ";" +
                        "-fx-border-color:rgba(0,217,255,0.25);" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:20;" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:4 12;" +
                        "-fx-font-size:11px;" +
                        "-fx-font-weight:bold;"
        );

        // Bouton fermer [✕]
        Button btnFermer = new Button("✕");
        btnFermer.setStyle(
                "-fx-background-color:rgba(255,75,110,0.12);" +
                        "-fx-text-fill:" + RED + ";" +
                        "-fx-border-color:rgba(255,75,110,0.25);" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;" +
                        "-fx-padding:5 11;" +
                        "-fx-cursor:hand;" +
                        "-fx-font-weight:bold;"
        );
        // Effet hover : rouge plein au survol
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(
                "-fx-background-color:" + RED + ";-fx-text-fill:" + DARK + ";" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:5 11;-fx-cursor:hand;-fx-font-weight:bold;"
        ));
        btnFermer.setOnMouseExited(e -> btnFermer.setStyle(
                "-fx-background-color:rgba(255,75,110,0.12);-fx-text-fill:" + RED + ";" +
                        "-fx-border-color:rgba(255,75,110,0.25);-fx-border-width:1;" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:5 11;-fx-cursor:hand;-fx-font-weight:bold;"
        ));
        btnFermer.setOnAction(e -> {
            nettoyerRessources();  // Fermer la webcam proprement
            fenetreDialog.close();
        });

        barre.getChildren().addAll(icone, textesTitre, espace, badgeIA, btnFermer);
        return barre;
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 2 : ZONE CAMÉRA AVEC CADRE ANIMÉ
    // ══════════════════════════════════════════════════════════════

    private StackPane creerZoneCamera() {
        // StackPane = empile ses enfants les uns sur les autres (comme des calques Photoshop)
        StackPane conteneur = new StackPane();
        conteneur.setPrefHeight(300);
        conteneur.setStyle("-fx-background-color:#000;"); // Fond noir caméra

        // ── CALQUE 1 : Vue caméra ────────────────────────
        vueCamera = new ImageView();
        vueCamera.setFitWidth(560);
        vueCamera.setFitHeight(300);
        vueCamera.setPreserveRatio(true); // Garde les proportions
        vueCamera.setSmooth(true);        // Anti-aliasing

        // ── CALQUE 2 : Cadre de guidage animé ────────────
        // Représente le format d'une carte bancaire (ratio 85.6 × 54 mm)
        StackPane cadreGuidage = new StackPane();
        cadreGuidage.setPrefSize(380, 240);

        // Rectangle représentant le cadre cible
        javafx.scene.shape.Rectangle rectangleCadre = new javafx.scene.shape.Rectangle(380, 240);
        rectangleCadre.setFill(Color.TRANSPARENT);       // Transparent au milieu
        rectangleCadre.setStroke(Color.web(CYAN, 0.75)); // Bordure cyan
        rectangleCadre.setStrokeWidth(2);
        rectangleCadre.setArcWidth(14);  // Coins arrondis
        rectangleCadre.setArcHeight(14);

        // Animation de pulsation : le cadre clignote doucement
        // pour indiquer "zone active de scan"
        Timeline animPulsation = new Timeline(
                new KeyFrame(Duration.ZERO,          new KeyValue(rectangleCadre.opacityProperty(), 0.3)),
                new KeyFrame(Duration.seconds(1.2),  new KeyValue(rectangleCadre.opacityProperty(), 1.0))
        );
        animPulsation.setAutoReverse(true);         // Aller-retour
        animPulsation.setCycleCount(Animation.INDEFINITE); // Infini
        animPulsation.play();

        // 4 coins verts style "scanner de document"
        StackPane coins = creerCoinsScanner(380, 240);

        cadreGuidage.getChildren().addAll(rectangleCadre, coins);

        // ── CALQUE 3 : Overlay "Analyse en cours" ────────
        // Visible seulement pendant l'envoi à l'API Claude
        overlayAnalyse = new VBox(12);
        overlayAnalyse.setAlignment(Pos.CENTER);
        overlayAnalyse.setStyle("-fx-background-color:rgba(0,0,0,0.80);"); // Fond semi-transparent
        overlayAnalyse.setVisible(false); // Caché au départ

        Label iconeAnalyse = new Label("🔍");
        iconeAnalyse.setStyle("-fx-font-size:40px;");

        // Animation de rotation de la loupe
        RotateTransition rotation = new RotateTransition(Duration.seconds(1.5), iconeAnalyse);
        rotation.setByAngle(360);
        rotation.setCycleCount(Animation.INDEFINITE);
        rotation.setInterpolator(Interpolator.LINEAR);
        overlayAnalyse.visibleProperty().addListener((obs, ancien, visible) -> {
            if (visible) rotation.play();
            else         rotation.stop();
        });

        Label texteAnalyse = new Label("Claude Vision analyse votre carte...");
        texteAnalyse.setStyle("-fx-text-fill:" + CYAN + ";-fx-font-size:14px;-fx-font-weight:bold;");

        Label sousTitreAnalyse = new Label("Extraction des informations en cours");
        sousTitreAnalyse.setStyle("-fx-text-fill:" + DIM + ";-fx-font-size:11px;");

        ProgressBar barreProgression = new ProgressBar(-1); // -1 = indéfinie (infinie)
        barreProgression.setPrefWidth(220);
        barreProgression.setStyle("-fx-accent:" + CYAN + ";");

        overlayAnalyse.getChildren().addAll(iconeAnalyse, texteAnalyse, sousTitreAnalyse, barreProgression);

        // ── CALQUE 4 : Placeholder "Initialisation..." ────
        // Visible seulement avant que la caméra ne soit prête
        VBox placeholder = new VBox(14);
        placeholder.setId("placeholder"); // ID pour le retrouver plus tard
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-background-color:#0A0A0A;");

        Label iconeAttente = new Label("📷");
        iconeAttente.setStyle("-fx-font-size:52px;");

        // L'icône pulse en attendant
        ScaleTransition pulsIcone = new ScaleTransition(Duration.seconds(1.2), iconeAttente);
        pulsIcone.setFromX(0.85); pulsIcone.setFromY(0.85);
        pulsIcone.setToX(1.15);   pulsIcone.setToY(1.15);
        pulsIcone.setAutoReverse(true);
        pulsIcone.setCycleCount(Animation.INDEFINITE);
        pulsIcone.play();

        Label texteAttente = new Label("Initialisation de la caméra...");
        texteAttente.setStyle("-fx-text-fill:" + DIM + ";-fx-font-size:13px;");

        ProgressIndicator spinner = new ProgressIndicator(-1);
        spinner.setPrefSize(32, 32);
        spinner.setStyle("-fx-progress-color:" + CYAN + ";");

        placeholder.getChildren().addAll(iconeAttente, texteAttente, spinner);

        // Empiler tous les calques
        // Ordre : caméra (fond) → cadre → overlay → placeholder (dessus)
        conteneur.getChildren().addAll(vueCamera, cadreGuidage, overlayAnalyse, placeholder);
        return conteneur;
    }

    /**
     * Crée les 4 coins verts style "scanner de document".
     * Chaque coin est un petit carré avec seulement 2 côtés visibles.
     *
     *  ┌─              ─┐
     *  │                │
     *  └─              ─┘
     */
    private StackPane creerCoinsScanner(double largeur, double hauteur) {
        StackPane sp = new StackPane();
        sp.setPrefSize(largeur, hauteur);

        int tailleCoin = 22;
        String couleur = GREEN;

        // Coin haut-gauche : bordure en haut et à gauche
        javafx.scene.layout.Pane coinHG = new javafx.scene.layout.Pane();
        coinHG.setPrefSize(tailleCoin, tailleCoin);
        coinHG.setStyle("-fx-border-color:" + couleur + ";-fx-border-width:2.5 0 0 2.5;");
        StackPane.setAlignment(coinHG, Pos.TOP_LEFT);

        // Coin haut-droite : bordure en haut et à droite
        javafx.scene.layout.Pane coinHD = new javafx.scene.layout.Pane();
        coinHD.setPrefSize(tailleCoin, tailleCoin);
        coinHD.setStyle("-fx-border-color:" + couleur + ";-fx-border-width:2.5 2.5 0 0;");
        StackPane.setAlignment(coinHD, Pos.TOP_RIGHT);

        // Coin bas-gauche : bordure en bas et à gauche
        javafx.scene.layout.Pane coinBG = new javafx.scene.layout.Pane();
        coinBG.setPrefSize(tailleCoin, tailleCoin);
        coinBG.setStyle("-fx-border-color:" + couleur + ";-fx-border-width:0 0 2.5 2.5;");
        StackPane.setAlignment(coinBG, Pos.BOTTOM_LEFT);

        // Coin bas-droite : bordure en bas et à droite
        javafx.scene.layout.Pane coinBD = new javafx.scene.layout.Pane();
        coinBD.setPrefSize(tailleCoin, tailleCoin);
        coinBD.setStyle("-fx-border-color:" + couleur + ";-fx-border-width:0 2.5 2.5 0;");
        StackPane.setAlignment(coinBD, Pos.BOTTOM_RIGHT);

        sp.getChildren().addAll(coinHG, coinHD, coinBG, coinBD);
        return sp;
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 3 : CONSEILS D'UTILISATION
    // ══════════════════════════════════════════════════════════════

    private HBox creerConseilsUtilisation() {
        HBox conseils = new HBox(0);
        conseils.setAlignment(Pos.CENTER);
        conseils.setPadding(new Insets(10, 20, 10, 20));
        conseils.setStyle("-fx-background-color:" + SURFACE + ";");

        // 4 conseils avec séparateurs entre eux
        String[][] items = {
                {"💡", "Bonne lumière"},
                {"📐", "Carte à plat"},
                {"🔍", "Texte visible"},
                {"⚡", "Résultat rapide"},
        };

        for (int i = 0; i < items.length; i++) {
            // Un conseil = icône + texte empilés verticalement
            VBox conseil = new VBox(4);
            conseil.setAlignment(Pos.CENTER);
            conseil.setPrefWidth(130);

            Label emoji = new Label(items[i][0]);
            emoji.setStyle("-fx-font-size:17px;");

            Label texte = new Label(items[i][1]);
            texte.setStyle("-fx-text-fill:" + DIM + ";-fx-font-size:10px;");

            conseil.getChildren().addAll(emoji, texte);
            conseils.getChildren().add(conseil);

            // Séparateur vertical entre les conseils (sauf après le dernier)
            if (i < items.length - 1) {
                Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
                sep.setStyle("-fx-background-color:rgba(0,217,255,0.10);");
                conseils.getChildren().add(sep);
            }
        }
        return conseils;
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 4 : BARRE DE CONTRÔLES (Status + Bouton)
    // ══════════════════════════════════════════════════════════════

    private VBox creerBarreControles() {
        VBox barre = new VBox(10);
        barre.setPadding(new Insets(14, 20, 14, 20));
        barre.setAlignment(Pos.CENTER);
        barre.setStyle(
                // Filet fin en haut
                "-fx-border-color:rgba(0,217,255,0.08) transparent transparent transparent;" +
                        "-fx-border-width:1 0 0 0;"
        );

        // ── Label Status ─────────────────────────────────
        // Ce label change selon l'état :
        // • "⏳ Initialisation..." au démarrage
        // • "✓ Caméra prête" quand la webcam est ouverte
        // • "🔍 Envoi à Claude..." pendant l'analyse
        // • "✓ Analysé !" après succès
        // • "❌ Erreur..." en cas de problème
        lblStatus = new Label("⏳  Initialisation de la caméra...");
        lblStatus.setStyle("-fx-text-fill:" + DIM + ";-fx-font-size:12px;");

        // ── Bouton Capturer ───────────────────────────────
        btnCapturer = new Button("📷   CAPTURER ET ANALYSER AVEC L'IA");
        btnCapturer.setMaxWidth(Double.MAX_VALUE); // Prend toute la largeur
        btnCapturer.setDisable(true); // Désactivé jusqu'à ce que la caméra soit prête

        appliquerStyleBouton(btnCapturer, false); // Style normal

        // Effets hover
        btnCapturer.setOnMouseEntered(e -> {
            if (!btnCapturer.isDisabled()) appliquerStyleBouton(btnCapturer, true);
        });
        btnCapturer.setOnMouseExited(e -> appliquerStyleBouton(btnCapturer, false));

        // Action principale : capturer et analyser
        btnCapturer.setOnAction(e -> capturerEtAnalyser());

        barre.getChildren().addAll(lblStatus, btnCapturer);
        return barre;
    }

    /** Applique le style du bouton capturer (normal ou hover) */
    private void appliquerStyleBouton(Button btn, boolean hover) {
        btn.setStyle(
                "-fx-background-color:linear-gradient(to right," +
                        (hover ? GREEN + "," + CYAN : CYAN + "," + GREEN) + ");" +
                        "-fx-text-fill:" + DARK + ";" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:14px;" +
                        "-fx-background-radius:12;" +
                        "-fx-border-radius:12;" +
                        "-fx-padding:13 20;" +
                        "-fx-cursor:hand;" +
                        (hover ? "-fx-effect:dropshadow(gaussian," + CYAN + ",16,0.45,0,0);" : "")
        );
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 5 : RÉSULTAT DU SCAN (caché au départ)
    // ══════════════════════════════════════════════════════════════

    private VBox creerSectionResultat() {
        sectionResultat = new VBox(12);
        sectionResultat.setPadding(new Insets(0, 20, 20, 20));

        // Caché au départ, affiché après un scan réussi
        sectionResultat.setVisible(false);
        sectionResultat.setManaged(false); // Ne prend pas de place quand caché

        // ── Carte affichant les informations détectées ────
        VBox carteResultat = new VBox(12);
        carteResultat.setPadding(new Insets(18, 20, 18, 20));
        carteResultat.setStyle(
                "-fx-background-color:" + CARD + ";" +
                        "-fx-border-color:rgba(0,255,136,0.28);" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:14;" +
                        "-fx-background-radius:14;"
        );

        // Titre de la carte résultat
        HBox ligneTitre = new HBox(8);
        ligneTitre.setAlignment(Pos.CENTER_LEFT);
        Label checkVert = new Label("✓");
        checkVert.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:16px;-fx-font-weight:bold;");
        Label titreCarte = new Label("Carte bancaire détectée avec succès !");
        titreCarte.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:13px;-fx-font-weight:bold;");
        ligneTitre.getChildren().addAll(checkVert, titreCarte);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:rgba(0,255,136,0.12);");

        // Créer les labels pour chaque information
        lblNumero     = creerLabelResultat("•••• •••• •••• ••••");
        lblExpiration = creerLabelResultat("MM/AA");
        lblTitulaire  = creerLabelResultat("—");

        carteResultat.getChildren().addAll(
                ligneTitre,
                sep,
                creerLigneResultat("💳   Numéro de carte",   lblNumero),
                creerLigneResultat("📅   Date d'expiration",  lblExpiration),
                creerLigneResultat("👤   Titulaire",           lblTitulaire)
        );

        // ── Boutons d'action ──────────────────────────────
        HBox boutonsAction = new HBox(10);

        // Bouton "✓ Utiliser" — remplit les champs du formulaire
        Button btnUtiliser = new Button("✓   Utiliser ces informations");
        btnUtiliser.setStyle(
                "-fx-background-color:linear-gradient(to right," + GREEN + "," + CYAN + ");" +
                        "-fx-text-fill:" + DARK + ";" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:13px;" +
                        "-fx-background-radius:12;" +
                        "-fx-border-radius:12;" +
                        "-fx-padding:12 20;" +
                        "-fx-cursor:hand;"
        );
        btnUtiliser.setOnAction(e -> {
            // Appeler le callback avec les données du scan
            if (callback != null && dernierResultat != null) {
                callback.accept(dernierResultat);
            }
            // Fermer la dialog proprement
            nettoyerRessources();
            fenetreDialog.close();
        });

        // Bouton "↩ Réessayer" — cache le résultat et permet de rescanner
        Button btnReessayer = new Button("↩   Réessayer");
        btnReessayer.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:" + MUTED + ";" +
                        "-fx-border-color:rgba(0,217,255,0.20);" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:12;" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:12 20;" +
                        "-fx-cursor:hand;"
        );
        btnReessayer.setOnMouseEntered(e -> btnReessayer.setStyle(
                "-fx-background-color:rgba(0,217,255,0.08);-fx-text-fill:" + CYAN + ";" +
                        "-fx-border-color:" + CYAN + ";-fx-border-width:1;" +
                        "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 20;-fx-cursor:hand;"
        ));
        btnReessayer.setOnMouseExited(e -> btnReessayer.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + MUTED + ";" +
                        "-fx-border-color:rgba(0,217,255,0.20);-fx-border-width:1;" +
                        "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 20;-fx-cursor:hand;"
        ));
        btnReessayer.setOnAction(e -> cacherResultat());

        HBox.setHgrow(btnUtiliser, Priority.ALWAYS);
        boutonsAction.getChildren().addAll(btnUtiliser, btnReessayer);

        sectionResultat.getChildren().addAll(carteResultat, boutonsAction);
        return sectionResultat;
    }

    /** Crée une ligne "Étiquette : Valeur" dans la carte résultat */
    private HBox creerLigneResultat(String etiquette, Label valeur) {
        HBox ligne = new HBox(12);
        ligne.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(etiquette);
        lbl.setPrefWidth(170);
        lbl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");

        ligne.getChildren().addAll(lbl, valeur);
        return ligne;
    }

    /** Crée un label stylisé pour afficher une valeur (numéro, expiration, titulaire) */
    private Label creerLabelResultat(String placeholder) {
        Label l = new Label(placeholder);
        l.setStyle(
                "-fx-text-fill:" + TEXT + ";" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:14px;" +
                        "-fx-letter-spacing:1.5px;" +
                        "-fx-font-family:'Consolas';"
        );
        return l;
    }

    // ══════════════════════════════════════════════════════════════
    //  📷 DÉMARRAGE DE LA WEBCAM
    // ══════════════════════════════════════════════════════════════

    /**
     * Démarre la webcam dans un thread séparé pour ne pas bloquer l'UI.
     *
     * POURQUOI UN THREAD SÉPARÉ ?
     * ─────────────────────────────
     * L'ouverture d'une webcam prend 1 à 3 secondes.
     * Si on le fait sur le thread JavaFX principal, l'UI se gèle.
     * On utilise CompletableFuture pour exécuter en arrière-plan.
     *
     * CompletableFuture.runAsync(() -> { ... }) =
     *   "Exécute ce code dans un thread séparé du pool de threads Java"
     */
    private void demarrerWebcam() {
        CompletableFuture.runAsync(() -> {
            try {
                // Obtenir la première webcam disponible sur l'ordinateur
                webcam = Webcam.getDefault();

                // Aucune caméra connectée
                if (webcam == null) {
                    // Platform.runLater = modifier l'UI depuis un thread non-JavaFX
                    // (JavaFX n'autorise les modifications d'UI que depuis son thread principal)
                    Platform.runLater(() ->
                            setStatus("❌  Aucune caméra détectée. Connectez une webcam.", RED)
                    );
                    return;
                }

                // Définir la résolution : VGA = 640 × 480 pixels
                // Assez haute pour lire une carte, pas trop grande pour les perfs
                webcam.setViewSize(WebcamResolution.VGA.getSize());

                // Ouvrir physiquement la caméra (allume le voyant LED)
                webcam.open();

                streaming.set(true); // Indiquer que le stream peut commencer

                // Mettre à jour l'UI : caméra prête
                Platform.runLater(() -> {
                    setStatus("✓  Caméra prête — Positionnez votre carte dans le cadre", GREEN);
                    btnCapturer.setDisable(false); // Activer le bouton

                    // Cacher le placeholder "Initialisation..."
                    javafx.scene.Node ph = fenetreDialog.getScene().lookup("#placeholder");
                    if (ph != null) {
                        ph.setVisible(false);
                        ph.setManaged(false);
                    }
                });

                // ── Démarrer le thread de stream vidéo ──────────
                // Ce thread tourne en boucle et envoie les frames à l'ImageView
                threadStream = new Thread(() -> {
                    while (streaming.get()) { // Boucle tant que streaming = true
                        try {
                            // Ne capture pas si l'IA est en train d'analyser
                            if (webcam.isOpen() && !scanning.get()) {
                                // Capturer une frame de la webcam
                                BufferedImage frame = webcam.getImage();

                                if (frame != null) {
                                    // Convertir en image JavaFX
                                    WritableImage fxImage = awtVersJavaFX(frame);
                                    // Mettre à jour l'ImageView sur le thread JavaFX
                                    Platform.runLater(() -> vueCamera.setImage(fxImage));
                                }
                            }

                            // Pause de 33ms = environ 30 images par seconde
                            // 1000ms / 30fps = 33ms par frame
                            Thread.sleep(33);

                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break; // Sortir de la boucle si interrompu
                        } catch (Exception ex) {
                            // Ignorer les erreurs de frame individuelles
                        }
                    }
                });
                threadStream.setDaemon(true); // Thread "démon" : se ferme automatiquement avec l'app
                threadStream.start();

            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("❌  Erreur caméra : " + e.getMessage(), RED)
                );
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  📸 CAPTURE + ANALYSE IA
    // ══════════════════════════════════════════════════════════════

    /**
     * Déclenché quand l'utilisateur clique "📷 CAPTURER ET ANALYSER".
     *
     * Ce qui se passe dans l'ordre :
     * 1. Désactiver le bouton + afficher l'overlay "Analyse en cours"
     * 2. Capturer une frame de la webcam (dans un thread séparé)
     * 3. Envoyer la frame à Claude Vision API
     * 4. Recevoir le résultat (numéro, expiration, titulaire)
     * 5. Afficher le résultat dans la section résultat
     */
    private void capturerEtAnalyser() {
        if (webcam == null || !webcam.isOpen()) {
            setStatus("❌  Caméra non disponible", RED);
            return;
        }

        // ── Mettre en mode "analyse" ─────────────────────
        scanning.set(true);              // Pause le stream vidéo
        btnCapturer.setDisable(true);    // Désactiver le bouton
        overlayAnalyse.setVisible(true); // Afficher "Analyse en cours..."
        setStatus("🔍  Capture et envoi à Claude Vision...", CYAN);

        // Petite animation sur le bouton (feedback visuel du clic)
        ScaleTransition rebond = new ScaleTransition(Duration.millis(100), btnCapturer);
        rebond.setToX(0.95); rebond.setToY(0.95);
        rebond.setAutoReverse(true); rebond.setCycleCount(2);
        rebond.play();

        // ── Exécuter capture + analyse en arrière-plan ───
        // On utilise CompletableFuture pour ne pas bloquer l'UI
        CompletableFuture
                .supplyAsync(() -> {
                    // THREAD SÉPARÉ : capturer + analyser
                    System.out.println("[Scanner] Capture de la frame...");
                    BufferedImage frameCapturee = webcam.getImage();
                    System.out.println("[Scanner] Frame : " + frameCapturee.getWidth() + "×" + frameCapturee.getHeight());

                    System.out.println("[Scanner] Envoi à Claude Vision API...");
                    return serviceIA.analyserCarte(frameCapturee);
                })
                .thenAccept(resultat -> {
                    // THREAD JAVAFX : mettre à jour l'UI avec le résultat
                    Platform.runLater(() -> {
                        scanning.set(false);              // Reprendre le stream
                        overlayAnalyse.setVisible(false); // Cacher l'overlay
                        btnCapturer.setDisable(false);    // Réactiver le bouton

                        if (resultat.success) {
                            // ✓ Scan réussi
                            dernierResultat = resultat;
                            afficherResultat(resultat);
                            setStatus("✓  Carte analysée avec succès !", GREEN);
                        } else {
                            // ✗ Scan échoué
                            setStatus("❌  " + resultat.erreur, RED);
                        }
                    });
                })
                .exceptionally(ex -> {
                    // En cas d'exception non gérée
                    Platform.runLater(() -> {
                        scanning.set(false);
                        overlayAnalyse.setVisible(false);
                        btnCapturer.setDisable(false);
                        setStatus("❌  Erreur : " + ex.getMessage(), RED);
                    });
                    return null;
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  AFFICHER / CACHER LE RÉSULTAT
    // ══════════════════════════════════════════════════════════════

    private void afficherResultat(CardScannerService.ScanResult resultat) {
        // Remplir les 3 labels
        lblNumero.setText(resultat.numero.isEmpty()     ? "Non détecté" : resultat.numero);
        lblExpiration.setText(resultat.expiration.isEmpty() ? "Non détecté" : resultat.expiration);
        lblTitulaire.setText(resultat.titulaire.isEmpty()   ? "—"           : resultat.titulaire);

        // Afficher la section résultat
        sectionResultat.setOpacity(0); // Commence transparent
        sectionResultat.setVisible(true);
        sectionResultat.setManaged(true);

        // Animation fade in (0 → 1 en 400ms)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), sectionResultat);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        fadeIn.play();

        // Agrandir la fenêtre pour afficher la section résultat
        fenetreDialog.setHeight(fenetreDialog.getHeight() + 200);
    }

    private void cacherResultat() {
        sectionResultat.setVisible(false);
        sectionResultat.setManaged(false);
        dernierResultat = null;
        fenetreDialog.setHeight(fenetreDialog.getHeight() - 200);
        setStatus("✓  Caméra active — Réessayez", CYAN);
    }

    // ══════════════════════════════════════════════════════════════
    //  🛠️ UTILITAIRES
    // ══════════════════════════════════════════════════════════════

    /** Change le texte et la couleur du label de status */
    private void setStatus(String message, String couleur) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:12px;");
    }

    /**
     * Convertit une BufferedImage Java AWT en WritableImage JavaFX.
     *
     * POURQUOI NÉCESSAIRE ?
     * ──────────────────────
     * La webcam (sarxos) retourne des images au format AWT (java.awt.image.BufferedImage).
     * JavaFX utilise son propre format d'image (javafx.scene.image.WritableImage).
     * Ces deux formats ne sont pas compatibles directement.
     * Cette méthode copie pixel par pixel d'un format à l'autre.
     *
     * Performance : pour une image 640×480, ça copie 307 200 pixels.
     * C'est rapide (quelques millisecondes) mais répété 30x par seconde.
     */
    private WritableImage awtVersJavaFX(BufferedImage imageAWT) {
        int largeur  = imageAWT.getWidth();
        int hauteur  = imageAWT.getHeight();

        WritableImage imageFX = new WritableImage(largeur, hauteur);
        PixelWriter ecriteurPixel = imageFX.getPixelWriter();

        // Copier chaque pixel de l'image AWT vers l'image JavaFX
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // getRGB() retourne la couleur du pixel en format ARGB (int)
                ecriteurPixel.setArgb(x, y, imageAWT.getRGB(x, y));
            }
        }
        return imageFX;
    }

    /**
     * Libère les ressources webcam proprement.
     * Appelé quand la fenêtre se ferme (bouton ✕ ou automatiquement).
     *
     * IMPORTANT : Sans ce nettoyage, la caméra reste "occupée" par Java
     * et les autres applications ne peuvent pas l'utiliser.
     */
    private void nettoyerRessources() {
        // Signaler au thread de stream de s'arrêter
        streaming.set(false);

        // Interrompre le thread de stream s'il est en attente (Thread.sleep)
        if (threadStream != null) threadStream.interrupt();

        // Fermer physiquement la caméra (LED s'éteint)
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            System.out.println("[Scanner] ✓ Caméra fermée proprement.");
        }
    }
}