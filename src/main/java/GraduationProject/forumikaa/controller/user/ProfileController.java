package GraduationProject.forumikaa.controller.user;
import GraduationProject.forumikaa.dto.*;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserProfile;
import GraduationProject.forumikaa.service.UserService;
import GraduationProject.forumikaa.service.ProfileDataService;
import GraduationProject.forumikaa.service.ProfileAnalyticsService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;

@Controller
public class ProfileController {

    private UserService userService;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private ProfileDataService profileDataService;
    @Autowired private ProfileAnalyticsService profileAnalyticsService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/{username}")
    public String profilePage(@PathVariable String username, 
                             @RequestParam(defaultValue = "posts") String tab,
                             Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

        // Lấy tất cả dữ liệu profile từ service
        ProfileDataService.ProfileDataResult profileData = profileDataService.getProfileData(user);
        
        // Set model attributes
        model.addAttribute("user", user);
        model.addAttribute("posts", profileData.getUserPosts());
        model.addAttribute("friends", profileData.getFriends());
        model.addAttribute("userGroups", profileData.getUserGroups());
        model.addAttribute("userDocuments", profileData.getUserDocuments());
        model.addAttribute("postCount", profileData.getPostCount());
        model.addAttribute("friendsCount", profileData.getFriendsCount());
        model.addAttribute("groupsCount", profileData.getGroupsCount());
        model.addAttribute("documentsCount", profileData.getDocumentsCount());
        model.addAttribute("activeTab", tab);
        
        // Analytics data will be loaded via API when tab is accessed

        // Kiểm tra xem có phải profile của chính mình không
        model.addAttribute("isOwnProfile", isOwnProfile(user.getId()));

        return "user/profile";
    }
    
    
    // API endpoints for analytics charts
    @GetMapping("/api/analytics/topics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTopicsAnalytics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            
            if (!isValidDateRange(startDate, endDate)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = securityUtil.getCurrentUser();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> result = profileAnalyticsService.getTopicsAnalytics(user.getId(), startDate, endDate);
            
            if (result.containsKey("error")) {
                return ResponseEntity.status(500).body(result);
            }
            
            return ResponseEntity.ok(result);
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
            if (!isValidDateRange(startDate, endDate)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = securityUtil.getCurrentUser();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> result = profileAnalyticsService.getGroupsAnalytics(user.getId(), startDate, endDate);
            
            if (result.containsKey("error")) {
                return ResponseEntity.status(500).body(result);
            }
            
            return ResponseEntity.ok(result);
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

            if (!isValidDateRange(startDate, endDate)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = securityUtil.getCurrentUser();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> result = profileAnalyticsService.getActivityAnalytics(user.getId(), startDate, endDate);
            
            if (result.containsKey("error")) {
                return ResponseEntity.status(500).body(result);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi tải dữ liệu activity: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    
    private boolean isOwnProfile(Long userId) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            return currentUserId.equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidDateRange(String startDate, String endDate) {
        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return !start.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/profile/edit/{username}")
    public String editProfilePage(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + username));
        
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
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User existingUser = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            updateUserBasicInfo(existingUser, request);
            updateUserProfile(existingUser, request);

            userService.save(existingUser);

            return ResponseEntity.ok().body("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }
    
    // Helper methods for profile update
    private void updateUserBasicInfo(User user, ProfileUpdateRequest request) {
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getBirthDate() != null && !request.getBirthDate().trim().isEmpty()) {
            user.setBirthDate(java.time.LocalDate.parse(request.getBirthDate()));
        }
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getProfileInfo() != null) user.setProfileInfo(request.getProfileInfo());
    }
    
    private void updateUserProfile(User user, ProfileUpdateRequest request) {
        if (user.getUserProfile() == null) {
            user.setUserProfile(new UserProfile(user));
        }
        
        UserProfile profile = user.getUserProfile();
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getAvatar() != null) profile.setAvatar(request.getAvatar());
        if (request.getCover() != null) profile.setCover(request.getCover());
        if (request.getSocialLinks() != null) profile.setSocialLinks(request.getSocialLinks());
    }
}