package entities;

public class Participation {

    private int id_p;
    private Evenement evenement;        // objet Evenement au lieu de idEvenement
    private UserUnified utilisateur;           // objet User au lieu de idUtilisateur
    private String modeparticipation;
    private String objectif;

    public Participation() {}

    // Constructeur pour insertion (sans id_p)
    public Participation(Evenement evenement, UserUnified utilisateur, String modeparticipation, String objectif) {
        this.evenement = evenement;
        this.utilisateur = utilisateur;
        this.modeparticipation = modeparticipation;
        this.objectif = objectif;
    }

    // Constructeur complet (lecture BDD)
    public Participation(int id_p, Evenement evenement, UserUnified utilisateur, String modeparticipation, String objectif) {
        this.id_p = id_p;
        this.evenement = evenement;
        this.utilisateur = utilisateur;
        this.modeparticipation = modeparticipation;
        this.objectif = objectif;
    }

    // Getters & Setters
    public int getId_p() { return id_p; }
    public void setId_p(int id_p) { this.id_p = id_p; }

    public Evenement getEvenement() { return evenement; }
    public void setEvenement(Evenement evenement) { this.evenement = evenement; }

    public UserUnified getUtilisateur() { return utilisateur; }
    public void setUtilisateur(UserUnified utilisateur) { this.utilisateur = utilisateur; }

    public String getModeparticipation() { return modeparticipation; }
    public void setModeparticipation(String modeparticipation) { this.modeparticipation = modeparticipation; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    // Helpers de compatibilité pour le service (évite de tout casser)
    public int getIdEvenement()    { return evenement   != null ? evenement.getId_e()     : -1; }
    public int getIdUtilisateur()  { return utilisateur != null ? utilisateur.getId() : -1; }

    @Override
    public String toString() {
        return "Participation{id_p=" + id_p +
                ", evenement=" + (evenement != null ? evenement.getTitre_e() : "null") +
                ", utilisateur=" + (utilisateur != null ? utilisateur.getEmail() : "null") +
                ", mode='" + modeparticipation + '\'' +
                ", objectif='" + objectif + '\'' + '}';
    }
}