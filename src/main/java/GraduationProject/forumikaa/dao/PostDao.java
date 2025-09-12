package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostDao extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

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


    // Tìm posts theo username của user
    List<Post> findByUserUsername(String username);
    
    // Tìm posts theo topics và username
    List<Post> findByTopicsAndUserUsername(Topic topic, String username);
    
    // Tìm posts theo user ID
    List<Post> findByUserId(Long userId);

    // Admin pagination with filters - basic search only
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findPaginated(@Param("keyword") String keyword, 
                            @Param("status") String status, 
                            @Param("privacy") String privacy, 
                            Pageable pageable);

    // Find posts by status
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE p.status = :status
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByStatus(@Param("status") PostStatus status, Pageable pageable);

    // Find posts by privacy
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE p.privacy = :privacy
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByPrivacy(@Param("privacy") PostPrivacy privacy, Pageable pageable);

    // Find posts by status and privacy
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE p.status = :status AND p.privacy = :privacy
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByStatusAndPrivacy(@Param("status") PostStatus status, 
                                     @Param("privacy") PostPrivacy privacy, 
                                     Pageable pageable);

    // Find posts by keyword and status
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND p.status = :status
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByKeywordAndStatus(@Param("keyword") String keyword, 
                                     @Param("status") PostStatus status, 
                                     Pageable pageable);

    // Find posts by keyword and privacy
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND p.privacy = :privacy
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByKeywordAndPrivacy(@Param("keyword") String keyword, 
                                      @Param("privacy") PostPrivacy privacy, 
                                      Pageable pageable);

    // Find posts by keyword, status and privacy
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN FETCH p.topics
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND p.status = :status AND p.privacy = :privacy
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByKeywordAndStatusAndPrivacy(@Param("keyword") String keyword, 
                                               @Param("status") PostStatus status, 
                                               @Param("privacy") PostPrivacy privacy, 
                                               Pageable pageable);

    default boolean canEdit(Post post, Long userId) {
        return post != null && post.getUser().getId().equals(userId);
    }
}
