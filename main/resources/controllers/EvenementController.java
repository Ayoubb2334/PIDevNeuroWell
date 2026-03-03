package controllers;

import entities.Evenement;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import services.ServiceEvenement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  NEUROWELL — Créer un Événement                          ║
 * ║  Contrôle de saisie complet · Dark Futuristic UI         ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * RÈGLES DE VALIDATION
 * ─────────────────────────────────────────────────────────
 * Titre        : 5–100 caractères, pas de caractères spéciaux dangereux
 * Description  : 10–300 caractères
 * Date         : obligatoire, pas dans le passé, max +5 ans
 * Localisation : 3–100 caractères, ne commence pas par un chiffre
 * Type         : choix fermé parmi : Conférence, Formation, Workshop, Autre
 * Capacité     : entier entre 1 et 10 000
 * Statut       : automatiquement "En cours" (non modifiable)
 * Prix         : nombre ≥ 0 suivi de "DT" (ex: 0DT, 50DT, 29.99DT)
 * Image        : optionnelle, PNG/JPG/GIF, max 5 Mo
 */
public class EvenementController {

    // ═══════════════════════════════════════════════════════
    //  PALETTE
    // ═══════════════════════════════════════════════════════
    private static final String C_CYAN    = "#00D9FF";
    private static final String C_GREEN   = "#00FF88";
    private static final String C_DARK    = "#050C07";
    private static final String C_CARD    = "#0D1F12";
    private static final String C_SURFACE = "#112016";
    private static final String C_DANGER  = "#FF4D6D";
    private static final String C_TEXT    = "#E8FFF0";

    // ═══════════════════════════════════════════════════════
    //  FXML
    // ═══════════════════════════════════════════════════════
    @FXML private TextField        titreField;
    @FXML private TextField        imageField;
    @FXML private Button           btnChooseImage;
    @FXML private ImageView        imagePreview;
    @FXML private TextArea         descriptionField;
    @FXML private DatePicker       dateField;
    @FXML private TextField        localisationField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        capaciteField;
    @FXML private Label            statutLabel;
    @FXML private TextField        prixField;
    @FXML private Button           btnAjouter;
    @FXML private Button           btnReset;

    @FXML private Label titreCpt;
    @FXML private Label descCpt;

    @FXML private Label titreError;
    @FXML private Label imageError;
    @FXML private Label descriptionError;
    @FXML private Label dateError;
    @FXML private Label localisationError;
    @FXML private Label typeError;
    @FXML private Label capaciteError;
    @FXML private Label prixError;

    // ═══════════════════════════════════════════════════════
    //  STATE
    // ═══════════════════════════════════════════════════════
    private ServiceEvenement service;
    private String           selectedImagePath = "";
    private Runnable         onEventAdded;

    public void setOnEventAdded(Runnable r) { this.onEventAdded = r; }

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        service = new ServiceEvenement();

