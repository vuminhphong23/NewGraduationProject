package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.GroupMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import GraduationProject.forumikaa.entity.User;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private PostService postService;
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private UserService userService;

    @Override
    public Map<String, Object> getPostStatistics(String startDate, String endDate) {
        List<Post> allPosts = postService.findAll();
        
        // Parse dates with specific format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        
        // Filter posts by date range
        List<Post> filteredPosts = filterPostsByDateRange(allPosts, start, end);
        
        // Group posts by day
        Map<String, Long> postCounts = groupPostsByDay(filteredPosts, start, end);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);
        statistics.put("data", postCounts);
        statistics.put("totalPosts", filteredPosts.size());
        
        return statistics;
    }

    @Override
    public Map<String, Object> getPostSummary() {
        List<Post> allPosts = postService.findAll();
        
        // Calculate summary statistics
        long totalPosts = allPosts.size();
        long todayPosts = allPosts.stream()
                .filter(post -> post.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .count();
        
        long thisWeekPosts = allPosts.stream()
                .filter(post -> {
                    LocalDate postDate = post.getCreatedAt().toLocalDate();
                    LocalDate weekStart = LocalDate.now().minusDays(6);
                    return !postDate.isBefore(weekStart) && !postDate.isAfter(LocalDate.now());
                })
                .count();
        
        long thisMonthPosts = allPosts.stream()
                .filter(post -> {
                    LocalDate postDate = post.getCreatedAt().toLocalDate();
                    LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
                    return !postDate.isBefore(monthStart) && !postDate.isAfter(LocalDate.now());
                })
                .count();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPosts", totalPosts);
        summary.put("todayPosts", todayPosts);
        summary.put("thisWeekPosts", thisWeekPosts);
        summary.put("thisMonthPosts", thisMonthPosts);
        
        return summary;
    }

    @Override
    public Map<String, Object> getPostDistributionByGroups(String startDate, String endDate) {
        try {
            System.out.println("DEBUG: Getting post distribution by groups - startDate: " + startDate + ", endDate: " + endDate);
            
            List<Post> allPosts = postService.findAll();
            System.out.println("DEBUG: Total posts found: " + allPosts.size());
            
            // Parse dates with specific format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);
            System.out.println("DEBUG: Parsed dates - start: " + start + ", end: " + end);
            
            // Filter posts by date range
            List<Post> filteredPosts = filterPostsByDateRange(allPosts, start, end);
            System.out.println("DEBUG: Filtered posts count: " + filteredPosts.size());
        
        // Group posts by group
        Map<String, Long> groupCounts = filteredPosts.stream()
                .filter(post -> post.getGroup() != null)
                .collect(Collectors.groupingBy(
                    post -> {
                        try {
                            return post.getGroup().getName();
                        } catch (Exception e) {
                            return "Unknown Group";
                        }
                    },
                    Collectors.counting()
                ));
        
        // Sort by count descending
        Map<String, Long> sortedGroupCounts = groupCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
        
            Map<String, Object> result = new HashMap<>();
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            result.put("data", sortedGroupCounts);
            result.put("totalPosts", filteredPosts.size());
            result.put("totalGroups", sortedGroupCounts.size());
            
            System.out.println("DEBUG: Group distribution result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in getPostDistributionByGroups: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<Map<String, Object>> getRecentActivities(String activityType, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        try {
            // Get recent posts
            if (activityType.equals("all") || activityType.equals("posts")) {
                List<Post> recentPosts = postService.findAll().stream()
                        .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                        .limit(limit)
                        .collect(Collectors.toList());
                
                for (Post post : recentPosts) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "post");
                    activity.put("id", post.getId());
                    activity.put("title", post.getTitle());
                    activity.put("userName", post.getUser().getUsername());
                    activity.put("userAvatar", getUserAvatar(post.getUser()));
                    activity.put("createdAt", post.getCreatedAt());
                    activity.put("description", "đã đăng bài viết mới" + 
                        (post.getGroup() != null ? " trong nhóm " + post.getGroup().getName() : ""));
                    activities.add(activity);
                }
            }
            
            // Get recent group activities
            if (activityType.equals("all") || activityType.equals("groups")) {
                List<UserGroup> recentGroups = groupService.findAll().stream()
                        .sorted((g1, g2) -> g2.getCreatedAt().compareTo(g1.getCreatedAt()))
                        .limit(limit)
                        .collect(Collectors.toList());
                
                for (UserGroup group : recentGroups) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "group");
                    activity.put("id", group.getId());
                    activity.put("title", group.getName());
                    activity.put("userName", group.getCreatedBy().getUsername());
                    activity.put("userAvatar", getUserAvatar(group.getCreatedBy()));
                    activity.put("createdAt", group.getCreatedAt());
                    activity.put("description", "đã tạo nhóm mới " + group.getName());
                    activities.add(activity);
                }
                
                // Get recent group member joins
                List<GroupMember> recentJoins = getRecentGroupJoins(limit);
                for (GroupMember member : recentJoins) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "group_join");
                    activity.put("id", member.getId());
                    activity.put("title", member.getGroup().getName());
                    activity.put("userName", member.getUser().getUsername());
                    activity.put("userAvatar", getUserAvatar(member.getUser()));
                    activity.put("createdAt", member.getJoinedAt());
                    activity.put("description", "đã tham gia nhóm " + member.getGroup().getName());
                    activities.add(activity);
                }
            }
            
            // Get recent user registrations
            if (activityType.equals("all") || activityType.equals("users")) {
                List<User> recentUsers = userService.findAll().stream()
                        .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                        .limit(limit)
                        .collect(Collectors.toList());
                
                for (User user : recentUsers) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "user");
                    activity.put("id", user.getId());
                    activity.put("title", user.getUsername());
                    activity.put("userName", user.getUsername());
                    activity.put("userAvatar", getUserAvatar(user));
                    activity.put("createdAt", user.getCreatedAt());
                    activity.put("description", "đã tạo tài khoản mới");
                    activities.add(activity);
                }
            }
            
            // Sort all activities by creation time
            activities.sort((a1, a2) -> {
                LocalDateTime time1 = (LocalDateTime) a1.get("createdAt");
                LocalDateTime time2 = (LocalDateTime) a2.get("createdAt");
                return time2.compareTo(time1);
            });
            
            // Limit results
            if (activities.size() > limit) {
                activities = activities.subList(0, limit);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting recent activities: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }

    @Override
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // Lấy dữ liệu hiện tại
            long totalUsers = userService.findAll().size();
            long totalPosts = postService.findAll().size();
            long totalGroups = groupService.findAll().size();
            
            // Tính toán dữ liệu tuần trước (tổng số đến cuối tuần trước)
            LocalDate now = LocalDate.now();
            LocalDate oneWeekAgo = now.minusWeeks(1);
            
            // Đếm users đến cuối tuần trước
            long usersLastWeek = userService.findAll().stream()
                    .filter(user -> {
                        LocalDate userCreatedAt = user.getCreatedAt().toLocalDate();
                        return userCreatedAt.isBefore(oneWeekAgo);
                    })
                    .count();
            
            // Đếm posts đến cuối tuần trước
            long postsLastWeek = postService.findAll().stream()
                    .filter(post -> {
                        LocalDate postCreatedAt = post.getCreatedAt().toLocalDate();
                        return postCreatedAt.isBefore(oneWeekAgo);
                    })
                    .count();
            
            // Đếm groups đến cuối tuần trước
            long groupsLastWeek = groupService.findAll().stream()
                    .filter(group -> {
                        LocalDate groupCreatedAt = group.getCreatedAt().toLocalDate();
                        return groupCreatedAt.isBefore(oneWeekAgo);
                    })
                    .count();
            
            // Tính phần trăm thay đổi: (hiện tại - tuần trước) / tuần trước * 100
            double userChangePercent = calculatePercentageChange(totalUsers, usersLastWeek);
            double postChangePercent = calculatePercentageChange(totalPosts, postsLastWeek);
            double groupChangePercent = calculatePercentageChange(totalGroups, groupsLastWeek);
            
            overview.put("totalUsers", totalUsers);
            overview.put("userChangePercent", userChangePercent);
            overview.put("totalPosts", totalPosts);
            overview.put("postChangePercent", postChangePercent);
            overview.put("totalGroups", totalGroups);
            overview.put("groupChangePercent", groupChangePercent);
            
            // Báo cáo - tính năng phát triển sau (giá trị cố định)
            overview.put("pendingReports", 5);
            overview.put("reportChangePercent", -2.5);
            
        } catch (Exception e) {
            System.err.println("Error getting dashboard overview: " + e.getMessage());
            
            // Dữ liệu mặc định
            overview.put("totalUsers", 0);
            overview.put("userChangePercent", 0.0);
            overview.put("totalPosts", 0);
            overview.put("postChangePercent", 0.0);
            overview.put("totalGroups", 0);
            overview.put("groupChangePercent", 0.0);
            overview.put("pendingReports", 0);
            overview.put("reportChangePercent", 0.0);
        }
        
        return overview;
    }
    
    private double calculatePercentageChange(long currentValue, long previousValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? 100.0 : 0.0;
        }
        double change = ((double) currentValue - previousValue) / previousValue * 100;
        return Math.round(change * 10.0) / 10.0;
    }
    
    private String getUserAvatar(User user) {
        if (user.getUserProfile() != null && user.getUserProfile().getAvatar() != null) {
            return user.getUserProfile().getAvatar();
        }
        return "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
    }
    
    private List<GroupMember> getRecentGroupJoins(int limit) {
        try {
            // This is a simplified approach - in a real implementation, 
            // you might want to add a method to GroupService to get recent joins
            List<GroupMember> allJoins = new ArrayList<>();
            
            // Get all groups and their members
            List<UserGroup> allGroups = groupService.findAll();
            for (UserGroup group : allGroups) {
                List<GroupMember> members = groupService.getGroupMembers(group.getId());
                allJoins.addAll(members);
            }
            
            // Sort by join date and limit
            return allJoins.stream()
                    .sorted((m1, m2) -> m2.getJoinedAt().compareTo(m1.getJoinedAt()))
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            System.err.println("Error getting recent group joins: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<Post> filterPostsByDateRange(List<Post> posts, LocalDate startDate, LocalDate endDate) {
        return posts.stream()
                .filter(post -> {
                    LocalDate postDate = post.getCreatedAt().toLocalDate();
                    return !postDate.isBefore(startDate) && !postDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }
    
    private Map<String, Long> groupPostsByDay(List<Post> posts, LocalDate startDate, LocalDate endDate) {
        Map<String, Long> counts = new HashMap<>();
        
        // Generate all dates in the range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            final LocalDate currentDate = current; // Make effectively final
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("dd/MM"));
            long count = posts.stream()
                    .filter(post -> post.getCreatedAt().toLocalDate().equals(currentDate))
                    .count();
            counts.put(dateStr, count);
            current = current.plusDays(1);
        }
        
        return counts;
    }
}
