import entities.Evenement;
import org.junit.jupiter.api.*;
import services.ServiceEvenement;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceEvenementTest {

    static ServiceEvenement service;
    static int testEventId;

    @BeforeAll
    static void setUp() {
        service = new ServiceEvenement();
    }

    @Test
    @Order(1)
    void testAjouterEvenement() throws SQLException {
        Evenement e = new Evenement(
                "TestTitre",
                "TestDesc",
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)),
                "Tunis",
                "Conférence",
                50,
                "Actif",
                "100DT",
                "images/evenements/test.jpg"   // image = dernier paramètre
        );

        service.ajouter(e);

        List<Evenement> events = service.recuperer();
        assertTrue(events.stream().anyMatch(ev -> ev.getTitre_e().equals("TestTitre")));

        testEventId = events.stream()
                .filter(ev -> ev.getTitre_e().equals("TestTitre"))
                .findFirst()
                .map(Evenement::getId_e)
                .orElse(-1);

        System.out.println("ID du test ajouté : " + testEventId);
        assertNotEquals(-1, testEventId, "L'événement devrait avoir été ajouté avec un ID valide");
    }

    @Test
    @Order(2)
    void testModifierEvenement() throws SQLException {
        Evenement e = new Evenement();
        e.setId_e(testEventId);
        e.setTitre_e("TitreModifie");
        e.setDescription_e("DescModifie");
        e.setDate_e(Timestamp.valueOf(LocalDateTime.now().plusDays(2)));
        e.setLocalisation_e("Sousse");
        e.setType_e("Workshop");
        e.setCapacitemax_e(100);
        e.setStatut_e("Inactif");
        e.setPrix_e("150DT");
        e.setImage("images/evenements/modified.jpg");

        service.modifier(e);

        List<Evenement> events = service.recuperer();
        assertTrue(events.stream().anyMatch(ev -> ev.getTitre_e().equals("TitreModifie")));

        Evenement modifie = events.stream()
                .filter(ev -> ev.getId_e() == testEventId)
                .findFirst()
                .orElse(null);

        assertNotNull(modifie);
        assertEquals("images/evenements/modified.jpg", modifie.getImage());
    }

    @Test
    @Order(3)
    void testSupprimerEvenement() throws SQLException {
        Evenement e = new Evenement();
        e.setId_e(testEventId);

        service.supprimer(e);

        List<Evenement> events = service.recuperer();
        assertFalse(events.stream().anyMatch(ev -> ev.getId_e() == testEventId));
    }

    @Test
    @Order(4)
    void testRecupererEvenements() throws SQLException {
        List<Evenement> events = service.recuperer();
        assertNotNull(events);
        assertTrue(events.size() >= 0);

        if (!events.isEmpty()) {
            Evenement premier = events.get(0);
            assertNotNull(premier.getTitre_e());
            assertNotNull(premier.getDescription_e());
            assertNotNull(premier.getDate_e());
            // image peut être vide selon les anciens enregistrements
        }
    }
}
