package services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

// ── AWT : imports EXPLICITES (évite le conflit java.awt.List vs java.util.List) ──
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;

// ── Java util : imports EXPLICITES ────────────────────────────────────────────────
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CardScannerService — VERSION FINALE CORRIGÉE
 *
 * Corrections :
 *   - Suppression de "import java.awt.*" → remplacé par imports explicites
 *     pour éviter le conflit java.awt.List vs java.util.List
 *   - Utilisation de java.util.ArrayList et java.util.List explicitement
 */
public class CardScannerService {

    public static class ScanResult {
        public String  numero     = "";
        public String  expiration = "";
        public String  titulaire  = "";
        public boolean success    = false;
        public String  erreur     = "";
    }

    // ══════════════════════════════════════════════════════════════
    //  🚀  MÉTHODE PRINCIPALE
    // ══════════════════════════════════════════════════════════════

    public ScanResult analyserCarte(BufferedImage image) {
        ScanResult meilleur = new ScanResult();

        try {
            System.out.println("[OCR] Démarrage analyse multi-pass...");
            Tesseract tess = creerTesseract();

            BufferedImage[] variantes = {
                    methode_Inversion(image),
                    methode_InversionForte(image),
                    methode_Sobel(image),
                    methode_GrisContraste(image),
                    methode_Original(image),
                    methode_CanalBleu(image),
            };
            String[] noms = { "Inversion","InversionForte","Sobel","Gris","Original","CanalBleu" };

            // java.util.ArrayList — explicitement pour éviter ambiguïté
            java.util.List<String> tousLesTextes = new ArrayList<>();

            for (int i = 0; i < variantes.length; i++) {
                try {
                    String texte = tess.doOCR(variantes[i]);
                    System.out.println("[OCR][" + noms[i] + "] → " +
                            texte.replaceAll("\\n+", " ").trim());
                    tousLesTextes.add(texte);

                    ScanResult r = extraireInfos(texte);
                    if (scoreResultat(r) > scoreResultat(meilleur)) {
                        meilleur = r;
                        System.out.println("[OCR][" + noms[i] + "] ✓ Meilleur score=" + scoreResultat(r));
                    }

                } catch (TesseractException e) {
                    System.err.println("[OCR][" + noms[i] + "] Erreur : " + e.getMessage());
                }
            }

            // Stratégie de vote si numéro incomplet
            if (meilleur.numero.isEmpty() || meilleur.numero.replaceAll(" ", "").length() < 16) {
                System.out.println("[OCR] Tentative assemblage par vote...");
                String numeroVote = voterNumeroCarte(tousLesTextes);
                if (!numeroVote.isEmpty()
                        && numeroVote.replaceAll(" ", "").length()
                        >= meilleur.numero.replaceAll(" ", "").length()) {
                    meilleur.numero = numeroVote;
                    System.out.println("[OCR] Vote → " + numeroVote);
                }
            }

            // Chercher expiration et titulaire dans toutes les passes
            if (meilleur.expiration.isEmpty()) {
                for (String t : tousLesTextes) {
                    String exp = extraireExpiration(t);
                    if (!exp.isEmpty()) { meilleur.expiration = exp; break; }
                }
            }
            if (meilleur.titulaire.isEmpty()) {
                for (String t : tousLesTextes) {
                    String nom = extraireTitulaire(t);
                    if (!nom.isEmpty()) { meilleur.titulaire = nom; break; }
                }
            }

            meilleur.success = !meilleur.numero.isEmpty();

            if (meilleur.success) {
                System.out.println("[OCR] ✓ Numéro    : " + meilleur.numero);
                System.out.println("[OCR] ✓ Expiration: " + meilleur.expiration);
                System.out.println("[OCR] ✓ Titulaire : " + meilleur.titulaire);
            } else {
                meilleur.erreur =
                        "Numéro de carte non détecté.\n\n" +
                                "Conseils :\n" +
                                "• Éclairez bien la carte (évitez les reflets)\n" +
                                "• Posez-la bien à plat dans le cadre\n" +
                                "• Maintenez la caméra stable\n" +
                                "• Réessayez en changeant l'angle légèrement";
            }

        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            meilleur.erreur = "Tesseract non installé.\nWindows: https://github.com/UB-Mannheim/tesseract/wiki";
        } catch (Exception e) {
            System.err.println("[OCR] Erreur : " + e.getMessage());
            meilleur.erreur = "Erreur : " + e.getMessage();
        }

        return meilleur;
    }

