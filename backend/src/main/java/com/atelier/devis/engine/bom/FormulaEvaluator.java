package com.atelier.devis.engine.bom;

import java.util.Locale;
import java.util.Map;

/**
 * Évaluateur d'expressions arithmétiques simples (+ - * / parenthèses, nombres, variables).
 * Sert aux formules des gabarits, ex. "L-38", "L/Q-1", "2*Q", "CH/50". Pas de script, pas de fonctions :
 * une formule ne peut ni lire ni écrire autre chose que les variables fournies.
 */
public final class FormulaEvaluator {
    private final String src;
    private final Map<String, Double> vars;
    private int pos;

    private FormulaEvaluator(String src, Map<String, Double> vars) { this.src = src; this.vars = vars; }

    public static double eval(String formula, Map<String, Double> vars) {
        if (formula == null || formula.isBlank()) throw new IllegalArgumentException("Formule vide");
        FormulaEvaluator p = new FormulaEvaluator(formula, vars);
        double v = p.expr();
        p.skip();
        if (p.pos < p.src.length()) throw p.error("caractère inattendu « " + p.src.charAt(p.pos) + " »");
        if (Double.isNaN(v) || Double.isInfinite(v)) throw p.error("résultat invalide");
        return v;
    }

    private double expr() {
        double v = term();
        while (true) {
            skip();
            if (match('+')) v += term(); else if (match('-')) v -= term(); else return v;
        }
    }

    private double term() {
        double v = factor();
        while (true) {
            skip();
            if (match('*')) v *= factor();
            else if (match('/')) {
                double d = factor();
                if (d == 0) throw error("division par zéro");
                v /= d;
            } else return v;
        }
    }

    private double factor() {
        skip();
        if (match('-')) return -factor();
        if (match('+')) return factor();
        if (match('(')) {
            double v = expr();
            skip();
            if (!match(')')) throw error("parenthèse fermante manquante");
            return v;
        }
        if (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == ',')) {
            int s = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == ',')) pos++;
            try { return Double.parseDouble(src.substring(s, pos).replace(',', '.')); }
            catch (NumberFormatException e) { throw error("nombre invalide « " + src.substring(s, pos) + " »"); }
        }
        if (pos < src.length() && (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_')) {
            int s = pos;
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
            String name = src.substring(s, pos).toUpperCase(Locale.ROOT);
            Double v = vars.get(name);
            if (v == null) throw error("variable inconnue « " + name + " » (disponibles : " + String.join(", ", vars.keySet()) + ")");
            return v;
        }
        throw error(pos >= src.length() ? "expression incomplète" : "caractère inattendu « " + src.charAt(pos) + " »");
    }

    private void skip() { while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++; }

    private boolean match(char c) {
        if (pos < src.length() && src.charAt(pos) == c) { pos++; return true; }
        return false;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("Formule invalide « " + src + " » : " + msg);
    }
}
