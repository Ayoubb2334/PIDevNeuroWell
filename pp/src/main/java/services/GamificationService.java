package services;

import entities.Evaluation;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de Gamification — Gestion XP, Niveaux, Badges, Streak
 * Aucune table BDD supplémentaire — tout calculé depuis les évaluations existantes.
 */
public class GamificationService {

    // ══════════════════════════════════════════════════════
    //  BADGES
    // ══════════════════════════════════════════════════════
    public enum Badge {
        PREMIERE_EVAL   ("🥇", "Première évaluation",  "Faites votre 1ère évaluation",         "Complétée le "),
        EN_FEU          ("🔥", "En Feu !",              "3 jours consécutifs d'évaluation",      "Streak de 3 atteint le "),
        ASSIDU          ("💎", "Assidu",                "7 jours consécutifs d'évaluation",      "Streak de 7 atteint le "),
        EN_PROGRES      ("📈", "En Progrès",            "3 améliorations consécutives",           "3 amélio. consécutives le "),
        ZEN             ("🧘", "Zen",                   "5 évaluations Faible de suite",          "5 Faibles consécutifs le "),
        COMBATTANT      ("⚡", "Combattant",            "10 évaluations au total",               "10 évals atteintes le "),
        EXPERT          ("🏆", "Expert",                "25 évaluations au total",               "25 évals atteintes le ");

        public final String emoji;
        public final String nom;
        public final String condition;
        public final String prefixeDate;

        Badge(String emoji, String nom, String condition, String prefixeDate) {
            this.emoji = emoji; this.nom = nom;
            this.condition = condition; this.prefixeDate = prefixeDate;
        }
    }

    // ══════════════════════════════════════════════════════
    //  NIVEAUX
    // ══════════════════════════════════════════════════════
    public static class Niveau {
        public final int    numero;
        public final String titre;
        public final String emoji;
        public final int    xpRequis;
        public final int    xpProchain;
        public final String couleur;

        public Niveau(int numero, String titre, String emoji, int xpRequis, int xpProchain, String couleur) {
            this.numero = numero; this.titre = titre; this.emoji = emoji;
            this.xpRequis = xpRequis; this.xpProchain = xpProchain; this.couleur = couleur;
        }
    }

    private static final Niveau[] NIVEAUX = {
        new Niveau(1, "Débutant",       "🌱", 0,    100,  "#00D9FF"),
        new Niveau(2, "Intermédiaire",  "⚡", 100,  250,  "#00FF88"),
        new Niveau(3, "Avancé",         "💫", 250,  500,  "#FFD700"),
        new Niveau(4, "Expert",         "🏆", 500,  1000, "#FF8C00"),
        new Niveau(5, "Maître",         "👑", 1000, 9999, "#FF4D6D"),
    };

    // ══════════════════════════════════════════════════════
    //  RÉSULTAT GAMIFICATION
    // ══════════════════════════════════════════════════════
    public static class ResultatGamification {
        public int              xpTotal;
        public int              xpGagne;          // XP gagné pour CETTE évaluation
        public Niveau           niveauActuel;
        public boolean          levelUp;           // true si on vient de monter de niveau
        public Niveau           ancienNiveau;
        public int              streak;
        public List<Badge>      nouveauxBadges;    // badges débloqués par CETTE évaluation
        public Set<Badge>       tousLesBadges;     // tous les badges de l'utilisateur
        public boolean          xpBonus_amelioration;
        public boolean          xpBonus_streak;
        public boolean          xpBonus_faible;
    }

    // ══════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ══════════════════════════════════════════════════════
    public ResultatGamification calculer(List<Evaluation> toutesEvaluations, Evaluation nouvelleEval) {
        ResultatGamification r = new ResultatGamification();

        // Évaluations AVANT la nouvelle (pour comparer badges avant/après)
        // On exclut la nouvelle évaluation en cherchant la plus récente par date+type+score
        List<Evaluation> tries = toutesEvaluations.stream()
            .filter(e -> e.getDateEvaluation() != null)
            .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
            .collect(Collectors.toList());
        // La nouvelle évaluation est la plus récente — on l'exclut
        List<Evaluation> avant = tries.isEmpty() ? new ArrayList<>() : tries.subList(1, tries.size());

        // ── XP de base ──────────────────────────────────
        int xp = 0;
        xp += 10; // évaluation de base

        // Bonus Faible
        if ("Faible".equals(nouvelleEval.getNiveau())) {
            xp += 5;
            r.xpBonus_faible = true;
        }

        // Bonus amélioration
        List<Evaluation> memeType = avant.stream()
            .filter(e -> e.getTypeTest().equals(nouvelleEval.getTypeTest()))
            .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
            .collect(Collectors.toList());

        if (!memeType.isEmpty() && nouvelleEval.getScore() < memeType.get(0).getScore()) {
            xp += 15;
            r.xpBonus_amelioration = true;
        }

        // Streak
        int streak = calculerStreak(toutesEvaluations);
        r.streak = streak;
        if (streak > 1) {
            xp += 20;
            r.xpBonus_streak = true;
        }

        // Badges AVANT
        Set<Badge> badgesAvant = getBadgesDebloques(avant);

        // Badges APRÈS (avec la nouvelle)
        Set<Badge> badgesApres = getBadgesDebloques(toutesEvaluations);

        // Nouveaux badges = différence
        List<Badge> nouveaux = new ArrayList<>();
        for (Badge b : badgesApres) {
            if (!badgesAvant.contains(b)) {
                nouveaux.add(b);
                xp += 25; // bonus badge
            }
        }

        r.xpGagne       = xp;
        r.nouveauxBadges = nouveaux;
        r.tousLesBadges  = badgesApres;

        // XP total = XP de toutes les évaluations passées + XP actuel
        int xpTotal = calculerXPTotal(avant) + xp;
        r.xpTotal = xpTotal;

        // Niveau
        Niveau niveauAvant  = getNiveau(calculerXPTotal(avant));
        Niveau niveauApres  = getNiveau(xpTotal);
        r.niveauActuel = niveauApres;
        r.ancienNiveau  = niveauAvant;
        r.levelUp       = niveauApres.numero > niveauAvant.numero;

        return r;
    }

