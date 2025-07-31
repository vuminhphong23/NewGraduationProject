package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.PostDao;
import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.dto.UpdatePostRequest;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class PostServiceImpl implements PostService {

    @Autowired private PostDao postDao;
    @Autowired private UserDao userDao;
    @Autowired private TopicDao topicDao;

    @Override
    public PostDto createPost(CreatePostRequest request, Long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Topic topic = topicDao.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUser(user);
        post.setPrivacy(request.getPrivacy());
        post.setTopic(topic);
        post.setStatus(PostStatus.APPROVED); // Mặc định pending

        return convertToDto(postDao.save(post));
    }

    @Override
    public PostDto updatePost(Long postId, UpdatePostRequest request, Long userId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own posts");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        if (request.getPrivacy() != null) post.setPrivacy(request.getPrivacy());

        if (request.getTopicId() != null) {
            Topic topic = topicDao.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
            post.setTopic(topic);
        }

        return convertToDto(postDao.save(post));
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own posts");
        }

        postDao.delete(post);
    }

    @Override
    public PostDto getPostById(Long postId, Long userId) {
        Post post = postDao.findPostByIdAndUserAccess(postId, userId);
        if (post == null) {
            throw new ResourceNotFoundException("Post not found or access denied");
        }
        return convertToDto(post);
    }

    @Override
    public List<PostDto> getUserFeed(Long userId) {
        return postDao.findUserFeed(userId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<PostDto> getUserPosts(Long userId) {
        return postDao.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<PostDto> getPostsByTopic(Long topicId, Long userId) {
        return postDao.findByTopicIdWithUserAccess(topicId, userId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public PostDto approvePost(Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setStatus(PostStatus.APPROVED);
        return convertToDto(postDao.save(post));
    }

    @Override
    public PostDto rejectPost(Long postId, String reason) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setStatus(PostStatus.REJECTED);
        return convertToDto(postDao.save(post));
    }

    @Override
    public boolean canAccessPost(Long postId, Long userId) {
        return postDao.findPostByIdAndUserAccess(postId, userId) != null;
    }

    @Override
    public boolean canEditPost(Long postId, Long userId) {
        return postDao.findById(postId)
                .map(p -> p.getUser().getId().equals(userId))
                .orElse(false);
    }

    private PostDto convertToDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setUserId(post.getUser().getId());
        dto.setUserName(post.getUser().getUsername()); // FIX: Added missing userName mapping

        if (post.getTopic() != null) {
            dto.setTopicId(post.getTopic().getId());
            dto.setTopicName(post.getTopic().getName());
        } else {
            dto.setTopicId(null);
            dto.setTopicName("No Topic");
        }

        dto.setStatus(post.getStatus());
        dto.setPrivacy(post.getPrivacy());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setLikeCount(0L);     // placeholder
        dto.setCommentCount(0L);  // placeholder

        return dto;
    }
}
