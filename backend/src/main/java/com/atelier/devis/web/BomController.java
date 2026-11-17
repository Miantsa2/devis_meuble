package com.atelier.devis.web;

import com.atelier.devis.config.BomSupport.DbArticleCatalog;
import com.atelier.devis.config.DbEngineSupport.DbEngineConfig;
import com.atelier.devis.domain.BomEntities.BomQuoteEntity;
import com.atelier.devis.domain.BomEntities.ComponentTemplateEntity;
import com.atelier.devis.domain.BomEntities.TemplateLineEntity;
import com.atelier.devis.domain.BomRepos.ArticleRepo;
import com.atelier.devis.domain.BomRepos.BomQuoteRepo;
import com.atelier.devis.domain.BomRepos.ComponentTemplateRepo;
import com.atelier.devis.domain.BomRepos.TemplatePieceRepo;
import com.atelier.devis.domain.Repos.SettingRepo;
import com.atelier.devis.engine.bom.ComponentExpander;
import com.atelier.devis.domain.BomRepos.TemplateLineRepo;
import com.atelier.devis.engine.bom.BomCalculator;
import com.atelier.devis.engine.bom.BomInput;
import com.atelier.devis.engine.bom.BomResult;
import com.atelier.devis.pdf.BomPdfService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Devis par composants : catalogue + gabarits (lecture), calcul, enregistrement, PDF. */
@RestController
@RequestMapping("/api/bom")
public class BomController {
    private final BomCalculator calculator; private final DbArticleCatalog catalog; private final DbEngineConfig config;
    private final ArticleRepo articles; private final TemplateLineRepo templates; private final BomQuoteRepo quotes;
    private final ObjectMapper mapper; private final BomPdfService pdf;
    private final ComponentTemplateRepo headers; private final TemplatePieceRepo pieces; private final SettingRepo settings;
    private final ComponentExpander expander = new ComponentExpander();

    public BomController(BomCalculator calculator, DbArticleCatalog catalog, DbEngineConfig config, ArticleRepo articles,
                         TemplateLineRepo templates, BomQuoteRepo quotes, ObjectMapper mapper, BomPdfService pdf,
                         ComponentTemplateRepo headers, TemplatePieceRepo pieces, SettingRepo settings) {
        this.headers = headers; this.pieces = pieces; this.settings = settings;
        this.calculator = calculator; this.catalog = catalog; this.config = config; this.articles = articles;
        this.templates = templates; this.quotes = quotes; this.mapper = mapper; this.pdf = pdf;
    }

