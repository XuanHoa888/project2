package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.BoothConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoothConfigRepository extends JpaRepository<BoothConfig, Long> {
    Optional<BoothConfig> findByBoothKey(String boothKey);
}
