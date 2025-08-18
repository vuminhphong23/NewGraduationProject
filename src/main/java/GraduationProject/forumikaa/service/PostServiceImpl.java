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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    @Autowired private PostDao postDao;
    @Autowired private UserDao userDao;
    @Autowired private TopicDao topicDao;
    @Autowired private TopicService topicService;

    @Override
    public PostDto createPost(CreatePostRequest request, Long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUser(user);
        post.setPrivacy(request.getPrivacy());
        post.setStatus(PostStatus.APPROVED);

        // Process topics from topicNames (hashtags)
        Set<Topic> topics = new HashSet<>();
        
        // If topicNames is provided (from hashtag input)
        if (request.getTopicNames() != null && !request.getTopicNames().isEmpty()) {
            for (String topicName : request.getTopicNames()) {
                if (topicName != null && !topicName.trim().isEmpty()) {
                    Topic topic = topicService.findOrCreateTopic(topicName.trim(), user);
                    topics.add(topic);
                }
            }
        }
        
        // Fallback: if topicId is provided (backward compatibility)
        else if (request.getTopicId() != null) {
            topicDao.findById(request.getTopicId()).ifPresent(topics::add);
        }
        
        // If no topics provided, create a default "General" topic
        if (topics.isEmpty()) {
            Topic defaultTopic = topicService.findOrCreateTopic("general", user);
            topics.add(defaultTopic);
        }
        
        post.setTopics(topics);

        // Save post
        Post savedPost = postDao.save(post);

        // Update usage count for topics
        topics.forEach(topicService::incrementUsageCount);

        return convertToDto(savedPost);
    }

    @Override
    public PostDto updatePost(Long postId, UpdatePostRequest request, Long userId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own posts");
        }

        // Giảm usage count của topics cũ
        post.getTopics().forEach(topicService::decrementUsageCount);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        if (request.getPrivacy() != null) {
            post.setPrivacy(request.getPrivacy());
        }

        // Process topics from topicNames (hashtags) first
        Set<Topic> newTopics = new HashSet<>();
        
        // If topicNames is provided (from hashtag input)
        if (request.getTopicNames() != null && !request.getTopicNames().isEmpty()) {
            for (String topicName : request.getTopicNames()) {
                if (topicName != null && !topicName.trim().isEmpty()) {
                    Topic topic = topicService.findOrCreateTopic(topicName.trim(), post.getUser());
                    newTopics.add(topic);
                }
            }
        }
        // Fallback: if topicId is provided (backward compatibility)
        else if (request.getTopicId() != null) {
            topicDao.findById(request.getTopicId()).ifPresent(newTopics::add);
        }
        
        // If no topics provided, create a default "General" topic
        if (newTopics.isEmpty()) {
            Topic defaultTopic = topicService.findOrCreateTopic("general", post.getUser());
            newTopics.add(defaultTopic);
        }


        post.setTopics(newTopics);

        Post savedPost = postDao.save(post);

        // Cập nhật usage count cho topics mới
        newTopics.forEach(topicService::incrementUsageCount);

        return convertToDto(savedPost);
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own posts");
        }

        // Giảm usage count của các topics
        post.getTopics().forEach(topicService::decrementUsageCount);

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

    /**
     * Trích xuất hashtags từ content và tạo/lấy topics tương ứng
     */
    private Set<Topic> extractAndProcessHashtags(String content, User user) {
        Set<Topic> topics = new HashSet<>();

        if (content == null || content.trim().isEmpty()) {
            return topics;
        }

        // Pattern để tìm hashtags (#word)
        Pattern hashtagPattern = Pattern.compile("#([a-zA-Z0-9_àáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]+)");
        Matcher matcher = hashtagPattern.matcher(content);

        while (matcher.find()) {
            String hashtagName = matcher.group(1); // Lấy phần sau dấu #
            Topic topic = topicService.findOrCreateTopic(hashtagName, user);
            topics.add(topic); // Set tự động tránh duplicate
        }

        return topics;
    }

    /**
     * Convert Post entity sang PostDto
     */
    private PostDto convertToDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setUserId(post.getUser().getId());
        dto.setUserName(post.getUser().getUsername());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0L);
        dto.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0L);
        dto.setShareCount(post.getShareCount() != null ? post.getShareCount() : 0L);
        dto.setStatus(post.getStatus());
        dto.setPrivacy(post.getPrivacy());

        // Set topic names for hashtag functionality 
        if (post.getTopics() != null && !post.getTopics().isEmpty()) {
            List<String> topicNames = post.getTopics().stream()
                    .map(Topic::getName)
                    .toList();
            dto.setTopicNames(topicNames);
        } else {
            // Set empty list if no topics
            dto.setTopicNames(new ArrayList<>());
        }

        return dto;
    }
}
