package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.UserExample;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Demo Controller Method Level Security với @PreAuthorize
@RestController
@RequestMapping("/api/method-security")
public class MethodSecurityDemoController {

    @Autowired
    private UserService userService;


    //Method chỉ dành cho ADMIN
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminOnlyMethod() {
        return ResponseEntity.ok(Map.of(
                "message", "Chỉ ADMIN mới có thể truy cập method này",
                "role", "ADMIN"
        ));
    }

    //Method dành cho cả USER và ADMIN
    @GetMapping("/user-or-admin")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> userOrAdminMethod() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(Map.of(
                "message", "USER hoặc ADMIN có thể truy cập method này",
                "currentUser", auth.getName(),
                "authorities", auth.getAuthorities().toString()
        ));
    }


    //Method sử dụng SpEL expression
    @GetMapping("/spel-demo")
    @PreAuthorize("hasRole('ADMIN') and #userType == 'admin'")
    public ResponseEntity<Map<String, String>> spelDemoMethod(@RequestParam String userType) {
        return ResponseEntity.ok(Map.of(
                "message", "SpEL expression demo",
                "userType", userType,
                "access", "granted"
        ));
    }

    //Method với custom permission check
    @GetMapping("/custom-permission/{resourceId}")
    @PreAuthorize("@securityService.hasPermission(#resourceId, 'READ')")
    public ResponseEntity<Map<String, Object>> customPermissionMethod(@PathVariable Long resourceId) {
        return ResponseEntity.ok(Map.of(
                "message", "Custom permission check passed",
                "resourceId", resourceId,
                "permission", "READ"
        ));
    }

    //Method sử dụng QBE với method security
    @PostMapping("/users/search-secure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchUsersSecure(@RequestBody UserExample example) {
        List<User> users = userService.findByExample(example);
        return ResponseEntity.ok(users);
    }


}







