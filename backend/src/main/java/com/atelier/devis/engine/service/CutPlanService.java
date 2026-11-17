package com.atelier.devis.engine.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plan de débit : heuristique "par niveaux" (strip/level packing) à coupes guillotine, avec trait de scie et
 * parage. Famille d'heuristiques FFDH/BFDH décrite dans la littérature sur le 2D bin packing guillotine
 * (Lodi, Martello, Vigo ; Pietrobuoni, thèse Bologne). Ce n'est PAS un optimiseur exact : le résultat est une
 * borne supérieure du nombre de feuilles ; à comparer au logiciel d'optimisation de l'atelier (cf. docs/FORMULES.md).
 * Les chutes réutilisables ne sont pas récupérées.
 */
public class CutPlanService {

    public record Item(String label, double a, double b, boolean grainFixed) { }
    public record Placement(String label, double x, double y, double w, double h) { }
    public record Sheet(int index, List<Placement> placements, int cuts) { }
    public record Result(String materialCode, int sheetCount, double netAreaM2, double sheetsAreaM2,
                         double wastePercent, int cutCount, List<Sheet> sheets) { }

    private static final class Strip { int bin; double y, h, usedX; int items; }
    private static final class Bin { double usedY; List<Placement> placements = new ArrayList<>(); int strips; int items; }
    private record Norm(String label, double w, double h, boolean canRot) { }

    public Result plan(String materialCode, List<Item> items, double sheetL, double sheetW,
                       boolean sheetHasGrain, double trimMm, double kerfMm) {
        if (items.isEmpty()) return new Result(materialCode, 0, 0, 0, 0, 0, List.of());
        Result best = null;
        IllegalArgumentException failure = null;
        for (boolean transposed : new boolean[]{false, true}) {
            Result r;
            try {
                r = pack(materialCode, items, sheetL, sheetW, sheetHasGrain, trimMm, kerfMm, transposed);
            } catch (IllegalArgumentException e) { failure = e; continue; }
            if (best == null || r.sheetCount() < best.sheetCount()
                    || (r.sheetCount() == best.sheetCount() && r.cutCount() < best.cutCount())) best = r;
        }
        if (best == null) throw failure;
        return best;
    }

    private Result pack(String code, List<Item> items, double L, double W, boolean grain,
                        double trim, double kerf, boolean transposed) {
        double X = transposed ? W : L, Y = transposed ? L : W;
        double uX = X - 2 * trim, uY = Y - 2 * trim;
        List<Norm> list = new ArrayList<>();
        double net = 0;
        for (Item it : items) {
            double w, h; boolean rot;
            if (grain && it.grainFixed()) { w = transposed ? it.b() : it.a(); h = transposed ? it.a() : it.b(); rot = false; }
            else { w = Math.max(it.a(), it.b()); h = Math.min(it.a(), it.b()); rot = true; }
            boolean fits = w <= uX && h <= uY;
            if (!fits && rot && h <= uX && w <= uY) { double tmp = w; w = h; h = tmp; fits = true; }
            if (!fits) {
                // l'autre variante (transposée) peut convenir : on signale via exception interceptée par l'appelant
                throw new IllegalArgumentException("Pièce « " + it.label() + " » (" + Math.round(it.a()) + " x "
                        + Math.round(it.b()) + " mm) plus grande que la feuille utile (" + Math.round(uX) + " x " + Math.round(uY) + " mm)");
            }
            list.add(new Norm(it.label(), w, h, rot));
            net += it.a() * it.b();
        }
        list.sort(Comparator.comparingDouble(Norm::h).reversed().thenComparing(Comparator.comparingDouble(Norm::w).reversed()));

        List<Bin> bins = new ArrayList<>();
        List<Strip> strips = new ArrayList<>();
        for (Norm n : list) {
            Strip best = null; double bestLeft = Double.MAX_VALUE; boolean bestRot = false;
            for (Strip s : strips) {
                if (n.h() <= s.h && s.usedX + n.w() <= uX) {
                    double left = uX - (s.usedX + n.w());
                    if (left < bestLeft) { bestLeft = left; best = s; bestRot = false; }
                }
                if (n.canRot() && n.w() <= s.h && s.usedX + n.h() <= uX) {
                    double left = uX - (s.usedX + n.h());
                    if (left < bestLeft) { bestLeft = left; best = s; bestRot = true; }
                }
            }
            if (best != null) {
                place(bins.get(best.bin), best, n, bestRot, trim, kerf);
                continue;
            }
            Bin target = null; int idx = -1;
            for (int i = 0; i < bins.size(); i++) if (bins.get(i).usedY + n.h() <= uY) { target = bins.get(i); idx = i; break; }
            if (target == null) { target = new Bin(); bins.add(target); idx = bins.size() - 1; }
            Strip s = new Strip(); s.bin = idx; s.y = target.usedY; s.h = n.h();
            target.usedY += n.h() + kerf; target.strips++;
            strips.add(s);
            place(target, s, n, false, trim, kerf);
        }

        List<Sheet> sheets = new ArrayList<>(); int cuts = 0;
        for (int i = 0; i < bins.size(); i++) {
            Bin b = bins.get(i);
            List<Placement> ps = new ArrayList<>();
            for (Placement p : b.placements)
                ps.add(transposed ? new Placement(p.label(), p.y(), p.x(), p.h(), p.w()) : p);
            int c = b.strips + b.items;
            cuts += c;
            sheets.add(new Sheet(i + 1, ps, c));
        }
        double sheetsArea = bins.size() * L * W / 1e6;
        double netM2 = net / 1e6;
        double waste = sheetsArea == 0 ? 0 : (1 - netM2 / sheetsArea) * 100.0;
        return new Result(code, bins.size(), netM2, sheetsArea, waste, cuts, sheets);
    }

    private void place(Bin bin, Strip s, Norm n, boolean rotated, double trim, double kerf) {
        double w = rotated ? n.h() : n.w(), h = rotated ? n.w() : n.h();
        bin.placements.add(new Placement(n.label(), trim + s.usedX, trim + s.y, w, h));
        s.usedX += w + kerf; s.items++; bin.items++;
    }
}
