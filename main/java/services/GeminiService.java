package services;

import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.util.List;

public class GeminiService {

    // ================================================================
    //  CONFIGURATION — Clé API Groq
    // ================================================================
    private static final String API_KEY = "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";  // <-- colle ta clé ici
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile"; // modèle Groq gratuit
    // ================================================================

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // ================================================================
    //  APPEL HTTP vers l'API Groq (compatible OpenAI)
    // ================================================================
    private String callGroq(String prompt) {
        try {
            String body = """
                {
                    "model": "%s",
                    "messages": [{"role": "user", "content": %s}],
                    "max_tokens": 500
                }
                """.formatted(MODEL, gson.toJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseGroqResponse(response.body());
            } else {
                System.err.println("Groq API erreur " + response.statusCode() + " : " + response.body());
                return "Analyse indisponible";
            }
        } catch (Exception e) {
            System.err.println("Groq API exception : " + e.getMessage());
            return "Analyse indisponible";
        }
    }

    private String parseGroqResponse(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString()
                    .trim();
        } catch (Exception e) {
            return "Erreur parsing reponse";
        }
    }

    // ================================================================
    //  Les méthodes restent IDENTIQUES, juste remplacer callGemini → callGroq
    // ================================================================

    public String analyserSentimentObjectif(String objectif) {
        String prompt = """
            Analyse le sentiment de cet objectif de participation a un evenement.
            Reponds UNIQUEMENT avec un de ces mots (sans explication) :
            "Tres motive" ou "Motive" ou "Neutre" ou "Peu motive"
            Objectif : """ + objectif;
        return callGroq(prompt).trim();
    }

    public String recommanderEvenements(String objectif, String modeParticipation,
                                        List<String> typesDisponibles) {
        String types = String.join(", ", typesDisponibles);
        String prompt = "Profil - Objectif: " + objectif +
                " | Mode: " + modeParticipation +
                " | Types dispo: " + types +
                "\nRecommande 2-3 types d'evenements en 2-3 phrases. Reponds en francais.";
        return callGroq(prompt);
    }

    public String genererResume(List<String> objectifs, int totalParticipants,
                                int presentiel, int distanciel) {
        String listeObjectifs = String.join("\n- ", objectifs);
        String prompt = "Stats: " + totalParticipants + " participants, " +
                presentiel + " presentiel, " + distanciel + " distanciel.\n" +
                "Objectifs:\n- " + listeObjectifs +
                "\nGenere un resume analytique de 4-5 phrases en francais.";
        return callGroq(prompt);
    }

    public String evaluerRisqueAbandon(String objectif, String mode) {
        String prompt = "Objectif: " + objectif + " | Mode: " + mode +
                "\nReponds UNIQUEMENT avec: 'Faible' ou 'Moyen' ou 'Eleve'";
        return callGroq(prompt).trim();
    }

    public String ameliorerObjectif(String objectif) {
        String prompt = "Objectif: \"" + objectif +
                "\"\nSi vague, propose une version amelioree en 1-2 phrases." +
                " Si bon, reponds 'Objectif clair et precis.' Reponds en francais.";
        return callGroq(prompt);
    }
}