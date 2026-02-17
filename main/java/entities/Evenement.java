package entities;

import java.sql.Timestamp;

public class Evenement {

    private int id_e;
    private String titre_e;
    private String description_e;
    private Timestamp date_e;
    private String localisation_e;
    private String type_e;
    private int capacitemax_e;
    private String statut_e;
    private String prix_e;   // مطابق للـ SQL
    private String image;    // مطابق للـ SQL

    public Evenement() {
    }

    // ✅ Constructeur SANS ID
    public Evenement(String titre_e, String description_e, Timestamp date_e,
                     String localisation_e, String type_e,
                     int capacitemax_e, String statut_e, String prix_e, String image) {

        this.titre_e = titre_e;
        this.description_e = description_e;
        this.date_e = date_e;
        this.localisation_e = localisation_e;
        this.type_e = type_e;
        this.capacitemax_e = capacitemax_e;
        this.statut_e = statut_e;
        this.prix_e = prix_e;
        this.image = image;
    }

    // ✅ Constructeur AVEC ID
    public Evenement(int id_e, String titre_e, String description_e, Timestamp date_e,
                     String localisation_e, String type_e,
                     int capacitemax_e, String statut_e, String prix_e, String image) {

        this.id_e = id_e;
        this.titre_e = titre_e;
        this.description_e = description_e;
        this.date_e = date_e;
        this.localisation_e = localisation_e;
        this.type_e = type_e;
        this.capacitemax_e = capacitemax_e;
        this.statut_e = statut_e;
        this.prix_e = prix_e;
        this.image = image;
    }

    public int getId_e() {
        return id_e;
    }

    public void setId_e(int id_e) {
        this.id_e = id_e;
    }

    public String getTitre_e() {
        return titre_e;
    }

    public void setTitre_e(String titre_e) {
        this.titre_e = titre_e;
    }

    public String getDescription_e() {
        return description_e;
    }

    public void setDescription_e(String description_e) {
        this.description_e = description_e;
    }

    public Timestamp getDate_e() {
        return date_e;
    }

    public void setDate_e(Timestamp date_e) {
        this.date_e = date_e;
    }

    public String getLocalisation_e() {
        return localisation_e;
    }

    public void setLocalisation_e(String localisation_e) {
        this.localisation_e = localisation_e;
    }

    public String getType_e() {
        return type_e;
    }

    public void setType_e(String type_e) {
        this.type_e = type_e;
    }

    public int getCapacitemax_e() {
        return capacitemax_e;
    }

    public void setCapacitemax_e(int capacitemax_e) {
        this.capacitemax_e = capacitemax_e;
    }

    public String getStatut_e() {
        return statut_e;
    }

    public void setStatut_e(String statut_e) {
        this.statut_e = statut_e;
    }

    public String getPrix_e() {
        return prix_e;
    }

    public void setPrix_e(String prix_e) {
        this.prix_e = prix_e;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id_e=" + id_e +
                ", titre_e='" + titre_e + '\'' +
                ", description_e='" + description_e + '\'' +
                ", date_e=" + date_e +
                ", localisation_e='" + localisation_e + '\'' +
                ", type_e='" + type_e + '\'' +
                ", capacitemax_e=" + capacitemax_e +
                ", statut_e='" + statut_e + '\'' +
                ", prix_e='" + prix_e + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
