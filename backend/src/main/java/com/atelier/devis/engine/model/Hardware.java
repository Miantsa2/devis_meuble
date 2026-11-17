package com.atelier.devis.engine.model;

import java.math.BigDecimal;

/** Quincaillerie / accessoire. slideLengthMm n'est renseigné que pour les coulisses. */
public record Hardware(String code, String name, String category, BigDecimal unitPrice, Double slideLengthMm) {
}
