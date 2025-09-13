package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.dto.MemberDTO;
import GraduationProject.forumikaa.dto.DocumentDTO;
import GraduationProject.forumikaa.dto.LinkDTO;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.service.GroupService;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        
        // Get member count
        Long memberCount = groupService.getMemberCount(groupId);
        group.setMemberCount(memberCount);
        
        // Get group members for display
        List<MemberDTO> recentMembers = groupService.getGroupMembers(groupId).stream()
                .limit(6)
                .map(member -> new MemberDTO(
                    member.getUser().getId(),
                    member.getUser().getUsername(),
                    member.getUser().getFirstName(),
                    member.getUser().getLastName(),
                    member.getUser().getUserProfile() != null ? 
                            member.getUser().getUserProfile().getAvatar() : 
                            "https://i.pravatar.cc/40?u=" + member.getUser().getId(),
                    member.getRole().toString(),
                    member.getJoinedAt().toString()
                ))
                .collect(Collectors.toList());
        
        // Get current user ID
        Long currentUserId = securityUtil.getCurrentUserId();
        
        // Get group posts from database
        List<PostResponse> posts = postService.getPostsByGroup(groupId, currentUserId);
        
        // Get post count for group
        Long postCount = postService.getPostCountByGroup(groupId);
        
        // Get group documents from database
        List<DocumentDTO> groupDocuments = groupService.getGroupDocuments(groupId);
        Long documentCount = (long) groupDocuments.size();
        
        // Get pinned documents (top 3 most downloaded)
        List<DocumentDTO> pinnedDocuments = groupDocuments.stream()
                .sorted((a, b) -> Integer.compare(b.getDownloadCount(), a.getDownloadCount()))
                .limit(3)
                .collect(Collectors.toList());
        
        // Get important links from group settings (if available)
        List<LinkDTO> importantLinks = groupService.getGroupLinks(groupId);
        
        model.addAttribute("group", group);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("postCount", postCount);
        model.addAttribute("documentCount", documentCount);
        model.addAttribute("recentMembers", recentMembers);
        model.addAttribute("posts", posts);
        model.addAttribute("groupDocuments", groupDocuments);
        model.addAttribute("pinnedDocuments", pinnedDocuments);
        model.addAttribute("importantLinks", importantLinks);
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
