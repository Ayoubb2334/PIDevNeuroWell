package entities;

import java.sql.Date;

public class Evaluation {

    // =========================
    // ATTRIBUTS (colonnes BD)
    // =========================

    private int idEvaluation;      // PK
    private String typeTest;       // Stress, Anxiété, Bien-être...
    private int score;             // Score du test
    private String niveau;         // Faible, Moyen, Élevé
    private Date dateEvaluation;   // Date du test
    private int idUser;            // FK vers utilisateur

    // =========================
    // CONSTRUCTEURS
    // =========================

    // Constructeur vide (OBLIGATOIRE pour JDBC)
    public Evaluation() {
    }

    // Constructeur SANS id (pour INSERT)
    public Evaluation(String typeTest, int score, String niveau,
                      Date dateEvaluation, int idUser) {
        this.typeTest = typeTest;
        this.score = score;
        this.niveau = niveau;
        this.dateEvaluation = dateEvaluation;
        this.idUser = idUser;
    }

    // Constructeur AVEC id (pour UPDATE / SELECT)
    public Evaluation(int idEvaluation, String typeTest, int score,
                      String niveau, Date dateEvaluation, int idUser) {
        this.idEvaluation = idEvaluation;
        this.typeTest = typeTest;
        this.score = score;
        this.niveau = niveau;
        this.dateEvaluation = dateEvaluation;
        this.idUser = idUser;
    }

    // =========================
    // GETTERS & SETTERS
    // =========================

    public int getIdEvaluation() {
        return idEvaluation;
    }

    public void setIdEvaluation(int idEvaluation) {
        this.idEvaluation = idEvaluation;
    }

    public String getTypeTest() {
        return typeTest;
    }

    public void setTypeTest(String typeTest) {
        this.typeTest = typeTest;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public Date getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(Date dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    // =========================
    // toString (pour affichage)
    // =========================

    @Override
    public String toString() {
        return "Evaluation{" +
                "idEvaluation=" + idEvaluation +
                ", typeTest='" + typeTest + '\'' +
                ", score=" + score +
                ", niveau='" + niveau + '\'' +
                ", dateEvaluation=" + dateEvaluation +
                ", idUser=" + idUser +
                '}';
    }
}

