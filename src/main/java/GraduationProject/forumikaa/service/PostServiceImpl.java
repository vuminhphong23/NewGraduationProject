package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.PostDao;
import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.dao.GroupDao;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.UpdatePostRequest;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.Comment;
import GraduationProject.forumikaa.entity.Document;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.exception.UnauthorizedException;
import GraduationProject.forumikaa.patterns.strategy.FileStorageStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import GraduationProject.forumikaa.dto.FileUploadResponse;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    @Autowired private PostDao postDao;
    @Autowired private UserDao userDao;
    @Autowired private TopicDao topicDao;
    @Autowired private TopicService topicService;
    @Autowired private LikeService likeService;
    @Autowired private CommentDao commentDao;
    @Autowired private NotificationService notificationService;
    @Autowired private FileUploadService fileUploadService;
    @Autowired private FileStorageStrategyFactory strategyFactory;
    @Autowired private GroupDao groupDao;

    @Override
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUser(user);
        post.setPrivacy(request.getPrivacy());
        post.setStatus(PostStatus.APPROVED);
        
        // Set group if groupId is provided
        if (request.getGroupId() != null) {
            UserGroup group = groupDao.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            post.setGroup(group);
        }

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
    public PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId) {
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

        // Get documents before deleting post (for file cleanup)
        List<Document> documents = new ArrayList<>(post.getDocuments());

        // Decrement topic usage counts
        post.getTopics().forEach(topicService::decrementUsageCount);

        // Delete the post first (documents will be deleted by cascade due to orphanRemoval=true)
        postDao.delete(post);

        // Now delete physical files from storage (after database cleanup)
        try {
            for (Document document : documents) {
                // Delete from storage using the appropriate strategy
                if (strategyFactory.getCurrentStrategyType().equals("local")) {
                    // For local storage, delete physical file
                    Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", document.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.deleteIfExists(filePath);
                    }
                } else if (strategyFactory.getCurrentStrategyType().equals("cloudinary")) {
                    // For Cloudinary, delete using the correct public_id
                    try {
                        deleteFromCloudinary(document.getFileName());
                    } catch (Exception ex) {
                        System.err.println("Failed to delete Cloudinary file: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to delete physical files for post " + postId + ": " + e.getMessage());
        }
    }

    /**
     * Delete file from Cloudinary using public_id
     */
    private void deleteFromCloudinary(String publicId) throws IOException {
        // Get Cloudinary credentials from application properties
        String cloudName = "dsqkymrkm";
        String apiKey = "769412498641216";
        String apiSecret = "iCRSwCZ8p3j7NzIUm8pb3itcMB4";
        
        // Try different resource types
        String[] resourceTypes = {"image", "video", "raw"};
        
        for (String resourceType : resourceTypes) {
            try {
                String deleteUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/destroy";
                
                // Generate timestamp and signature
                long timestamp = System.currentTimeMillis() / 1000;
                String signature = generateSignature(publicId, timestamp, apiSecret);
                
                // URL encode the public_id
                String encodedPublicId = java.net.URLEncoder.encode(publicId, StandardCharsets.UTF_8);
                String formData = "public_id=" + encodedPublicId + 
                                "&api_key=" + apiKey + 
                                "&timestamp=" + timestamp + 
                                "&signature=" + signature;
                
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(deleteUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return; // Success, exit the method
                }
            } catch (Exception e) {
                // Continue to next resource type
            }
        }
        
        throw new IOException("Failed to delete file from Cloudinary with public_id: " + publicId);
    }

    /**
     * Generate Cloudinary signature for API authentication
     */
    private String generateSignature(String publicId, long timestamp, String apiSecret) {
        try {
            // Create the string to sign in the format: key=value&key=value
            String stringToSign = "public_id=" + publicId + "&timestamp=" + timestamp;
            
            // Append API secret to the string to sign
            String stringToSignWithSecret = stringToSign + apiSecret;
            
            // Create SHA-1 hash
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(stringToSignWithSecret.getBytes("UTF-8"));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    

    @Override
    public PostResponse getPostById(Long postId, Long userId) {
        Post post = postDao.findPostByIdAndUserAccess(postId, userId);
        if (post == null) {
            throw new ResourceNotFoundException("Post not found or access denied");
        }
        return convertToDto(post);
    }

    @Override
    public List<PostResponse> getUserFeed(Long userId) {
        return postDao.findUserFeed(userId)
                .stream()
                .map(post -> convertToDto(post, userId))
                .toList();
    }

    @Override
    public List<PostResponse> getUserPosts(Long userId) {
        return postDao.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(post -> convertToDto(post, userId))
                .toList();
    }

    @Override
    public List<PostResponse> getPostsByTopic(Long topicId, Long userId) {
        return postDao.findByTopicIdWithUserAccess(topicId, userId)
                .stream()
                .map(post -> convertToDto(post, userId))
                .toList();
    }

    @Override
    public List<PostResponse> getPostsByGroup(Long groupId, Long userId) {
        return postDao.findByGroupIdWithUserAccess(groupId, userId)
                .stream()
                .map(post -> convertToDto(post, userId))
                .toList();
    }

    @Override
    public PostResponse approvePost(Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setStatus(PostStatus.APPROVED);
        return convertToDto(postDao.save(post));
    }

    @Override
    public PostResponse rejectPost(Long postId, String reason) {
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

         if (!canAccessPost(postId, userId)) {
             throw new UnauthorizedException("Cannot access post");
         }
        
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
     * Convert Post entity sang PostDto
     */
    private PostResponse convertToDto(Post post) {
        return convertToDto(post, null);
    }
    
    /**
     * Convert Post entity sang PostDto with user context for isLiked
     */
    private PostResponse convertToDto(Post post, Long currentUserId) {
        PostResponse dto = new PostResponse();
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
        
        // Set isLiked if currentUserId is provided
        if (currentUserId != null) {
            try {
                dto.setIsLiked(isPostLikedByUser(post.getId(), currentUserId));
            } catch (Exception e) {
                dto.setIsLiked(false);
            }
        } else {
            dto.setIsLiked(false);
        }

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

        // Set documents
        try {
            List<FileUploadResponse> documents = fileUploadService.getFilesByPostId(post.getId());
            dto.setDocuments(documents != null ? documents : new ArrayList<>());
        } catch (Exception e) {
            // Log error but don't fail the entire post
            System.err.println("Error loading documents for post " + post.getId() + ": " + e.getMessage());
            dto.setDocuments(new ArrayList<>());
        }

        // Set group information if post belongs to a group
        if (post.getGroup() != null) {
            dto.setGroupId(post.getGroup().getId());
            dto.setGroupName(post.getGroup().getName());
            dto.setGroupDescription(post.getGroup().getDescription());
            
            // Set group avatar with fallback
            String groupAvatar = post.getGroup().getAvatar();
            if (groupAvatar != null && !groupAvatar.trim().isEmpty()) {
                dto.setGroupAvatar(groupAvatar);
            } else {
                // Default group avatar or generate one based on group name
                dto.setGroupAvatar("https://ui-avatars.com/api/?name=" + 
                    java.net.URLEncoder.encode(post.getGroup().getName(), java.nio.charset.StandardCharsets.UTF_8) + 
                    "&background=007bff&color=ffffff&size=64");
            }
            
            // Get member count from group entity
            Long memberCount = post.getGroup().getMemberCount();
            dto.setGroupMemberCount(memberCount != null ? memberCount : 0L);
        }

        return dto;
    }

    // ========== ADMIN MANAGEMENT METHODS ==========

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findPaginated(String keyword, String status, String privacy, Pageable pageable) {
        // Convert String to enum
        PostStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = PostStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, keep as null
            }
        }
        
        PostPrivacy privacyEnum = null;
        if (privacy != null && !privacy.trim().isEmpty()) {
            try {
                privacyEnum = PostPrivacy.valueOf(privacy);
            } catch (IllegalArgumentException e) {
                // Invalid privacy, keep as null
            }
        }
        
        // Use different queries based on filters
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        
        if (statusEnum != null && privacyEnum != null) {
            if (hasKeyword) {
                return postDao.findByKeywordAndStatusAndPrivacy(keyword, statusEnum, privacyEnum, pageable);
            } else {
                return postDao.findByStatusAndPrivacy(statusEnum, privacyEnum, pageable);
            }
        } else if (statusEnum != null) {
            if (hasKeyword) {
                return postDao.findByKeywordAndStatus(keyword, statusEnum, pageable);
            } else {
                return postDao.findByStatus(statusEnum, pageable);
            }
        } else if (privacyEnum != null) {
            if (hasKeyword) {
                return postDao.findByKeywordAndPrivacy(keyword, privacyEnum, pageable);
            } else {
                return postDao.findByPrivacy(privacyEnum, pageable);
            }
        } else {
            return postDao.findPaginated(keyword, null, null, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findPostsByGroup(Long groupId, String keyword, String status, Pageable pageable) {
        // Convert String to enum
        PostStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = PostStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, keep as null
            }
        }
        
        return postDao.findPostsByGroup(groupId, keyword, status, statusEnum, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long id) {
        return postDao.findById(id);
    }

    @Override
    @Transactional
    public Post save(Post post) {
        return postDao.save(post);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // Find the post first to get topics before deletion
        Optional<Post> postOpt = postDao.findById(id);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            
            // Get documents before deleting post (for file cleanup)
            List<Document> documents = new ArrayList<>(post.getDocuments());
            
            // Decrement topic usage counts
            post.getTopics().forEach(topicService::decrementUsageCount);
            
            // Delete the post first (documents will be deleted by cascade due to orphanRemoval=true)
            postDao.delete(post);
            
            // Now delete physical files from storage (after database cleanup)
            try {
                for (Document document : documents) {
                    // Delete from storage using the appropriate strategy
                    if (strategyFactory.getCurrentStrategyType().equals("local")) {
                        // For local storage, delete physical file
                        Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", document.getFilePath());
                        if (Files.exists(filePath)) {
                            Files.deleteIfExists(filePath);
                        }
                    } else if (strategyFactory.getCurrentStrategyType().equals("cloudinary")) {
                        // For Cloudinary, delete using the correct public_id
                        try {
                            deleteFromCloudinary(document.getFileName());
                        } catch (Exception ex) {
                            System.err.println("Failed to delete Cloudinary file: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to delete physical files for post " + id + ": " + e.getMessage());
            }
        } else {
            // Post not found, just delete by ID (in case of orphaned records)
            postDao.deleteById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPostCountByGroup(Long groupId) {
        return postDao.countByGroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getNewPostCountByGroupToday(Long groupId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        return postDao.countNewPostsByGroupToday(groupId, startOfDay, endOfDay);
    }

}
