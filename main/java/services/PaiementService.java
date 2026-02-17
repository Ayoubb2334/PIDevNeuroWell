package services;

import entities.Paiement;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaiementService {

    private Connection cnx;

    public PaiementService() {
        cnx = MyConnection.getInstance();
    }

    // =========================
    // AJOUTER
    // =========================
    public void ajouter(Paiement p) throws SQLException {

        String sql = "INSERT INTO paiement (montant, mode_paiement, statut, date_paiement, id_user) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDouble(1, p.getMontant());
        ps.setString(2, p.getModePaiement());
        ps.setString(3, p.getStatut());
        ps.setDate(4, p.getDatePaiement());
        ps.setInt(5, p.getIdUser());

        ps.executeUpdate();
    }

    // =========================
    // AFFICHER TOUT
    // =========================
    public List<Paiement> afficher() throws SQLException {

        List<Paiement> list = new ArrayList<>();

        String sql = "SELECT * FROM paiement";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Paiement p = new Paiement(
                    rs.getInt("id"),
                    rs.getDouble("montant"),
                    rs.getString("mode_paiement"),
                    rs.getString("statut"),
                    rs.getDate("date_paiement"),
                    rs.getInt("id_user")
            );

            list.add(p);
        }

        return list;
    }

    // =========================
    // AFFICHER PAR USER  🔥 IMPORTANT POUR FRONT
    // =========================
    public List<Paiement> afficherParUser(int idUser) throws SQLException {

        List<Paiement> list = new ArrayList<>();

        String sql = "SELECT * FROM paiement WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idUser);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Paiement p = new Paiement(
                    rs.getInt("id"),
                    rs.getDouble("montant"),
                    rs.getString("mode_paiement"),
                    rs.getString("statut"),
                    rs.getDate("date_paiement"),
                    rs.getInt("id_user")
            );

            list.add(p);
        }

        return list;
    }

    // =========================
    // SUPPRIMER
    // =========================
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM paiement WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // =========================
    // MODIFIER
    // =========================
    public void modifier(Paiement p) throws SQLException {

        String sql = "UPDATE paiement SET montant=?, mode_paiement=?, statut=?, date_paiement=?, id_user=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setDouble(1, p.getMontant());
        ps.setString(2, p.getModePaiement());
        ps.setString(3, p.getStatut());
        ps.setDate(4, p.getDatePaiement());
        ps.setInt(5, p.getIdUser());
        ps.setInt(6, p.getId());

        ps.executeUpdate();
    }
}
