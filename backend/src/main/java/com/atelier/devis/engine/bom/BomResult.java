package com.atelier.devis.engine.bom;

import java.math.BigDecimal;
import java.util.List;

public record BomResult(
        String name, int quantity, String currency,
        List<ComponentResult> components,
        BigDecimal materialsPerUnit, BigDecimal materialsTotal,
        BigDecimal laborHours, BigDecimal hourlyRate, BigDecimal laborCost,
        BigDecimal productionCost, BigDecimal markupPercent, BigDecimal markup,
        BigDecimal transport, BigDecimal installation, BigDecimal totalHt,
        List<TaxLine> taxes, BigDecimal totalTtc, List<String> warnings) {

    public record ComponentResult(String name, Double widthMm, Double depthMm, Double heightMm, int qty,
                                  List<LineResult> lines, BigDecimal unitTotal, BigDecimal total) { }

    public record LineResult(String category, String code, String name, String unit, BigDecimal quantity,
                             BigDecimal unitPrice, BigDecimal coef, BigDecimal amount) { }

    public record TaxLine(String code, String label, BigDecimal ratePercent, BigDecimal amount) { }
}
