package services;

import entities.Evenement;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvenement implements IService<Evenement> {

    private Connection connection;

    public ServiceEvenement() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override

    public void ajouter(Evenement e) throws SQLException {
        String req = "INSERT INTO évenements(titre_e, description_e, date_e, localisation_e, type_e, capacitemax_e, statut_e, prix_e, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(req);

        ps.setString(1, e.getTitre_e() != null ? e.getTitre_e() : "");  // jamais null
        ps.setString(2, e.getDescription_e() != null ? e.getDescription_e() : "");
        ps.setTimestamp(3, e.getDate_e() != null ? e.getDate_e() : new Timestamp(System.currentTimeMillis()));
        ps.setString(4, e.getLocalisation_e() != null ? e.getLocalisation_e() : "");
        ps.setString(5, e.getType_e() != null ? e.getType_e() : "");
        ps.setInt(6, e.getCapacitemax_e() > 0 ? e.getCapacitemax_e() : 0);  // valeur par défaut si <=0

        // ✅ Statut obligatoire : si null ou vide, mettre une valeur par défaut
        ps.setString(7, (e.getStatut_e() != null && !e.getStatut_e().isEmpty()) ? e.getStatut_e() : "En cours");

        ps.setString(8, e.getPrix_e() != null ? e.getPrix_e() : "0");  // valeur par défaut
        ps.setString(9, e.getImage() != null ? e.getImage() : "");

        ps.executeUpdate();
        System.out.println("Événement ajouté ✅");
    }

    @Override
    public void modifier(Evenement e) throws SQLException {
        String req = "UPDATE évenements SET titre_e=?, description_e=?, date_e=?, localisation_e=?, type_e=?, capacitemax_e=?, statut_e=?, prix_e=?, image=? WHERE id_e=?";
        PreparedStatement ps = connection.prepareStatement(req);

        ps.setString(1, e.getTitre_e());
        ps.setString(2, e.getDescription_e());
        ps.setTimestamp(3, e.getDate_e() != null ? e.getDate_e() : new Timestamp(System.currentTimeMillis()));
        ps.setString(4, e.getLocalisation_e());
        ps.setString(5, e.getType_e());
        ps.setInt(6, e.getCapacitemax_e());
        ps.setString(7, e.getStatut_e());
        ps.setString(8, e.getPrix_e());
        ps.setString(9, e.getImage());
        ps.setInt(10, e.getId_e());

        ps.executeUpdate();
        System.out.println("Événement modifié");
    }

    @Override
    public void supprimer(Evenement e) throws SQLException {
        String req = "DELETE FROM évenements WHERE id_e=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, e.getId_e());
        ps.executeUpdate();
        System.out.println("Événement supprimé");
    }

    @Override
    public List<Evenement> recuperer() throws SQLException {
        List<Evenement> events = new ArrayList<>();
        String req = "SELECT * FROM évenements";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            int id = rs.getInt("id_e");
            String titre = rs.getString("titre_e");
            String desc = rs.getString("description_e");
            Timestamp date = rs.getTimestamp("date_e");
            String localisation = rs.getString("localisation_e");
            String type = rs.getString("type_e");
            int capacite = rs.getInt("capacitemax_e");
            String statut = rs.getString("statut_e");
            String prix = rs.getString("prix_e");
            String image = rs.getString("image");

            Evenement e = new Evenement(id, titre, desc, date, localisation, type, capacite, statut, prix, image);
            events.add(e);
        }
        return events;
    }
}
