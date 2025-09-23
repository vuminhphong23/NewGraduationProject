package GraduationProject.forumikaa.controller.admin;

import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.service.RoleService;
import GraduationProject.forumikaa.service.UserService;
import GraduationProject.forumikaa.service.StatisticsService;
import GraduationProject.forumikaa.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import java.util.*;

@Controller
public class UserManagementController {

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

    private SecurityUtil securityUtil;

    @Autowired
    public void setSecurityUtil(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    private StatisticsService statisticsService;
    
    @Autowired
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        try {
            // Lấy thống kê tổng quan từ StatisticsService
            Map<String, Object> overview = statisticsService.getDashboardOverview();
            
            // Truyền dữ liệu vào model
            model.addAttribute("totalUsers", overview.get("totalUsers"));
            model.addAttribute("userChangePercent", overview.get("userChangePercent"));
            model.addAttribute("totalPosts", overview.get("totalPosts"));
            model.addAttribute("postChangePercent", overview.get("postChangePercent"));
            model.addAttribute("totalGroups", overview.get("totalGroups"));
            model.addAttribute("groupChangePercent", overview.get("groupChangePercent"));
            model.addAttribute("pendingReports", overview.get("pendingReports"));
            model.addAttribute("reportChangePercent", overview.get("reportChangePercent"));
            
        } catch (Exception e) {
            // Nếu có lỗi, truyền dữ liệu mặc định
            model.addAttribute("totalUsers", 0);
            model.addAttribute("userChangePercent", 0.0);
            model.addAttribute("totalPosts", 0);
            model.addAttribute("postChangePercent", 0.0);
            model.addAttribute("totalGroups", 0);
            model.addAttribute("groupChangePercent", 0.0);
            model.addAttribute("pendingReports", 0);
            model.addAttribute("reportChangePercent", 0.0);
        }
        
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

    @PostMapping("/admin/users/bulk-delete")
    public String bulkDeleteUsers(@RequestParam("userIds") List<Long> userIds, RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = 0;
            for (Long userId : userIds) {
                try {
                    userService.deleteUser(userId);
                    deletedCount++;
                } catch (Exception e) {
                    System.err.println("Error deleting user " + userId + ": " + e.getMessage());
                }
            }
            
            if (deletedCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành công " + deletedCount + " người dùng!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa người dùng nào!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa hàng loạt: " + e.getMessage());
        }
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
} 