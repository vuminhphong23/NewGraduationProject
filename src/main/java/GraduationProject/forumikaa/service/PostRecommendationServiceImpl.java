package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.*;
import GraduationProject.forumikaa.dto.*;
import GraduationProject.forumikaa.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class PostRecommendationServiceImpl implements PostRecommendationService{
    
    @Autowired private PostDao postDao;
    @Autowired private LikeDao likeDao;
    @Autowired private CommentDao commentDao;
    @Autowired private TopicDao topicDao;
    @Autowired private TopicService topicService;
    @Autowired private FriendshipDao friendshipDao;
    @Autowired private FileUploadService fileUploadService;

    /**
     * Tab 1: Cho riêng bạn - Phân tích điểm dựa trên topic quan tâm và tương tác
     */
    @Override
    public List<PostResponse> getPersonalizedContent(Long userId, Integer limit) {
        return getRecommendedAllPosts(userId, limit);
    }
    
    /**
     * Tab 2: Được nhiều người quan tâm - Sắp xếp theo điểm Like(1) + Comment(2) + Share(3)
     */
    @Override
    public List<PostResponse> getTrendingContent(Long userId, Integer limit) {
        return getPopularPosts(userId, limit, 0);
    }
    
    /**
     * Lấy bài viết được nhiều người quan tâm với phân trang
     */
    public List<PostResponse> getPopularPosts(Long userId, Integer limit, Integer offset) {
        System.out.println("=== POPULAR POSTS RECOMMENDATION ===");
        System.out.println("User ID: " + userId + ", Limit: " + limit + ", Offset: " + offset);
        
        // Lấy tất cả posts (không chỉ crawled content)
        List<Post> allPosts = postDao.findAll();
        System.out.println("Total posts: " + allPosts.size());
        
        if (allPosts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Tính điểm trending: Like(1) + Comment(2) + Share(3)
        List<PostRecommendationScore> scoredPosts = new ArrayList<>();
        
        for (Post post : allPosts) {
            // Loại trừ posts của chính user
            if (post.getUser().getId().equals(userId)) {
                continue;
            }
            
            // Loại trừ shared posts
            if (post.getOriginalPostId() != null) {
                continue;
            }
            
            double trendingScore = calculateTrendingScore(post);
            scoredPosts.add(new PostRecommendationScore(post, trendingScore));
        }
        
        // Sắp xếp theo điểm trending và áp dụng phân trang
        return scoredPosts.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .skip(offset) // Bỏ qua offset posts đầu tiên
                .limit(limit)  // Lấy limit posts tiếp theo
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Gợi ý tất cả bài viết dựa trên mối quan tâm và tương tác của user
     */
    public List<PostResponse> getRecommendedAllPosts(Long userId, Integer limit) {
        System.out.println("=== ALL POSTS RECOMMENDATION ===");
        System.out.println("User ID: " + userId);
        
        // Lấy tất cả posts (không chỉ crawled content)
        List<Post> allPosts = postDao.findAll();
        System.out.println("Total posts: " + allPosts.size());
        
        if (allPosts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Lấy mối quan tâm của user
        Set<String> userInterests = getUserInterests(userId);
        System.out.println("User interests: " + userInterests);
        
        // Lấy lịch sử tương tác của user
        Map<Long, Double> userInteractionScores = getUserInteractionScores(userId);
        System.out.println("User interaction scores: " + userInteractionScores);
        
        // Tính điểm recommendation cho từng post
        List<PostRecommendationScore> scoredPosts = new ArrayList<>();
        
        for (Post post : allPosts) {
            double score = calculateRecommendationScore(post, userId, userInterests, userInteractionScores);
            if (score > 0) {
                scoredPosts.add(new PostRecommendationScore(post, score));
            }
        }
        
        // Sắp xếp theo điểm số và trả về
        return scoredPosts.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Gợi ý crawled content dựa trên mối quan tâm và tương tác của user (Legacy)
     */
    @Override
    @Deprecated
    public List<PostResponse> getRecommendedCrawledContent(Long userId, Integer limit) {
        System.out.println("=== CRAWLED CONTENT RECOMMENDATION ===");
        System.out.println("User ID: " + userId);
        
        // Lấy tất cả crawled posts (từ admin user)
        List<Post> crawledPosts = postDao.findByUserUsername("admin");
        System.out.println("Total crawled posts: " + crawledPosts.size());
        
        if (crawledPosts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Lấy mối quan tâm của user
        Set<String> userInterests = getUserInterests(userId);
        System.out.println("User interests: " + userInterests);
        
        // Lấy lịch sử tương tác của user
        Map<Long, Double> userInteractionScores = getUserInteractionScores(userId);
        System.out.println("User interaction scores: " + userInteractionScores);
        
        // Tính điểm recommendation cho từng crawled post
        List<PostRecommendationScore> scoredPosts = new ArrayList<>();
        
        for (Post post : crawledPosts) {
            double score = calculateRecommendationScore(post, userId, userInterests, userInteractionScores);
            if (score > 0) {
                scoredPosts.add(new PostRecommendationScore(post, score));
            }
        }
        
        // Sắp xếp theo điểm số và trả về
        return scoredPosts.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Gợi ý crawled content theo trending topics
     */
    @Override
    public List<PostResponse> getTrendingCrawledContent(Long userId, Integer limit) {
        // Lấy trending topics
        List<Topic> trendingTopics = topicService.getTopTopics(10);
        
        // Lấy crawled posts thuộc trending topics (từ admin user)
        List<Post> trendingCrawledPosts = new ArrayList<>();
        for (Topic topic : trendingTopics) {
            List<Post> posts = postDao.findByTopicsAndUserUsername(topic, "admin");
            trendingCrawledPosts.addAll(posts);
        }
        
        // Sắp xếp theo engagement (likes + comments + shares)
        return trendingCrawledPosts.stream()
                .sorted((a, b) -> {
                    long engagementA = a.getLikeCount() + a.getCommentCount() + a.getShareCount();
                    long engagementB = b.getLikeCount() + b.getCommentCount() + b.getShareCount();
                    return Long.compare(engagementB, engagementA);
                })
                .limit(limit)
                .map(post -> convertToPostResponse(new PostRecommendationScore(post, 0.0)))
                .collect(Collectors.toList());
    }
    
    /**
     * Gợi ý crawled content theo mối quan tâm cụ thể
     */
    @Override
    public List<PostResponse> getCrawledContentByInterest(Long userId, String interest, Integer limit) {
        // Tìm topic tương ứng với interest
        Optional<Topic> topic = topicDao.findByNameIgnoreCase(interest);
        if (topic.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Lấy crawled posts thuộc topic này (từ admin user)
        List<Post> posts = postDao.findByTopicsAndUserUsername(topic.get(), "admin");
        
        // Sắp xếp theo engagement
        return posts.stream()
                .sorted((a, b) -> {
                    long engagementA = a.getLikeCount() + a.getCommentCount() + a.getShareCount();
                    long engagementB = b.getLikeCount() + b.getCommentCount() + b.getShareCount();
                    return Long.compare(engagementB, engagementA);
                })
                .limit(limit)
                .map(post -> convertToPostResponse(new PostRecommendationScore(post, 0.0)))
                .collect(Collectors.toList());
    }
    
    private double calculateRecommendationScore(Post post, Long userId, Set<String> userInterests, Map<Long, Double> userInteractionScores) {
        double score = 0.0;
        
        // Loại trừ posts của chính user
        if (post.getUser().getId().equals(userId)) {
            return 0.0;
        }
        
        // Loại trừ shared posts (posts có originalPostId)
        if (post.getOriginalPostId() != null) {
            return 0.0;
        }
        
        // Lấy topics của post hiện tại
        Set<String> postTopics = post.getTopics().stream()
                .map(Topic::getName)
                .collect(Collectors.toSet());
        
        // 1. ĐIỂM TỪ BÀI VIẾT CỦA USER (40%) - User đã đăng về chủ đề này
        double authoredScore = calculateAuthoredTopicsScore(postTopics, userId);
        score += authoredScore * 0.4;
        
        // 2. ĐIỂM TỪ TƯƠNG TÁC (35%) - User đã like/comment/share về chủ đề này
        double interactionScore = calculateInteractionTopicsScore(postTopics, userId);
        score += interactionScore * 0.35;
        
        // 3. ĐIỂM TỪ BẠN BÈ (25%) - Bạn bè đang đăng nhiều về chủ đề này
        double friendsScore = calculateFriendsTopicsScore(postTopics, userId);
        score += friendsScore * 0.25;
        
        return Math.max(0, score); // Đảm bảo score >= 0
    }
    
    /**
     * Tính điểm dựa trên topics mà user đã đăng bài
     */
    private double calculateAuthoredTopicsScore(Set<String> postTopics, Long userId) {
        double score = 0.0;
        
        // Lấy topics từ các bài viết mà user đã đăng
        Set<String> userAuthoredTopics = getUserAuthoredTopics(userId);
        
        // Đếm topics chung
        long commonTopics = postTopics.stream()
                .mapToLong(topic -> userAuthoredTopics.contains(topic) ? 1 : 0)
                .sum();
        
        if (commonTopics > 0) {
            // Trọng số cao cho topics mà user đã đăng
            score += (commonTopics * 8.0); // 8 điểm mỗi topic user đã đăng
            
            // Bonus cho exact match
            if (commonTopics == postTopics.size()) {
                score += 5.0; // Bonus nếu tất cả topics đều match
            }
            
            // Bonus cho nhiều topics chung
            if (commonTopics > 1) {
                score += (commonTopics - 1) * 3.0; // Bonus 3 điểm cho mỗi topic chung thêm
            }
        }
        
        return score;
    }
    
    /**
     * Tính điểm dựa trên topics mà user đã like/comment/share
     */
    private double calculateInteractionTopicsScore(Set<String> postTopics, Long userId) {
        double score = 0.0;
        
        // Lấy topics từ các bài viết mà user đã tương tác
        Set<String> userInteractionTopics = getUserInteractionTopics(userId);
        
        // Đếm topics chung
        long commonTopics = postTopics.stream()
                .mapToLong(topic -> userInteractionTopics.contains(topic) ? 1 : 0)
                .sum();
        
        if (commonTopics > 0) {
            // Trọng số trung bình cho topics mà user đã tương tác
            score += (commonTopics * 6.0); // 6 điểm mỗi topic user đã tương tác
            
            // Bonus cho exact match
            if (commonTopics == postTopics.size()) {
                score += 4.0; // Bonus nếu tất cả topics đều match
            }
            
            // Bonus cho nhiều topics chung
            if (commonTopics > 1) {
                score += (commonTopics - 1) * 2.0; // Bonus 2 điểm cho mỗi topic chung thêm
            }
        }
        
        return score;
    }
    
    /**
     * Tính điểm dựa trên topics mà bạn bè đang đăng nhiều
     */
    private double calculateFriendsTopicsScore(Set<String> postTopics, Long userId) {
        double score = 0.0;
        
        // Lấy topics từ các bài viết của bạn bè
        Set<String> friendsTopics = getFriendsActiveTopics(userId);
        
        // Đếm topics chung
        long commonTopics = postTopics.stream()
                .mapToLong(topic -> friendsTopics.contains(topic) ? 1 : 0)
                .sum();
        
        if (commonTopics > 0) {
            // Trọng số thấp hơn cho topics của bạn bè
            score += (commonTopics * 4.0); // 4 điểm mỗi topic bạn bè đang đăng
            
            // Bonus cho exact match
            if (commonTopics == postTopics.size()) {
                score += 3.0; // Bonus nếu tất cả topics đều match
            }
            
            // Bonus cho nhiều topics chung
            if (commonTopics > 1) {
                score += (commonTopics - 1) * 1.5; // Bonus 1.5 điểm cho mỗi topic chung thêm
            }
        }
        
        return score;
    }
    
    /**
     * Lấy topics từ các bài viết mà user đã đăng
     */
    private Set<String> getUserAuthoredTopics(Long userId) {
        return postDao.findByUserId(userId).stream()
                .flatMap(post -> post.getTopics().stream())
                .map(Topic::getName)
                .collect(Collectors.toSet());
    }
    
    /**
     * Lấy topics từ các bài viết mà user đã like/comment/share
     */
    private Set<String> getUserInteractionTopics(Long userId) {
        Set<String> topics = new HashSet<>();
        
        // Topics từ posts đã like
        topics.addAll(likeDao.findByUserIdAndLikeableType(userId, LikeableType.POST).stream()
                .flatMap(like -> {
                    Post post = postDao.findById(like.getLikeableId()).orElse(null);
                    return post != null ? post.getTopics().stream() : Stream.empty();
                })
                .map(Topic::getName)
                .collect(Collectors.toSet()));
        
        // Topics từ posts đã comment
        topics.addAll(commentDao.findByUserId(userId).stream()
                .flatMap(comment -> comment.getPost().getTopics().stream())
                .map(Topic::getName)
                .collect(Collectors.toSet()));
        
        // Topics từ posts đã share (shared posts có originalPostId)
        topics.addAll(postDao.findByUserId(userId).stream()
                .filter(post -> post.getOriginalPostId() != null) // Chỉ lấy shared posts
                .flatMap(sharedPost -> {
                    // Lấy topics từ original post
                    Post originalPost = postDao.findById(sharedPost.getOriginalPostId()).orElse(null);
                    return originalPost != null ? originalPost.getTopics().stream() : Stream.empty();
                })
                .map(Topic::getName)
                .collect(Collectors.toSet()));
        
        return topics;
    }
    
    /**
     * Lấy topics từ các bài viết của bạn bè
     */
    private Set<String> getFriendsActiveTopics(Long userId) {
        // Lấy danh sách bạn bè
        List<User> friends = friendshipDao.findFriendsOfWithProfile(userId);
        
        if (friends.isEmpty()) {
            return new HashSet<>();
        }
        
        // Lấy topics từ posts của bạn bè
        return friends.stream()
                .flatMap(friend -> postDao.findByUserId(friend.getId()).stream())
                .flatMap(post -> post.getTopics().stream())
                .map(Topic::getName)
                .collect(Collectors.toSet());
    }
    
    private double calculateTrendingScore(Post post) {
        // Tính điểm trending: Like(1) + Comment(2) + Share(3)
        long likeScore = post.getLikeCount() * 1;
        long commentScore = post.getCommentCount() * 2;
        long shareScore = post.getShareCount() * 3;
        
        return likeScore + commentScore + shareScore;
    }
    
    private Set<String> getUserInterests(Long userId) {
        // Lấy mối quan tâm từ posts user đã tương tác
        Set<String> interests = new HashSet<>();
        
        try {
        // Từ posts user đã like
        List<Post> likedPosts = likeDao.findPostsLikedByUser(userId);
            if (likedPosts != null) {
        for (Post post : likedPosts) {
                    if (post.getTopics() != null) {
            post.getTopics().forEach(topic -> interests.add(topic.getName()));
                    }
                }
        }
        
        // Từ posts user đã comment
        List<Post> commentedPosts = commentDao.findPostsCommentedByUser(userId);
            if (commentedPosts != null) {
        for (Post post : commentedPosts) {
                    if (post.getTopics() != null) {
                        post.getTopics().forEach(topic -> interests.add(topic.getName()));
                    }
                }
            }
            
            // Từ posts user đã đăng (topics từ posts của chính họ)
            List<Post> userPosts = postDao.findByUserId(userId);
            if (userPosts != null) {
                for (Post post : userPosts) {
                    if (post.getTopics() != null) {
            post.getTopics().forEach(topic -> interests.add(topic.getName()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting user interests: " + e.getMessage());
        }
        
        return interests;
    }
    
    private Map<Long, Double> getUserInteractionScores(Long userId) {
        Map<Long, Double> scores = new HashMap<>();
        
        // Tính điểm dựa trên tương tác trước đó
        List<Post> userPosts = postDao.findByUserId(userId);
        for (Post post : userPosts) {
            long engagement = post.getLikeCount() + post.getCommentCount() + post.getShareCount();
            scores.put(post.getId(), engagement * 0.05); // 0.05 điểm mỗi engagement
        }
        
        return scores;
    }
    
    private PostResponse convertToPostResponse(PostRecommendationScore scoredPost) {
        Post post = scoredPost.getPost();
        PostResponse response = new PostResponse();
        
        // Basic post info
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setCreatedAt(post.getCreatedAt());
        response.setStatus(post.getStatus());
        response.setPrivacy(post.getPrivacy());
        
        // Engagement counts
        response.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0L);
        response.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0L);
        response.setShareCount(post.getShareCount() != null ? post.getShareCount() : 0L);
        
        // User info
        response.setUserId(post.getUser().getId());
        response.setUserName(post.getUser().getUsername());
        
        // User avatar
        String userAvatar = null;
        if (post.getUser().getUserProfile() != null && 
            post.getUser().getUserProfile().getAvatar() != null && 
            !post.getUser().getUserProfile().getAvatar().trim().isEmpty()) {
            userAvatar = post.getUser().getUserProfile().getAvatar();
        } else {
            userAvatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
        }
        response.setUserAvatar(userAvatar);
        
        // Set topic names
        if (post.getTopics() != null && !post.getTopics().isEmpty()) {
            List<String> topicNames = post.getTopics().stream()
                    .map(Topic::getName)
                    .collect(Collectors.toList());
            response.setTopicNames(topicNames);
        } else {
            response.setTopicNames(new ArrayList<>());
        }
        
        // Set documents
        try {
            List<FileUploadResponse> documents = fileUploadService.getFilesByPostId(post.getId());
            response.setDocuments(documents != null ? documents : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error loading documents for post " + post.getId() + ": " + e.getMessage());
            response.setDocuments(new ArrayList<>());
        }
        
        // Set group information if post belongs to a group
        if (post.getGroup() != null) {
            response.setGroupId(post.getGroup().getId());
            response.setGroupName(post.getGroup().getName());
            response.setGroupDescription(post.getGroup().getDescription());
            response.setGroupMemberCount(post.getGroup().getMemberCount());
            
            // Group avatar
            if (post.getGroup().getAvatar() != null && !post.getGroup().getAvatar().trim().isEmpty()) {
                response.setGroupAvatar(post.getGroup().getAvatar());
            } else {
                response.setGroupAvatar("https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png");
            }
        }
        
        // Check if this is a shared post
        if (post.getOriginalPostId() != null) {
            try {
                Optional<Post> originalPost = postDao.findById(post.getOriginalPostId());
                if (originalPost.isPresent()) {
                    Map<String, Object> originalPostInfo = new HashMap<>();
                    originalPostInfo.put("id", originalPost.get().getId());
                    originalPostInfo.put("title", originalPost.get().getTitle());
                    originalPostInfo.put("content", originalPost.get().getContent());
                    originalPostInfo.put("userName", originalPost.get().getUser().getUsername());
                    originalPostInfo.put("createdAt", originalPost.get().getCreatedAt());
                    
                    // Original post user avatar
                    String originalUserAvatar = null;
                    if (originalPost.get().getUser().getUserProfile() != null && 
                        originalPost.get().getUser().getUserProfile().getAvatar() != null && 
                        !originalPost.get().getUser().getUserProfile().getAvatar().trim().isEmpty()) {
                        originalUserAvatar = originalPost.get().getUser().getUserProfile().getAvatar();
                    } else {
                        originalUserAvatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
                    }
                    originalPostInfo.put("userAvatar", originalUserAvatar);
                    
                    response.setOriginalPost(originalPostInfo);
                }
            } catch (Exception e) {
                System.err.println("Error loading original post: " + e.getMessage());
            }
        }
        
        // Set isLiked (will be set by controller if needed)
        response.setIsLiked(false);
        
        // Recommendation specific fields
        response.setRecommendationScore(scoredPost.getScore());
        
        // Kiểm tra xem có phải crawled content không
        boolean isCrawledContent = "admin".equals(post.getUser().getUsername());
        response.setIsCrawledContent(isCrawledContent);
        
        // Set user info for compatibility
        UserBasicDto userResponse = new UserBasicDto();
        userResponse.setId(post.getUser().getId());
        userResponse.setUsername(post.getUser().getUsername());
        userResponse.setFirstName(post.getUser().getFirstName());
        userResponse.setLastName(post.getUser().getLastName());
        response.setUser(userResponse);
        
        return response;
    }
    
    
    // Inner class để lưu điểm recommendation
    private static class PostRecommendationScore {
        private final Post post;
        private final double score;
        
        public PostRecommendationScore(Post post, double score) {
            this.post = post;
            this.score = score;
        }
        
        public Post getPost() { return post; }
        public double getScore() { return score; }
    }
}
