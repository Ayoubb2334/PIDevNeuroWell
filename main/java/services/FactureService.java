package services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import entities.Facture;
import entities.Paiement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * FactureService — Génération de factures PDF professionnelles pour NeuroWell
 * Dépendance: com.itextpdf:itextpdf:5.5.13.3
 */
public class FactureService {

    // ══════════════ COULEURS NEUROWELL ══════════════
    private static final BaseColor CYAN        = new BaseColor(0, 255, 185);
    private static final BaseColor DARK_BG     = new BaseColor(10, 14, 26);
    private static final BaseColor DARK_PANEL  = new BaseColor(15, 22, 41);
    private static final BaseColor GRAY_TEXT   = new BaseColor(160, 174, 192);
    private static final BaseColor WHITE       = BaseColor.WHITE;
    private static final BaseColor LIGHT_GRAY  = new BaseColor(237, 242, 247);

    // ══════════════ FONTS ══════════════
    private Font fontTitle;
    private Font fontH1;
    private Font fontH2;
    private Font fontBody;
    private Font fontSmall;
    private Font fontBold;
    private Font fontCyan;
    private Font fontWhite;

    public FactureService() {
        try {
            BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BaseFont baseBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            fontTitle  = new Font(baseBold, 28, Font.BOLD, WHITE);
            fontH1     = new Font(baseBold, 16, Font.BOLD, WHITE);
            fontH2     = new Font(baseBold, 13, Font.BOLD, DARK_BG);
            fontBody   = new Font(base, 11, Font.NORMAL, GRAY_TEXT);
            fontSmall  = new Font(base, 9, Font.NORMAL, GRAY_TEXT);
            fontBold   = new Font(baseBold, 11, Font.BOLD, DARK_BG);
            fontCyan   = new Font(baseBold, 14, Font.BOLD, CYAN);
            fontWhite  = new Font(base, 11, Font.NORMAL, WHITE);
        } catch (Exception e) {
            // fallback
            fontTitle  = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, WHITE);
            fontH1     = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, WHITE);
            fontH2     = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, DARK_BG);
            fontBody   = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, GRAY_TEXT);
            fontSmall  = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
            fontBold   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, DARK_BG);
            fontCyan   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, CYAN);
            fontWhite  = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, WHITE);
        }
    }

    /**
     * Génère un PDF de facture et retourne le chemin du fichier.
     */
    public String genererFacturePDF(Facture facture, Paiement paiement,
                                    String prenom, String nom, String email)
            throws DocumentException, IOException {

        String fileName = System.getProperty("user.home") + "/Downloads/"
                + facture.getNumeroFacture().replace("/", "-") + ".pdf";

        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));

        document.open();

        // ─── HEADER BAND (dark background) ───
        addHeader(writer, document, facture);

        document.setMargins(40, 40, 20, 40);

        // ─── INFO ROW ───
        addInfoRow(document, facture, prenom, nom, email, paiement);

        // ─── DETAIL TABLE ───
        addDetailTable(document, paiement);

        // ─── TOTAL SECTION ───
        addTotalSection(document, facture);

        // ─── NOTES ───
        addNotes(document, paiement);

        // ─── FOOTER ───
        addFooter(writer, document, facture);

        document.close();

        return fileName;
    }

    // ══════════════════════════════════════════════════════
    // HEADER
    // ══════════════════════════════════════════════════════
    private void addHeader(PdfWriter writer, Document doc, Facture facture)
            throws DocumentException {

        PdfContentByte canvas = writer.getDirectContentUnder();

        // Dark background rectangle
        canvas.setColorFill(DARK_BG);
        canvas.rectangle(0, PageSize.A4.getHeight() - 130, PageSize.A4.getWidth(), 130);
        canvas.fill();

        // Cyan accent bar at top
        canvas.setColorFill(CYAN);
        canvas.rectangle(0, PageSize.A4.getHeight() - 5, PageSize.A4.getWidth(), 5);
        canvas.fill();

        // Decorative circle
        canvas.setColorFill(new BaseColor(0, 255, 185, 30));
        canvas.circle(PageSize.A4.getWidth() - 60, PageSize.A4.getHeight() - 65, 80);
        canvas.fill();

        // Logo text
        PdfContentByte cb = writer.getDirectContent();

        cb.beginText();
        cb.setFontAndSize(getHelveticaBold(), 28);
        cb.setColorFill(CYAN);
        cb.setTextMatrix(40, PageSize.A4.getHeight() - 55);
        cb.showText("NeuroWell");
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(getHelvetica(), 10);
        cb.setColorFill(GRAY_TEXT);
        cb.setTextMatrix(40, PageSize.A4.getHeight() - 72);
        cb.showText("Psychologie & Developpement Personnel");
        cb.endText();

        // FACTURE label
        cb.beginText();
        cb.setFontAndSize(getHelveticaBold(), 26);
        cb.setColorFill(WHITE);
        cb.setTextMatrix(PageSize.A4.getWidth() - 180, PageSize.A4.getHeight() - 52);
        cb.showText("FACTURE");
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(getHelvetica(), 10);
        cb.setColorFill(GRAY_TEXT);
        cb.setTextMatrix(PageSize.A4.getWidth() - 180, PageSize.A4.getHeight() - 68);
        cb.showText("N° " + facture.getNumeroFacture());
        cb.endText();

        cb.beginText();
        cb.setFontAndSize(getHelvetica(), 10);
        cb.setColorFill(GRAY_TEXT);
        cb.setTextMatrix(PageSize.A4.getWidth() - 180, PageSize.A4.getHeight() - 83);
        String dateFmt = facture.getDateGeneration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        cb.showText("Date: " + dateFmt);
        cb.endText();

        // Spacer after header
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
    }

    // ══════════════════════════════════════════════════════
    // INFO ROW (Emetteur + Destinataire)
    // ══════════════════════════════════════════════════════
    private void addInfoRow(Document doc, Facture facture,
                            String prenom, String nom, String email, Paiement paiement)
            throws DocumentException {

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(20);
        infoTable.setWidths(new float[]{1, 1});

        // ── Emetteur (NeuroWell) ──
        PdfPCell emetteurCell = new PdfPCell();
        emetteurCell.setBorder(Rectangle.NO_BORDER);
        emetteurCell.setPadding(16);
        emetteurCell.setBackgroundColor(DARK_PANEL);

        Paragraph emetteurTitle = new Paragraph("EMIS PAR", fontSmall);
        emetteurTitle.setSpacingAfter(6);
        emetteurCell.addElement(emetteurTitle);

        emetteurCell.addElement(new Paragraph("NeuroWell SARL", fontBold));
        emetteurCell.addElement(new Paragraph("Centre de Psychologie & Bien-etre", fontBody));
        emetteurCell.addElement(new Paragraph("Avenue Habib Bourguiba, Tunis 1001", fontBody));
        emetteurCell.addElement(new Paragraph("Tel: +216 71 000 000", fontBody));
        emetteurCell.addElement(new Paragraph("Email: contact@neurowell.tn", fontBody));
        emetteurCell.addElement(new Paragraph("MF: 123456789A", fontBody));
        infoTable.addCell(emetteurCell);

        // ── Destinataire (Client) ──
        PdfPCell destCell = new PdfPCell();
        destCell.setBorder(Rectangle.NO_BORDER);
        destCell.setPadding(16);
        destCell.setBackgroundColor(LIGHT_GRAY);

        Paragraph destTitle = new Paragraph("FACTURE A", fontSmall);
        destTitle.setSpacingAfter(6);
        destCell.addElement(destTitle);

        Font destBold = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, DARK_BG);
        destCell.addElement(new Paragraph(prenom + " " + nom, destBold));
        destCell.addElement(new Paragraph(email, fontBold));
        destCell.addElement(new Paragraph("ID Patient: " + paiement.getUserId(), fontBody));
        destCell.addElement(new Paragraph("Ref: " + paiement.getReference(), fontBody));
        infoTable.addCell(destCell);

        doc.add(infoTable);
    }

    // ══════════════════════════════════════════════════════
    // DETAIL TABLE
    // ══════════════════════════════════════════════════════
    private void addDetailTable(Document doc, Paiement paiement) throws DocumentException {

        // Table header
        Paragraph tableTitle = new Paragraph("DETAIL DE LA PRESTATION", fontBold);
        tableTitle.setSpacingBefore(10);
        tableTitle.setSpacingAfter(8);
        doc.add(tableTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.5f, 1f, 1.2f, 1.3f});
        table.setSpacingAfter(20);

        // Header row
        String[] headers = {"Description", "Qte", "Prix HT", "Total HT"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontH1));
            cell.setBackgroundColor(DARK_BG);
            cell.setPadding(10);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Row 1: Consultation
        addTableRow(table, "Consultation Psychologique\n(Seance individuelle 50 min)", "1",
                "90,00 DT", "90,00 DT", true);

        // Row 2: Frais plateforme
        addTableRow(table, "Frais de plateforme (2%)", "1",
                "1,80 DT", "1,80 DT", false);

        doc.add(table);
    }

    private void addTableRow(PdfPTable table, String desc, String qty, String pu, String total, boolean shaded) {
        BaseColor bg = shaded ? new BaseColor(245, 248, 255) : WHITE;
        Font bodyDark = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, DARK_BG);
        Font bodyGray = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

        PdfPCell c1 = new PdfPCell();
        c1.addElement(new Paragraph(desc, bodyDark));
        styleDataCell(c1, bg);

        PdfPCell c2 = new PdfPCell(new Phrase(qty, bodyGray));
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        styleDataCell(c2, bg);

        PdfPCell c3 = new PdfPCell(new Phrase(pu, bodyGray));
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        styleDataCell(c3, bg);

        PdfPCell c4 = new PdfPCell(new Phrase(total, bodyDark));
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        styleDataCell(c4, bg);

        table.addCell(c1);
        table.addCell(c2);
        table.addCell(c3);
        table.addCell(c4);
    }

    private void styleDataCell(PdfPCell cell, BaseColor bg) {
        cell.setBackgroundColor(bg);
        cell.setPadding(10);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        cell.setBorderWidth(0.5f);
    }

    // ══════════════════════════════════════════════════════
    // TOTAL SECTION
    // ══════════════════════════════════════════════════════
    private void addTotalSection(Document doc, Facture facture) throws DocumentException {

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingAfter(24);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, GRAY_TEXT);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, DARK_BG);
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, WHITE);
        Font totalValFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, CYAN);

        addTotalRow(totalTable, "Sous-total HT:", "91,80 DT", labelFont, valueFont, LIGHT_GRAY);
        addTotalRow(totalTable, "TVA (19%):", "17,10 DT", labelFont, valueFont, WHITE);
        addTotalRow(totalTable, "TOTAL TTC:", String.format("%.2f DT", facture.getMontantTotal()), totalFont, totalValFont, DARK_BG);

        doc.add(totalTable);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont, BaseColor bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBackgroundColor(bg);
        lc.setPadding(10);
        lc.setBorder(Rectangle.NO_BORDER);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBackgroundColor(bg);
        vc.setPadding(10);
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(lc);
        table.addCell(vc);
    }

    // ══════════════════════════════════════════════════════
    // NOTES
    // ══════════════════════════════════════════════════════
    private void addNotes(Document doc, Paiement paiement) throws DocumentException {

        PdfPTable notesTable = new PdfPTable(1);
        notesTable.setWidthPercentage(100);
        notesTable.setSpacingAfter(30);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new BaseColor(235, 255, 248));
        cell.setBorderColor(CYAN);
        cell.setBorderWidth(1f);
        cell.setPadding(14);

        Font noteTitle = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, DARK_BG);
        Font noteBody  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

        cell.addElement(new Paragraph("Informations de paiement", noteTitle));
        cell.addElement(new Paragraph("Mode: " + paiement.getModePaiement(), noteBody));
        cell.addElement(new Paragraph("Reference: " + paiement.getReference(), noteBody));
        cell.addElement(new Paragraph("Statut: " + paiement.getStatut(), noteBody));
        cell.addElement(new Paragraph("Date: " + paiement.getDatePaiement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), noteBody));
        cell.addElement(new Paragraph(" ", noteBody));
        cell.addElement(new Paragraph("Merci de conserver cette facture. Elle est votre preuve de paiement officielle.", noteBody));

        notesTable.addCell(cell);
        doc.add(notesTable);
    }

    // ══════════════════════════════════════════════════════
    // FOOTER
    // ══════════════════════════════════════════════════════
    private void addFooter(PdfWriter writer, Document doc, Facture facture) throws DocumentException {
        PdfContentByte canvas = writer.getDirectContent();

        float footerY = 50;

        // Footer bar
        canvas.setColorFill(DARK_PANEL);
        canvas.rectangle(0, 0, PageSize.A4.getWidth(), footerY + 20);
        canvas.fill();

        // Cyan top line
        canvas.setColorFill(CYAN);
        canvas.rectangle(0, footerY + 20, PageSize.A4.getWidth(), 2);
        canvas.fill();

        // Footer text
        canvas.beginText();
        canvas.setFontAndSize(getHelvetica(), 8);
        canvas.setColorFill(GRAY_TEXT);
        canvas.setTextMatrix(40, footerY + 5);
        canvas.showText("NeuroWell SARL · MF: 123456789A · contact@neurowell.tn · +216 71 000 000");
        canvas.endText();

        canvas.beginText();
        canvas.setFontAndSize(getHelvetica(), 8);
        canvas.setColorFill(GRAY_TEXT);
        canvas.setTextMatrix(PageSize.A4.getWidth() - 200, footerY + 5);
        canvas.showText("Facture: " + facture.getNumeroFacture() + " · Page 1/1");
        canvas.endText();
    }

    // ══════════════════════════════════════════════════════
    // HELPER: save Facture to DB
    // ══════════════════════════════════════════════════════
    public void ajouterFacture(Facture facture) throws java.sql.SQLException {
        utils.MyDatabase db = utils.MyDatabase.getInstance();
        java.sql.Connection cnx = db.getConnection();

        String sql = "INSERT INTO facture (numero_facture, date_generation, montant_total, description) VALUES (?,?,?,?)";
        java.sql.PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, facture.getNumeroFacture());
        ps.setDate(2, java.sql.Date.valueOf(facture.getDateGeneration()));
        ps.setDouble(3, facture.getMontantTotal());
        ps.setString(4, facture.getDescription());
        ps.executeUpdate();
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE UTILS
    // ══════════════════════════════════════════════════════
    private com.itextpdf.text.pdf.BaseFont getHelvetica() {
        try {
            return com.itextpdf.text.pdf.BaseFont.createFont(
                    com.itextpdf.text.pdf.BaseFont.HELVETICA,
                    com.itextpdf.text.pdf.BaseFont.CP1252,
                    com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED);
        } catch (Exception e) { return null; }
    }

    private com.itextpdf.text.pdf.BaseFont getHelveticaBold() {
        try {
            return com.itextpdf.text.pdf.BaseFont.createFont(
                    com.itextpdf.text.pdf.BaseFont.HELVETICA_BOLD,
                    com.itextpdf.text.pdf.BaseFont.CP1252,
                    com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED);
        } catch (Exception e) { return null; }
    }
}