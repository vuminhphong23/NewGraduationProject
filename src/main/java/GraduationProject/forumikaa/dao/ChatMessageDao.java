package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageDao extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Lấy tin nhắn của room, sắp xếp theo thời gian tạo (mới nhất trước)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room.id = :roomId ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId, Pageable pageable);
    
    /**
     * Lấy tin nhắn của room, sắp xếp theo thời gian tạo (cũ nhất trước)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room.id = :roomId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);
    
    /**
     * Đếm số tin nhắn chưa đọc của user trong room
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.room.id = :roomId " +
           "AND cm.sender.id != :userId " +
           "AND cm.isRead = false")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * Lấy tin nhắn chưa đọc của user trong room
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.room.id = :roomId " +
           "AND cm.sender.id != :userId " +
           "AND cm.isRead = false " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * Lấy tin nhắn cuối cùng của room
     */
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.room.id = :roomId " +
           "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findLastMessageByRoomId(@Param("roomId") Long roomId, Pageable pageable);
    
    /**
     * Đếm số tin nhắn chưa đọc của user trong room (alias method)
     */
    default int countUnreadMessagesByRoomIdAndUserId(Long roomId, Long userId) {
        return (int) countUnreadMessages(roomId, userId);
    }
    
    /**
     * Xóa tất cả tin nhắn trong room
     */
    void deleteByRoomId(Long roomId);
}