    // ══════════════════════════════════════════════════════════════
    //  ⚙️  TESSERACT
    // ══════════════════════════════════════════════════════════════

    private Tesseract creerTesseract() {
        Tesseract t = new Tesseract();
        for (String c : new String[]{
                "C:\\Program Files\\Tesseract-OCR\\tessdata",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/local/share/tessdata",
                "/opt/homebrew/share/tessdata"
        }) {
            if (new java.io.File(c).exists()) { t.setDatapath(c); break; }
        }
        t.setLanguage("eng");
        t.setPageSegMode(6);
        t.setOcrEngineMode(1);
        return t;
    }

    // ══════════════════════════════════════════════════════════════
    //  🖼️  MÉTHODES DE PRÉTRAITEMENT
    // ══════════════════════════════════════════════════════════════

    private BufferedImage methode_Inversion(BufferedImage src) {
        BufferedImage img = agrandir(src, 2.5);
        img = toGris(img);
        img = inverser(img);
        img = contraste(img, 2.0f, -20);
        img = seuiller(img, 115);
        return img;
    }

    private BufferedImage methode_InversionForte(BufferedImage src) {
        BufferedImage img = agrandir(src, 2.5);
        img = toGris(img);
        img = inverser(img);
        img = contraste(img, 3.0f, -50);
        img = seuiller(img, 100);
        return img;
    }

    private BufferedImage methode_Sobel(BufferedImage src) {
        BufferedImage img = agrandir(src, 2.5);
        img = toGris(img);
        float[] k = { -1,0,1, -2,0,2, -1,0,1 };
        BufferedImage dst = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        try {
            new ConvolveOp(new Kernel(3, 3, k), ConvolveOp.EDGE_NO_OP, null).filter(img, dst);
        } catch (Exception e) { dst = img; }
        dst = contraste(dst, 4.0f, -80);
        dst = seuiller(dst, 60);
        return dst;
    }

    private BufferedImage methode_GrisContraste(BufferedImage src) {
        BufferedImage img = agrandir(src, 2.0);
        img = toGris(img);
        img = contraste(img, 2.5f, -40);
        img = seuiller(img, 140);
        return img;
    }

    private BufferedImage methode_Original(BufferedImage src) {
        return agrandir(src, 2.0);
    }

