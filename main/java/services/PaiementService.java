package services;

import entities.Paiement;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class PaiementService {

    private Connection cnx = MyDatabase.getInstance().getConnection();

    // ---------------- ADD ----------------
    public void add(Paiement p) throws SQLException {

        String sql = "INSERT INTO paiement (montant, date_paiement, mode_paiement, statut, reference, user_id) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setDouble(1, p.getMontant());
        ps.setDate(2, java.sql.Date.valueOf(p.getDatePaiement())); // ✅ CORRECTION
        ps.setString(3, p.getModePaiement());
        ps.setString(4, p.getStatut());
        ps.setString(5, p.getReference());
        ps.setInt(6, p.getUserId());

        ps.executeUpdate();
    }

    // ---------------- UPDATE ----------------
    public void update(Paiement p) throws SQLException {
        String sql = "UPDATE paiement SET montant=?, mode_paiement=?, statut=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDouble(1, p.getMontant());
        ps.setString(2, p.getModePaiement());
        ps.setString(3, p.getStatut());
        ps.setInt(4, p.getId());
        ps.executeUpdate();
    }

    // ---------------- DELETE ----------------
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM paiement WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
    public int countByStatut(String statut) throws SQLException {

        String sql = "SELECT COUNT(*) FROM paiement WHERE statut = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, statut);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }

        return 0;
    }

    // ---------------- GET ALL + JOINTURE ----------------
    public List<Paiement> getAll() throws SQLException {

        String sql = """
            SELECT p.*, u.nom 
            FROM paiement p
            JOIN users u ON p.user_id = u.id_user
        """;

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Paiement> list = new ArrayList<>();

        while (rs.next()) {
            list.add(new Paiement(
                    rs.getInt("id"),
                    rs.getDouble("montant"),
                    rs.getDate("date_paiement").toLocalDate(),
                    rs.getString("mode_paiement"),
                    rs.getString("statut"),
                    rs.getString("reference"),
                    rs.getInt("user_id"),
                    rs.getString("nom")
            ));
        }
        return list;
    }

    // ---------------- SEARCH ----------------
    public List<Paiement> search(String keyword) throws SQLException {

        String sql = """
            SELECT p.*, u.nom 
            FROM paiement p
            JOIN users u ON p.user_id = u.id_user
            WHERE p.reference LIKE ? 
            OR p.statut LIKE ?
            OR u.nom LIKE ?
        """;

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ps.setString(2, "%" + keyword + "%");
        ps.setString(3, "%" + keyword + "%");

        ResultSet rs = ps.executeQuery();

        List<Paiement> list = new ArrayList<>();

        while (rs.next()) {
            list.add(new Paiement(
                    rs.getInt("id"),
                    rs.getDouble("montant"),
                    rs.getDate("date_paiement").toLocalDate(),
                    rs.getString("mode_paiement"),
                    rs.getString("statut"),
                    rs.getString("reference"),
                    rs.getInt("user_id"),
                    rs.getString("nom")
            ));
        }
        return list;
    }

    // ---------------- TOTAL REVENUE ----------------
    public double getTotalRevenue() throws SQLException {

        String sql = "SELECT SUM(montant) FROM paiement";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    // ---------------- REVENUE BY MONTH ----------------
    public Map<String, Double> getMonthlyRevenue() throws SQLException {

        String sql = """
            SELECT MONTH(date_paiement) as mois, SUM(montant) as total
            FROM paiement
            GROUP BY MONTH(date_paiement)
        """;

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        Map<String, Double> map = new HashMap<>();

        while (rs.next()) {
            map.put("M" + rs.getInt("mois"), rs.getDouble("total"));
        }

        return map;
    }
    public double getMonthlyRevenue(int month) throws SQLException {

        String sql = """
        SELECT SUM(montant) 
        FROM paiement
        WHERE MONTH(date_paiement) = ?
        AND statut = 'REUSSI'
    """;

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, month);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble(1);
        }

        return 0;
    }
}