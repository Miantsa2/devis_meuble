package com.atelier.devis.engine;

import com.atelier.devis.engine.model.*;
import com.atelier.devis.engine.service.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de vérification mathématique (géométrie, chants, flèche, système 32) et scénarios de fabrication.
 * Les valeurs attendues ont été calculées à la main (voir commentaires). Les estimations de pertes/temps ne sont
 * PAS validées ici : elles doivent l'être sur des cas réels (docs/VALIDATION.md).
 */
class EngineTest {

    private final CarcassGeometryService geo = new CarcassGeometryService();

    private CarcassGeometryService.Result meubleHautGeometry() {
        return geo.generate(TestFixtures.meubleHaut(1), TestFixtures.carcass(0), TestFixtures.back(0),
                TestFixtures.carcass(0), null, TestFixtures.rules());
    }

    private Part find(List<Part> parts, String role) {
        return parts.stream().filter(p -> p.role().equals(role)).findFirst().orElseThrow();
    }

    @Test
    void geometrieMeubleHaut() {
        var parts = meubleHautGeometry().parts();
        Part side = find(parts, "SIDE");
        assertEquals(2, side.qty()); assertEquals(720, side.a(), 1e-9); assertEquals(330, side.b(), 1e-9);
        Part top = find(parts, "TOP");                       // 800 - 2x18 = 764
        assertEquals(764, top.a(), 1e-9); assertEquals(330, top.b(), 1e-9);
        Part shelf = find(parts, "SHELF");                   // 764 - 2 (jeu) = 762 ; 330 - 20 = 310
        assertEquals(762, shelf.a(), 1e-9); assertEquals(310, shelf.b(), 1e-9);
        Part door = find(parts, "DOOR");                     // h = 720-2-2 = 716 ; l = (800-4-3)/2 = 396.5
        assertEquals(716, door.a(), 1e-9); assertEquals(396.5, door.b(), 1e-9); assertEquals(2, door.qty());
        Part back = find(parts, "BACK");
        assertEquals(720, back.a(), 1e-9); assertEquals(800, back.b(), 1e-9);
    }

    @Test
    void largeursDePortesPlusJeuxEgalentLaLargeur() {
        var g = meubleHautGeometry();
        double sum = g.doorSizes().stream().mapToDouble(d -> d[1]).sum();
        assertEquals(800, sum + 2 * 2 + 1 * 3, 1e-9);
    }

    @Test
    void conservationDesLargeursAvecSeparations() {
        FurnitureSpec s = new FurnitureSpec("DEFAULT", 1200, 720, 330, 1, "MFC18", "FOND3", "MFC18", "CH1",
                true, 2, 2, 3, 0, 0, false, 0, "HINGE", null, "PIN", null, "CONN", null, null);
        var g = geo.generate(s, TestFixtures.carcass(0), TestFixtures.back(0), TestFixtures.carcass(0), null, TestFixtures.rules());
        // 3 compartiments : 3 x compW + 2 x 18 = 1200 - 36
        assertEquals(1200 - 36, 3 * g.compartmentWidthMm() + 2 * 18, 1e-9);
        assertEquals(6, find(g.parts(), "SHELF").qty());    // 3 compartiments x 2 étagères
    }

    @Test
    void dimensionsInvalidesRejetees() {
        FurnitureSpec s = new FurnitureSpec("DEFAULT", 30, 720, 330, 1, "MFC18", "FOND3", "MFC18", "CH1",
                true, 0, 0, 0, 0, 0, false, 0, null, null, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () ->
                geo.generate(s, TestFixtures.carcass(0), TestFixtures.back(0), TestFixtures.carcass(0), null, TestFixtures.rules()));
    }

    @Test
    void longueurDeChantsMeubleHaut() {
        EdgeBandingService svc = new EdgeBandingService();
        var r = svc.compute(meubleHautGeometry().parts(), TestFixtures.rules());
        // portes : 2 x (2x716 + 2x396.5) = 4450 ; côtés 2x720 = 1440 ; dessus+dessous 2x764 = 1528 ; étagère 762
        assertEquals(8180, r.netMm(), 1e-9);
        // bandes : 8 + 2 + 2 + 1 = 13 ; surlongueur 13 x 2 x 10 = 260
        assertEquals(13, r.strips());
        assertEquals(8440, r.withOffsetMm(), 1e-9);
        assertEquals(8440 * 1.10, r.totalMm(), 1e-9);
    }

    @Test
    void dimensionsDeDebitDeduisentLEpaisseurDuChant() {
        EdgeBandingService svc = new EdgeBandingService();
        Part side = new Part("SIDE", "Côté", 1, 720, 330, "MFC18", true, 1, 0);
        double[] core = svc.coreDims(side, 1.0);   // chant sur un bord de longueur 720 -> b - 1
        assertEquals(720, core[0], 1e-9); assertEquals(329, core[1], 1e-9);
        Part door = new Part("DOOR", "Porte", 1, 716, 396.5, "MFC18", true, 2, 2);
        double[] c2 = svc.coreDims(door, 1.0);
        assertEquals(714, c2[0], 1e-9); assertEquals(394.5, c2[1], 1e-9);
    }

