import entities.Ressource;
import org.junit.jupiter.api.*;
import services.ServiceRessource;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceRessourceTest {

    static ServiceRessource service;
    static int testRessourceId;

    @BeforeAll
    static void setUp() {
        service = new ServiceRessource();
        System.out.println("=== Début des tests ServiceRessource ===");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== Fin des tests ServiceRessource ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Ajouter une ressource")
    void testAjouterRessource() throws SQLException {
        System.out.println("\n--- Test Ajouter Ressource ---");
        
        Ressource ressource = new Ressource(
                "Introduction à la Psychologie",  // titre
                "Document PDF sur les bases",     // description
                "PDF",                             // type
                "C:/docs/intro_psycho.pdf",       // cheminFichier
                2048.5,                            // tailleFichier (Ko)
                "pdf",                             // format
                "publié",                          // statut
                1                                  // idUser
        );
        ressource.setDatePublication(Date.valueOf(LocalDate.now()));

        service.ajouter(ressource);
        System.out.println("✓ Ressource ajoutée");

        List<Ressource> ressources = service.recuperer();
        assertTrue(ressources.stream().anyMatch(r -> 
            r.getTitre().equals("Introduction à la Psychologie")
        ), "La ressource devrait être dans la liste");

        testRessourceId = ressources.stream()
                .filter(r -> r.getTitre().equals("Introduction à la Psychologie"))
                .findFirst()
                .map(Ressource::getIdRessource)
                .orElse(-1);

        System.out.println("✓ ID de la ressource ajoutée : " + testRessourceId);
        assertNotEquals(-1, testRessourceId, "La ressource devrait avoir un ID valide");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Modifier une ressource")
    void testModifierRessource() throws SQLException {
        System.out.println("\n--- Test Modifier Ressource ---");
        
        Ressource ressource = new Ressource();
        ressource.setIdRessource(testRessourceId);
        ressource.setTitre("Psychologie Avancée");
        ressource.setDescription("Document mis à jour");
        ressource.setType("PDF");
        ressource.setCheminFichier("C:/docs/psycho_advanced.pdf");
        ressource.setTailleFichier(3072.0);
        ressource.setFormat("pdf");
        ressource.setStatut("archivé");
        ressource.setIdUser(1);

        service.modifier(ressource);
        System.out.println("✓ Ressource modifiée");

        List<Ressource> ressources = service.recuperer();
        Ressource modifiee = ressources.stream()
                .filter(r -> r.getIdRessource() == testRessourceId)
                .findFirst()
                .orElse(null);

        assertNotNull(modifiee, "La ressource modifiée devrait exister");
        assertEquals("Psychologie Avancée", modifiee.getTitre(), "Le titre devrait être modifié");
        assertEquals("archivé", modifiee.getStatut(), "Le statut devrait être archivé");
        assertEquals(3072.0, modifiee.getTailleFichier(), 0.01, "La taille devrait être 3072.0");
        System.out.println("✓ Vérifications réussies : Titre=" + modifiee.getTitre() + 
                          ", Statut=" + modifiee.getStatut() + 
                          ", Taille=" + modifiee.getTailleFichier() + " Ko");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Récupérer toutes les ressources")
    void testRecupererRessources() throws SQLException {
        System.out.println("\n--- Test Récupérer Ressources ---");
        
        List<Ressource> ressources = service.recuperer();
        assertNotNull(ressources, "La liste ne devrait pas être nulle");
        assertTrue(ressources.size() > 0, "La liste devrait contenir au moins une ressource");
        
        System.out.println("✓ Nombre de ressources : " + ressources.size());

        // Vérifier que toutes les ressources ont des données valides
        for (Ressource r : ressources) {
            assertNotNull(r.getTitre(), "Le titre ne devrait pas être null");
            assertNotNull(r.getType(), "Le type ne devrait pas être null");
            assertNotNull(r.getFormat(), "Le format ne devrait pas être null");
            assertTrue(r.getTailleFichier() > 0, "La taille devrait être positive");
            assertNotNull(r.getStatut(), "Le statut ne devrait pas être null");
        }
        System.out.println("✓ Toutes les ressources ont des données valides");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Rechercher par type")
    void testRechercherParType() throws SQLException {
        System.out.println("\n--- Test Rechercher par Type ---");
        
        // Ajouter des ressources de différents types
        Ressource video = new Ressource("Vidéo Test", "Description", "Vidéo", 
                                        "C:/videos/test.mp4", 4096.0, "mp4", "publié", 1);
        video.setDatePublication(Date.valueOf(LocalDate.now()));
        service.ajouter(video);
        System.out.println("✓ Vidéo ajoutée");

        Ressource image = new Ressource("Image Test", "Description", "Image", 
                                        "C:/images/test.png", 512.0, "png", "publié", 1);
        image.setDatePublication(Date.valueOf(LocalDate.now()));
        service.ajouter(image);
        System.out.println("✓ Image ajoutée");

        // Rechercher par type
        List<Ressource> videos = service.rechercherParType("Vidéo");
        List<Ressource> images = service.rechercherParType("Image");
        List<Ressource> pdfs = service.rechercherParType("PDF");

        assertTrue(videos.size() > 0, "Devrait trouver au moins une vidéo");
        assertTrue(images.size() > 0, "Devrait trouver au moins une image");
        assertTrue(pdfs.size() > 0, "Devrait trouver au moins un PDF");

        System.out.println("✓ Vidéos trouvées : " + videos.size());
        System.out.println("✓ Images trouvées : " + images.size());
        System.out.println("✓ PDFs trouvés : " + pdfs.size());

        // Vérifier que les types sont corrects
        assertTrue(videos.stream().allMatch(r -> r.getType().equals("Vidéo")));
        assertTrue(images.stream().allMatch(r -> r.getType().equals("Image")));
        assertTrue(pdfs.stream().allMatch(r -> r.getType().equals("PDF")));

        // Nettoyage
        service.supprimer(video);
        service.supprimer(image);
        System.out.println("✓ Ressources de test nettoyées");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Rechercher par statut")
    void testRechercherParStatut() throws SQLException {
        System.out.println("\n--- Test Rechercher par Statut ---");
        
        // Ajouter des ressources avec différents statuts
        Ressource brouillon = new Ressource("Brouillon Test", "Desc", "PDF", 
                                            "C:/docs/brouillon.pdf", 1024.0, "pdf", "brouillon", 1);
        brouillon.setDatePublication(Date.valueOf(LocalDate.now()));
        service.ajouter(brouillon);
        System.out.println("✓ Brouillon ajouté");

        Ressource publie = new Ressource("Publié Test", "Desc", "PDF", 
                                         "C:/docs/publie.pdf", 1024.0, "pdf", "publié", 1);
        publie.setDatePublication(Date.valueOf(LocalDate.now()));
        service.ajouter(publie);
        System.out.println("✓ Publié ajouté");

        // Rechercher par statut
        List<Ressource> brouillons = service.rechercherParStatut("brouillon");
        List<Ressource> publies = service.rechercherParStatut("publié");
        List<Ressource> archives = service.rechercherParStatut("archivé");

        assertTrue(brouillons.size() > 0, "Devrait trouver au moins un brouillon");
        assertTrue(publies.size() > 0, "Devrait trouver au moins une ressource publiée");

        System.out.println("✓ Brouillons trouvés : " + brouillons.size());
        System.out.println("✓ Publiés trouvés : " + publies.size());
        System.out.println("✓ Archivés trouvés : " + archives.size());

        // Vérifier que les statuts sont corrects
        assertTrue(brouillons.stream().allMatch(r -> r.getStatut().equals("brouillon")));
        assertTrue(publies.stream().allMatch(r -> r.getStatut().equals("publié")));

        // Nettoyage
        service.supprimer(brouillon);
        service.supprimer(publie);
        System.out.println("✓ Ressources de test nettoyées");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Récupérer par ID")
    void testRecupererParId() throws SQLException {
        System.out.println("\n--- Test Récupérer par ID ---");
        
        Ressource ressource = service.recupererParId(testRessourceId);
        
        assertNotNull(ressource, "La ressource devrait exister");
        assertEquals(testRessourceId, ressource.getIdRessource(), "L'ID devrait correspondre");
        System.out.println("✓ Ressource trouvée : " + ressource.getTitre());

        // Test avec ID inexistant
        Ressource inexistante = service.recupererParId(99999);
        assertNull(inexistante, "Une ressource inexistante devrait retourner null");
        System.out.println("✓ ID inexistant retourne null comme prévu");
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Validation des types de ressources")
    void testTypesRessourcesValides() throws SQLException {
        System.out.println("\n--- Test Types de Ressources Valides ---");
        
        String[] typesValides = {"PDF", "Vidéo", "Image", "Audio", "Article"};
        String[] formatsValides = {"pdf", "mp4", "png", "mp3", "docx"};
        
        for (int i = 0; i < typesValides.length; i++) {
            Ressource r = new Ressource(
                "Test " + typesValides[i],
                "Description",
                typesValides[i],
                "C:/test." + formatsValides[i],
                1024.0,
                formatsValides[i],
                "brouillon",
                1
            );
            r.setDatePublication(Date.valueOf(LocalDate.now()));
            service.ajouter(r);
            System.out.println("✓ Type ajouté : " + typesValides[i]);
        }

        List<Ressource> ressources = service.recuperer();
        for (String type : typesValides) {
            assertTrue(ressources.stream().anyMatch(r -> r.getType().equals(type)),
                      "Le type " + type + " devrait exister");
        }
        
        // Nettoyage
        for (String type : typesValides) {
            ressources.stream()
                .filter(r -> r.getTitre().startsWith("Test " + type))
                .forEach(r -> {
                    try {
                        service.supprimer(r);
                    } catch (SQLException ex) {
                        fail("Erreur lors du nettoyage : " + ex.getMessage());
                    }
                });
        }
        System.out.println("✓ Tous les types de ressources sont valides et nettoyés");
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Supprimer une ressource")
    void testSupprimerRessource() throws SQLException {
        System.out.println("\n--- Test Supprimer Ressource ---");
        
        Ressource ressource = new Ressource();
        ressource.setIdRessource(testRessourceId);

        service.supprimer(ressource);
        System.out.println("✓ Ressource supprimée (ID: " + testRessourceId + ")");

        List<Ressource> ressources = service.recuperer();
        boolean existe = ressources.stream()
                .anyMatch(r -> r.getIdRessource() == testRessourceId);
        
        assertFalse(existe, "La ressource ne devrait plus exister");
        System.out.println("✓ Vérification : La ressource n'existe plus dans la base");
    }
}
