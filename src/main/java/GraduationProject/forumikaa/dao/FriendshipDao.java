package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.FriendshipStatus;
import GraduationProject.forumikaa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipDao extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.user.id = :userId AND f.friend.id = :otherUserId)
           OR (f.user.id = :otherUserId AND f.friend.id = :userId)
    """)
    Optional<Friendship> findBetweenUsers(@Param("userId") Long userId,
                                          @Param("otherUserId") Long otherUserId);

    List<Friendship> findByUserIdAndStatus(Long userId, FriendshipStatus status);
    List<Friendship> findByFriendIdAndStatus(Long friendId, FriendshipStatus status);

    @Query("""
        SELECT CASE WHEN f.user.id = :userId THEN f.friend ELSE f.user END
        FROM Friendship f
        WHERE (f.user.id = :userId OR f.friend.id = :userId)
          AND f.status = 'ACCEPTED'
    """)
    List<User> findFriendsOf(@Param("userId") Long userId);
}


