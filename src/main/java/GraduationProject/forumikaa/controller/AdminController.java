package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.GroupMemberRole;
import GraduationProject.forumikaa.service.RoleService;
import GraduationProject.forumikaa.service.UserService;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.GroupService;
import GraduationProject.forumikaa.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    // Static maps for status and privacy mapping
    private static final Map<PostStatus, String> STATUS_TEXT_MAP = Map.of(
        PostStatus.PENDING, "Chờ duyệt",
        PostStatus.APPROVED, "Đã duyệt", 
        PostStatus.REJECTED, "Từ chối"
    );
    
    private static final Map<PostStatus, String> STATUS_CLASS_MAP = Map.of(
        PostStatus.PENDING, "badge badge-status-pending",
        PostStatus.APPROVED, "badge badge-status-approved",
        PostStatus.REJECTED, "badge badge-status-rejected"
    );
    
    private static final Map<PostPrivacy, String> PRIVACY_TEXT_MAP = Map.of(
        PostPrivacy.PUBLIC, "Công khai",
        PostPrivacy.PRIVATE, "Riêng tư",
        PostPrivacy.FRIENDS, "Bạn bè"
    );
    
    private static final Map<PostPrivacy, String> PRIVACY_CLASS_MAP = Map.of(
        PostPrivacy.PUBLIC, "badge badge-privacy-public",
        PostPrivacy.PRIVATE, "badge badge-privacy-private", 
        PostPrivacy.FRIENDS, "badge badge-privacy-friends"
    );

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


    private RoleService roleService;

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    private PostService postService;

    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    private GroupService groupService;

    @Autowired
    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }



    private SecurityUtil securityUtil;

    @Autowired
    public void setSecurityUtil(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        long totalUsers = userService.findAll().size();
        long totalPosts = postService.findAll().size();
        long totalGroups = groupService.findAll().size();
        long pendingPosts = postService.findAll().stream()
                .filter(post -> post.getStatus() == PostStatus.PENDING)
                .count();
        long approvedPosts = postService.findAll().stream()
                .filter(post -> post.getStatus() == PostStatus.APPROVED)
                .count();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("totalGroups", totalGroups);
        model.addAttribute("pendingPosts", pendingPosts);
        model.addAttribute("approvedPosts", approvedPosts);
        return "admin/admin";
    }

    @GetMapping("/admin/users")
    public String userManagementPage(Model model,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false, defaultValue = "") String roleName,
                                     @RequestParam(required = false, defaultValue = "") String status) {
        Page<User> userPage = userService.findPaginated(keyword, status, roleName, PageRequest.of(page - 1, size));
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("roleName", roleName);
        return "admin/user-management";
    }

    @PostMapping("/admin/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.updateUserEnabledStatus(id, !userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với id " + id)).isEnabled());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa user thành công.");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/crawling")
    public String crawlingManagementPage() {
        return "admin/crawling-management";
    }

    @GetMapping("/admin/users/add")
    public String addUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.findAll());
        return "admin/user-form";
    }

    @GetMapping("/admin/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với id " + id));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleService.findAll());
        return "admin/user-form";
    }

    @PostMapping("/admin/users/save")
    public String saveUser(@ModelAttribute("user") @Valid User user, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("allRoles", roleService.findAll());
        if(bindingResult.hasErrors()){
            return "admin/user-form";
        }
        if(userService.existsByUsername(user.getUsername(), user.getId())){
            bindingResult.rejectValue("username", "error.user", "Tài khoản đã tồn tại");
            return "admin/user-form";
        }
        if(userService.existsByEmail(user.getEmail(), user.getId())){
            bindingResult.rejectValue("email", "error.user", "Email đã tồn tại");
            return "admin/user-form";
        }
        if(userService.existsPhone(user.getPhone(), user.getId())){
            bindingResult.rejectValue("phone", "error.user", "Số điện thoại đã tồn tại");
            return "admin/user-form";
        }
        // Xử lý mật khẩu riêng biệt
        String newPassword = user.getPassword();
        if(newPassword != null && !newPassword.trim().isEmpty()) {
            // Validate mật khẩu mới
            if(!userService.checkPassword(newPassword)){
                bindingResult.rejectValue("password", "error.user", "Mật khẩu phải nhiều hơn 6 ký tự");
                return "admin/user-form";
            }
        }
        
        // Lưu thông tin user (không bao gồm mật khẩu)
        user.setPassword(null); // Xóa mật khẩu khỏi object để tránh xử lý trong save()
        userService.save(user);
        
        // Cập nhật mật khẩu riêng biệt nếu có
        if(newPassword != null && !newPassword.trim().isEmpty()) {
            userService.updateUserPassword(user.getId(), newPassword);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công.");
        return "redirect:/admin/users";
    }

    // ========== POST MANAGEMENT ==========
    
    @GetMapping("/admin/posts")
    public String postManagementPage(Model model,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false, defaultValue = "") String status,
                                   @RequestParam(required = false, defaultValue = "") String privacy) {
        // Tạo PageRequest với page-1 vì Spring Data sử dụng 0-based indexing
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        
        // Lấy danh sách bài viết với phân trang và filter
        Page<Post> postPage = postService.findPaginated(keyword, status, privacy, pageRequest);
        
        model.addAttribute("postPage", postPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("privacy", privacy);
        model.addAttribute("allStatuses", PostStatus.values());
        model.addAttribute("allPrivacies", PostPrivacy.values());
        
        // Add maps for status and privacy display
        model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
        model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
        model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
        model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);
        
        return "admin/post-management";
    }

    @PostMapping("/admin/posts/{id}/toggle-status")
    public String togglePostStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            
            // Toggle status: PENDING -> APPROVED, APPROVED -> PENDING, REJECTED -> PENDING
            PostStatus newStatus;
            if (post.getStatus() == PostStatus.PENDING) {
                newStatus = PostStatus.APPROVED;
            } else if (post.getStatus() == PostStatus.APPROVED) {
                newStatus = PostStatus.PENDING;
            } else { // REJECTED
                newStatus = PostStatus.PENDING;
            }
            
            post.setStatus(newStatus);
            postService.save(post);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/approve")
    public String approvePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            post.setStatus(PostStatus.APPROVED);
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi duyệt bài viết: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/reject")
    public String rejectPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            post.setStatus(PostStatus.REJECTED);
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Từ chối bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi từ chối bài viết: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            postService.deleteById(id); // Admin có thể xóa bất kỳ bài viết nào
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa bài viết: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @GetMapping("/admin/posts/edit/{id}")
    public String editPostForm(@PathVariable Long id, Model model) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            model.addAttribute("post", post);
            model.addAttribute("allStatuses", PostStatus.values());
            model.addAttribute("allPrivacies", PostPrivacy.values());
            
            // Add maps for status and privacy display
            model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
            model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
            model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
            model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);
            
            return "admin/post-form";
        } catch (Exception e) {
            return "redirect:/admin/posts?error=" + e.getMessage();
        }
    }

    @PostMapping("/admin/posts/save")
    public String savePost(@ModelAttribute("post") @Valid Post post, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("allStatuses", PostStatus.values());
        model.addAttribute("allPrivacies", PostPrivacy.values());
        
        // Add maps for status and privacy display
        model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
        model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
        model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
        model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);
        
        if (bindingResult.hasErrors()) {
            return "admin/post-form";
        }
        
        try {
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bài viết thành công.");
            return "redirect:/admin/posts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu bài viết: " + e.getMessage());
            return "admin/post-form";
        }
    }

    // ========== GROUP POST MANAGEMENT ==========
    
    @GetMapping("/admin/groups/{groupId}/posts")
    public String groupPostManagementPage(@PathVariable Long groupId, 
                                        Model model,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false, defaultValue = "") String status) {
        try {
            // Lấy thông tin nhóm
            UserGroup group = groupService.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm"));
            
            // Tạo PageRequest
            PageRequest pageRequest = PageRequest.of(page - 1, size);
            
            // Lấy bài viết theo nhóm với phân trang
            Page<Post> postPage = postService.findPostsByGroup(groupId, keyword, status, pageRequest);
            
            model.addAttribute("group", group);
            model.addAttribute("postPage", postPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("status", status);
            model.addAttribute("allStatuses", PostStatus.values());
            
            // Add maps for status and privacy display
            model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
            model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
            model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
            model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);
            
            return "admin/group-post-management";
        } catch (Exception e) {
            return "redirect:/admin/groups?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/admin/groups/{groupId}/posts/{postId}/toggle-status")
    public String toggleGroupPostStatus(@PathVariable Long groupId, 
                                      @PathVariable Long postId, 
                                      RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(postId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
            
            if (post.getStatus() == PostStatus.PENDING) {
                post.setStatus(PostStatus.APPROVED);
                redirectAttributes.addFlashAttribute("successMessage", "Duyệt bài viết thành công.");
            } else if (post.getStatus() == PostStatus.APPROVED) {
                post.setStatus(PostStatus.PENDING);
                redirectAttributes.addFlashAttribute("successMessage", "Chuyển bài viết về trạng thái chờ duyệt.");
            }
            
            postService.save(post);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }
    
    @PostMapping("/admin/groups/{groupId}/posts/{postId}/reject")
    public String rejectGroupPost(@PathVariable Long groupId, 
                                @PathVariable Long postId, 
                                RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(postId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
            post.setStatus(PostStatus.REJECTED);
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Từ chối bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi từ chối bài viết: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }
    
    @PostMapping("/admin/groups/{groupId}/posts/{postId}/delete")
    public String deleteGroupPost(@PathVariable Long groupId, 
                                @PathVariable Long postId, 
                                RedirectAttributes redirectAttributes) {
        try {
            postService.deleteById(postId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa bài viết: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }
    
    @GetMapping("/admin/groups/{groupId}/posts/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGroupPostStats(@PathVariable Long groupId) {
        try {
            List<Post> groupPosts = postService.findPostsByGroup(groupId, null, null, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            
            long totalPosts = groupPosts.size();
            long pendingPosts = groupPosts.stream().filter(p -> p.getStatus() == PostStatus.PENDING).count();
            long approvedPosts = groupPosts.stream().filter(p -> p.getStatus() == PostStatus.APPROVED).count();
            long rejectedPosts = groupPosts.stream().filter(p -> p.getStatus() == PostStatus.REJECTED).count();
            
            Map<String, Object> stats = Map.of(
                "totalPosts", totalPosts,
                "pendingPosts", pendingPosts,
                "approvedPosts", approvedPosts,
                "rejectedPosts", rejectedPosts
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== GROUP MANAGEMENT ==========
    
    @GetMapping("/admin/groups")
    public String groupManagementPage(Model model,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword) {
        // Tạo PageRequest với page-1 vì Spring Data sử dụng 0-based indexing
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        
        // Lấy danh sách nhóm với phân trang và filter
        Page<UserGroup> groupPage = groupService.findPaginated(keyword, null, null, pageRequest);
        
        // Tính số thành viên cho mỗi nhóm
        for (UserGroup group : groupPage.getContent()) {
            Long memberCount = groupService.getMemberCount(group.getId());
            group.setMemberCount(memberCount != null ? memberCount : 0L);
        }
        
        model.addAttribute("groupPage", groupPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("allRoles", GroupMemberRole.values());
        
        return "admin/group-management";
    }

    @PostMapping("/admin/groups/{id}/delete")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            groupService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa nhóm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa nhóm: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/edit/{id}")
    public String editGroupForm(@PathVariable Long id, Model model) {
        try {
            UserGroup group = groupService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm với id " + id));
            model.addAttribute("group", group);
            model.addAttribute("allRoles", GroupMemberRole.values());
            return "admin/group-form";
        } catch (Exception e) {
            return "redirect:/admin/groups?error=" + e.getMessage();
        }
    }

    @PostMapping("/admin/groups/save")
    public String saveGroup(@ModelAttribute("group") @Valid UserGroup group, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("allRoles", GroupMemberRole.values());
        
        if (bindingResult.hasErrors()) {
            return "admin/group-form";
        }
        
        try {
            groupService.save(group);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhóm thành công.");
            return "redirect:/admin/groups";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu nhóm: " + e.getMessage());
            return "admin/group-form";
        }
    }

    @GetMapping("/admin/groups/add")
    public String addGroupForm(Model model) {
        model.addAttribute("group", new UserGroup());
        model.addAttribute("allRoles", GroupMemberRole.values());
        return "admin/group-form";
    }

    @PostMapping("/admin/groups/create")
    public String createGroup(@ModelAttribute("group") UserGroup group,
                            BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("allRoles", GroupMemberRole.values());
        
        if (bindingResult.hasErrors()) {
            return "admin/group-form";
        }
        
        try {
            // Set admin as creator
            Long currentUserId = securityUtil.getCurrentUserId();
            if (currentUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xác định người dùng hiện tại.");
                return "admin/group-form";
            }
            
            User adminUser = userService.findById(currentUserId).orElse(null);
            if (adminUser != null) {
                group.setCreatedBy(adminUser);
            }
            
            // Topics will be handled by entity relationship only
            
            // Save group
            System.out.println("DEBUG: Saving group: " + group.getName());
            UserGroup savedGroup = groupService.save(group);
            System.out.println("DEBUG: Group saved with ID: " + savedGroup.getId());
            
            // Add admin as member
            try {
                System.out.println("DEBUG: Adding admin as member to group: " + savedGroup.getName());
                groupService.addMember(savedGroup.getId(), currentUserId, "ADMIN");
                System.out.println("DEBUG: Admin added as member successfully to group: " + savedGroup.getName());
                
                // Verify admin is member
                boolean isMember = groupService.isGroupMember(savedGroup.getId(), currentUserId);
                System.out.println("DEBUG: Verification - Admin is member of group " + savedGroup.getName() + ": " + isMember);
            } catch (Exception e) {
                System.err.println("DEBUG: Error adding admin as member: " + e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm admin vào nhóm: " + e.getMessage());
                return "admin/group-form";
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm thành công.");
            return "redirect:/admin/groups";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo nhóm: " + e.getMessage());
            return "admin/group-form";
        }
    }
    
    // ========== NEW ADMIN FEATURES ==========
    
    @PostMapping("/admin/posts/bulk-delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkDeletePosts(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("DEBUG: Received request: " + request);
            
            @SuppressWarnings("unchecked")
            List<Object> postIdsObj = (List<Object>) request.get("postIds");
            
            if (postIdsObj == null || postIdsObj.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không có bài viết nào được chọn"));
            }
            
            // Convert to Long list
            List<Long> postIds = postIdsObj.stream()
                    .map(obj -> {
                        if (obj instanceof Number) {
                            return ((Number) obj).longValue();
                        } else if (obj instanceof String) {
                            return Long.parseLong((String) obj);
                        }
                        throw new IllegalArgumentException("Cannot convert to Long: " + obj);
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("DEBUG: PostIds to delete: " + postIds);
            
            int deletedCount = 0;
            for (Long postId : postIds) {
                try {
                    postService.deleteById(postId);
                    deletedCount++;
                } catch (Exception e) {
                    // Log error but continue with other posts
                    System.err.println("Error deleting post " + postId + ": " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Đã xóa " + deletedCount + " bài viết thành công",
                "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi khi xóa bài viết: " + e.getMessage()));
        }
    }
    
    @PostMapping("/admin/groups/bulk-delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkDeleteGroups(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> groupIds = (List<Long>) request.get("groupIds");
            
            if (groupIds == null || groupIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không có nhóm nào được chọn"));
            }
            
            int deletedCount = 0;
            for (Long groupId : groupIds) {
                try {
                    groupService.deleteById(groupId);
                    deletedCount++;
                } catch (Exception e) {
                    // Log error but continue with other groups
                    System.err.println("Error deleting group " + groupId + ": " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Đã xóa " + deletedCount + " nhóm thành công",
                "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi khi xóa nhóm: " + e.getMessage()));
        }
    }
    
    
    @GetMapping("/admin/groups/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGroupStats() {
        try {
            List<UserGroup> allGroups = groupService.findAll();
            long totalGroups = allGroups.size();
            long totalMembers = allGroups.stream()
                .mapToLong(group -> groupService.getMemberCount(group.getId()))
                .sum();
            double avgMembersPerGroup = totalGroups > 0 ? (double) totalMembers / totalGroups : 0;
            
            return ResponseEntity.ok(Map.of(
                "totalGroups", totalGroups,
                "totalMembers", totalMembers,
                "avgMembersPerGroup", Math.round(avgMembersPerGroup * 100.0) / 100.0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    
    @PostMapping("/admin/groups/create-simple")
    public String createGroupSimple(@RequestParam String name, 
                            @RequestParam String description,
                            @RequestParam(required = false) String avatar,
                            RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            if (currentUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xác định người dùng hiện tại.");
                return "redirect:/admin/groups";
            }
            
            // Create group with admin as creator
            UserGroup group = new UserGroup();
            group.setName(name);
            group.setDescription(description);
            
            // Set admin as creator
            User adminUser = userService.findById(currentUserId).orElse(null);
            if (adminUser != null) {
                group.setCreatedBy(adminUser);
            }
            
            if (avatar != null && !avatar.trim().isEmpty()) {
                group.setAvatar(avatar.trim());
            }
            
            UserGroup savedGroup = groupService.save(group);
            // Add admin as member
            try {
                System.out.println("DEBUG: Adding admin as member to group: " + savedGroup.getName());
                groupService.addMember(savedGroup.getId(), currentUserId, "ADMIN");
                System.out.println("DEBUG: Admin added as member successfully to group: " + savedGroup.getName());
                
                // Verify admin is member
                boolean isMember = groupService.isGroupMember(savedGroup.getId(), currentUserId);
                System.out.println("DEBUG: Verification - Admin is member of group " + savedGroup.getName() + ": " + isMember);
            } catch (Exception e) {
                System.err.println("DEBUG: Error adding admin as member: " + e.getMessage());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo nhóm: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/{id}/members")
    @ResponseBody
    public ResponseEntity<?> getGroupMembers(@PathVariable Long id) {
        try {
            List<GroupMember> members = groupService.getGroupMembers(id);
            
            List<Map<String, Object>> memberData = members.stream()
                .map(member -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", member.getUser().getId());
                    data.put("username", member.getUser().getUsername());
                    data.put("firstName", member.getUser().getFirstName());
                    data.put("lastName", member.getUser().getLastName());
                    data.put("avatar", member.getUser().getUserProfile() != null ? 
                        member.getUser().getUserProfile().getAvatar() : 
                        "https://i.pravatar.cc/40?u=" + member.getUser().getId());
                    data.put("role", member.getRole().name());
                    data.put("joinedAt", member.getJoinedAt());
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(memberData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy danh sách thành viên: " + e.getMessage()));
        }
    }
} 