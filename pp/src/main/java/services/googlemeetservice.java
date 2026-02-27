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
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

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
 * Service pour créer des réunions Google Meet via la Google Calendar API (REST).
 *
 * PRÉREQUIS :
 * 1. Aller sur https://console.cloud.google.com
 * 2. Créer un projet → Activer "Google Calendar API"
 * 3. Créer des identifiants OAuth 2.0 (type "Application de bureau")
 * 4. Télécharger le fichier JSON → le renommer "credentials.json"
 * 5. Le placer dans :  src/main/resources/google/credentials.json
 *
 * DÉPENDANCES Maven à ajouter dans pom.xml :
 * <dependency>
 *   <groupId>com.google.api-client</groupId>
 *   <artifactId>google-api-client</artifactId>
 *   <version>2.2.0</version>
 * </dependency>
 * <dependency>
 *   <groupId>com.google.oauth-client</groupId>
 *   <artifactId>google-oauth-client-jetty</artifactId>
 *   <version>1.34.1</version>
 * </dependency>
 * <dependency>
 *   <groupId>com.google.apis</groupId>
 *   <artifactId>google-api-services-calendar</artifactId>
 *   <version>v3-rev20230707-2.0.0</version>
 * </dependency>
 */
public class googlemeetservice {

    private static final String APPLICATION_NAME = "NeuroWell Meet Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // Dossier où les tokens OAuth sont stockés après la première connexion
    private static final String TOKENS_DIRECTORY = "tokens/google";

    // Scope requis : gestion complète du calendrier (pour créer des événements avec Meet)
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    // Chemin vers votre fichier credentials.json (téléchargé depuis Google Cloud Console)
    private static final String CREDENTIALS_FILE =
            "src/main/resources/google/credentials.json";

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Charge les credentials OAuth2 et ouvre le navigateur pour l'autorisation
     * si c'est la première fois (le token est ensuite sauvegardé localement).
     */
    private Credential getCredentials(final NetHttpTransport transport) throws IOException {
        File credFile = new File(CREDENTIALS_FILE);
        if (!credFile.exists()) {
            throw new IOException(
                    "Fichier credentials.json introuvable : " + credFile.getAbsolutePath() +
                            "\nTéléchargez-le depuis Google Cloud Console et placez-le dans src/main/resources/google/");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(new FileInputStream(credFile)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY)))
                .setAccessType("offline")
                .build();

        // Ouvre le navigateur pour la connexion Google (1ère fois seulement)
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Crée une réunion Google Meet liée à un événement du calendrier Google.
     *
     * @param titreEvenement  Nom de l'événement (affiché dans le calendrier)
     * @param dateEvenement   Date/heure de début de la réunion
     * @return                Le lien Google Meet généré (ex: https://meet.google.com/abc-defg-hij)
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public String creerReunionMeet(String titreEvenement, Timestamp dateEvenement)
            throws IOException, GeneralSecurityException {

        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        // Construction du client Calendar API authentifié
        Calendar service = new Calendar.Builder(transport, JSON_FACTORY, getCredentials(transport))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Calcul de l'heure de fin : début + 1 heure
        long startMillis = dateEvenement != null
                ? dateEvenement.getTime()
                : System.currentTimeMillis();
        long endMillis = startMillis + (60 * 60 * 1000); // +1h

        // Création de l'événement Google Calendar avec conférence Meet intégrée
        Event event = new Event()
                .setSummary("📹 " + titreEvenement + " - Session en ligne")
                .setDescription("Réunion Google Meet générée automatiquement par NeuroWell.");

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(startMillis))
                .setTimeZone("Africa/Tunis"));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(endMillis))
                .setTimeZone("Africa/Tunis"));

        // ⬇️ C'est ici que Google Meet est demandé via l'API
        event.setConferenceData(new ConferenceData()
                .setCreateRequest(new CreateConferenceRequest()
                        .setRequestId(UUID.randomUUID().toString()) // ID unique par réunion
                        .setConferenceSolutionKey(
                                new ConferenceSolutionKey().setType("hangoutsMeet"))));

        // Insertion dans le calendrier "primary" avec conferenceDataVersion=1
        // (obligatoire pour que Meet soit créé)
        Event createdEvent = service.events()
                .insert("primary", event)
                .setConferenceDataVersion(1)
                .execute();

        // Extraction du lien Meet depuis la réponse
        if (createdEvent.getConferenceData() != null
                && createdEvent.getConferenceData().getEntryPoints() != null) {

            return createdEvent.getConferenceData().getEntryPoints()
                    .stream()
                    .filter(ep -> "video".equals(ep.getEntryPointType()))
                    .map(ep -> ep.getUri())
                    .findFirst()
                    .orElseThrow(() -> new IOException("Lien Meet non trouvé dans la réponse API"));
        }

        throw new IOException("Impossible de créer la réunion Meet via l'API Google Calendar.");
    }
}