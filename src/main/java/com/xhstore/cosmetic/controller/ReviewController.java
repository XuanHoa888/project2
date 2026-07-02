package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.ProductReview;
import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.service.ReviewService;
import com.xhstore.cosmetic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API Đánh Giá Sản Phẩm.
 *
 * Khách hàng:
 *   POST  /api/reviews gửi đánh giá mới
 *   GET   /api/products/{id}/reviews xem đánh giá của sản phẩm
 *
 * Admin:
 *   GET   /api/admin/reviews xem tất cả đánh giá
 *   DELETE /api/admin/reviews/{id} xóa đánh giá
 */
@RestController
public class ReviewController {

    @Autowired ReviewService reviewService;
    @Autowired UserService userService;


    /**
     * Gửi đánh giá mới.
     * Body: { "productId": 1, "orderId": 5, "rating": 5, "comment": "Rất tốt!" }
     */
    @PostMapping("/api/reviews")
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        try {
            User user = AuthController.currentUser(req, userService);
            Long productId = Long.valueOf(body.get("productId").toString());
            Long orderId   = Long.valueOf(body.get("orderId").toString());
            int  rating    = Integer.parseInt(body.get("rating").toString());
            String comment = body.getOrDefault("comment", "").toString();

            ProductReview review = reviewService.createReview(user, productId, orderId, rating, comment);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xem đánh giá của sản phẩm (public).
     * GET /api/products/{id}/reviews?page=0&size=5
     */
    @GetMapping("/api/products/{id}/reviews")
    public ResponseEntity<?> getProductReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        try {
            return ResponseEntity.ok(reviewService.getProductReviews(id, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách ID các sản phẩm đã được đánh giá bởi user trong đơn hàng cụ thể.
     * GET /api/reviews/my-reviewed-products?orderId=...
     */
    @GetMapping("/api/reviews/my-reviewed-products")
    public ResponseEntity<?> getMyReviewedProducts(@RequestParam Long orderId, HttpServletRequest req) {
        try {
            User user = AuthController.currentUser(req, userService);
            return ResponseEntity.ok(reviewService.getReviewedProductIds(user.getId(), orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    /** Admin xem tất cả đánh giá. */
    @GetMapping("/api/admin/reviews")
    public ResponseEntity<?> adminListReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    /** Admin xóa đánh giá spam/không phù hợp. */
    @DeleteMapping("/api/admin/reviews/{id}")
    public ResponseEntity<?> adminDeleteReview(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa đánh giá"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