    @Test
    void debitPetitCasUneFeuille() {
        var items = new ArrayList<CutPlanService.Item>();
        for (int i = 0; i < 4; i++) items.add(new CutPlanService.Item("P" + i, 600, 400, false));
        var r = new CutPlanService().plan("X", items, 2440, 1220, false, 0, 0);
        assertEquals(1, r.sheetCount());
        assertEquals(4 * 0.24, r.netAreaM2(), 1e-9);
    }

    @Test
    void debitSansChevauchementNiDepassementAvecTrait() {
        Random rnd = new Random(42);
        for (int run = 0; run < 50; run++) {
            var items = new ArrayList<CutPlanService.Item>();
            int n = 5 + rnd.nextInt(40);
            for (int i = 0; i < n; i++)
                items.add(new CutPlanService.Item("P" + i, 150 + rnd.nextInt(1200), 80 + rnd.nextInt(600), rnd.nextBoolean()));
            double kerf = 3.2, trim = 10;
            var r = new CutPlanService().plan("X", items, 2440, 1220, true, trim, kerf);
            double eps = 1e-6;
            int placed = 0;
            for (var sheet : r.sheets()) {
                var ps = sheet.placements();
                placed += ps.size();
                for (var p : ps) {
                    assertTrue(p.x() >= trim - eps && p.y() >= trim - eps);
                    assertTrue(p.x() + p.w() <= 2440 - trim + eps, "dépassement en x");
                    assertTrue(p.y() + p.h() <= 1220 - trim + eps, "dépassement en y");
                }
                for (int i = 0; i < ps.size(); i++) for (int j = i + 1; j < ps.size(); j++) {
                    var a = ps.get(i); var b = ps.get(j);
                    double gapX = Math.max(a.x() - (b.x() + b.w()), b.x() - (a.x() + a.w()));
                    double gapY = Math.max(a.y() - (b.y() + b.h()), b.y() - (a.y() + a.h()));
                    assertTrue(gapX >= kerf - eps || gapY >= kerf - eps, "pièces trop proches ou superposées");
                }
            }
            assertEquals(n, placed);
            double usable = (2440 - 2 * trim) * (1220 - 2 * trim) / 1e6;
            assertTrue(r.sheetCount() >= Math.ceil(r.netAreaM2() / usable) - 1e-9, "moins de feuilles que la borne inférieure");
        }
    }

    @Test
    void filImposeRespecteLeSensDeLaFeuille() {
        var items = List.of(new CutPlanService.Item("Long", 2000, 500, true), new CutPlanService.Item("Long2", 2000, 500, true));
        var r = new CutPlanService().plan("X", items, 2440, 1220, true, 0, 3.2);
        for (var s : r.sheets()) for (var p : s.placements()) {
            assertEquals(2000, p.w(), 1e-9);   // le fil (dimension a) reste le long de la longueur de feuille
            assertEquals(500, p.h(), 1e-9);
        }
    }

    @Test
    void pieceTropGrandeEstRefusee() {
        var items = List.of(new CutPlanService.Item("Géante", 3000, 500, false));
        assertThrows(IllegalArgumentException.class, () -> new CutPlanService().plan("X", items, 2440, 1220, false, 10, 3.2));
    }

    @Test
    void flecheEtagere_formuleEtLoisDEchelle() {
        StructuralCheckService s = new StructuralCheckService();
        double load = 40, depth = 300, t = 18, L = 600, E = 2500;
        double d = s.deflectionMm(load, depth, t, L, E);
        // Calcul indépendant avec la charge totale W : d = 5 W L^3 / (384 E I)
        double W = load * 9.80665 / 1e6 * depth * L;                // N
        double I = depth * t * t * t / 12.0;
        assertEquals(5 * W * Math.pow(L, 3) / (384 * E * I), d, 1e-12);
        // Portée doublée à charge totale constante => flèche x8 ; épaisseur doublée => /8
        assertEquals(8 * d, s.deflectionMm(load / 2, depth, t, 2 * L, E), 1e-9);
        assertEquals(d / 8, s.deflectionMm(load, depth, 2 * t, L, E), 1e-9);
        // Portée max : à cette portée, flèche = portée / ratio
        double max = s.maxSpanMm(load, depth, t, E, 400);
        assertEquals(max / 400, s.deflectionMm(load, depth, t, max, E), 1e-9);
    }

