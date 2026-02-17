package entities;

import java.sql.Date;

public class Ressource {

    // =========================
    // ATTRIBUTS (colonnes BD)
    // =========================

    private int idRessource;        // PK
    private String titre;           // Titre de la ressource
    private String description;     // Description
    private String type;            // PDF, Vidéo, Image, Audio, Article
    private String cheminFichier;   // Chemin du fichier
    private double tailleFichier;   // Taille en Ko
    private String format;          // pdf, jpg, mp4, etc.
    private Date datePublication;   // Date de publication
    private String statut;          // brouillon, publié, archivé
    private int idUser;             // FK vers utilisateur

    // =========================
    // CONSTRUCTEURS
    // =========================

    // Constructeur vide (OBLIGATOIRE pour JDBC)
    public Ressource() {
    }

    // Constructeur SANS id (pour INSERT)
    public Ressource(String titre, String description, String type, 
                     String cheminFichier, double tailleFichier, String format,
                     String statut, int idUser) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.cheminFichier = cheminFichier;
        this.tailleFichier = tailleFichier;
        this.format = format;
        this.statut = statut;
        this.idUser = idUser;
    }

    // Constructeur AVEC id (pour UPDATE / SELECT)
    public Ressource(int idRessource, String titre, String description, String type,
                     String cheminFichier, double tailleFichier, String format,
                     Date datePublication, String statut, int idUser) {
        this.idRessource = idRessource;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.cheminFichier = cheminFichier;
        this.tailleFichier = tailleFichier;
        this.format = format;
        this.datePublication = datePublication;
        this.statut = statut;
        this.idUser = idUser;
    }

    // =========================
    // GETTERS & SETTERS
    // =========================

    public int getIdRessource() {
        return idRessource;
    }

    public void setIdRessource(int idRessource) {
        this.idRessource = idRessource;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public double getTailleFichier() {
        return tailleFichier;
    }

    public void setTailleFichier(double tailleFichier) {
        this.tailleFichier = tailleFichier;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Date getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(Date datePublication) {
        this.datePublication = datePublication;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
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
        return "Ressource{" +
                "idRessource=" + idRessource +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", cheminFichier='" + cheminFichier + '\'' +
                ", tailleFichier=" + tailleFichier +
                ", format='" + format + '\'' +
                ", datePublication=" + datePublication +
                ", statut='" + statut + '\'' +
                ", idUser=" + idUser +
                '}';
    }
}
