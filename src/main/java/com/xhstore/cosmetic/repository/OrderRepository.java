package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findAllByOrderByCreatedAtDesc();

    /** Kiểm tra trùng mã đơn dùng cho genCode() để tránh unique constraint violation. */
    boolean existsByOrderCode(String orderCode);
}
