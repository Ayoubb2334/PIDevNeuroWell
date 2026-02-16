

import entities.Participation;
import org.junit.jupiter.api.*;
import services.ServiceParticipation;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceParticipationTest {

    private static ServiceParticipation service;
    private static Participation testParticipation;

    @BeforeAll
    public static void setup() throws SQLException {
        service = new ServiceParticipation();
        // On crée une participation de test
        testParticipation = new Participation(8, 1, "presentiel", "Objectif initial");
        // Assure-toi que l'événement id=8 existe dans la BDD avant ce test
        service.ajouter(testParticipation);
    }

    @Test
    @Order(1)
    public void testAjouter() throws SQLException {
        Participation p = new Participation(8, 1, "distanciel", "Nouvel objectif");
        service.ajouter(p);

        List<Participation> participations = service.recuperer();
        boolean found = participations.stream().anyMatch(part -> part.getObjectif().equals("Nouvel objectif"));
        assertTrue(found, "La participation ajoutée doit exister dans la BDD");
    }

    @Test
    @Order(2)
    public void testModifier() throws SQLException {
        testParticipation.setObjectif("Objectif modifié");
        testParticipation.setModeparticipation("distanciel");
        service.modifier(testParticipation);

        List<Participation> participations = service.recuperer();
        Participation updated = participations.stream()
                .filter(p -> p.getId_p() == testParticipation.getId_p())
                .findFirst().orElse(null);

        assertNotNull(updated, "La participation modifiée doit être retrouvée");
        assertEquals("Objectif modifié", updated.getObjectif());
        assertEquals("distanciel", updated.getModeparticipation());
    }

    @Test
    @Order(3)
    public void testSupprimer() throws SQLException {
        service.supprimer(testParticipation);

        List<Participation> participations = service.recuperer();
        boolean found = participations.stream()
                .anyMatch(p -> p.getId_p() == testParticipation.getId_p());
        assertFalse(found, "La participation supprimée ne doit plus exister");
    }

    @Test
    @Order(4)
    public void testAjouterEvenementInexistant() {
        // Tentative d'ajout d'une participation pour un événement inexistant
        Participation p = new Participation(9999, 1, "presentiel", "Test événement inexistant");
        assertThrows(SQLException.class, () -> service.ajouter(p),
                "Ajouter une participation pour un événement inexistant doit lancer une SQLException");
    }

    @Test
    @Order(5)
    public void testRecuperer() throws SQLException {
        List<Participation> participations = service.recuperer();
        assertNotNull(participations, "La liste des participations ne doit pas être null");
        assertTrue(participations.size() >= 0, "La liste des participations doit être récupérable");
    }

    @AfterAll
    public static void cleanup() throws SQLException {
        ServiceParticipation serviceCleanup = new ServiceParticipation();
        List<Participation> participations = serviceCleanup.recuperer();
        for (Participation p : participations) {
            if (p.getObjectif().contains("Objectif") || p.getObjectif().contains("Test événement")) {
                serviceCleanup.supprimer(p);
            }
        }
        // pas de close()
    }}

