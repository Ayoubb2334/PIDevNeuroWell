package entities;

import java.time.LocalDate;

public class Facture {

    private int id;
    private String numeroFacture;
    private LocalDate dateGeneration;
    private double montantTotal;
    private String description;

    public Facture() {}

    public Facture(String numeroFacture, LocalDate dateGeneration,
                   double montantTotal, String description) {
        this.numeroFacture = numeroFacture;
        this.dateGeneration = dateGeneration;
        this.montantTotal = montantTotal;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public LocalDate getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDate dateGeneration) { this.dateGeneration = dateGeneration; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}