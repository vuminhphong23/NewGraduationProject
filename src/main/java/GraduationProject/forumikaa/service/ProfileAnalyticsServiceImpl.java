package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.GroupMemberDto;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.LikeableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileAnalyticsServiceImpl implements ProfileAnalyticsService {

    @Autowired private LikeService likeService;
    @Autowired private CommentService commentService;
    @Autowired private PostService postService;
    @Autowired private GroupMemberDao groupMemberDao;

    @Override
    public Map<String, Object> calculateAnalyticsData(Long userId, List<PostResponse> userPosts, List<GroupMemberDto> userGroups) {
        Map<String, Object> analyticsData = new HashMap<>();

        // Calculate posts in last 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long postsInLast30Days = userPosts.stream()
                .filter(post -> post.getCreatedAt() != null && 
                        post.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .count();
        
        // Calculate likes, comments and shares that user has given in last 30 days
        LocalDateTime thirtyDaysAgoDateTime = thirtyDaysAgo.atStartOfDay();
        LocalDateTime nowDateTime = LocalDateTime.now();
        
        Long totalLikes = likeService.getUserLikeCountInDateRange(userId, LikeableType.POST, thirtyDaysAgoDateTime, nowDateTime);
        Long totalComments = commentService.getUserCommentCountInDateRange(userId, thirtyDaysAgoDateTime, nowDateTime);
        Long totalShares = postService.getUserShareCountInDateRange(userId, thirtyDaysAgoDateTime, nowDateTime);
        
        analyticsData.put("totalPosts", (int) postsInLast30Days);
        analyticsData.put("totalComments", totalComments != null ? totalComments.intValue() : 0);
        analyticsData.put("totalLikes", totalLikes != null ? totalLikes.intValue() : 0);
        analyticsData.put("totalShares", totalShares != null ? totalShares.intValue() : 0);
        
        // Get topics data from posts
        Map<String, Integer> topicCounts = new HashMap<>();
        for (PostResponse post : userPosts) {
            if (post.getTopicNames() != null && !post.getTopicNames().isEmpty()) {
                for (String topic : post.getTopicNames()) {
                    String topicKey = "#" + topic.toLowerCase().replace(" ", "_");
                    topicCounts.put(topicKey, topicCounts.getOrDefault(topicKey, 0) + 1);
                }
            }
        }
        
        // Convert to list and sort by count
        List<Map<String, Object>> topicsData = new ArrayList<>();
        if (!topicCounts.isEmpty()) {
            topicsData = topicCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> topicMap = new HashMap<>();
                    topicMap.put("name", entry.getKey());
                    topicMap.put("count", entry.getValue());
                    return topicMap;
                })
                .collect(Collectors.toList());
        }
        analyticsData.put("topicsData", topicsData);
        
        // Get groups activity data
        List<Map<String, Object>> groupsData = calculateGroupsActivity(userPosts, userGroups);
        analyticsData.put("groupsData", groupsData);
        
        // Activity timeline (last 4 weeks)
        List<Map<String, Object>> timelineData = calculateActivityTimeline(userId, userPosts);
        analyticsData.put("timelineData", timelineData);
        
        return analyticsData;
    }

    @Override
    public Map<String, Object> getTopicsAnalytics(Long userId, String startDate, String endDate) {
        try {
            // Get user posts in date range
            List<PostResponse> userPosts = getUserPostsInDateRange(userId, startDate, endDate);
            
            // Calculate topics data
            Map<String, Integer> topicCounts = new HashMap<>();
            for (PostResponse post : userPosts) {
                if (post.getTopicNames() != null) {
                    for (String topic : post.getTopicNames()) {
                        topicCounts.put(topic, topicCounts.getOrDefault(topic, 0) + 1);
                    }
                }
            }
            
            List<Map<String, Object>> topicsData = new ArrayList<>();
            if (!topicCounts.isEmpty()) {
                topicsData = topicCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> topicMap = new HashMap<>();
                        topicMap.put("name", entry.getKey());
                        topicMap.put("count", entry.getValue());
                        return topicMap;
                    })
                    .collect(Collectors.toList());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("topicsData", topicsData);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu topics: " + e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> getGroupsAnalytics(Long userId, String startDate, String endDate) {
        try {
            // Get user groups from database
            List<GroupMember> groupMembers = groupMemberDao.findByUserId(userId);
            List<GroupMemberDto> userGroups = groupMembers.stream()
                    .map(member -> GroupMemberDto.builder()
                            .id(member.getGroup().getId())
                            .userId(member.getUser().getId())
                            .username(member.getGroup().getName())
                            .firstName(member.getGroup().getName())
                            .lastName("")
                            .fullName(member.getGroup().getName())
                            .avatar(member.getGroup().getAvatar() != null ? 
                                    member.getGroup().getAvatar() : 
                                    "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60")
                            .role(member.getRole().name())
                            .isOnline(false)
                            .joinedAt(member.getJoinedAt())
                            .memberCount(member.getGroup().getMemberCount() != null ? member.getGroup().getMemberCount() : 0)
                            .postCount(0L)
                            .build())
                    .collect(Collectors.toList());
            
            // Get user posts in date range
            List<PostResponse> userPosts = getUserPostsInDateRange(userId, startDate, endDate);
            
            // Calculate groups activity data
            List<Map<String, Object>> groupsData = new ArrayList<>();
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            
            for (GroupMemberDto group : userGroups) {
                Long groupActivity = userPosts.stream()
                        .filter(post -> {
                            if (post.getCreatedAt() == null) return false;
                            LocalDateTime postDateTime = post.getCreatedAt();
                            if (postDateTime.isBefore(startDateTime) || postDateTime.isAfter(endDateTime)) {
                                return false;
                            }
                            return post.getGroupId() != null && post.getGroupId().equals(group.getId());
                        })
                        .count();
                
                Map<String, Object> groupMap = new HashMap<>();
                groupMap.put("name", group.getFirstName());
                groupMap.put("activity", groupActivity);
                groupsData.add(groupMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupsData", groupsData);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu groups: " + e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> getActivityAnalytics(Long userId, String startDate, String endDate) {
        try {
            // Get user posts in date range
            List<PostResponse> userPosts = getUserPostsInDateRange(userId, startDate, endDate);
            System.out.println("DEBUG: Found " + userPosts.size() + " posts for user " + userId + " between " + startDate + " and " + endDate);
            
            // Calculate activity timeline
            List<Map<String, Object>> timelineData = new ArrayList<>();
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            // Always show daily data for better granularity
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            daysBetween = Math.min(daysBetween, 30); // Limit to 30 days max for performance
            
            for (int i = 0; i < daysBetween; i++) {
                LocalDate currentDate = start.plusDays(i);
                
                Map<String, Object> dayMap = new HashMap<>();
                String dayLabel = String.format("%d/%d", currentDate.getDayOfMonth(), currentDate.getMonthValue());
                dayMap.put("week", dayLabel);
                dayMap.put("weekRange", currentDate.toString());
                
                // Count posts created on this day
                int dayPosts = (int) userPosts.stream()
                        .filter(post -> {
                            if (post.getCreatedAt() == null) return false;
                            LocalDate postDate = post.getCreatedAt().toLocalDate();
                            return postDate.equals(currentDate);
                        })
                        .count();
                
                // Count likes, comments and shares that user gave on this day
                LocalDateTime dayStartDateTime = currentDate.atStartOfDay();
                LocalDateTime dayEndDateTime = currentDate.atTime(23, 59, 59);
                Long dayLikes = likeService.getUserLikeCountInDateRange(userId, LikeableType.POST, dayStartDateTime, dayEndDateTime);
                Long dayComments = commentService.getUserCommentCountInDateRange(userId, dayStartDateTime, dayEndDateTime);
                Long dayShares = postService.getUserShareCountInDateRange(userId, dayStartDateTime, dayEndDateTime);
                
                dayMap.put("posts", dayPosts);
                dayMap.put("likes", dayLikes != null ? dayLikes.intValue() : 0);
                dayMap.put("comments", dayComments != null ? dayComments.intValue() : 0);
                dayMap.put("shares", dayShares != null ? dayShares.intValue() : 0);
                timelineData.add(dayMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("timelineData", timelineData);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu activity: " + e.getMessage());
            return error;
        }
    }

    // Helper methods
    private List<Map<String, Object>> calculateGroupsActivity(List<PostResponse> userPosts, List<GroupMemberDto> userGroups) {
        List<Map<String, Object>> groupsData = new ArrayList<>();
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDateTime startDateTime = thirtyDaysAgo.atStartOfDay();
        LocalDateTime endDateTime = LocalDate.now().atTime(23, 59, 59);
        
        for (GroupMemberDto group : userGroups) {
            Long groupActivity = userPosts.stream()
                    .filter(post -> {
                        if (post.getCreatedAt() == null) return false;
                        LocalDateTime postDateTime = post.getCreatedAt();
                        if (postDateTime.isBefore(startDateTime) || postDateTime.isAfter(endDateTime)) {
                            return false;
                        }
                        return post.getGroupId() != null && post.getGroupId().equals(group.getId());
                    })
                    .count();
            
            Map<String, Object> groupMap = new HashMap<>();
            groupMap.put("name", group.getFirstName());
            groupMap.put("activity", groupActivity);
            groupsData.add(groupMap);
        }
        
        return groupsData;
    }

    private List<Map<String, Object>> calculateActivityTimeline(Long userId, List<PostResponse> userPosts) {
        List<Map<String, Object>> timelineData = new ArrayList<>();
        
        // Calculate date ranges for last 4 weeks
        LocalDate today = LocalDate.now();
        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            Map<String, Object> weekMap = new HashMap<>();
            weekMap.put("week", "Tuần " + (4 - i));
            weekMap.put("weekRange", weekStart.toString() + " - " + weekEnd.toString());
            
            // Count posts created in this week
            int weekPosts = (int) userPosts.stream()
                    .filter(post -> {
                        if (post.getCreatedAt() == null) return false;
                        LocalDate postDate = post.getCreatedAt().toLocalDate();
                        return !postDate.isBefore(weekStart) && !postDate.isAfter(weekEnd);
                    })
                    .count();
            
            // Count likes, comments and shares that user gave in this week
            LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
            LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
            Long weekLikes = likeService.getUserLikeCountInDateRange(userId, LikeableType.POST, weekStartDateTime, weekEndDateTime);
            Long weekComments = commentService.getUserCommentCountInDateRange(userId, weekStartDateTime, weekEndDateTime);
            Long weekShares = postService.getUserShareCountInDateRange(userId, weekStartDateTime, weekEndDateTime);
            
            weekMap.put("posts", weekPosts);
            weekMap.put("likes", weekLikes);
            weekMap.put("comments", weekComments);
            weekMap.put("shares", weekShares);
            timelineData.add(weekMap);
        }
        
        return timelineData;
    }

    private List<PostResponse> getUserPostsInDateRange(Long userId, String startDate, String endDate) {
        try {
            List<PostResponse> allUserPosts = postService.getUserPosts(userId);
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            return allUserPosts.stream()
                .filter(post -> {
                    if (post.getCreatedAt() == null) return false;
                    LocalDate postDate = post.getCreatedAt().toLocalDate();
                    return !postDate.isBefore(start) && !postDate.isAfter(end);
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
