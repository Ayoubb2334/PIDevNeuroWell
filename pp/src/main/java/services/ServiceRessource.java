package services;

import entities.Ressource;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRessource implements IService<Ressource> {

    private Connection connection;

    public ServiceRessource() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════
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
        ps.setDate(7, r.getDatePublication() != null
                ? r.getDatePublication()
                : new java.sql.Date(System.currentTimeMillis()));
        ps.setString(8, r.getStatut());
        ps.setInt(9, r.getIdUser());

        ps.executeUpdate();
        System.out.println("Ressource ajoutée avec succès");
    }

    // ══════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════
    @Override
    public void supprimer(Ressource r) throws SQLException {
        String req = "DELETE FROM ressource WHERE id_ressource=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, r.getIdRessource());
        ps.executeUpdate();
        System.out.println("Ressource supprimée avec succès");
    }

    // ══════════════════════════════════════════════════════════
    //  READ ALL
    // ══════════════════════════════════════════════════════════
    @Override
    public List<Ressource> recuperer() throws SQLException {
        List<Ressource> ressources = new ArrayList<>();
        String req = "SELECT * FROM ressource ORDER BY date_publication DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            ressources.add(map(rs));
        }
        return ressources;
    }

    // ══════════════════════════════════════════════════════════
    //  READ BY ID
    // ══════════════════════════════════════════════════════════
    public Ressource recupererParId(int id) throws SQLException {
        String req = "SELECT * FROM ressource WHERE id_ressource=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? map(rs) : null;
    }

    // ══════════════════════════════════════════════════════════
    //  RECHERCHE PAR TYPE
    // ══════════════════════════════════════════════════════════
    public List<Ressource> rechercherParType(String type) throws SQLException {
        List<Ressource> ressources = new ArrayList<>();
        String req = "SELECT * FROM ressource WHERE type=? ORDER BY date_publication DESC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) ressources.add(map(rs));
        return ressources;
    }

    // ══════════════════════════════════════════════════════════
    //  RECHERCHE PAR STATUT
    // ══════════════════════════════════════════════════════════
    public List<Ressource> rechercherParStatut(String statut) throws SQLException {
        List<Ressource> ressources = new ArrayList<>();
        String req = "SELECT * FROM ressource WHERE statut=? ORDER BY date_publication DESC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, statut);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) ressources.add(map(rs));
        return ressources;
    }

    // ══════════════════════════════════════════════════════════
    //  TRACKING — enregistrer une consultation
    // ══════════════════════════════════════════════════════════

    /**
     * Insère un événement de consultation pour la ressource donnée.
     *
     * @param idRessource  ID de la ressource consultée
     * @param source       "backoffice" ou "front"
     */
    /**
     * Enregistre une consultation SANS utilisateur identifié (rétrocompatibilité).
     */
    public void enregistrerConsultation(int idRessource, String source) {
        enregistrerConsultation(idRessource, source, null);
    }

    /**
     * Enregistre une consultation avec l'utilisateur connecté.
     * Passer {@code null} pour idUser si l'utilisateur n'est pas connu.
     *
     * @param idRessource  ID de la ressource consultée
     * @param source       "backoffice" ou "front"
     * @param idUser       ID de l'utilisateur connecté (peut être null)
     */
    public void enregistrerConsultation(int idRessource, String source, Integer idUser) {
        String req = "INSERT INTO ressource_consultation(id_ressource, id_user, date_heure, source) " +
                     "VALUES (?, ?, NOW(), ?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idRessource);
            if (idUser != null) ps.setInt(2, idUser);
            else                ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, source);
            ps.executeUpdate();
            System.out.println("[TRACKING] Consultation enregistrée — id_ressource=" + idRessource
                    + " id_user=" + idUser + " source=" + source);
        } catch (SQLException e) {
            System.err.println("[TRACKING] ERREUR — SQLState=" + e.getSQLState()
                    + " code=" + e.getErrorCode()
                    + " msg=" + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  STATS — nombre total de vues par ressource
    // ══════════════════════════════════════════════════════════

    /** Retourne une Map< idRessource → totalVues >. */
    public Map<Integer, Integer> getTotalVuesParRessource() throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        String req = "SELECT id_ressource, COUNT(*) AS total " +
                     "FROM ressource_consultation GROUP BY id_ressource";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            map.put(rs.getInt("id_ressource"), rs.getInt("total"));
        }
        return map;
    }

    /** Retourne le nombre de vues d'une ressource précise sur les N derniers jours. */
    public int getVuesDerniersJours(int idRessource, int jours) throws SQLException {
        String req = "SELECT COUNT(*) FROM ressource_consultation " +
                     "WHERE id_ressource=? AND date_heure >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, idRessource);
        ps.setInt(2, jours);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    /**
     * Retourne les N ressources les plus consultées (tous temps confondus).
     * Chaque entrée : [id_ressource, titre, type, total_vues]
     */
    public List<Object[]> getTopRessources(int limit) throws SQLException {
        List<Object[]> result = new ArrayList<>();
        String req = "SELECT r.id_ressource, r.titre, r.type, COUNT(c.id) AS total " +
                     "FROM ressource r " +
                     "LEFT JOIN ressource_consultation c ON r.id_ressource = c.id_ressource " +
                     "GROUP BY r.id_ressource, r.titre, r.type " +
                     "ORDER BY total DESC LIMIT ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, limit);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            result.add(new Object[]{
                rs.getInt("id_ressource"),
                rs.getString("titre"),
                rs.getString("type"),
                rs.getInt("total")
            });
        }
        return result;
    }

    /** Retourne le nombre de vues agrégées par jour sur les N derniers jours. */
    public List<Object[]> getVuesParJour(int dernierJours) throws SQLException {
        List<Object[]> result = new ArrayList<>();
        String req = "SELECT DATE(date_heure) AS jour, COUNT(*) AS total " +
                     "FROM ressource_consultation " +
                     "WHERE date_heure >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                     "GROUP BY jour ORDER BY jour ASC";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, dernierJours);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            result.add(new Object[]{ rs.getString("jour"), rs.getInt("total") });
        }
        return result;
    }

    /** Retourne le nombre de vues agrégées par type de ressource. */
    public List<Object[]> getVuesParType() throws SQLException {
        List<Object[]> result = new ArrayList<>();
        String req = "SELECT r.type, COUNT(c.id) AS total " +
                     "FROM ressource r " +
                     "LEFT JOIN ressource_consultation c ON r.id_ressource = c.id_ressource " +
                     "GROUP BY r.type ORDER BY total DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            result.add(new Object[]{ rs.getString("type"), rs.getInt("total") });
        }
        return result;
    }

    /** Nombre total de consultations (tous temps, toutes ressources). */
    public int getTotalConsultations() throws SQLException {
        String req = "SELECT COUNT(*) FROM ressource_consultation";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ══════════════════════════════════════════════════════════
    //  ★  RECOMMANDATIONS PERSONNALISÉES
    // ══════════════════════════════════════════════════════════

    /**
     * Retourne le type de ressource le plus consulté par un utilisateur donné.
     * Utilisé pour orienter les recommandations.
     *
     * @param idUser  ID de l'utilisateur connecté
     * @return le type favori (ex : "Audio"), ou {@code null} si aucun historique
     */
    public String getTypeFavoriUser(int idUser) throws SQLException {
        String req = "SELECT r.type, COUNT(*) AS total " +
                     "FROM ressource_consultation c " +
                     "JOIN ressource r ON r.id_ressource = c.id_ressource " +
                     "WHERE c.id_user = ? " +
                     "GROUP BY r.type ORDER BY total DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("type") : null;
        }
    }

    /**
     * Recommandations basées sur le type préféré de l'utilisateur.
     * Retourne les {@code limit} ressources publiées du même type,
     * triées par popularité globale (toutes consultations confondues).
     * Les ressources déjà consultées par l'utilisateur sont exclues.
     *
     * @param idUser  ID de l'utilisateur connecté
     * @param limit   Nombre max de résultats
     */
    public List<Ressource> getRecommandationsParHistorique(int idUser, int limit) throws SQLException {
        String typeFavori = getTypeFavoriUser(idUser);
        if (typeFavori == null) {
            // Pas d'historique : retourner les plus consultées toutes catégories
            return getTopRessourcesPubliees(limit);
        }

        List<Ressource> result = new ArrayList<>();
        String req = "SELECT r.* FROM ressource r " +
                     "LEFT JOIN ressource_consultation c ON r.id_ressource = c.id_ressource " +
                     "WHERE r.type = ? AND r.statut = 'publié' " +
                     "AND r.id_ressource NOT IN ( " +
                     "    SELECT DISTINCT id_ressource FROM ressource_consultation WHERE id_user = ? " +
                     ") " +
                     "GROUP BY r.id_ressource " +
                     "ORDER BY COUNT(c.id) DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, typeFavori);
            ps.setInt(2, idUser);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(map(rs));
        }
        // Compléter si moins de `limit` résultats dans ce type
        if (result.size() < limit) {
            List<Ressource> complements = getTopRessourcesPubliees(limit - result.size());
            java.util.Set<Integer> dejaDedans = new java.util.HashSet<>();
            result.forEach(r -> dejaDedans.add(r.getIdRessource()));
            complements.stream()
                       .filter(r -> !dejaDedans.contains(r.getIdRessource()))
                       .forEach(result::add);
        }
        return result;
    }

    /**
     * Ressources "Tendance" : les plus consultées sur les 7 derniers jours.
     * Un badge "🔥 Tendance" pourra être affiché côté front si le seuil est dépassé.
     *
     * @param limit            Nombre max de résultats
     * @param seuilVuesSemaine Seuil minimum de vues sur 7 jours pour être "tendance"
     */
    public List<Ressource> getRessourcesTendance(int limit, int seuilVuesSemaine) throws SQLException {
        List<Ressource> result = new ArrayList<>();
        String req = "SELECT r.*, COUNT(c.id) AS vues_semaine " +
                     "FROM ressource r " +
                     "JOIN ressource_consultation c ON r.id_ressource = c.id_ressource " +
                     "WHERE r.statut = 'publié' " +
                     "  AND c.date_heure >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                     "GROUP BY r.id_ressource " +
                     "HAVING vues_semaine >= ? " +
                     "ORDER BY vues_semaine DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, seuilVuesSemaine);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    /**
     * Retourne les IDs des ressources tendance cette semaine (pour affichage badge rapide).
     *
     * @param seuilVuesSemaine Seuil minimum de vues sur 7 jours
     */
    public java.util.Set<Integer> getIdsTendance(int seuilVuesSemaine) throws SQLException {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String req = "SELECT r.id_ressource " +
                     "FROM ressource r " +
                     "JOIN ressource_consultation c ON r.id_ressource = c.id_ressource " +
                     "WHERE c.date_heure >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                     "GROUP BY r.id_ressource " +
                     "HAVING COUNT(c.id) >= ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, seuilVuesSemaine);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_ressource"));
        }
        return ids;
    }

    /**
     * Helper interne — top ressources publiées par popularité globale.
     */
    private List<Ressource> getTopRessourcesPubliees(int limit) throws SQLException {
        List<Ressource> result = new ArrayList<>();
        String req = "SELECT r.* FROM ressource r " +
                     "LEFT JOIN ressource_consultation c ON r.id_ressource = c.id_ressource " +
                     "WHERE r.statut = 'publi\u00e9' " +
                     "GROUP BY r.id_ressource " +
                     "ORDER BY COUNT(c.id) DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════
    //  HELPER — mapping ResultSet → Ressource
    // ══════════════════════════════════════════════════════════
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
