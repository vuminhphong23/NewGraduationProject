package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.dto.UpdatePostRequest;

import java.util.List;

public interface PostService {
    PostDto createPost(CreatePostRequest request, Long userId);
    PostDto updatePost(Long postId, UpdatePostRequest request, Long userId);
    void deletePost(Long postId, Long userId);
    PostDto getPostById(Long postId, Long userId);
    List<PostDto> getUserFeed(Long userId);
    List<PostDto> getUserPosts(Long userId);
    List<PostDto> getPostsByTopic(Long topicId, Long userId);
    PostDto approvePost(Long postId);
    PostDto rejectPost(Long postId, String reason);
    boolean canAccessPost(Long postId, Long userId);
    boolean canEditPost(Long postId, Long userId);
}