    public record ArticleDto(String code, String name, String category, String unit, BigDecimal unitPrice, BigDecimal approachCoef) { }
    public record ExpandRequest(String template, Double widthMm, Double depthMm, Double heightMm, Integer qty) { }
    public record PieceDto(String name, String panelSlot, double qty, double dim1, double dim2, double surfaceM2, double chantM) { }
    public record ExpandedLineDto(int index, String articleCode, String slot, String kind, BigDecimal quantity) { }
    public record ExpandResponse(List<PieceDto> pieces, List<ExpandedLineDto> lines, double chantMeters) { }
    public record SaveRequest(String customerName, String customerContact, BomInput input) { }
    public record Summary(Long id, String reference, String customerName, Instant createdAt, BigDecimal totalHt,
                          BigDecimal totalTtc, String currency) { }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        List<ArticleDto> arts = articles.findByActiveTrueOrderByCategoryAscNameAsc().stream()
                .map(a -> new ArticleDto(a.getCode(), a.getName(), a.getCategory(), a.getUnit(), a.getUnitPrice(), a.getApproachCoef())).toList();
        List<String> tpls = headers.findAll().stream().map(ComponentTemplateEntity::getName).sorted().toList();
        return Map.of("articles", arts, "templates", tpls);
    }

    /**
     * Calcule les quantités d'un composant à partir de son gabarit et de ses mesures :
     * surfaces de panneaux et mètres de chant issus des pièces, quincaillerie par défaut.
     */
    @PostMapping("/expand")
    public ExpandResponse expand(@RequestBody ExpandRequest r) {
        if (r.template() == null || r.template().isBlank()) throw new IllegalArgumentException("Gabarit requis");
        var header = headers.findByName(r.template()).orElseThrow(() -> new IllegalArgumentException("Gabarit inconnu : " + r.template()));
        List<ComponentExpander.PieceDef> pieceDefs = pieces.findByTemplateNameOrderByPositionAsc(r.template()).stream()
                .map(p -> new ComponentExpander.PieceDef(p.getName(), p.getPanelSlot(), p.getQtyFormula(), p.getDim1Formula(),
                        p.getDim2Formula(), p.getChantSlot(), p.getEdgesDim1(), p.getEdgesDim2())).toList();
        List<ComponentExpander.LineDef> lineDefs = templates.findByTemplateNameOrderByPositionAsc(r.template()).stream()
                .map(l -> new ComponentExpander.LineDef(l.getArticleCode(), l.getSlot(), l.getQuantityFormula(), l.getDefaultQuantity())).toList();
        var def = new ComponentExpander.TemplateDef(header.getName(), nz(header.getPanelWastePercent()), nz(header.getChantWastePercent()),
                pieceDefs, lineDefs);
        double allowance = settings.findById("bom.chant_allowance_mm").map(x -> x.getValueNumeric() == null ? 50.0 : x.getValueNumeric().doubleValue()).orElse(50.0);
        var e = expander.expand(def, d(r.widthMm()), d(r.depthMm()), d(r.heightMm()), r.qty() == null || r.qty() < 1 ? 1 : r.qty(), allowance);
        return new ExpandResponse(
                e.pieces().stream().map(p -> new PieceDto(p.name(), p.panelSlot(), p.qty(), p.dim1(), p.dim2(), p.surfaceM2(), p.chantM())).toList(),
                e.lines().stream().map(l -> new ExpandedLineDto(l.index(), l.articleCode(), l.slot(), l.kind(), l.quantity())).toList(),
                e.chantMeters());
    }

    private static double d(Double v) { return v == null ? 0 : v; }
    private static double nz(BigDecimal v) { return v == null ? 0 : v.doubleValue(); }

    @PostMapping("/calculate")
    public BomResult calculate(@RequestBody BomInput input) {
        return calculator.compute(input, catalog, config.pricing());
    }

    /** Calcule puis fige le devis (saisie + résultat) : les prix modifiés plus tard n'altèrent pas un devis émis. */
    @PostMapping("/quotes")
    public Summary save(@RequestBody SaveRequest req, Authentication auth) throws JsonProcessingException {
        BomResult result = calculator.compute(req.input(), catalog, config.pricing());
        BomQuoteEntity q = new BomQuoteEntity();
        q.setCustomerName(req.customerName()); q.setCustomerContact(req.customerContact());
        q.setCreatedAt(Instant.now()); q.setCreatedBy(auth.getName());
        q.setInputJson(mapper.writeValueAsString(req.input())); q.setResultJson(mapper.writeValueAsString(result));
        q.setTotalHt(result.totalHt()); q.setTotalTtc(result.totalTtc()); q.setCurrency(result.currency());
        q.setReference("TMP");
        q = quotes.save(q);
        q.setReference("DV-" + DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneOffset.UTC).format(q.getCreatedAt())
                + "-" + String.format("%05d", q.getId()));
        return summary(quotes.save(q));
    }

    @GetMapping("/quotes")
    public List<Summary> list() { return quotes.findAllByOrderByCreatedAtDesc().stream().map(this::summary).toList(); }

    @GetMapping("/quotes/{id}")
    public BomResult get(@PathVariable Long id) throws JsonProcessingException {
        return mapper.readValue(find(id).getResultJson(), BomResult.class);
    }

    /** detail=true : fiche de revient interne (lignes, prix, coefficients) ; sinon devis client (sans coûts). */
    @GetMapping("/quotes/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean detail) throws Exception {
        BomQuoteEntity q = find(id);
        BomResult r = mapper.readValue(q.getResultJson(), BomResult.class);
        byte[] bytes = pdf.render(q, r, detail);
        String name = (detail ? "REVIENT-" : "") + q.getReference() + ".pdf";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }

    private BomQuoteEntity find(Long id) {
        return quotes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Summary summary(BomQuoteEntity q) {
        return new Summary(q.getId(), q.getReference(), q.getCustomerName(), q.getCreatedAt(), q.getTotalHt(), q.getTotalTtc(), q.getCurrency());
    }
}
