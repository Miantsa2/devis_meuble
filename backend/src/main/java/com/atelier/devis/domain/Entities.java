package com.atelier.devis.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Entités JPA regroupées dans un fichier pour la lisibilité (une classe publique = un conteneur, entités statiques). */
public final class Entities {
    private Entities() { }

    @Entity @Table(name = "panel_material") @Getter @Setter @NoArgsConstructor
    public static class PanelMaterialEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String code;
        @Column(nullable = false) private String name;
        private String family;
        private double thicknessMm;
        private double lengthMm;
        private double widthMm;
        @Column(precision = 14, scale = 2) private BigDecimal pricePerSheet = BigDecimal.ZERO;
        private boolean grain;
        @Column(name = "density_kg_m3")
        private Double densityKgM3;
        private Double youngModulusMpa;
        private boolean active = true;
    }

    @Entity @Table(name = "edge_band") @Getter @Setter @NoArgsConstructor
    public static class EdgeBandEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String code;
        @Column(nullable = false) private String name;
        private double thicknessMm;
        private double widthMm;
        @Column(precision = 14, scale = 4) private BigDecimal pricePerMeter = BigDecimal.ZERO;
        private boolean active = true;
    }

    @Entity @Table(name = "hardware_item") @Getter @Setter @NoArgsConstructor
    public static class HardwareEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String code;
        @Column(nullable = false) private String name;
        private String category;
        @Column(precision = 14, scale = 4) private BigDecimal unitPrice = BigDecimal.ZERO;
        private Double slideLengthMm;
        private boolean active = true;
    }

    @Entity @Table(name = "furniture_type") @Getter @Setter @NoArgsConstructor
    public static class FurnitureTypeEntity implements HasId<String> {
        @Id private String id;           // code, ex. MEUBLE_HAUT
        @Column(nullable = false) private String label;
    }

    @Entity @Table(name = "manufacturing_rule") @Getter @Setter @NoArgsConstructor
    public static class RuleEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String furnitureType;     // DEFAULT ou code d'un type : la valeur du type écrase DEFAULT
        private String ruleKey;
        @Column(precision = 18, scale = 6) private BigDecimal value;
        private String unit;
        @Column(length = 1000) private String source;
        private String validationStatus;  // DOCUMENTED | DESIGN_CHOICE | TO_VALIDATE
        @Column(length = 1000) private String description;
    }

    @Entity @Table(name = "labor_operation") @Getter @Setter @NoArgsConstructor
    public static class LaborOperationEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String furnitureType;
        private String code;
        private String label;
        private String basis;             // valeur de LaborBasis
        @Column(precision = 12, scale = 4) private BigDecimal minutes = BigDecimal.ZERO;
        private String validationStatus;
    }

    @Entity @Table(name = "threshold_rule") @Getter @Setter @NoArgsConstructor
    public static class ThresholdEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String kind;              // HINGE_HEIGHT | HINGE_WEIGHT
        private double upTo;
        private int count;
        @Column(length = 1000) private String source;
        private String validationStatus;
    }

    @Entity @Table(name = "app_setting") @Getter @Setter @NoArgsConstructor
    public static class SettingEntity implements HasId<String> {
        @Id private String id;            // clé, ex. labor.hourly_rate
        @Column(precision = 18, scale = 6) private BigDecimal valueNumeric;
        private String valueText;
        private String unit;
        @Column(length = 1000) private String description;
    }

    @Entity @Table(name = "tax_rate") @Getter @Setter @NoArgsConstructor
    public static class TaxEntity implements HasId<Long> {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true) private String code;
        private String label;
        @Column(precision = 8, scale = 4) private BigDecimal ratePercent = BigDecimal.ZERO;
        private boolean active = true;
    }

    @Entity @Table(name = "app_user") @Getter @Setter @NoArgsConstructor
    public static class UserEntity {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String username;
        @Column(nullable = false) private String passwordHash;
        @Column(nullable = false) private String role;   // ADMIN | USER
    }

    @Entity @Table(name = "quote") @Getter @Setter @NoArgsConstructor
    public static class QuoteEntity {
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
