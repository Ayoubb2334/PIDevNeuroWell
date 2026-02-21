package controllers;

import entities.Evenement;
import entities.Participation;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import services.googlemeetservice;
import services.ServiceEvenement;
import services.ServiceParticipation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ParticipationController {

    private static final String PRIMARY_COLOR   = "#00D9FF";
    private static final String SECONDARY_COLOR = "#00FF88";
    private static final String DARK_BG         = "#050C07";
    private static final String CARD_BG         = "#0D1F12";

    @FXML private Circle    orb1, orb2, orb3;
    @FXML private VBox      participationContainer;
    @FXML private VBox      mainAnchorPane;
    @FXML private TextField     searchField;
    @FXML private RadioButton   filterPresentiel;
    @FXML private RadioButton   filterEnLigne;
    @FXML private RadioButton   filterTous;
    @FXML private Label         totalParticipations;
    @FXML private Label         resultsCount;
    @FXML private ToggleGroup   modeGroup;

    private final ServiceParticipation serviceParticipation = new ServiceParticipation();
    private final ServiceEvenement     serviceEvenement     = new ServiceEvenement();
    private final googlemeetservice    googleMeetService    = new googlemeetservice();

    @FXML
    public void initialize() {
        if (modeGroup == null) modeGroup = new ToggleGroup();
        if (filterPresentiel != null) filterPresentiel.setToggleGroup(modeGroup);
        if (filterEnLigne    != null) filterEnLigne.setToggleGroup(modeGroup);
        if (filterTous       != null) { filterTous.setToggleGroup(modeGroup); filterTous.setSelected(true); }

        initializeAnimations();
        loadParticipations();
        setupFilterListeners();
    }

    private void initializeAnimations() {
        if (orb1 != null) animateOrb(orb1,  30, -20, Duration.seconds(15));
        if (orb2 != null) animateOrb(orb2, -35,  25, Duration.seconds(18));
        if (orb3 != null) animateOrb(orb3,  20, -15, Duration.seconds(20));
    }

    private void animateOrb(Circle orb, double dx, double dy, Duration dur) {
        TranslateTransition tt = new TranslateTransition(dur, orb);
        tt.setByX(dx);
        tt.setByY(dy);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void setupFilterListeners() {
        if (filterPresentiel != null) filterPresentiel.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (filterEnLigne    != null) filterEnLigne.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (filterTous       != null) filterTous.selectedProperty().addListener((o, v, n) -> applyFilters());
        if (searchField      != null) searchField.textProperty().addListener((o, v, n) -> applyFilters());
    }

    @FXML
    private void handleBackToFront() {
        navigateTo("/views/dashboard.fxml", "NeuroWell - Accueil");
    }

    @FXML
    private void handleEvenement() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/showEvent.fxml"));
            mainAnchorPane.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) participationContainer.getScene().getWindow();
            FadeTransition ft = new FadeTransition(Duration.millis(300),
                    participationContainer.getScene().getRoot());
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                stage.setTitle(title);
                stage.getScene().setRoot(root);
                FadeTransition fi = new FadeTransition(Duration.millis(300), root);
                fi.setFromValue(0);
                fi.setToValue(1);
                fi.play();
            });
            ft.play();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page", Alert.AlertType.ERROR);
        }
    }

    private void loadParticipations() {
        participationContainer.getChildren().clear();
        try {
            List<Participation> list = serviceParticipation.recuperer();
            if (list.isEmpty()) {
                showEmptyState("Aucune participation disponible.");
                updateStats(0, 0);
                return;
            }
            if (totalParticipations != null) totalParticipations.setText(String.valueOf(list.size()));
            if (resultsCount != null)
                resultsCount.setText(list.size() + " participation" + (list.size() > 1 ? "s" : ""));

            for (int i = 0; i < list.size(); i++) {
                VBox card = createCard(list.get(i));
                participationContainer.getChildren().add(card);
                animateCard(card, i);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les participations", Alert.AlertType.ERROR);
        }
    }

    private VBox createCard(Participation p) throws SQLException {
        Evenement ev = serviceEvenement.recuperer().stream()
                .filter(e -> e.getId_e() == p.getIdEvenement())
                .findFirst().orElse(null);

        String titre = ev != null ? ev.getTitre_e()        : "Evenement supprime";
        String type  = ev != null ? ev.getType_e()         : "-";
        String prix  = ev != null ? ev.getPrix_e()         : "-";
        String lieu  = ev != null ? ev.getLocalisation_e() : "-";

        VBox card = new VBox(0);
        card.setStyle(cardStyle(false));
        DropShadow shadow = cardShadow(false);
        card.setEffect(shadow);

        card.setOnMouseEntered(e -> {
            card.setStyle(cardStyle(true));
            TranslateTransition ttIn = new TranslateTransition(Duration.millis(200), card);
            ttIn.setToY(-8);
            ttIn.play();
            card.setEffect(cardShadow(true));
        });
        card.setOnMouseExited(e -> {
            card.setStyle(cardStyle(false));
            TranslateTransition ttOut = new TranslateTransition(Duration.millis(200), card);
            ttOut.setToY(0);
            ttOut.play();
            card.setEffect(shadow);
        });

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20));

        VBox info = new VBox(10);
        info.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblTitre = new Label("🎫  " + titre);
        lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(460);

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
                infoBadge("🎯", type),
                infoBadge("📍", lieu),
                infoBadge("💰", prix)
        );

        Label modeBadge = modeBadge(p.getModeparticipation());

        Label lblObj = new Label("Objectif : " + p.getObjectif());
        lblObj.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(248,249,250,0.65);");
        lblObj.setWrapText(true);
        lblObj.setMaxWidth(460);

        info.getChildren().addAll(lblTitre, badges, modeBadge, lblObj);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8));

        Button btnEdit   = actionBtn("✎  Modifier",  PRIMARY_COLOR);
        Button btnDelete = actionBtn("✖  Supprimer", "#FF6B6B");
        btnEdit.setOnAction(e -> openEditDialog(p));
        btnDelete.setOnAction(e -> handleDelete(p));
        actions.getChildren().addAll(btnEdit, btnDelete);

        if ("distanciel".equalsIgnoreCase(p.getModeparticipation())) {
            final java.sql.Timestamp dateFinal  = ev != null ? ev.getDate_e() : null;
            final String             titreFinal = titre;

            Button btnMeet = meetBtn();
            btnMeet.setOnAction(e -> ouvrirSalleMeet(titreFinal, dateFinal, p));
            actions.getChildren().add(btnMeet);
        }

        row.getChildren().addAll(info, actions);
        card.getChildren().add(row);
        return card;
    }

    private void ouvrirSalleMeet(String titre, java.sql.Timestamp date, Participation p) {
        Stage meetStage = new Stage();
        meetStage.initModality(Modality.APPLICATION_MODAL);
        meetStage.initStyle(StageStyle.TRANSPARENT);
        meetStage.setTitle("NeuroWell Meet — " + titre);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MeetRoom.fxml"));
            Parent root = loader.load();
            MeetRoomController ctrl = loader.getController();

            ctrl.initMeet(titre, null, date, p.getModeparticipation());

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            meetStage.setScene(scene);

            root.setOpacity(0);
            meetStage.show();
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setToValue(1);
            ft.play();

            new Thread(() -> {
                try {
                    String meetUrl = googleMeetService.creerReunionMeet(titre, date);
                    javafx.application.Platform.runLater(() ->
                            ctrl.initMeet(titre, meetUrl, date, p.getModeparticipation())
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        meetStage.close();
                        showAlert("Erreur Google Meet",
                                "Impossible de creer la reunion :\n" + ex.getMessage(),
                                Alert.AlertType.ERROR);
                    });
                }
            }, "MeetGen-Thread").start();

        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface Meet", Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleSearch() { applyFilters(); }

    @FXML private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (filterTous  != null) filterTous.setSelected(true);
        loadParticipations();
    }

    private void applyFilters() {
        participationContainer.getChildren().clear();
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        String mode   = filterPresentiel != null && filterPresentiel.isSelected() ? "presentiel"
                : filterEnLigne != null && filterEnLigne.isSelected() ? "distanciel" : "";
        try {
            List<Participation> list = serviceParticipation.recuperer();
            int count = 0;
            for (Participation p : list) {
                Evenement ev = serviceEvenement.recuperer().stream()
                        .filter(e -> e.getId_e() == p.getIdEvenement())
                        .findFirst().orElse(null);

                String titre    = ev != null ? ev.getTitre_e() : "";
                boolean okSearch = search.isEmpty()
                        || p.getObjectif().toLowerCase().contains(search)
                        || titre.toLowerCase().contains(search);
                boolean okMode   = mode.isEmpty()
                        || p.getModeparticipation().equalsIgnoreCase(mode);

                if (okSearch && okMode) {
                    VBox card = createCard(p);
                    participationContainer.getChildren().add(card);
                    animateCard(card, count++);
                }
            }
            updateStats(list.size(), count);
            if (count == 0) showEmptyState("Aucune participation ne correspond a vos criteres.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void handleDelete(Participation p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer la participation");
        confirm.setContentText("Cette action est irreversible. Confirmer ?");
        styleDialogPane(confirm.getDialogPane());
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    serviceParticipation.supprimer(p);
                    loadParticipations();
                    showAlert("Succes", "Participation supprimee", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void openEditDialog(Participation p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la participation");
        DialogPane pane = dialog.getDialogPane();
        styleDialogPane(pane);

        ButtonType save   = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler",     ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        VBox form = new VBox(18);
        form.setPadding(new Insets(28));

        Label titleLbl = new Label("Modifier la participation");
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");

        Label lblObj = new Label("Objectif");
        lblObj.setStyle("-fx-text-fill: rgba(200,255,220,0.70); -fx-font-size: 13px; -fx-font-weight: 600;");

        TextArea taObj = new TextArea(p.getObjectif());
        taObj.setWrapText(true);
        taObj.setPrefRowCount(4);
        taObj.setStyle(
                "-fx-control-inner-background: #112016; -fx-background-color: #112016; " +
                        "-fx-text-fill: #E8FFF0; -fx-font-size: 13px; -fx-padding: 10; " +
                        "-fx-background-radius: 12; -fx-border-color: rgba(0,217,255,0.25); " +
                        "-fx-border-width: 1.5; -fx-border-radius: 12;"
        );

        Label lblMode = new Label("Mode");
        lblMode.setStyle("-fx-text-fill: rgba(200,255,220,0.70); -fx-font-size: 13px; -fx-font-weight: 600;");

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbDist = new RadioButton("En ligne (distanciel)");
        RadioButton rbPres = new RadioButton("Presentiel");
        rbDist.setToggleGroup(tg);
        rbPres.setToggleGroup(tg);
        rbDist.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;");
        rbPres.setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;");
        if (p.getModeparticipation().equalsIgnoreCase("presentiel")) {
            rbPres.setSelected(true);
        } else {
            rbDist.setSelected(true);
        }

        HBox modeRow = new HBox(24, rbDist, rbPres);
        form.getChildren().addAll(titleLbl, lblObj, taObj, lblMode, modeRow);
        pane.setContent(form);

        pane.lookupButton(save).setStyle(
                "-fx-background-color: linear-gradient(to right," + PRIMARY_COLOR + "," + SECONDARY_COLOR + "); " +
                        "-fx-text-fill: " + DARK_BG + "; -fx-font-weight: bold; " +
                        "-fx-padding: 10 24; -fx-background-radius: 20; -fx-cursor: hand;"
        );
        pane.lookupButton(cancel).setStyle(
                "-fx-background-color: transparent; -fx-border-color: #FF4D6D; " +
                        "-fx-border-width: 1.5; -fx-border-radius: 20; -fx-background-radius: 20; " +
                        "-fx-text-fill: #FF4D6D; -fx-font-weight: bold; -fx-padding: 10 24; -fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(r -> {
            if (r == save) {
                String obj  = taObj.getText().trim();
                String mode = rbDist.isSelected() ? "distanciel" : "presentiel";
                if (obj.length() < 10) {
                    showAlert("Validation", "L'objectif doit contenir au moins 10 caracteres.", Alert.AlertType.WARNING);
                    return;
                }
                p.setObjectif(obj);
                p.setModeparticipation(mode);
                try {
                    serviceParticipation.modifier(p);
                    loadParticipations();
                    showAlert("Succes", "Participation modifiee", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible de modifier.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private String cardStyle(boolean hover) {
        return "-fx-background-color: linear-gradient(to bottom right," +
                (hover ? "rgba(19,36,24,0.85),rgba(19,36,24,0.55)"
                        : "rgba(19,36,24,0.60),rgba(19,36,24,0.30)") + "); " +
                "-fx-background-radius: 20; " +
                "-fx-border-color: " + (hover ? PRIMARY_COLOR : "rgba(0,217,255,0.10)") + "; " +
                "-fx-border-width: 1; -fx-border-radius: 20; -fx-padding: 5;";
    }

    private DropShadow cardShadow(boolean hover) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(PRIMARY_COLOR, hover ? 0.40 : 0.20));
        ds.setRadius(hover ? 25 : 15);
        ds.setSpread(hover ? 0.30 : 0.20);
        return ds;
    }

    private Label infoBadge(String emoji, String text) {
        Label l = new Label(emoji + " " + text);
        l.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: rgba(248,249,250,0.65); " +
                        "-fx-background-color: rgba(0,217,255,0.08); -fx-padding: 5 12; " +
                        "-fx-background-radius: 14; -fx-border-color: rgba(0,217,255,0.25); " +
                        "-fx-border-width: 1; -fx-border-radius: 14;"
        );
        return l;
    }

    private Label modeBadge(String mode) {
        boolean pre = mode.equalsIgnoreCase("presentiel");
        Label l = new Label(pre ? "Presentiel" : "En ligne");
        String c = pre ? SECONDARY_COLOR : PRIMARY_COLOR;
        l.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + c + "; " +
                        "-fx-background-color: rgba(0,217,255,0.08); -fx-padding: 6 16; " +
                        "-fx-background-radius: 18; -fx-border-color: " + c + "; " +
                        "-fx-border-width: 1; -fx-border-radius: 18;"
        );
        return l;
    }

    private Button actionBtn(String text, String color) {
        Button btn = new Button(text);
        String base =
                "-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 9 22; " +
                        "-fx-background-radius: 14; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.05); st.setToY(1.05); st.play();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        return btn;
    }

    private Button meetBtn() {
        Button btn = new Button("Rejoindre Meet");
        String base =
                "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88); " +
                        "-fx-text-fill: #050C07; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 9 18; -fx-background-radius: 14; -fx-cursor: hand;";
        btn.setStyle(base);
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00D9FF", 0.40));
        glow.setRadius(16);
        btn.setEffect(glow);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(base.replace("#00D9FF, #00FF88", "#00FF88, #00D9FF"));
            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.05); st.setToY(1.05); st.play();
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(base);
            ScaleTransition st = new ScaleTransition(Duration.millis(130), btn);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        return btn;
    }

    private void animateCard(VBox card, int idx) {
        card.setOpacity(0);
        card.setTranslateY(28);

        FadeTransition ft = new FadeTransition(Duration.millis(480), card);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(idx * 75L));

        TranslateTransition tt = new TranslateTransition(Duration.millis(480), card);
        tt.setToY(0);
        tt.setDelay(Duration.millis(idx * 75L));

        new ParallelTransition(ft, tt).play();
    }

    private void showEmptyState(String msg) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 64px;");
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(200,255,220,0.55);");
        box.getChildren().addAll(icon, lbl);
        participationContainer.getChildren().add(box);
    }

    private void updateStats(int total, int filtered) {
        if (totalParticipations != null) totalParticipations.setText(String.valueOf(total));
        if (resultsCount != null)
            resultsCount.setText(filtered + " participation" + (filtered > 1 ? "s" : ""));
    }

    private void styleDialogPane(DialogPane pane) {
        pane.setStyle(
                "-fx-background-color: " + CARD_BG + "; " +
                        "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-width: 2; " +
                        "-fx-border-radius: 18; -fx-background-radius: 18;"
        );
        try {
            pane.lookup(".content.label")
                    .setStyle("-fx-text-fill: #E8FFF0; -fx-font-size: 13px;");
        } catch (Exception ignored) {}
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        styleDialogPane(a.getDialogPane());
        a.showAndWait();
    }
}