        initTypeCombo();
        styleAllFields();
        styleButtons();
        styleErrorLabels();
        wireCharCounters();
        wireLiveValidation();
        wireInputFilters();
        wireActions();
        animateEntrance();
    }

    // ═══════════════════════════════════════════════════════
    //  TYPE COMBO
    // ═══════════════════════════════════════════════════════
    private void initTypeCombo() {
        if (typeCombo == null) return;
        typeCombo.getItems().addAll("Conférence", "Formation", "Workshop", "Autre");
        typeCombo.setPromptText("Sélectionner un type…");
        typeCombo.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-text-fill: " + C_TEXT + "; " +
                        "-fx-font-size: 13px; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: rgba(0,217,255,0.20); " +
                        "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-padding: 6 12;"
        );
    }

    // ═══════════════════════════════════════════════════════
    //  STYLES
    // ═══════════════════════════════════════════════════════
    private void styleAllFields() {
        for (TextField tf : new TextField[]{
                titreField, localisationField, capaciteField, prixField
        }) styleTextField(tf);

        if (imageField != null) {
            imageField.setEditable(false);
            imageField.setPromptText("Aucune image sélectionnée");
            styleTextField(imageField);
        }

        styleTextArea(descriptionField);
        styleDatePicker(dateField);

        if (statutLabel != null) {
            statutLabel.setText("🟡  En cours  (automatique)");
            statutLabel.setStyle(
                    "-fx-text-fill: #FFB347; -fx-font-size: 12px; " +
                            "-fx-font-weight: 600; -fx-padding: 6 12; " +
                            "-fx-background-color: rgba(255,179,71,0.10); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: rgba(255,179,71,0.30); " +
                            "-fx-border-width: 1; -fx-border-radius: 8;"
            );
        }

        if (imagePreview != null) {
            imagePreview.setFitWidth(180);
            imagePreview.setFitHeight(130);
            imagePreview.setPreserveRatio(true);
            Rectangle clip = new Rectangle(180, 130);
            clip.setArcWidth(14); clip.setArcHeight(14);
            imagePreview.setClip(clip);
            DropShadow ds = new DropShadow();
            ds.setColor(Color.web(C_CYAN, 0.25)); ds.setRadius(16);
            imagePreview.setEffect(ds);
        }
    }

    private void styleTextField(TextField tf) {
        if (tf == null) return;
        tf.setStyle(fieldStyle(false));
        tf.focusedProperty().addListener((o, old, f) -> {
            tf.setStyle(fieldStyle(f));
            if (f) glowNode(tf, C_CYAN, 0.28); else tf.setEffect(null);
        });
    }

    private void styleTextArea(TextArea ta) {
        if (ta == null) return;
        ta.setStyle(areaStyle(false));
        ta.focusedProperty().addListener((o, old, f) -> {
            ta.setStyle(areaStyle(f));
            if (f) glowNode(ta, C_CYAN, 0.25); else ta.setEffect(null);
        });
    }

    private void styleDatePicker(DatePicker dp) {
        if (dp == null) return;
        dp.setStyle(
                "-fx-background-color: " + C_SURFACE + "; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: rgba(0,217,255,0.20); " +
                        "-fx-border-width: 1.5; -fx-border-radius: 12;"
        );
        dp.getEditor().setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px; -fx-padding: 8 12;"
        );
    }

    private String fieldStyle(boolean focused) {
        return "-fx-background-color: " + C_SURFACE + "; " +
                "-fx-text-fill: " + C_TEXT + "; " +
                "-fx-prompt-text-fill: rgba(200,255,220,0.28); " +
                "-fx-font-size: 13px; -fx-padding: 10 14; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: " + (focused ? C_CYAN : "rgba(0,217,255,0.20)") + "; " +
                "-fx-border-width: " + (focused ? "2" : "1.5") + "; -fx-border-radius: 12;";
    }

    private String areaStyle(boolean focused) {
        return "-fx-control-inner-background: " + C_SURFACE + "; " +
                "-fx-background-color: " + C_SURFACE + "; " +
                "-fx-text-fill: " + C_TEXT + "; " +
                "-fx-prompt-text-fill: rgba(200,255,220,0.28); " +
                "-fx-font-size: 13px; -fx-padding: 10 14; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: " + (focused ? C_CYAN : "rgba(0,217,255,0.20)") + "; " +
                "-fx-border-width: " + (focused ? "2" : "1.5") + "; -fx-border-radius: 12;";
    }

    private void glowNode(javafx.scene.Node n, String color, double alpha) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(color, alpha)); ds.setRadius(14);
        n.setEffect(ds);
    }

    // ── Button styles ────────────────────────────────────────
    private void styleButtons() {
        if (btnChooseImage != null) {
            btnChooseImage.setText("🖼  Choisir une image");
            applyOutlineBtn(btnChooseImage, C_CYAN);
        }
        if (btnAjouter != null) {
            btnAjouter.setText("✚  Créer l'événement");
            btnAjouter.setMaxWidth(Double.MAX_VALUE);
            applyGradientBtn(btnAjouter);
        }
        if (btnReset != null) {
            btnReset.setText("↺  Réinitialiser");
            applyOutlineBtn(btnReset, C_DANGER);
        }
    }

    private void applyOutlineBtn(Button btn, String color) {
        String base =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 9 18; " +
                        "-fx-border-color: " + color + "; -fx-border-width: 1.5; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(base.replace("transparent", color)
                    .replace("text-fill: " + color, "text-fill: " + C_DARK));
            glowNode(btn, color, 0.30);
        });
        btn.setOnMouseExited(e -> { btn.setStyle(base); btn.setEffect(null); });
    }

    private void applyGradientBtn(Button btn) {
        String base =
                "-fx-background-color: linear-gradient(to right, " + C_CYAN + ", " + C_GREEN + "); " +
                        "-fx-text-fill: " + C_DARK + "; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-padding: 13 28; -fx-background-radius: 28; -fx-cursor: hand;";
        btn.setStyle(base);
        glowNode(btn, C_CYAN, 0.35);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(base.replace(C_CYAN + ", " + C_GREEN, C_GREEN + ", " + C_CYAN));
            glowNode(btn, C_GREEN, 0.50);
            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.03); st.setToY(1.03); st.play();
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(base); glowNode(btn, C_CYAN, 0.35);
            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
    }

    // ── Error labels ─────────────────────────────────────────
    private void styleErrorLabels() {
        for (Label l : new Label[]{
                titreError, imageError, descriptionError, dateError,
                localisationError, typeError, capaciteError, prixError
        }) {
            if (l == null) continue;
            l.setStyle("-fx-text-fill: " + C_DANGER + "; -fx-font-size: 11px; -fx-padding: 2 0 0 4;");
            l.setVisible(false); l.setManaged(false);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  CHARACTER COUNTERS
    // ═══════════════════════════════════════════════════════
    private void wireCharCounters() {
        bindCounter(titreField,       titreCpt, 100);
        bindCounter(descriptionField, descCpt,  300);
    }

    // FIX: unified bindCounter using TextInputControl to avoid duplication
    private void bindCounter(TextInputControl field, Label cpt, int max) {
        if (field == null || cpt == null) return;
        cpt.setText("0 / " + max);
        cpt.setStyle("-fx-text-fill: rgba(200,255,220,0.45); -fx-font-size: 10px;");
        field.textProperty().addListener((o, old, n) -> {
            int len = n.length();
            cpt.setText(len + " / " + max);
            cpt.setStyle("-fx-text-fill: " +
                    (len > max ? C_DANGER : "rgba(200,255,220,0.45)") +
                    "; -fx-font-size: 10px;");
        });
    }

    // ═══════════════════════════════════════════════════════
    //  INPUT FILTERS
    // ═══════════════════════════════════════════════════════
    private void wireInputFilters() {
        filterField(capaciteField, "[0-9]*");
        filterField(prixField, "[0-9]*\\.?[0-9]{0,2}(?:DT)?");

        if (titreField != null) titreField.textProperty().addListener((o, old, n) -> {
            if (n.matches(".*[<>\"';%()&+].*")) titreField.setText(old);
        });
    }

    private void filterField(TextField tf, String regex) {
        if (tf == null) return;
        tf.textProperty().addListener((o, old, n) -> {
            if (!n.matches(regex)) tf.setText(old);
        });
    }

    // ═══════════════════════════════════════════════════════
    //  LIVE VALIDATION
    // ═══════════════════════════════════════════════════════
    private void wireLiveValidation() {

        liveValidate(titreField, titreError,
                v -> v.length() >= 5 && v.length() <= 100,
                "5 à 100 caractères requis");

        liveValidate(descriptionField, descriptionError,
                v -> v.length() >= 10 && v.length() <= 300,
                "10 à 300 caractères requis");

        liveValidate(localisationField, localisationError,
                v -> v.length() >= 3 && v.length() <= 100 && !v.matches("^\\d.*"),
                "3–100 caractères, ne commence pas par un chiffre");

        liveValidate(capaciteField, capaciteError, v -> {
            try { int c = Integer.parseInt(v); return c >= 1 && c <= 10_000; }
            catch (NumberFormatException ex) { return false; }
        }, "Entier entre 1 et 10 000");

        liveValidate(prixField, prixError,
                v -> v.matches("^\\d+(\\.\\d{1,2})?DT$"),
                "Format : 0DT  ·  50DT  ·  29.99DT");

        if (typeCombo != null) typeCombo.valueProperty().addListener((o, old, n) -> {
            hideError(typeError);
            if (n != null) markComboOk(typeCombo);
        });

        if (dateField != null) dateField.valueProperty().addListener((o, old, n) -> {
            if (n == null) { hideError(dateError); return; }
            if (n.isBefore(LocalDate.now())) {
                showError(dateError, "La date ne peut pas être dans le passé");
                markDpErr(dateField);
            } else if (n.isAfter(LocalDate.now().plusYears(5))) {
                showError(dateError, "Maximum 5 ans dans le futur");
                markDpErr(dateField);
            } else {
                hideError(dateError); markDpOk(dateField);
            }
        });
    }

    // FIX: unified liveValidate using TextInputControl — eliminates the
    //      duplicated liveValidateTF / liveValidateTA pattern
    private void liveValidate(TextInputControl field, Label err,
                              Predicate<String> rule, String msg) {
        if (field == null) return;
        field.textProperty().addListener((o, old, n) -> {
            String v = n.trim();
            if (v.isEmpty()) {
                hideError(err);
                if (field instanceof TextField) ((TextField) field).setStyle(fieldStyle(field.isFocused()));
                return;
            }
            if (rule.test(v)) { hideError(err);    markFieldOk(field); }
            else              { showError(err, msg); markFieldErr(field); }
        });
        field.focusedProperty().addListener((o, old, f) -> {
            if (!f) {
                String v = field.getText().trim();
                if (!v.isEmpty() && !rule.test(v)) showError(err, msg);
            }
        });
    }

    // ── Border helpers ────────────────────────────────────────

    // FIX: unified markFieldOk/Err to handle both TextField and TextArea
    private void markFieldOk(TextInputControl field) {
        if (field instanceof TextField) markTfOk((TextField) field);
    }
    private void markFieldErr(TextInputControl field) {
        if (field instanceof TextField) markTfErr((TextField) field);
    }

    private void markTfOk(TextField tf) {
        if (tf == null) return;
        tf.setStyle(fieldStyle(tf.isFocused())
                .replace(tf.isFocused() ? C_CYAN : "rgba(0,217,255,0.20)", C_GREEN));
    }
    private void markTfErr(TextField tf) {
        if (tf == null) return;
        tf.setStyle(fieldStyle(tf.isFocused())
                .replace(tf.isFocused() ? C_CYAN : "rgba(0,217,255,0.20)", C_DANGER));
    }
    private void markDpOk(DatePicker dp) {
        if (dp == null) return;
        dp.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + C_GREEN + "; -fx-border-width: 2; -fx-border-radius: 12;");
    }
    private void markDpErr(DatePicker dp) {
        if (dp == null) return;
        dp.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + C_DANGER + "; -fx-border-width: 2; -fx-border-radius: 12;");
    }
    private void markComboOk(ComboBox<String> cb) {
        if (cb == null) return;
        cb.setStyle(cb.getStyle().replace("rgba(0,217,255,0.20)", C_GREEN));
    }

    // ── Error label helpers ───────────────────────────────────
    private void showError(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText("⚠  " + msg);
        if (!lbl.isVisible()) {
            lbl.setVisible(true); lbl.setManaged(true);
            TranslateTransition shake = new TranslateTransition(Duration.millis(55), lbl);
            shake.setFromX(0); shake.setToX(7);
            shake.setCycleCount(4); shake.setAutoReverse(true); shake.play();
        }
    }
    private void hideError(Label lbl) {
        if (lbl == null) return;
        lbl.setVisible(false); lbl.setManaged(false);
    }

    // ═══════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════
    private void wireActions() {
        if (btnAjouter     != null) btnAjouter.setOnAction(e -> soumettre());
        if (btnReset       != null) btnReset.setOnAction(e -> resetForm());
        if (btnChooseImage != null) btnChooseImage.setOnAction(e -> choisirImage());
    }

    // ═══════════════════════════════════════════════════════
    //  IMAGE CHOOSER
    // ═══════════════════════════════════════════════════════
    private void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images (PNG, JPG, GIF)",
                        "*.png","*.jpg","*.jpeg","*.gif")
        );
        File chosen = fc.showOpenDialog(btnChooseImage.getScene().getWindow());
        if (chosen == null) return;

        String lc = chosen.getName().toLowerCase();
        if (!lc.endsWith(".png") && !lc.endsWith(".jpg")
                && !lc.endsWith(".jpeg") && !lc.endsWith(".gif")) {
            showError(imageError, "Format non supporté — PNG, JPG ou GIF uniquement");
            return;
        }
        if (chosen.length() > 5L * 1024 * 1024) {
            showError(imageError, "Fichier trop volumineux (max 5 Mo)");
            return;
        }

        try {
            File dest = new File("src/main/resources/images/evenements/");
            dest.mkdirs();
            String fname = System.currentTimeMillis() + "_" + chosen.getName();
            Files.copy(chosen.toPath(), new File(dest, fname).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            selectedImagePath = "images/evenements/" + fname;
            if (imageField != null) imageField.setText(selectedImagePath);
            hideError(imageError);

            if (imagePreview != null) {
                imagePreview.setImage(new Image(chosen.toURI().toString()));
                imagePreview.setOpacity(0);

                // FIX: do not subclass final animation classes — use instances directly
                FadeTransition ft = new FadeTransition(Duration.millis(380), imagePreview);
                ft.setToValue(1);
                ft.play();

                ScaleTransition st = new ScaleTransition(Duration.millis(380), imagePreview);
                st.setFromX(0.88); st.setFromY(0.88);
                st.setToX(1.0);   st.setToY(1.0);
                st.play();
            }
            if (btnChooseImage != null) {
                btnChooseImage.setText("✔  Image sélectionnée");
                glowNode(btnChooseImage, C_GREEN, 0.30);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(imageError, "Impossible de copier l'image");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  SUBMIT — full validation gate
    // ═══════════════════════════════════════════════════════
    private void soumettre() {

        for (Label l : new Label[]{titreError, imageError, descriptionError,
                dateError, localisationError, typeError, capaciteError, prixError})
            hideError(l);

        boolean ok = true;

        // ── Titre ─────────────────────────────────────────────
        String titre = titreField != null ? titreField.getText().trim() : "";
        if (titre.isEmpty()) {
            showError(titreError, "Le titre est obligatoire"); markTfErr(titreField); ok = false;
        } else if (titre.length() < 5) {
            showError(titreError, "Minimum 5 caractères"); markTfErr(titreField); ok = false;
        } else if (titre.length() > 100) {
            showError(titreError, "Maximum 100 caractères"); markTfErr(titreField); ok = false;
        } else if (titre.matches(".*[<>\"';%()&+].*")) {
            showError(titreError, "Caractères spéciaux non autorisés : < > \" ' ; % ( ) & +");
            markTfErr(titreField); ok = false;
        }

        // ── Description ───────────────────────────────────────
        String desc = descriptionField != null ? descriptionField.getText().trim() : "";
        if (desc.isEmpty()) {
            showError(descriptionError, "La description est obligatoire"); ok = false;
        } else if (desc.length() < 10) {
            showError(descriptionError, "Minimum 10 caractères"); ok = false;
        } else if (desc.length() > 300) {
            showError(descriptionError, "Maximum 300 caractères"); ok = false;
        }

        // ── Date ──────────────────────────────────────────────
        LocalDate date = dateField != null ? dateField.getValue() : null;
        if (date == null) {
            showError(dateError, "La date est obligatoire"); ok = false;
        } else if (date.isBefore(LocalDate.now())) {
            showError(dateError, "La date ne peut pas être dans le passé");
            markDpErr(dateField); ok = false;
        } else if (date.isAfter(LocalDate.now().plusYears(5))) {
            showError(dateError, "Maximum 5 ans dans le futur");
            markDpErr(dateField); ok = false;
        }

        // ── Localisation ──────────────────────────────────────
        String loc = localisationField != null ? localisationField.getText().trim() : "";
        if (loc.isEmpty()) {
            showError(localisationError, "La localisation est obligatoire");
            markTfErr(localisationField); ok = false;
        } else if (loc.length() < 3) {
            showError(localisationError, "Minimum 3 caractères");
            markTfErr(localisationField); ok = false;
        } else if (loc.length() > 100) {
            showError(localisationError, "Maximum 100 caractères");
            markTfErr(localisationField); ok = false;
        } else if (loc.matches("^\\d.*")) {
            showError(localisationError, "Ne doit pas commencer par un chiffre");
            markTfErr(localisationField); ok = false;
        }

        // ── Type ──────────────────────────────────────────────
        String type = typeCombo != null ? typeCombo.getValue() : null;
        if (type == null || type.isEmpty()) {
            showError(typeError, "Veuillez sélectionner un type d'événement"); ok = false;
        }

        // ── Capacité ──────────────────────────────────────────
        String capStr = capaciteField != null ? capaciteField.getText().trim() : "";
        int capacite  = 0;
        if (capStr.isEmpty()) {
            showError(capaciteError, "La capacité est obligatoire");
            markTfErr(capaciteField); ok = false;
        } else {
            try {
                capacite = Integer.parseInt(capStr);
                if (capacite < 1) {
                    showError(capaciteError, "La capacité doit être ≥ 1");
                    markTfErr(capaciteField); ok = false;
                } else if (capacite > 10_000) {
                    showError(capaciteError, "Maximum 10 000 places");
                    markTfErr(capaciteField); ok = false;
                }
            } catch (NumberFormatException ex) {
                showError(capaciteError, "Entier valide requis (ex: 100)");
                markTfErr(capaciteField); ok = false;
            }
        }

        // ── Prix ──────────────────────────────────────────────
        String prix = prixField != null ? prixField.getText().trim() : "";
        if (prix.isEmpty()) {
            showError(prixError, "Le prix est obligatoire — ex: 0DT ou 50DT");
            markTfErr(prixField); ok = false;
        } else if (!prix.matches("^\\d+(\\.\\d{1,2})?DT$")) {
            showError(prixError, "Format invalide — ex: 0DT · 50DT · 29.99DT");
            markTfErr(prixField); ok = false;
        } else {
            try {
                double val = Double.parseDouble(prix.replace("DT", ""));
                if (val < 0) {
                    showError(prixError, "Le prix ne peut pas être négatif");
                    markTfErr(prixField); ok = false;
                } else if (val > 99_999) {
                    showError(prixError, "Prix trop élevé (max 99999DT)");
                    markTfErr(prixField); ok = false;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (!ok) { shakeBtn(btnAjouter); return; }

        // ── Persist ───────────────────────────────────────────
        try {
            Evenement ev = new Evenement();
            ev.setTitre_e(titre);
            ev.setDescription_e(desc);
            ev.setDate_e(Timestamp.valueOf(date.atStartOfDay()));
            ev.setLocalisation_e(loc);
            ev.setType_e(type);
            ev.setCapacitemax_e(capacite);
            ev.setStatut_e("En cours");
            ev.setPrix_e(prix);
            ev.setImage(selectedImagePath.isEmpty() ? null : selectedImagePath);

            service.ajouter(ev);
            showSuccess();
            resetForm();
            if (onEventAdded != null) onEventAdded.run();

        } catch (SQLException ex) {
            ex.printStackTrace();
            alert("Erreur base de données",
                    "Impossible d'enregistrer l'événement :\n" + ex.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  RESET FORM
    // ═══════════════════════════════════════════════════════
    private void resetForm() {
        for (TextField tf : new TextField[]{
                titreField, localisationField, capaciteField, prixField
        }) { if (tf != null) { tf.clear(); tf.setStyle(fieldStyle(false)); } }

        if (descriptionField != null) descriptionField.clear();
        if (dateField        != null) { dateField.setValue(null); styleDatePicker(dateField); }
        if (typeCombo        != null) typeCombo.setValue(null);
        if (imageField       != null) imageField.clear();

        if (imagePreview != null) {
            // FIX: do not subclass final FadeTransition — use instance directly
            FadeTransition ft = new FadeTransition(Duration.millis(300), imagePreview);
            ft.setToValue(0);
            ft.setOnFinished(e -> imagePreview.setImage(null));
            ft.play();
        }
        if (btnChooseImage != null) {
            btnChooseImage.setText("🖼  Choisir une image");
            btnChooseImage.setEffect(null);
        }
        selectedImagePath = "";
        for (Label l : new Label[]{titreError, imageError, descriptionError,
                dateError, localisationError, typeError, capaciteError, prixError})
            hideError(l);
    }

    // ═══════════════════════════════════════════════════════
    //  SUCCESS
    // ═══════════════════════════════════════════════════════
    private void showSuccess() {
        if (btnAjouter != null) {
            btnAjouter.setText("✔  Événement créé !");
            btnAjouter.setStyle(
                    "-fx-background-color: linear-gradient(to right, " + C_GREEN + ", #00BFFF); " +
                            "-fx-text-fill: " + C_DARK + "; -fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-padding: 13 28; -fx-background-radius: 28; -fx-cursor: hand;"
            );
            glowNode(btnAjouter, C_GREEN, 0.55);

            // FIX: do not subclass final PauseTransition — use instance directly
            PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
            pause.setOnFinished(e -> {
                btnAjouter.setText("✚  Créer l'événement");
                applyGradientBtn(btnAjouter);
            });
            pause.play();
        }
        alert("Succès ✔", "L'événement a été créé avec succès !", Alert.AlertType.INFORMATION);
    }

    // ═══════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════
    private void shakeBtn(Button btn) {
        if (btn == null) return;
        TranslateTransition t = new TranslateTransition(Duration.millis(55), btn);
        t.setFromX(0); t.setToX(9); t.setCycleCount(6); t.setAutoReverse(true); t.play();
    }

    private void animateEntrance() {
        javafx.scene.Node[] nodes = {
                titreField, descriptionField, dateField,
                localisationField, typeCombo, capaciteField,
                prixField, imageField, btnChooseImage, imagePreview,
                statutLabel, btnAjouter, btnReset
        };

        // FIX: removed anonymous subclassing of FadeTransition (illegal — final class)
        //      and the invalid indexOf(Node[], Node) call.
        //      Use plain for-loop with index-based delay instead.
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null) continue;
            final long delay = 55L * i;
            nodes[i].setOpacity(0);
            nodes[i].setTranslateY(14);

            FadeTransition ft = new FadeTransition(Duration.millis(380), nodes[i]);
            ft.setDelay(Duration.millis(delay));
            ft.setToValue(1);
            ft.play();

            TranslateTransition tt = new TranslateTransition(Duration.millis(380), nodes[i]);
            tt.setDelay(Duration.millis(delay));
            tt.setToY(0);
            tt.play();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ALERT HELPER
    // ═══════════════════════════════════════════════════════
    private void alert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        String border = type == Alert.AlertType.ERROR   ? C_DANGER :
                type == Alert.AlertType.WARNING ? "#FFB347" : C_GREEN;
        a.getDialogPane().setStyle(
                "-fx-background-color: " + C_CARD + "; -fx-border-color: " + border + "; " +
                        "-fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;"
        );
        try {
            a.getDialogPane().lookup(".content.label")
                    .setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px;");
        } catch (Exception ignored) {}
        a.showAndWait();
    }
}