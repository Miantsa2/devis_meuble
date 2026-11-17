package com.atelier.devis.engine.model;

/** Base de quantité à laquelle s'applique un temps de référence (en minutes). */
public enum LaborBasis {
    PER_SHEET, PER_CUT, PER_PART, PER_EDGE_METER, PER_EDGE_STRIP, PER_HOLE,
    PER_UNIT, PER_DOOR, PER_DRAWER, PER_HINGE, PER_M2_FINISH
}
