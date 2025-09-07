package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomDao extends JpaRepository<ChatRoom, Long> {
    
    /**
     * Tìm chat room 1-1 giữa 2 user
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "WHERE cr.isGroup = false " +
           "AND cr.id IN (SELECT m1.room.id FROM ChatRoomMember m1 WHERE m1.user.id = :user1Id) " +
           "AND cr.id IN (SELECT m2.room.id FROM ChatRoomMember m2 WHERE m2.user.id = :user2Id)")
    Optional<ChatRoom> findPrivateChatBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
    
    /**
     * Lấy danh sách room của user, sắp xếp theo tin nhắn cuối
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "JOIN cr.members m ON m.user.id = :userId " +
           "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUserIdOrderByLastMessageDesc(@Param("userId") Long userId);
    
    /**
     * Tìm room theo tên (cho group chat)
     */
    List<ChatRoom> findByRoomNameContainingIgnoreCase(String roomName);
    
    /**
     * Tìm room được tạo bởi user
     */
    List<ChatRoom> findByCreatedById(Long createdById);
}
