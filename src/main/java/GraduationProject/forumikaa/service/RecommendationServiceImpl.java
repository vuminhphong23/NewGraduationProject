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
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired private PostDao postDao;
    @Autowired private UserDao userDao;
    @Autowired private LikeDao likeDao;
    @Autowired private CommentDao commentDao;
    @Autowired private FriendshipDao friendshipDao;


    @Override
    public List<UserRecommendationResponse> recommendUsers(Long userId, Integer limit) {
        List<UserRecommendationResponse> recommendations = new ArrayList<>();
        
        // Gợi ý dựa trên sở thích (bao gồm cả tương tác chung) (40%)
        recommendations.addAll(recommendUsersByInterests(userId, limit * 2 / 5));
        
        // Gợi ý dựa trên bạn bè chung (30%)
        recommendations.addAll(recommendUsersByMutualFriends(userId, limit * 3 / 10));
        
        // Gợi ý dựa trên tương tác gần đây (30%)
        recommendations.addAll(recommendUsersByRecentInteractions(userId, limit * 3 / 10));
        
        return recommendations.stream()
                .distinct()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRecommendationResponse> recommendUsersByMutualFriends(Long userId, Integer limit) {
        System.out.println("=== USER RECOMMENDATION BY MUTUAL FRIENDS ===");
        System.out.println("User ID: " + userId);
        
        List<User> allUsers = userDao.findAll();
        List<User> friends = getFriends(userId);
        Set<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toSet());
        friendIds.add(userId); // Thêm chính user để loại trừ
        
        System.out.println("User has " + friends.size() + " friends");
        
        List<UserRecommendationResponse> recommendations = new ArrayList<>();
        
        for (User user : allUsers) {
            if (!friendIds.contains(user.getId())) {
                int mutualFriendsCount = countMutualFriends(userId, user.getId());
                if (mutualFriendsCount > 0) {
                    // Chỉ dựa trên số lượng bạn chung
                    double score = mutualFriendsCount * 1.0; // Trọng số 100% cho bạn chung
                    
                    String reason = "Có " + mutualFriendsCount + " bạn chung";
                    
                    recommendations.add(UserRecommendationResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .userAvatar(getUserAvatar(user))
                            .createdAt(user.getCreatedAt())
                            .score(score)
                            .reason(reason)
                            .mutualFriendsCount(mutualFriendsCount)
                            .recommendationType("MUTUAL_FRIENDS_BASED")
                            .build());
                }
            }
        }
        
        System.out.println("Mutual friends-based user recommendations: " + recommendations.size());
        return recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRecommendationResponse> recommendUsersByInterests(Long userId, Integer limit) {
        System.out.println("=== USER RECOMMENDATION BY INTERESTS ===");
        System.out.println("User ID: " + userId);
        
        // Lấy topics user quan tâm từ tương tác và posts đã đăng
        Set<String> userInterests = getUserInterests(userId);
        Set<String> userPostedTopics = getUserPostedTopics(userId);
        Set<String> allUserTopics = new HashSet<>();
        allUserTopics.addAll(userInterests);
        allUserTopics.addAll(userPostedTopics);
        
        System.out.println("User interests from interactions: " + userInterests);
        System.out.println("User posted topics: " + userPostedTopics);
        System.out.println("All user topics: " + allUserTopics);
        
        List<User> allUsers = userDao.findAll();
        List<User> friends = getFriends(userId);
        Set<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toSet());
        friendIds.add(userId);
        
        List<UserRecommendationResponse> recommendations = new ArrayList<>();
        
        for (User user : allUsers) {
            if (!friendIds.contains(user.getId())) {
                // Lấy topics của user khác
                Set<String> otherUserInterests = getUserInterests(user.getId());
                Set<String> otherUserPostedTopics = getUserPostedTopics(user.getId());
                Set<String> allOtherUserTopics = new HashSet<>();
                allOtherUserTopics.addAll(otherUserInterests);
                allOtherUserTopics.addAll(otherUserPostedTopics);
                
                // Tính topics chung
                List<String> commonTopics = allUserTopics.stream()
                        .filter(allOtherUserTopics::contains)
                        .collect(Collectors.toList());
                
                // Tính tương tác chung
                int commonInteractions = countCommonInteractions(userId, user.getId());
                
                // Tính điểm tổng hợp
                double score = 0.0;
                String reason = "";
                
                if (!commonTopics.isEmpty() && commonInteractions > 0) {
                    // Có cả topics chung và tương tác chung
                    score = (commonTopics.size() * 1.0) + (commonInteractions * 0.5);
                    reason = "Chung sở thích: " + commonTopics.size() + " topics, tương tác chung: " + commonInteractions + " posts";
                } else if (!commonTopics.isEmpty()) {
                    // Chỉ có topics chung
                    score = commonTopics.size() * 1.0;
                    reason = "Chung sở thích: " + commonTopics.size() + " topics";
                } else if (commonInteractions > 0) {
                    // Chỉ có tương tác chung
                    score = commonInteractions * 0.5;
                    reason = "Tương tác chung: " + commonInteractions + " posts";
                }
                
                if (score > 0) {
                    recommendations.add(UserRecommendationResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .userAvatar(getUserAvatar(user))
                            .createdAt(user.getCreatedAt())
                            .score(score)
                            .reason(reason)
                            .commonTopics(commonTopics)
                            .recommendationType("INTEREST_BASED")
                            .build());
                }
            }
        }
        
        System.out.println("Interest-based user recommendations: " + recommendations.size());
        return recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    private Set<Long> getInteractedPostIds(Long userId) {
        Set<Long> postIds = new HashSet<>();
        
        try {
            // Lấy posts user đã like
            List<Like> likes = likeDao.findByUserIdAndLikeableType(userId, LikeableType.POST);
            if (likes != null) {
                postIds.addAll(likes.stream()
                    .filter(like -> like.getLikeableId() != null)
                    .map(Like::getLikeableId)
                    .collect(Collectors.toSet()));
            }
            
            // Lấy posts user đã comment
            List<Comment> comments = commentDao.findByUserId(userId);
            if (comments != null) {
                postIds.addAll(comments.stream()
                    .filter(comment -> comment.getPost() != null && comment.getPost().getId() != null)
                    .map(comment -> comment.getPost().getId())
                    .collect(Collectors.toSet()));
            }
            
            // Lấy posts user đã tạo
            List<Post> userPosts = postDao.findByUserIdOrderByCreatedAtDesc(userId);
            if (userPosts != null) {
                postIds.addAll(userPosts.stream()
                    .filter(post -> post.getId() != null)
                    .map(Post::getId)
                    .collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            System.err.println("Error getting interacted post IDs: " + e.getMessage());
        }
        
        return postIds;
    }
    
    private Set<String> getUserInterests(Long userId) {
        Set<String> interests = new HashSet<>();
        
        try {
            // Lấy topics từ posts user đã tương tác
            List<Post> userPosts = postDao.findByUserIdOrderByCreatedAtDesc(userId);
            if (userPosts != null) {
                for (Post post : userPosts) {
                    if (post != null && post.getTopics() != null) {
                        interests.addAll(post.getTopics().stream()
                                .filter(topic -> topic != null && topic.getName() != null)
                                .map(Topic::getName)
                                .collect(Collectors.toSet()));
                    }
                }
            }
            
            // Lấy topics từ posts user đã like
            List<Like> likes = likeDao.findByUserIdAndLikeableType(userId, LikeableType.POST);
            if (likes != null) {
                for (Like like : likes) {
                    if (like != null && like.getLikeableId() != null) {
                        Optional<Post> post = postDao.findById(like.getLikeableId());
                        if (post.isPresent() && post.get().getTopics() != null) {
                            interests.addAll(post.get().getTopics().stream()
                                    .filter(topic -> topic != null && topic.getName() != null)
                                    .map(Topic::getName)
                                    .collect(Collectors.toSet()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting user interests: " + e.getMessage());
        }
        
        return interests;
    }

    
    private Set<String> getUserPostedTopics(Long userId) {
        Set<String> postedTopics = new HashSet<>();
        
        try {
            // Lấy tất cả posts user đã đăng
            List<Post> userPosts = postDao.findByUserIdOrderByCreatedAtDesc(userId);
            if (userPosts != null) {
                for (Post post : userPosts) {
                    if (post != null && post.getTopics() != null) {
                        postedTopics.addAll(post.getTopics().stream()
                                .filter(topic -> topic != null && topic.getName() != null)
                                .map(Topic::getName)
                                .collect(Collectors.toSet()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting user posted topics: " + e.getMessage());
        }
        
        return postedTopics;
    }
    

    
    private List<User> getFriends(Long userId) {
        List<Friendship> friendships = friendshipDao.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);
        System.out.println("Found " + friendships.size() + " accepted friendships for user " + userId);
        
        List<User> friends = friendships.stream()
                .map(Friendship::getFriend)
                .collect(Collectors.toList());
        
        System.out.println("Friends list: " + friends.stream().map(User::getUsername).collect(Collectors.toList()));
        return friends;
    }
    
    private int countMutualFriends(Long userId1, Long userId2) {
        List<User> friends1 = getFriends(userId1);
        List<User> friends2 = getFriends(userId2);
        
        Set<Long> friendIds1 = friends1.stream().map(User::getId).collect(Collectors.toSet());
        Set<Long> friendIds2 = friends2.stream().map(User::getId).collect(Collectors.toSet());
        
        friendIds1.retainAll(friendIds2);
        return friendIds1.size();
    }
    
    private String getUserAvatar(User user) {
        if (user.getUserProfile() != null && 
            user.getUserProfile().getAvatar() != null && 
            !user.getUserProfile().getAvatar().trim().isEmpty()) {
            return user.getUserProfile().getAvatar();
        }
        return "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
    }

    
    /**
     * Gợi ý users dựa trên tương tác gần đây (like/comment posts của user)
     */
    public List<UserRecommendationResponse> recommendUsersByRecentInteractions(Long userId, Integer limit) {
        System.out.println("=== USER RECOMMENDATION BY RECENT INTERACTIONS ===");
        System.out.println("User ID: " + userId);
        
        List<User> allUsers = userDao.findAll();
        List<User> friends = getFriends(userId);
        Set<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toSet());
        friendIds.add(userId);
        
        List<UserRecommendationResponse> recommendations = new ArrayList<>();
        
        for (User user : allUsers) {
            if (!friendIds.contains(user.getId())) {
                // Đếm số lần user này đã tương tác với posts của userId
                int recentInteractions = countRecentInteractions(userId, user.getId());
                
                if (recentInteractions > 0) {
                    double score = recentInteractions * 1.0; // Trọng số 100% cho tương tác gần đây
                    
                    String reason = "Quan tâm đến bạn: " + recentInteractions + " lần";
                    
                    recommendations.add(UserRecommendationResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .userAvatar(getUserAvatar(user))
                            .createdAt(user.getCreatedAt())
                            .score(score)
                            .reason(reason)
                            .recommendationType("RECENT_INTERACTIONS_BASED")
                            .build());
                }
            }
        }
        
        System.out.println("Recent interactions-based user recommendations: " + recommendations.size());
        return recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Đếm số posts chung mà cả hai user đã tương tác
     */
    private int countCommonInteractions(Long userId1, Long userId2) {
        try {
            // Lấy posts user1 đã tương tác
            Set<Long> user1InteractedPosts = getInteractedPostIds(userId1);
            
            // Lấy posts user2 đã tương tác
            Set<Long> user2InteractedPosts = getInteractedPostIds(userId2);
            
            // Tìm giao của hai tập hợp
            user1InteractedPosts.retainAll(user2InteractedPosts);
            
            return user1InteractedPosts.size();
        } catch (Exception e) {
            System.err.println("Error counting common interactions: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Đếm số lần user2 đã tương tác với posts của user1
     */
    private int countRecentInteractions(Long targetUserId, Long interactingUserId) {
        try {
            int count = 0;
            
            // Lấy tất cả posts của targetUserId
            List<Post> targetUserPosts = postDao.findByUserIdOrderByCreatedAtDesc(targetUserId);
            
            for (Post post : targetUserPosts) {
                // Kiểm tra xem interactingUserId có like post này không
                Optional<Like> like = likeDao.findByUserIdAndLikeableIdAndLikeableType(
                    interactingUserId, post.getId(), LikeableType.POST);
                if (like.isPresent()) {
                    count++;
                }
                
                // Đếm comments của interactingUserId cho post này
                List<Comment> comments = commentDao.findByPostIdOrderByCreatedAtAsc(post.getId());
                for (Comment comment : comments) {
                    if (comment.getUser().getId().equals(interactingUserId)) {
                        count++;
                    }
                }
            }
            
            return count;
        } catch (Exception e) {
            System.err.println("Error counting recent interactions: " + e.getMessage());
            return 0;
        }
    }
}
