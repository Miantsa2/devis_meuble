package com.atelier.devis.config;

import com.atelier.devis.domain.Entities.*;
import com.atelier.devis.domain.Repos.*;
import com.atelier.devis.engine.model.*;
import com.atelier.devis.engine.service.QuoteEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/** Branche le moteur (pur Java) sur la base : catalogue, règles, temps, seuils et paramètres commerciaux. */
@Configuration
public class DbEngineSupport {

    @Bean
    public QuoteEngine quoteEngine() { return new QuoteEngine(); }

    @Component
    public static class DbCatalog implements Catalog {
        private final PanelRepo panels; private final EdgeRepo edges; private final HardwareRepo hardware;

        public DbCatalog(PanelRepo panels, EdgeRepo edges, HardwareRepo hardware) {
            this.panels = panels; this.edges = edges; this.hardware = hardware;
        }

        public PanelMaterial panel(String code) {
            return panels.findByCode(code).filter(PanelMaterialEntity::isActive).map(p -> new PanelMaterial(
                    p.getCode(), p.getName(), p.getThicknessMm(), p.getLengthMm(), p.getWidthMm(),
                    nz(p.getPricePerSheet()), p.isGrain(), p.getDensityKgM3(), p.getYoungModulusMpa())).orElse(null);
        }

        public EdgeBand edgeBand(String code) {
            return edges.findByCode(code).filter(EdgeBandEntity::isActive).map(e -> new EdgeBand(
                    e.getCode(), e.getName(), e.getThicknessMm(), nz(e.getPricePerMeter()))).orElse(null);
        }

        public Hardware hardware(String code) {
            return hardware.findByCode(code).filter(HardwareEntity::isActive).map(h -> new Hardware(
                    h.getCode(), h.getName(), h.getCategory(), nz(h.getUnitPrice()), h.getSlideLengthMm())).orElse(null);
        }
    }

    @Component
    public static class DbEngineConfig implements EngineConfig {
        private final RuleRepo rules; private final LaborRepo labor; private final ThresholdRepo thresholds;
        private final SettingRepo settings; private final TaxRepo taxes;

        public DbEngineConfig(RuleRepo rules, LaborRepo labor, ThresholdRepo thresholds, SettingRepo settings, TaxRepo taxes) {
            this.rules = rules; this.labor = labor; this.thresholds = thresholds; this.settings = settings; this.taxes = taxes;
        }

        /** DEFAULT d'abord, puis surcharge par le type demandé. */
        public Rules rules(String type) {
            Map<String, BigDecimal> values = new HashMap<>();
            Set<String> toValidate = new HashSet<>();
            for (String t : chain(type)) {
                for (RuleEntity r : rules.findByFurnitureTypeIn(List.of(t))) {
                    values.put(r.getRuleKey(), r.getValue());
                    if ("TO_VALIDATE".equals(r.getValidationStatus())) toValidate.add(r.getRuleKey());
                    else toValidate.remove(r.getRuleKey());
                }
            }
            return new Rules(values, toValidate);
        }

        public List<LaborOp> laborOps(String type) {
            Map<String, LaborOp> byCode = new LinkedHashMap<>();
            for (String t : chain(type))
                for (LaborOperationEntity l : labor.findByFurnitureTypeIn(List.of(t)))
                    byCode.put(l.getCode(), new LaborOp(l.getCode(), l.getLabel(), LaborBasis.valueOf(l.getBasis()), nz(l.getMinutes())));
            return new ArrayList<>(byCode.values());
        }

        public List<ThresholdRow> hingesByHeight() { return rows("HINGE_HEIGHT"); }
        public List<ThresholdRow> hingesByWeight() { return rows("HINGE_WEIGHT"); }

        public PricingSettings pricing() {
            Map<String, SettingEntity> m = new HashMap<>();
            settings.findAll().forEach(s -> m.put(s.getId(), s));
            List<Tax> t = taxes.findByActiveTrue().stream().map(x -> new Tax(x.getCode(), x.getLabel(), nz(x.getRatePercent()))).toList();
            return new PricingSettings(num(m, "labor.hourly_rate"), num(m, "finish.price_per_m2"), num(m, "transport.fee"),
                    num(m, "installation.fee_per_unit"), num(m, "margin.markup_percent"),
                    num(m, "panel.costing_mode").intValue(), num(m, "panel.waste_percent"),
                    m.containsKey("currency.code") && m.get("currency.code").getValueText() != null ? m.get("currency.code").getValueText() : "", t);
        }

        private List<String> chain(String type) {
            return (type == null || type.equals("DEFAULT")) ? List.of("DEFAULT") : List.of("DEFAULT", type);
        }

        private List<ThresholdRow> rows(String kind) {
            return thresholds.findByKind(kind).stream().map(t -> new ThresholdRow(t.getUpTo(), t.getCount())).toList();
        }

        private BigDecimal num(Map<String, SettingEntity> m, String key) {
            SettingEntity s = m.get(key);
            return s == null || s.getValueNumeric() == null ? BigDecimal.ZERO : s.getValueNumeric();
        }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
