package com.atelier.devis.engine.bom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Développe un gabarit de composant à partir des mesures du composant (LARG, PROF, HAUT, QTE) :
 *  - pièces : quantité, dimensions 1 et 2 par formule ; surface = dim1 x dim2 x quantité / 10^6 (m²) ;
 *    chant = quantité x [bords sur dim1 x (dim1 + surlongueur) + bords sur dim2 x (dim2 + surlongueur)] / 1000 (m) ;
 *  - total par emplacement (slot) de panneau, majoré de la chute de panneau ; idem pour le chant ;
 *  - lignes : un article de panneau/chant reçoit le total de son slot ; les autres reçoivent leur formule
 *    (variables L, P, H, Q et CH = total de chant en mètres) ou leur quantité par défaut.
 * Reprend la logique du second tableau du fichier MEUBLE_HAUT.xlsx. Aucune valeur n'est codée ici.
 */
public class ComponentExpander {

    public record PieceDef(String name, String panelSlot, String qtyFormula, String dim1Formula, String dim2Formula,
                           String chantSlot, int edgesDim1, int edgesDim2) { }

    public record LineDef(String articleCode, String slot, String quantityFormula, BigDecimal defaultQuantity) { }

    public record TemplateDef(String name, double panelWastePercent, double chantWastePercent,
                              List<PieceDef> pieces, List<LineDef> lines) { }

    public record PieceResult(String name, String panelSlot, String chantSlot, double qty, double dim1, double dim2,
                              double surfaceM2, double chantM) { }

    /** kind : SURFACE | CHANT (calculés, non modifiables) ; FORMULA | FIXED (quantité par défaut, modifiable). */
    public record LineResult(int index, String articleCode, String slot, String kind, BigDecimal quantity) { }

    public record Expanded(List<PieceResult> pieces, List<LineResult> lines, double chantMeters) { }

    public Expanded expand(TemplateDef t, double l, double p, double h, int q, double chantAllowanceMm) {
        Map<String, Double> vars = new LinkedHashMap<>();
        vars.put("L", l); vars.put("P", p); vars.put("H", h); vars.put("Q", (double) q);

        List<PieceResult> pieces = new ArrayList<>();
        Map<String, Double> panelByslot = new LinkedHashMap<>();
        Map<String, Double> chantBySlot = new LinkedHashMap<>();
        for (PieceDef d : t.pieces()) {
            double qty = FormulaEvaluator.eval(d.qtyFormula(), vars);
            double m = FormulaEvaluator.eval(d.dim1Formula(), vars);
            double n = FormulaEvaluator.eval(d.dim2Formula(), vars);
            double surface = m * n * qty / 1_000_000.0;
            double chant = qty * (d.edgesDim1() * (m + chantAllowanceMm) + d.edgesDim2() * (n + chantAllowanceMm)) / 1000.0;
            boolean hasChant = d.chantSlot() != null && !d.chantSlot().isBlank() && (d.edgesDim1() > 0 || d.edgesDim2() > 0);
            if (!hasChant) chant = 0;
            pieces.add(new PieceResult(d.name(), d.panelSlot(), d.chantSlot(), qty, m, n, surface, chant));
            if (d.panelSlot() != null && !d.panelSlot().isBlank()) panelByslot.merge(d.panelSlot(), surface, Double::sum);
            if (hasChant) chantBySlot.merge(d.chantSlot(), chant, Double::sum);
        }
        panelByslot.replaceAll((k, v) -> v * (1 + t.panelWastePercent() / 100.0));
        chantBySlot.replaceAll((k, v) -> v * (1 + t.chantWastePercent() / 100.0));
        double ch = chantBySlot.values().stream().mapToDouble(Double::doubleValue).sum();
        vars.put("CH", ch);

        List<LineResult> lines = new ArrayList<>();
        int i = 0;
        for (LineDef d : t.lines()) {
            String kind; double value;
            if (d.slot() != null && panelByslot.containsKey(d.slot())) { kind = "SURFACE"; value = panelByslot.get(d.slot()); }
            else if (d.slot() != null && chantBySlot.containsKey(d.slot())) { kind = "CHANT"; value = chantBySlot.get(d.slot()); }
            else if (d.quantityFormula() != null && !d.quantityFormula().isBlank()) {
                kind = "FORMULA"; value = FormulaEvaluator.eval(d.quantityFormula(), vars);
            } else {
                kind = "FIXED"; value = d.defaultQuantity() == null ? 0 : d.defaultQuantity().doubleValue();
            }
            lines.add(new LineResult(i++, d.articleCode(), d.slot(), kind, round(value)));
        }
        return new Expanded(pieces, lines, ch);
    }

    private static BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }
}
