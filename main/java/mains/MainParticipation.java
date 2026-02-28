package mains;

import entities.Evenement;
import entities.Participation;
import entities.UserUnified;
import services.ServiceEvenement;
import services.ServiceParticipation;
import services.UserService;
import services.UserService;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class MainParticipation {

    public static void main(String[] args) {

        ServiceParticipation serviceParticipation = new ServiceParticipation();
        ServiceEvenement     serviceEvenement     = new ServiceEvenement();
        UserService serviceUser          = new UserService();
        Scanner sc = new Scanner(System.in);
        int choix;

        do {
            System.out.println("\n====== MENU PARTICIPATION ======");
            System.out.println("1. Ajouter une participation");
            System.out.println("2. Modifier une participation");
            System.out.println("3. Supprimer une participation");
            System.out.println("4. Afficher toutes les participations");
            System.out.println("0. Quitter");
            System.out.print("Choisissez une option : ");

            while (!sc.hasNextInt()) {
                System.out.print("Veuillez entrer un nombre valide : ");
                sc.next();
            }
            choix = sc.nextInt();
            sc.nextLine();

            switch (choix) {

                case 1 -> {
                    try {
                        System.out.print("Id de l'evenement : ");
                        int id_e = sc.nextInt(); sc.nextLine();

                        System.out.print("Id de l'utilisateur : ");
                        int id_u = sc.nextInt(); sc.nextLine();

                        // ✅ Charger les objets depuis la BDD
                        Evenement ev = serviceEvenement.recupererParId(id_e);
                        if (ev == null) { System.out.println("Evenement introuvable."); break; }

                        UserUnified user = serviceUser.recupererParId(id_u);
                        if (user == null) { System.out.println("Utilisateur introuvable."); break; }

                        System.out.print("Objectif de participation : ");
                        String objectif = sc.nextLine();

                        String mode;
                        do {
                            System.out.print("Mode (presentiel / distanciel) : ");
                            mode = sc.nextLine().trim().toLowerCase();
                        } while (!mode.equals("presentiel") && !mode.equals("distanciel"));

                        Participation p = new Participation(ev, user, mode, objectif);
                        serviceParticipation.ajouter(p);

                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                }

                case 2 -> {
                    try {
                        System.out.print("Id de la participation a modifier : ");
                        int id_p = sc.nextInt(); sc.nextLine();

                        System.out.print("Id de l'evenement : ");
                        int id_e = sc.nextInt(); sc.nextLine();

                        System.out.print("Id de l'utilisateur : ");
                        int id_u = sc.nextInt(); sc.nextLine();

                        // ✅ Charger les objets depuis la BDD
                        Evenement ev = serviceEvenement.recupererParId(id_e);
                        if (ev == null) { System.out.println("Evenement introuvable."); break; }

                        UserUnified user = serviceUser.recupererParId(id_u);
                        if (user == null) { System.out.println("Utilisateur introuvable."); break; }

                        System.out.print("Nouvel objectif : ");
                        String objectif = sc.nextLine();

                        String mode;
                        do {
                            System.out.print("Nouveau mode (presentiel / distanciel) : ");
                            mode = sc.nextLine().trim().toLowerCase();
                        } while (!mode.equals("presentiel") && !mode.equals("distanciel"));

                        Participation pMod = new Participation(id_p, ev, user, mode, objectif);
                        serviceParticipation.modifier(pMod);

                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                }

                case 3 -> {
                    try {
                        System.out.print("Id de la participation a supprimer : ");
                        int id_p = sc.nextInt(); sc.nextLine();

                        Participation pSupp = new Participation();
                        pSupp.setId_p(id_p);
                        serviceParticipation.supprimer(pSupp);

                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                }

                case 4 -> {
                    try {
                        List<Participation> list = serviceParticipation.recuperer();
                        if (list.isEmpty()) {
                            System.out.println("Aucune participation trouvee.");
                        } else {
                            System.out.println("\nListe des participations :");
                            list.forEach(System.out::println);
                        }
                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                }

                case 0 -> System.out.println("Au revoir !");

                default -> System.out.println("Option invalide, reessayez !");
            }

        } while (choix != 0);

        sc.close();
    }
}