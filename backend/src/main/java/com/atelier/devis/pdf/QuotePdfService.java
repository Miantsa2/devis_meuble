package com.atelier.devis.pdf;

import com.atelier.devis.domain.Entities.QuoteEntity;
import com.atelier.devis.engine.model.FurnitureSpec;
import com.atelier.devis.engine.service.QuoteResult;
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

/** Devis PDF : client, description, détail des coûts, totaux et hypothèses. Généré à partir du résultat figé. */
@Service
public class QuotePdfService {

    private static final Color BRAND = new Color(0x3b, 0x2a, 0x1e);
    private static final Font H1 = new Font(Font.HELVETICA, 20, Font.BOLD, BRAND);
    private static final Font H2 = new Font(Font.HELVETICA, 12, Font.BOLD, BRAND);
    private static final Font TXT = new Font(Font.HELVETICA, 9);
    private static final Font BOLD = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font SMALL = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY);
    private static final Font WHITE = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

    public byte[] render(QuoteEntity q, FurnitureSpec s, QuoteResult r) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2);
        String cur = r.currency() == null ? "" : " " + r.currency();

        doc.add(new Paragraph("DEVIS " + q.getReference(), H1));
        doc.add(new Paragraph("Date : " + DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC).format(q.getCreatedAt()), TXT));
        doc.add(new Paragraph("Client : " + nvl(q.getCustomerName()) + (q.getCustomerContact() == null ? "" : "  -  " + q.getCustomerContact()), TXT));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Désignation", H2));
        doc.add(new Paragraph(String.format(Locale.FRANCE, "Meuble « %s » : %.0f x %.0f x %.0f mm (L x H x P) - quantité : %d",
                s.typeCode(), s.widthMm(), s.heightMm(), s.depthMm(), r.units()), TXT));
        doc.add(new Paragraph(String.format("%d porte(s), %d tiroir(s), %d séparation(s), %d étagère(s) par compartiment",
                s.doors(), s.drawers(), s.dividers(), s.shelvesPerCompartment()), TXT));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Détail des coûts", H2));
        PdfPTable t = new PdfPTable(new float[]{5, 2, 2});
        t.setWidthPercentage(100); t.setSpacingBefore(4);
        header(t, "Poste", "Quantité", "Montant" + cur);
        for (QuoteResult.PanelUsage p : r.panels())
            row(t, "Panneau " + p.name() + " (" + p.thicknessMm() + " mm)", p.sheets() + " feuille(s)", nf.format(p.cost()));
        if (r.edge() != null)
            row(t, "Chant " + r.edge().name(), String.format(Locale.FRANCE, "%.1f m", r.edge().purchasedMeters()), nf.format(r.edge().cost()));
        for (var h : r.hardware()) row(t, h.name(), String.valueOf(h.qty()), nf.format(h.total()));
        row(t, "Main-d'œuvre", String.format(Locale.FRANCE, "%.2f h", r.labor().hours()), nf.format(r.labor().cost()));
        row(t, "Finition", String.format(Locale.FRANCE, "%.2f m²", r.finishAreaM2()), nf.format(r.costs().finish()));
        doc.add(t);

        var c = r.costs();
        PdfPTable tot = new PdfPTable(new float[]{7, 2});
        tot.setWidthPercentage(100); tot.setSpacingBefore(8);
        total(tot, "Coût de production", c.productionCost(), nf, false);
        total(tot, "Marge commerciale (" + c.markupPercent().stripTrailingZeros().toPlainString() + " %)", c.markup(), nf, false);
        if (c.transport().signum() != 0) total(tot, "Transport", c.transport(), nf, false);
        if (c.installation().signum() != 0) total(tot, "Installation", c.installation(), nf, false);
        total(tot, "TOTAL HT", c.totalHt(), nf, true);
        for (var tx : c.taxes()) total(tot, tx.label() + " (" + tx.ratePercent().stripTrailingZeros().toPlainString() + " %)", tx.amount(), nf, false);
        total(tot, "TOTAL TTC" + cur, c.totalTtc(), nf, true);
        doc.add(tot);

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Hypothèses de calcul", H2));
        doc.add(new Paragraph("Quantités issues du calcul paramétrique de fabrication (pièces, plan de débit avec trait de scie et parage, "
                + "chants avec surlongueur, quincaillerie). Les prix sont ceux en vigueur à la date d'émission. "
                + "Le plan de débit est une estimation : le nombre de feuilles réellement consommées peut varier.", SMALL));
        doc.close();
        return out.toByteArray();
    }

    private void header(PdfPTable t, String... cols) {
        for (String col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(col, WHITE));
            c.setBackgroundColor(BRAND); c.setPadding(5); t.addCell(c);
        }
    }

    private void row(PdfPTable t, String a, String b, String c) {
        String[] vals = {a, b, c};
        for (int i = 0; i < vals.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(vals[i], TXT));
            cell.setPadding(4); cell.setBorderColor(Color.LIGHT_GRAY);
            if (i > 0) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(cell);
        }
    }

    private void total(PdfPTable t, String label, BigDecimal v, NumberFormat nf, boolean strong) {
        PdfPCell a = new PdfPCell(new Phrase(label, strong ? BOLD : TXT));
        PdfPCell b = new PdfPCell(new Phrase(nf.format(v), strong ? BOLD : TXT));
        for (PdfPCell x : new PdfPCell[]{a, b}) { x.setBorder(strong ? Rectangle.TOP : Rectangle.NO_BORDER); x.setPadding(3); }
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(a); t.addCell(b);
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
