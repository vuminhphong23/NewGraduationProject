package GraduationProject.forumikaa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_attachments")
public class ChatAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false)
    private AttachmentType attachmentType;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "cloudinary_url", length = 500)
    private String cloudinaryUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "is_cloud_storage", nullable = false)
    @Builder.Default
    private boolean isCloudStorage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @JsonIgnore
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnore
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    @JsonIgnore
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public enum AttachmentType {
        IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
    }

    /**
     * Kiểm tra xem file có phải là ảnh không
     */
    public boolean isImage() {
        return attachmentType == AttachmentType.IMAGE;
    }

    /**
     * Kiểm tra xem file có phải là video không
     */
    public boolean isVideo() {
        return attachmentType == AttachmentType.VIDEO;
    }

    /**
     * Kiểm tra xem file có phải là audio không
     */
    public boolean isAudio() {
        return attachmentType == AttachmentType.AUDIO;
    }

    /**
     * Kiểm tra xem file có phải là document không
     */
    public boolean isDocument() {
        return attachmentType == AttachmentType.DOCUMENT;
    }

    /**
     * Lấy URL hiển thị (ưu tiên cloudinary nếu có)
     */
    public String getDisplayUrl() {
        return isCloudStorage && cloudinaryUrl != null ? cloudinaryUrl : filePath;
    }

    /**
     * Lấy URL thumbnail (ưu tiên cloudinary thumbnail nếu có)
     */
    public String getDisplayThumbnailUrl() {
        if (isCloudStorage && thumbnailUrl != null) {
            return thumbnailUrl;
        }
        // Nếu là ảnh và không có thumbnail, dùng chính URL đó
        if (isImage()) {
            return getDisplayUrl();
        }
        return null;
    }
}
