package com.atelier.devis.engine;

import com.atelier.devis.engine.bom.*;
import com.atelier.devis.engine.model.PricingSettings;
import com.atelier.devis.engine.model.Tax;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Reprend les lignes du devis Excel de référence (prix unitaires et quantités saisis, coefficient par article). */
class BomCalculatorTest {

    private static Article art(String code, String cat, String unit, String pu, String coef) {
        return new Article(code, code, cat, unit, new BigDecimal(pu), new BigDecimal(coef));
    }

    private final Map<String, Article> catalog = new HashMap<>();
    {
        for (Article a : List.of(
                art("MEL19", "PANNEAU", "M2", "10.96", "1.45"), art("MEL10", "PANNEAU", "M2", "8.38", "1.45"),
                art("MEL19D", "PANNEAU", "M2", "11.56", "1.45"), art("CHANT_BL", "CHANT", "ML", "0.22", "1.46"),
                art("CHANT_DEC", "CHANT", "ML", "0.50", "1.45"), art("COLLE", "COLLE", "U", "4.44", "1.1"),
                art("LAMELLO", "QUINCAILLERIE", "PCE", "0.05", "1.6"), art("ATTACHE", "QUINCAILLERIE", "PCE", "3.51", "1.6"),
                art("PROFIL", "QUINCAILLERIE", "PCE", "0.32", "1.6"), art("VIS550", "QUINCAILLERIE", "PCE", "0.04", "1.0"),
                art("CHEVILLE", "QUINCAILLERIE", "PCE", "0.04", "1.0"), art("TAQUET", "QUINCAILLERIE", "PCE", "0.05", "1.6"),
                art("CHARN", "QUINCAILLERIE", "PCE", "2.74", "1.6"), art("EMBASE", "QUINCAILLERIE", "PCE", "0.47", "1.6"),
                art("VIS316", "QUINCAILLERIE", "PCE", "0.01", "1.6"), art("POIGNEE", "QUINCAILLERIE", "PCE", "2.00", "1.0")))
            catalog.put(a.code(), a);
    }
    private final ArticleCatalog cat = catalog::get;

    private PricingSettings pricing(String markup, String transport, String install, String hourly, String vat) {
        return new PricingSettings(new BigDecimal(hourly), BigDecimal.ZERO, new BigDecimal(transport), new BigDecimal(install),
                new BigDecimal(markup), 0, BigDecimal.ZERO, "TST",
                vat.equals("0") ? List.of() : List.of(new Tax("TVA", "TVA", new BigDecimal(vat))));
    }

    private BomInput.LineInput line(String code, String q) { return new BomInput.LineInput(code, new BigDecimal(q)); }

    private BomInput.ComponentInput comp(String name, int qty, BomInput.LineInput... lines) {
        return new BomInput.ComponentInput(name, 500.0, 330.0, 900.0, qty, List.of(lines));
    }

