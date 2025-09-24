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
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired private PostDao postDao;
    @Autowired private UserDao userDao;
    @Autowired private LikeDao likeDao;
    @Autowired private CommentDao commentDao;
    @Autowired private FriendshipDao friendshipDao;
    @Autowired private GroupDao groupDao;
    @Autowired private GroupMemberDao groupMemberDao;


    @Override
    public List<UserRecommendationResponse> recommendUsers(Long userId, Integer limit) {
        List<UserRecommendationResponse> recommendations = new ArrayList<>();
        
        // Gợi ý dựa trên mối quan tâm chung
        recommendations.addAll(recommendUsersByInterests(userId, limit));
        
        // Gợi ý dựa trên bạn bè chung
        recommendations.addAll(recommendUsersByMutualFriends(userId, limit));
        
        // Gợi ý dựa trên tương tác gần đây
        recommendations.addAll(recommendUsersByRecentInteractions(userId, limit));
        
        return recommendations.stream()
                .distinct()
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
                    String reason = mutualFriendsCount + " bạn chung";
                    
                    recommendations.add(UserRecommendationResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .userAvatar(getUserAvatar(user))
                            .createdAt(user.getCreatedAt())
                            .reason(reason)
                            .mutualFriendsCount(mutualFriendsCount)
                            .recommendationType("MUTUAL_FRIENDS_BASED")
                            .build());
                }
            }
        }
        
        System.out.println("Mutual friends-based user recommendations: " + recommendations.size());
        return recommendations.stream()
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
                
                // Tính tương tác chung (cùng like/comment trên posts không phải của chính mình)
                int commonInteractions = countCommonInteractionsOnOthersPosts(userId, user.getId());
                
                // Đơn giản hóa logic gợi ý
                String reason = "";
                
                if (!commonTopics.isEmpty() && commonInteractions > 0) {
                    reason = "Cùng quan tâm: " + commonTopics.size() + " topics, tương tác chung: " + commonInteractions + " posts";
                } else if (!commonTopics.isEmpty()) {
                    reason = "Cùng quan tâm: " + commonTopics.size() + " topics";
                } else if (commonInteractions > 0) {
                    reason = "Tương tác chung: " + commonInteractions + " posts";
                }
                
                if (!reason.isEmpty()) {
                    recommendations.add(UserRecommendationResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .userAvatar(getUserAvatar(user))
                            .createdAt(user.getCreatedAt())
                            .reason(reason)
                            .commonTopics(commonTopics)
                            .recommendationType("INTEREST_BASED")
                            .build());
                }
            }
        }
        
        System.out.println("Interest-based user recommendations: " + recommendations.size());
        return recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }


    
    /**
     * Lấy danh sách posts user đã tương tác (loại trừ posts của chính họ)
     */
    private Set<Long> getInteractedPostIdsExcludingOwn(Long userId) {
        Set<Long> postIds = new HashSet<>();
        
        try {
            // Lấy posts user đã like (loại trừ posts của chính họ)
            List<Like> likes = likeDao.findByUserIdAndLikeableType(userId, LikeableType.POST);
            if (likes != null) {
                for (Like like : likes) {
                    if (like.getLikeableId() != null) {
                        // Kiểm tra xem post này có phải của chính user không
                        Optional<Post> post = postDao.findById(like.getLikeableId());
                        if (post.isPresent() && !post.get().getUser().getId().equals(userId)) {
                            postIds.add(like.getLikeableId());
                        }
                    }
                }
            }
            
            // Lấy posts user đã comment (loại trừ posts của chính họ)
            List<Comment> comments = commentDao.findByUserId(userId);
            if (comments != null) {
                for (Comment comment : comments) {
                    if (comment.getPost() != null && comment.getPost().getId() != null) {
                        // Kiểm tra xem post này có phải của chính user không
                        if (!comment.getPost().getUser().getId().equals(userId)) {
                            postIds.add(comment.getPost().getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting interacted post IDs excluding own: " + e.getMessage());
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
        // Sử dụng method đã có sẵn trong FriendshipDao để lấy tất cả bạn bè (cả 2 chiều)
        List<User> friends = friendshipDao.findFriendsOfWithProfile(userId);
        System.out.println("Found " + friends.size() + " accepted friendships for user " + userId);
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
                    String reason = "Quan tâm đến bạn: " + recentInteractions + " lần";
                    
                    recommendations.add(UserRecommendationResponse.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .userAvatar(getUserAvatar(user))
                            .createdAt(user.getCreatedAt())
                            .reason(reason)
                            .recommendationType("RECENT_INTERACTIONS_BASED")
                            .build());
                }
            }
        }
        
        System.out.println("Recent interactions-based user recommendations: " + recommendations.size());
        return recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Đếm số posts chung mà cả hai user đã tương tác (không phải posts của chính họ)
     */
    private int countCommonInteractionsOnOthersPosts(Long userId1, Long userId2) {
        try {
            // Lấy posts user1 đã tương tác (loại trừ posts của chính họ)
            Set<Long> user1InteractedPosts = getInteractedPostIdsExcludingOwn(userId1);
            
            // Lấy posts user2 đã tương tác (loại trừ posts của chính họ)
            Set<Long> user2InteractedPosts = getInteractedPostIdsExcludingOwn(userId2);
            
            // Tìm giao của hai tập hợp
            user1InteractedPosts.retainAll(user2InteractedPosts);
            
            return user1InteractedPosts.size();
        } catch (Exception e) {
            System.err.println("Error counting common interactions on others' posts: " + e.getMessage());
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
    
    @Override
    public List<GroupRecommendationResponse> recommendGroups(Long userId, Integer limit) {
        List<GroupRecommendationResponse> recommendations = new ArrayList<>();
        
        try {
            // Lấy tất cả groups từ database
            List<Group> allGroups = groupDao.findAll();
            
            // Lấy danh sách bạn bè của user
            Set<Long> friendIds = getFriendIds(userId);
            
            for (Group group : allGroups) {
                // Kiểm tra user đã tham gia group chưa
                if (isUserInGroup(userId, group.getId())) {
                    continue;
                }
                
                // Kiểm tra điều kiện khớp
                if (isGroupRelevant(userId, group, friendIds)) {
                    GroupRecommendationResponse response = GroupRecommendationResponse.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .description(group.getDescription())
                            .avatar(group.getAvatar())
                            .createdAt(group.getCreatedAt())
                            .memberCount(getGroupMemberCount(group.getId()))
                            .commonFriendsCount((int) getMutualFriendsInGroup(userId, group.getId(), friendIds))
                            .topics(getGroupTopics(group))
                            // Không cần tính điểm
                            .reason(getRelevanceReason(userId, group))
                            .recommendationType(getRecommendationType(userId, group, friendIds))
                            .isJoined(false)
                            .build();
                    
                    recommendations.add(response);
                }
            }
            
            return recommendations.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            System.err.println("Error in recommendGroups: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
    /**
     * Kiểm tra group có khớp điều kiện gợi ý không
     */
    private boolean isGroupRelevant(Long userId, Group group, Set<Long> friendIds) {
        try {
            // 1. Có topics chung với user
            if (hasCommonTopics(userId, group)) {
                return true;
            }
            
            // 2. Có bạn bè chung trong group
            long mutualFriends = getMutualFriendsInGroup(userId, group.getId(), friendIds);
            if (mutualFriends > 0) {
                return true;
            }
            
            // 3. Group có nhiều thành viên (phổ biến)
            long memberCount = getGroupMemberCount(group.getId());
            if (memberCount > 20) {
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Error checking group relevance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Lấy danh sách bạn bè của user
     */
    private Set<Long> getFriendIds(Long userId) {
        try {
            List<User> friends = friendshipDao.findFriendsOf(userId);
            return friends.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            System.err.println("Error getting friend IDs: " + e.getMessage());
            return new HashSet<>();
        }
    }
    
    /**
     * Lấy topics của group
     */
    private List<String> getGroupTopics(Group group) {
        try {
            return group.getTopics().stream()
                    .map(Topic::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting group topics: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
    /**
     * Xác định loại recommendation
     */
    private String getRecommendationType(Long userId, Group group, Set<Long> friendIds) {
        boolean hasCommonTopics = hasCommonTopics(userId, group);
        long mutualFriends = getMutualFriendsInGroup(userId, group.getId(), friendIds);
        
        if (hasCommonTopics && mutualFriends > 0) {
            return "INTEREST_BASED";
        } else if (mutualFriends > 0) {
            return "MUTUAL_FRIENDS";
        } else if (hasCommonTopics) {
            return "INTEREST_BASED";
        } else {
            return "POPULAR";
        }
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
     * Kiểm tra user đã tham gia group chưa
     */
    private boolean isUserInGroup(Long userId, Long groupId) {
        return groupMemberDao.existsByGroupIdAndUserId(groupId, userId);
    }
    
    /**
     * Lấy số lượng thành viên của group
     */
    private long getGroupMemberCount(Long groupId) {
        return groupMemberDao.countByGroupId(groupId);
    }
    
    
    /**
     * Đếm số bạn bè chung trong group
     */
    private long getMutualFriendsInGroup(Long userId, Long groupId, Set<Long> friendIds) {
        // Lấy tất cả members của group
        List<GroupMember> groupMembers = groupMemberDao.findByGroupId(groupId);
        
        // Đếm số bạn bè trong group
        return groupMembers.stream()
                .mapToLong(member -> friendIds.contains(member.getUser().getId()) ? 1 : 0)
                .sum();
    }
    
    
    /**
     * Kiểm tra có topics chung không
     */
    private boolean hasCommonTopics(Long userId, Group group) {
        // Lấy topics của user
        Set<String> userTopics = getUserInterests(userId);
        userTopics.addAll(getUserInteractionTopics(userId));
        
        // Lấy topics của group
        Set<String> groupTopics = group.getTopics().stream()
                .map(Topic::getName)
                .collect(Collectors.toSet());
        
        // Kiểm tra có topics chung không
        return userTopics.stream().anyMatch(groupTopics::contains);
    }
    
    /**
     * Kiểm tra có bạn bè trong group không
     */
    private boolean hasFriendsInGroup(Long userId, Long groupId) {
        // Lấy danh sách bạn bè
        List<User> friends = friendshipDao.findFriendsOfWithProfile(userId);
        Set<Long> friendIds = friends.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        
        // Kiểm tra có bạn bè trong group không
        return getMutualFriendsInGroup(userId, groupId, friendIds) > 0;
    }
    
    /**
     * Lấy lý do gợi ý
     */
    private String getRelevanceReason(Long userId, Group group) {
        if (hasCommonTopics(userId, group)) {
            return "Cùng quan tâm";
        }
        if (hasFriendsInGroup(userId, group.getId())) {
            return "Có bạn bè trong nhóm";
        }
        if (getGroupMemberCount(group.getId()) > 10) {
            return "Nhóm phổ biến";
        }
        return "Được gợi ý";
    }
    
}
