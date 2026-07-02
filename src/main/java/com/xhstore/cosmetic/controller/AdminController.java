package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.*;
import com.xhstore.cosmetic.repository.BoothConfigRepository;
import com.xhstore.cosmetic.repository.CategoryRepository;
import com.xhstore.cosmetic.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Tất cả endpoint /api/admin/** được AuthInterceptor bảo vệ chỉ ADMIN được vào.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired AdminService adminService;
    @Autowired ProductService productService;
    @Autowired OrderService orderService;
    @Autowired VoucherService voucherService;
    @Autowired UserService userService;
    @Autowired BoothConfigRepository boothConfigRepository;
    @Autowired CategoryRepository categoryRepository;


    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }


    /** Admin xem toàn bộ sản phẩm  */
    @GetMapping("/products")
    public ResponseEntity<List<Product>> products() {
        return ResponseEntity.ok(productService.getAll());
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Product sp) {
        try {
            return ResponseEntity.ok(productService.save(sp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product sp) {
        try {
            return ResponseEntity.ok(productService.update(id, sp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            boolean hardDeleted = productService.delete(id);
            if (hardDeleted) {
                return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm vĩnh viễn thành công."));
            } else {
                return ResponseEntity.ok(Map.of("message", "Đã ẩn sản phẩm (không thể xóa vĩnh viễn vì sản phẩm này đã có lịch sử đặt hàng)."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/orders")
    public ResponseEntity<List<Order>> orders() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái không được để trống"));
            return ResponseEntity.ok(orderService.updateStatus(id, status.trim()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/refunds")
    public ResponseEntity<List<RefundRequest>> refunds() {
        return ResponseEntity.ok(orderService.getAllRefunds());
    }

    @PutMapping("/refunds/{id}")
    public ResponseEntity<?> handleRefund(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean approve = body.get("approve");
            if (approve == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu tham số 'approve'"));
            return ResponseEntity.ok(orderService.handleRefund(id, approve));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/vouchers")
    public ResponseEntity<List<Voucher>> vouchers() {
        return ResponseEntity.ok(voucherService.getAll());
    }

    @PostMapping("/vouchers")
    public ResponseEntity<?> createVoucher(@RequestBody Voucher v) {
        try {
            if (v.getCode() == null || v.getCode().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Mã voucher không được để trống"));
            if (!"PERCENT".equals(v.getDiscountType()) && !"FIXED".equals(v.getDiscountType()))
                return ResponseEntity.badRequest().body(Map.of("error", "discountType phải là PERCENT hoặc FIXED"));
            if (v.getDiscountValue() == null || v.getDiscountValue().signum() <= 0)
                return ResponseEntity.badRequest().body(Map.of("error", "Giá trị giảm phải lớn hơn 0"));
            return ResponseEntity.ok(voucherService.save(v));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long id) {
        try {
            voucherService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Xóa voucher thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(adminService.getAllUsers()); // password bị @JsonIgnore
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String role = body.get("role");
            if (role == null || (!"ADMIN".equals(role) && !"CLIENT".equals(role)))
                return ResponseEntity.badRequest().body(Map.of("error", "Role phải là ADMIN hoặc CLIENT"));
            return ResponseEntity.ok(userService.updateRole(id, role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/booths")
    public ResponseEntity<List<BoothConfig>> getBooths() {
        return ResponseEntity.ok(boothConfigRepository.findAll());
    }

    @PutMapping("/booths/{id}")
    public ResponseEntity<?> updateBooth(@PathVariable Long id, @RequestBody BoothConfig data) {
        try {
            BoothConfig booth = boothConfigRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Gian hàng không tồn tại (id=" + id + ")"));
            booth.setName(data.getName());
            booth.setDescription(data.getDescription());
            booth.setSaleTag(data.getSaleTag());
            booth.setBannerSubtitle(data.getBannerSubtitle());
            booth.setBannerTitle(data.getBannerTitle());
            booth.setBannerImageUrl(data.getBannerImageUrl());
            
            // Áp discount giá sản phẩm theo booth xử lý tại ProductService với @Transactional
            productService.applyBoothDiscount(booth.getBoothKey(), data.getSaleTag());

            return ResponseEntity.ok(boothConfigRepository.save(booth));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tên danh mục không được để trống"));
            }
            if (categoryRepository.findByName(category.getName()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh mục đã tồn tại"));
            }
            return ResponseEntity.ok(categoryRepository.save(category));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category data) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại (id=" + id + ")"));
            if (data.getName() == null || data.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tên danh mục không được để trống"));
            }
            category.setName(data.getName());
            category.setIcon(data.getIcon());
            return ResponseEntity.ok(categoryRepository.save(category));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            if (!categoryRepository.existsById(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh mục không tồn tại"));
            }
            categoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa danh mục thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể xóa danh mục này (có thể đang được sử dụng bởi sản phẩm)"));
        }
    }

// Doanh thu
    @GetMapping("/reports")
    public ResponseEntity<?> reports(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            java.time.LocalDate fromDate = java.time.LocalDate.parse(from);
            java.time.LocalDate toDate = java.time.LocalDate.parse(to);
            java.time.LocalDateTime fromDT = fromDate.atStartOfDay();
            java.time.LocalDateTime toDT = toDate.atTime(23, 59, 59);

            List<Order> allOrders = orderService.getAll();
            List<Order> filtered = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(fromDT) && !o.getCreatedAt().isAfter(toDT))
                .toList();

            java.math.BigDecimal revenue = filtered.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()))
                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            long refundedCount = filtered.stream().filter(o -> "REFUNDED".equals(o.getStatus())).count();
            int refundRate = filtered.isEmpty() ? 0 : (int) Math.round(refundedCount * 100.0 / filtered.size());

            // Top products chỉ tính từ đơn DELIVERED
            java.util.Map<String, java.util.Map<String, Object>> prodMap = new java.util.LinkedHashMap<>();
            for (Order o : filtered) {
                if (!"DELIVERED".equals(o.getStatus())) continue; // bỏ qua đơn chưa giao/đã huỷ/đã hoàn
                if (o.getItems() != null) {
                    for (OrderItem it : o.getItems()) {
                        String key = it.getProductName() != null ? it.getProductName() : "?";
                        prodMap.computeIfAbsent(key, k -> {
                            java.util.Map<String, Object> m = new java.util.HashMap<>();
                            m.put("name", k);
                            m.put("qty", 0);
                            m.put("revenue", java.math.BigDecimal.ZERO);
                            return m;
                        });
                        java.util.Map<String, Object> entry = prodMap.get(key);
                        entry.put("qty", (int) entry.get("qty") + (it.getQuantity() != null ? it.getQuantity() : 0));
                        java.math.BigDecimal itemRev = it.getProductPrice() != null
                            ? it.getProductPrice().multiply(java.math.BigDecimal.valueOf(it.getQuantity() != null ? it.getQuantity() : 0))
                            : java.math.BigDecimal.ZERO;
                        entry.put("revenue", ((java.math.BigDecimal) entry.get("revenue")).add(itemRev));
                    }
                }
            }
            List<java.util.Map<String, Object>> topProducts = prodMap.values().stream()
                .sorted((a, b) -> Integer.compare((int) b.get("qty"), (int) a.get("qty")))
                .limit(10)
                .toList();

            // Daily revenue for chart
            java.util.Map<String, java.math.BigDecimal> dailyRevenue = new java.util.LinkedHashMap<>();
            java.time.LocalDate current = fromDate;
            while (!current.isAfter(toDate)) {
                dailyRevenue.put(current.toString(), java.math.BigDecimal.ZERO);
                current = current.plusDays(1);
            }
            for (Order o : filtered) {
                if ("DELIVERED".equals(o.getStatus())) {
                    String dayKey = o.getCreatedAt().toLocalDate().toString();
                    if (dailyRevenue.containsKey(dayKey)) {
                        dailyRevenue.put(dayKey, dailyRevenue.get(dayKey).add(
                            o.getFinalAmount() != null ? o.getFinalAmount() : java.math.BigDecimal.ZERO));
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "totalOrders", filtered.size(),
                "revenue", revenue,
                "refundedCount", refundedCount,
                "refundRate", refundRate,
                "topProducts", topProducts,
                "dailyRevenue", dailyRevenue,
                "orders", filtered
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
