package GraduationProject.forumikaa.config;

import GraduationProject.forumikaa.handler.chat.ChatWebSocketHandler;
import GraduationProject.forumikaa.handler.notification.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;
    private final ChatWebSocketHandler chatHandler;

    public WebSocketConfig(NotificationWebSocketHandler notificationHandler, ChatWebSocketHandler chatHandler) {
        this.notificationHandler = notificationHandler;
        this.chatHandler = chatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");

        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}
