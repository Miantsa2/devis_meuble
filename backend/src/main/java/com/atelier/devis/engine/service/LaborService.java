package com.atelier.devis.engine.service;

import com.atelier.devis.engine.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main-d'œuvre : heures = somme (quantité de la base x minutes de référence) / 60 ; coût = heures x taux horaire.
 * AUCUN temps n'est codé en dur : ils sont saisis par l'administrateur puis calibrés sur des cas réels.
 */
public class LaborService {

    public record Line(String code, String label, LaborBasis basis, double quantity,
                       BigDecimal minutesEach, BigDecimal totalMinutes, BigDecimal cost) { }
    public record Result(List<Line> lines, BigDecimal hours, BigDecimal cost) { }

    public Result compute(List<LaborOp> ops, Map<LaborBasis, Double> quantities, BigDecimal hourlyRate) {
        List<Line> lines = new ArrayList<>();
        BigDecimal minutes = BigDecimal.ZERO;
        for (LaborOp op : ops) {
            double q = quantities.getOrDefault(op.basis(), 0.0);
            BigDecimal tot = op.minutes().multiply(BigDecimal.valueOf(q));
            BigDecimal cost = tot.divide(BigDecimal.valueOf(60), 10, RoundingMode.HALF_UP).multiply(hourlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            lines.add(new Line(op.code(), op.label(), op.basis(), q, op.minutes(), tot.setScale(2, RoundingMode.HALF_UP), cost));
            minutes = minutes.add(tot);
        }
        BigDecimal hours = minutes.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal cost = lines.stream().map(Line::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Result(lines, hours, cost);
    }
}
