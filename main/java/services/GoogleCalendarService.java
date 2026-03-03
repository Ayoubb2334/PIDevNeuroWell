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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import entities.Consultation;

import java.io.*;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME    = "NeuroWell";
    private static final JsonFactory JSON_FACTORY   = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY    = "tokens";
    private static final String CREDENTIALS_FILE    = "/credentials.json";
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    // ── Service réutilisé (évite de se reconnecter à chaque appel) ──
    private static Calendar cachedService = null;

    // ════════════════════════════════════════════════════════
    //  ✅ FIX 1 : Trouver un port LIBRE automatiquement
    //
    //  CAUSE DE L'ERREUR :
    //    Le port 8888 était codé en dur.
    //    Si une autre instance de l'app tourne, ce port est déjà pris.
    //    → "Address already in use: bind"
    //
    //  SOLUTION :
    //    On demande au système de trouver un port libre lui-même.
    //    Un port = 0 → Java choisit automatiquement un port disponible.
    // ════════════════════════════════════════════════════════
    private int trouverPortLibre() {
        // Essayer d'abord les ports favoris dans l'ordre
        int[] candidats = {8888, 9999, 10000, 10001, 10002};
        for (int port : candidats) {
            try (ServerSocket ss = new ServerSocket(port)) {
                ss.setReuseAddress(true);
                return port; // Ce port est libre
            } catch (IOException ignored) {
                // Port occupé, essayer le suivant
            }
        }
        // Aucun port favori disponible → laisser le système choisir
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        } catch (IOException e) {
            return 8888; // fallback (ne devrait pas arriver)
        }
    }

    // ════════════════════════════════════════════════════════
    //  AUTHENTIFICATION OAUTH2
    // ════════════════════════════════════════════════════════
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE);
        if (in == null) {
            throw new FileNotFoundException(
                    "credentials.json introuvable !\n" +
                            "→ Placez-le dans : src/main/resources/credentials.json"
            );
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // ✅ FIX 2 : setAccessType("offline") + approval_prompt="force"
        // → Génère un refresh_token permanent qui évite de se reconnecter à chaque lancement
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY)))
                .setAccessType("offline")
                .build();

        // ✅ FIX 1 : Port dynamique au lieu du port fixe 8888
        int portLibre = trouverPortLibre();
        System.out.println("[GoogleCalendar] Utilisation du port OAuth2 : " + portLibre);

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(portLibre)
                .build();

        // ✅ FIX 3 : Si un token existe déjà en cache → pas de navigateur
        // Le token est stocké dans le dossier "tokens/" après la 1ère connexion.
        // Les lancements suivants utilisent ce token directement.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    // ════════════════════════════════════════════════════════
    //  ✅ FIX 3 : Service mis en CACHE (singleton)
    //
    //  AVANT : Un nouveau service était créé à chaque appel → nouveau port → conflit
    //  APRÈS : Le service est créé une seule fois et réutilisé
    // ════════════════════════════════════════════════════════
    private Calendar getCalendarService() throws GeneralSecurityException, IOException {
        if (cachedService == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            cachedService = new Calendar.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            System.out.println("[GoogleCalendar] ✅ Service Google Calendar initialisé.");
        }
        return cachedService;
    }

    // ════════════════════════════════════════════════════════
    //  SYNCHRONISER UNE CONSULTATION
    // ════════════════════════════════════════════════════════
    public String syncConsultation(Consultation consultation) throws Exception {
        Calendar service = getCalendarService();

        LocalDateTime dateDebut = consultation.getDateConsultation();
        LocalDateTime dateFin   = dateDebut.plusHours(1);
        String zoneId = "Africa/Tunis";

        DateTime start = new DateTime(dateDebut
                .atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli());
        DateTime end = new DateTime(dateFin
                .atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli());

        Event event = new Event()
                .setSummary("🧠 Consultation NeuroWell")
                .setDescription(buildDescription(consultation))
                .setLocation(consultation.getType().equals("presentiel")
                        ? "Cabinet NeuroWell, Tunis"
                        : "Consultation en ligne — Lien envoyé par email");

        event.setStart(new EventDateTime().setDateTime(start).setTimeZone(zoneId));
        event.setEnd(new EventDateTime().setDateTime(end).setTimeZone(zoneId));

        if (consultation.getPatient() != null
                && consultation.getPatient().getEmail() != null) {
            EventAttendee attendee = new EventAttendee()
                    .setEmail(consultation.getPatient().getEmail())
                    .setDisplayName(consultation.getPatientFullName());
            event.setAttendees(List.of(attendee));
        }

        event.setColorId(consultation.getType().equals("presentiel") ? "2" : "1");

        Event created = service.events().insert("primary", event).execute();
        System.out.println("✅ Google Calendar — événement créé : " + created.getHtmlLink());
        return created.getId();
    }

    // ════════════════════════════════════════════════════════
    //  SUPPRIMER UN ÉVÉNEMENT
    // ════════════════════════════════════════════════════════
    public void deleteConsultation(String googleEventId) throws Exception {
        getCalendarService().events().delete("primary", googleEventId).execute();
        System.out.println("🗑️ Événement supprimé : " + googleEventId);
    }

    // ════════════════════════════════════════════════════════
    //  METTRE À JOUR UN ÉVÉNEMENT
    // ════════════════════════════════════════════════════════
    public void updateConsultation(String googleEventId, Consultation consultation) throws Exception {
        Calendar service = getCalendarService();

        Event event = service.events().get("primary", googleEventId).execute();

        LocalDateTime dateDebut = consultation.getDateConsultation();
        LocalDateTime dateFin   = dateDebut.plusHours(1);
        String zoneId = "Africa/Tunis";

        DateTime start = new DateTime(dateDebut
                .atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli());
        DateTime end = new DateTime(dateFin
                .atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli());

        event.setStart(new EventDateTime().setDateTime(start).setTimeZone(zoneId));
        event.setEnd(new EventDateTime().setDateTime(end).setTimeZone(zoneId));
        event.setDescription(buildDescription(consultation));

        service.events().update("primary", googleEventId, event).execute();
        System.out.println("✏️ Événement mis à jour sur Google Calendar");
    }

    // ════════════════════════════════════════════════════════
    //  ✅ RÉINITIALISER LE TOKEN (si problème d'authentification)
    //
    //  Si l'erreur persiste après les 3 fix ci-dessus :
    //    1. Supprimez le dossier "tokens/" dans votre projet
    //    2. Relancez l'application → une fenêtre de connexion Google s'ouvrira
    //    3. Connectez-vous → le token sera recréé
    // ════════════════════════════════════════════════════════
    public static void resetToken() {
        cachedService = null;
        File tokensDir = new File(TOKENS_DIRECTORY);
        if (tokensDir.exists()) {
            for (File f : tokensDir.listFiles()) f.delete();
            tokensDir.delete();
            System.out.println("🔄 Token Google Calendar réinitialisé. Reconnectez-vous.");
        }
    }

    // ════════════════════════════════════════════════════════
    //  DESCRIPTION
    // ════════════════════════════════════════════════════════
    private String buildDescription(Consultation consultation) {
        return "👤 Patient : " + consultation.getPatientFullName() + "\n" +
                "🧑‍⚕️ Psychologue : Dr. " + consultation.getPsyFullName() + "\n" +
                "📍 Type : " + consultation.getTypeDisplay() + "\n" +
                "💰 Prix : " + String.format("%.2f TND", consultation.getPrix()) + "\n" +
                "📋 Statut : " + consultation.getStatutDisplay() + "\n" +
                (consultation.getNotes() != null ? "📝 Notes : " + consultation.getNotes() : "") +
                "\n\n— NeuroWell Platform";
    }
}