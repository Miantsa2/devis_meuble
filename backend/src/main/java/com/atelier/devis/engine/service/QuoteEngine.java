package com.atelier.devis.engine.service;

import com.atelier.devis.engine.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Orchestrateur : spécification -> pièces -> débit -> chants -> quincaillerie -> main-d'œuvre -> prix. */
public class QuoteEngine {

    private final CarcassGeometryService geometry = new CarcassGeometryService();
    private final EdgeBandingService edging = new EdgeBandingService();
    private final CutPlanService cutPlan = new CutPlanService();
    private final HardwareService hardwareService = new HardwareService();
    private final System32Service system32 = new System32Service();
    private final LaborService laborService = new LaborService();
    private final StructuralCheckService structural = new StructuralCheckService();

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public QuoteResult compute(FurnitureSpec s, Catalog cat, EngineConfig cfg) {
        if (s.quantity() < 1) throw new IllegalArgumentException("La quantité doit être >= 1");
        Rules r = cfg.rules(s.typeCode());
        PricingSettings pr = cfg.pricing();
        List<String> warnings = new ArrayList<>();
        int units = s.quantity();

        PanelMaterial carcass = panel(cat, s.carcassMaterial(), "caisson");
        PanelMaterial back = (s.backMaterial() == null || s.backMaterial().isBlank()) ? null : panel(cat, s.backMaterial(), "fond");
        PanelMaterial front = (s.frontMaterial() == null || s.frontMaterial().isBlank()) ? carcass : panel(cat, s.frontMaterial(), "façades");
        Hardware slide = s.drawers() > 0 ? cat.hardware(s.slideCode()) : null;
        EdgeBand edgeBand = (s.edgeBand() == null || s.edgeBand().isBlank()) ? null : cat.edgeBand(s.edgeBand());
        if (s.edgeBand() != null && !s.edgeBand().isBlank() && edgeBand == null)
            throw new IllegalArgumentException("Chant inconnu : " + s.edgeBand());

        // 1. Pièces
        CarcassGeometryService.Result geo = geometry.generate(s, carcass, back, front, slide, r);
        warnings.addAll(geo.warnings());
        List<Part> unitParts = geo.parts();
        List<Part> allParts = unitParts.stream().map(p -> p.withQty(p.qty() * units)).toList();

        // 2. Chants et dimensions de débit
        double eT = edgeBand == null ? 0 : edgeBand.thicknessMm();
        List<Part> banded = edgeBand == null
                ? allParts.stream().map(p -> new Part(p.role(), p.label(), p.qty(), p.a(), p.b(), p.materialCode(), p.grainFixed(), 0, 0)).toList()
                : allParts;
        EdgeBandingService.Result edgeRes = edging.compute(banded, r);

        List<QuoteResult.PartLine> partLines = new ArrayList<>();
        Map<String, List<CutPlanService.Item>> byMaterial = new LinkedHashMap<>();
        Map<String, PanelMaterial> materials = new LinkedHashMap<>();
        for (Part p : banded) {
            double[] core = edging.coreDims(p, eT);
            PanelMaterial m = materialOf(p.materialCode(), carcass, back, front);
            materials.put(m.code(), m);
            partLines.add(new QuoteResult.PartLine(p.role(), p.label(), p.qty(), p.a(), p.b(), core[0], core[1],
                    m.thicknessMm(), m.code(), p.aEdges(), p.bEdges()));
            for (int i = 0; i < p.qty(); i++)
                byMaterial.computeIfAbsent(m.code(), k -> new ArrayList<>())
                        .add(new CutPlanService.Item(p.label(), core[0], core[1], p.grainFixed()));
        }

        // 3. Débit par matériau + coût panneaux
        List<QuoteResult.PanelUsage> panels = new ArrayList<>();
        BigDecimal panelCost = BigDecimal.ZERO;
        int totalSheets = 0, totalCuts = 0;
        for (var e : byMaterial.entrySet()) {
            PanelMaterial m = materials.get(e.getKey());
            CutPlanService.Result cp = cutPlan.plan(m.code(), e.getValue(), m.lengthMm(), m.widthMm(), m.grain(),
                    r.d("sheet.trim_mm"), r.d("sheet.kerf_mm"));
            BigDecimal cost;
            if (pr.panelCostingMode() == 0) {
                cost = m.pricePerSheet().multiply(BigDecimal.valueOf(cp.sheetCount()));
            } else {
                BigDecimal share = BigDecimal.valueOf(cp.netAreaM2() / (m.lengthMm() * m.widthMm() / 1e6));
                cost = m.pricePerSheet().multiply(share)
                        .multiply(BigDecimal.ONE.add(pr.panelWastePercent().divide(HUNDRED, 6, RoundingMode.HALF_UP)));
            }
            cost = cost.setScale(2, RoundingMode.HALF_UP);
            panelCost = panelCost.add(cost);
            totalSheets += cp.sheetCount(); totalCuts += cp.cutCount();
            if (m.pricePerSheet().signum() == 0) warnings.add("Prix non renseigné pour le panneau " + m.code());
            panels.add(new QuoteResult.PanelUsage(m.code(), m.name(), m.thicknessMm(), cp.sheetCount(),
                    cp.netAreaM2(), cp.sheetsAreaM2(), cp.wastePercent(), cp.cutCount(), m.pricePerSheet(), cost,
                    m.lengthMm(), m.widthMm(), cp.sheets()));
        }

        // 4. Coût des chants
        QuoteResult.EdgeLine edgeLine = null;
        BigDecimal edgeCost = BigDecimal.ZERO;
        if (edgeBand != null) {
            edgeCost = edgeBand.pricePerMeter().multiply(BigDecimal.valueOf(edgeRes.totalMeters())).setScale(2, RoundingMode.HALF_UP);
            if (edgeBand.pricePerMeter().signum() == 0) warnings.add("Prix non renseigné pour le chant " + edgeBand.code());
            edgeLine = new QuoteResult.EdgeLine(edgeBand.code(), edgeBand.name(), edgeRes.netMm() / 1000.0,
                    edgeRes.appliedMeters(), edgeRes.totalMeters(), edgeRes.strips(), edgeBand.pricePerMeter(), edgeCost);
        } else warnings.add("Aucun chant sélectionné : bords non plaqués et non chiffrés.");

        // 5. Quincaillerie
        HardwareService.Result hw = hardwareService.compute(s, geo, front, cat, cfg, r);
        warnings.addAll(hw.warnings());
        BigDecimal hwCost = hw.lines().stream().map(HardwareService.Line::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        hw.lines().stream().filter(l -> l.unitPrice().signum() == 0)
                .forEach(l -> warnings.add("Prix non renseigné pour l'accessoire " + l.code()));

        // 6. Finition (surface visible x facteur de faces)
        double visibleMm2 = 0;
        Set<String> visible = Set.of("SIDE", "TOP", "BOTTOM", "DIVIDER", "SHELF", "PLINTH", "DOOR", "DRAWER_FRONT");
        for (Part p : allParts) if (visible.contains(p.role())) visibleMm2 += p.qty() * p.a() * p.b();
        double finishM2 = visibleMm2 / 1e6 * r.d("finish.faces_factor");
        BigDecimal finishCost = pr.finishPricePerM2().multiply(BigDecimal.valueOf(finishM2)).setScale(2, RoundingMode.HALF_UP);

        // 7. Main-d'œuvre
        int compartments = s.dividers() + 1;
        double innerH = (s.heightMm() - (s.hasPlinth() ? s.plinthHeightMm() : 0)) - 2 * carcass.thicknessMm();
        int pinHoles = s.shelvesPerCompartment() > 0 ? system32.shelfPinHoles(compartments, innerH, s.depthMm(), r) * units : 0;
        int hingeCups = hw.hinges();
        int connectorHoles = hw.connectors() * r.iOr("connector.holes_each", 0);
        int pieces = allParts.stream().mapToInt(Part::qty).sum();
        Map<LaborBasis, Double> q = new EnumMap<>(LaborBasis.class);
        q.put(LaborBasis.PER_SHEET, (double) totalSheets);
        q.put(LaborBasis.PER_CUT, (double) totalCuts);
        q.put(LaborBasis.PER_PART, (double) pieces);
        q.put(LaborBasis.PER_EDGE_METER, edgeRes.appliedMeters());
        q.put(LaborBasis.PER_EDGE_STRIP, (double) edgeRes.strips());
        q.put(LaborBasis.PER_HOLE, (double) (pinHoles + hingeCups + connectorHoles));
        q.put(LaborBasis.PER_UNIT, (double) units);
        q.put(LaborBasis.PER_DOOR, (double) s.doors() * units);
        q.put(LaborBasis.PER_DRAWER, (double) s.drawers() * units);
        q.put(LaborBasis.PER_HINGE, (double) hingeCups);
        q.put(LaborBasis.PER_M2_FINISH, finishM2);
        List<LaborOp> ops = cfg.laborOps(s.typeCode());
        LaborService.Result labor = laborService.compute(ops, q, pr.hourlyRate());
        if (ops.stream().allMatch(o -> o.minutes().signum() == 0))
            warnings.add("Temps de fabrication de référence non renseignés : main-d'œuvre chiffrée à 0.");
        if (pr.hourlyRate().signum() == 0) warnings.add("Taux horaire non renseigné.");

        // 8. Contrôle de flèche des étagères (si charge, E et épaisseur connus)
        List<QuoteResult.ShelfCheck> checks = new ArrayList<>();
        if (s.shelvesPerCompartment() > 0 && s.shelfLoadKgPerM2() != null && s.shelfLoadKgPerM2() > 0) {
            if (carcass.eModulusMpa() == null) {
                warnings.add("Module d'élasticité du panneau non renseigné : flèche des étagères non vérifiée.");
            } else {
                for (Part p : unitParts) if (p.role().equals("SHELF")) {
                    double span = p.a(), depth = p.b();
                    double d = structural.deflectionMm(s.shelfLoadKgPerM2(), depth, carcass.thicknessMm(), span, carcass.eModulusMpa());
                    double limit = span / r.d("shelf.max_deflection_ratio");
                    double maxSpan = structural.maxSpanMm(s.shelfLoadKgPerM2(), depth, carcass.thicknessMm(),
                            carcass.eModulusMpa(), r.d("shelf.max_deflection_ratio"));
                    boolean ok = d <= limit;
                    checks.add(new QuoteResult.ShelfCheck(span, d, limit, maxSpan, ok));
                    if (!ok) warnings.add(String.format(Locale.ROOT,
                            "Étagère de %.0f mm : flèche estimée %.1f mm > %.1f mm admissible. Portée max indicative %.0f mm : ajouter une séparation ou un panneau plus épais.",
                            span, d, limit, maxSpan));
                }
            }
        }
        if (!r.toValidateKeys().isEmpty())
            warnings.add(r.toValidateKeys().size() + " règle(s) de fabrication marquée(s) « à valider » sont utilisées dans ce calcul (voir l'administration).");

        // 9. Prix
        BigDecimal production = panelCost.add(edgeCost).add(hwCost).add(labor.cost()).add(finishCost);
        BigDecimal markup = production.multiply(pr.markupPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal transport = pr.transportFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal install = pr.installationPerUnit().multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalHt = production.add(markup).add(transport).add(install);
        List<QuoteResult.TaxLine> taxLines = new ArrayList<>();
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (Tax t : pr.taxes()) {
            BigDecimal amount = totalHt.multiply(t.ratePercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            taxLines.add(new QuoteResult.TaxLine(t.code(), t.label(), t.ratePercent(), amount));
            taxTotal = taxTotal.add(amount);
        }
        if (pr.taxes().isEmpty()) warnings.add("Aucune taxe active configurée : total TTC = total HT.");
        QuoteResult.Costs costs = new QuoteResult.Costs(panelCost, edgeCost, hwCost, labor.cost(), finishCost,
                production, pr.markupPercent(), markup, transport, install, totalHt, taxLines, totalHt.add(taxTotal));

        return new QuoteResult(pr.currency(), units, partLines, panels, edgeLine, hw.lines(), labor,
                finishM2, checks, costs, warnings, r.toValidateKeys().size());
    }

    private PanelMaterial panel(Catalog cat, String code, String what) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Matériau requis : " + what);
        PanelMaterial m = cat.panel(code);
        if (m == null) throw new IllegalArgumentException("Panneau inconnu (" + what + ") : " + code);
        return m;
    }

    private PanelMaterial materialOf(String code, PanelMaterial... candidates) {
        for (PanelMaterial m : candidates) if (m != null && m.code().equals(code)) return m;
        throw new IllegalStateException("Matériau introuvable : " + code);
    }
}
