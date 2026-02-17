package mains;

import entities.Participation;
import services.ServiceParticipation;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class MainParticipation {

    public static void main(String[] args) {

        ServiceParticipation service = new ServiceParticipation();
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
            sc.nextLine(); // Consommer le retour à la ligne

            switch (choix) {
                case 1:
                    try {
                        System.out.print("Id de l'événement : ");
                        int id_e = sc.nextInt();
                        sc.nextLine();

                        System.out.print("Id de l'utilisateur : ");
                        int id_u = sc.nextInt();
                        sc.nextLine();

                        System.out.print("Objectif de participation : ");
                        String objectif = sc.nextLine();

                        String modeparticipation;
                        do {
                            System.out.print("Mode de participation (presentiel / en_ligne) : ");
                            modeparticipation = sc.nextLine().trim().toLowerCase();
                        } while (!modeparticipation.equals("presentiel") && !modeparticipation.equals("en_ligne"));

                        Participation p = new Participation(id_e, id_u, modeparticipation, objectif);
                        service.ajouter(p);

                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                    break;

                case 2:
                    try {
                        System.out.print("Id de la participation à modifier : ");
                        int id_p = sc.nextInt();
                        sc.nextLine();

                        System.out.print("Id de l'événement : ");
                        int id_eMod = sc.nextInt();
                        sc.nextLine();

                        System.out.print("Id de l'utilisateur : ");
                        int id_uMod = sc.nextInt();
                        sc.nextLine();

                        System.out.print("Nouvel objectif de participation : ");
                        String objectifMod = sc.nextLine();

                        String modeMod;
                        do {
                            System.out.print("Nouveau mode de participation (presentiel / en_ligne) : ");
                            modeMod = sc.nextLine().trim().toLowerCase();
                        } while (!modeMod.equals("presentiel") && !modeMod.equals("en_ligne"));

                        Participation pMod = new Participation(id_p, id_eMod, id_uMod, modeMod, objectifMod);
                        service.modifier(pMod);

                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                    break;

                case 3:
                    try {
                        System.out.print("Id de la participation à supprimer : ");
                        int id_pSupp = sc.nextInt();
                        sc.nextLine();

                        Participation pSupp = new Participation();
                        pSupp.setId_p(id_pSupp);
                        service.supprimer(pSupp);

                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                    break;

                case 4:
                    try {
                        List<Participation> list = service.recuperer();
                        if (list.isEmpty()) {
                            System.out.println("Aucune participation trouvée.");
                        } else {
                            System.out.println("\nListe des participations :");
                            for (Participation p : list) {
                                System.out.println(p);
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Erreur SQL : " + e.getMessage());
                    }
                    break;

                case 0:
                    System.out.println("Au revoir !");
                    break;

                default:
                    System.out.println("Option invalide, réessayez !");
            }

        } while (choix != 0);

        sc.close();
    }
}
