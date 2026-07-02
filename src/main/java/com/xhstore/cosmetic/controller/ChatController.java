package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.ChatMessage;
import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.repository.ChatMessageRepository;
import com.xhstore.cosmetic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserService userService;

    /**
     * Lấy lịch sử chat của phòng chat.
     * Client chỉ xem được phòng của chính mình (chatRoomId = clientId).
     * Admin xem được tất cả các phòng chat.
     */
    @GetMapping("/api/chat/history")
    public ResponseEntity<?> getHistory(@RequestParam Long chatRoomId, HttpServletRequest request) {
        try {
            User user = AuthController.currentUser(request, userService);
            boolean isAdmin = "ADMIN".equals(user.getRole());
            
            if (!isAdmin && !user.getId().equals(chatRoomId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem phòng chat này"));
            }

            List<ChatMessage> history = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    //Lấy danh sách các phòng chat đang hoạt động admin.
    @GetMapping("/api/admin/chat/rooms")
    public ResponseEntity<?> getActiveRooms(HttpServletRequest request) {
        try {
            User user = AuthController.currentUser(request, userService);
            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of("error", "Chỉ Admin được xem danh sách phòng chat"));
            }

            List<Map<String, Object>> rooms = chatMessageRepository.findActiveChatRooms();
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
