package entities;

import java.time.LocalDate;

public class CompteRendu {

    private int idCompteRendu;
    private int idConsultation;
    private String description;
    private LocalDate dateRedaction;

    public CompteRendu() {
    }

    public CompteRendu(int idConsultation, String description, LocalDate dateRedaction) {
        this.idConsultation = idConsultation;
        this.description = description;
        this.dateRedaction = dateRedaction;
    }

    public int getIdCompteRendu() {
        return idCompteRendu;
    }

    public void setIdCompteRendu(int idCompteRendu) {
        this.idCompteRendu = idCompteRendu;
    }

    public int getIdConsultation() {
        return idConsultation;
    }

    public void setIdConsultation(int idConsultation) {
        this.idConsultation = idConsultation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateRedaction() {
        return dateRedaction;
    }

    public void setDateRedaction(LocalDate dateRedaction) {
        this.dateRedaction = dateRedaction;
    }

    @Override
    public String toString() {
        return "CompteRendu{" +
                "idCompteRendu=" + idCompteRendu +
                ", idConsultation=" + idConsultation +
                ", description='" + description + '\'' +
                ", dateRedaction=" + dateRedaction +
                '}';
    }
}
