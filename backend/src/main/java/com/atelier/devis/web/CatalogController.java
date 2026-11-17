package com.atelier.devis.web;

import com.atelier.devis.domain.Entities.*;
import com.atelier.devis.domain.Repos.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Données de référence en lecture seule pour construire le formulaire (SANS les prix). */
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final PanelRepo panels; private final EdgeRepo edges; private final HardwareRepo hardware; private final TypeRepo types;

    public CatalogController(PanelRepo p, EdgeRepo e, HardwareRepo h, TypeRepo t) { panels = p; edges = e; hardware = h; types = t; }

    public record Option(String code, String name, String category, Double thicknessMm, Double lengthMm, Double widthMm, Double slideLengthMm) { }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return Map.of(
                "furnitureTypes", types.findAll().stream().map(t -> Map.of("code", t.getId(), "label", t.getLabel())).toList(),
                "panels", panels.findAll().stream().filter(PanelMaterialEntity::isActive)
                        .map(p -> new Option(p.getCode(), p.getName(), p.getFamily(), p.getThicknessMm(), p.getLengthMm(), p.getWidthMm(), null)).toList(),
                "edgeBands", edges.findAll().stream().filter(EdgeBandEntity::isActive)
                        .map(e -> new Option(e.getCode(), e.getName(), null, e.getThicknessMm(), null, null, null)).toList(),
                "hardware", hardware.findAll().stream().filter(HardwareEntity::isActive)
                        .map(h -> new Option(h.getCode(), h.getName(), h.getCategory(), null, null, null, h.getSlideLengthMm())).toList());
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(Authentication a) {
        return Map.of("username", a.getName(), "admin", a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN")));
    }
}
