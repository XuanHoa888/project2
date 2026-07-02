package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findByOrderId(Long orderId);
    List<RefundRequest> findAllByOrderByCreatedAtDesc();
}
