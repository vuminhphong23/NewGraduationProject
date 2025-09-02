package GraduationProject.forumikaa.service;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.UpdatePostRequest;

import java.util.List;
import java.util.Map;

public interface PostService {
    PostResponse createPost(CreatePostRequest request, Long userId);
    PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId);

    void deletePost(Long postId, Long userId);
    PostResponse getPostById(Long postId, Long userId);
    List<PostResponse> getUserFeed(Long userId);
    List<PostResponse> getUserPosts(Long userId);
    List<PostResponse> getPostsByTopic(Long topicId, Long userId);
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
    Map<String, Object> sharePost(Long postId, Long userId, String message);
    Long getPostShareCount(Long postId);

}
