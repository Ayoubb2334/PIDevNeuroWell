package services;

import entities.Participation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceParticipation implements IService<Participation> {

    private final Connection connection;

    public ServiceParticipation() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // Vérifie si l'événement existe
    private boolean evenementExiste(int idEvenement) throws SQLException {
        String sql = "SELECT id_e FROM évenements WHERE id_e = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idEvenement);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Vérifie si l'utilisateur existe
    private boolean utilisateurExiste(int idUtilisateur) throws SQLException {
        String sql = "SELECT id_user FROM users WHERE id_user= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void ajouter(Participation p) throws SQLException {
        String req = "INSERT INTO participation (id_user, id_e, modeparticipation, objectif) VALUES (?, ?, ?, ?)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setInt(1, p.getIdUtilisateur());
            ps.setInt(2, p.getIdEvenement());

            // Vérification de l'enum pour éviter les erreurs SQL
            String mode = p.getModeparticipation().toLowerCase();
            if (!mode.equals("presentiel") && !mode.equals("distanciel")) {
                throw new SQLException("Mode de participation invalide : " + mode);
            }
            ps.setString(3, mode);

            ps.setString(4, p.getObjectif());

            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Participation p) throws SQLException {
        if (!evenementExiste(p.getIdEvenement())) {
            throw new SQLException("L'événement avec id = " + p.getIdEvenement() + " n'existe pas");
        }

        if (!utilisateurExiste(p.getIdUtilisateur())) {
            throw new SQLException("L'utilisateur avec id = " + p.getIdUtilisateur() + " n'existe pas");
        }

        String sql = "UPDATE participation SET id_e = ?, id_user = ?, modeparticipation = ?, objectif = ? WHERE id_p = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, p.getIdEvenement());
            ps.setInt(2, p.getIdUtilisateur());
            ps.setString(3, p.getModeparticipation());
            ps.setString(4, p.getObjectif());
            ps.setInt(5, p.getId_p());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Modification impossible, la participation n'existe pas.");
            }

            System.out.println("✅ Participation modifiée : " + p);
        }
    }

    @Override
    public void supprimer(Participation p) throws SQLException {
        String sql = "DELETE FROM participation WHERE id_p = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, p.getId_p());
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new SQLException("Suppression impossible, la participation n'existe pas.");
            }
            System.out.println("✅ Participation supprimée : " + p);
        }
    }

    @Override
    public List<Participation> recuperer() throws SQLException {
        List<Participation> list = new ArrayList<>();
        String sql = "SELECT * FROM participation";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Participation p = new Participation(
                        rs.getInt("id_p"),
                        rs.getInt("id_e"),
                        rs.getInt("id_user"),
                        rs.getString("modeparticipation"),
                        rs.getString("objectif")
                );
                list.add(p);
            }
        }
        return list;
    }
}
