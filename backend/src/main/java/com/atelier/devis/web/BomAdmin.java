package com.atelier.devis.web;

import com.atelier.devis.domain.BomEntities.ArticleEntity;
import com.atelier.devis.domain.BomEntities.ComponentTemplateEntity;
import com.atelier.devis.domain.BomEntities.TemplatePieceEntity;
import com.atelier.devis.domain.BomEntities.TemplateLineEntity;
import com.atelier.devis.domain.BomRepos.ArticleRepo;
import com.atelier.devis.domain.BomRepos.ComponentTemplateRepo;
import com.atelier.devis.domain.BomRepos.TemplatePieceRepo;
import com.atelier.devis.domain.BomRepos.TemplateLineRepo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administration du catalogue d'articles et des gabarits de composants (CRUD générique, rôle ADMIN). */
public final class BomAdmin {
    private BomAdmin() { }

    @RestController @RequestMapping("/api/admin/articles")
    public static class Articles extends AdminCrudController<ArticleEntity, Long> {
        public Articles(ArticleRepo r) { super(r); }
    }

    @RestController @RequestMapping("/api/admin/template-lines")
    public static class TemplateLines extends AdminCrudController<TemplateLineEntity, Long> {
        public TemplateLines(TemplateLineRepo r) { super(r); }
    }

    @RestController @RequestMapping("/api/admin/component-templates")
    public static class ComponentTemplates extends AdminCrudController<ComponentTemplateEntity, Long> {
        public ComponentTemplates(ComponentTemplateRepo r) { super(r); }
    }

    @RestController @RequestMapping("/api/admin/template-pieces")
    public static class TemplatePieces extends AdminCrudController<TemplatePieceEntity, Long> {
        public TemplatePieces(TemplatePieceRepo r) { super(r); }
    }
}
