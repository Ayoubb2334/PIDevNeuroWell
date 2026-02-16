package entities;

public class Participation {

    private int id_p;               // clé primaire auto-incrémentée
    private int id_e;               // id de l'événement
    private int id_u;               // id de l'utilisateur
    private String modeparticipation; // 'presentiel' ou 'en_ligne'
    private String objectif;          // texte libre

    // Constructeur vide
    public Participation() {}

    // Constructeur sans id_p (pour insertion)
    public Participation(int id_e, int id_u, String modeparticipation, String objectif) {
        this.id_e = id_e;
        this.id_u = id_u;
        this.modeparticipation = modeparticipation;
        this.objectif = objectif;
    }

    // Constructeur complet avec id_p (lecture depuis la BDD)
    public Participation(int id_p, int id_e, int id_u, String modeparticipation, String objectif) {
        this.id_p = id_p;
        this.id_e = id_e;
        this.id_u = id_u;
        this.modeparticipation = modeparticipation;
        this.objectif = objectif;
    }

    // Getters et setters
    public int getId_p() { return id_p; }
    public void setId_p(int id_p) { this.id_p = id_p; }

    public int getId_e() { return id_e; }
    public void setId_e(int id_e) { this.id_e = id_e; }

    public int getId_u() { return id_u; }
    public void setId_u(int id_u) { this.id_u = id_u; }

    public String getModeparticipation() { return modeparticipation; }
    public void setModeparticipation(String modeparticipation) { this.modeparticipation = modeparticipation; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    @Override
    public String toString() {
        return "Participation{" +
                "id_p=" + id_p +
                ", id_e=" + id_e +
                ", id_u=" + id_u +
                ", modeparticipation='" + modeparticipation + '\'' +
                ", objectif='" + objectif + '\'' +
                '}';
    }
}
