package GraduationProject.forumikaa.handler.chat;

import GraduationProject.forumikaa.entity.ChatMessage;
import GraduationProject.forumikaa.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatBroadcaster broadcaster;
    private final ChatService chatService;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ChatWebSocketHandler(ChatBroadcaster broadcaster, ChatService chatService) {
        this.broadcaster = broadcaster;
        this.chatService = chatService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (payload == null || payload.trim().isEmpty()) return;

        ChatMessage chatMsg = mapper.readValue(payload, ChatMessage.class);

        // lưu DB
        chatService.saveMessage(chatMsg);

        // publish ra redis
        broadcaster.publish(chatMsg);
    }
}