    private BufferedImage methode_CanalBleu(BufferedImage src) {
        BufferedImage img = agrandir(src, 2.5);
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage bleu = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int b = img.getRGB(x, y) & 0xFF;
                bleu.setRGB(x, y, (b << 16) | (b << 8) | b);
            }
        }
        bleu = egaliserHisto(bleu);
        bleu = contraste(bleu, 2.0f, -20);
        bleu = seuiller(bleu, 128);
        return bleu;
    }

    // ══════════════════════════════════════════════════════════════
    //  🔤  CORRECTION OCR
    // ══════════════════════════════════════════════════════════════

    private String corrigerOCR_Chiffres(String texte) {
        return texte
                .replace("O", "0").replace("o", "0").replace("Q", "0")
                .replace("I", "1").replace("l", "1").replace("|", "1")
                .replace("Z", "2").replace("z", "2")
                .replace("E", "3")
                .replace("A", "4")
                .replace("S", "5").replace("s", "5")
                .replace("G", "6").replace("b", "6")
                .replace("T", "7").replace("Y", "7")
                .replace("B", "8")
                .replace("g", "9")
                .replace(".", "/").replace(",", "/")
                .replaceAll("(\\d)\\s(\\d)", "$1$2");
    }

    // ══════════════════════════════════════════════════════════════
    //  🔍  EXTRACTION
    // ══════════════════════════════════════════════════════════════

    private ScanResult extraireInfos(String texte) {
        ScanResult r = new ScanResult();
        if (texte == null || texte.isBlank()) return r;
        String texteChiffres = corrigerOCR_Chiffres(texte);
        r.numero     = extraireNumero(texteChiffres);
        r.expiration = extraireExpiration(texte);
        r.titulaire  = extraireTitulaire(texte);
        return r;
    }

    private int scoreResultat(ScanResult r) {
        int s = 0;
        if (!r.numero.isEmpty())     s += 10;
        if (!r.expiration.isEmpty()) s += 5;
        if (!r.titulaire.isEmpty())  s += 3;
        int len = r.numero.replaceAll(" ", "").length();
        if (len == 16)     s += 10;
        else if (len >= 12) s += 5;
        else if (len >= 8)  s += 2;
        return s;
    }

    private String voterNumeroCarte(java.util.List<String> tousLesTextes) {
        java.util.List<String> candidats = new ArrayList<>();
        for (String texte : tousLesTextes) {
            String corrige = corrigerOCR_Chiffres(texte);
            String num = extraireNumero(corrige);
            if (!num.isEmpty() && num.replaceAll(" ", "").length() >= 12) {
                candidats.add(num);
                System.out.println("[OCR][Vote] Candidat : " + num);
            }
        }

        if (candidats.isEmpty()) return "";
        if (candidats.size() == 1) return candidats.get(0);

        String[][] groupes = new String[candidats.size()][4];
        for (int i = 0; i < candidats.size(); i++) {
            String[] parts = candidats.get(i).split(" ");
            for (int j = 0; j < 4 && j < parts.length; j++) {
                groupes[i][j] = parts[j];
            }
        }

        StringBuilder resultat = new StringBuilder();
        for (int pos = 0; pos < 4; pos++) {
            Map<String, Integer> compteur = new HashMap<>();
            for (String[] groupe : groupes) {
                if (groupe[pos] != null && groupe[pos].length() == 4) {
                    compteur.merge(groupe[pos], 1, Integer::sum);
                }
            }
            String meilleurGroupe = compteur.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
            if (!meilleurGroupe.isEmpty()) {
                if (resultat.length() > 0) resultat.append(" ");
                resultat.append(meilleurGroupe);
            }
        }
        return resultat.toString();
    }

    private String extraireNumero(String texte) {
        if (texte == null || texte.isEmpty()) return "";

        // Pattern 1 : 4 groupes de 4 chiffres séparés
        Matcher m = Pattern.compile("(\\d{4})[\\s\\-\\.](\\d{4})[\\s\\-\\.](\\d{4})[\\s\\-\\.](\\d{4})").matcher(texte);
        if (m.find()) {
            String num = m.group(1) + " " + m.group(2) + " " + m.group(3) + " " + m.group(4);
            System.out.println("[OCR] Pattern 4x4 → " + num);
            return num;
        }

        // Pattern 2 : 16 chiffres collés
        m = Pattern.compile("\\b(\\d{16})\\b").matcher(texte.replaceAll("[^\\d]", ""));
        if (m.find()) {
            String n = m.group(1);
            return n.substring(0,4) + " " + n.substring(4,8) + " " + n.substring(8,12) + " " + n.substring(12);
        }

        // Pattern 3 : Tolérant — séparateurs variables
        m = Pattern.compile("(\\d{4})[^\\d]{0,3}(\\d{4})[^\\d]{0,3}(\\d{4})[^\\d]{0,3}(\\d{4})").matcher(texte);
        if (m.find()) {
            String num = m.group(1) + " " + m.group(2) + " " + m.group(3) + " " + m.group(4);
            System.out.println("[OCR] Pattern souple → " + num);
            return num;
        }

        // Pattern 4 : Amex 15 chiffres
        m = Pattern.compile("\\b(3[47]\\d{13})\\b").matcher(texte.replaceAll("[^\\d]", ""));
        if (m.find()) {
            String n = m.group(1);
            return n.substring(0,4) + " " + n.substring(4,10) + " " + n.substring(10);
        }

        return "";
    }

    private String extraireExpiration(String texte) {
        if (texte == null || texte.isEmpty()) return "";

        String t = texte.toUpperCase()
                .replace("O", "0").replace("o", "0")
                .replace("I", "1").replace("l", "1")
                .replace(",", "/").replace(".", "/")
                .replace(" /", "/").replace("/ ", "/");

        // Avec mot-clé VALID/THRU/EXP
        Matcher m = Pattern.compile(
                "(?:VALID|THRU|EXPIRES?|EXPIRY|EXP)[^\\d]*(\\d{1,2})[/\\-](\\d{2,4})",
                Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) {
            String mois = m.group(1).length() == 1 ? "0" + m.group(1) : m.group(1);
            String an   = m.group(2).length() == 4 ? m.group(2).substring(2) : m.group(2);
            int moisInt = Integer.parseInt(mois);
            if (moisInt >= 1 && moisInt <= 12) return mois + "/" + an;
        }

        // Pattern MM/YY général
        m = Pattern.compile("\\b(0[1-9]|1[0-2])[/\\-](\\d{2}(?:\\d{2})?)\\b").matcher(t);
        if (m.find()) {
            String an = m.group(2).length() == 4 ? m.group(2).substring(2) : m.group(2);
            return m.group(1) + "/" + an;
        }

        // Pattern très souple : MM espace/point/slash YY
        m = Pattern.compile("\\b(0[1-9]|1[0-2])[ ./](\\d{2})\\b").matcher(t);
        if (m.find()) return m.group(1) + "/" + m.group(2);

        return "";
    }

    private String extraireTitulaire(String texte) {
        if (texte == null || texte.isEmpty()) return "";

        Set<String> ignorer = new HashSet<>(Arrays.asList(
                "VISA","MASTERCARD","AMEX","DISCOVER","CARD","BANK","CREDIT","DEBIT",
                "VALID","THRU","EXPIRES","EXPIRY","DATE","GOOD","FROM","MEMBER",
                "SINCE","THROUGH","CVV","CVC","EMV","CHIP","ACCOUNT","BIAT",
                "EPARGNE","BANQUE","CARTE","GOLD","PLATINUM","CLASSIC","ELECTRON",
                "INTERNATIONAL","MONDIALE","PREPAID"
        ));

        String upper = texte.toUpperCase()
                .replace("W", "M")
                .replace("VV", "W");

        // Chercher MR/MRS/MS/DR + nom
        Matcher m = Pattern.compile("\\b(MR|MRS|MS|DR|ME)\\.?\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,}){0,3})")
                .matcher(upper);
        if (m.find()) {
            String nom = m.group(2).trim();
            boolean valide = true;
            for (String mot : nom.split("\\s+")) {
                if (ignorer.contains(mot)) { valide = false; break; }
            }
            if (valide) return "MR " + nom;
        }

        // 2-4 mots majuscules consécutifs
        m = Pattern.compile("\\b([A-Z]{2,}(?:\\s+[A-Z]{2,}){1,3})\\b").matcher(upper);
        String meilleur = "";
        int meilleurScore = 0;
        while (m.find()) {
            String cand = m.group(1).trim();
            String[] mots = cand.split("\\s+");
            int score = 0;
            boolean interdit = false;
            for (String mot : mots) {
                if (ignorer.contains(mot)) { interdit = true; break; }
                if (mot.length() >= 3 && mot.length() <= 15) score++;
            }
            if (!interdit && mots.length >= 2 && score > meilleurScore) {
                meilleur = cand;
                meilleurScore = score;
            }
        }
        return meilleur;
    }

    // ══════════════════════════════════════════════════════════════
    //  🛠️  UTILITAIRES IMAGE
    // ══════════════════════════════════════════════════════════════

    private BufferedImage agrandir(BufferedImage src, double f) {
        int w = (int)(src.getWidth() * f), h = (int)(src.getHeight() * f);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private BufferedImage toGris(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private BufferedImage contraste(BufferedImage src, float scale, float offset) {
        RescaleOp op = new RescaleOp(scale, offset, null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        try { op.filter(src, dst); return dst; } catch (Exception e) { return src; }
    }

    private BufferedImage seuiller(BufferedImage src, int seuil) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dst.setRGB(x, y, (src.getRGB(x, y) & 0xFF) > seuil ? 0xFFFFFF : 0x000000);
            }
        }
        return dst;
    }

    private BufferedImage inverser(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, src.getType());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = 255 - (src.getRGB(x, y) & 0xFF);
                dst.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        return dst;
    }

    private BufferedImage egaliserHisto(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight(), total = w * h;
        int[] histo = new int[256];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                histo[src.getRGB(x, y) & 0xFF]++;
        int[] lut = new int[256];
        int cum = 0;
        for (int i = 0; i < 256; i++) { cum += histo[i]; lut[i] = (int)((double)cum / total * 255); }
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = lut[src.getRGB(x, y) & 0xFF];
                dst.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        return dst;
    }
}