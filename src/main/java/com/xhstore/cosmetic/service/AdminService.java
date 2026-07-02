package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.*;
import com.xhstore.cosmetic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ dành riêng cho Admin thống kê dashboard và quản lý users.
 */
@Service
public class AdminService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private UserRepository userRepo;

    /** Thống kê tổng quan dashboard. */
    public Map<String, Object> getDashboardStats() {
        List<Order> all = orderRepo.findAllByOrderByCreatedAtDesc();

        long totalOrders = all.size();

        // Doanh thu: chỉ tính đơn đã giao thành công (DELIVERED)
        // Đơn REFUNDED đã hoàn tiền cho khách — không được tính vào doanh thu
        BigDecimal revenue = all.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()))
                .map(Order::getFinalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalProducts = productRepo.count();
        long totalUsers    = userRepo.countByRole("CLIENT");
        long pendingRefunds = all.stream()
                .filter(o -> "REFUND_REQUESTED".equals(o.getStatus()))
                .count();

        List<Order> recent = all.stream().limit(5).toList();

        return Map.of(
            "totalOrders",   totalOrders,
            "revenue",       revenue,
            "totalRevenue",  revenue,
            "totalProducts", totalProducts,
            "totalUsers",    totalUsers,
            "pendingRefunds",pendingRefunds,
            "recent",        recent,
            "recentOrders",  recent
        );
    }

    /** Danh sách tất cả users để admin quản lý (password bị @JsonIgnore). */
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }
}
