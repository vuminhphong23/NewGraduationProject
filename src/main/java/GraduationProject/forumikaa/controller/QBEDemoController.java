package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.UserExample;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo Controller để showcase Query By Example (QBE) functionality
 */
@RestController
@RequestMapping("/api/qbe")
public class QBEDemoController {

    @Autowired
    private UserService userService;

    /**
     * Tìm kiếm user bằng QBE với matching linh hoạt
     */
    @PostMapping("/users/search")
    public ResponseEntity<List<User>> searchUsersByExample(@RequestBody UserExample example) {
        List<User> users = userService.findByExample(example);
        return ResponseEntity.ok(users);
    }

    /**
     * Tìm kiếm user bằng QBE với matching chính xác
     */
    @PostMapping("/users/search/exact")
    public ResponseEntity<List<User>> searchUsersByExampleExact(@RequestBody UserExample example) {
        List<User> users = userService.findByExampleExact(example);
        return ResponseEntity.ok(users);
    }

    /**
     * Tìm kiếm user bằng QBE với pagination
     */
    @PostMapping("/users/search/paged")
    public ResponseEntity<Page<User>> searchUsersByExamplePaged(
            @RequestBody UserExample example,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userService.findByExample(example, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Đếm số lượng user theo example
     */
    @PostMapping("/users/count")
    public ResponseEntity<Map<String, Long>> countUsersByExample(@RequestBody UserExample example) {
        long count = userService.countByExample(example);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Kiểm tra xem có user nào thỏa mãn example không
     */
    @PostMapping("/users/exists")
    public ResponseEntity<Map<String, Boolean>> existsUserByExample(@RequestBody UserExample example) {
        boolean exists = userService.existsByExample(example);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Demo các trường hợp sử dụng QBE
     */
    @GetMapping("/users/demo")
    public ResponseEntity<Map<String, Object>> demoQBE() {
        // Demo 1: Tìm user có username chứa "user"
        UserExample example1 = new UserExample();
        example1.setUsername("user");
        List<User> usersWithUsername = userService.findByExample(example1);

        // Demo 2: Tìm user có email chứa "@example.com"
        UserExample example2 = new UserExample();
        example2.setEmail("@example.com");
        List<User> usersWithEmail = userService.findByExample(example2);

        // Demo 3: Tìm user có firstName chứa "Admin"
        UserExample example3 = new UserExample();
        example3.setFirstName("Admin");
        List<User> usersWithFirstName = userService.findByExample(example3);

        // Demo 4: Tìm user enabled
        UserExample example4 = new UserExample();
        example4.setEnabled(true);
        List<User> enabledUsers = userService.findByExample(example4);

        return ResponseEntity.ok(Map.of(
                "usersWithUsername", usersWithUsername,
                "usersWithEmail", usersWithEmail,
                "usersWithFirstName", usersWithFirstName,
                "enabledUsers", enabledUsers
        ));
    }
}


