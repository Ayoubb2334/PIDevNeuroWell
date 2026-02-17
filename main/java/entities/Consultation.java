package entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Consultation {

    private int idConsultation;
    private int idUser;
    private int idPsychologue;
    private LocalDate dateConsultation;
    private LocalTime heureConsultation;
    private String typeConsultation;
    private String statut;

    public Consultation() {
    }

    public Consultation(int idUser, int idPsychologue,
                        LocalDate dateConsultation,
                        LocalTime heureConsultation,
                        String typeConsultation,
                        String statut) {

        this.idUser = idUser;
        this.idPsychologue = idPsychologue;
        this.dateConsultation = dateConsultation;
        this.heureConsultation = heureConsultation;
        this.typeConsultation = typeConsultation;
        this.statut = statut;
    }

    public int getIdConsultation() {
        return idConsultation;
    }

    public void setIdConsultation(int idConsultation) {
        this.idConsultation = idConsultation;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdPsychologue() {
        return idPsychologue;
    }

    public void setIdPsychologue(int idPsychologue) {
        this.idPsychologue = idPsychologue;
    }

    public LocalDate getDateConsultation() {
        return dateConsultation;
    }

    public void setDateConsultation(LocalDate dateConsultation) {
        this.dateConsultation = dateConsultation;
    }

    public LocalTime getHeureConsultation() {
        return heureConsultation;
    }

    public void setHeureConsultation(LocalTime heureConsultation) {
        this.heureConsultation = heureConsultation;
    }

    public String getTypeConsultation() {
        return typeConsultation;
    }

    public void setTypeConsultation(String typeConsultation) {
        this.typeConsultation = typeConsultation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
