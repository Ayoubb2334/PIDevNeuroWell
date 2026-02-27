package services;

import entities.Commentaire;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire {

    private Connection connection;

    public ServiceCommentaire() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ── AJOUTER ───────────────────────────────────────────────────────
    public void ajouter(Commentaire c) throws SQLException {
        String req = "INSERT INTO commentaire (id_ressource, contenu) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, c.getIdRessource());
        ps.setString(2, c.getContenu());
        ps.executeUpdate();
        System.out.println("✅ Commentaire ajouté.");
    }

    // ── SUPPRIMER ─────────────────────────────────────────────────────
    public void supprimer(int idCommentaire) throws SQLException {
        String req = "DELETE FROM commentaire WHERE id_commentaire = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, idCommentaire);
        ps.executeUpdate();
        System.out.println("🗑️ Commentaire supprimé.");
    }

    // ── RÉCUPÉRER PAR RESSOURCE ───────────────────────────────────────
    public List<Commentaire> recupererParRessource(int idRessource) throws SQLException {
        List<Commentaire> liste = new ArrayList<>();
        String req = "SELECT * FROM commentaire WHERE id_ressource = ? ORDER BY date_commentaire ASC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, idRessource);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Timestamp ts = rs.getTimestamp("date_commentaire");
            LocalDateTime date = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

            Commentaire c = new Commentaire(
                rs.getInt("id_commentaire"),
                rs.getInt("id_ressource"),
                rs.getString("contenu"),
                date
            );
            liste.add(c);
        }
        return liste;
    }
}
