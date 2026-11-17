package com.atelier.devis.engine.model;

import java.util.List;

/** Configuration métier lue en base : règles, temps de référence, seuils, paramètres commerciaux. */
public interface EngineConfig {
    Rules rules(String furnitureType);
    List<LaborOp> laborOps(String furnitureType);
    List<ThresholdRow> hingesByHeight();
    List<ThresholdRow> hingesByWeight();
    PricingSettings pricing();
}
