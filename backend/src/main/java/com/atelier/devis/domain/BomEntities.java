package com.atelier.devis.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Entités du devis par composants : catalogue d'articles, gabarits de composants, devis enregistrés. */
public final class BomEntities {
    private BomEntities() { }

    @Entity @Table(name = "article") @Getter @Setter @NoArgsConstructor
    public static class ArticleEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String code;
        @Column(nullable = false) private String name;
        @Column(nullable = false) private String category;     // PANNEAU | CHANT | COLLE | QUINCAILLERIE | AUTRE
        @Column(nullable = false) private String unit;         // M2 | ML | KG | L | PCE | U
        @Column(precision = 18, scale = 6) private BigDecimal unitPrice = BigDecimal.ZERO;
        @Column(precision = 8, scale = 4) private BigDecimal approachCoef = BigDecimal.ONE;
        private boolean active = true;
        private String note;
    }

    @Entity @Table(name = "template_line") @Getter @Setter @NoArgsConstructor
    public static class TemplateLineEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String templateName;
        private int position;
        private String articleCode;
        @Column(precision = 14, scale = 4) private BigDecimal defaultQuantity = BigDecimal.ZERO;
        private String slot;               // emplacement de panneau/chant alimenté automatiquement par les pièces
        private String quantityFormula;    // ex. 2*Q, CH/50 (variables L, P, H, Q, CH)
    }

    @Entity @Table(name = "component_template") @Getter @Setter @NoArgsConstructor
    public static class ComponentTemplateEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String name;
        @Column(precision = 8, scale = 3) private BigDecimal panelWastePercent = BigDecimal.ZERO;
        @Column(precision = 8, scale = 3) private BigDecimal chantWastePercent = BigDecimal.ZERO;
    }

    @Entity @Table(name = "template_piece") @Getter @Setter @NoArgsConstructor
    public static class TemplatePieceEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String templateName;
        private int position;
        private String name;
        private String panelSlot;
        private String qtyFormula = "Q";
        @Column(name = "dim1_formula")
        private String dim1Formula;

        @Column(name = "dim2_formula")
        private String dim2Formula;
        private String chantSlot;
        private int edgesDim1;
        private int edgesDim2;
    }

    @Entity @Table(name = "bom_quote") @Getter @Setter @NoArgsConstructor
    public static class BomQuoteEntity {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String reference;
        private String customerName;
        private String customerContact;
        private Instant createdAt;
        private String createdBy;
        @Column(columnDefinition = "text") private String inputJson;
        @Column(columnDefinition = "text") private String resultJson;
        @Column(precision = 14, scale = 2) private BigDecimal totalHt;
        @Column(precision = 14, scale = 2) private BigDecimal totalTtc;
        private String currency;
    }
}
