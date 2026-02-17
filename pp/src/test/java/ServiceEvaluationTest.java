import entities.Evaluation;
import org.junit.jupiter.api.*;
import services.ServiceEvaluation;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceEvaluationTest {

    static ServiceEvaluation service;
    static int testEvaluationId;

    @BeforeAll
    static void setUp() {
        service = new ServiceEvaluation();
        System.out.println("=== Début des tests ServiceEvaluation ===");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== Fin des tests ServiceEvaluation ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Ajouter une évaluation")
    void testAjouterEvaluation() throws SQLException {
        System.out.println("\n--- Test Ajouter Évaluation ---");
        
        Evaluation evaluation = new Evaluation(
                "Stress",           // typeTest
                15,                 // score
                "Élevé",           // niveau
                Date.valueOf(LocalDate.now()), // dateEvaluation
                1                   // idUser
        );

        service.ajouter(evaluation);
        System.out.println("✓ Évaluation ajoutée");

        List<Evaluation> evaluations = service.recuperer();
        assertTrue(evaluations.stream().anyMatch(e -> 
            e.getTypeTest().equals("Stress") && e.getScore() == 15
        ), "L'évaluation devrait être dans la liste");

        testEvaluationId = evaluations.stream()
                .filter(e -> e.getTypeTest().equals("Stress") && e.getScore() == 15)
                .findFirst()
                .map(Evaluation::getIdEvaluation)
                .orElse(-1);

        System.out.println("✓ ID de l'évaluation ajoutée : " + testEvaluationId);
        assertNotEquals(-1, testEvaluationId, "L'évaluation devrait avoir un ID valide");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Modifier une évaluation")
    void testModifierEvaluation() throws SQLException {
        System.out.println("\n--- Test Modifier Évaluation ---");
        
        Evaluation evaluation = new Evaluation();
        evaluation.setIdEvaluation(testEvaluationId);
        evaluation.setTypeTest("Anxiété");
        evaluation.setScore(12);
        evaluation.setNiveau("Moyen");
        evaluation.setDateEvaluation(Date.valueOf(LocalDate.now().minusDays(1)));
        evaluation.setIdUser(1);

        service.modifier(evaluation);
        System.out.println("✓ Évaluation modifiée");

        List<Evaluation> evaluations = service.recuperer();
        Evaluation modifiee = evaluations.stream()
                .filter(e -> e.getIdEvaluation() == testEvaluationId)
                .findFirst()
                .orElse(null);

        assertNotNull(modifiee, "L'évaluation modifiée devrait exister");
        assertEquals("Anxiété", modifiee.getTypeTest(), "Le type de test devrait être Anxiété");
        assertEquals(12, modifiee.getScore(), "Le score devrait être 12");
        assertEquals("Moyen", modifiee.getNiveau(), "Le niveau devrait être Moyen");
        System.out.println("✓ Vérifications réussies : Type=" + modifiee.getTypeTest() + 
                          ", Score=" + modifiee.getScore() + 
                          ", Niveau=" + modifiee.getNiveau());
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Récupérer toutes les évaluations")
    void testRecupererEvaluations() throws SQLException {
        System.out.println("\n--- Test Récupérer Évaluations ---");
        
        List<Evaluation> evaluations = service.recuperer();
        assertNotNull(evaluations, "La liste ne devrait pas être nulle");
        assertTrue(evaluations.size() > 0, "La liste devrait contenir au moins une évaluation");
        
        System.out.println("✓ Nombre d'évaluations : " + evaluations.size());

        // Vérifier que toutes les évaluations ont des données valides
        for (Evaluation e : evaluations) {
            assertNotNull(e.getTypeTest(), "Le type de test ne devrait pas être null");
            assertTrue(e.getScore() >= 0 && e.getScore() <= 100, 
                      "Le score devrait être entre 0 et 100");
            assertNotNull(e.getNiveau(), "Le niveau ne devrait pas être null");
            assertNotNull(e.getDateEvaluation(), "La date ne devrait pas être null");
        }
        System.out.println("✓ Toutes les évaluations ont des données valides");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Validation du niveau selon le score")
    void testValidationNiveau() {
        System.out.println("\n--- Test Validation Niveau ---");
        
        // Score < 8 → Faible
        Evaluation eval1 = new Evaluation("Test", 5, "Faible", Date.valueOf(LocalDate.now()), 1);
        assertEquals("Faible", eval1.getNiveau());
        System.out.println("✓ Score 5 → Niveau Faible");

        // 8 ≤ Score ≤ 14 → Moyen
        Evaluation eval2 = new Evaluation("Test", 10, "Moyen", Date.valueOf(LocalDate.now()), 1);
        assertEquals("Moyen", eval2.getNiveau());
        System.out.println("✓ Score 10 → Niveau Moyen");

        // Score > 14 → Élevé
        Evaluation eval3 = new Evaluation("Test", 18, "Élevé", Date.valueOf(LocalDate.now()), 1);
        assertEquals("Élevé", eval3.getNiveau());
        System.out.println("✓ Score 18 → Niveau Élevé");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Supprimer une évaluation")
    void testSupprimerEvaluation() throws SQLException {
        System.out.println("\n--- Test Supprimer Évaluation ---");
        
        Evaluation evaluation = new Evaluation();
        evaluation.setIdEvaluation(testEvaluationId);

        service.supprimer(evaluation);
        System.out.println("✓ Évaluation supprimée (ID: " + testEvaluationId + ")");

        List<Evaluation> evaluations = service.recuperer();
        boolean existe = evaluations.stream()
                .anyMatch(e -> e.getIdEvaluation() == testEvaluationId);
        
        assertFalse(existe, "L'évaluation ne devrait plus exister");
        System.out.println("✓ Vérification : L'évaluation n'existe plus dans la base");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Tester les types de test valides")
    void testTypesTestValides() throws SQLException {
        System.out.println("\n--- Test Types de Test Valides ---");
        
        String[] typesValides = {"Stress", "Anxiété", "Dépression", "Bien-être", "Burnout"};
        
        for (String type : typesValides) {
            Evaluation eval = new Evaluation(type, 10, "Moyen", Date.valueOf(LocalDate.now()), 1);
            service.ajouter(eval);
            System.out.println("✓ Type ajouté : " + type);
        }

        List<Evaluation> evaluations = service.recuperer();
        for (String type : typesValides) {
            assertTrue(evaluations.stream().anyMatch(e -> e.getTypeTest().equals(type)),
                      "Le type " + type + " devrait exister");
        }
        
        // Nettoyage
        for (String type : typesValides) {
            evaluations.stream()
                .filter(e -> e.getTypeTest().equals(type))
                .forEach(e -> {
                    try {
                        service.supprimer(e);
                    } catch (SQLException ex) {
                        fail("Erreur lors du nettoyage : " + ex.getMessage());
                    }
                });
        }
        System.out.println("✓ Tous les types de test sont valides et nettoyés");
    }
}
