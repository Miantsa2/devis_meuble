package com.atelier.devis.pdf;

import com.atelier.devis.domain.BomEntities.BomQuoteEntity;
import com.atelier.devis.engine.bom.BomResult;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Deux documents : le devis client (désignation et totaux, sans coûts ni coefficients)
 * et la fiche de revient interne (toutes les lignes : quantité, prix unitaire, coefficient, montant).
 */
@Service
public class BomPdfService {
    private static final Color BRAND = new Color(0x3b, 0x2a, 0x1e);
    private static final Font H1 = new Font(Font.HELVETICA, 20, Font.BOLD, BRAND);
    private static final Font H2 = new Font(Font.HELVETICA, 11, Font.BOLD, BRAND);
    private static final Font TXT = new Font(Font.HELVETICA, 9);
    private static final Font BOLD = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font WHITE = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

    public byte[] render(BomQuoteEntity q, BomResult r, boolean detail) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();
        NumberFormat money = NumberFormat.getNumberInstance(Locale.FRANCE);
        money.setMinimumFractionDigits(2); money.setMaximumFractionDigits(2);
        NumberFormat qty = NumberFormat.getNumberInstance(Locale.FRANCE);
        qty.setMaximumFractionDigits(4);
        String cur = r.currency() == null || r.currency().isBlank() ? "" : " " + r.currency();

        doc.add(new Paragraph((detail ? "FICHE DE REVIENT " : "DEVIS ") + q.getReference(), H1));
        doc.add(new Paragraph("Date : " + DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC).format(q.getCreatedAt()), TXT));
        doc.add(new Paragraph("Client : " + nvl(q.getCustomerName()) + (blank(q.getCustomerContact()) ? "" : "  -  " + q.getCustomerContact()), TXT));
        doc.add(new Paragraph("Meuble : " + nvl(r.name()) + "  -  quantité : " + r.quantity(), BOLD));
        doc.add(Chunk.NEWLINE);

        if (detail) {
            for (BomResult.ComponentResult c : r.components()) {
                doc.add(new Paragraph(c.name() + dims(c) + "  x " + c.qty(), H2));
                PdfPTable t = new PdfPTable(new float[]{4, 1.5f, 1.3f, 1.1f, 1.5f});
                t.setWidthPercentage(100); t.setSpacingBefore(3);
                head(t, "Article", "Quantité", "PU", "Coef.", "Montant");
                for (BomResult.LineResult l : c.lines())
                    row(t, l.name(), qty.format(l.quantity()) + " " + l.unit(), money.format(l.unitPrice()), qty.format(l.coef()), money.format(l.amount()));
                row(t, "Total composant", "", "", "", money.format(c.total()));
                doc.add(t);
                doc.add(Chunk.NEWLINE);
            }
        } else {
            PdfPTable t = new PdfPTable(new float[]{6, 2});
            t.setWidthPercentage(100);
            head(t, "Désignation", "Quantité");
            for (BomResult.ComponentResult c : r.components())
                row2(t, c.name() + dims(c), String.valueOf(c.qty() * r.quantity()));
            doc.add(t);
            doc.add(Chunk.NEWLINE);
        }

        PdfPTable tot = new PdfPTable(new float[]{7, 2});
        tot.setWidthPercentage(100);
        if (detail) {
            total(tot, "Matériaux (par meuble)", r.materialsPerUnit(), money, false);
            total(tot, "Matériaux (x " + r.quantity() + ")", r.materialsTotal(), money, false);
            total(tot, "Main-d'œuvre (" + qty.format(r.laborHours()) + " h par meuble)", r.laborCost(), money, false);
            total(tot, "Coût de production", r.productionCost(), money, true);
            total(tot, "Marge (" + qty.format(r.markupPercent()) + " %)", r.markup(), money, false);
            if (r.transport().signum() != 0) total(tot, "Transport", r.transport(), money, false);
            if (r.installation().signum() != 0) total(tot, "Installation", r.installation(), money, false);
        }
        total(tot, "TOTAL HT" + cur, r.totalHt(), money, true);
        for (BomResult.TaxLine t : r.taxes()) total(tot, t.label() + " (" + qty.format(t.ratePercent()) + " %)", t.amount(), money, false);
        total(tot, "TOTAL TTC" + cur, r.totalTtc(), money, true);
        doc.add(tot);
        doc.close();
        return out.toByteArray();
    }

    private String dims(BomResult.ComponentResult c) {
        if (c.widthMm() == null && c.depthMm() == null && c.heightMm() == null) return "";
        return String.format(Locale.FRANCE, " (%s x %s x %s mm)", d(c.widthMm()), d(c.depthMm()), d(c.heightMm()));
    }
    private String d(Double v) { return v == null ? "-" : String.valueOf(Math.round(v)); }

    private void head(PdfPTable t, String... cols) {
        for (String c : cols) { PdfPCell x = new PdfPCell(new Phrase(c, WHITE)); x.setBackgroundColor(BRAND); x.setPadding(4); t.addCell(x); }
    }
    private void row(PdfPTable t, String... vals) {
        for (int i = 0; i < vals.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(vals[i], TXT)); c.setPadding(3); c.setBorderColor(Color.LIGHT_GRAY);
            if (i > 0) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(c);
        }
    }
    private void row2(PdfPTable t, String a, String b) { row(t, a, b); }
    private void total(PdfPTable t, String label, BigDecimal v, NumberFormat nf, boolean strong) {
        PdfPCell a = new PdfPCell(new Phrase(label, strong ? BOLD : TXT));
        PdfPCell b = new PdfPCell(new Phrase(nf.format(v), strong ? BOLD : TXT));
        for (PdfPCell x : new PdfPCell[]{a, b}) { x.setBorder(strong ? Rectangle.TOP : Rectangle.NO_BORDER); x.setPadding(3); }
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(a); t.addCell(b);
    }
    private String nvl(String s) { return s == null ? "" : s; }
    private boolean blank(String s) { return s == null || s.isBlank(); }
}
