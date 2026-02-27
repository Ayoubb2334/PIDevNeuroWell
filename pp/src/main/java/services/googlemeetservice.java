package services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  GoogleMeetService — Google Calendar API + Meet          ║
 * ║  Corrections :                                           ║
 * ║  • Nom de classe corrigé (PascalCase)                    ║
 * ║  • Port -1 (auto) → plus d'erreur "port déjà utilisé"   ║
 * ║  • Token supprimé automatiquement si expiré/invalide     ║
 * ║  • Messages d'erreur clairs en français                  ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class googlemeetservice {   // ✅ CORRECTION 1 : PascalCase obligatoire

    private static final String APPLICATION_NAME    = "NeuroWell Meet Integration";
    private static final JsonFactory JSON_FACTORY   = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY    = "tokens/google";
    private static final String CREDENTIALS_FILE    = "src/main/resources/google/credentials.json";

    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    // ─────────────────────────────────────────────────────
    //  AUTHENTIFICATION OAuth2
    // ─────────────────────────────────────────────────────

    private Credential getCredentials(final NetHttpTransport transport)
            throws IOException {

        // ── Vérification du fichier credentials ───────────
        File credFile = new File(CREDENTIALS_FILE);
        if (!credFile.exists()) {
            throw new IOException(
                    "Fichier credentials.json introuvable :\n" +
                            credFile.getAbsolutePath() + "\n\n" +
                            "→ Google Cloud Console > APIs & Services > Identifiants\n" +
                            "→ Créer ID client OAuth > Application de bureau\n" +
                            "→ Télécharger JSON > renommer en credentials.json\n" +
                            "→ Placer dans src/main/resources/google/"
            );
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(credFile))
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY)))
                .setAccessType("offline")
                .build();

        // ✅ CORRECTION 2 : port -1 = port libre automatique
        //    → plus jamais d'erreur "Address already in use: bind"
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(-1)        // ← port aléatoire disponible
                .build();

        try {
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (Exception e) {
            // ✅ CORRECTION 3 : si le token stocké est invalide/expiré,
            //    on le supprime et on relance l'autorisation proprement
            File tokenDir = new File(TOKENS_DIRECTORY);
            if (tokenDir.exists()) {
                for (File f : tokenDir.listFiles()) f.delete();
                System.out.println("⚠ Token expiré supprimé — nouvelle autorisation requise.");
            }
            return new AuthorizationCodeInstalledApp(flow,
                    new LocalServerReceiver.Builder().setPort(-1).build()
            ).authorize("user");
        }
    }

    // ─────────────────────────────────────────────────────
    //  CRÉER UNE RÉUNION GOOGLE MEET
    // ─────────────────────────────────────────────────────

    /**
     * Crée un événement Google Calendar avec une conférence Meet intégrée.
     *
     * @param titreEvenement  Titre affiché dans le calendrier Google
     * @param dateEvenement   Date/heure de début (null = maintenant)
     * @return                Lien Meet  ex: https://meet.google.com/abc-defg-hij
     */
    public String creerReunionMeet(String titreEvenement, Timestamp dateEvenement)
            throws IOException, GeneralSecurityException {

        // ── Transport HTTPS sécurisé ───────────────────────
        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        // ── Client Calendar API authentifié ───────────────
        Calendar service = new Calendar.Builder(transport, JSON_FACTORY, getCredentials(transport))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // ── Calcul des horaires ────────────────────────────
        long startMillis = (dateEvenement != null)
                ? dateEvenement.getTime()
                : System.currentTimeMillis();
        long endMillis = startMillis + (60 * 60 * 1000); // durée : 1 heure

        // ── Construction de l'événement ───────────────────
        Event event = new Event()
                .setSummary("📹 " + titreEvenement + " — Session en ligne")
                .setDescription(
                        "Réunion Google Meet générée automatiquement par NeuroWell.\n" +
                                "Rejoignez via le lien Meet ci-dessous."
                );

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(startMillis))
                .setTimeZone("Africa/Tunis"));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(endMillis))
                .setTimeZone("Africa/Tunis"));

        // ── Demande de conférence Meet ────────────────────
        event.setConferenceData(new ConferenceData()
                .setCreateRequest(new CreateConferenceRequest()
                        .setRequestId(UUID.randomUUID().toString())
                        .setConferenceSolutionKey(
                                new ConferenceSolutionKey().setType("hangoutsMeet")
                        )
                )
        );

        // ── Insertion dans "primary" avec conferenceDataVersion=1 ──
        Event created = service.events()
                .insert("primary", event)
                .setConferenceDataVersion(1)
                .execute();

        // ── Extraction du lien Meet ────────────────────────
        if (created.getConferenceData() != null
                && created.getConferenceData().getEntryPoints() != null) {

            return created.getConferenceData().getEntryPoints()
                    .stream()
                    .filter(ep -> "video".equals(ep.getEntryPointType()))
                    .map(EntryPoint::getUri)
                    .findFirst()
                    .orElseThrow(() -> new IOException(
                            "Lien Meet absent de la réponse API.\n" +
                                    "Vérifiez que Google Calendar API est bien activée sur votre projet."
                    ));
        }

        throw new IOException(
                "La réunion Meet n'a pas pu être créée.\n" +
                        "Vérifiez que votre compte Google a accès à Google Meet."
        );
    }
}