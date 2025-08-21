package GraduationProject.forumikaa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "likeable_id", "likeable_type"})
})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "likeable_id", nullable = false)
    private Long likeableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "likeable_type", nullable = false)
    private LikeableType likeableType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isPostLike() {
        return likeableType == LikeableType.POST;
    }

    public boolean isCommentLike() {
        return likeableType == LikeableType.COMMENT;
    }
} 