package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service de traduction utilisant MyMemory (API 100% gratuite, sans clé).
 *
 * Limite gratuite : 5000 mots/jour — suffisant pour une app locale.
 *
 * Dépendance Maven :
 * ─────────────────
 *  <dependency>
 *      <groupId>com.google.code.gson</groupId>
 *      <artifactId>gson</artifactId>
 *      <version>2.10.1</version>
 *  </dependency>
 */
public class TraductionService {

    // Langues disponibles
    public enum Langue {
        FRANCAIS("fr"),
        ANGLAIS("en"),
        ARABE("ar"),
        ESPAGNOL("es"),
        ALLEMAND("de"),
        ITALIEN("it");

        public final String code;
        Langue(String code) { this.code = code; }

        @Override
        public String toString() {
            return switch (this) {
                case FRANCAIS  -> "🇫🇷 Français";
                case ANGLAIS   -> "🇬🇧 English";
                case ARABE     -> "🇹🇳 العربية";
                case ESPAGNOL  -> "🇪🇸 Español";
                case ALLEMAND  -> "🇩🇪 Deutsch";
                case ITALIEN   -> "🇮🇹 Italiano";
            };
        }
    }

    /**
     * Traduit un texte via l'API MyMemory (gratuite, sans inscription).
     *
     * @param texte      texte source en français
     * @param cible      langue cible (ex: Langue.ANGLAIS)
     * @return           texte traduit
     * @throws Exception si la requête échoue
     */
    public static String traduire(String texte, Langue cible) throws Exception {
        if (texte == null || texte.isBlank()) return "";

        // Encoder le texte pour l'URL
        String textEncode = java.net.URLEncoder.encode(texte, StandardCharsets.UTF_8);
        String langPair   = "fr|" + cible.code;

        String urlStr = "https://api.mymemory.translated.net/get"
                      + "?q="        + textEncode
                      + "&langpair=" + langPair;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new Exception("Erreur API traduction : HTTP " + status);
        }

        // Lire la réponse
        BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        // Parser le JSON
        // Réponse MyMemory : {"responseData":{"translatedText":"..."},...}
        JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
        String traduit  = json.getAsJsonObject("responseData")
                             .get("translatedText")
                             .getAsString();

        // Fallback si la traduction échoue
        if (traduit == null || traduit.isBlank() || traduit.equals(texte)) {
            return texte + " [traduction indisponible]";
        }

        return traduit;
    }

    /**
     * Traduit un texte avec une langue source et une langue cible spécifiées en String.
     * Permet de traduire depuis n'importe quelle langue (pas seulement le français).
     *
     * @param texte      texte source
     * @param langSource code ISO de la langue source (ex: "fr", "en")
     * @param langCible  code ISO de la langue cible  (ex: "en", "ar")
     * @return           texte traduit
     * @throws Exception si la requête échoue
     */
    public static String traduire(String texte, String langSource, String langCible) throws Exception {
        if (texte == null || texte.isBlank()) return "";

        String textEncode = java.net.URLEncoder.encode(texte, StandardCharsets.UTF_8);
        String langPair   = langSource + "|" + langCible;

        String urlStr = "https://api.mymemory.translated.net/get"
                      + "?q="        + textEncode
                      + "&langpair=" + langPair;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new Exception("Erreur API traduction : HTTP " + status);
        }

        BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
        String traduit  = json.getAsJsonObject("responseData")
                             .get("translatedText")
                             .getAsString();

        if (traduit == null || traduit.isBlank() || traduit.equals(texte)) {
            return texte + " [traduction indisponible]";
        }

        return traduit;
    }

    /**
     * Version asynchrone (JavaFX Thread-safe) pour ne pas bloquer l'interface.
     * Utilisation :
     *   TraductionService.traduireAsync("Bonjour", Langue.ANGLAIS,
     *       resultat -> monLabel.setText(resultat),
     *       erreur   -> System.err.println(erreur));
     */
    public static void traduireAsync(String texte, Langue cible,
                                     java.util.function.Consumer<String> onSuccess,
                                     java.util.function.Consumer<String> onError) {
        Thread thread = new Thread(() -> {
            try {
                String resultat = traduire(texte, cible);
                javafx.application.Platform.runLater(() -> onSuccess.accept(resultat));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
