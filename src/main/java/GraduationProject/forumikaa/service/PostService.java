package GraduationProject.forumikaa.service;
import GraduationProject.forumikaa.dto.PostRequest;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.entity.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostService {
    PostResponse createPost(PostRequest request, Long userId);
    PostResponse updatePost(Long postId, PostRequest request, Long userId);

    void deletePost(Long postId, Long userId);
    PostResponse getPostById(Long postId, Long userId);
    List<PostResponse> getUserFeed(Long userId);
    List<PostResponse> getUserPosts(Long userId);
    List<PostResponse> getPostsByTopic(Long topicId, Long userId);
    List<PostResponse> getPostsByGroup(Long groupId, Long userId);
    Long getPostCountByGroup(Long groupId);
    Long getNewPostCountByGroupToday(Long groupId);
    PostResponse approvePost(Long postId);

    PostResponse rejectPost(Long postId, String reason);
    boolean canAccessPost(Long postId, Long userId);
    boolean canEditPost(Long postId, Long userId);
    
    // Like functionality
    boolean toggleLike(Long postId, Long userId);
    boolean isPostLikedByUser(Long postId, Long userId);
    Long getPostLikeCount(Long postId);
    
    // Comment functionality
    List<Map<String, Object>> getPostComments(Long postId, Long userId, int page, int size);
    Map<String, Object> addComment(Long postId, Long userId, String content);
    void deleteComment(Long postId, Long commentId, Long userId);
    Long getPostCommentCount(Long postId);
    Map<String, Object> updateComment(Long postId, Long commentId, Long userId, String content);
    
    // Share functionality
    Map<String, Object> sharePost(Long postId, Long userId, String message, String privacy);
    Long getPostShareCount(Long postId);
    Long getUserShareCount(Long userId);
    Long getUserShareCountInDateRange(Long userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    // Admin management methods
    Page<Post> findPaginated(String keyword, String status, String privacy, Pageable pageable);
    Page<Post> findPostsByGroup(Long groupId, String keyword, String status, Pageable pageable);
    Optional<Post> findById(Long id);
    Post save(Post post);
    void deleteById(Long id);
    List<Post> findAll();

}
