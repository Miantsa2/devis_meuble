package com.atelier.devis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
// Les dépôts sont des interfaces imbriquées dans Repos : Spring Data les ignore sauf demande explicite.
@EnableJpaRepositories(basePackages = "com.atelier.devis.domain", considerNestedRepositories = true)
public class DevisApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevisApplication.class, args);
    }
}
