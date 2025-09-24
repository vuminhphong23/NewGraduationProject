package GraduationProject.forumikaa.controller.admin;

import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.GroupMemberRole;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Group;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.service.GroupService;
import GraduationProject.forumikaa.service.UserService;
import GraduationProject.forumikaa.service.TopicService;
import GraduationProject.forumikaa.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class GroupManagementController {
    private GroupService groupService;

    @Autowired
    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private SecurityUtil securityUtil;

    @Autowired
    public void setSecurityUtil(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    private TopicService topicService;

    @Autowired
    public void setTopicService(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/admin/groups")
    public String groupManagementPage(Model model,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword) {
        // Tạo PageRequest với page-1 vì Spring Data sử dụng 0-based indexing
        PageRequest pageRequest = PageRequest.of(page - 1, size);

        // Lấy danh sách nhóm với phân trang và filter
        Page<Group> groupPage = groupService.findPaginated(keyword, null, null, pageRequest);

        // Tính số thành viên cho mỗi nhóm và load topics
        for (Group group : groupPage.getContent()) {
            Long memberCount = groupService.getMemberCount(group.getId());
            group.setMemberCount(memberCount != null ? memberCount : 0L);
            
            // Load topics for display - topics will be accessed in template
            // The template will handle displaying the main topic
        }

        model.addAttribute("groupPage", groupPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("allRoles", GroupMemberRole.values());

        return "admin/group-management";
    }

    @PostMapping("/admin/groups/{id}/delete")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            groupService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa nhóm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa nhóm: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/edit/{id}")
    public String editGroupForm(@PathVariable Long id, Model model) {
        try {
            Group group = groupService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm với id " + id));
            
            // Force load createdBy to avoid lazy loading issues
            if (group.getCreatedBy() != null) {
                System.out.println("DEBUG: Group created by: " + group.getCreatedBy().getUsername());
            } else {
                System.out.println("DEBUG: Group createdBy is null");
            }
            
            // Load topics for the group
            String currentMainTopic = "";
            if (group.getTopics() != null && !group.getTopics().isEmpty()) {
                // Get the first topic as main topic
                currentMainTopic = group.getTopics().iterator().next().getName();
            }
            
            model.addAttribute("group", group);
            model.addAttribute("currentMainTopic", currentMainTopic);
            model.addAttribute("allRoles", GroupMemberRole.values());
            return "admin/group-form";
        } catch (Exception e) {
            return "redirect:/admin/groups?error=" + e.getMessage();
        }
    }

    @PostMapping("/admin/groups/save")
    public String saveGroup(@ModelAttribute("group") @Valid Group group, 
                           @RequestParam(required = false) String mainTopic,
                           BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("allRoles", GroupMemberRole.values());

        if (bindingResult.hasErrors()) {
            return "admin/group-form";
        }

        try {
            // Get the original group to preserve createdBy
            Group originalGroup = groupService.findById(group.getId()).orElse(null);
            if (originalGroup != null && originalGroup.getCreatedBy() != null) {
                // Preserve the original createdBy
                group.setCreatedBy(originalGroup.getCreatedBy());
                System.out.println("DEBUG: Preserved createdBy: " + originalGroup.getCreatedBy().getUsername());
            }
            
            // Save group first
            Group savedGroup = groupService.save(group);
            
            // Process main topic if provided
            if (mainTopic != null && !mainTopic.trim().isEmpty()) {
                try {
                    Long currentUserId = securityUtil.getCurrentUserId();
                    User currentUser = userService.findById(currentUserId).orElse(null);
                    
                    if (currentUser != null) {
                        // Clear existing topics first
                        savedGroup.getTopics().clear();
                        
                        // Add new topic
                        Topic topic = topicService.findOrCreateTopic(mainTopic.trim(), currentUser);
                        savedGroup.getTopics().add(topic);
                        groupService.save(savedGroup);
                        System.out.println("DEBUG: Updated main topic for group: " + topic.getName());
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Error updating main topic: " + e.getMessage());
                    // Don't fail the group update if topic processing fails
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhóm thành công.");
            return "redirect:/admin/groups";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu nhóm: " + e.getMessage());
            return "admin/group-form";
        }
    }

    @GetMapping("/admin/groups/add")
    public String addGroupForm(Model model) {
        model.addAttribute("group", new Group());
        model.addAttribute("allRoles", GroupMemberRole.values());
        return "admin/group-form";
    }

    @PostMapping("/admin/groups/create")
    public String createGroup(@ModelAttribute("group") Group group,
                              @RequestParam(required = false) String mainTopic,
                              BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("allRoles", GroupMemberRole.values());

        if (bindingResult.hasErrors()) {
            return "admin/group-form";
        }

        try {
            // Set admin as creator
            Long currentUserId = securityUtil.getCurrentUserId();
            if (currentUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xác định người dùng hiện tại.");
                return "admin/group-form";
            }

            User adminUser = userService.findById(currentUserId).orElse(null);
            if (adminUser != null) {
                group.setCreatedBy(adminUser);
            }

            // Save group first
            System.out.println("DEBUG: Saving group: " + group.getName());
            Group savedGroup = groupService.save(group);
            System.out.println("DEBUG: Group saved with ID: " + savedGroup.getId());
            
            // Process main topic if provided
            if (mainTopic != null && !mainTopic.trim().isEmpty()) {
                try {
                    Topic topic = topicService.findOrCreateTopic(mainTopic.trim(), adminUser);
                    savedGroup.getTopics().add(topic);
                    groupService.save(savedGroup);
                    System.out.println("DEBUG: Added main topic to group: " + topic.getName());
                } catch (Exception e) {
                    System.err.println("DEBUG: Error adding main topic: " + e.getMessage());
                    // Don't fail the group creation if topic processing fails
                }
            }

            // Add admin as member
            try {
                System.out.println("DEBUG: Adding admin as member to group: " + savedGroup.getName());
                groupService.addMember(savedGroup.getId(), currentUserId, "ADMIN");
                System.out.println("DEBUG: Admin added as member successfully to group: " + savedGroup.getName());

                // Verify admin is member
                boolean isMember = groupService.isGroupMember(savedGroup.getId(), currentUserId);
                System.out.println("DEBUG: Verification - Admin is member of group " + savedGroup.getName() + ": " + isMember);
            } catch (Exception e) {
                System.err.println("DEBUG: Error adding admin as member: " + e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm admin vào nhóm: " + e.getMessage());
                return "admin/group-form";
            }

            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm thành công.");
            return "redirect:/admin/groups";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo nhóm: " + e.getMessage());
            return "admin/group-form";
        }
    }

    @PostMapping("/admin/groups/bulk-delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkDeleteGroups(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> groupIds = (List<Long>) request.get("groupIds");

            if (groupIds == null || groupIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không có nhóm nào được chọn"));
            }

            int deletedCount = 0;
            for (Long groupId : groupIds) {
                try {
                    groupService.deleteById(groupId);
                    deletedCount++;
                } catch (Exception e) {
                    // Log error but continue with other groups
                    System.err.println("Error deleting group " + groupId + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa " + deletedCount + " nhóm thành công",
                    "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi khi xóa nhóm: " + e.getMessage()));
        }
    }


    @GetMapping("/admin/groups/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGroupStats() {
        try {
            List<Group> allGroups = groupService.findAll();
            long totalGroups = allGroups.size();
            long totalMembers = allGroups.stream()
                    .mapToLong(group -> groupService.getMemberCount(group.getId()))
                    .sum();
            double avgMembersPerGroup = totalGroups > 0 ? (double) totalMembers / totalGroups : 0;

            return ResponseEntity.ok(Map.of(
                    "totalGroups", totalGroups,
                    "totalMembers", totalMembers,
                    "avgMembersPerGroup", Math.round(avgMembersPerGroup * 100.0) / 100.0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/admin/groups/create-simple")
    public String createGroupSimple(@RequestParam String name,
                                    @RequestParam String description,
                                    @RequestParam(required = false) String avatar,
                                    @RequestParam(required = false) String mainTopic,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            if (currentUserId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xác định người dùng hiện tại.");
                return "redirect:/admin/groups";
            }

            // Create group with admin as creator
            Group group = new Group();
            group.setName(name);
            group.setDescription(description);

            // Set admin as creator
            User adminUser = userService.findById(currentUserId).orElse(null);
            if (adminUser != null) {
                group.setCreatedBy(adminUser);
            }

            if (avatar != null && !avatar.trim().isEmpty()) {
                group.setAvatar(avatar.trim());
            }

            Group savedGroup = groupService.save(group);
            
            // Process main topic if provided
            if (mainTopic != null && !mainTopic.trim().isEmpty()) {
                try {
                    Topic topic = topicService.findOrCreateTopic(mainTopic.trim(), adminUser);
                    savedGroup.getTopics().add(topic);
                    groupService.save(savedGroup);
                    System.out.println("DEBUG: Added main topic to group: " + topic.getName());
                } catch (Exception e) {
                    System.err.println("DEBUG: Error adding main topic: " + e.getMessage());
                    // Don't fail the group creation if topic processing fails
                }
            }
            
            // Add admin as member
            try {
                System.out.println("DEBUG: Adding admin as member to group: " + savedGroup.getName());
                groupService.addMember(savedGroup.getId(), currentUserId, "ADMIN");
                System.out.println("DEBUG: Admin added as member successfully to group: " + savedGroup.getName());

                // Verify admin is member
                boolean isMember = groupService.isGroupMember(savedGroup.getId(), currentUserId);
                System.out.println("DEBUG: Verification - Admin is member of group " + savedGroup.getName() + ": " + isMember);
            } catch (Exception e) {
                System.err.println("DEBUG: Error adding admin as member: " + e.getMessage());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo nhóm: " + e.getMessage());
        }
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/{id}/members")
    @ResponseBody
    public ResponseEntity<?> getGroupMembers(@PathVariable Long id) {
        try {
            List<GroupMember> members = groupService.getGroupMembers(id);

            List<Map<String, Object>> memberData = members.stream()
                    .map(member -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", member.getUser().getId());
                        data.put("username", member.getUser().getUsername());
                        data.put("firstName", member.getUser().getFirstName());
                        data.put("lastName", member.getUser().getLastName());
                        data.put("avatar", member.getUser().getUserProfile() != null ?
                                member.getUser().getUserProfile().getAvatar() :
                                "https://i.pravatar.cc/40?u=" + member.getUser().getId());
                        data.put("role", member.getRole().name());
                        data.put("joinedAt", member.getJoinedAt());
                        return data;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(memberData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy danh sách thành viên: " + e.getMessage()));
        }
    }
}
