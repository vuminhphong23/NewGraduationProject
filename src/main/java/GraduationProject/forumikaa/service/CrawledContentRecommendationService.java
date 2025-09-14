package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.*;
import GraduationProject.forumikaa.dto.*;
import GraduationProject.forumikaa.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CrawledContentRecommendationService {
    
    @Autowired private PostDao postDao;
    @Autowired private LikeDao likeDao;
    @Autowired private CommentDao commentDao;
    @Autowired private TopicDao topicDao;
    @Autowired
    private TopicService topicService;

    /**
     * Gợi ý crawled content dựa trên mối quan tâm và tương tác của user
     */
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
        
        // 1. Điểm dựa trên mối quan tâm (40%)
        Set<String> postTopics = post.getTopics().stream()
                .map(Topic::getName)
                .collect(Collectors.toSet());
        
        long commonTopics = postTopics.stream()
                .mapToLong(topic -> userInterests.contains(topic) ? 1 : 0)
                .sum();
        
        if (commonTopics > 0) {
            score += (commonTopics * 2.0); // 2 điểm mỗi topic chung
        }
        
        // 2. Điểm dựa trên engagement (30%)
        long totalEngagement = post.getLikeCount() + post.getCommentCount() + post.getShareCount();
        score += (totalEngagement * 0.1); // 0.1 điểm mỗi engagement
        
        // 3. Điểm dựa trên lịch sử tương tác (20%)
        if (userInteractionScores.containsKey(post.getId())) {
            score += userInteractionScores.get(post.getId());
        }
        
        // 4. Điểm dựa trên thời gian (10%) - ưu tiên nội dung mới
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                post.getCreatedAt().toLocalDate(), 
                java.time.LocalDate.now()
        );
        score += Math.max(0, 10 - daysSinceCreation); // Giảm dần theo thời gian
        
        return score;
    }
    
    private Set<String> getUserInterests(Long userId) {
        // Lấy mối quan tâm từ posts user đã tương tác
        Set<String> interests = new HashSet<>();
        
        // Từ posts user đã like
        List<Post> likedPosts = likeDao.findPostsLikedByUser(userId);
        for (Post post : likedPosts) {
            post.getTopics().forEach(topic -> interests.add(topic.getName()));
        }
        
        // Từ posts user đã comment
        List<Post> commentedPosts = commentDao.findPostsCommentedByUser(userId);
        for (Post post : commentedPosts) {
            post.getTopics().forEach(topic -> interests.add(topic.getName()));
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
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setShareCount(post.getShareCount());
        response.setCreatedAt(post.getCreatedAt());
        response.setRecommendationScore(scoredPost.getScore()); // Thêm điểm recommendation
        response.setIsCrawledContent(true); // Đánh dấu là crawled content
        
        // Set user info
        UserResponse userResponse = new UserResponse();
        userResponse.setId(post.getUser().getId());
        userResponse.setUsername(post.getUser().getUsername());
        userResponse.setFirstName(post.getUser().getFirstName());
        userResponse.setLastName(post.getUser().getLastName());
        response.setUser(userResponse);
        
        // Set legacy fields for compatibility
        response.setUserId(post.getUser().getId());
        response.setUserName(post.getUser().getFirstName() + " " + post.getUser().getLastName());
        
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
