package GraduationProject.forumikaa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String roomName;

    @Column(name = "is_group", nullable = false)
    private boolean isGroup = false;

    @Column(name = "room_avatar")
    private String roomAvatar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ChatRoomMember> members = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ChatMessage> messages = new HashSet<>();
    
    // Helper methods để tính toán thông tin cuối cùng
    public LocalDateTime getLastMessageAt() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.stream()
                .map(ChatMessage::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
    
    public String getLastMessageContent() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.stream()
                .max((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(ChatMessage::getContent)
                .orElse(null);
    }
    
    public Long getLastMessageSenderId() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.stream()
                .max((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(ChatMessage::getSenderId)
                .orElse(null);
    }
    
    public User getLastMessageSender() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.stream()
                .max((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(ChatMessage::getSender)
                .orElse(null);
    }
} 