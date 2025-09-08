package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.RoleService;
import GraduationProject.forumikaa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

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

    @GetMapping("/admin")
    public String adminPage(Model model) {
        long totalUsers = userService.findAll().size();
        model.addAttribute("totalUsers", totalUsers);
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
} 