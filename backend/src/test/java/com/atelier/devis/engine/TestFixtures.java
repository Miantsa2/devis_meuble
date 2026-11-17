package com.atelier.devis.engine;

import com.atelier.devis.engine.model.*;

import java.math.BigDecimal;
import java.util.*;

/** Jeux de données DE TEST uniquement (les prix ci-dessous n'ont aucune valeur commerciale). */
final class TestFixtures {
    private TestFixtures() { }

    static Rules rules() {
        Map<String, BigDecimal> m = new HashMap<>();
        put(m, "construction.top_bottom_between_sides", 1);
        put(m, "back.grooved", 0); put(m, "back.groove_depth_mm", 8); put(m, "back.groove_setback_mm", 10);
        put(m, "divider.front_setback_mm", 20); put(m, "shelf.side_clearance_mm", 2); put(m, "shelf.front_setback_mm", 20);
        put(m, "shelf.pins_per_shelf", 4); put(m, "shelf.max_deflection_ratio", 400);
        put(m, "door.outer_reveal_mm", 2); put(m, "door.gap_mm", 3); put(m, "door.top_reveal_mm", 2); put(m, "door.bottom_reveal_mm", 2);
        put(m, "hinge.min_per_door", 2); put(m, "hinge.max_door_width_mm", 600);
        put(m, "drawer.slide_sets_per_drawer", 1); put(m, "drawer.side_clearance_total_mm", 26);
        put(m, "drawer.box_height_reduction_mm", 30); put(m, "drawer.bottom_groove_depth_mm", 8); put(m, "drawer.rear_clearance_mm", 50);
        put(m, "handle.per_front", 1);
        put(m, "connector.min_per_joint", 2); put(m, "connector.spacing_mm", 300); put(m, "connector.holes_each", 2);
        put(m, "s32.margin_mm", 64); put(m, "s32.pitch_mm", 32); put(m, "s32.front_row_setback_mm", 37); put(m, "s32.rear_row_setback_mm", 37);
        put(m, "sheet.trim_mm", 10); put(m, "sheet.kerf_mm", 3.2);
        put(m, "edge.trim_offset_mm", 10); put(m, "edge.waste_percent", 10);
        put(m, "finish.faces_factor", 2);
        for (String role : new String[]{"side", "top", "bottom", "divider", "shelf"}) { put(m, "edge." + role + ".a_edges", 1); put(m, "edge." + role + ".b_edges", 0); }
        for (String role : new String[]{"door", "drawer_front"}) { put(m, "edge." + role + ".a_edges", 2); put(m, "edge." + role + ".b_edges", 2); }
        return new Rules(m, Set.of("shelf.side_clearance_mm"));
    }

    private static void put(Map<String, BigDecimal> m, String k, double v) { m.put(k, BigDecimal.valueOf(v)); }

    static PanelMaterial carcass(double price) {
        return new PanelMaterial("MFC18", "MFC 18 mm", 18, 2440, 1220, BigDecimal.valueOf(price), true, 650.0, 2500.0);
    }

    static PanelMaterial back(double price) {
        return new PanelMaterial("FOND3", "Fond 3 mm", 3, 2440, 1220, BigDecimal.valueOf(price), false, null, null);
    }

    static Catalog catalog(double panelPrice, double hwPrice, double edgePrice) {
        Map<String, PanelMaterial> panels = Map.of("MFC18", carcass(panelPrice), "FOND3", back(panelPrice / 4));
        EdgeBand eb = new EdgeBand("CH1", "Chant 1 mm", 1.0, BigDecimal.valueOf(edgePrice));
        Map<String, Hardware> hw = new HashMap<>();
        BigDecimal p = BigDecimal.valueOf(hwPrice);
        hw.put("HINGE", new Hardware("HINGE", "Charnière", "HINGE", p, null));
        hw.put("SLIDE", new Hardware("SLIDE", "Coulisse 450", "SLIDE", p, 450.0));
        hw.put("PIN", new Hardware("PIN", "Taquet", "SHELF_PIN", p, null));
        hw.put("HANDLE", new Hardware("HANDLE", "Poignée", "HANDLE", p, null));
        hw.put("CONN", new Hardware("CONN", "Connecteur", "CONNECTOR", p, null));
        return new Catalog() {
            public PanelMaterial panel(String c) { return panels.get(c); }
            public EdgeBand edgeBand(String c) { return "CH1".equals(c) ? eb : null; }
            public Hardware hardware(String c) { return hw.get(c); }
        };
    }

    static EngineConfig config(double hourly, double minutesEach, double markup, double vat) {
        PricingSettings pr = new PricingSettings(BigDecimal.valueOf(hourly), BigDecimal.valueOf(1), BigDecimal.valueOf(5),
                BigDecimal.valueOf(2), BigDecimal.valueOf(markup), 0, BigDecimal.ZERO, "TST",
                vat > 0 ? List.of(new Tax("VAT", "TVA test", BigDecimal.valueOf(vat))) : List.of());
        List<LaborOp> ops = new ArrayList<>();
        for (LaborBasis b : LaborBasis.values()) ops.add(new LaborOp(b.name(), b.name(), b, BigDecimal.valueOf(minutesEach)));
        Rules r = rules();
        return new EngineConfig() {
            public Rules rules(String t) { return r; }
            public List<LaborOp> laborOps(String t) { return ops; }
            public List<ThresholdRow> hingesByHeight() { return List.of(new ThresholdRow(1514, 2), new ThresholdRow(2286, 3)); }
            public List<ThresholdRow> hingesByWeight() { return List.of(new ThresholdRow(6, 2), new ThresholdRow(12, 3)); }
            public PricingSettings pricing() { return pr; }
        };
    }

    /** Meuble haut : 800 x 720 x 330, 2 portes, 1 étagère, fond appliqué. */
    static FurnitureSpec meubleHaut(int qty) {
        return new FurnitureSpec("MEUBLE_HAUT", 800, 720, 330, qty, "MFC18", "FOND3", "MFC18", "CH1",
                true, 0, 1, 2, 0, 0, false, 0, "HINGE", null, "PIN", "HANDLE", "CONN", null, null);
    }
}
