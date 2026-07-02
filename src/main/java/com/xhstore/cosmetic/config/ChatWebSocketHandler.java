package com.xhstore.cosmetic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xhstore.cosmetic.model.ChatMessage;
import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.repository.ChatMessageRepository;
import com.xhstore.cosmetic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // mỗi tab mở = 1 session riêng, đóng tab chỉ xóa session đó, không ảnh hưởng tab còn lại
    private static final Map<Long, Set<WebSocketSession>> clientSessions = new ConcurrentHashMap<>();
    private static final Map<Long, Set<WebSocketSession>> adminSessions  = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userIdObj = session.getAttributes().get("userId");

        if (userIdObj == null) {
            // Chưa đăng nhập — đóng kết nối ngay, không cho vào
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId = ((Number) userIdObj).longValue();

        // Tra DB để lấy role thực 
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String role = user.getRole(); 
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("role", role);

        if ("ADMIN".equals(role)) {
            adminSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        } else {
            clientSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);

        Long userId = (Long) session.getAttributes().get("userId");
        String role  = (String) session.getAttributes().get("role");

        if (userId == null || role == null) return;

        Long   chatRoomId = Long.valueOf(data.get("chatRoomId").toString());
        String content    = (String) data.get("content");
        String senderName = (String) data.get("senderName");
        boolean isAdmin   = "ADMIN".equals(role);

        // Lưu DB
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(userId);
        msg.setSenderName(senderName);
        msg.setChatRoomId(chatRoomId);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setIsAdmin(isAdmin);

        ChatMessage saved = chatMessageRepository.save(msg);
        String jsonMsg    = objectMapper.writeValueAsString(saved);
        TextMessage outbound = new TextMessage(jsonMsg);

        // Phát tán tin nhắn
        if (isAdmin) {
            // Admin gửi -> gửi tới tất cả tab/session của client đó
            sendToUser(clientSessions, chatRoomId, outbound);
            // Đồng bộ sang tất cả tab Admin khác đang mở
            broadcastToAdmins(outbound);
        } else {
            // Client gửi -> gửi tới tất cả Admin đang online
            broadcastToAdmins(outbound);
            // Echo lại cho chính client (tất cả tab đang mở của user đó)
            sendToUser(clientSessions, userId, outbound);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long   userId = (Long)   session.getAttributes().get("userId");
        String role   = (String) session.getAttributes().get("role");

        if (userId == null) return;

        // Xóa đúng session này khỏi Set — không ảnh hưởng các tab còn lại của cùng user
        Map<Long, Set<WebSocketSession>> map = "ADMIN".equals(role) ? adminSessions : clientSessions;
        Set<WebSocketSession> sessions = map.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            // Dọn entry rỗng để tránh memory leak
            if (sessions.isEmpty()) {
                map.remove(userId);
            }
        }
    }

    /**
     * Gửi tin nhắn tới tất cả session của một userId cụ thể.
     */
    private void sendToUser(Map<Long, Set<WebSocketSession>> map, Long userId, TextMessage msg) {
        Set<WebSocketSession> sessions = map.get(userId);
        if (sessions == null) return;
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try { s.sendMessage(msg); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Broadcast tin nhắn tới tất cả Admin đang online
     */
    private void broadcastToAdmins(TextMessage msg) {
        for (Set<WebSocketSession> sessions : adminSessions.values()) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    try { s.sendMessage(msg); } catch (IOException ignored) {}
                }
            }
        }
    }
}
