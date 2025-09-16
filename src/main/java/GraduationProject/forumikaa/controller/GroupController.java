package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.dto.UserDisplayDto;
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
            Page<UserGroup> groupsPage = groupService.findGroupsForExplore(keyword, category, pageable);
            
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
        Optional<UserGroup> groupOpt = groupService.findById(groupId);
        if (groupOpt.isEmpty()) {
            return "error/404";
        }
        
        UserGroup group = groupOpt.get();
        
        // Get member count (now stored in entity)
        Long memberCount = group.getMemberCount() != null ? group.getMemberCount() : groupService.getMemberCount(groupId);
        
        // Get group members for display
        List<UserDisplayDto> recentMembers = groupService.getGroupMembers(groupId).stream()
                .limit(6)
                .map(member -> {
                    UserDisplayDto dto = new UserDisplayDto();
                    dto.setId(member.getUser().getId());
                    dto.setUsername(member.getUser().getUsername());
                    dto.setFirstName(member.getUser().getFirstName());
                    dto.setLastName(member.getUser().getLastName());
                    dto.setAvatar(member.getUser().getUserProfile() != null ? 
                            member.getUser().getUserProfile().getAvatar() : 
                            "https://i.pravatar.cc/40?u=" + member.getUser().getId());
                    dto.setRole(member.getRole().toString());
                    dto.setJoinedAt(member.getJoinedAt().toString().substring(0, 10));
                    return dto;
                })
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
        model.addAttribute("recentMembers", recentMembers);
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
    
}
