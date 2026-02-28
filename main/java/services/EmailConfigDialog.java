package services;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EmailConfigDialog {

    private String email;
    private String password;
    private boolean confirmed = false;

    public boolean show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Connexion Gmail — NeuroWell");

        // ── Champs ──────────────────────────────────────
        TextField emailField = new TextField();
        emailField.setPromptText("ayoubbenaribia252@gmail.com");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("knzi uiih nhhy vyof");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 12px;");

        // ── Boutons ──────────────────────────────────────
        Button btnConfirm = new Button("✉ Envoyer");
        btnConfirm.setStyle(
                "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88);" +
                        "-fx-text-fill: #050C07; -fx-font-weight: bold;" +
                        "-fx-padding: 10 30; -fx-border-radius: 50; -fx-background-radius: 50;"
        );

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.5);" +
                        "-fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 50;" +
                        "-fx-background-radius: 50; -fx-padding: 10 30;"
        );

        // ── Actions ──────────────────────────────────────
        btnConfirm.setOnAction(e -> {
            String em = emailField.getText().trim();
            String pw = passwordField.getText().trim();

            if (!em.contains("@") || !em.endsWith(".com")) {
                errorLabel.setText("⚠ Adresse Gmail invalide.");
                return;
            }
            if (pw.replace(" ", "").length() != 16) {
                errorLabel.setText("⚠ Le mot de passe d'application doit avoir 16 caractères.");
                return;
            }

            this.email     = em;
            this.password  = pw.replace(" ", ""); // supprimer les espaces
            this.confirmed = true;
            stage.close();
        });

        btnCancel.setOnAction(e -> stage.close());

        // ── Layout ───────────────────────────────────────
        VBox root = new VBox(14,
                new Label("📧 Compte Gmail expéditeur") {{
                    setStyle("-fx-text-fill: #00D9FF; -fx-font-size: 16px; -fx-font-weight: bold;");
                }},
                new Label("Adresse Gmail :") {{ setStyle("-fx-text-fill: rgba(255,255,255,0.7);"); }},
                emailField,
                new Label("Mot de passe d'application :") {{ setStyle("-fx-text-fill: rgba(255,255,255,0.7);"); }},
                passwordField,
                new Label("ℹ Générez-le sur : myaccount.google.com/apppasswords") {{
                    setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 11px;");
                }},
                errorLabel,
                new HBox(12, btnConfirm, btnCancel) {{ setAlignment(Pos.CENTER); }}
        );

        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #0D1F12; -fx-border-color: rgba(0,217,255,0.2); -fx-border-radius: 12;");
        root.setAlignment(Pos.CENTER_LEFT);

        // Styles des champs
        String fieldStyle = "-fx-background-color: rgba(255,255,255,0.07);" +
                "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.3);" +
                "-fx-border-color: rgba(0,217,255,0.3); -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-padding: 8 12;";
        emailField.setStyle(fieldStyle);
        passwordField.setStyle(fieldStyle);
        emailField.setPrefWidth(340);
        passwordField.setPrefWidth(340);

        stage.setScene(new Scene(root, 420, 360));
        stage.setResizable(false);
        stage.showAndWait();

        return confirmed;
    }

    public String getEmail()    { return email; }
    public String getPassword() { return password; }
}