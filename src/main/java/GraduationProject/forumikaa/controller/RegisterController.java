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
public class RegisterController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        // Kiểm tra password và confirmPassword
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.userDto", "Passwords do not match");
            return "register";
        }

        // Kiểm tra username đã tồn tại chưa
        if (userDao.existsByUsername(userDto.getUsername())) {
            result.rejectValue("username", "error.userDto", "Username already exists");
            return "register";
        }

        // Kiểm tra email đã tồn tại chưa
        if (userDao.existsByEmail(userDto.getEmail())) {
            result.rejectValue("email", "error.userDto", "Email already exists");
            return "register";
        }

        try {
            // Tạo user mới
            User user = new User();
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());

            // Gán role mặc định là ROLE_USER
            Role userRole = roleDao.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRoles(Set.of(userRole));

            userDao.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";

        } catch (Exception e) {
            result.rejectValue("", "error.userDto", "Registration failed. Please try again.");
            return "register";
        }
    }
} 