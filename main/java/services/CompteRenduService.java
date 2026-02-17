package services;

import entities.CompteRendu;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompteRenduService {

    Connection cnx;

    public CompteRenduService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // ================= AJOUTER =================
    public void ajouter(CompteRendu cr) {

        String sql = "INSERT INTO compte_rendu (id_consultation, description, date_redaction) VALUES (?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setInt(1, cr.getIdConsultation());
            ps.setString(2, cr.getDescription());
            ps.setDate(3, Date.valueOf(cr.getDateRedaction()));

            ps.executeUpdate();
            System.out.println("Compte rendu ajouté !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= AFFICHER =================
    public List<CompteRendu> afficher() {

        List<CompteRendu> list = new ArrayList<>();
        String sql = "SELECT * FROM compte_rendu";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                CompteRendu cr = new CompteRendu();

                cr.setIdCompteRendu(rs.getInt("id_compte_rendu"));
                cr.setIdConsultation(rs.getInt("id_consultation"));
                cr.setDescription(rs.getString("description"));
                cr.setDateRedaction(rs.getDate("date_redaction").toLocalDate());

                list.add(cr);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    // ================= SUPPRIMER =================
    public void supprimer(int id) {

        String sql = "DELETE FROM compte_rendu WHERE id_compte_rendu = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("Compte rendu supprimé !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= MODIFIER =================
    public void modifier(CompteRendu cr) {

        String sql = "UPDATE compte_rendu SET id_consultation=?, description=?, date_redaction=? WHERE id_compte_rendu=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setInt(1, cr.getIdConsultation());
            ps.setString(2, cr.getDescription());
            ps.setDate(3, Date.valueOf(cr.getDateRedaction()));
            ps.setInt(4, cr.getIdCompteRendu());

            ps.executeUpdate();

            System.out.println("Compte rendu modifié !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
