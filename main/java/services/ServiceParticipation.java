package services;

import entities.Evenement;
import entities.Participation;
import entities.UserUnified;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceParticipation implements IService<Participation> {

    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private Evenement mapEvenement(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId_e(rs.getInt("id_e"));
        e.setTitre_e(rs.getString("titre_e"));
        e.setType_e(rs.getString("type_e"));
        e.setLocalisation_e(rs.getString("localisation_e"));
        e.setPrix_e(rs.getString("prix_e"));
        e.setDate_e(rs.getTimestamp("date_e"));
        e.setCapacitemax_e(rs.getInt("capacitemax_e"));
        e.setDescription_e(rs.getString("description_e"));
        e.setImage(rs.getString("image"));
        e.setStatut_e(rs.getString("statut_e"));
        return e;
    }

    private UserUnified mapUser(ResultSet rs) throws SQLException {
        UserUnified  u = new UserUnified ();
        u.setId(rs.getInt("id_user"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        return u;
    }

    private Participation mapParticipation(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId_p(rs.getInt("id_p"));
        p.setModeparticipation(rs.getString("modeparticipation"));
        p.setObjectif(rs.getString("objectif"));
        p.setEvenement(mapEvenement(rs));
        p.setUtilisateur(mapUser(rs));
        return p;
    }

    // SQL avec JOIN ─────────────────────────────────────────────────────────────
    private static final String SELECT_WITH_JOIN =
            "SELECT p.id_p, p.modeparticipation, p.objectif, " +
                    "       e.id_e, e.titre_e, e.type_e, e.localisation_e, e.prix_e, " +
                    "       e.date_e, e.capacitemax_e, e.description_e, e.image, e.statut_e, " +
                    "       u.id_user, u.nom, u.prenom, u.email " +
                    "FROM participation p " +
                    "JOIN évenements e ON p.id_e  = e.id_e " +
                    "JOIN users      u ON p.id_user = u.id_user";

    // ── Vérifications ─────────────────────────────────────────────────────────

    private boolean evenementExiste(int idEvenement) throws SQLException {
        String sql = "SELECT id_e FROM évenements WHERE id_e = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idEvenement);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean utilisateurExiste(int idUtilisateur) throws SQLException {
        String sql = "SELECT id_user FROM users WHERE id_user = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int countByEvenement(int idEvenement) throws SQLException {
        String sql = "SELECT COUNT(*) FROM participation WHERE id_e = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idEvenement);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public void ajouter(Participation p) throws SQLException {
        String mode = p.getModeparticipation().toLowerCase();
        if (!mode.equals("presentiel") && !mode.equals("distanciel"))
            throw new SQLException("Mode invalide : " + mode);

        String sql = "INSERT INTO participation (id_user, id_e, modeparticipation, objectif) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p.getIdUtilisateur());
            ps.setInt(2, p.getIdEvenement());
            ps.setString(3, mode);
            ps.setString(4, p.getObjectif());
            ps.executeUpdate();
            System.out.println("✅ Participation ajoutée.");
        }
    }

    @Override
    public void modifier(Participation p) throws SQLException {
        if (!evenementExiste(p.getIdEvenement()))
            throw new SQLException("Événement id=" + p.getIdEvenement() + " introuvable");
        if (!utilisateurExiste(p.getIdUtilisateur()))
            throw new SQLException("Utilisateur id=" + p.getIdUtilisateur() + " introuvable");

        String sql = "UPDATE participation SET id_e=?, id_user=?, modeparticipation=?, objectif=? WHERE id_p=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p.getIdEvenement());
            ps.setInt(2, p.getIdUtilisateur());
            ps.setString(3, p.getModeparticipation());
            ps.setString(4, p.getObjectif());
            ps.setInt(5, p.getId_p());
            if (ps.executeUpdate() == 0)
                throw new SQLException("Modification impossible, participation introuvable.");
            System.out.println("✅ Participation modifiée : " + p);
        }
    }

    @Override
    public void supprimer(Participation p) throws SQLException {
        String sql = "DELETE FROM participation WHERE id_p = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p.getId_p());
            if (ps.executeUpdate() == 0)
                throw new SQLException("Suppression impossible, participation introuvable.");
            System.out.println("✅ Participation supprimée : " + p);
        }
    }

    @Override
    public List<Participation> recuperer() throws SQLException {
        List<Participation> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(SELECT_WITH_JOIN)) {
            while (rs.next()) list.add(mapParticipation(rs));
        }
        return list;
    }

    public List<Participation> recupererParUser(int idUser) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String sql = SELECT_WITH_JOIN + " WHERE p.id_user = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapParticipation(rs));
            }
        }
        return list;
    }
}