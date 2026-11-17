package com.atelier.devis.engine.model;

import java.util.List;

/** Description paramétrique d'un meuble (dimensions extérieures en mm). */
public record FurnitureSpec(
        String typeCode,
        double widthMm, double heightMm, double depthMm, int quantity,
        String carcassMaterial, String backMaterial, String frontMaterial, String edgeBand,
        boolean hasBack, int dividers, int shelvesPerCompartment,
        int doors, int drawers, double drawerFrontHeightMm,
        boolean hasPlinth, double plinthHeightMm,
        String hingeCode, String slideCode, String shelfPinCode, String handleCode, String connectorCode,
        Double shelfLoadKgPerM2, List<ExtraItem> extras) {

    public record ExtraItem(String code, int qty) { }

    public List<ExtraItem> extrasOrEmpty() { return extras == null ? List.of() : extras; }
}
