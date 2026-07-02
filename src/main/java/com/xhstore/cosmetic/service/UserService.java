package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    /**
     * Đăng ký validate đầu vào, mã hóa mật khẩu, ép role CLIENT.
     */
    public User register(User user) {
        String name = user.getUsername();
        if (name == null || name.trim().isEmpty())
            throw new RuntimeException("Tên đăng nhập không được để trống");
        if (name.length() < 4 || name.length() > 50)
            throw new RuntimeException("Tên đăng nhập phải từ 4 đến 50 ký tự");
        if (!name.matches("^[a-zA-Z0-9_]+$"))
            throw new RuntimeException("Tên đăng nhập chỉ dùng chữ, số và dấu _");
        if (repo.findByUsername(name).isPresent())
            throw new RuntimeException("Tên đăng nhập đã tồn tại");

        String pwd = user.getPassword();
        if (pwd == null || pwd.length() < 6)
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
        if (pwd.length() > 128)
            throw new RuntimeException("Mật khẩu không được vượt quá 128 ký tự");

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (!user.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                throw new RuntimeException("Email không hợp lệ");
        }

        // Mã hóa BCrypt, ép role CLIENT không cho tự set ADMIN
        user.setPassword(BCrypt.hashpw(pwd, BCrypt.gensalt()));
        user.setRole("CLIENT");
        return repo.save(user);
    }

    /**
     * Đăng nhập xác thực BCrypt, thông báo lỗi chung để tránh lộ thông tin.
     */
    public User login(String name, String pwd) {
        if (name == null || pwd == null)
            throw new RuntimeException("Tên đăng nhập và mật khẩu không được để trống");

        User user = repo.findByUsername(name.trim())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!BCrypt.checkpw(pwd, user.getPassword()))
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");

        return user;
    }

    public User getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    public List<User> getAll() {
        return repo.findAll();
    }

    /** Admin đổi role (CLIENT ↔ ADMIN). */
    public User updateRole(Long id, String role) {
        if (!"CLIENT".equals(role) && !"ADMIN".equals(role))
            throw new RuntimeException("Role không hợp lệ. Chỉ chấp nhận: CLIENT, ADMIN");
        User user = getById(id);
        user.setRole(role);
        return repo.save(user);
    }

    /**
     * Khách hàng tự cập nhật thông tin cá nhân.
     * Chỉ cho phép sửa email, phone, address không cho đổi role hay username.
     */
    public User updateProfile(Long id, String email, String phone, String address) {
        User user = getById(id);
        if (email != null && !email.isEmpty()) {
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                throw new RuntimeException("Email không hợp lệ");
            user.setEmail(email.trim());
        }
        if (phone != null) user.setPhone(phone.trim());
        if (address != null) user.setAddress(address.trim());
        return repo.save(user);
    }

    /**
     * Đổi mật khẩu yêu cầu mật khẩu cũ đúng.
     */
    public User changePassword(Long id, String oldPassword, String newPassword) {
        User user = getById(id);
        if (!BCrypt.checkpw(oldPassword, user.getPassword()))
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        if (newPassword == null || newPassword.length() < 6)
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        if (newPassword.length() > 128)
            throw new RuntimeException("Mật khẩu mới không được vượt quá 128 ký tự");
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        return repo.save(user);
    }
}
