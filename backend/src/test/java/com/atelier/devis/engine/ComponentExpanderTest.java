package com.atelier.devis.engine;

import com.atelier.devis.engine.bom.*;
import com.atelier.devis.engine.bom.ComponentExpander.*;
import com.atelier.devis.engine.model.PricingSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduit le fichier MEUBLE_HAUT.xlsx (meuble de 500 x 330 x 900 mm, 1 porte).
 * Les valeurs attendues sont celles calculées par Excel (valeurs en cache du fichier).
 */
class ComponentExpanderTest {

    private static final double ALLOW = 50;
    private final ComponentExpander ex = new ComponentExpander();

    private static LineDef line(String code, String slot, String f) { return new LineDef(code, slot, f, BigDecimal.ZERO); }

    private final TemplateDef caisson = new TemplateDef("Caisson", 10, 0,
            List.of(new PieceDef("Joue", "MEL19", "2*Q", "H", "P", "CHANT", 1, 1),
                    new PieceDef("Dessous / Dessus", "MEL19", "2*Q", "L-38", "P", "CHANT", 1, 0),
                    new PieceDef("Fond", "MEL10", "1*Q", "H-28", "L-14", null, 0, 0)),
            List.of(line("MEL19", "MEL19", null), line("MEL10", "MEL10", null), line("CHANT_BL", "CHANT", null), line("COLLE", null, "CH/50"),
                    line("LAMELLO", null, "8*Q"), line("ATTACHE", null, "2*Q"), line("PROFIL", null, "2*Q"),
                    line("VIS550", null, "4*Q"), line("CHEVILLE", null, "4*Q")));

    private final TemplateDef etagere = new TemplateDef("Etagere", 10, 10,
            List.of(new PieceDef("Etagere", "MEL19", "Q", "L-38", "P-25", "CHANT", 1, 0)),
            List.of(line("MEL19", "MEL19", null), line("CHANT_BL", "CHANT", null), line("COLLE", null, "CH/50"), line("TAQUET", null, "4*Q")));

    private final TemplateDef porte = new TemplateDef("Porte", 10, 0,
            List.of(new PieceDef("Porte", "MEL19D", "Q", "H-10", "L/Q-1", "CHANT", 2, 2)),
            List.of(line("MEL19D", "MEL19D", null), line("CHANT_DEC", "CHANT", null), line("COLLE", null, "CH/50"),
                    line("CHARN", null, "2*Q"), line("EMBASE", null, "2*Q"), line("VIS316", null, "8*Q")));

    private final TemplateDef poignees = new TemplateDef("Poignees", 0, 0, List.of(),
            List.of(line("POIGNEE", null, "1*Q"), line("VIS422", null, "2*Q")));

    private double q(Expanded e, int i) { return e.lines().get(i).quantity().doubleValue(); }

    @Test
    void caisson_surfacesChantColle_commeExcel() {
        Expanded e = ex.expand(caisson, 500, 330, 900, 1, ALLOW);
        assertEquals(0.988812, q(e, 0), 1e-6);      // O14 : mela 19 avec 10 % de chute
        assertEquals(0.4661712, q(e, 1), 1e-6);     // O17 : fond mela 10 avec 10 % de chute
        assertEquals(3.684, q(e, 2), 1e-6);         // P14 : chant
        assertEquals(0.07368, q(e, 3), 1e-6);       // D13 : colle = chant / 50
        assertEquals(8, q(e, 4), 1e-9); assertEquals(2, q(e, 5), 1e-9); assertEquals(2, q(e, 6), 1e-9);
        assertEquals(4, q(e, 7), 1e-9); assertEquals(4, q(e, 8), 1e-9);
        assertEquals("SURFACE", e.lines().get(0).kind()); assertEquals("CHANT", e.lines().get(2).kind());
        assertEquals("FORMULA", e.lines().get(3).kind());
        assertEquals(0.594, e.pieces().get(0).surfaceM2(), 1e-9);       // O10 joues
        assertEquals(2.66, e.pieces().get(0).chantM(), 1e-9);           // P10
        assertEquals(0.30492, e.pieces().get(1).surfaceM2(), 1e-9);     // O11
        assertEquals(1.024, e.pieces().get(1).chantM(), 1e-9);          // P11
    }

    @Test
    void etagere_commeExcel() {
        Expanded e = ex.expand(etagere, 500, 330, 900, 1, ALLOW);
        assertEquals(0.155001, q(e, 0), 1e-6);      // O24
        assertEquals(0.5632, q(e, 1), 1e-6);        // P24 : chant avec 10 % de chute
        assertEquals(0.011264, q(e, 2), 1e-6);      // D27
        assertEquals(4, q(e, 3), 1e-9);             // taquets
    }

    @Test
    void porte_commeExcel() {
        Expanded e = ex.expand(porte, 500, 330, 900, 1, ALLOW);
        assertEquals(0.488521, q(e, 0), 1e-6);      // O34
        assertEquals(2.978, q(e, 1), 1e-6);         // P34
        assertEquals(0.05956, q(e, 2), 1e-6);       // D38
        assertEquals(2, q(e, 3), 1e-9); assertEquals(2, q(e, 4), 1e-9); assertEquals(8, q(e, 5), 1e-9);
        Expanded p = ex.expand(poignees, 500, 330, 900, 1, ALLOW);
        assertEquals(1, q(p, 0), 1e-9); assertEquals(2, q(p, 1), 1e-9);
    }

