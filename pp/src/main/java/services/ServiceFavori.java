package services;

import entities.Ressource;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceFavori {

    private final Connection connection;

    public ServiceFavori() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ──────────────────────────────────────────────────────
    //  Ajouter un favori
    // ──────────────────────────────────────────────────────
    public void ajouterFavori(int idUser, int idRessource) throws SQLException {
        String req = "INSERT IGNORE INTO ressource_favori(id_user, id_ressource) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idRessource);
            ps.executeUpdate();
            System.out.println("[FAVORI] Ajouté — user=" + idUser + " ressource=" + idRessource);
        }
    }

    // ──────────────────────────────────────────────────────
    //  Supprimer un favori
    // ──────────────────────────────────────────────────────
    public void supprimerFavori(int idUser, int idRessource) throws SQLException {
        String req = "DELETE FROM ressource_favori WHERE id_user=? AND id_ressource=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idRessource);
            ps.executeUpdate();
            System.out.println("[FAVORI] Supprimé — user=" + idUser + " ressource=" + idRessource);
        }
    }

    // ──────────────────────────────────────────────────────
    //  Toggle
    // ──────────────────────────────────────────────────────
    public boolean toggleFavori(int idUser, int idRessource) throws SQLException {
        System.out.println("[FAVORI] toggleFavori() — idUser=" + idUser + " idRessource=" + idRessource);
        boolean dejafav = estFavori(idUser, idRessource);
        System.out.println("[FAVORI] estFavori avant toggle = " + dejafav);
        if (dejafav) {
            supprimerFavori(idUser, idRessource);
            return false;
        } else {
            ajouterFavori(idUser, idRessource);
            return true;
        }
    }

    // ──────────────────────────────────────────────────────
    //  Vérifier si une ressource est en favori
    // ──────────────────────────────────────────────────────
    public boolean estFavori(int idUser, int idRessource) throws SQLException {
        String req = "SELECT 1 FROM ressource_favori WHERE id_user=? AND id_ressource=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idRessource);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    // ──────────────────────────────────────────────────────
    //  IDs favoris d'un utilisateur
    // ──────────────────────────────────────────────────────
    public java.util.Set<Integer> getIdsFavorisParUser(int idUser) throws SQLException {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String req = "SELECT id_ressource FROM ressource_favori WHERE id_user=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_ressource"));
        }
        System.out.println("[FAVORI] getIdsFavorisParUser(" + idUser + ") = " + ids);
        return ids;
    }

    // ──────────────────────────────────────────────────────
    //  Favoris complets d'un utilisateur
    // ──────────────────────────────────────────────────────
    public List<Ressource> getFavorisParUser(int idUser) throws SQLException {
        List<Ressource> list = new ArrayList<>();
        String req = "SELECT r.* FROM ressource r " +
                     "JOIN ressource_favori f ON r.id_ressource = f.id_ressource " +
                     "WHERE f.id_user = ? AND r.statut = 'publi\u00e9' " +
                     "ORDER BY f.date_ajout DESC";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ──────────────────────────────────────────────────────
    //  Nombre de favoris pour une ressource
    // ──────────────────────────────────────────────────────
    public int getNbFavoris(int idRessource) throws SQLException {
        String req = "SELECT COUNT(*) FROM ressource_favori WHERE id_ressource=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idRessource);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ──────────────────────────────────────────────────────
    //  Helper
    // ──────────────────────────────────────────────────────
    private Ressource map(ResultSet rs) throws SQLException {
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
}
