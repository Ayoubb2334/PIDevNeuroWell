package entities;

import java.sql.Date;

public class Paiement {

    private int id;
    private double montant;
    private String modePaiement;
    private String statut;
    private Date datePaiement;
    private int idUser;

    public Paiement() {}

    public Paiement(double montant, String modePaiement, String statut, Date datePaiement, int idUser) {
        this.montant = montant;
        this.modePaiement = modePaiement;
        this.statut = statut;
        this.datePaiement = datePaiement;
        this.idUser = idUser;
    }

    public Paiement(int id, double montant, String modePaiement, String statut, Date datePaiement, int idUser) {
        this.id = id;
        this.montant = montant;
        this.modePaiement = modePaiement;
        this.statut = statut;
        this.datePaiement = datePaiement;
        this.idUser = idUser;
    }

    // GETTERS
    public int getId() { return id; }
    public double getMontant() { return montant; }
    public String getModePaiement() { return modePaiement; }
    public String getStatut() { return statut; }
    public Date getDatePaiement() { return datePaiement; }
    public int getIdUser() { return idUser; }

    // SETTERS
    public void setId(int id) { this.id = id; }
    public void setMontant(double montant) { this.montant = montant; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDatePaiement(Date datePaiement) { this.datePaiement = datePaiement; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
}
