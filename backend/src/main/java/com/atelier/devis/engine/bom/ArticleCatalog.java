package com.atelier.devis.engine.bom;

public interface ArticleCatalog {
    /** @return l'article actif correspondant au code, ou null. */
    Article article(String code);
}
