package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findByProductIdAndName(Long productId, String name);
}
