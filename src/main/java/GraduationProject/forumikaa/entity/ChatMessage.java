package GraduationProject.forumikaa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Data
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column
    private String senderId;
    @Column
    private String receiverId;
    @Column
    private String content;
    @Column
    private Instant createdAt;
}
