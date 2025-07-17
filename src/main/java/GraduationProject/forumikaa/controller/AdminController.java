package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.RoleDao;
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

import java.util.List;

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
        return "admin";
    }

    @GetMapping("/admin/users")
    public String userManagementPage(Model model,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false, defaultValue = "") String status) {
        Page<User> userPage = userService.findPaginated(keyword, status, PageRequest.of(page - 1, size));
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "user-management";
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
        redirectAttributes.addFlashAttribute("successMessage", "Xóa thành công.");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/add")
    public String addUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.findAll());
        return "user-form";
    }

    @GetMapping("/admin/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với id " + id));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleService.findAll());
        return "user-form";
    }

    @PostMapping("/admin/users/save")
    public String saveUser(@ModelAttribute("userDto") @Valid User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if(bindingResult.hasErrors()){
            return "user-form";
        }
        userService.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công.");
        return "redirect:/admin/users";
    }
} 