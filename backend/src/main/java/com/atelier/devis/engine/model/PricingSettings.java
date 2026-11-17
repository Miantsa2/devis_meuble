package com.atelier.devis.engine.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Paramètres commerciaux, tous issus de la base (administrables).
 * panelCostingMode : 0 = feuilles entières consommées ; 1 = surface nette x (1 + panelWastePercent).
 * markupPercent : marge appliquée sur le coût de production = (prix - coût) / coût.
 */
public record PricingSettings(
        BigDecimal hourlyRate, BigDecimal finishPricePerM2,
        BigDecimal transportFee, BigDecimal installationPerUnit,
        BigDecimal markupPercent, int panelCostingMode, BigDecimal panelWastePercent,
        String currency, List<Tax> taxes) {
}
