package com.atelier.devis.engine.service;

/**
 * Flèche d'une étagère posée sur appuis simples, charge uniforme : d = 5 w L^4 / (384 E I)  (résistance des
 * matériaux, formule classique). I = b t^3 / 12. Aucune valeur de E n'est codée ici : elle vient du panneau.
 * Limite : initial seulement (le fluage ajoute de la flèche dans le temps), matériau supposé homogène.
 */
public class StructuralCheckService {

    private static final double G = 9.80665;

    /** @param loadKgPerM2 charge répartie ; @return flèche en mm. */
    public double deflectionMm(double loadKgPerM2, double depthMm, double thicknessMm, double spanMm, double eMpa) {
        double pNperMm2 = loadKgPerM2 * G / 1e6;
        double w = pNperMm2 * depthMm;                          // N/mm de portée
        double inertia = depthMm * Math.pow(thicknessMm, 3) / 12.0;
        return 5 * w * Math.pow(spanMm, 4) / (384 * eMpa * inertia);
    }

    /** Portée maximale pour que la flèche reste <= portée / ratio. */
    public double maxSpanMm(double loadKgPerM2, double depthMm, double thicknessMm, double eMpa, double ratio) {
        double pNperMm2 = loadKgPerM2 * G / 1e6;
        double w = pNperMm2 * depthMm;
        double inertia = depthMm * Math.pow(thicknessMm, 3) / 12.0;
        // 5 w L^4 / (384 E I) = L / ratio  ->  L^3 = 384 E I / (5 w ratio)
        return Math.cbrt(384 * eMpa * inertia / (5 * w * ratio));
    }
}
