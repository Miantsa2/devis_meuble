package com.atelier.devis.engine.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Règles de fabrication résolues pour un type de meuble (valeurs numériques, clés "famille.nom"). */
public final class Rules {
    private final Map<String, BigDecimal> values;
    private final Set<String> toValidate;

    public Rules(Map<String, BigDecimal> values, Set<String> toValidate) {
        this.values = Map.copyOf(values);
        this.toValidate = Set.copyOf(toValidate);
    }

    public double d(String key) {
        BigDecimal v = values.get(key);
        if (v == null) throw new IllegalStateException("Règle de fabrication manquante : " + key);
        return v.doubleValue();
    }

    public int i(String key) { return (int) Math.round(d(key)); }

    public boolean b(String key) { return d(key) != 0.0; }

    public int iOr(String key, int fallback) {
        return values.containsKey(key) ? i(key) : fallback;
    }

    public Set<String> toValidateKeys() { return toValidate; }
}
