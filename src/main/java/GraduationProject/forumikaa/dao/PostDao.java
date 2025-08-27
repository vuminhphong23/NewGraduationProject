package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostDao extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.user u LEFT JOIN FETCH u.userProfile WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        LEFT JOIN Friendship f1 ON (f1.user.id = :userId AND f1.friend.id = p.user.id AND f1.status = 'ACCEPTED')
        LEFT JOIN Friendship f2 ON (f2.user.id = p.user.id AND f2.friend.id = :userId AND f2.status = 'ACCEPTED')
        WHERE p.status = 'APPROVED' AND (
            (p.privacy = 'PUBLIC') OR
            (p.privacy = 'FRIENDS' AND (p.user.id = :userId OR f1.id IS NOT NULL OR f2.id IS NOT NULL)) OR
            (p.privacy = 'PRIVATE' AND p.user.id = :userId)
        )
        ORDER BY p.createdAt DESC
    """)
    List<Post> findUserFeed(@Param("userId") Long userId);

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        LEFT JOIN Friendship f1 ON (f1.user.id = :userId AND f1.friend.id = p.user.id AND f1.status = 'ACCEPTED')
        LEFT JOIN Friendship f2 ON (f2.user.id = p.user.id AND f2.friend.id = :userId AND f2.status = 'ACCEPTED')
        WHERE p.id = :postId AND (
            (p.privacy = 'PUBLIC') OR
            (p.privacy = 'FRIENDS' AND (p.user.id = :userId OR f1.id IS NOT NULL OR f2.id IS NOT NULL)) OR
            (p.privacy = 'PRIVATE' AND p.user.id = :userId)
        )
    """)
    Post findPostByIdAndUserAccess(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics t
        LEFT JOIN Friendship f1 ON (f1.user.id = :userId AND f1.friend.id = p.user.id AND f1.status = 'ACCEPTED')
        LEFT JOIN Friendship f2 ON (f2.user.id = p.user.id AND f2.friend.id = :userId AND f2.status = 'ACCEPTED')
        WHERE t.id = :topicId AND p.status = 'APPROVED' AND (
            (p.privacy = 'PUBLIC') OR
            (p.privacy = 'FRIENDS' AND (p.user.id = :userId OR f1.id IS NOT NULL OR f2.id IS NOT NULL)) OR
            (p.privacy = 'PRIVATE' AND p.user.id = :userId)
        )
        ORDER BY p.createdAt DESC
    """)
    List<Post> findByTopicIdWithUserAccess(@Param("topicId") Long topicId, @Param("userId") Long userId);

    default boolean canEdit(Post post, Long userId) {
        return post != null && post.getUser().getId().equals(userId);
    }

}
