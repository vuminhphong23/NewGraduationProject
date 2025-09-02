package GraduationProject.forumikaa.handler.chat;
import GraduationProject.forumikaa.entity.ChatMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatBroadcaster {

    private final RedisTemplate<String, Object> redisTemplate;

    public ChatBroadcaster(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Publish
    public void publish(ChatMessage msg) {
        String topic = getTopic(msg.getSenderId(), msg.getReceiverId());
        redisTemplate.convertAndSend(topic, msg);
    }

    private String getTopic(Long user1, Long user2) {
        return user1.compareTo(user2) < 0
                ? "chat_" + user1 + "_" + user2
                : "chat_" + user2 + "_" + user1;
    }
}
