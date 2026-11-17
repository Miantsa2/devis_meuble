package com.atelier.devis.engine.model;

import java.math.BigDecimal;

public record LaborOp(String code, String label, LaborBasis basis, BigDecimal minutes) {
}
