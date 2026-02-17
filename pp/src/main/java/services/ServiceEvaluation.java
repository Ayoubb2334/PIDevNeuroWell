package services;

import entities.Evaluation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvaluation implements IService<Evaluation> {

    private Connection connection;

    public ServiceEvaluation() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // =========================
    // CREATE
    // =========================
    @Override
    public void ajouter(Evaluation e) throws SQLException {

        String req = "INSERT INTO evaluation(type_test, score, niveau, date_evaluation, id_user) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, e.getTypeTest());
        ps.setInt(2, e.getScore());
        ps.setString(3, e.getNiveau());
        ps.setDate(4, e.getDateEvaluation());
        ps.setInt(5, e.getIdUser());

        ps.executeUpdate();
        System.out.println("Evaluation ajoutée");
    }

    // =========================
    // UPDATE
    // =========================
    @Override
    public void modifier(Evaluation e) throws SQLException {

        String req = "UPDATE evaluation SET type_test=?, score=?, niveau=?, date_evaluation=?, id_user=? " +
                "WHERE id_evaluation=?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, e.getTypeTest());
        ps.setInt(2, e.getScore());
        ps.setString(3, e.getNiveau());
        ps.setDate(4, e.getDateEvaluation());
        ps.setInt(5, e.getIdUser());
        ps.setInt(6, e.getIdEvaluation());

        ps.executeUpdate();
        System.out.println("Evaluation modifiée");
    }

    // =========================
    // DELETE
    // =========================
    @Override
    public void supprimer(Evaluation e) throws SQLException {

        String req = "DELETE FROM evaluation WHERE id_evaluation=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, e.getIdEvaluation());
        ps.executeUpdate();

        System.out.println("Evaluation supprimée");
    }

    // =========================
    // READ
    // =========================
    @Override
    public List<Evaluation> recuperer() throws SQLException {

        List<Evaluation> evaluations = new ArrayList<>();
        String req = "SELECT * FROM evaluation";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Evaluation e = new Evaluation(
                    rs.getInt("id_evaluation"),
                    rs.getString("type_test"),
                    rs.getInt("score"),
                    rs.getString("niveau"),
                    rs.getDate("date_evaluation"),
                    rs.getInt("id_user")
            );
            evaluations.add(e);
        }

        return evaluations;
    }
}

