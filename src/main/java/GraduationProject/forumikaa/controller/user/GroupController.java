package GraduationProject.forumikaa.controller.user;

import GraduationProject.forumikaa.entity.Group;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.dto.GroupMemberDto;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.service.GroupService;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import GraduationProject.forumikaa.entity.GroupMember;
import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping("/explore")
    public String exploreGroups(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "all") String category,
                               Model model) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            User currentUser = securityUtil.getCurrentUser();
            
            // Pagination
            Pageable pageable = PageRequest.of(page, 12);
            
            // Get groups with filters
            Page<Group> groupsPage = groupService.findGroupsForExplore(keyword, category, pageable);
            
            // Get user's joined groups
            List<Long> joinedGroupIds = groupService.getUserJoinedGroupIds(currentUserId);
            
            // Get popular topics for filter
            List<Topic> popularTopics = groupService.getPopularTopics(10);
            
            // Get group statistics
            Long totalGroups = groupService.getTotalGroupCount();
            Long totalMembers = groupService.getTotalMemberCount();
            
            model.addAttribute("groups", groupsPage);
            model.addAttribute("user", currentUser);
            model.addAttribute("joinedGroupIds", joinedGroupIds);
            model.addAttribute("popularTopics", popularTopics);
            model.addAttribute("totalGroups", totalGroups);
            model.addAttribute("totalMembers", totalMembers);
            model.addAttribute("keyword", keyword);
            model.addAttribute("category", category);
            model.addAttribute("currentPage", page);
            
            return "user/explore-groups";
        } catch (Exception e) {
            return "redirect:/login";
        }
    }

    @GetMapping("/{groupId}")
    public String groupDetail(@PathVariable Long groupId, 
                             @RequestParam(defaultValue = "posts") String tab,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Optional<Group> groupOpt = groupService.findById(groupId);
        if (groupOpt.isEmpty()) {
            return "error/404";
        }
        
        Group group = groupOpt.get();
        
        // Get member count (now stored in entity)
        Long memberCount = group.getMemberCount() != null ? group.getMemberCount() : groupService.getMemberCount(groupId);
        
        // Get most active members for display
        List<GroupMember> mostActiveMembers = groupService.getMostActiveMembers(groupId, 10);
        List<GroupMemberDto> activeMembers = mostActiveMembers.stream()
                .map(member -> GroupMemberDto.builder()
                        .id(member.getUser().getId())
                        .userId(member.getUser().getId())
                        .username(member.getUser().getUsername())
                        .firstName(member.getUser().getFirstName())
                        .lastName(member.getUser().getLastName())
                        .fullName(member.getUser().getFirstName() + " " + member.getUser().getLastName())
                        .avatar(member.getUser().getUserProfile() != null ? 
                                member.getUser().getUserProfile().getAvatar() : 
                                "https://i.pravatar.cc/40?u=" + member.getUser().getId())
                        .role(member.getRole().toString())
                        .isOnline(false) // Có thể thêm logic kiểm tra online status
                        .joinedAt(member.getJoinedAt())
                        .memberCount(0L)
                        .postCount(groupService.getPostCountByUserInGroup(groupId, member.getUser().getId()))
                        .build())
                .collect(Collectors.toList());
        
        // Get current user ID and user info
        Long currentUserId = securityUtil.getCurrentUserId();
        User currentUser = securityUtil.getCurrentUser();
        
        // Get group posts from database
        List<PostResponse> posts = postService.getPostsByGroup(groupId, currentUserId);
        
        // Get post count for group
        Long postCount = postService.getPostCountByGroup(groupId);
        
        // Get group documents from database
        List<FileUploadResponse> groupDocuments = groupService.getGroupDocuments(groupId);
        
        // Count all files from posts (images + documents)
        Long documentCount = posts.stream()
                .mapToLong(post -> post.getDocuments() != null ? post.getDocuments().size() : 0L)
                .sum();
        
        // Get pinned documents (top 3 most downloaded)
        List<FileUploadResponse> pinnedDocuments = groupDocuments.stream()
                .sorted((a, b) -> Integer.compare(b.getDownloadCount(), a.getDownloadCount()))
                .limit(3)
                .collect(Collectors.toList());
        
        
        // Get popular topics in group (top 5)
        List<Topic> popularTopics = groupService.getPopularTopicsInGroup(groupId, 5);
        
        model.addAttribute("group", group);
        model.addAttribute("user", currentUser);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("postCount", postCount);
        model.addAttribute("documentCount", documentCount);
        model.addAttribute("activeMembers", activeMembers);
        model.addAttribute("posts", posts);
        model.addAttribute("groupDocuments", groupDocuments);
        model.addAttribute("pinnedDocuments", pinnedDocuments);
        model.addAttribute("popularTopics", popularTopics);
        model.addAttribute("currentTab", tab);
        model.addAttribute("isMember", groupService.isGroupMember(groupId, currentUserId));
        model.addAttribute("isAdmin", groupService.isGroupAdmin(groupId, currentUserId));
        
        return "user/group-detail";
    }
    
    @PostMapping("/{groupId}/join")
    @ResponseBody
    public String joinGroup(@PathVariable Long groupId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            groupService.addMember(groupId, userId, "MEMBER");
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
    
    @PostMapping("/{groupId}/leave")
    @ResponseBody
    public String leaveGroup(@PathVariable Long groupId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            groupService.removeMember(groupId, userId);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
    
    @PostMapping("/{groupId}/posts/{postId}/like")
    @ResponseBody
    public String likePost(@PathVariable Long groupId, @PathVariable Long postId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            postService.toggleLike(postId, userId);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
    
    @PostMapping("/{groupId}/posts/{postId}/comment")
    @ResponseBody
    public String commentPost(@PathVariable Long groupId, @PathVariable Long postId, 
                             @RequestParam String content) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            postService.addComment(postId, userId, content);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
    
    @GetMapping("/{groupId}/members")
    @ResponseBody
    public ResponseEntity<?> getGroupMembersWithFilters(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Create pageable with sorting
            Pageable pageable = PageRequest.of(page, size);
            if (sortBy != null && !sortBy.isEmpty()) {
                switch (sortBy) {
                    case "name":
                        pageable = PageRequest.of(page, size, 
                            org.springframework.data.domain.Sort.by("user.firstName").ascending()
                            .and(org.springframework.data.domain.Sort.by("user.lastName").ascending()));
                        break;
                    case "username":
                        pageable = PageRequest.of(page, size, 
                            org.springframework.data.domain.Sort.by("user.username").ascending());
                        break;
                    case "joinedAt":
                        pageable = PageRequest.of(page, size, 
                            org.springframework.data.domain.Sort.by("joinedAt").descending());
                        break;
                    case "role":
                        pageable = PageRequest.of(page, size, 
                            org.springframework.data.domain.Sort.by("role").ascending());
                        break;
                    default:
                        pageable = PageRequest.of(page, size, 
                            org.springframework.data.domain.Sort.by("joinedAt").descending());
                }
            }
            
            // Use proper pagination query
            Page<GroupMember> membersPage = groupService.getGroupMembersWithFilters(groupId, search, role, sortBy, pageable);
            
            List<Map<String, Object>> memberData = membersPage.getContent().stream()
                .map(member -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", member.getId());
                    data.put("userId", member.getUser().getId());
                    data.put("username", member.getUser().getUsername());
                    data.put("firstName", member.getUser().getFirstName());
                    data.put("lastName", member.getUser().getLastName());
                    data.put("fullName", member.getUser().getFirstName() + " " + member.getUser().getLastName());
                    
                    // Get avatar - need to load userProfile separately
                    String avatar = "https://i.pravatar.cc/40?u=" + member.getUser().getId();
                    try {
                        if (member.getUser().getUserProfile() != null && 
                            member.getUser().getUserProfile().getAvatar() != null &&
                            !member.getUser().getUserProfile().getAvatar().trim().isEmpty()) {
                            avatar = member.getUser().getUserProfile().getAvatar();
                        }
                    } catch (Exception e) {
                        // Lazy loading issue - use default avatar
                    }
                    data.put("avatar", avatar);
                    
                    data.put("role", member.getRole().name());
                    data.put("joinedAt", member.getJoinedAt());
                    
                    // Add post count for activity sorting and display
                    Long postCount = groupService.getPostCountByUserInGroup(groupId, member.getUser().getId());
                    data.put("postCount", postCount);
                    
                    return data;
                })
                .collect(Collectors.toList());
            
            // Create paginated response
            Map<String, Object> paginatedData = new HashMap<>();
            paginatedData.put("content", memberData);
            paginatedData.put("totalElements", membersPage.getTotalElements());
            paginatedData.put("totalPages", membersPage.getTotalPages());
            paginatedData.put("currentPage", membersPage.getNumber());
            paginatedData.put("size", membersPage.getSize());
            paginatedData.put("first", membersPage.isFirst());
            paginatedData.put("last", membersPage.isLast());
            paginatedData.put("currentUserId", securityUtil.getCurrentUserId());
            
            return ResponseEntity.ok(paginatedData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy danh sách thành viên: " + e.getMessage()));
        }
    }
    
}
