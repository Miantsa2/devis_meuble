package com.atelier.devis.web;

import com.atelier.devis.domain.Entities.*;
import com.atelier.devis.domain.HasId;
import com.atelier.devis.domain.Repos.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** CRUD générique de l'administration : toutes les valeurs (prix, temps, règles, marges, taxes) sont modifiables ici. */
public abstract class AdminCrudController<T extends HasId<ID>, ID> {
    private final JpaRepository<T, ID> repo;

    protected AdminCrudController(JpaRepository<T, ID> repo) { this.repo = repo; }

    @GetMapping
    public List<T> list() { return repo.findAll(); }

    @PostMapping
    public T create(@RequestBody T body) {
        body.setId(null);
        return repo.save(body);
    }

    @PutMapping("/{id}")
    public T update(@PathVariable ID id, @RequestBody T body) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        body.setId(id);
        return repo.save(body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable ID id) { repo.deleteById(id); }

    @RestController @RequestMapping("/api/admin/panels")
    public static class Panels extends AdminCrudController<PanelMaterialEntity, Long> { public Panels(PanelRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/edge-bands")
    public static class Edges extends AdminCrudController<EdgeBandEntity, Long> { public Edges(EdgeRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/hardware")
    public static class Hardware extends AdminCrudController<HardwareEntity, Long> { public Hardware(HardwareRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/furniture-types")
    public static class Types extends AdminCrudController<FurnitureTypeEntity, String> {
        public Types(TypeRepo r) { super(r); }

        @Override @PostMapping
        public FurnitureTypeEntity create(@RequestBody FurnitureTypeEntity body) {
            if (body.getId() == null || body.getId().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code requis");
            return super.createKeepingId(body);
        }
    }

    @RestController @RequestMapping("/api/admin/rules")
    public static class Rules extends AdminCrudController<RuleEntity, Long> { public Rules(RuleRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/labor")
    public static class Labor extends AdminCrudController<LaborOperationEntity, Long> { public Labor(LaborRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/thresholds")
    public static class Thresholds extends AdminCrudController<ThresholdEntity, Long> { public Thresholds(ThresholdRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/taxes")
    public static class Taxes extends AdminCrudController<TaxEntity, Long> { public Taxes(TaxRepo r) { super(r); } }

    @RestController @RequestMapping("/api/admin/settings")
    public static class Settings extends AdminCrudController<SettingEntity, String> {
        public Settings(SettingRepo r) { super(r); }

        @Override @PostMapping
        public SettingEntity create(@RequestBody SettingEntity body) {
            if (body.getId() == null || body.getId().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clé requise");
            return super.createKeepingId(body);
        }
    }

    /** Pour les entités à identifiant naturel (code/clé) fourni par le client. */
    protected T createKeepingId(T body) { return repo.save(body); }
}
