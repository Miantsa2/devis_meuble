package com.atelier.devis.engine.model;

/**
 * Pièce finie. a = dimension le long du fil/décor, b = autre dimension (mm).
 * aEdges = nombre de chants collés sur les bords de longueur a ; bEdges idem pour les bords de longueur b.
 */
public record Part(String role, String label, int qty, double a, double b,
                   String materialCode, boolean grainFixed, int aEdges, int bEdges) {
    public Part withQty(int newQty) {
        return new Part(role, label, newQty, a, b, materialCode, grainFixed, aEdges, bEdges);
    }
}
