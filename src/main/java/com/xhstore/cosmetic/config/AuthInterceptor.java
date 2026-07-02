package com.xhstore.cosmetic.config;

import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Bảo vệ API:
 *   /api/admin/**         — chỉ ADMIN
 *   /api/orders/**        — phải đăng nhập
 *   /api/auth/profile     — phải đăng nhập
 *   /api/auth/password    — phải đăng nhập
 *   /api/custom-orders/** — phải đăng nhập
 *   /api/reviews/**       — phải đăng nhập
 *   /api/chat/**          — phải đăng nhập (khớp với WebConfig addPathPatterns)
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();
        HttpSession session = req.getSession(false);
        Long userId = (session != null) ? (Long) session.getAttribute("userId") : null;

        // Bảo vệ /api/admin
        if (uri.startsWith("/api/admin")) {
            if (userId == null) return deny(res, 401, "Chưa đăng nhập");

            // Lấy user mới nhất từ DB để kiểm tra role chính xác
            try {
                User user = userService.getById(userId);
                if (!"ADMIN".equals(user.getRole())) return deny(res, 403, "Không có quyền Admin");
            } catch (Exception e) {
                return deny(res, 401, "Phiên đăng nhập không hợp lệ");
            }
        }

        // Bảo vệ các route cần đăng nhập
        if (uri.startsWith("/api/orders") || uri.startsWith("/api/auth/profile")
                || uri.startsWith("/api/auth/password")
                || uri.startsWith("/api/custom-orders")
                || uri.startsWith("/api/reviews")
                || uri.startsWith("/api/chat")) {   // /api/chat/history — khớp với WebConfig
            if (userId == null) return deny(res, 401, "Chưa đăng nhập");
        }

        return true;
    }

    private boolean deny(HttpServletResponse res, int status, String msg) throws Exception {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        // Dùng chuỗi an toàn — escape ký tự đặc biệt để tránh JSON injection
        String safe = msg.replace("\\", "\\\\").replace("\"", "\\\"");
        res.getWriter().write("{\"error\":\"" + safe + "\"}");
        return false;
    }
}
