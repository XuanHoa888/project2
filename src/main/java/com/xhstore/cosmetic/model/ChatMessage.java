package com.xhstore.cosmetic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", nullable = false, length = 50)
    private String senderName;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId; // ID của client, đại diện cho ID phòng chat

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = false; // true nếu gửi từ Admin, false nếu gửi từ Client

    public ChatMessage() {}

    public ChatMessage(Long id, Long senderId, String senderName, Long chatRoomId, String content, LocalDateTime createdAt, Boolean isAdmin) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.chatRoomId = chatRoomId;
        this.content = content;
        this.createdAt = createdAt;
        this.isAdmin = isAdmin;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(Long chatRoomId) { this.chatRoomId = chatRoomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
}
