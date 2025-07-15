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
        String userName = "Khách";
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof GraduationProject.forumikaa.entity.User) {
                User user = (User) principal;
                userName = user.getFirstName() + (user.getLastName() != null ? (" " + user.getLastName()) : "");
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                userName = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                userName = (String) principal;
            }
        }
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
            user.setPassword(passwordEncoder != null ? passwordEncoder.encode(userDto.getPassword()) : userDto.getPassword());
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            if (roleService != null) {
                Role userRole = roleService.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Default role not found"));
                user.setRoles(Set.of(userRole));
            }
            if (userService != null) {
                userService.save(user);
            }
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