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

          // Participation déjà existante en BDD
          testParticipation = new Participation(8, 1, "presentiel", "Objectif initial");
          testParticipation.setId_p(10); // ✅ ID réel de la participation en base
      }

      @Test
      @Order(1)
      public void testRecuperer() throws SQLException {
          List<Participation> participations = service.recuperer();

          boolean found = participations.stream()
                  .anyMatch(p -> p.getId_p() == 10);

          assertTrue(found, "La participation id=10 doit exister");
      }

      @Test
      @Order(2)
      public void testModifier() throws SQLException {
          testParticipation.setObjectif("Objectif modifié");
          testParticipation.setModeparticipation("distanciel");

          service.modifier(testParticipation);

          Participation updated = service.recuperer().stream()
                  .filter(p -> p.getId_p() == 10)
                  .findFirst()
                  .orElse(null);

          assertNotNull(updated, "La participation modifiée doit être retrouvée");
          assertEquals("Objectif modifié", updated.getObjectif());
          assertEquals("distanciel", updated.getModeparticipation());
      }

      @Test
      @Order(3)
      public void testSupprimer() throws SQLException {
          service.supprimer(testParticipation);

          boolean found = service.recuperer().stream()
                  .anyMatch(p -> p.getId_p() == 10);

          assertFalse(found, "La participation id=10 doit être supprimée");
      }

      @Test
      @Order(4)
      public void testAjouterEvenementInexistant() {
          Participation p = new Participation(9999, 1, "presentiel", "Test événement inexistant");
          assertThrows(SQLException.class, () -> service.ajouter(p));
      }
  }
