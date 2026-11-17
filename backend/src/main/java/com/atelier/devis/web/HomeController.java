package com.atelier.devis.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Page d'accueil de l'API (le backend ne sert pas d'interface : l'interface est le frontend React). */
@RestController
public class HomeController {
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("service", "devis-meubles API", "status", "ok",
                "interface", "http://localhost:5173", "documentation", "/swagger-ui.html");
    }
}
