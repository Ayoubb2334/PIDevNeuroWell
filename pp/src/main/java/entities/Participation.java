package entities;

public class Participation {

    private int id_p;                  // clé primaire auto-incrémentée
    private int idEvenement;           // id de l'événement
    private int idUtilisateur;         // id de l'utilisateur
    private String modeparticipation;  // 'presentiel' ou 'distanciel'
    private String objectif;           // texte libre

    // Constructeur vide
    public Participation() {}

    // Constructeur sans id_p (pour insertion)
    public Participation(int idEvenement, int idUtilisateur, String modeparticipation, String objectif) {
        this.idEvenement = idEvenement;
        this.idUtilisateur = idUtilisateur;
        this.modeparticipation = modeparticipation;
        this.objectif = objectif;
    }

    // Constructeur complet avec id_p (lecture depuis la BDD)
    public Participation(int id_p, int idEvenement, int idUtilisateur, String modeparticipation, String objectif) {
        this.id_p = id_p;
        this.idEvenement = idEvenement;
        this.idUtilisateur = idUtilisateur;
        this.modeparticipation = modeparticipation;
        this.objectif = objectif;
    }

    // Getters et setters
    public int getId_p() { return id_p; }
    public void setId_p(int id_p) { this.id_p = id_p; }

    public int getIdEvenement() { return idEvenement; }
    public void setIdEvenement(int idEvenement) { this.idEvenement = idEvenement; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getModeparticipation() { return modeparticipation; }
    public void setModeparticipation(String modeparticipation) { this.modeparticipation = modeparticipation; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    @Override
    public String toString() {
        return "Participation{" +
                "id_p=" + id_p +
                ", idEvenement=" + idEvenement +
                ", idUtilisateur=" + idUtilisateur +
                ", modeparticipation='" + modeparticipation + '\'' +
                ", objectif='" + objectif + '\'' +
                '}';
    }
}
