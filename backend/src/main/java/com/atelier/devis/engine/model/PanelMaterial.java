package com.atelier.devis.engine.model;

import java.math.BigDecimal;

/** Panneau (une ligne = une référence avec ses dimensions et son épaisseur). Unités : mm, kg/m³, MPa (N/mm²). */
public record PanelMaterial(
        String code, String name,
        double thicknessMm, double lengthMm, double widthMm,
        BigDecimal pricePerSheet,
        boolean grain,            // true = décor/fil à respecter (pas de rotation libre des pièces à fil imposé)
        Double densityKgM3,       // optionnel : estimation du poids des portes
        Double eModulusMpa) {     // optionnel : contrôle de flèche des étagères
}
