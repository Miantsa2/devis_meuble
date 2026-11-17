package com.atelier.devis.engine.service;

import com.atelier.devis.engine.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Génère les pièces d'un caisson à partir de ses dimensions extérieures.
 * Géométrie : arithmétique pure (somme/différence d'épaisseurs, répartition d'un jeu). Les jeux et retraits
 * viennent de {@link Rules} (configurables par type de meuble) ; aucune valeur n'est codée en dur ici.
 */
public class CarcassGeometryService {

    public record Result(List<Part> parts, List<String> warnings,
                         List<double[]> doorSizes /* {hauteur, largeur} par porte */,
                         double compartmentWidthMm) { }

    public Result generate(FurnitureSpec s, PanelMaterial carcass, PanelMaterial back, PanelMaterial front,
                           Hardware slide, Rules r) {
        List<Part> parts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<double[]> doorSizes = new ArrayList<>();

        double w = s.widthMm(), h = s.heightMm(), d = s.depthMm();
        double t = carcass.thicknessMm();
        positive("largeur", w); positive("hauteur", h); positive("profondeur", d);
        if (s.dividers() < 0 || s.shelvesPerCompartment() < 0 || s.doors() < 0 || s.drawers() < 0)
            throw new IllegalArgumentException("Les quantités ne peuvent pas être négatives");

        double plinth = s.hasPlinth() ? s.plinthHeightMm() : 0;
        double hc = h - plinth;                       // hauteur du caisson
        positive("hauteur du caisson", hc);
        boolean grooved = r.b("back.grooved");
        double setback = grooved ? r.d("back.groove_setback_mm") : 0;
        double g = grooved ? r.d("back.groove_depth_mm") : 0;
        boolean between = r.b("construction.top_bottom_between_sides");
        PanelMaterial frontMat = front != null ? front : carcass;

        // Côtés : hc x d
        parts.add(part(r, "SIDE", "Côté", 2, hc, d, carcass, true));
        // Dessus / dessous
        double tbLen = between ? w - 2 * t : w;
        double tbDepth = d - setback;
        positive("longueur dessus/dessous", tbLen); positive("profondeur dessus/dessous", tbDepth);
        parts.add(part(r, "TOP", "Dessus", 1, tbLen, tbDepth, carcass, true));
        parts.add(part(r, "BOTTOM", "Dessous", 1, tbLen, tbDepth, carcass, true));

        int compartments = s.dividers() + 1;
        double inner = w - 2 * t;
        double compW = (inner - s.dividers() * t) / compartments;
        positive("largeur de compartiment", compW);

        if (s.dividers() > 0) {
            parts.add(part(r, "DIVIDER", "Séparation verticale", s.dividers(),
                    hc - 2 * t, d - setback - r.d("divider.front_setback_mm"), carcass, true));
        }
        if (s.shelvesPerCompartment() > 0) {
            double sw = compW - r.d("shelf.side_clearance_mm");
            double sd = d - setback - r.d("shelf.front_setback_mm");
            positive("largeur d'étagère", sw); positive("profondeur d'étagère", sd);
            parts.add(part(r, "SHELF", "Étagère", compartments * s.shelvesPerCompartment(), sw, sd, carcass, true));
        }
        if (s.hasBack()) {
            if (back == null) throw new IllegalArgumentException("Matériau de fond requis (hasBack = true)");
            double ba = grooved ? hc - 2 * t + 2 * g : hc;
            double bb = grooved ? w - 2 * t + 2 * g : w;
            parts.add(part(r, "BACK", "Fond", 1, ba, bb, back, true));
        }
        if (s.hasPlinth()) {
            parts.add(part(r, "PLINTH", "Plinthe", 1, w - 2 * t, plinth, carcass, true));
        }

        // Façades : portes au-dessus de la zone tiroirs
        double gap = r.d("door.gap_mm"), outer = r.d("door.outer_reveal_mm");
        double drawerZone = s.drawers() > 0 ? s.drawers() * (s.drawerFrontHeightMm() + gap) : 0;
        if (s.doors() > 0) {
            double doorH = hc - drawerZone - r.d("door.top_reveal_mm") - r.d("door.bottom_reveal_mm");
            double doorW = (w - 2 * outer - (s.doors() - 1) * gap) / s.doors();
            positive("hauteur de porte", doorH); positive("largeur de porte", doorW);
            parts.add(part(r, "DOOR", "Porte", s.doors(), doorH, doorW, frontMat, true));
            for (int i = 0; i < s.doors(); i++) doorSizes.add(new double[]{doorH, doorW});
        }
        if (s.drawers() > 0) {
            positive("hauteur de façade de tiroir", s.drawerFrontHeightMm());
            if (slide == null || slide.slideLengthMm() == null)
                throw new IllegalArgumentException("Coulisse avec longueur renseignée requise pour les tiroirs");
            if (drawerZone >= hc)
                throw new IllegalArgumentException("Les tiroirs occupent toute la hauteur du caisson");
            parts.add(part(r, "DRAWER_FRONT", "Façade de tiroir", s.drawers(),
                    w - 2 * outer, s.drawerFrontHeightMm(), frontMat, true));
            double boxW = w - 2 * t - r.d("drawer.side_clearance_total_mm");
            double boxD = slide.slideLengthMm();
            double boxH = s.drawerFrontHeightMm() - r.d("drawer.box_height_reduction_mm");
            double gd = r.d("drawer.bottom_groove_depth_mm");
            positive("largeur de caisse de tiroir", boxW); positive("hauteur de caisse de tiroir", boxH);
            if (boxD > d - setback - r.d("drawer.rear_clearance_mm"))
                warnings.add("Coulisse de " + (int) boxD + " mm trop longue pour la profondeur du meuble (" + (int) d + " mm).");
            PanelMaterial bottomMat = back != null ? back : carcass;
            parts.add(part(r, "DRAWER_SIDE", "Côté de tiroir", 2 * s.drawers(), boxD, boxH, carcass, true));
            parts.add(part(r, "DRAWER_FB", "Face/arrière de tiroir", 2 * s.drawers(), boxW - 2 * t, boxH, carcass, true));
            parts.add(part(r, "DRAWER_BOTTOM", "Fond de tiroir", s.drawers(),
                    boxW - 2 * t + 2 * gd, boxD - 2 * t + 2 * gd, bottomMat, true));
        }
        return new Result(parts, warnings, doorSizes, compW);
    }

    private Part part(Rules r, String role, String label, int qty, double a, double b, PanelMaterial m, boolean grain) {
        String k = "edge." + role.toLowerCase();
        return new Part(role, label, qty, a, b, m.code(), grain, r.iOr(k + ".a_edges", 0), r.iOr(k + ".b_edges", 0));
    }

    private static void positive(String what, double v) {
        if (!(v > 0)) throw new IllegalArgumentException("Dimension invalide (" + what + " = " + v + " mm) : vérifier les dimensions saisies");
    }
}
