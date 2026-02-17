package services;

import entities.Consultation;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConsultationServiceTest {

    static ConsultationService service;
    static int idConsultationTest;
    static int idUser;
    static int idPsychologue;

    @BeforeAll
    static void setup() throws SQLException {

        System.out.println("Initialisation du test...");

        service = new ConsultationService();
        Connection cnx = utils.MyDatabase.getInstance().getConnection();

        // Récupérer un utilisateur existant
        Statement st1 = cnx.createStatement();
        ResultSet rs1 = st1.executeQuery("SELECT id_user FROM users LIMIT 1");

        if (rs1.next()) {
            idUser = rs1.getInt("id_user");
            System.out.println("User trouvé : " + idUser);
        } else {
            fail("Aucun utilisateur trouvé dans la base !");
        }

        // Récupérer un psychologue existant
        Statement st2 = cnx.createStatement();
        ResultSet rs2 = st2.executeQuery("SELECT id_psychologue FROM psychologues LIMIT 1");

        if (rs2.next()) {
            idPsychologue = rs2.getInt("id_psychologue");
            System.out.println("Psychologue trouvé : " + idPsychologue);
        } else {
            fail("Aucun psychologue trouvé dans la base !");
        }
    }

    @Test
    @Order(1)
    void testAjouterConsultation() throws SQLException {

        System.out.println("Test Ajouter Consultation");

        Consultation c = new Consultation();
        c.setIdUser(idUser);
        c.setIdPsychologue(idPsychologue);
        c.setDateConsultation(LocalDate.now().plusDays(1));
        c.setHeureConsultation(LocalTime.of(10, 0));
        c.setTypeConsultation("presentiel");
        c.setStatut("planifiee");

        service.ajouter(c);

        List<Consultation> list = service.afficher();

        assertFalse(list.isEmpty());

        idConsultationTest = list.get(list.size() - 1).getIdConsultation();

        System.out.println("Consultation ajoutée avec ID : " + idConsultationTest);
    }

    @Test
    @Order(2)
    void testModifierConsultation() throws SQLException {

        System.out.println("Test Modifier Consultation");

        Consultation c = new Consultation();
        c.setIdConsultation(idConsultationTest);
        c.setIdUser(idUser);
        c.setIdPsychologue(idPsychologue);
        c.setDateConsultation(LocalDate.now().plusDays(2));
        c.setHeureConsultation(LocalTime.of(12, 0));
        c.setTypeConsultation("en_ligne");
        c.setStatut("en_cours");

        service.modifier(c);

        List<Consultation> list = service.afficher();

        boolean trouve = list.stream()
                .anyMatch(con -> con.getIdConsultation() == idConsultationTest
                        && con.getTypeConsultation().equals("en_ligne"));

        assertTrue(trouve);

        System.out.println("Modification réussie.");
    }

    @Test
    @Order(3)
    void testSupprimerConsultation() throws SQLException {

        System.out.println("Test Supprimer Consultation");

        service.supprimer(idConsultationTest);

        List<Consultation> list = service.afficher();

        boolean existe = list.stream()
                .anyMatch(c -> c.getIdConsultation() == idConsultationTest);

        assertFalse(existe);

        System.out.println("Suppression réussie.");
    }
}
