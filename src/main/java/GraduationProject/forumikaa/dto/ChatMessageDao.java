package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageDao extends JpaRepository<ChatMessage, Long> {

    // Lấy toàn bộ lịch sử chat giữa 2 user
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE (m.senderId = :user1 AND m.receiverId = :user2) " +
            "   OR (m.senderId = :user2 AND m.receiverId = :user1) " +
            "ORDER BY m.createdAt ASC")
    List<ChatMessage> getChatHistory(@Param("user1") String user1,
                                     @Param("user2") String user2);
}