    // ══════════════════════════════════════════════════════
    //  XP TOTAL depuis une liste d'évaluations
    // ══════════════════════════════════════════════════════
    public int calculerXPTotal(List<Evaluation> evals) {
        if (evals == null || evals.isEmpty()) return 0;
        List<Evaluation> tries = evals.stream()
            .sorted(Comparator.comparing(Evaluation::getDateEvaluation))
            .collect(Collectors.toList());

        int xp = 0;
        for (int i = 0; i < tries.size(); i++) {
            Evaluation e = tries.get(i);
            xp += 10; // base
            if ("Faible".equals(e.getNiveau())) xp += 5;
            // Amélioration
            List<Evaluation> memeType = tries.subList(0, i).stream()
                .filter(prev -> prev.getTypeTest().equals(e.getTypeTest()))
                .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
                .collect(Collectors.toList());
            if (!memeType.isEmpty() && e.getScore() < memeType.get(0).getScore()) xp += 15;
        }
        // Streak bonus (simplifié : +20 par jour consécutif depuis le 2ème)
        int streak = calculerStreak(tries);
        if (streak > 1) xp += 20 * (streak - 1);
        // Badge bonus
        xp += getBadgesDebloques(evals).size() * 25;
        return xp;
    }

    // ══════════════════════════════════════════════════════
    //  STREAK — jours consécutifs
    // ══════════════════════════════════════════════════════
    public int calculerStreak(List<Evaluation> evals) {
        if (evals == null || evals.isEmpty()) return 0;

        Set<LocalDate> jours = evals.stream()
            .filter(e -> e.getDateEvaluation() != null)
            .map(e -> e.getDateEvaluation().toLocalDate())
            .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate jour = today;

        while (jours.contains(jour)) {
            streak++;
            jour = jour.minusDays(1);
        }
        // Si pas d'évaluation aujourd'hui, vérifier hier
        if (streak == 0) {
            jour = today.minusDays(1);
            while (jours.contains(jour)) {
                streak++;
                jour = jour.minusDays(1);
            }
        }
        return streak;
    }

    // ══════════════════════════════════════════════════════
    //  BADGES DÉBLOQUÉS
    // ══════════════════════════════════════════════════════
    public Set<Badge> getBadgesDebloques(List<Evaluation> evals) {
        Set<Badge> badges = new LinkedHashSet<>();
        if (evals == null || evals.isEmpty()) return badges;

        int total = evals.size();
        int streak = calculerStreak(evals);

        // 🥇 Première évaluation
        if (total >= 1) badges.add(Badge.PREMIERE_EVAL);

        // 🔥 En Feu — 3 jours consécutifs
        if (streak >= 3) badges.add(Badge.EN_FEU);

        // 💎 Assidu — 7 jours consécutifs
        if (streak >= 7) badges.add(Badge.ASSIDU);

        // 📈 En Progrès — 3 améliorations consécutives
        if (has3AmeliorationsConsecutives(evals)) badges.add(Badge.EN_PROGRES);

        // 🧘 Zen — 5 Faible consécutifs
        if (has5FaiblesConsecutifs(evals)) badges.add(Badge.ZEN);

        // ⚡ Combattant — 10 évaluations
        if (total >= 10) badges.add(Badge.COMBATTANT);

        // 🏆 Expert — 25 évaluations
        if (total >= 25) badges.add(Badge.EXPERT);

        return badges;
    }

    // ══════════════════════════════════════════════════════
    //  NIVEAU selon XP
    // ══════════════════════════════════════════════════════
    public Niveau getNiveau(int xp) {
        Niveau current = NIVEAUX[0];
        for (Niveau n : NIVEAUX) {
            if (xp >= n.xpRequis) current = n;
        }
        return current;
    }

    public Niveau getNiveauDepuisEvals(List<Evaluation> evals) {
        return getNiveau(calculerXPTotal(evals));
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ══════════════════════════════════════════════════════
    private boolean has3AmeliorationsConsecutives(List<Evaluation> evals) {
        if (evals.size() < 4) return false;
        List<Evaluation> tries = evals.stream()
            .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
            .collect(Collectors.toList());
        int count = 0;
        for (int i = 0; i < tries.size() - 1; i++) {
            if (tries.get(i).getScore() < tries.get(i + 1).getScore()) {
                count++;
                if (count >= 3) return true;
            } else {
                count = 0;
            }
        }
        return false;
    }

    private boolean has5FaiblesConsecutifs(List<Evaluation> evals) {
        if (evals.size() < 5) return false;
        List<Evaluation> tries = evals.stream()
            .sorted(Comparator.comparing(Evaluation::getDateEvaluation).reversed())
            .collect(Collectors.toList());
        int count = 0;
        for (Evaluation e : tries) {
            if ("Faible".equals(e.getNiveau())) {
                count++;
                if (count >= 5) return true;
            } else {
                count = 0;
            }
        }
        return false;
    }
}
