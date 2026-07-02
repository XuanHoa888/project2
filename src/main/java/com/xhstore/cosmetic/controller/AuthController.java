package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.register(user)); // password bị @JsonIgnore
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            User user = userService.login(body.get("username"), body.get("password"));

            // Chỉ lưu id vào session tránh object cũ sau khi role bị đổi
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setMaxInactiveInterval(60 * 60 * 8); // 8 giờ

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Luôn lấy user mới nhất từ DB
    @GetMapping("/profile")
    public ResponseEntity<?> profile(HttpServletRequest req) {
        try {
            User user = currentUser(req, userService);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    //Cập nhật thông tin hồ sơ cá nhân
  
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            User user = currentUser(req, userService);
            User updated = userService.updateProfile(user.getId(),
                    body.get("email"), body.get("phone"), body.get("address"));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

 // đổi mật khẩu
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            User user = currentUser(req, userService);
            String oldPwd = body.get("oldPassword");
            String newPwd = body.get("newPassword");
            if (oldPwd == null || oldPwd.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập mật khẩu hiện tại"));
            if (newPwd == null || newPwd.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập mật khẩu mới"));
            if (newPwd.length() < 6)
                return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu mới phải có ít nhất 6 ký tự"));
            userService.changePassword(user.getId(), oldPwd, newPwd);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy user hiện tại fresh từ DB.
     * Dùng static để các controller khác tái sử dụng mà không cần inject thêm.
     */
    public static User currentUser(HttpServletRequest req, UserService userService) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null)
            throw new RuntimeException("Chưa đăng nhập");
        Long id = (Long) session.getAttribute("userId");
        return userService.getById(id); // luôn fresh từ DB
    }
}
