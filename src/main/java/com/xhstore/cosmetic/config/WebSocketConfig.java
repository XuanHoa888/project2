package com.xhstore.cosmetic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Đăng ký endpoint WebSocket cho Chat và hỗ trợ CORS mọi nguồn
        // HttpSessionHandshakeInterceptor copy toàn bộ HttpSession attributes (userId, role)
        // vào WebSocketSession.getAttributes() trong pha HTTP Upgrade — client không thể giả mạo
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
