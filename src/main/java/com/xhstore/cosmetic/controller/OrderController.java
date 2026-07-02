package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.Order;
import com.xhstore.cosmetic.model.RefundRequest;
import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.service.OrderService;
import com.xhstore.cosmetic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired OrderService orderService;
    @Autowired UserService userService;

    /** Đặt đơn hàng mới yêu cầu đăng nhập. */
    @PostMapping
    public ResponseEntity<?> place(@RequestBody OrderService.OrderRequest body, HttpServletRequest req) {
        try {
            User user = AuthController.currentUser(req, userService);
            Order order = orderService.placeOrder(user, body);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            if ("Chưa đăng nhập".equals(e.getMessage()))
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Danh sách đơn hàng của tôi. */
    @GetMapping("/my")
    public ResponseEntity<?> myOrders(HttpServletRequest req) {
        try {
            User user = AuthController.currentUser(req, userService);
            return ResponseEntity.ok(orderService.getMyOrders(user.getId()));
        } catch (Exception e) {
            if ("Chưa đăng nhập".equals(e.getMessage()))
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Chi tiết đơn chỉ chủ đơn hoặc ADMIN được xem. */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpServletRequest req) {
        try {
            User user = AuthController.currentUser(req, userService);
            Order order = orderService.getById(id);

            boolean owner = order.getUser().getId().equals(user.getId());
            boolean admin = "ADMIN".equals(user.getRole());
            if (!owner && !admin)
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem đơn hàng này"));

            return ResponseEntity.ok(order);
        } catch (Exception e) {
            if ("Chưa đăng nhập".equals(e.getMessage()))
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /** Yêu cầu hoàn hàng chỉ chủ đơn, chỉ khi đơn đã DELIVERED. */
    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refund(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        try {
            User user = AuthController.currentUser(req, userService);
            String reason = body.get("reason");
            if (reason == null || reason.trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn lý do hoàn hàng"));

            RefundRequest result = orderService.requestRefund(id, user.getId(), reason, body.get("description"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            if ("Chưa đăng nhập".equals(e.getMessage()))
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
