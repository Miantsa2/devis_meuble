package com.atelier.devis.engine.bom;

import java.math.BigDecimal;
import java.util.List;

/** Saisie d'un devis : meuble -> composants -> lignes (article + surface/quantité saisie). */
public record BomInput(String name, int quantity, List<ComponentInput> components, BigDecimal laborHours) {

    public record ComponentInput(String name, Double widthMm, Double depthMm, Double heightMm, int qty, List<LineInput> lines) { }

    public record LineInput(String articleCode, BigDecimal quantity) { }
}
