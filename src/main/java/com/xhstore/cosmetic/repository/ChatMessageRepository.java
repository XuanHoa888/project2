package com.xhstore.cosmetic.repository;

import com.xhstore.cosmetic.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    /**
     * Lấy danh sách các phòng chat hiện có cho Admin, sắp xếp theo tin nhắn mới nhất.
     * Trả về danh sách chứa Map có: chatRoomId, clientName, lastMessage, updatedAt.
     */
    @Query("SELECT m.chatRoomId AS chatRoomId, " +
           "(SELECT u.username FROM User u WHERE u.id = m.chatRoomId) AS clientName, " +
           "m.content AS lastMessage, " +
           "m.createdAt AS updatedAt " +
           "FROM ChatMessage m " +
           "WHERE m.id IN (SELECT MAX(msg.id) FROM ChatMessage msg GROUP BY msg.chatRoomId) " +
           "ORDER BY m.createdAt DESC")
    List<Map<String, Object>> findActiveChatRooms();
}
