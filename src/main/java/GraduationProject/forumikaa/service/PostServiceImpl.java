package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.PostDao;
import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.LikeDao;
import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.dto.UpdatePostRequest;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.Comment;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    @Autowired private PostDao postDao;
    @Autowired private UserDao userDao;
    @Autowired private TopicDao topicDao;
    @Autowired private TopicService topicService;
    @Autowired
    private LikeService likeService;
    @Autowired private CommentDao commentDao;
    @Autowired private NotificationService notificationService;

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

        Post savedPost = postDao.save(post);

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
        Post post = postDao.findPostByIdAndUserAccess(postId, userId);
        return post != null;
    }

    @Override
    public boolean canEditPost(Long postId, Long userId) {
        return postDao.findById(postId)
                .map(p -> p.getUser().getId().equals(userId))
                .orElse(false);
    }

    // Like functionality
    @Override
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean wasLiked = likeService.isPostLikedByUser(postId, userId);
        if (wasLiked) {
            likeService.unlikePost(postId, userId);
        } else {
            likeService.likePost(postId, userId);
            // Gửi notification khi like bài viết (chỉ khi chưa like trước đó)
            if (!post.getUser().getId().equals(userId)) { // Không gửi notification cho chính mình
                notificationService.createPostLikeNotification(postId, post.getUser().getId(), userId);
            }
        }
        // always recompute from DB to avoid drift
        Long freshCount = likeService.getPostLikeCount(postId);
        post.setLikeCount(freshCount);
        postDao.save(post);
        return !wasLiked;
    }

    @Override
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return likeService.isPostLikedByUser(postId, userId);
    }

    @Override
    public Long getPostLikeCount(Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return post.getLikeCount() != null ? post.getLikeCount() : 0L;
    }

    // Comment functionality
    @Override
    public List<Map<String, Object>> getPostComments(Long postId, Long userId, int page, int size) {
        // Tạm thời bypass việc kiểm tra quyền truy cập để debug
        // if (!canAccessPost(postId, userId)) {
        //     throw new UnauthorizedException("Cannot access post");
        // }
        
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentDao.findByPostIdOrderByCreatedAtDesc(postId, pageable).getContent();
        
        return comments.stream().map(comment -> {
            // Lấy avatar từ UserProfile
            String userAvatar = null;
            if (comment.getUser().getUserProfile() != null) {
                userAvatar = comment.getUser().getUserProfile().getAvatar();
            }
            
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("content", comment.getContent());
            commentMap.put("userId", comment.getUser().getId());
            commentMap.put("userName", comment.getUser().getUsername());
            commentMap.put("userAvatar", userAvatar);
            commentMap.put("createdAt", comment.getCreatedAt());
            
            return commentMap;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> addComment(Long postId, Long userId, String content) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Add comment
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent(content);
        Comment savedComment = commentDao.save(comment);
        
        // Update comment count
        post.setCommentCount(post.getCommentCount() + 1);
        postDao.save(post);
        
        // Gửi notification khi comment bài viết
        if (!post.getUser().getId().equals(userId)) { // Không gửi notification cho chính mình
            notificationService.createPostCommentNotification(postId, post.getUser().getId(), userId, savedComment.getId());
        }
        
        // Return comment data
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", savedComment.getId());
        commentData.put("content", content);
        commentData.put("userId", userId);
        commentData.put("userName", user.getUsername());
        commentData.put("userAvatar", user.getUserProfile() != null ? user.getUserProfile().getAvatar() : null);
        commentData.put("createdAt", savedComment.getCreatedAt());
        
        return commentData;
    }

    @Override
    public void deleteComment(Long postId, Long commentId, Long userId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        // Check if user can delete comment (owner of comment or owner of post)
        boolean canDelete = commentDao.existsByIdAndUserId(commentId, userId) || 
                           post.getUser().getId().equals(userId);
        
        if (!canDelete) {
            throw new UnauthorizedException("Cannot delete this comment");
        }
        
        // Delete comment
        commentDao.deleteById(commentId);
        
        // Update comment count
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postDao.save(post);
    }

    @Override
    public Map<String, Object> updateComment(Long postId, Long commentId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung bình luận không được trống");
        }
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        Comment comment = commentDao.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!comment.getPost().getId().equals(post.getId())) {
            throw new UnauthorizedException("Comment không thuộc bài viết này");
        }
        if (!comment.getUser().getId().equals(userId) && !post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền sửa bình luận này");
        }
        comment.setContent(content.trim());
        Comment saved = commentDao.save(comment);
        
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", saved.getId());
        commentData.put("content", saved.getContent());
        commentData.put("userId", saved.getUser().getId());
        commentData.put("userName", saved.getUser().getUsername());
        commentData.put("userAvatar", saved.getUser().getUserProfile() != null ? saved.getUser().getUserProfile().getAvatar() : null);
        commentData.put("createdAt", saved.getCreatedAt());
        
        return commentData;
    }

    @Override
    public Long getPostCommentCount(Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return post.getCommentCount() != null ? post.getCommentCount() : 0L;
    }

    // Share functionality
    @Override
    public Map<String, Object> sharePost(Long postId, Long userId, String message) {
        Post originalPost = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Create shared post
        Post sharedPost = new Post();
        sharedPost.setTitle("Chia sẻ: " + (originalPost.getTitle() != null ? originalPost.getTitle() : "Bài viết"));
        sharedPost.setContent(message != null ? message : "Đã chia sẻ bài viết này");
        sharedPost.setUser(user);
        sharedPost.setPrivacy(originalPost.getPrivacy()); // Use same privacy as original
        sharedPost.setStatus(PostStatus.APPROVED);
        
        // Update share count of original post
        originalPost.setShareCount(originalPost.getShareCount() + 1);
        postDao.save(originalPost);
        
        // Save shared post
        Post savedSharedPost = postDao.save(sharedPost);
        
        Map<String, Object> sharedPostData = new HashMap<>();
        sharedPostData.put("id", savedSharedPost.getId());
        sharedPostData.put("title", savedSharedPost.getTitle());
        sharedPostData.put("content", savedSharedPost.getContent());
        sharedPostData.put("userId", userId);
        sharedPostData.put("userName", user.getUsername());
        
        // Lấy avatar của user chia sẻ
        String userAvatar = null;
        if (user.getUserProfile() != null && user.getUserProfile().getAvatar() != null && !user.getUserProfile().getAvatar().trim().isEmpty()) {
            userAvatar = user.getUserProfile().getAvatar();
        } else {
            userAvatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
        }
        sharedPostData.put("userAvatar", userAvatar);
        
        sharedPostData.put("createdAt", savedSharedPost.getCreatedAt());
        sharedPostData.put("originalPostId", postId);
        
        return sharedPostData;
    }

    @Override
    public Long getPostShareCount(Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return post.getShareCount() != null ? post.getShareCount() : 0L;
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
        
        // Lấy avatar của user từ UserProfile
        String userAvatar = null;
        if (post.getUser().getUserProfile() != null && post.getUser().getUserProfile().getAvatar() != null && !post.getUser().getUserProfile().getAvatar().trim().isEmpty()) {
            userAvatar = post.getUser().getUserProfile().getAvatar();
        } else {
            userAvatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
        }
        dto.setUserAvatar(userAvatar);
        
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
