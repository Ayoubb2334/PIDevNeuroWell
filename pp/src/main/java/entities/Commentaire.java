package entities;

import java.time.LocalDateTime;

public class Commentaire {

    private int           idCommentaire;
    private int           idRessource;
    private String        contenu;
    private LocalDateTime dateCommentaire;

    public Commentaire() {}

    // Constructeur INSERT (sans id ni date, la BD les génère)
    public Commentaire(int idRessource, String contenu) {
        this.idRessource = idRessource;
        this.contenu     = contenu;
    }

    // Constructeur complet (SELECT)
    public Commentaire(int idCommentaire, int idRessource, String contenu, LocalDateTime dateCommentaire) {
        this.idCommentaire   = idCommentaire;
        this.idRessource     = idRessource;
        this.contenu         = contenu;
        this.dateCommentaire = dateCommentaire;
    }

    // ── Getters & Setters ─────────────────────────────────────────────
    public int getIdCommentaire()               { return idCommentaire; }
    public void setIdCommentaire(int id)        { this.idCommentaire = id; }

    public int getIdRessource()                 { return idRessource; }
    public void setIdRessource(int idRessource) { this.idRessource = idRessource; }

    public String getContenu()                  { return contenu; }
    public void setContenu(String contenu)      { this.contenu = contenu; }

    public LocalDateTime getDateCommentaire()               { return dateCommentaire; }
    public void setDateCommentaire(LocalDateTime date)      { this.dateCommentaire = date; }

    @Override
    public String toString() {
        return "Commentaire{id=" + idCommentaire +
               ", idRessource=" + idRessource +
               ", contenu='" + contenu + '\'' +
               ", date=" + dateCommentaire + '}';
    }
}
