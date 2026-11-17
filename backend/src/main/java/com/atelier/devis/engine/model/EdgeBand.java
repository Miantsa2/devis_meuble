package com.atelier.devis.engine.model;

import java.math.BigDecimal;

public record EdgeBand(String code, String name, double thicknessMm, BigDecimal pricePerMeter) {
}
