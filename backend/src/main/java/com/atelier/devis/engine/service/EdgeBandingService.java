package com.atelier.devis.engine.service;

import com.atelier.devis.engine.model.*;

import java.util.List;

/**
 * Chants. Longueur nette = somme des bords plaqués. Les dimensions de débit déduisent l'épaisseur du chant
 * (le chant collé sur un bord de longueur a réduit la dimension b du panneau brut, et inversement).
 * Surlongueur par bande et pourcentage de perte : règles configurables.
 */
public class EdgeBandingService {

    public record Result(double netMm, double withOffsetMm, double totalMm, int strips) {
        public double totalMeters() { return totalMm / 1000.0; }
        public double appliedMeters() { return withOffsetMm / 1000.0; }
    }

    /** Dimensions de débit {a, b} d'une pièce finie. */
    public double[] coreDims(Part p, double edgeThicknessMm) {
        double coreA = p.a() - edgeThicknessMm * p.bEdges();
        double coreB = p.b() - edgeThicknessMm * p.aEdges();
        return new double[]{coreA, coreB};
    }

    /** parts : pièces avec quantités déjà multipliées par le nombre de meubles. */
    public Result compute(List<Part> parts, Rules r) {
        double net = 0; int strips = 0;
        for (Part p : parts) {
            net += p.qty() * (p.aEdges() * p.a() + p.bEdges() * p.b());
            strips += p.qty() * (p.aEdges() + p.bEdges());
        }
        double withOffset = net + strips * 2 * r.d("edge.trim_offset_mm");
        double total = withOffset * (1 + r.d("edge.waste_percent") / 100.0);
        return new Result(net, withOffset, total, strips);
    }
}
