package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrue();
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    List<Product> findByCategoryIdAndIdNotAndIsActiveTrue(Long categoryId, Long id);

    /**
     * Lấy sản phẩm có khóa ghi (chống race condition trừ kho đồng thời).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
