package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.FriendshipStatus;
import GraduationProject.forumikaa.service.FriendshipService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import GraduationProject.forumikaa.entity.User;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired private FriendshipService friendshipService;
    @Autowired private SecurityUtil securityUtil;

    @PostMapping("/request/{targetUserId}")
    public ResponseEntity<Map<String, String>> sendFriendRequest(@PathVariable Long targetUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        friendshipService.sendFriendRequest(currentUserId, targetUserId);
        return ResponseEntity.ok(Map.of("message", "Yêu cầu kết bạn đã được gửi"));
    }

    @PostMapping("/accept/{requesterId}")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(@PathVariable Long requesterId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        friendshipService.acceptFriendRequest(currentUserId, requesterId);
        return ResponseEntity.ok(Map.of("message", "Đã chấp nhận yêu cầu kết bạn"));
    }

    @PostMapping("/decline/{requesterId}")
    public ResponseEntity<Map<String, String>> declineFriendRequest(@PathVariable Long requesterId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        friendshipService.declineFriendRequest(currentUserId, requesterId);
        return ResponseEntity.ok(Map.of("message", "Đã từ chối yêu cầu kết bạn"));
    }

    @PostMapping("/cancel/{targetUserId}")
    public ResponseEntity<Map<String, String>> cancelFriendRequest(@PathVariable Long targetUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        friendshipService.cancelFriendRequest(currentUserId, targetUserId);
        return ResponseEntity.ok(Map.of("message", "Đã hủy yêu cầu kết bạn"));
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Map<String, String>> unfriend(@PathVariable Long friendUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        friendshipService.unfriend(currentUserId, friendUserId);
        return ResponseEntity.ok(Map.of("message", "Đã hủy kết bạn"));
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getFriendsList() {
        Long currentUserId = securityUtil.getCurrentUserId();
        List<User> friends = friendshipService.listFriends(currentUserId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : friends) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            String fullName = ((u.getFirstName() != null ? u.getFirstName() : "") +
                    (u.getLastName() != null ? " " + u.getLastName() : "")).trim();
            m.put("fullName", fullName.isEmpty() ? u.getUsername() : fullName);
            String avatar = null;
            if (u.getUserProfile() != null && u.getUserProfile().getAvatar() != null && !u.getUserProfile().getAvatar().trim().isEmpty()) {
                avatar = u.getUserProfile().getAvatar();
            }
            m.put("avatar", avatar);
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests() {
        Long currentUserId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(friendshipService.listPendingRequests(currentUserId));
    }

    @GetMapping("/status/{targetUserId}")
    public ResponseEntity<Map<String, Object>> getFriendshipStatus(@PathVariable Long targetUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Optional<Friendship> friendship = friendshipService.getFriendshipBetween(currentUserId, targetUserId);
        
        Map<String, Object> response = new HashMap<>();
        if (friendship.isPresent()) {
            response.put("status", friendship.get().getStatus().name());
            
            // Kiểm tra xem current user có phải là người gửi yêu cầu không
            if (friendship.get().getStatus() == FriendshipStatus.PENDING) {
                boolean requestedByMe = friendship.get().getUser().getId().equals(currentUserId);
                response.put("requestedByMe", requestedByMe);
            }
        } else {
            response.put("status", "NONE");
        }
        
        return ResponseEntity.ok(response);
    }
}


