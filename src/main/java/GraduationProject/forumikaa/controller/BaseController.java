package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.UserDto;
import GraduationProject.forumikaa.entity.Role;
import GraduationProject.forumikaa.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired(required = false)
    private UserDao userDao;
    @Autowired(required = false)
    private RoleDao roleDao;
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String home() {
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
        if (userDao != null && userDao.existsByUsername(userDto.getUsername())) {
            result.rejectValue("username", "error.userDto", "Username already exists");
            return "register";
        }
        if (userDao != null && userDao.existsByEmail(userDto.getEmail())) {
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
            if (roleDao != null) {
                Role userRole = roleDao.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Default role not found"));
                user.setRoles(Set.of(userRole));
            }
            if (userDao != null) {
                userDao.save(user);
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