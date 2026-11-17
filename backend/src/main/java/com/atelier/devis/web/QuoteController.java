package com.atelier.devis.web;

import com.atelier.devis.config.DbEngineSupport.DbCatalog;
import com.atelier.devis.config.DbEngineSupport.DbEngineConfig;
import com.atelier.devis.domain.Entities.QuoteEntity;
import com.atelier.devis.domain.Repos.QuoteRepo;
import com.atelier.devis.engine.model.FurnitureSpec;
import com.atelier.devis.engine.service.QuoteEngine;
import com.atelier.devis.engine.service.QuoteResult;
import com.atelier.devis.pdf.QuotePdfService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {
    private final QuoteEngine engine; private final DbCatalog catalog; private final DbEngineConfig config;
    private final QuoteRepo quotes; private final ObjectMapper mapper; private final QuotePdfService pdf;

    public QuoteController(QuoteEngine engine, DbCatalog catalog, DbEngineConfig config, QuoteRepo quotes,
                           ObjectMapper mapper, QuotePdfService pdf) {
        this.engine = engine; this.catalog = catalog; this.config = config; this.quotes = quotes; this.mapper = mapper; this.pdf = pdf;
    }

    public record SaveRequest(String customerName, String customerContact, FurnitureSpec spec) { }
    public record Summary(Long id, String reference, String customerName, Instant createdAt, java.math.BigDecimal totalHt,
                          java.math.BigDecimal totalTtc, String currency) { }

    /** Calcul sans enregistrement (aperçu en direct dans le formulaire). */
    @PostMapping("/calculate")
    public QuoteResult calculate(@RequestBody FurnitureSpec spec) {
        return engine.compute(spec, catalog, config);
    }

    /** Calcule puis fige le devis (entrée + résultat) : les prix ultérieurs n'altèrent pas un devis émis. */
    @PostMapping
    public Summary save(@RequestBody SaveRequest req, Authentication auth) throws JsonProcessingException {
        QuoteResult result = engine.compute(req.spec(), catalog, config);
        QuoteEntity q = new QuoteEntity();
        q.setCustomerName(req.customerName()); q.setCustomerContact(req.customerContact());
        q.setCreatedAt(Instant.now()); q.setCreatedBy(auth.getName());
        q.setInputJson(mapper.writeValueAsString(req.spec()));
        q.setResultJson(mapper.writeValueAsString(result));
        q.setTotalHt(result.costs().totalHt()); q.setTotalTtc(result.costs().totalTtc()); q.setCurrency(result.currency());
        q.setReference("TMP");
        q = quotes.save(q);
        q.setReference("DV-" + DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneOffset.UTC).format(q.getCreatedAt())
                + "-" + String.format("%05d", q.getId()));
        q = quotes.save(q);
        return summary(q);
    }

    @GetMapping
    public List<Summary> list() { return quotes.findAllByOrderByCreatedAtDesc().stream().map(this::summary).toList(); }

    @GetMapping("/{id}")
    public QuoteResult get(@PathVariable Long id) throws JsonProcessingException {
        return mapper.readValue(find(id).getResultJson(), QuoteResult.class);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) throws Exception {
        QuoteEntity q = find(id);
        QuoteResult r = mapper.readValue(q.getResultJson(), QuoteResult.class);
        FurnitureSpec s = mapper.readValue(q.getInputJson(), FurnitureSpec.class);
        byte[] bytes = pdf.render(q, s, r);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + q.getReference() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }

    private QuoteEntity find(Long id) {
        return quotes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Summary summary(QuoteEntity q) {
        return new Summary(q.getId(), q.getReference(), q.getCustomerName(), q.getCreatedAt(), q.getTotalHt(), q.getTotalTtc(), q.getCurrency());
    }
}
