package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.UserDisplayDto;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.dto.ProfileUpdateRequest;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserProfile;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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
        List<UserDisplayDto> friends = friendsList.stream()
                .map(friend -> {
                    UserDisplayDto dto = new UserDisplayDto();
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
        List<UserDisplayDto> userGroups = groupMembers.stream()
                .map(member -> {
                    UserDisplayDto dto = new UserDisplayDto();
                    dto.setId(member.getGroup().getId());
                    dto.setUsername(member.getGroup().getName());
                    dto.setFirstName(member.getGroup().getName());
                    dto.setLastName("");
                    dto.setAvatar(member.getGroup().getAvatar() != null ? 
                            member.getGroup().getAvatar() : 
                            "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60");
                    dto.setRole(member.getRole().name()); // Convert enum to string
                    dto.setJoinedAt(member.getJoinedAt().toString().substring(0, 10)); // Format date
                    dto.setMemberCount(postService.getNewPostCountByGroupToday(member.getGroup().getId()));
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Lấy tất cả tài liệu của user từ posts
        List<FileUploadResponse> userDocuments = userPosts.stream()
                .filter(post -> post.getDocuments() != null && !post.getDocuments().isEmpty())
                .flatMap(post -> post.getDocuments().stream()
                    .filter(doc -> doc.getFileName() != null)
                    .map(doc -> {
                        // Refine fileType based on file extension for better categorization
                        String fileName = doc.getFileName().toLowerCase();
                        String currentFileType = doc.getFileType();
                        
                        // Only refine if current fileType is generic "document"
                        if ("document".equals(currentFileType)) {
                            if (fileName.endsWith(".pdf")) {
                                doc.setFileType("pdf");
                            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                                doc.setFileType("doc");
                            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                                doc.setFileType("xls");
                            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                                doc.setFileType("ppt");
                            } else if (fileName.endsWith(".txt")) {
                                doc.setFileType("text");
                            }
                            // Keep "document" for other application/* types
                        }
                        // Keep existing fileType for "image", "video", "other"
                        
                        return doc;
                    }))
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
        
        // Đảm bảo UserProfile được khởi tạo nếu chưa có
        if (user.getUserProfile() == null) {
            UserProfile userProfile = new UserProfile(user);
            user.setUserProfile(userProfile);
        }
        
        model.addAttribute("user", user);
        return "user/profile-edit";
    }


    // REST API endpoint for profile update
    @PutMapping("/api/profile/update")
    public ResponseEntity<?> updateProfileApi(@RequestBody ProfileUpdateRequest request) {
        try {
            // Lấy user hiện tại từ security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            String username = authentication.getName();
            User existingUser = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Cập nhật thông tin cơ bản
            if (request.getFirstName() != null) {
                existingUser.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                existingUser.setLastName(request.getLastName());
            }
            if (request.getEmail() != null) {
                existingUser.setEmail(request.getEmail());
            }
            if (request.getPhone() != null) {
                existingUser.setPhone(request.getPhone());
            }
            if (request.getAddress() != null) {
                existingUser.setAddress(request.getAddress());
            }
            if (request.getBirthDate() != null && !request.getBirthDate().trim().isEmpty()) {
                existingUser.setBirthDate(java.time.LocalDate.parse(request.getBirthDate()));
            }
            if (request.getGender() != null) {
                existingUser.setGender(request.getGender());
            }
            if (request.getProfileInfo() != null) {
                existingUser.setProfileInfo(request.getProfileInfo());
            }

            // Cập nhật UserProfile
            if (existingUser.getUserProfile() == null) {
                UserProfile newProfile = new UserProfile(existingUser);
                existingUser.setUserProfile(newProfile);
            }

            if (request.getBio() != null) {
                existingUser.getUserProfile().setBio(request.getBio());
            }
            if (request.getAvatar() != null) {
                existingUser.getUserProfile().setAvatar(request.getAvatar());
            }
            if (request.getCover() != null) {
                existingUser.getUserProfile().setCover(request.getCover());
            }
            if (request.getSocialLinks() != null) {
                existingUser.getUserProfile().setSocialLinks(request.getSocialLinks());
            }

            userService.save(existingUser);

            return ResponseEntity.ok().body("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }
}