package services;

import entities.Consultation;
import utils.MyDatabase;

import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService {

    private Connection cnx;

    public ConsultationService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // ================= AJOUTER =================
    public void ajouter(Consultation c) throws SQLException {

        LocalDateTime dateTime =
                LocalDateTime.of(c.getDateConsultation(), c.getHeureConsultation());

        // ❌ Interdire date passée
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Impossible d'ajouter une consultation passée."
            );
        }

        String sql = "INSERT INTO consultation " +
                "(id_user, id_psychologue, date_consultation, heure_consultation, type_consultation, statut) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, c.getIdUser());
            ps.setInt(2, c.getIdPsychologue());
            ps.setDate(3, Date.valueOf(c.getDateConsultation()));
            ps.setTime(4, Time.valueOf(c.getHeureConsultation()));
            ps.setString(5, c.getTypeConsultation());
            ps.setString(6, c.getStatut());

            ps.executeUpdate();
        }
    }

    // ================= AFFICHER =================
    public List<Consultation> afficher() throws SQLException {

        List<Consultation> list = new ArrayList<>();
        String sql = "SELECT * FROM consultation";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Consultation c = new Consultation();

                c.setIdConsultation(rs.getInt("id_consultation"));
                c.setIdUser(rs.getInt("id_user"));
                c.setIdPsychologue(rs.getInt("id_psychologue"));
                c.setDateConsultation(rs.getDate("date_consultation").toLocalDate());
                c.setHeureConsultation(rs.getTime("heure_consultation").toLocalTime());
                c.setTypeConsultation(rs.getString("type_consultation"));
                c.setStatut(rs.getString("statut"));

                list.add(c);
            }
        }

        return list;
    }

    // ================= SUPPRIMER =================
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM consultation WHERE id_consultation = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ================= MODIFIER =================
    public void modifier(Consultation c) throws SQLException {

        LocalDateTime nouvelleDateTime =
                LocalDateTime.of(c.getDateConsultation(), c.getHeureConsultation());

        LocalDateTime maintenant = LocalDateTime.now();

        // ❌ Interdire date passée
        if (nouvelleDateTime.isBefore(maintenant)) {
            throw new IllegalArgumentException(
                    "Impossible de modifier vers une date passée."
            );
        }

        String sql = "UPDATE consultation SET " +
                "id_user=?, id_psychologue=?, date_consultation=?, heure_consultation=?, type_consultation=?, statut=? " +
                "WHERE id_consultation=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, c.getIdUser());
            ps.setInt(2, c.getIdPsychologue());
            ps.setDate(3, Date.valueOf(c.getDateConsultation()));
            ps.setTime(4, Time.valueOf(c.getHeureConsultation()));
            ps.setString(5, c.getTypeConsultation());
            ps.setString(6, c.getStatut());
            ps.setInt(7, c.getIdConsultation());

            ps.executeUpdate();
        }
    }

    // ================= DOUBLE RÉSERVATION =================
    public boolean existeConsultation(int idPsychologue, LocalDate date, LocalTime heure) throws SQLException {

        String sql = "SELECT heure_consultation FROM consultation " +
                "WHERE id_psychologue = ? AND date_consultation = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, idPsychologue);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    LocalTime heureExistante =
                            rs.getTime("heure_consultation").toLocalTime();

                    long difference = Math.abs(
                            Duration.between(heureExistante, heure).toMinutes()
                    );

                    if (difference < 15) {
                        return true; // conflit
                    }
                }
            }
        }

        return false;
    }
}
