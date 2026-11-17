package com.atelier.devis.domain;

import com.atelier.devis.domain.Entities.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public final class Repos {
    private Repos() { }

    public interface PanelRepo extends JpaRepository<PanelMaterialEntity, Long> { Optional<PanelMaterialEntity> findByCode(String code); }
    public interface EdgeRepo extends JpaRepository<EdgeBandEntity, Long> { Optional<EdgeBandEntity> findByCode(String code); }
    public interface HardwareRepo extends JpaRepository<HardwareEntity, Long> { Optional<HardwareEntity> findByCode(String code); }
    public interface TypeRepo extends JpaRepository<FurnitureTypeEntity, String> { }
    public interface RuleRepo extends JpaRepository<RuleEntity, Long> { List<RuleEntity> findByFurnitureTypeIn(List<String> types); }
    public interface LaborRepo extends JpaRepository<LaborOperationEntity, Long> { List<LaborOperationEntity> findByFurnitureTypeIn(List<String> types); }
    public interface ThresholdRepo extends JpaRepository<ThresholdEntity, Long> { List<ThresholdEntity> findByKind(String kind); }
    public interface SettingRepo extends JpaRepository<SettingEntity, String> { }
    public interface TaxRepo extends JpaRepository<TaxEntity, Long> { List<TaxEntity> findByActiveTrue(); }
    public interface UserRepo extends JpaRepository<UserEntity, Long> { Optional<UserEntity> findByUsername(String username); }
    public interface QuoteRepo extends JpaRepository<QuoteEntity, Long> { List<QuoteEntity> findAllByOrderByCreatedAtDesc(); }
}
