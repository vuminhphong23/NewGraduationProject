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

@Controller
public class ProfileController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/{username}")
    public String profilePage(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        model.addAttribute("user", user);
        return "user/profile";
    }

    @GetMapping("/profile/edit/{username}")
    public String editProfilePage(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + username));
        model.addAttribute("user", user);
        return "user/profile-edit";
    }

//    @PostMapping("/profile/update")
//    public String updateProfile(@ModelAttribute("user") User user, Model model) {
//        User existingUser = userService.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + user.getId()));
//
//        // Cập nhật các trường User
//        existingUser.setFirstName(user.getFirstName());
//        existingUser.setLastName(user.getLastName());
//        existingUser.setEmail(user.getEmail());
//        existingUser.setPhone(user.getPhone());
//        existingUser.setGender(user.getGender());
//        existingUser.setAddress(user.getAddress());
//        existingUser.setBirthDate(user.getBirthDate());
//        existingUser.setProfileInfo(user.getProfileInfo());
//
//        // Cập nhật bio cho UserProfile
//        if (existingUser.getUserProfile() == null) {
//            existingUser.setUserProfile(new GraduationProject.forumikaa.entity.UserProfile());
//            existingUser.getUserProfile().setUser(existingUser);
//            existingUser.getUserProfile().setSocialLinks("/profile/" + existingUser.getUsername());
//
//        }
//        existingUser.getUserProfile().setBio(
//                user.getUserProfile() != null ? user.getUserProfile().getBio() : null
//        );
//
//        userService.save(existingUser);
//        model.addAttribute("user", existingUser);
//        return "redirect:profile";
//    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User user, Model model) {
        User existingUser = userService.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + user.getId()));

        // Update thông tin cơ bản
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setGender(user.getGender());
        existingUser.setAddress(user.getAddress());
        existingUser.setBirthDate(user.getBirthDate());
        existingUser.setProfileInfo(user.getProfileInfo());

        // Nếu UserProfile null thì tạo mới
        if (existingUser.getUserProfile() == null) {
            existingUser.setUserProfile(new GraduationProject.forumikaa.entity.UserProfile());
            existingUser.getUserProfile().setUser(existingUser);
            existingUser.getUserProfile().setSocialLinks("/profile/" + existingUser.getUsername());
        }

        // Cập nhật bio, avatar, cover
        if (user.getUserProfile() != null) {
            existingUser.getUserProfile().setBio(user.getUserProfile().getBio());

            // Avatar & Cover chỉ cập nhật nếu người dùng upload ảnh mới
            if (user.getUserProfile().getAvatar() != null && !user.getUserProfile().getAvatar().isBlank()) {
                existingUser.getUserProfile().setAvatar(user.getUserProfile().getAvatar());
            }
            if (user.getUserProfile().getCover() != null && !user.getUserProfile().getCover().isBlank()) {
                existingUser.getUserProfile().setCover(user.getUserProfile().getCover());
            }
        }

        userService.save(existingUser);
        model.addAttribute("user", existingUser);
        return "redirect:/profile/" + user.getUsername();
    }

}