    @Test
    void systeme32_nombreDeTrous() {
        System32Service s = new System32Service();
        Rules r = TestFixtures.rules();
        assertEquals(18, s.holesPerRow(684, r));        // (684 - 128) / 32 = 17.375 -> 17 intervalles -> 18 trous
        assertEquals(2, s.rows(330, r));                 // 330 - 74 = 256 >= 32
        assertEquals(1, s.rows(100, r));                 // 100 - 74 = 26 < 32
        assertEquals(72, s.shelfPinHoles(1, 684, 330, r));   // 1 compartiment x 2 faces x 2 rangées x 18
    }

    @Test
    void mainDOeuvre_arithmetique() {
        var ops = List.of(new LaborOp("A", "A", LaborBasis.PER_CUT, new BigDecimal("1.5")));
        var res = new LaborService().compute(ops, Map.of(LaborBasis.PER_CUT, 40.0), new BigDecimal("12.00"));
        // 40 x 1.5 min = 60 min = 1 h -> 12.00
        assertEquals(new BigDecimal("1.0000"), res.hours());
        assertEquals(new BigDecimal("12.00"), res.cost());
    }

    @Test
    void scenarioMeubleHaut_charnieresEtTotaux() {
        var catalog = TestFixtures.catalog(100, 2, 1);
        var cfg = TestFixtures.config(10, 1, 20, 10);
        QuoteResult q = new QuoteEngine().compute(TestFixtures.meubleHaut(1), catalog, cfg);
        // poids porte = 0.716 x 0.3965 x 0.018 x 650 = 3.3 kg ; hauteur 716 : 2 charnières par porte
        assertEquals(4, q.hardware().stream().filter(l -> l.category().equals("HINGE")).mapToInt(l -> l.qty()).sum());
        assertEquals(4 * 1, q.hardware().stream().filter(l -> l.category().equals("SHELF_PIN")).mapToInt(l -> l.qty()).sum());
        var c = q.costs();
        BigDecimal production = c.panels().add(c.edge()).add(c.hardware()).add(c.labor()).add(c.finish());
        assertEquals(production, c.productionCost());
        assertEquals(production.multiply(new BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP), c.markup());
        assertEquals(c.productionCost().add(c.markup()).add(c.transport()).add(c.installation()), c.totalHt());
        assertEquals(c.totalHt().add(c.taxes().get(0).amount()), c.totalTtc());
        assertEquals(new BigDecimal("5.00"), c.transport());
        assertEquals(new BigDecimal("2.00"), c.installation());
    }

    @Test
    void echelleDeQuantite_leDebitMutualise() {
        var catalog = TestFixtures.catalog(100, 1, 1);
        var cfg = TestFixtures.config(10, 0, 0, 0);
        var q1 = new QuoteEngine().compute(TestFixtures.meubleHaut(1), catalog, cfg);
        var q10 = new QuoteEngine().compute(TestFixtures.meubleHaut(10), catalog, cfg);
        int s1 = q1.panels().stream().mapToInt(QuoteResult.PanelUsage::sheets).sum();
        int s10 = q10.panels().stream().mapToInt(QuoteResult.PanelUsage::sheets).sum();
        assertTrue(s10 <= 10 * s1);
        assertTrue(s10 >= s1);
    }

    @Test
    void pricesAbsentsGenerentDesAvertissements() {
        var q = new QuoteEngine().compute(TestFixtures.meubleHaut(1), TestFixtures.catalog(0, 0, 0), TestFixtures.config(0, 0, 0, 0));
        String all = String.join("|", q.warnings());
        assertTrue(all.contains("Prix non renseigné"));
        assertTrue(all.contains("Taux horaire non renseigné"));
        assertTrue(all.contains("Temps de fabrication de référence non renseignés"));
        assertTrue(all.contains("Aucune taxe"));
        assertEquals(0, q.costs().panels().signum());
        assertEquals(0, q.costs().edge().signum());
        assertEquals(0, q.costs().hardware().signum());
        assertEquals(0, q.costs().labor().signum());
    }

    @Test
    void scenarioAvecTiroirsEtControleDeFleche() {
        var catalog = TestFixtures.catalog(100, 2, 1);
        var cfg = TestFixtures.config(10, 1, 15, 20);
        FurnitureSpec s = new FurnitureSpec("MEUBLE_BAS", 900, 720, 560, 1, "MFC18", "FOND3", "MFC18", "CH1",
                true, 0, 1, 1, 2, 180, true, 100, "HINGE", "SLIDE", "PIN", "HANDLE", "CONN", 400.0, null);
        QuoteResult q = new QuoteEngine().compute(s, catalog, cfg);
        assertTrue(q.parts().stream().anyMatch(p -> p.role().equals("DRAWER_BOTTOM")));
        assertEquals(2, q.hardware().stream().filter(l -> l.category().equals("SLIDE")).mapToInt(l -> l.qty()).sum());
        assertFalse(q.shelfChecks().isEmpty());
        // 400 kg/m2 sur 18 mm, portée ~860 mm : la flèche dépasse L/400 -> avertissement
        assertTrue(q.warnings().stream().anyMatch(w -> w.contains("flèche estimée")));
    }
}
