package com.atelier.devis.engine.bom;

import java.math.BigDecimal;

/** Article du catalogue. coef = coefficient d'arrivée (frais d'approche) propre à l'article ; 1 = aucun. */
public record Article(String code, String name, String category, String unit, BigDecimal unitPrice, BigDecimal coef) {
}
