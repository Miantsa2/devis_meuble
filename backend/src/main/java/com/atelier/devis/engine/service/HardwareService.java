package com.atelier.devis.engine.service;

import com.atelier.devis.engine.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Quantités de quincaillerie. Chaque quantité dérive d'une règle configurable ou d'une table de seuils. */
public class HardwareService {

    public record Line(String code, String name, String category, int qty, BigDecimal unitPrice, BigDecimal total) { }
    public record Result(List<Line> lines, int hinges, int connectors, int shelfPins, List<String> warnings) { }

    public Result compute(FurnitureSpec s, CarcassGeometryService.Result geo, PanelMaterial front, Catalog cat,
                          EngineConfig cfg, Rules r) {
        int units = s.quantity();
        List<Line> lines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int hingesPerUnit = 0;

        if (s.doors() > 0) {
            Hardware hinge = need(cat, s.hingeCode(), "charnière");
            double thk = (front != null ? front.thicknessMm() : 0);
            Double density = front != null ? front.densityKgM3() : null;
            for (double[] door : geo.doorSizes()) {
                double h = door[0], wd = door[1];
                int byHeight = lookup(cfg.hingesByHeight(), h, warnings, "hauteur de porte (mm)");
                int byWeight = 0;
                if (density != null && thk > 0) {
                    double kg = (h / 1000.0) * (wd / 1000.0) * (thk / 1000.0) * density;
                    byWeight = lookup(cfg.hingesByWeight(), kg, warnings, "poids de porte (kg)");
                } else warnings.add("Densité du matériau de façade non renseignée : nombre de charnières basé sur la hauteur seule.");
                if (wd > r.d("hinge.max_door_width_mm"))
                    warnings.add("Porte de " + Math.round(wd) + " mm de large : au-delà du domaine de validité de la table de charnières ; valider avec le fabricant.");
                hingesPerUnit += Math.max(r.i("hinge.min_per_door"), Math.max(byHeight, byWeight));
            }
            add(lines, hinge, hingesPerUnit * units);
        }
        if (s.drawers() > 0) {
            Hardware slide = need(cat, s.slideCode(), "coulisse");
            add(lines, slide, s.drawers() * r.i("drawer.slide_sets_per_drawer") * units);
        }
        int pins = 0;
        if (s.shelvesPerCompartment() > 0) {
            int shelves = (s.dividers() + 1) * s.shelvesPerCompartment();
            pins = shelves * r.i("shelf.pins_per_shelf") * units;
            add(lines, need(cat, s.shelfPinCode(), "taquet d'étagère"), pins);
        }
        int fronts = s.doors() + s.drawers();
        if (fronts > 0 && s.handleCode() != null && !s.handleCode().isBlank())
            add(lines, need(cat, s.handleCode(), "poignée"), fronts * r.i("handle.per_front") * units);

        // Assemblage : joints dessus/dessous <-> côtés (4), séparations <-> dessus/dessous (2 par séparation)
        double depth = s.depthMm();
        int jointsPerUnit = 0; int connectors = 0;
        double spacing = r.d("connector.spacing_mm");
        int minPer = r.i("connector.min_per_joint");
        int perJoint = Math.max(minPer, (int) Math.ceil(depth / spacing));
        jointsPerUnit = 4 + 2 * s.dividers();
        connectors = jointsPerUnit * perJoint * units;
        if (s.connectorCode() != null && !s.connectorCode().isBlank())
            add(lines, need(cat, s.connectorCode(), "connecteur d'assemblage"), connectors);
        else warnings.add("Aucun connecteur d'assemblage sélectionné : " + connectors + " fixations estimées non chiffrées.");

        for (FurnitureSpec.ExtraItem e : s.extrasOrEmpty())
            if (e.qty() > 0) add(lines, need(cat, e.code(), "accessoire " + e.code()), e.qty() * units);

        return new Result(lines, hingesPerUnit * units, connectors, pins, warnings);
    }

    private int lookup(List<ThresholdRow> rows, double value, List<String> warnings, String what) {
        if (rows.isEmpty()) return 0;
        List<ThresholdRow> sorted = rows.stream().sorted(java.util.Comparator.comparingDouble(ThresholdRow::upTo)).toList();
        for (ThresholdRow row : sorted) if (value <= row.upTo()) return row.count();
        warnings.add("Valeur hors de la table de seuils (" + what + " = " + Math.round(value) + ") : dernière ligne appliquée, à valider.");
        return sorted.get(sorted.size() - 1).count();
    }

    private Hardware need(Catalog cat, String code, String what) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Référence requise pour : " + what);
        Hardware h = cat.hardware(code);
        if (h == null) throw new IllegalArgumentException("Référence inconnue (" + what + ") : " + code);
        return h;
    }

    private void add(List<Line> lines, Hardware h, int qty) {
        BigDecimal total = h.unitPrice().multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        lines.add(new Line(h.code(), h.name(), h.category(), qty, h.unitPrice(), total));
    }
}
