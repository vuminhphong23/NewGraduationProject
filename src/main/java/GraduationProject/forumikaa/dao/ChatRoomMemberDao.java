package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberDao extends JpaRepository<ChatRoomMember, Long> {
    
    /**
     * Tìm thành viên theo room và user
     */
    @Query("SELECT c FROM ChatRoomMember c WHERE c.room.id = :roomId AND c.user.id = :userId")
    Optional<ChatRoomMember> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * Kiểm tra user có phải thành viên của room không
     */
    @Query("SELECT COUNT(c) > 0 FROM ChatRoomMember c WHERE c.room.id = :roomId AND c.user.id = :userId")
    boolean existsByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * Lấy danh sách thành viên của room
     */
    @Query("SELECT c FROM ChatRoomMember c WHERE c.room.id = :roomId")
    List<ChatRoomMember> findByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Lấy danh sách room mà user là thành viên
     */
    @Query("SELECT c FROM ChatRoomMember c WHERE c.user.id = :userId")
    List<ChatRoomMember> findByUserId(@Param("userId") Long userId);
    
    /**
     * Đếm số thành viên của room
     */
    @Query("SELECT COUNT(c) FROM ChatRoomMember c WHERE c.room.id = :roomId")
    long countByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Xóa tất cả thành viên của room
     */
    @Query("DELETE FROM ChatRoomMember c WHERE c.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
    
    /**
     * Xóa thành viên khỏi room
     */
    @Query("DELETE FROM ChatRoomMember c WHERE c.room.id = :roomId AND c.user.id = :userId")
    void deleteByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
