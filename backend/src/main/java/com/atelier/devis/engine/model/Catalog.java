package com.atelier.devis.engine.model;

/** Accès au catalogue de prix/références (implémenté par la base en production, en mémoire dans les tests). */
public interface Catalog {
    PanelMaterial panel(String code);   // null si inconnu
    EdgeBand edgeBand(String code);
    Hardware hardware(String code);
}
