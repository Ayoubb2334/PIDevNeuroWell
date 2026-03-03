package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class SmsService {

    // ✅ Récupère les secrets depuis l'environnement
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN  = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER = System.getenv("TWILIO_FROM_NUMBER");

    private static final boolean USE_TEST_MODE = false;

    private static final String API_URL =
            "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean envoyerConfirmationConsultation(String toPhone, String patientNom,
                                                   String psyNom, LocalDateTime dateConsultation,
                                                   String typeConsultation, double prix) {
        if (toPhone == null || toPhone.trim().isEmpty()) {
            System.out.println("[SMS] Numéro manquant — non envoyé.");
            return false;
        }
        String dateStr = dateConsultation != null
                ? dateConsultation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                : "Date à confirmer";

        String message = String.format(
                "NeuroWell ✅ Bonjour %s,\n" +
                        "Consultation confirmée avec Dr. %s.\n" +
                        "📅 %s | 📍 %s\n" +
                        "💰 %.2f TND\n" +
                        "contact@neurowell.tn",
                patientNom, psyNom, dateStr, typeConsultation, prix);

        return sendSms(toPhone, message);
    }

    public boolean envoyerRappelConsultation(String toPhone, String patientNom,
                                             String psyNom, LocalDateTime dateConsultation) {
        if (toPhone == null || toPhone.trim().isEmpty()) return false;
        String dateStr = dateConsultation != null
                ? dateConsultation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "";
        String message = String.format(
                "NeuroWell 🔔 Rappel !\n" +
                        "Bonjour %s, votre séance avec Dr. %s est demain : %s.\n" +
                        "En cas d'empêchement, annulez via l'appli.",
                patientNom, psyNom, dateStr);
        return sendSms(toPhone, message);
    }

    public boolean envoyerAnnulation(String toPhone, String patientNom, String psyNom) {
        if (toPhone == null || toPhone.trim().isEmpty()) return false;
        String message = String.format(
                "NeuroWell ❌ Consultation annulée.\n" +
                        "Bonjour %s, votre consultation avec Dr. %s a été annulée.\n" +
                        "Reprenez RDV sur l'application.",
                patientNom, psyNom);
        return sendSms(toPhone, message);
    }

    private boolean sendSms(String toPhone, String message) {
        if (USE_TEST_MODE) {
            System.out.println("════════════════════════════════");
            System.out.println("[SMS TEST] To: " + toPhone);
            System.out.println("[SMS TEST] " + message);
            System.out.println("════════════════════════════════");
            return true;
        }

        try {
            String normalizedPhone = normalizePhone(toPhone);
            String body = "To="   + URLEncoder.encode(normalizedPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(FROM_NUMBER, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String auth = Base64.getEncoder().encodeToString(
                    (ACCOUNT_SID + ":" + AUTH_TOKEN).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + auth)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("[SMS] ✅ Envoyé à " + normalizedPhone);
                return true;
            } else {
                System.err.println("[SMS] ❌ Twilio " + response.statusCode() + " : " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("[SMS] Exception : " + e.getMessage());
            return false;
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return phone;
        phone = phone.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("+"))  return phone;
        if (phone.startsWith("00")) return "+" + phone.substring(2);
        if (phone.length() == 8)    return "+216" + phone;
        return "+" + phone;
    }
}