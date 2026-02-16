package mains;

import entities.Evenement;
import services.ServiceEvenement;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;

public class MainEvenement {

    private static ServiceEvenement serviceEvenement;

    public static void main(String[] args) {

        try {
            serviceEvenement = new ServiceEvenement();
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\n=== Menu Événements ===");
                System.out.println("1 - Ajouter un événement");
                System.out.println("2 - Modifier un événement");
                System.out.println("3 - Supprimer un événement");
                System.out.println("4 - Afficher tous les événements");
                System.out.println("0 - Quitter");
                System.out.print("Choisissez une option : ");
                int choix = sc.nextInt();
                sc.nextLine();

                switch (choix) {
                    case 1:
                        ajouterEvenement(sc);
                        break;
                    case 2:
                        modifierEvenement(sc);
                        break;
                    case 3:
                        supprimerEvenement(sc);
                        break;
                    case 4:
                        afficherListe();
                        break;
                    case 0:
                        System.out.println("Au revoir !");
                        return;
                    default:
                        System.out.println("Option invalide !");
                }
            }

        } catch (SQLException ex) {
            System.out.println("Erreur SQL : " + ex.getMessage());
        }
    }

    private static Timestamp lireDate(Scanner sc) {
        System.out.print("Date (yyyy-MM-dd HH:mm:ss) : ");
        String dateStr = sc.nextLine();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date parsedDate = sdf.parse(dateStr);
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException ex) {
            System.out.println("Format invalide, date actuelle utilisée.");
            return new Timestamp(System.currentTimeMillis());
        }
    }

    private static void ajouterEvenement(Scanner sc) throws SQLException {
        System.out.print("Titre : ");
        String titre = sc.nextLine();

        System.out.print("Image (chemin ou vide) : ");
        String image = sc.nextLine();

        System.out.print("Description : ");
        String description = sc.nextLine();
        Timestamp date = lireDate(sc);
        System.out.print("Localisation : ");
        String localisation = sc.nextLine();
        System.out.print("Type : ");
        String type = sc.nextLine();
        System.out.print("Capacité max : ");
        int capacite = sc.nextInt();
        sc.nextLine();
        System.out.print("Statut : ");
        String statut = sc.nextLine();
        System.out.print("Prix : ");
        String prix = sc.nextLine();

        Evenement e = new Evenement(titre, description, date, localisation, type, capacite, statut, prix, image);
        serviceEvenement.ajouter(e);
        System.out.println("✅ Événement ajouté avec succès !");
    }

    private static void modifierEvenement(Scanner sc) throws SQLException {
        System.out.print("ID de l'événement à modifier : ");
        int id = sc.nextInt();
        sc.nextLine();

        System.out.print("Titre : ");
        String titre = sc.nextLine();

        System.out.print("Image (chemin ou vide) : ");
        String image = sc.nextLine();

        System.out.print("Description : ");
        String description = sc.nextLine();
        Timestamp date = lireDate(sc);
        System.out.print("Localisation : ");
        String localisation = sc.nextLine();
        System.out.print("Type : ");
        String type = sc.nextLine();
        System.out.print("Capacité max : ");
        int capacite = sc.nextInt();
        sc.nextLine();
        System.out.print("Statut : ");
        String statut = sc.nextLine();
        System.out.print("Prix : ");
        String prix = sc.nextLine();

        Evenement e = new Evenement(id, titre, description, date, localisation, type, capacite, statut, prix, image);
        serviceEvenement.modifier(e);
        System.out.println("✅ Événement modifié avec succès !");
    }

    private static void supprimerEvenement(Scanner sc) throws SQLException {
        System.out.print("ID de l'événement à supprimer : ");
        int id = sc.nextInt();
        sc.nextLine();

        Evenement e = new Evenement();
        e.setId_e(id);
        serviceEvenement.supprimer(e);

        System.out.println("✅ Événement supprimé avec succès !");
    }

    private static void afficherListe() throws SQLException {
        List<Evenement> events = serviceEvenement.recuperer();
        if (events.isEmpty()) {
            System.out.println("Aucun événement trouvé.");
        } else {
            System.out.println("\n=== Liste des événements ===");
            for (Evenement e : events) {
                System.out.println(e);
            }
        }
    }
}
