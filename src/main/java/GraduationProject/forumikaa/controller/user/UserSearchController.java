package GraduationProject.forumikaa.controller.user;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.FriendshipService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    @Autowired private UserDao userDao;
    @Autowired private FriendshipService friendshipService;
    @Autowired private SecurityUtil securityUtil;

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(name = "q", required = false) String keyword
    ) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("email")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("firstName")), like.toLowerCase()),
                    cb.like(cb.lower(root.get("lastName")), like.toLowerCase()),
                    cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), like.toLowerCase())
            ));
        }

        // exclude self
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("id"), currentUserId));

        List<User> result = userDao.findAll(spec);

        List<Map<String, Object>> users = result.stream().map(u -> {

            Optional<Friendship> fs = friendshipService.getFriendshipBetween(currentUserId, u.getId());
            String friendshipStatus = fs.map(f -> f.getStatus().name()).orElse("NONE");
            Long requesterId = fs.map(f -> f.getUser().getId()).orElse(null);
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("firstName", u.getFirstName());
            m.put("lastName", u.getLastName());
            m.put("email", u.getEmail());
            m.put("friendshipStatus", friendshipStatus);
            m.put("requestedByMe", requesterId != null && requesterId.equals(currentUserId));
            
            // Lấy avatar từ UserProfile - xử lý LazyInitializationException
            String avatar = null;
            try {
                if (u.getUserProfile() != null && u.getUserProfile().getAvatar() != null && !u.getUserProfile().getAvatar().trim().isEmpty()) {
                    avatar = u.getUserProfile().getAvatar();
                } else {
                    // Sử dụng avatar mặc định
                    avatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
                }
            } catch (Exception e) {
                // LazyInitializationException hoặc lỗi khác - sử dụng avatar mặc định
                System.err.println("Error accessing UserProfile for user " + u.getUsername() + ": " + e.getMessage());
                avatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
            }
            m.put("avatar", avatar);
            
            return m;
        }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "totalElements", users.size(),
                    "items", users
            ));
        } catch (Exception e) {
            System.err.println("Error in UserSearchController.search(): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }

    @GetMapping("/suggested")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getSuggestedUsers(
            @RequestParam(defaultValue = "5") int limit
    ) {
        Long currentUserId = securityUtil.getCurrentUserId();
        System.out.println("Getting suggested users for current user ID: " + currentUserId);
        
        // Tạo Pageable với limit
        Pageable pageable = PageRequest.of(0, limit);
        
        // Lấy danh sách người dùng gợi ý
        // Logic: người dùng chưa kết bạn, sắp xếp ngẫu nhiên
        List<User> suggestedUsers = userDao.findSuggestedUsers(currentUserId, pageable);
        System.out.println("Found " + suggestedUsers.size() + " suggested users from database");
        
        List<Map<String, Object>> result = suggestedUsers.stream().map(u -> {
            System.out.println("Processing user: " + u.getUsername() + " (ID: " + u.getId() + ")");
            System.out.println("User profile: " + (u.getUserProfile() != null ? "exists" : "null"));
            if (u.getUserProfile() != null) {
                System.out.println("Avatar from profile: " + u.getUserProfile().getAvatar());
            }
            Map<String, Object> userMap = new java.util.HashMap<>();
            userMap.put("id", u.getId());
            userMap.put("username", u.getUsername());
            userMap.put("fullName", (u.getFirstName() != null ? u.getFirstName() : "") + 
                               (u.getLastName() != null ? " " + u.getLastName() : "").trim());
            
            // Lấy avatar từ UserProfile - xử lý LazyInitializationException
            String avatar = null;
            try {
                if (u.getUserProfile() != null && u.getUserProfile().getAvatar() != null && !u.getUserProfile().getAvatar().trim().isEmpty()) {
                    avatar = u.getUserProfile().getAvatar();
                    System.out.println("User " + u.getUsername() + " has avatar: " + avatar);
                } else {
                    System.out.println("User " + u.getUsername() + " has no avatar or empty avatar");
                    // Sử dụng avatar mặc định từ Pixabay
                    avatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
                }
            } catch (Exception e) {
                System.out.println("Error accessing UserProfile for user " + u.getUsername() + ": " + e.getMessage());
                // Sử dụng avatar mặc định từ Pixabay
                avatar = "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png";
            }
            userMap.put("avatar", avatar);
            
            // Đếm số bạn chung (có thể implement sau)
            int mutualFriends = 0; // TODO: implement mutual friends count
            userMap.put("mutualFriends", mutualFriends);
            
            // Kiểm tra trạng thái kết bạn
            Optional<Friendship> fs = friendshipService.getFriendshipBetween(currentUserId, u.getId());
            String friendshipStatus = fs.map(f -> f.getStatus().name()).orElse("NONE");
            Long requesterId = fs.map(f -> f.getUser().getId()).orElse(null);
            userMap.put("friendshipStatus", friendshipStatus);
            userMap.put("requestedByMe", requesterId != null && requesterId.equals(currentUserId));
            
            return userMap;
        }).collect(Collectors.toList());
        
        System.out.println("Returning " + result.size() + " suggested users");
        return ResponseEntity.ok(result);
    }
}


