package com.atelier.devis.domain;

import com.atelier.devis.domain.BomEntities.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public final class BomRepos {
    private BomRepos() { }

    public interface ArticleRepo extends JpaRepository<ArticleEntity, Long> {
        Optional<ArticleEntity> findByCode(String code);
        List<ArticleEntity> findByActiveTrueOrderByCategoryAscNameAsc();
    }
    public interface TemplateLineRepo extends JpaRepository<TemplateLineEntity, Long> {
        List<TemplateLineEntity> findAllByOrderByTemplateNameAscPositionAsc();
        List<TemplateLineEntity> findByTemplateNameOrderByPositionAsc(String templateName);
    }
    public interface ComponentTemplateRepo extends JpaRepository<ComponentTemplateEntity, Long> {
        Optional<ComponentTemplateEntity> findByName(String name);
    }
    public interface TemplatePieceRepo extends JpaRepository<TemplatePieceEntity, Long> {
        List<TemplatePieceEntity> findByTemplateNameOrderByPositionAsc(String templateName);
    }
    public interface BomQuoteRepo extends JpaRepository<BomQuoteEntity, Long> {
        List<BomQuoteEntity> findAllByOrderByCreatedAtDesc();
    }
}
