package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.*;
import com.xhstore.cosmetic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired private ProductReviewRepository reviewRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private OrderRepository orderRepo;

    /**
     * Tạo đánh giá mới.
     * Validate: đơn hàng phải DELIVERED, chưa từng đánh giá SP này trong đơn này.
     */
    @Transactional
    public ProductReview createReview(User user, Long productId, Long orderId, int rating, String comment) {
        // Validate rating
        if (rating < 1 || rating > 5)
            throw new RuntimeException("Số sao phải từ 1 đến 5");

        // Validate product exists
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // Validate order belongs to user and is DELIVERED
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!order.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Bạn không có quyền đánh giá đơn hàng này");

        if (!"DELIVERED".equals(order.getStatus()))
            throw new RuntimeException("Chỉ có thể đánh giá sau khi đơn hàng đã được giao");

        // Check product is in that order
        boolean productInOrder = order.getItems().stream()
                .anyMatch(it -> it.getProduct() != null && it.getProduct().getId().equals(productId));
        if (!productInOrder)
            throw new RuntimeException("Sản phẩm này không có trong đơn hàng");

        // Check duplicate review
        if (reviewRepo.findByProductIdAndUserIdAndOrderId(productId, user.getId(), orderId).isPresent())
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi");

        // Save review
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : "");
        ProductReview saved = reviewRepo.save(review);

        // Update product rating
        updateProductRating(productId);

        return saved;
    }

    // Cập nhật rating trung bình của sản phẩm
    @Transactional
    public void updateProductRating(Long productId) {
        Double avg = reviewRepo.avgRatingByProductId(productId);
        Product product = productRepo.findById(productId).orElse(null);
        if (product != null) {
            product.setRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 5.0);
            productRepo.save(product);
        }
    }

    //Lấy danh sách đánh giá của sản phẩm có phân trang
    public Map<String, Object> getProductReviews(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductReview> pageResult = reviewRepo.findByProductIdOrderByCreatedAtDesc(productId, pageable);

        Double avg = reviewRepo.avgRatingByProductId(productId);
        long total = pageResult.getTotalElements();

        return Map.of(
            "reviews", pageResult.getContent(),
            "totalReviews", total,
            "avgRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 5.0,
            "totalPages", pageResult.getTotalPages(),
            "currentPage", page
        );
    }

    // Admin xem tất cả đánh giá
    public List<ProductReview> getAllReviews() {
        return reviewRepo.findAllByOrderByCreatedAtDesc();
    }

    //Admin xóa đánh giá spam
    @Transactional
    public void deleteReview(Long reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        Long productId = review.getProduct().getId();
        reviewRepo.delete(review);
        updateProductRating(productId);
    }

    // Lấy danh sách ID sản phẩm đã được đánh giá trong đơn hàng của user
    public List<Long> getReviewedProductIds(Long userId, Long orderId) {
        return reviewRepo.findProductIdsByUserIdAndOrderId(userId, orderId);
    }
}
