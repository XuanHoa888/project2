package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.CustomOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {
    List<CustomOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CustomOrder> findAllByOrderByCreatedAtDesc();
}
