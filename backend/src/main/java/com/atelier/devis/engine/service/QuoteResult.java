package com.atelier.devis.engine.service;

import java.math.BigDecimal;
import java.util.List;

/** Résultat complet d'un calcul (sérialisable en JSON, figé tel quel dans le devis enregistré). */
public record QuoteResult(
        String currency, int units,
        List<PartLine> parts, List<PanelUsage> panels, EdgeLine edge,
        List<HardwareService.Line> hardware, LaborService.Result labor,
        double finishAreaM2, List<ShelfCheck> shelfChecks,
        Costs costs, List<String> warnings, int rulesToValidate) {

    public record PartLine(String role, String label, int qty, double a, double b, double coreA, double coreB,
                           double thicknessMm, String materialCode, int aEdges, int bEdges) { }

    public record PanelUsage(String materialCode, String name, double thicknessMm, int sheets, double netAreaM2,
                             double sheetsAreaM2, double wastePercent, int cuts, BigDecimal unitPrice, BigDecimal cost,
                             double sheetLengthMm, double sheetWidthMm, List<CutPlanService.Sheet> layout) { }

    public record EdgeLine(String code, String name, double netMeters, double appliedMeters, double purchasedMeters,
                           int strips, BigDecimal unitPrice, BigDecimal cost) { }

    public record ShelfCheck(double spanMm, double deflectionMm, double limitMm, double maxSpanMm, boolean ok) { }

    public record TaxLine(String code, String label, BigDecimal ratePercent, BigDecimal amount) { }

    public record Costs(BigDecimal panels, BigDecimal edge, BigDecimal hardware, BigDecimal labor, BigDecimal finish,
                        BigDecimal productionCost, BigDecimal markupPercent, BigDecimal markup,
                        BigDecimal transport, BigDecimal installation, BigDecimal totalHt,
                        List<TaxLine> taxes, BigDecimal totalTtc) { }
}