    @Test
    void deuxPortes_largeurDivisee_etQuincaillerieProportionnelle() {
        Expanded e = ex.expand(porte, 1000, 330, 900, 2, ALLOW);
        assertEquals(2, e.pieces().get(0).qty(), 1e-9);
        assertEquals(499, e.pieces().get(0).dim2(), 1e-9);      // 1000 / 2 - 1
        assertEquals(4, q(e, 3), 1e-9);                         // 4 charnières pour 2 portes
        assertEquals(16, q(e, 5), 1e-9);
    }

    @Test
    void changerLesMesuresRecalculeLesSurfaces() {
        Expanded a = ex.expand(caisson, 500, 330, 900, 1, ALLOW);
        Expanded b = ex.expand(caisson, 600, 330, 900, 1, ALLOW);
        assertTrue(q(b, 0) > q(a, 0));
        assertTrue(q(b, 1) > q(a, 1));
        assertEquals(8, q(b, 4), 1e-9);                         // la quincaillerie par défaut ne change pas
    }

    @Test
    void devisExcelComplet_totauxIdentiques() {
        Map<String, Article> cat = new HashMap<>();
        for (Article a : List.of(
                new Article("MEL19", "mela 19", "PANNEAU", "M2", bd("10.955077"), bd("1.45")),
                new Article("MEL10", "mela 10", "PANNEAU", "M2", bd("8.375305"), bd("1.45")),
                new Article("MEL19D", "mela 19 decor", "PANNEAU", "M2", bd("11.56"), bd("1.45")),
                new Article("CHANT_BL", "chant bl", "CHANT", "ML", bd("0.22"), bd("1.45")),
                new Article("CHANT_DEC", "chant decor", "CHANT", "ML", bd("0.50"), bd("1.45")),
                new Article("COLLE", "colle", "COLLE", "U", bd("4.444444"), bd("1.1")),
                new Article("LAMELLO", "l", "QUINCAILLERIE", "P", bd("0.05"), bd("1.6")),
                new Article("ATTACHE", "a", "QUINCAILLERIE", "P", bd("3.51"), bd("1.6")),
                new Article("PROFIL", "p", "QUINCAILLERIE", "P", bd("0.32"), bd("1.6")),
                new Article("VIS550", "v", "QUINCAILLERIE", "P", bd("0.04"), bd("1")),
                new Article("CHEVILLE", "c", "QUINCAILLERIE", "P", bd("0.04"), bd("1")),
                new Article("TAQUET", "t", "QUINCAILLERIE", "P", bd("0.05"), bd("1.6")),
                new Article("CHARN", "ch", "QUINCAILLERIE", "P", bd("2.74"), bd("1.6")),
                new Article("EMBASE", "e", "QUINCAILLERIE", "P", bd("0.47"), bd("1.6")),
                new Article("VIS316", "v", "QUINCAILLERIE", "P", bd("0.01"), bd("1.6")),
                new Article("POIGNEE", "po", "QUINCAILLERIE", "P", bd("2"), bd("1")),
                new Article("VIS422", "v", "QUINCAILLERIE", "P", bd("0.01"), bd("1.6")))) cat.put(a.code(), a);

        List<BomInput.ComponentInput> comps = new ArrayList<>();
        for (TemplateDef t : List.of(caisson, etagere, porte, poignees)) {
            Expanded e = ex.expand(t, 500, 330, 900, 1, ALLOW);
            List<BomInput.LineInput> lines = new ArrayList<>();
            for (LineResult l : e.lines()) lines.add(new BomInput.LineInput(l.articleCode(), l.quantity()));
            comps.add(new BomInput.ComponentInput(t.name(), 500.0, 330.0, 900.0, 1, lines));
        }
        PricingSettings none = new PricingSettings(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, BigDecimal.ZERO, "EUR", List.of());
        BomResult r = new BomCalculator().compute(new BomInput("Meuble haut 50", 1, comps, null), cat::get, none);
        // Excel : F20 = 36,12 ; F30 = 3,02 ; F44 = 21,04 ; F50 = 2,03 ; F52 (PRIX REVIENT TOTAL) = 62,2075
        assertEquals(new BigDecimal("36.12"), r.components().get(0).total());
        assertEquals(new BigDecimal("3.02"), r.components().get(1).total());
        assertEquals(new BigDecimal("21.04"), r.components().get(2).total());
        assertEquals(new BigDecimal("2.03"), r.components().get(3).total());
        assertEquals(new BigDecimal("62.21"), r.materialsPerUnit());
    }

    @Test
    void evaluateurDeFormules() {
        Map<String, Double> v = Map.of("L", 500.0, "Q", 2.0);
        assertEquals(1000, FormulaEvaluator.eval("2*L", v), 1e-9);
        assertEquals(249, FormulaEvaluator.eval("L/Q-1", v), 1e-9);
        assertEquals(-4, FormulaEvaluator.eval("-(2+2)", v), 1e-9);
        assertEquals(1.5, FormulaEvaluator.eval("1,5", v), 1e-9);
        assertEquals(14, FormulaEvaluator.eval("2 + 3 * 4", v), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("L/0", v));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("X+1", v));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("2*(3", v));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("2 3", v));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("", v));
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }
}
