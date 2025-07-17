package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ProfileController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/{username}")
    public String profilePage(@PathVariable String username, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/profile/edit/{username}")
    public String editProfilePage(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + username));
        model.addAttribute("user", user);
        return "profile-edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User user, Model model) {
        User existingUser = userService.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + user.getId()));

        // Cập nhật các trường User
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setGender(user.getGender());
        existingUser.setAddress(user.getAddress());
        existingUser.setBirthDate(user.getBirthDate());
        existingUser.setProfileInfo(user.getProfileInfo());

        // Cập nhật bio cho UserProfile
        if (existingUser.getUserProfile() == null) {
            existingUser.setUserProfile(new GraduationProject.forumikaa.entity.UserProfile());
            existingUser.getUserProfile().setUser(existingUser);
            existingUser.getUserProfile().setSocialLinks("/profile/" + existingUser.getUsername());

        }
        existingUser.getUserProfile().setBio(
                user.getUserProfile() != null ? user.getUserProfile().getBio() : null
        );

        userService.save(existingUser);
        model.addAttribute("user", existingUser);
        return "redirect: profile";
    }
}

