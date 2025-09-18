package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.UserDisplayDto;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.dto.ProfileUpdateRequest;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserProfile;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.LikeableType;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.UserService;
import GraduationProject.forumikaa.service.FriendshipService;
import GraduationProject.forumikaa.service.LikeService;
import GraduationProject.forumikaa.service.CommentService;
import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

@Controller
public class ProfileController {

    private UserService userService;
    @Autowired private PostService postService;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private FriendshipService friendshipService;
    @Autowired private GroupMemberDao groupMemberDao;
    @Autowired private LikeService likeService;
    @Autowired private CommentService commentService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/{username}")
    public String profilePage(@PathVariable String username, 
                             @RequestParam(defaultValue = "posts") String tab,
                             Model model) {
        // Kiểm tra authentication trước
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

        // Lấy các bài viết của người dùng này
        List<PostResponse> userPosts = postService.getUserPosts(user.getId());
        
        // Lấy danh sách bạn bè thật từ database
        List<User> friendsList = friendshipService.listFriends(user.getId());
        List<UserDisplayDto> friends = friendsList.stream()
                .map(friend -> {
                    UserDisplayDto dto = new UserDisplayDto();
                    dto.setId(friend.getId());
                    dto.setUsername(friend.getUsername());
                    dto.setFirstName(friend.getUsername()); // UserProfile không có firstName, dùng username
                    dto.setLastName(""); // UserProfile không có lastName
                    dto.setAvatar(friend.getUserProfile() != null && friend.getUserProfile().getAvatar() != null ? 
                            friend.getUserProfile().getAvatar() : 
                            "https://ui-avatars.com/api/?name=" + friend.getUsername() + "&background=007bff&color=ffffff&size=60");
                    dto.setRole("FRIEND");
                    dto.setJoinedAt("2024-01-01"); // Có thể lấy từ friendship table nếu cần
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Lấy danh sách nhóm của user thật từ database
        List<GroupMember> groupMembers = groupMemberDao.findByUserId(user.getId());
        List<UserDisplayDto> userGroups = groupMembers.stream()
                .map(member -> {
                    UserDisplayDto dto = new UserDisplayDto();
                    dto.setId(member.getGroup().getId());
                    dto.setUsername(member.getGroup().getName());
                    dto.setFirstName(member.getGroup().getName());
                    dto.setLastName("");
                    dto.setAvatar(member.getGroup().getAvatar() != null ? 
                            member.getGroup().getAvatar() : 
                            "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60");
                    dto.setRole(member.getRole().name()); // Convert enum to string
                    dto.setJoinedAt(member.getJoinedAt().toString().substring(0, 10)); // Format date
                    dto.setMemberCount(postService.getNewPostCountByGroupToday(member.getGroup().getId()));
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Lấy tất cả tài liệu của user từ posts
        List<FileUploadResponse> userDocuments = userPosts.stream()
                .filter(post -> post.getDocuments() != null && !post.getDocuments().isEmpty())
                .flatMap(post -> post.getDocuments().stream()
                    .filter(doc -> doc.getFileName() != null)
                    .map(doc -> {
                        // Refine fileType based on file extension for better categorization
                        String fileName = doc.getFileName().toLowerCase();
                        String currentFileType = doc.getFileType();
                        
                        // Only refine if current fileType is generic "document"
                        if ("document".equals(currentFileType)) {
                            if (fileName.endsWith(".pdf")) {
                                doc.setFileType("pdf");
                            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                                doc.setFileType("doc");
                            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                                doc.setFileType("xls");
                            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                                doc.setFileType("ppt");
                            } else if (fileName.endsWith(".txt")) {
                                doc.setFileType("text");
                            }
                            // Keep "document" for other application/* types
                        }
                        // Keep existing fileType for "image", "video", "other"
                        
                        return doc;
                    }))
                .collect(Collectors.toList());
        
        // Không tạo data mẫu, sử dụng dữ liệu thật từ database
        
        model.addAttribute("user", user);
        model.addAttribute("posts", userPosts);
        model.addAttribute("friends", friends);
        model.addAttribute("userGroups", userGroups);
        model.addAttribute("userDocuments", userDocuments);
        model.addAttribute("postCount", userPosts.size());
        model.addAttribute("friendsCount", friends.size());
        model.addAttribute("groupsCount", userGroups.size());
        model.addAttribute("documentsCount", userDocuments.size());
        model.addAttribute("activeTab", tab);
        
        // Analytics data for analytics tab
        if ("analytics".equals(tab)) {
            Map<String, Object> analyticsData = new HashMap<>();

            // Calculate total likes, comments and shares that user has given (not received)
            Long totalLikes = likeService.getUserLikeCount(user.getId(), LikeableType.POST);
            Long totalComments = commentService.getUserCommentCount(user.getId());
            Long totalShares = postService.getUserShareCount(user.getId());

            analyticsData.put("totalPosts", userPosts.size());
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
            
            // Get groups activity data based on user's posts in each group (last 30 days)
            List<Map<String, Object>> groupsData = new ArrayList<>();
            java.time.LocalDate thirtyDaysAgo = java.time.LocalDate.now().minusDays(30);
            java.time.LocalDateTime startDateTime = thirtyDaysAgo.atStartOfDay();
            java.time.LocalDateTime endDateTime = java.time.LocalDate.now().atTime(23, 59, 59);
            
            for (UserDisplayDto group : userGroups) {
                // Count posts that user created in this group within last 30 days
                Long groupActivity = 0L;
                try {
                    List<PostResponse> userPostsInGroup = userPosts.stream()
                            .filter(post -> {
                                // Check if post was created in last 30 days
                                if (post.getCreatedAt() == null) return false;
                                java.time.LocalDateTime postDateTime = post.getCreatedAt();
                                if (postDateTime.isBefore(startDateTime) || postDateTime.isAfter(endDateTime)) {
                                    return false;
                                }
                                
                                // Check if post belongs to this group
                                return post.getGroupId() != null && post.getGroupId().equals(group.getId());
                            })
                            .collect(Collectors.toList());
                    
                    groupActivity = (long) userPostsInGroup.size();
                } catch (Exception e) {
                    // Fallback to member count if there's an error
                    groupActivity = group.getMemberCount() != null ? group.getMemberCount() : 0L;
                }
                
                Map<String, Object> groupMap = new HashMap<>();
                groupMap.put("name", group.getFirstName());
                groupMap.put("activity", groupActivity);
                groupsData.add(groupMap);
            }
            analyticsData.put("groupsData", groupsData);
            
            // Activity timeline (last 4 weeks) - real data based on post creation dates
            List<Map<String, Object>> timelineData = new ArrayList<>();
            
            // Calculate date ranges for last 4 weeks
            java.time.LocalDate today = java.time.LocalDate.now();
            for (int i = 3; i >= 0; i--) {
                java.time.LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
                java.time.LocalDate weekEnd = weekStart.plusDays(6);
                
                Map<String, Object> weekMap = new HashMap<>();
                weekMap.put("week", "Tuần " + (4 - i));
                weekMap.put("weekRange", weekStart.toString() + " - " + weekEnd.toString());
                
                // Count posts created in this week
                int weekPosts = 0;
                
                for (PostResponse post : userPosts) {
                    if (post.getCreatedAt() != null) {
                        java.time.LocalDate postDate = post.getCreatedAt().toLocalDate();
                        if (!postDate.isBefore(weekStart) && !postDate.isAfter(weekEnd)) {
                            weekPosts++;
                        }
                    }
                }
                
                // Count likes, comments and shares that user gave in this week
                java.time.LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
                java.time.LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
                Long weekLikes = likeService.getUserLikeCountInDateRange(user.getId(), LikeableType.POST, weekStartDateTime, weekEndDateTime);
                Long weekComments = commentService.getUserCommentCountInDateRange(user.getId(), weekStartDateTime, weekEndDateTime);
                Long weekShares = postService.getUserShareCountInDateRange(user.getId(), weekStartDateTime, weekEndDateTime);
                
                weekMap.put("posts", weekPosts);
                weekMap.put("likes", weekLikes);
                weekMap.put("comments", weekComments);
                weekMap.put("shares", weekShares);
                timelineData.add(weekMap);
            }
            analyticsData.put("timelineData", timelineData);
            
            model.addAttribute("analyticsData", analyticsData);
        }

        // Kiểm tra xem có phải profile của chính mình không
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            model.addAttribute("isOwnProfile", currentUserId.equals(user.getId()));
        } catch (Exception e) {
            model.addAttribute("isOwnProfile", false);
        }

        return "user/profile";
    }
    
    // API endpoints for analytics charts
    @GetMapping("/api/analytics/topics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTopicsAnalytics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // Validate date range
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Get current user
            User user = securityUtil.getCurrentUser();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get user posts in date range
            List<PostResponse> userPosts = getUserPostsInDateRange(user.getId(), startDate, endDate);
            
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
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu topics: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/api/analytics/groups")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGroupsAnalytics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // Validate date range
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Get current user
            User user = securityUtil.getCurrentUser();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get user groups from database
            List<GroupMember> groupMembers = groupMemberDao.findByUserId(user.getId());
            List<UserDisplayDto> userGroups = groupMembers.stream()
                    .map(member -> {
                        UserDisplayDto dto = new UserDisplayDto();
                        dto.setId(member.getGroup().getId());
                        dto.setUsername(member.getGroup().getName());
                        dto.setFirstName(member.getGroup().getName());
                        dto.setLastName("");
                        dto.setAvatar(member.getGroup().getAvatar() != null ? 
                                member.getGroup().getAvatar() : 
                                "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60");
                        dto.setRole(member.getRole().name());
                        dto.setJoinedAt(member.getJoinedAt().toString().substring(0, 10));
                        // Use member count as activity metric
                        dto.setMemberCount(member.getGroup().getMemberCount() != null ? member.getGroup().getMemberCount() : 0);
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            // Calculate groups activity data based on user's posts in each group
            List<Map<String, Object>> groupsData = new ArrayList<>();
            for (UserDisplayDto group : userGroups) {
                // Count posts that user created in this group within date range
                Long groupActivity = 0L;
                try {
                    java.time.LocalDateTime startDateTime = java.time.LocalDate.parse(startDate).atStartOfDay();
                    java.time.LocalDateTime endDateTime = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                    
                    // Get user posts in this group within date range
                    List<PostResponse> userPostsInGroup = postService.getUserPosts(user.getId()).stream()
                            .filter(post -> {
                                // Check if post was created in date range
                                if (post.getCreatedAt() == null) return false;
                                java.time.LocalDateTime postDateTime = post.getCreatedAt();
                                if (postDateTime.isBefore(startDateTime) || postDateTime.isAfter(endDateTime)) {
                                    return false;
                                }
                                
                                // Check if post belongs to this group
                                return post.getGroupId() != null && post.getGroupId().equals(group.getId());
                            })
                            .collect(Collectors.toList());
                    
                    groupActivity = (long) userPostsInGroup.size();
                } catch (Exception e) {
                    // Fallback to member count if there's an error
                    groupActivity = group.getMemberCount() != null ? group.getMemberCount() : 0L;
                }
                
                Map<String, Object> groupMap = new HashMap<>();
                groupMap.put("name", group.getFirstName());
                groupMap.put("activity", groupActivity);
                groupsData.add(groupMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("groupsData", groupsData);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu groups: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/api/analytics/activity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActivityAnalytics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // Validate date range
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Get current user
            User user = securityUtil.getCurrentUser();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get user posts in date range
            List<PostResponse> userPosts = getUserPostsInDateRange(user.getId(), startDate, endDate);
            
            // Calculate activity timeline
            List<Map<String, Object>> timelineData = new ArrayList<>();
            
            // Use already parsed dates from validation above
            
            // Always show daily data for better granularity
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            daysBetween = Math.min(daysBetween, 30); // Limit to 30 days max for performance
            
            for (int i = 0; i < daysBetween; i++) {
                java.time.LocalDate currentDate = start.plusDays(i);
                
                Map<String, Object> dayMap = new HashMap<>();
                String dayLabel = String.format("%d/%d", currentDate.getDayOfMonth(), currentDate.getMonthValue());
                dayMap.put("week", dayLabel);
                dayMap.put("weekRange", currentDate.toString());
                
                // Count posts created on this day
                int dayPosts = 0;
                
                for (PostResponse post : userPosts) {
                    if (post.getCreatedAt() != null) {
                        java.time.LocalDate postDate = post.getCreatedAt().toLocalDate();
                        if (postDate.equals(currentDate)) {
                            dayPosts++;
                        }
                    }
                }
                
                // Count likes, comments and shares that user gave on this day
                java.time.LocalDateTime dayStartDateTime = currentDate.atStartOfDay();
                java.time.LocalDateTime dayEndDateTime = currentDate.atTime(23, 59, 59);
                Long dayLikes = likeService.getUserLikeCountInDateRange(user.getId(), LikeableType.POST, dayStartDateTime, dayEndDateTime);
                Long dayComments = commentService.getUserCommentCountInDateRange(user.getId(), dayStartDateTime, dayEndDateTime);
                Long dayShares = postService.getUserShareCountInDateRange(user.getId(), dayStartDateTime, dayEndDateTime);
                
                dayMap.put("posts", dayPosts);
                dayMap.put("likes", dayLikes);
                dayMap.put("comments", dayComments);
                dayMap.put("shares", dayShares);
                timelineData.add(dayMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("timelineData", timelineData);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu activity: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // Helper method to get user posts in date range
    private List<PostResponse> getUserPostsInDateRange(Long userId, String startDate, String endDate) {
        try {
            // Get all user posts first
            List<PostResponse> allUserPosts = postService.getUserPosts(userId);
            
            // Filter by date range
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            
            return allUserPosts.stream()
                .filter(post -> {
                    if (post.getCreatedAt() == null) return false;
                    java.time.LocalDate postDate = post.getCreatedAt().toLocalDate();
                    return !postDate.isBefore(start) && !postDate.isAfter(end);
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Return empty list if error
            return new ArrayList<>();
        }
    }

    @GetMapping("/profile/edit/{username}")
    public String editProfilePage(@PathVariable String username, Model model) {
        // Kiểm tra authentication trước
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + username));
        
        // Đảm bảo UserProfile được khởi tạo nếu chưa có
        if (user.getUserProfile() == null) {
            UserProfile userProfile = new UserProfile(user);
            user.setUserProfile(userProfile);
        }
        
        model.addAttribute("user", user);
        return "user/profile-edit";
    }


    // REST API endpoint for profile update
    @PutMapping("/api/profile/update")
    public ResponseEntity<?> updateProfileApi(@RequestBody ProfileUpdateRequest request) {
        try {
            // Lấy user hiện tại từ security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            String username = authentication.getName();
            User existingUser = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Cập nhật thông tin cơ bản
            if (request.getFirstName() != null) {
                existingUser.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                existingUser.setLastName(request.getLastName());
            }
            if (request.getEmail() != null) {
                existingUser.setEmail(request.getEmail());
            }
            if (request.getPhone() != null) {
                existingUser.setPhone(request.getPhone());
            }
            if (request.getAddress() != null) {
                existingUser.setAddress(request.getAddress());
            }
            if (request.getBirthDate() != null && !request.getBirthDate().trim().isEmpty()) {
                existingUser.setBirthDate(java.time.LocalDate.parse(request.getBirthDate()));
            }
            if (request.getGender() != null) {
                existingUser.setGender(request.getGender());
            }
            if (request.getProfileInfo() != null) {
                existingUser.setProfileInfo(request.getProfileInfo());
            }

            // Cập nhật UserProfile
            if (existingUser.getUserProfile() == null) {
                UserProfile newProfile = new UserProfile(existingUser);
                existingUser.setUserProfile(newProfile);
            }

            if (request.getBio() != null) {
                existingUser.getUserProfile().setBio(request.getBio());
            }
            if (request.getAvatar() != null) {
                existingUser.getUserProfile().setAvatar(request.getAvatar());
            }
            if (request.getCover() != null) {
                existingUser.getUserProfile().setCover(request.getCover());
            }
            if (request.getSocialLinks() != null) {
                existingUser.getUserProfile().setSocialLinks(request.getSocialLinks());
            }

            userService.save(existingUser);

            return ResponseEntity.ok().body("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }
}