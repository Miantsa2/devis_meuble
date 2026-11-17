package com.atelier.devis.engine.model;

import java.math.BigDecimal;

public record Tax(String code, String label, BigDecimal ratePercent) {
}
