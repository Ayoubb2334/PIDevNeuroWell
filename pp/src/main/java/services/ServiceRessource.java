package services;

import entities.Ressource;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRessource implements IService<Ressource> {

    private Connection connection;

    public ServiceRessource() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // =========================
    // CREATE
    // =========================
    @Override
    public void ajouter(Ressource r) throws SQLException {
        String req = "INSERT INTO ressource(titre, description, type, chemin_fichier, " +
                     "taille_fichier, format, date_publication, statut, id_user) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, r.getTitre());
        ps.setString(2, r.getDescription());
        ps.setString(3, r.getType());
        ps.setString(4, r.getCheminFichier());
        ps.setDouble(5, r.getTailleFichier());
        ps.setString(6, r.getFormat());
        ps.setDate(7, r.getDatePublication() != null ? r.getDatePublication() : new Date(System.currentTimeMillis()));
        ps.setString(8, r.getStatut());
        ps.setInt(9, r.getIdUser());

        ps.executeUpdate();
        System.out.println("Ressource ajoutée avec succès");
    }

    // =========================
    // UPDATE
    // =========================
    @Override
    public void modifier(Ressource r) throws SQLException {
        String req = "UPDATE ressource SET titre=?, description=?, type=?, chemin_fichier=?, " +
                     "taille_fichier=?, format=?, statut=? WHERE id_ressource=?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, r.getTitre());
        ps.setString(2, r.getDescription());
        ps.setString(3, r.getType());
        ps.setString(4, r.getCheminFichier());
        ps.setDouble(5, r.getTailleFichier());
        ps.setString(6, r.getFormat());
        ps.setString(7, r.getStatut());
        ps.setInt(8, r.getIdRessource());

        ps.executeUpdate();
        System.out.println("Ressource modifiée avec succès");
    }

    // =========================
    // DELETE
    // =========================
    @Override
    public void supprimer(Ressource r) throws SQLException {
        String req = "DELETE FROM ressource WHERE id_ressource=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, r.getIdRessource());
        ps.executeUpdate();
        System.out.println("Ressource supprimée avec succès");
    }

    // =========================
    // READ ALL
    // =========================
    @Override
    public List<Ressource> recuperer() throws SQLException {
        List<Ressource> ressources = new ArrayList<>();
        String req = "SELECT * FROM ressource ORDER BY date_publication DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Ressource r = new Ressource(
                    rs.getInt("id_ressource"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getString("chemin_fichier"),
                    rs.getDouble("taille_fichier"),
                    rs.getString("format"),
                    rs.getDate("date_publication"),
                    rs.getString("statut"),
                    rs.getInt("id_user")
            );
            ressources.add(r);
        }

        return ressources;
    }

    // =========================
    // READ BY ID
    // =========================
    public Ressource recupererParId(int id) throws SQLException {
        String req = "SELECT * FROM ressource WHERE id_ressource=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Ressource(
                    rs.getInt("id_ressource"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getString("chemin_fichier"),
                    rs.getDouble("taille_fichier"),
                    rs.getString("format"),
                    rs.getDate("date_publication"),
                    rs.getString("statut"),
                    rs.getInt("id_user")
            );
        }
        return null;
    }

    // =========================
    // RECHERCHE PAR TYPE
    // =========================
    public List<Ressource> rechercherParType(String type) throws SQLException {
        List<Ressource> ressources = new ArrayList<>();
        String req = "SELECT * FROM ressource WHERE type=? ORDER BY date_publication DESC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Ressource r = new Ressource(
                    rs.getInt("id_ressource"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getString("chemin_fichier"),
                    rs.getDouble("taille_fichier"),
                    rs.getString("format"),
                    rs.getDate("date_publication"),
                    rs.getString("statut"),
                    rs.getInt("id_user")
            );
            ressources.add(r);
        }
        return ressources;
    }

    // =========================
    // RECHERCHE PAR STATUT
    // =========================
    public List<Ressource> rechercherParStatut(String statut) throws SQLException {
        List<Ressource> ressources = new ArrayList<>();
        String req = "SELECT * FROM ressource WHERE statut=? ORDER BY date_publication DESC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, statut);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Ressource r = new Ressource(
                    rs.getInt("id_ressource"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getString("chemin_fichier"),
                    rs.getDouble("taille_fichier"),
                    rs.getString("format"),
                    rs.getDate("date_publication"),
                    rs.getString("statut"),
                    rs.getInt("id_user")
            );
            ressources.add(r);
        }
        return ressources;
    }
}
