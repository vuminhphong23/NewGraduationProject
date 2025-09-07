package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    
    /**
     * Tìm tất cả attachment theo room ID
     */
    List<ChatAttachment> findByRoomIdOrderByUploadedAtDesc(Long roomId);
    
    /**
     * Tìm attachment theo message ID
     */
    List<ChatAttachment> findByMessageIdOrderByUploadedAtAsc(Long messageId);
    
    /**
     * Tìm attachment theo room ID và attachment type
     */
    List<ChatAttachment> findByRoomIdAndAttachmentTypeOrderByUploadedAtDesc(
            Long roomId, ChatAttachment.AttachmentType attachmentType);
    
    /**
     * Tìm attachment theo user ID và room ID
     */
    List<ChatAttachment> findByUploadedByIdAndRoomIdOrderByUploadedAtDesc(Long userId, Long roomId);
    
    /**
     * Tìm attachment theo cloudinary public ID
     */
    Optional<ChatAttachment> findByCloudinaryPublicId(String cloudinaryPublicId);
    
    /**
     * Đếm số lượng attachment theo room ID
     */
    long countByRoomId(Long roomId);
    
    /**
     * Đếm số lượng attachment theo room ID và type
     */
    long countByRoomIdAndAttachmentType(Long roomId, ChatAttachment.AttachmentType attachmentType);
    
    /**
     * Tìm tất cả ảnh trong room
     */
    @Query("SELECT ca FROM ChatAttachment ca WHERE ca.room.id = :roomId AND ca.attachmentType = 'IMAGE' ORDER BY ca.uploadedAt DESC")
    List<ChatAttachment> findImagesByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Tìm tất cả video trong room
     */
    @Query("SELECT ca FROM ChatAttachment ca WHERE ca.room.id = :roomId AND ca.attachmentType = 'VIDEO' ORDER BY ca.uploadedAt DESC")
    List<ChatAttachment> findVideosByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Tìm tất cả document trong room
     */
    @Query("SELECT ca FROM ChatAttachment ca WHERE ca.room.id = :roomId AND ca.attachmentType = 'DOCUMENT' ORDER BY ca.uploadedAt DESC")
    List<ChatAttachment> findDocumentsByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Tìm tất cả audio trong room
     */
    @Query("SELECT ca FROM ChatAttachment ca WHERE ca.room.id = :roomId AND ca.attachmentType = 'AUDIO' ORDER BY ca.uploadedAt DESC")
    List<ChatAttachment> findAudiosByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Tìm attachment theo file name trong room
     */
    @Query("SELECT ca FROM ChatAttachment ca WHERE ca.room.id = :roomId AND ca.fileName = :fileName")
    Optional<ChatAttachment> findByRoomIdAndFileName(@Param("roomId") Long roomId, @Param("fileName") String fileName);
    
    /**
     * Xóa tất cả attachment của một message
     */
    @Modifying
    @Query("DELETE FROM ChatAttachment ca WHERE ca.message.id = :messageId")
    void deleteByMessageId(@Param("messageId") Long messageId);
    
    /**
     * Xóa tất cả attachment của một room
     */
    @Modifying
    @Query("DELETE FROM ChatAttachment ca WHERE ca.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
