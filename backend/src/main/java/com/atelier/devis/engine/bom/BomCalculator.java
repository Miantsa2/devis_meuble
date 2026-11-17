package com.atelier.devis.engine.bom;

import com.atelier.devis.engine.model.PricingSettings;
import com.atelier.devis.engine.model.Tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul d'un devis par composants.
 *   montant de ligne      = quantité saisie x prix unitaire x coefficient d'arrivée de l'article (arrondi à 0,01)
 *   total d'un composant  = somme des lignes (les quantités de ligne incluent déjà la quantité QTE du composant)
 *   matériaux             = somme des composants ; x quantité de meuble
 *   coût de production    = matériaux + (heures x taux horaire x quantité de meuble)
 *   prix HT               = production + marge (% du coût) + transport (forfait) + installation (par meuble)
 * Tous les prix, coefficients et taux viennent du catalogue / des paramètres : rien n'est codé ici.
 */
public class BomCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public BomResult compute(BomInput in, ArticleCatalog catalog, PricingSettings pr) {
        if (in.quantity() < 1) throw new IllegalArgumentException("La quantité de meubles doit être >= 1");
        if (in.components() == null || in.components().isEmpty())
            throw new IllegalArgumentException("Ajoutez au moins un composant (caisson, porte...)");
        List<String> warnings = new ArrayList<>();
        List<BomResult.ComponentResult> comps = new ArrayList<>();
        BigDecimal perUnit = BigDecimal.ZERO;

        for (BomInput.ComponentInput c : in.components()) {
            if (c.qty() < 1) throw new IllegalArgumentException("Composant « " + c.name() + " » : la quantité doit être >= 1");
            List<BomResult.LineResult> lines = new ArrayList<>();
            BigDecimal unitTotal = BigDecimal.ZERO;
            for (BomInput.LineInput l : c.lines() == null ? List.<BomInput.LineInput>of() : c.lines()) {
                if (l.articleCode() == null || l.articleCode().isBlank()) continue;     // ligne non choisie : ignorée
                Article a = catalog.article(l.articleCode());
                if (a == null) throw new IllegalArgumentException("Article inconnu ou inactif : " + l.articleCode());
                BigDecimal q = l.quantity() == null ? BigDecimal.ZERO : l.quantity();
                if (q.signum() < 0) throw new IllegalArgumentException("Quantité négative pour " + a.code() + " (" + c.name() + ")");
                BigDecimal amount = q.multiply(a.unitPrice()).multiply(a.coef()).setScale(2, RoundingMode.HALF_UP);
                if (a.unitPrice().signum() == 0) warnings.add("Prix non renseigné pour l'article " + a.code());
                lines.add(new BomResult.LineResult(a.category(), a.code(), a.name(), a.unit(), q, a.unitPrice(), a.coef(), amount));
                unitTotal = unitTotal.add(amount);
            }
            BigDecimal total = unitTotal;
            comps.add(new BomResult.ComponentResult(c.name(), c.widthMm(), c.depthMm(), c.heightMm(), c.qty(), lines, unitTotal, total));
            perUnit = perUnit.add(total);
        }
        BigDecimal units = BigDecimal.valueOf(in.quantity());
        BigDecimal materials = perUnit.multiply(units);
        BigDecimal hours = in.laborHours() == null ? BigDecimal.ZERO : in.laborHours();
        if (hours.signum() < 0) throw new IllegalArgumentException("Les heures de main-d'œuvre ne peuvent pas être négatives");
        BigDecimal laborCost = hours.multiply(pr.hourlyRate()).multiply(units).setScale(2, RoundingMode.HALF_UP);
        if (hours.signum() > 0 && pr.hourlyRate().signum() == 0) warnings.add("Taux horaire non renseigné.");
        BigDecimal production = materials.add(laborCost);
        BigDecimal markup = production.multiply(pr.markupPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal transport = pr.transportFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal install = pr.installationPerUnit().multiply(units).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ht = production.add(markup).add(transport).add(install);

        List<BomResult.TaxLine> taxLines = new ArrayList<>();
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (Tax t : pr.taxes()) {
            BigDecimal amount = ht.multiply(t.ratePercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            taxLines.add(new BomResult.TaxLine(t.code(), t.label(), t.ratePercent(), amount));
            taxTotal = taxTotal.add(amount);
        }
        if (pr.taxes().isEmpty()) warnings.add("Aucune taxe active configurée : total TTC = total HT.");
        return new BomResult(in.name(), in.quantity(), pr.currency(), comps, perUnit, materials, hours, pr.hourlyRate(), laborCost,
                production, pr.markupPercent(), markup, transport, install, ht, taxLines, ht.add(taxTotal), warnings);
    }
}
