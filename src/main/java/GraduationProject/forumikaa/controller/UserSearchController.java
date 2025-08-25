package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.FriendshipService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(root.get("username"), like),
                    cb.like(root.get("email"), like),
                    cb.like(root.get("firstName"), like),
                    cb.like(root.get("lastName"), like)
            ));
        }

        // exclude self
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("id"), currentUserId));

        Page<User> result = userDao.findAll(spec, pageable);

        List<Map<String, Object>> users = result.getContent().stream().map(u -> {
            Optional<Friendship> fs = friendshipService.getFriendshipBetween(currentUserId, u.getId());
            String friendshipStatus = fs.map(f -> f.getStatus().name()).orElse("NONE");
            Long requesterId = fs.map(f -> f.getUser().getId()).orElse(null);
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("firstName", u.getFirstName());
            m.put("lastName", u.getLastName());
            m.put("friendshipStatus", friendshipStatus);
            m.put("requestedByMe", requesterId != null && requesterId.equals(currentUserId));
            
            // Lấy avatar từ UserProfile
            String avatar = null;
            if (u.getUserProfile() != null && u.getUserProfile().getAvatar() != null) {
                avatar = u.getUserProfile().getAvatar();
            }
            m.put("avatar", avatar);
            
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "items", users
        ));
    }
}


