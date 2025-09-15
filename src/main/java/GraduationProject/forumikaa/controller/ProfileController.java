package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.MemberDTO;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.UserService;
import GraduationProject.forumikaa.service.FriendshipService;
import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.util.SecurityUtil;
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

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    private UserService userService;
    @Autowired private PostService postService;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private FriendshipService friendshipService;
    @Autowired private GroupMemberDao groupMemberDao;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/{username}")
    public String profilePage(@PathVariable String username, 
                             @RequestParam(defaultValue = "posts") String tab,
                             Model model) {
        // Kiểm tra authentication trước
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

        // Lấy các bài viết của người dùng này
        List<PostResponse> userPosts = postService.getUserPosts(user.getId());
        
        // Lấy danh sách bạn bè thật từ database
        List<User> friendsList = friendshipService.listFriends(user.getId());
        List<MemberDTO> friends = friendsList.stream()
                .map(friend -> {
                    MemberDTO dto = new MemberDTO();
                    dto.setId(friend.getId());
                    dto.setUsername(friend.getUsername());
                    dto.setFirstName(friend.getUsername()); // UserProfile không có firstName, dùng username
                    dto.setLastName(""); // UserProfile không có lastName
                    dto.setAvatar(friend.getUserProfile() != null && friend.getUserProfile().getAvatar() != null ? 
                            friend.getUserProfile().getAvatar() : 
                            "https://ui-avatars.com/api/?name=" + friend.getUsername() + "&background=007bff&color=ffffff&size=60");
                    dto.setRole("FRIEND");
                    dto.setJoinedAt("2024-01-01"); // Có thể lấy từ friendship table nếu cần
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Lấy danh sách nhóm của user thật từ database
        List<GroupMember> groupMembers = groupMemberDao.findByUserId(user.getId());
        List<MemberDTO> userGroups = groupMembers.stream()
                .map(member -> {
                    MemberDTO dto = new MemberDTO();
                    dto.setId(member.getGroup().getId());
                    dto.setUsername(member.getGroup().getName());
                    dto.setFirstName(member.getGroup().getName());
                    dto.setLastName("");
                    dto.setAvatar(member.getGroup().getAvatar() != null ? 
                            member.getGroup().getAvatar() : 
                            "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60");
                    dto.setRole(member.getRole().name()); // Convert enum to string
                    dto.setJoinedAt(member.getJoinedAt().toString().substring(0, 10)); // Format date
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Lấy tài liệu từ posts
        List<FileUploadResponse> userDocuments = userPosts.stream()
                .filter(post -> post.getDocuments() != null && !post.getDocuments().isEmpty())
                .flatMap(post -> post.getDocuments().stream())
                .filter(doc -> doc.getFileName() != null && 
                        (doc.getFileName().toLowerCase().endsWith(".pdf") || 
                         doc.getFileName().toLowerCase().endsWith(".doc") || 
                         doc.getFileName().toLowerCase().endsWith(".docx") || 
                         doc.getFileName().toLowerCase().endsWith(".xls") || 
                         doc.getFileName().toLowerCase().endsWith(".xlsx") || 
                         doc.getFileName().toLowerCase().endsWith(".ppt") || 
                         doc.getFileName().toLowerCase().endsWith(".pptx") || 
                         doc.getFileName().toLowerCase().endsWith(".txt")))
                .limit(20) // Giới hạn 20 tài liệu
                .collect(Collectors.toList());
        
        // Không tạo data mẫu, sử dụng dữ liệu thật từ database
        
        model.addAttribute("user", user);
        model.addAttribute("posts", userPosts);
        model.addAttribute("friends", friends);
        model.addAttribute("userGroups", userGroups);
        model.addAttribute("userDocuments", userDocuments);
        model.addAttribute("postCount", userPosts.size());
        model.addAttribute("friendsCount", friends.size());
        model.addAttribute("groupsCount", userGroups.size());
        model.addAttribute("documentsCount", userDocuments.size());
        model.addAttribute("activeTab", tab);

        // Kiểm tra xem có phải profile của chính mình không
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            model.addAttribute("isOwnProfile", currentUserId.equals(user.getId()));
        } catch (Exception e) {
            model.addAttribute("isOwnProfile", false);
        }

        return "user/profile";
    }

    @GetMapping("/profile/edit/{username}")
    public String editProfilePage(@PathVariable String username, Model model) {
        // Kiểm tra authentication trước
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + username));
        model.addAttribute("user", user);
        return "user/edit-profile";
    }

    @PostMapping("/profile/edit/{username}")
    public String updateProfile(@PathVariable String username, @ModelAttribute User updatedUser) {
        // Kiểm tra authentication trước
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        try {
            User existingUser = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

            // Cập nhật thông tin profile
            if (updatedUser.getUserProfile() != null) {
                if (existingUser.getUserProfile() == null) {
                    existingUser.setUserProfile(updatedUser.getUserProfile());
                } else {
                    // Cập nhật từng field
                    if (updatedUser.getUserProfile().getBio() != null) {
                        existingUser.getUserProfile().setBio(updatedUser.getUserProfile().getBio());
                    }
                    if (updatedUser.getUserProfile().getAvatar() != null) {
                        existingUser.getUserProfile().setAvatar(updatedUser.getUserProfile().getAvatar());
                    }
                    if (updatedUser.getUserProfile().getCover() != null) {
                        existingUser.getUserProfile().setCover(updatedUser.getUserProfile().getCover());
                    }
                    if (updatedUser.getUserProfile().getSocialLinks() != null) {
                        existingUser.getUserProfile().setSocialLinks(updatedUser.getUserProfile().getSocialLinks());
                    }
            }
        }

        userService.save(existingUser);
            return "redirect:/profile/" + username + "?success=true";
        } catch (Exception e) {
            return "redirect:/profile/edit/" + username + "?error=true";
        }
    }
}