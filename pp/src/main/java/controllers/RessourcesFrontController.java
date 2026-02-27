package controllers;

import entities.Commentaire;
import entities.Ressource;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceCommentaire;
import services.ServiceFavori;
import services.ServiceRessource;
import services.TraductionService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RessourcesFrontController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────
    @FXML private Circle    orb1, orb2, orb3;
    @FXML private Button    btnBackToFront;
    @FXML private TextField searchField;
    @FXML private Button    btnSearch, btnReset;
    @FXML private CheckBox  filterPDF, filterVideo, filterImage, filterAudio, filterArticle;
    @FXML private Label     resultsCount;
    @FXML private VBox      ressourcesContainer;
    /** Onglet favoris — conteneur séparé dans le FXML (optionnel) */
    @FXML private VBox      favorisContainer;

    // ── COULEURS ─────────────────────────────────────────────────────
    private static final String PURPLE       = "#7C3AED";
    private static final String PURPLE_LIGHT = "#A78BFA";
    private static final String CARD_BG      = "rgba(37,37,54,0.95)";
    private static final String TEXT_MAIN    = "#E8E8FF";
    private static final String TEXT_SUB     = "#B8B8D1";
    private static final String SUCCESS      = "#00FF88";
    private static final String DANGER       = "#FF4D6D";
    private static final String GOLD         = "#FFD700";
    private static final String TREND_COLOR  = "#FF6B35";   // orange tendance

    // ── SERVICES ─────────────────────────────────────────────────────
    private ServiceRessource   serviceRessource;
    private ServiceCommentaire serviceCommentaire;
    private TraductionService  traductionService;
    private ServiceFavori      serviceFavori;

    // ── DONNEES ──────────────────────────────────────────────────────
    private List<Ressource>         allRessources;
    private List<Ressource>         filteredRessources;
    private final List<MediaPlayer> activePlayers = new ArrayList<>();

    /**
     * ID de l'utilisateur connecté.
     * À remplacer par la vraie session (ex : SessionManager.getCurrentUserId()).
     */
    private int currentUserId = 1; // TODO : injecter via SessionManager

    /** IDs des ressources marquées en favori pour l'utilisateur courant */
    private Set<Integer> favorisIds = new HashSet<>();

    /** IDs des ressources "Tendance" cette semaine */
    private Set<Integer> tendanceIds = new HashSet<>();

    /** Seuil de vues / 7 jours pour afficher le badge Tendance */
    private static final int SEUIL_TENDANCE = 3;

    /** true = afficher uniquement les favoris de l'utilisateur */
    private boolean showFavorisOnly = false;

    /** Référence au bouton filtre favoris (créé dynamiquement) */
    private Button btnFiltreF = null;

    /**
     * Sous-conteneur dédié UNIQUEMENT aux cartes ressource.
     * Séparé de ressourcesContainer pour que la barre de filtres
     * et la section recommandations ne soient jamais effacées.
     */
    private VBox cardsContainer = null;

    /** Nombre de recommandations à afficher */
    private static final int NB_RECOMMANDATIONS = 5;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[][] LANGUES = {
        {"Francais",  "fr"},
        {"Anglais",   "en"},
        {"Arabe",     "ar"},
        {"Espagnol",  "es"},
        {"Allemand",  "de"},
        {"Italien",   "it"},
        {"Turc",      "tr"},
        {"Portugais", "pt"},
    };

    // ── INIT ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceRessource   = new ServiceRessource();
        serviceCommentaire = new ServiceCommentaire();
        traductionService  = new TraductionService();
        serviceFavori      = new ServiceFavori();

        initializeAnimations();
        loadFavorisEtTendances();
        loadRessources();
        setupEventHandlers();
        // Crée le sous-conteneur des cartes et l'insère dans ressourcesContainer
        cardsContainer = new VBox(16);
        cardsContainer.setMaxWidth(Double.MAX_VALUE);
        if (ressourcesContainer != null) ressourcesContainer.getChildren().add(cardsContainer);

        displayRessources(allRessources);
        buildBtnFiltreFavoris();
        displayRecommandations();
    }

    // ── CHARGEMENT FAVORIS + TENDANCES ───────────────────────────────
    private void loadFavorisEtTendances() {
        try {
            favorisIds  = serviceFavori.getIdsFavorisParUser(currentUserId);
            tendanceIds = serviceRessource.getIdsTendance(SEUIL_TENDANCE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── ANIMATIONS ───────────────────────────────────────────────────
    private void initializeAnimations() {
        if (orb1 != null) animateOrb(orb1,  30, -20, Duration.seconds(15));
        if (orb2 != null) animateOrb(orb2, -35,  25, Duration.seconds(18));
        if (orb3 != null) animateOrb(orb3,  20, -15, Duration.seconds(20));
    }

    private void animateOrb(Circle orb, double dx, double dy, Duration dur) {
        TranslateTransition t = new TranslateTransition(dur, orb);
        t.setByX(dx); t.setByY(dy);
        t.setCycleCount(Animation.INDEFINITE);
        t.setAutoReverse(true);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.play();
    }

    // ── CHARGEMENT ───────────────────────────────────────────────────
    private void loadRessources() {
        try {
            allRessources = serviceRessource.recuperer().stream()
                    .filter(r -> "publi\u00e9".equals(r.getStatut()))
                    .collect(Collectors.toList());

            // Les favoris de l'utilisateur remontent en premier
            allRessources.sort((a, b) -> {
                boolean aFav = favorisIds.contains(a.getIdRessource());
                boolean bFav = favorisIds.contains(b.getIdRessource());
                if (aFav == bFav) return 0;
                return aFav ? -1 : 1;
            });

            filteredRessources = allRessources;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les ressources.", Alert.AlertType.ERROR);
            allRessources      = new ArrayList<>();
            filteredRessources = new ArrayList<>();
        }
    }

    // ── SECTION RECOMMANDATIONS ──────────────────────────────────────
    /**
     * Affiche une section "Recommandé pour vous" au-dessus des ressources
     * ou dans un conteneur dédié si présent dans le FXML.
     */
    private void displayRecommandations() {
        new Thread(() -> {
            try {
                List<Ressource> recos =
                    serviceRessource.getRecommandationsParHistorique(currentUserId, NB_RECOMMANDATIONS);

                Platform.runLater(() -> {
                    VBox target = (ressourcesContainer != null) ? ressourcesContainer : null;
                    if (target == null) return;

                    if (recos.isEmpty()) return;

                    // En-tête section — inséré à l'index 0 (tout en haut)
                    Label titreReco = new Label("Recommand\u00e9 pour vous");
                    titreReco.setStyle(
                        "-fx-text-fill:" + GOLD + ";-fx-font-size:16px;" +
                        "-fx-font-weight:bold;-fx-padding:12 0 4 0;"
                    );

                    HBox cardsRow = new HBox(14);
                    cardsRow.setPadding(new Insets(0, 0, 16, 0));
                    cardsRow.setStyle(
                        "-fx-background-color:rgba(255,215,0,0.04);" +
                        "-fx-border-color:rgba(255,215,0,0.15);-fx-border-width:1;" +
                        "-fx-border-radius:14;-fx-background-radius:14;" +
                        "-fx-padding:14;"
                    );

                    for (Ressource r : recos) {
                        cardsRow.getChildren().add(buildMiniCard(r));
                    }

                    VBox section = new VBox(6, titreReco, cardsRow);
                    section.setPadding(new Insets(0, 0, 10, 0));

                    // Insérer en tête de ressourcesContainer (index 0)
                    if (ressourcesContainer != null)
                        ressourcesContainer.getChildren().add(0, section);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Carte miniature pour la section recommandations.
     */
    private VBox buildMiniCard(Ressource r) {
        VBox card = new VBox(6);
        card.setPrefWidth(160);
        card.setMaxWidth(160);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color:rgba(37,37,54,0.98);" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.3),10,0,0,3);" +
            "-fx-border-color:rgba(124,58,237,0.3);-fx-border-width:1;-fx-border-radius:12;" +
            "-fx-cursor:hand;"
        );

        // Badge type
        Label typeBadge = new Label(getTypeEmoji(r.getType()) + " " + r.getType());
        typeBadge.setStyle(
            "-fx-background-color:rgba(124,58,237,0.25);" +
            "-fx-text-fill:" + PURPLE_LIGHT + ";" +
            "-fx-background-radius:8;-fx-padding:3 8;-fx-font-size:10px;-fx-font-weight:bold;"
        );

        // Titre (tronqué)
        String titreShort = r.getTitre() != null && r.getTitre().length() > 40
                ? r.getTitre().substring(0, 37) + "..." : r.getTitre();
        Label titre = new Label(titreShort);
        titre.setStyle("-fx-text-fill:" + TEXT_MAIN + ";-fx-font-size:12px;-fx-font-weight:bold;");
        titre.setWrapText(true);

        // Badge tendance éventuel
        if (tendanceIds.contains(r.getIdRessource())) {
            Label trendBadge = new Label("\uD83D\uDD25 Tendance");
            trendBadge.setStyle(
                "-fx-background-color:rgba(255,107,53,0.2);" +
                "-fx-text-fill:" + TREND_COLOR + ";" +
                "-fx-background-radius:8;-fx-padding:2 7;-fx-font-size:10px;-fx-font-weight:bold;"
            );
            card.getChildren().addAll(typeBadge, titre, trendBadge);
        } else {
            card.getChildren().addAll(typeBadge, titre);
        }

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
            .replace("rgba(37,37,54,0.98)", "rgba(60,40,100,0.98)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
            .replace("rgba(60,40,100,0.98)", "rgba(37,37,54,0.98)")));

        // Clic → scroll vers la ressource ou ouvrir
        card.setOnMouseClicked(e -> openFile(r.getCheminFichier()));

        return card;
    }

    // ── BOUTON FILTRE FAVORIS (créé dynamiquement) ──────────────────
    /**
     * Crée le bouton "♥ Mes Favoris" et l'insère dans le conteneur
     * principal (au-dessus des cartes). Appeler après initialize().
     */
    private void buildBtnFiltreFavoris() {
        if (ressourcesContainer == null) return;

        btnFiltreF = new Button("♡  Mes Favoris");
        btnFiltreF.setStyle(btnFiltreFavoriStyle(false));

        btnFiltreF.setOnAction(e -> {
            showFavorisOnly = !showFavorisOnly;
            updateBtnFiltreStyle();
            applyFilters();
        });

        // Hover
        btnFiltreF.setOnMouseEntered(e ->
            btnFiltreF.setOpacity(0.82));
        btnFiltreF.setOnMouseExited(e ->
            btnFiltreF.setOpacity(1.0));

        // Compteur favoris entre parenthèses
        int nb = favorisIds.size();
        btnFiltreF.setText("♡  Mes Favoris (" + nb + ")");

        // Barre de filtres insérée dans ressourcesContainer à l'index 0
        // (avant cardsContainer qui est à l'index 0 actuellement → devient index 1)
        HBox filtreBar = new HBox(12, btnFiltreF);
        filtreBar.setAlignment(Pos.CENTER_LEFT);
        filtreBar.setPadding(new Insets(0, 0, 14, 0));
        filtreBar.setId("filtreBarFavoris");

        if (ressourcesContainer != null) ressourcesContainer.getChildren().add(0, filtreBar);
    }

    private void updateBtnFiltreStyle() {
        if (btnFiltreF == null) return;
        btnFiltreF.setStyle(btnFiltreFavoriStyle(showFavorisOnly));
        int nb = favorisIds.size();
        btnFiltreF.setText(
            (showFavorisOnly ? "♥" : "♡") + "  Mes Favoris (" + nb + ")"
        );
    }

    private String btnFiltreFavoriStyle(boolean actif) {
        if (actif) {
            return "-fx-background-color:#C0392B;" +
                   "-fx-text-fill:white;" +
                   "-fx-font-size:13px;-fx-font-weight:bold;" +
                   "-fx-background-radius:22;-fx-border-radius:22;" +
                   "-fx-border-color:#E74C3C;-fx-border-width:1.5;" +
                   "-fx-padding:9 20;-fx-cursor:hand;" +
                   "-fx-effect:dropshadow(gaussian,rgba(231,76,60,0.6),10,0,0,2);";
        } else {
            return "-fx-background-color:rgba(255,255,255,0.07);" +
                   "-fx-text-fill:#E8E8FF;" +
                   "-fx-font-size:13px;-fx-font-weight:bold;" +
                   "-fx-background-radius:22;-fx-border-radius:22;" +
                   "-fx-border-color:rgba(232,232,255,0.4);-fx-border-width:1.5;" +
                   "-fx-padding:9 20;-fx-cursor:hand;";
        }
    }

    // ── EVENT HANDLERS ───────────────────────────────────────────────
    private void setupEventHandlers() {
        if (btnSearch      != null) btnSearch.setOnAction(e -> handleSearch());
        if (searchField    != null) searchField.setOnAction(e -> handleSearch());
        if (btnReset       != null) btnReset.setOnAction(e -> resetFilters());
        if (btnBackToFront != null) btnBackToFront.setOnAction(e -> handleBackToFront());

        if (filterPDF     != null) filterPDF.selectedProperty().addListener((o,a,b) -> applyFilters());
        if (filterVideo   != null) filterVideo.selectedProperty().addListener((o,a,b) -> applyFilters());
        if (filterImage   != null) filterImage.selectedProperty().addListener((o,a,b) -> applyFilters());
        if (filterAudio   != null) filterAudio.selectedProperty().addListener((o,a,b) -> applyFilters());
        if (filterArticle != null) filterArticle.selectedProperty().addListener((o,a,b) -> applyFilters());
    }

    @FXML private void handleSearch() { applyFilters(); }

    @FXML
    private void resetFilters() {
        if (searchField   != null) searchField.clear();
        if (filterPDF     != null) filterPDF.setSelected(false);
        if (filterVideo   != null) filterVideo.setSelected(false);
        if (filterImage   != null) filterImage.setSelected(false);
        if (filterAudio   != null) filterAudio.setSelected(false);
        if (filterArticle != null) filterArticle.setSelected(false);
        showFavorisOnly = false;
        updateBtnFiltreStyle();
        filteredRessources = allRessources;
        displayRessources(filteredRessources);
    }

    private void applyFilters() {
        String s = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        filteredRessources = allRessources.stream()
                .filter(r -> matchesSearch(r, s))
                .filter(this::matchesTypeFilter)
                .filter(r -> !showFavorisOnly || favorisIds.contains(r.getIdRessource()))
                .collect(Collectors.toList());
        displayRessources(filteredRessources);
    }

    private boolean matchesSearch(Ressource r, String s) {
        if (s.isEmpty()) return true;
        return r.getTitre().toLowerCase().contains(s) ||
               (r.getDescription() != null && r.getDescription().toLowerCase().contains(s));
    }

    private boolean matchesTypeFilter(Ressource r) {
        boolean none = (filterPDF     == null || !filterPDF.isSelected())    &&
                       (filterVideo   == null || !filterVideo.isSelected())  &&
                       (filterImage   == null || !filterImage.isSelected())  &&
                       (filterAudio   == null || !filterAudio.isSelected())  &&
                       (filterArticle == null || !filterArticle.isSelected());
        if (none) return true;
        String t = r.getType();
        return (filterPDF     != null && filterPDF.isSelected()     && "PDF".equals(t))       ||
               (filterVideo   != null && filterVideo.isSelected()   && "Vid\u00e9o".equals(t))||
               (filterImage   != null && filterImage.isSelected()   && "Image".equals(t))     ||
               (filterAudio   != null && filterAudio.isSelected()   && "Audio".equals(t))     ||
               (filterArticle != null && filterArticle.isSelected() && "Article".equals(t));
    }

    // ── AFFICHAGE ────────────────────────────────────────────────────
    private void displayRessources(List<Ressource> list) {
        // On cible cardsContainer (sous-conteneur dédié aux cartes)
        // Si pas encore initialisé, on replie sur ressourcesContainer
        VBox target = (cardsContainer != null) ? cardsContainer : ressourcesContainer;
        if (target == null) return;

        activePlayers.forEach(MediaPlayer::stop);
        activePlayers.clear();
        target.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucune ressource trouv\u00e9e");
            empty.setStyle("-fx-text-fill:" + TEXT_SUB + ";-fx-font-size:16px;");
            target.getChildren().add(empty);
        } else {
            int idx = 0;
            for (Ressource r : list) {
                VBox card = createRessourceCard(r);
                target.getChildren().add(card);
                animateCardEntry(card, idx++);
            }
        }
        if (resultsCount != null)
            resultsCount.setText((list != null ? list.size() : 0) + " ressource(s) disponible(s)");
    }

    // ══════════════════════════════════════════════════════════════════
    //  CARTE RESSOURCE
    // ══════════════════════════════════════════════════════════════════
    private VBox createRessourceCard(Ressource r) {
        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color:" + CARD_BG + ";" +
            "-fx-background-radius:18;" +
            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.35),18,0,0,4);" +
            "-fx-border-color:rgba(124,58,237,0.25);" +
            "-fx-border-width:1;-fx-border-radius:18;"
        );
        card.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(
            buildMediaZone(r),
            buildInfoZone(r),
            buildCommentZone(r)
        );
        return card;
    }

    // ── ZONE MEDIA ───────────────────────────────────────────────────
    private Pane buildMediaZone(Ressource r) {
        StackPane zone = new StackPane();
        zone.setMinHeight(200); zone.setMaxHeight(260);
        zone.setStyle(
            "-fx-background-color:rgba(10,8,25,0.85);" +
            "-fx-background-radius:18 18 0 0;"
        );

        File   file = new File(r.getCheminFichier() != null ? r.getCheminFichier() : "");
        String type = r.getType() != null ? r.getType() : "";

        if ("Image".equals(type)) {
            if (file.exists()) {
                try {
                    ImageView iv = new ImageView(new Image(file.toURI().toString(), true));
                    iv.setPreserveRatio(true); iv.setFitWidth(700); iv.setFitHeight(240);
                    zone.getChildren().add(iv);
                } catch (Exception ex) {
                    zone.getChildren().add(placeholder("Image non disponible"));
                }
            } else {
                zone.getChildren().add(placeholder("Image introuvable"));
            }

        } else if ("Vid\u00e9o".equals(type)) {
            if (file.exists()) {
                try {
                    MediaPlayer player = new MediaPlayer(new Media(file.toURI().toString()));
                    activePlayers.add(player);
                    MediaView mv = new MediaView(player);
                    mv.setFitWidth(700); mv.setFitHeight(220); mv.setPreserveRatio(true);
                    VBox w = new VBox(4, mv, buildVideoControls(player));
                    w.setAlignment(Pos.CENTER);
                    zone.getChildren().add(w);
                } catch (Exception ex) {
                    zone.getChildren().add(placeholder("Video non disponible"));
                }
            } else {
                zone.getChildren().add(placeholder("Video introuvable"));
            }

        } else if ("Audio".equals(type)) {
            if (file.exists()) {
                try {
                    MediaPlayer player = new MediaPlayer(new Media(file.toURI().toString()));
                    activePlayers.add(player);
                    zone.getChildren().add(buildAudioPlayer(player, r.getTitre()));
                } catch (Exception ex) {
                    zone.getChildren().add(placeholder("Audio non disponible"));
                }
            } else {
                zone.getChildren().add(placeholder("Audio introuvable"));
            }

        } else if ("PDF".equals(type)) {
            VBox p = new VBox(10); p.setAlignment(Pos.CENTER);
            Label ico = new Label("[ PDF ]"); ico.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:" + PURPLE_LIGHT + ";");
            Label lbl = new Label("Document PDF");
            lbl.setStyle("-fx-text-fill:" + PURPLE_LIGHT + ";-fx-font-size:14px;-fx-font-weight:bold;");
            Label name = new Label(file.getName());
            name.setStyle("-fx-text-fill:" + TEXT_SUB + ";-fx-font-size:12px;");
            p.getChildren().addAll(ico, lbl, name);
            zone.getChildren().add(p);

        } else if ("Article".equals(type)) {
            VBox p = new VBox(10); p.setAlignment(Pos.CENTER);
            Label ico = new Label("[ Article ]"); ico.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" + PURPLE_LIGHT + ";");
            Label lbl = new Label("Article / Document");
            lbl.setStyle("-fx-text-fill:" + PURPLE_LIGHT + ";-fx-font-size:14px;-fx-font-weight:bold;");
            p.getChildren().addAll(ico, lbl);
            zone.getChildren().add(p);

        } else {
            zone.getChildren().add(placeholder("Ressource"));
        }

        return zone;
    }

    private HBox buildVideoControls(MediaPlayer player) {
        Button play = ctrlBtn("Lecture"); Button pause = ctrlBtn("Pause"); Button stop = ctrlBtn("Stop");
        Slider sl = new Slider(0, 1, 0); sl.setPrefWidth(200);
        HBox.setHgrow(sl, Priority.ALWAYS);
        player.currentTimeProperty().addListener((o, ov, nv) -> {
            Duration tot = player.getTotalDuration();
            if (tot != null && !tot.isUnknown()) sl.setValue(nv.toSeconds() / tot.toSeconds());
        });
        sl.setOnMouseReleased(e -> {
            Duration tot = player.getTotalDuration();
            if (tot != null) player.seek(Duration.seconds(sl.getValue() * tot.toSeconds()));
        });
        play.setOnAction(e -> player.play());
        pause.setOnAction(e -> player.pause());
        stop.setOnAction(e -> { player.stop(); sl.setValue(0); });
        HBox h = new HBox(8, play, pause, stop, sl);
        h.setAlignment(Pos.CENTER); h.setPadding(new Insets(6, 12, 6, 12));
        h.setStyle("-fx-background-color:rgba(0,0,0,0.45);");
        return h;
    }

    private VBox buildAudioPlayer(MediaPlayer player, String titre) {
        Label tit = new Label("[ Audio ] " + titre);
        tit.setStyle("-fx-text-fill:" + TEXT_MAIN + ";-fx-font-size:14px;-fx-font-weight:bold;");
        tit.setWrapText(true);
        Slider sl = new Slider(0, 1, 0); sl.setPrefWidth(280);
        Label time = new Label("0:00 / 0:00");
        time.setStyle("-fx-text-fill:" + TEXT_SUB + ";-fx-font-size:11px;");
        player.currentTimeProperty().addListener((o, ov, nv) -> {
            Duration tot = player.getTotalDuration();
            if (tot != null && !tot.isUnknown()) {
                sl.setValue(nv.toSeconds() / tot.toSeconds());
                time.setText(fmt(nv) + " / " + fmt(tot));
            }
        });
        sl.setOnMouseReleased(e -> {
            Duration tot = player.getTotalDuration();
            if (tot != null) player.seek(Duration.seconds(sl.getValue() * tot.toSeconds()));
        });
        Button play = ctrlBtn("Lecture"); Button pause = ctrlBtn("Pause"); Button stop = ctrlBtn("Stop");
        play.setOnAction(e -> player.play());
        pause.setOnAction(e -> player.pause());
        stop.setOnAction(e -> { player.stop(); sl.setValue(0); });
        HBox btns = new HBox(10, play, pause, stop); btns.setAlignment(Pos.CENTER);
        VBox box = new VBox(10, tit, sl, time, btns);
        box.setAlignment(Pos.CENTER); box.setPadding(new Insets(20));
        return box;
    }

    private String fmt(Duration d) {
        int total = (int) d.toSeconds();
        return String.format("%d:%02d", total / 60, total % 60);
    }

    private Label placeholder(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill:" + TEXT_SUB + ";-fx-font-size:14px;");
        return l;
    }

    private Button ctrlBtn(String txt) {
        Button b = new Button(txt);
        b.setStyle(
            "-fx-background-color:rgba(124,58,237,0.35);" +
            "-fx-text-fill:" + PURPLE_LIGHT + ";" +
            "-fx-background-radius:8;-fx-padding:6 14;-fx-font-size:12px;-fx-cursor:hand;"
        );
        return b;
    }

    // ── ZONE INFO ────────────────────────────────────────────────────
    private VBox buildInfoZone(Ressource r) {
        VBox zone = new VBox(10);
        zone.setPadding(new Insets(18, 24, 14, 24));

        // ── Ligne supérieure : badge type + badge tendance + bouton favori ──
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Badge type
        Label typeBadge = new Label(getTypeEmoji(r.getType()) + "  " + r.getType());
        typeBadge.setStyle(
            "-fx-background-color:rgba(124,58,237,0.25);" +
            "-fx-text-fill:" + PURPLE_LIGHT + ";" +
            "-fx-background-radius:10;-fx-padding:4 12;" +
            "-fx-font-size:12px;-fx-font-weight:bold;"
        );

        topRow.getChildren().add(typeBadge);

        // Badge Tendance (si applicable)
        if (tendanceIds.contains(r.getIdRessource())) {
            Label trendBadge = new Label("\uD83D\uDD25 Tendance");
            trendBadge.setStyle(
                "-fx-background-color:rgba(255,107,53,0.18);" +
                "-fx-text-fill:" + TREND_COLOR + ";" +
                "-fx-background-radius:10;-fx-padding:4 12;" +
                "-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-border-color:rgba(255,107,53,0.4);-fx-border-width:1;-fx-border-radius:10;"
            );
            topRow.getChildren().add(trendBadge);
        }

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().add(spacer);

        // ── Bouton favori ❤️ ──
        boolean isFav = favorisIds.contains(r.getIdRessource());
        Button btnFavori = buildFavoriButton(r.getIdRessource(), isFav);
        topRow.getChildren().add(btnFavori);

        // Titre
        Label titre = new Label(r.getTitre());
        titre.setStyle("-fx-text-fill:" + TEXT_MAIN + ";-fx-font-size:17px;-fx-font-weight:bold;");
        titre.setWrapText(true);

        // Description + Traduction
        Label desc = new Label(r.getDescription() != null ? r.getDescription() : "Aucune description.");
        desc.setStyle("-fx-text-fill:" + TEXT_SUB + ";-fx-font-size:13px;");
        desc.setWrapText(true);
        VBox traductionBox = buildTraductionBox(r.getDescription(), desc);

        // Meta
        String dateStr = r.getDatePublication() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(r.getDatePublication()) : "-";
        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
            chip("Format : " + (r.getFormat() != null ? r.getFormat().toUpperCase() : "-")),
            chip("Taille : " + String.format("%.1f Ko", r.getTailleFichier())),
            chip("Date : " + dateStr)
        );

        // Boutons
        Button btnOuvrir = actionBtn("Ouvrir", false);
        btnOuvrir.setOnAction(e -> {
            openFile(r.getCheminFichier());
            new Thread(() -> serviceRessource.enregistrerConsultation(
                    r.getIdRessource(), "front", currentUserId)).start();
        });
        Button btnDl = actionBtn("T\u00e9l\u00e9charger", true);
        btnDl.setOnAction(e -> {
            downloadFile(r);
            new Thread(() -> serviceRessource.enregistrerConsultation(
                    r.getIdRessource(), "front", currentUserId)).start();
        });
        HBox actions = new HBox(12, btnOuvrir, btnDl);
        actions.setAlignment(Pos.CENTER_LEFT);

        zone.getChildren().addAll(topRow, titre, desc, traductionBox, meta, actions);
        return zone;
    }

    // ── BOUTON FAVORI ────────────────────────────────────────────────
    /**
     * Crée le bouton ❤️ / 🤍 toggle pour marquer une ressource en favori.
     */
    private Button buildFavoriButton(int idRessource, boolean isFav) {
        // On utilise du texte pur au lieu d'emojis Unicode pour garantir
        // l'affichage correct dans JavaFX (les emojis peuvent apparaître noirs)
        Button btn = new Button(isFav ? "♥ Favori" : "♡ Favori");
        btn.setStyle(favBtnStyle(isFav));
        btn.setTooltip(new Tooltip(isFav ? "Retirer des favoris" : "Ajouter aux favoris"));

        btn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    boolean nowFav = serviceFavori.toggleFavori(currentUserId, idRessource);
                    Platform.runLater(() -> {
                        if (nowFav) {
                            favorisIds.add(idRessource);
                            btn.setText("\u2665 Favori");
                            btn.setTooltip(new Tooltip("Retirer des favoris"));
                        } else {
                            favorisIds.remove(idRessource);
                            btn.setText("\u2661 Favori");
                            btn.setTooltip(new Tooltip("Ajouter aux favoris"));
                        }
                        btn.setStyle(favBtnStyle(nowFav));
                        // Mettre à jour le compteur du bouton filtre
                        updateBtnFiltreStyle();
                        // Si on est en mode "favoris seulement", rafraîchir la liste
                        if (showFavorisOnly) applyFilters();
                        // Animation pulse
                        ScaleTransition pulse = new ScaleTransition(Duration.millis(180), btn);
                        pulse.setFromX(1); pulse.setFromY(1);
                        pulse.setToX(1.2); pulse.setToY(1.2);
                        pulse.setCycleCount(2); pulse.setAutoReverse(true);
                        pulse.play();
                    });
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                        showAlert("Erreur", "Impossible de mettre \u00e0 jour les favoris.", Alert.AlertType.ERROR));
                }
            }).start();
        });

        // Hover effects
        btn.setOnMouseEntered(e -> {
            String current = btn.getText();
            btn.setStyle(favBtnStyle(current.startsWith("\u2665")) + "-fx-opacity:0.85;");
        });
        btn.setOnMouseExited(e -> {
            String current = btn.getText();
            btn.setStyle(favBtnStyle(current.startsWith("\u2665")));
        });

        return btn;
    }

    private String favBtnStyle(boolean isFav) {
        if (isFav) {
            // Favori actif : rouge/rose bien visible
            return "-fx-background-color:#C0392B;" +
                   "-fx-text-fill:white;" +
                   "-fx-font-size:13px;-fx-font-weight:bold;" +
                   "-fx-background-radius:20;-fx-border-radius:20;" +
                   "-fx-border-color:#E74C3C;-fx-border-width:1;" +
                   "-fx-padding:8 16;-fx-cursor:hand;" +
                   "-fx-effect:dropshadow(gaussian,rgba(231,76,60,0.55),8,0,0,2);";
        } else {
            // Favori inactif : contour visible sur fond sombre
            return "-fx-background-color:rgba(255,255,255,0.08);" +
                   "-fx-text-fill:#E8E8FF;" +
                   "-fx-font-size:13px;-fx-font-weight:bold;" +
                   "-fx-background-radius:20;-fx-border-radius:20;" +
                   "-fx-border-color:rgba(232,232,255,0.45);-fx-border-width:1.5;" +
                   "-fx-padding:8 16;-fx-cursor:hand;";
        }
    }

    // ── EMOJI TYPE ───────────────────────────────────────────────────
    private String getTypeEmoji(String type) {
        if (type == null) return "";
        switch (type) {
            case "PDF":     return "\uD83D\uDCC4";
            case "Vid\u00e9o": return "\uD83C\uDFA5";
            case "Audio":   return "\uD83C\uDFB5";
            case "Image":   return "\uD83D\uDDBC\uFE0F";
            case "Article": return "\uD83D\uDCDD";
            default:        return "\uD83D\uDCC1";
        }
    }

    // ── BLOC TRADUCTION ──────────────────────────────────────────────
    private VBox buildTraductionBox(String texteOriginal, Label descLabel) {
        VBox box = new VBox(10);
        box.setStyle(
            "-fx-background-color:rgba(255,215,0,0.07);" +
            "-fx-border-color:rgba(255,215,0,0.3);-fx-border-width:1;" +
            "-fx-border-radius:10;-fx-background-radius:10;" +
            "-fx-padding:12 16;"
        );

        Label titre = new Label("Traduire la description");
        titre.setStyle("-fx-text-fill:" + GOLD + ";-fx-font-size:12px;-fx-font-weight:bold;");

        ComboBox<String> langCombo = new ComboBox<>();
        for (String[] lang : LANGUES) langCombo.getItems().add(lang[0]);
        langCombo.setValue("Anglais");
        langCombo.setStyle(
            "-fx-background-color:rgba(37,37,54,0.95);" +
            "-fx-text-fill:" + TEXT_MAIN + ";" +
            "-fx-border-color:rgba(255,215,0,0.35);-fx-border-width:1;-fx-border-radius:8;" +
            "-fx-font-size:12px;"
        );
        langCombo.setPrefWidth(130);

        Button btnTrad = new Button("Traduire");
        btnTrad.setStyle(
            "-fx-background-color:rgba(255,215,0,0.2);" +
            "-fx-text-fill:" + GOLD + ";" +
            "-fx-border-color:rgba(255,215,0,0.55);-fx-border-width:1;" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-padding:7 18;-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;"
        );

        Button btnOriginal = new Button("Texte original");
        btnOriginal.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-text-fill:rgba(255,215,0,0.65);" +
            "-fx-border-color:rgba(255,215,0,0.3);-fx-border-width:1;" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-padding:7 12;-fx-font-size:12px;-fx-cursor:hand;"
        );
        btnOriginal.setVisible(false);

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-text-fill:rgba(255,215,0,0.75);-fx-font-size:11px;-fx-font-style:italic;");

        btnTrad.setOnAction(e -> {
            String langNom  = langCombo.getValue();
            String langCode = getLangCode(langNom);
            String texte = texteOriginal != null && !texteOriginal.isBlank()
                ? texteOriginal : "Aucune description disponible.";
            String langSource = "fr".equals(langCode) ? "en" : "fr";

            btnTrad.setDisable(true);
            btnOriginal.setVisible(false);
            statusLbl.setText("Traduction en cours...");

            Thread t = new Thread(() -> {
                try {
                    String traduit = TraductionService.traduire(texte, langSource, langCode);
                    Platform.runLater(() -> {
                        descLabel.setText(traduit);
                        btnTrad.setDisable(false);
                        btnOriginal.setVisible(true);
                        statusLbl.setText("Traduit en : " + langNom);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnTrad.setDisable(false);
                        statusLbl.setText("Erreur : " + ex.getMessage());
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        });

        btnOriginal.setOnAction(e -> {
            descLabel.setText(
                texteOriginal != null && !texteOriginal.isBlank()
                    ? texteOriginal : "Aucune description disponible."
            );
            btnOriginal.setVisible(false);
            statusLbl.setText("");
        });

        HBox controls = new HBox(10, langCombo, btnTrad, btnOriginal);
        controls.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(titre, controls, statusLbl);
        return box;
    }

    private String getLangCode(String nom) {
        for (String[] lang : LANGUES) {
            if (lang[0].equals(nom)) return lang[1];
        }
        return "en";
    }

    private Label chip(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill:" + PURPLE_LIGHT + ";-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }

    private Button actionBtn(String txt, boolean solid) {
        Button b = new Button(txt);
        String style = solid
            ? "-fx-background-color:" + SUCCESS + ";-fx-text-fill:#050C07;"
            : "-fx-background-color:rgba(124,58,237,0.25);" +
              "-fx-border-color:rgba(124,58,237,0.55);-fx-border-width:1;" +
              "-fx-text-fill:" + PURPLE_LIGHT + ";";
        b.setStyle(style +
            "-fx-background-radius:10;-fx-border-radius:10;" +
            "-fx-padding:10 22;-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;"
        );
        return b;
    }

    // ══════════════════════════════════════════════════════════════════
    //  ZONE COMMENTAIRES
    // ══════════════════════════════════════════════════════════════════
    private VBox buildCommentZone(Ressource r) {
        VBox zone = new VBox(10);
        zone.setPadding(new Insets(0, 24, 20, 24));

        Separator sep = new Separator();
        Label title = new Label("Commentaires");
        title.setStyle("-fx-text-fill:" + PURPLE_LIGHT + ";-fx-font-size:13px;-fx-font-weight:bold;");

        VBox listBox = new VBox(6);
        chargerCommentaires(listBox, r.getIdRessource());

        TextField input = new TextField();
        input.setPromptText("Ecrire un commentaire...");
        input.setStyle(
            "-fx-background-color:rgba(255,255,255,0.06);" +
            "-fx-text-fill:" + TEXT_MAIN + ";" +
            "-fx-prompt-text-fill:rgba(200,200,220,0.45);" +
            "-fx-border-color:rgba(124,58,237,0.35);-fx-border-width:1;" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-padding:9 14;-fx-font-size:13px;"
        );
        HBox.setHgrow(input, Priority.ALWAYS);

        Button btnSend = new Button("Envoyer");
        btnSend.setStyle(
            "-fx-background-color:linear-gradient(to right," + PURPLE + "," + PURPLE_LIGHT + ");" +
            "-fx-text-fill:white;-fx-background-radius:8;" +
            "-fx-padding:9 18;-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;"
        );

        btnSend.setOnAction(e -> {
            String txt = input.getText().trim();
            if (txt.isEmpty()) return;
            try {
                serviceCommentaire.ajouter(new Commentaire(r.getIdRessource(), txt));
                input.clear();
                chargerCommentaires(listBox, r.getIdRessource());
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible d'enregistrer le commentaire.", Alert.AlertType.ERROR);
            }
        });
        input.setOnAction(e -> btnSend.fire());

        HBox inputRow = new HBox(10, input, btnSend);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        zone.getChildren().addAll(sep, title, listBox, inputRow);
        return zone;
    }

    private void chargerCommentaires(VBox listBox, int idRessource) {
        listBox.getChildren().clear();
        try {
            List<Commentaire> liste = serviceCommentaire.recupererParRessource(idRessource);
            if (liste.isEmpty()) {
                Label none = new Label("Soyez le premier a commenter !");
                none.setStyle("-fx-text-fill:rgba(180,180,210,0.5);-fx-font-size:12px;-fx-font-style:italic;");
                listBox.getChildren().add(none);
            } else {
                for (Commentaire c : liste)
                    listBox.getChildren().add(buildCommentRow(c, listBox, idRessource));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Impossible de charger les commentaires.");
            err.setStyle("-fx-text-fill:" + DANGER + ";-fx-font-size:12px;");
            listBox.getChildren().add(err);
        }
    }

    private HBox buildCommentRow(Commentaire c, VBox listBox, int idRessource) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle(
            "-fx-background-color:rgba(124,58,237,0.1);" +
            "-fx-background-radius:10;" +
            "-fx-border-color:rgba(124,58,237,0.18);-fx-border-width:1;-fx-border-radius:10;"
        );

        String dateStr = c.getDateCommentaire() != null
            ? "  [" + c.getDateCommentaire().format(DATE_FMT) + "]" : "";

        VBox textBox = new VBox(2);
        Label contenu = new Label(c.getContenu());
        contenu.setStyle("-fx-text-fill:" + TEXT_MAIN + ";-fx-font-size:13px;");
        contenu.setWrapText(true);
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-text-fill:rgba(180,180,210,0.5);-fx-font-size:10px;");
        textBox.getChildren().addAll(contenu, dateLbl);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button btnDel = new Button("X");
        btnDel.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-text-fill:rgba(255,77,109,0.7);" +
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:0 6;"
        );
        btnDel.setOnMouseEntered(e -> btnDel.setStyle(btnDel.getStyle().replace("0.7", "1.0")));
        btnDel.setOnMouseExited(e  -> btnDel.setStyle(btnDel.getStyle().replace("1.0", "0.7")));
        btnDel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Supprimer"); confirm.setHeaderText(null);
            confirm.setContentText("Supprimer ce commentaire ?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    try {
                        serviceCommentaire.supprimer(c.getIdCommentaire());
                        chargerCommentaires(listBox, idRessource);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer.", Alert.AlertType.ERROR);
                    }
                }
            });
        });

        row.getChildren().addAll(textBox, btnDel);
        return row;
    }

    // ── TELECHARGEMENT ───────────────────────────────────────────────
    private void downloadFile(Ressource r) {
        String path = r.getCheminFichier();
        if (path == null || path.isBlank()) {
            showAlert("T\u00e9l\u00e9chargement", "Aucun fichier associ\u00e9.", Alert.AlertType.WARNING);
            return;
        }
        File source = new File(path);
        if (!source.exists()) {
            showAlert("T\u00e9l\u00e9chargement", "Fichier introuvable :\n" + path, Alert.AlertType.ERROR);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer sous...");
        chooser.setInitialFileName(source.getName());
        String ext = r.getFormat() != null ? r.getFormat().toLowerCase() : "";
        if (!ext.isEmpty())
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(ext.toUpperCase(), "*." + ext));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous", "*.*"));
        Stage owner = (Stage) ressourcesContainer.getScene().getWindow();
        File dest = chooser.showSaveDialog(owner);
        if (dest != null) {
            try {
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert("Succ\u00e8s", "Fichier enregistr\u00e9 :\n" + dest.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException ex) {
                showAlert("Erreur", "Copie \u00e9chou\u00e9e :\n" + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // ── OUVRIR ───────────────────────────────────────────────────────
    private void openFile(String path) {
        try {
            File f = new File(path != null ? path : "");
            if (f.exists()) Desktop.getDesktop().open(f);
            else showAlert("Erreur", "Fichier introuvable : " + path, Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le fichier.", Alert.AlertType.ERROR);
        }
    }

    // ── ANIMATION ────────────────────────────────────────────────────
    private void animateCardEntry(VBox card, int idx) {
        card.setOpacity(0); card.setTranslateY(35);
        FadeTransition      fade  = new FadeTransition(Duration.millis(480), card);
        TranslateTransition slide = new TranslateTransition(Duration.millis(480), card);
        fade.setFromValue(0); fade.setToValue(1);
        slide.setFromY(35); slide.setToY(0);
        fade.setDelay(Duration.millis(60 * idx));
        slide.setDelay(Duration.millis(60 * idx));
        new ParallelTransition(fade, slide).play();
    }

    // ── NAVIGATION ───────────────────────────────────────────────────
    @FXML private void handleBackToFront()  { navigate("/views/front.fxml",          "Accueil"); }
    @FXML private void handleEvents()       { navigate("/views/showEvent.fxml",       "Evenements"); }
    @FXML private void handleEvaluations()  { navigate("/views/showEvaluation.fxml",  "Evaluations"); }

    private void navigate(String fxml, String title) {
        try {
            activePlayers.forEach(MediaPlayer::stop);
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) (btnBackToFront != null
                ? btnBackToFront : ressourcesContainer).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger : " + fxml, Alert.AlertType.ERROR);
        }
    }

    // ── UTILS ────────────────────────────────────────────────────────
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
