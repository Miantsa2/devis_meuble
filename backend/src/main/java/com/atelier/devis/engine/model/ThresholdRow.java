package com.atelier.devis.engine.model;

/** Ligne de table de seuils : "jusqu'à upTo (mm ou kg) -> count". */
public record ThresholdRow(double upTo, int count) {
}
