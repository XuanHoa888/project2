package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.CustomOrder;
import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.service.CustomOrderService;
import com.xhstore.cosmetic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * API cho Đặt Hàng Theo Yêu Cầu (Custom Orders).
 *
 * Khách hàng:
 *   POST   /api/custom-orders              — gửi yêu cầu mới
 *   GET    /api/custom-orders/my           — xem danh sách yêu cầu của tôi
 *   PUT    /api/custom-orders/{id}/pay     — xác nhận đã chuyển khoản
 *
 * Admin (/api/admin/custom-orders/**) — protected by AuthInterceptor:
 *   GET    /api/admin/custom-orders                        — tất cả yêu cầu
 *   GET    /api/admin/custom-orders/{id}                   — chi tiết 1 yêu cầu
 *   PUT    /api/admin/custom-orders/{id}/approve           — duyệt & báo giá
 *   PUT    /api/admin/custom-orders/{id}/reject            — từ chối
 *   PUT    /api/admin/custom-orders/{id}/confirm-payment   — xác nhận đã nhận tiền
 *   PUT    /api/admin/custom-orders/{id}/status            — cập nhật trạng thái
 */
@RestController
public class CustomOrderController {

    @Autowired CustomOrderService customOrderService;
    @Autowired UserService userService;


    /** Gửi yêu cầu đặt hàng theo mẫu. */
    @PostMapping("/api/custom-orders")
    public ResponseEntity<?> create(@RequestBody CustomOrder req, HttpServletRequest request) {
        try {
            User user = AuthController.currentUser(request, userService);
            CustomOrder created = customOrderService.createRequest(user, req);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Danh sách yêu cầu của tôi. */
    @GetMapping("/api/custom-orders/my")
    public ResponseEntity<?> myOrders(HttpServletRequest request) {
        try {
            User user = AuthController.currentUser(request, userService);
            return ResponseEntity.ok(customOrderService.getMyRequests(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /** Khách xác nhận đã chuyển khoản sau khi admin báo giá. */
    @PutMapping("/api/custom-orders/{id}/pay")
    public ResponseEntity<?> clientPay(@PathVariable Long id, HttpServletRequest request) {
        try {
            User user = AuthController.currentUser(request, userService);
            return ResponseEntity.ok(customOrderService.clientPay(id, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    /** Tất cả yêu cầu (Admin xem). */
    @GetMapping("/api/admin/custom-orders")
    public ResponseEntity<?> adminList() {
        return ResponseEntity.ok(customOrderService.getAllRequests());
    }

    /** Chi tiết 1 yêu cầu (Admin xem). */
    @GetMapping("/api/admin/custom-orders/{id}")
    public ResponseEntity<?> adminDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(customOrderService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Admin duyệt và báo giá.
     * Body: { "price": 250000, "adminNote": "Có thể làm trong 3 ngày" }
     */
    @PutMapping("/api/admin/custom-orders/{id}/approve")
    public ResponseEntity<?> adminApprove(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object priceRaw = body.get("price");
            if (priceRaw == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập giá báo cho khách"));
            BigDecimal price = new BigDecimal(priceRaw.toString());
            String note = body.getOrDefault("adminNote", "").toString();
            return ResponseEntity.ok(customOrderService.adminApproveAndQuote(id, price, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin từ chối yêu cầu.
     * Body: { "adminNote": "Không sản xuất mẫu này" }
     */
    @PutMapping("/api/admin/custom-orders/{id}/reject")
    public ResponseEntity<?> adminReject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String note = body.getOrDefault("adminNote", "");
            return ResponseEntity.ok(customOrderService.adminReject(id, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Admin xác nhận đã nhận thanh toán từ khách. */
    @PutMapping("/api/admin/custom-orders/{id}/confirm-payment")
    public ResponseEntity<?> adminConfirmPayment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(customOrderService.adminConfirmPayment(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật trạng thái chung (PAID → PROCESSING → SHIPPED → COMPLETED).
     * Body: { "status": "PROCESSING" }
     */
    @PutMapping("/api/admin/custom-orders/{id}/status")
    public ResponseEntity<?> adminUpdateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null || newStatus.trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái không được để trống"));
            return ResponseEntity.ok(customOrderService.adminUpdateStatus(id, newStatus.trim()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
