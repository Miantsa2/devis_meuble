package com.atelier.devis.engine.service;

import com.atelier.devis.engine.model.Rules;

/**
 * Perçages en "système 32" : pas de 32 mm, première rangée à 37 mm du chant avant (convention documentée,
 * cf. docs/FORMULES.md). Sert uniquement à estimer le nombre de trous (temps de perçage).
 * La marge début/fin de rangée reste une règle de l'atelier (s32.margin_mm).
 */
public class System32Service {

    /** Nombre de trous par rangée sur une hauteur intérieure donnée. */
    public int holesPerRow(double innerHeightMm, Rules r) {
        double usable = innerHeightMm - 2 * r.d("s32.margin_mm");
        if (usable < 0) return 0;
        return (int) Math.floor(usable / r.d("s32.pitch_mm")) + 1;
    }

    /** Rangées par face : avant + arrière si la profondeur le permet. */
    public int rows(double depthMm, Rules r) {
        double between = depthMm - r.d("s32.front_row_setback_mm") - r.d("s32.rear_row_setback_mm");
        return between >= r.d("s32.pitch_mm") ? 2 : 1;
    }

    /** Trous de taquets : 2 faces par compartiment x rangées x trous par rangée. */
    public int shelfPinHoles(int compartments, double innerHeightMm, double depthMm, Rules r) {
        return compartments * 2 * rows(depthMm, r) * holesPerRow(innerHeightMm, r);
    }
}
