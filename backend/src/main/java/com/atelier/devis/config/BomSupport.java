package com.atelier.devis.config;

import com.atelier.devis.domain.BomRepos.ArticleRepo;
import com.atelier.devis.engine.bom.Article;
import com.atelier.devis.engine.bom.ArticleCatalog;
import com.atelier.devis.engine.bom.BomCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Branche le calcul par composants sur le catalogue d'articles en base. */
@Configuration
public class BomSupport {

    @Bean
    public BomCalculator bomCalculator() { return new BomCalculator(); }

    @Component
    public static class DbArticleCatalog implements ArticleCatalog {
        private final ArticleRepo repo;

        public DbArticleCatalog(ArticleRepo repo) { this.repo = repo; }

        public Article article(String code) {
            return repo.findByCode(code).filter(a -> a.isActive()).map(a -> new Article(a.getCode(), a.getName(), a.getCategory(),
                    a.getUnit(), nz(a.getUnitPrice(), BigDecimal.ZERO), nz(a.getApproachCoef(), BigDecimal.ONE))).orElse(null);
        }

        private static BigDecimal nz(BigDecimal v, BigDecimal d) { return v == null ? d : v; }
    }
}
