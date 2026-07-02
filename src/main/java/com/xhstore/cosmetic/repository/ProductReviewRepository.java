package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /** Tất cả đánh giá cho 1 sản phẩm, mới nhất trước, có phân trang. */
    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /** Tất cả đánh giá cho 1 sản phẩm (không phân trang, dùng để tính avg). */
    List<ProductReview> findByProductId(Long productId);

    /** Tất cả đánh giá (admin). */
    List<ProductReview> findAllByOrderByCreatedAtDesc();

    /** Kiểm tra user đã đánh giá SP này trong đơn hàng chưa. */
    Optional<ProductReview> findByProductIdAndUserIdAndOrderId(Long productId, Long userId, Long orderId);

    /** Rating trung bình của một sản phẩm. */
    @Query("SELECT AVG(r.rating * 1.0) FROM ProductReview r WHERE r.product.id = :productId")
    Double avgRatingByProductId(@Param("productId") Long productId);

    /** Lấy danh sách ID sản phẩm đã được đánh giá bởi user trong đơn hàng cụ thể. */
    @Query("SELECT r.product.id FROM ProductReview r WHERE r.user.id = :userId AND r.orderId = :orderId")
    List<Long> findProductIdsByUserIdAndOrderId(@Param("userId") Long userId, @Param("orderId") Long orderId);
}