    private BomResult.LineResult find(BomResult r, String comp, String code) {
        return r.components().stream().filter(c -> c.name().equals(comp)).findFirst().orElseThrow()
                .lines().stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void lignesQuincaillerieIdentiquesAuTableauExcel() {
        var porte = comp("Porte", 1, line("MEL19D", "0.49"), line("CHANT_DEC", "2.98"), line("COLLE", "0.06"),
                line("CHARN", "2"), line("EMBASE", "2"), line("VIS316", "8"));
        var caisson = comp("Caisson", 1, line("LAMELLO", "8"), line("ATTACHE", "2"), line("PROFIL", "2"),
                line("VIS550", "4"), line("CHEVILLE", "4"));
        var etagere = comp("Etagere", 1, line("TAQUET", "4"));
        var poignees = comp("Poignees", 1, line("POIGNEE", "1"));
        BomResult r = new BomCalculator().compute(new BomInput("Meuble", 1, List.of(porte, caisson, etagere, poignees), null),
                cat, pricing("0", "0", "0", "0", "0"));
        // valeurs de la feuille Excel : 8,77 / 1,50 / 0,13 / 0,64 / 11,23 / 1,02 / 0,16 / 0,16 / 0,32 / 2,00
        assertEquals(new BigDecimal("8.77"), find(r, "Porte", "CHARN").amount());
        assertEquals(new BigDecimal("1.50"), find(r, "Porte", "EMBASE").amount());
        assertEquals(new BigDecimal("0.13"), find(r, "Porte", "VIS316").amount());
        assertEquals(new BigDecimal("0.64"), find(r, "Caisson", "LAMELLO").amount());
        assertEquals(new BigDecimal("11.23"), find(r, "Caisson", "ATTACHE").amount());
        assertEquals(new BigDecimal("1.02"), find(r, "Caisson", "PROFIL").amount());
        assertEquals(new BigDecimal("0.16"), find(r, "Caisson", "VIS550").amount());
        assertEquals(new BigDecimal("0.16"), find(r, "Caisson", "CHEVILLE").amount());
        assertEquals(new BigDecimal("0.32"), find(r, "Etagere", "TAQUET").amount());
        assertEquals(new BigDecimal("2.00"), find(r, "Poignees", "POIGNEE").amount());
    }

    @Test
    void totauxCoherents_composantEtMeuble() {
        var shelf = comp("Etagere", 3, line("MEL19", "0.16"), line("TAQUET", "4"));
        var door = comp("Porte", 1, line("CHARN", "2"));
        BomResult r = new BomCalculator().compute(new BomInput("M", 2, List.of(shelf, door), null), cat, pricing("0", "0", "0", "0", "0"));
        var s = r.components().get(0);
        BigDecimal sumLines = s.lines().stream().map(BomResult.LineResult::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(sumLines, s.unitTotal());
        assertEquals(s.unitTotal(), s.total());   // les quantités de ligne incluent déjà QTE
        assertEquals(s.total().add(r.components().get(1).total()), r.materialsPerUnit());
        assertEquals(r.materialsPerUnit().multiply(BigDecimal.valueOf(2)), r.materialsTotal());   // quantité de meubles
    }

    @Test
    void chaineDePrix_margeTransportInstallationTaxes() {
        var c = comp("Porte", 1, line("CHARN", "2"));                     // 8,77
        BomResult r = new BomCalculator().compute(new BomInput("M", 2, List.of(c), new BigDecimal("1.5")),
                cat, pricing("20", "5", "3", "10", "20"));
        assertEquals(new BigDecimal("17.54"), r.materialsTotal());       // 8,77 x 2 meubles
        assertEquals(new BigDecimal("30.00"), r.laborCost());            // 1,5 h x 10 x 2 meubles
        assertEquals(new BigDecimal("47.54"), r.productionCost());
        assertEquals(new BigDecimal("9.51"), r.markup());                // 20 % de 47,54 = 9,508
        assertEquals(new BigDecimal("6.00"), r.installation());          // 3 x 2 meubles
        assertEquals(new BigDecimal("68.05"), r.totalHt());              // 47,54 + 9,51 + 5 + 6
        assertEquals(new BigDecimal("13.61"), r.taxes().get(0).amount());   // 20 % de 68,05
        assertEquals(new BigDecimal("81.66"), r.totalTtc());
    }

    @Test
    void erreursEtAvertissements() {
        var calc = new BomCalculator();
        assertThrows(IllegalArgumentException.class, () -> calc.compute(new BomInput("M", 1, List.of(), null), cat, pricing("0", "0", "0", "0", "0")));
        assertThrows(IllegalArgumentException.class, () -> calc.compute(
                new BomInput("M", 1, List.of(comp("C", 1, line("INCONNU", "1"))), null), cat, pricing("0", "0", "0", "0", "0")));
        assertThrows(IllegalArgumentException.class, () -> calc.compute(
                new BomInput("M", 1, List.of(comp("C", 1, line("MEL19", "-1"))), null), cat, pricing("0", "0", "0", "0", "0")));
        catalog.put("GRATUIT", art("GRATUIT", "AUTRE", "U", "0", "1"));
        BomResult r = calc.compute(new BomInput("M", 1, List.of(comp("C", 1, line("GRATUIT", "1"))), null), cat, pricing("0", "0", "0", "0", "0"));
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("Prix non renseigné")));
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("Aucune taxe")));
    }
}
