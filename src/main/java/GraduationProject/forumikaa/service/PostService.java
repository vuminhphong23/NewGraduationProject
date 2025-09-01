package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.dto.SuggestedPostDto;
import GraduationProject.forumikaa.dto.UpdatePostRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

public interface PostService {
    PostDto createPost(CreatePostRequest request, Long userId);
    PostDto updatePost(Long postId, UpdatePostRequest request, Long userId);

//    @PreAuthorize("hasRole('ADMIN')")
    void deletePost(Long postId, Long userId);
    PostDto getPostById(Long postId, Long userId);
    List<PostDto> getUserFeed(Long userId);
    List<PostDto> getUserPosts(Long userId);
    List<PostDto> getPostsByTopic(Long topicId, Long userId);

    PostDto approvePost(Long postId);

//    @PreAuthorize("hasRole('ADMIN')")
    PostDto rejectPost(Long postId, String reason);
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
    
    // Suggested posts
    List<SuggestedPostDto> getSuggestedPosts(Long userId, Integer maxLevel, Integer limit);

}
