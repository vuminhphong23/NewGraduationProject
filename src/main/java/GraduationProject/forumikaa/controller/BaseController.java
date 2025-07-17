package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.UserDto;
import GraduationProject.forumikaa.entity.Role;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.RoleService;
import GraduationProject.forumikaa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
public class BaseController {

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

    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //getPrincipal() sẽ trả về một đối tượng đại diện cho người dùng đã đăng nhập.
        //Trong Spring Security, khi một người dùng chưa đăng nhập, đối tượng Authentication sẽ có principal là một chuỗi "anonymousUser".
        String userName = (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser"))
                ? authentication.getName()
                : "Khách";

        model.addAttribute("userName", userName);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "register";
    }

    //model.addAttribute sẽ giữ dữ liệu trong request hiện tại, dữ liệu này sẽ bị mất khi chuyển hướng (redirect).
    //
    //addFlashAttribute giữ dữ liệu tạm thời chỉ trong một request tiếp theo sau khi redirect và sẽ tự động bị xóa
    //sau khi request đó kết thúc. Điều này rất hữu ích khi bạn muốn hiển thị một thông báo sau khi chuyển hướng mà không phải giữ dữ liệu lâu dài trong session.
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserDto userDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.userDto", "Passwords do not match");
            return "register";
        }
        if (userService != null && userService.findByUsername(userDto.getUsername().trim()).isPresent()) {
            result.rejectValue("username", "error.userDto", "Username already exists");
            return "register";
        }
        if (userService != null && userService.findByEmail(userDto.getEmail().trim()).isPresent()) {
            result.rejectValue("email", "error.userDto", "Email already exists");
            return "register";
        }
        try {
            User user = new User();
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());

            Role userRole = roleService.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRoles(Set.of(userRole));

            userService.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            result.rejectValue("", "error.userDto", "Registration failed. Please try again.");
            return "register";
        }
    }


    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login?logout";
    }
} 