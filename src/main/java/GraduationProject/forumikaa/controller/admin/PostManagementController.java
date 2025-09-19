package GraduationProject.forumikaa.controller.admin;

import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.service.GroupService;
import GraduationProject.forumikaa.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class PostManagementController {
    private PostService postService;

    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    private GroupService groupService;

    @Autowired
    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    // Static maps for status and privacy mapping
    private static final Map<PostStatus, String> STATUS_TEXT_MAP = Map.of(
            PostStatus.PENDING, "Chờ duyệt",
            PostStatus.APPROVED, "Đã duyệt",
            PostStatus.REJECTED, "Từ chối"
    );

    private static final Map<PostStatus, String> STATUS_CLASS_MAP = Map.of(
            PostStatus.PENDING, "badge badge-status-pending",
            PostStatus.APPROVED, "badge badge-status-approved",
            PostStatus.REJECTED, "badge badge-status-rejected"
    );

    private static final Map<PostPrivacy, String> PRIVACY_TEXT_MAP = Map.of(
            PostPrivacy.PUBLIC, "Công khai",
            PostPrivacy.PRIVATE, "Riêng tư",
            PostPrivacy.FRIENDS, "Bạn bè"
    );

    private static final Map<PostPrivacy, String> PRIVACY_CLASS_MAP = Map.of(
            PostPrivacy.PUBLIC, "badge badge-privacy-public",
            PostPrivacy.PRIVATE, "badge badge-privacy-private",
            PostPrivacy.FRIENDS, "badge badge-privacy-friends"
    );
    @GetMapping("/admin/posts")
    public String postManagementPage(Model model,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false, defaultValue = "") String status,
                                     @RequestParam(required = false, defaultValue = "") String privacy) {
        // Tạo PageRequest với page-1 vì Spring Data sử dụng 0-based indexing
        PageRequest pageRequest = PageRequest.of(page - 1, size);

        // Lấy danh sách bài viết với phân trang và filter
        Page<Post> postPage = postService.findPaginated(keyword, status, privacy, pageRequest);

        model.addAttribute("postPage", postPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("privacy", privacy);
        model.addAttribute("allStatuses", PostStatus.values());
        model.addAttribute("allPrivacies", PostPrivacy.values());

        // Add maps for status and privacy display
        model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
        model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
        model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
        model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);

        return "admin/post-management";
    }

    @PostMapping("/admin/posts/{id}/toggle-status")
    public String togglePostStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));

            // Toggle status: PENDING -> APPROVED, APPROVED -> PENDING, REJECTED -> PENDING
            PostStatus newStatus;
            if (post.getStatus() == PostStatus.PENDING) {
                newStatus = PostStatus.APPROVED;
            } else if (post.getStatus() == PostStatus.APPROVED) {
                newStatus = PostStatus.PENDING;
            } else { // REJECTED
                newStatus = PostStatus.PENDING;
            }

            post.setStatus(newStatus);
            postService.save(post);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }

        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/approve")
    public String approvePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            post.setStatus(PostStatus.APPROVED);
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi duyệt bài viết: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/reject")
    public String rejectPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            post.setStatus(PostStatus.REJECTED);
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Từ chối bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi từ chối bài viết: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @PostMapping("/admin/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            postService.deleteById(id); // Admin có thể xóa bất kỳ bài viết nào
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa bài viết: " + e.getMessage());
        }
        return "redirect:/admin/posts";
    }

    @GetMapping("/admin/posts/detail/{id}")
    public String viewPostDetail(@PathVariable Long id,
                                 @RequestParam(required = false) Long groupId,
                                 Model model) {
        try {
            Post post = postService.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với id " + id));
            model.addAttribute("post", post);
            model.addAttribute("groupId", groupId);
            model.addAttribute("allStatuses", PostStatus.values());
            model.addAttribute("allPrivacies", PostPrivacy.values());

            // Add maps for status and privacy display
            model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
            model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
            model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
            model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);

            return "admin/post-form";
        } catch (Exception e) {
            return "redirect:/admin/posts?error=" + e.getMessage();
        }
    }

    @PostMapping("/admin/posts/bulk-delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkDeletePosts(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("DEBUG: Received request: " + request);

            @SuppressWarnings("unchecked")
            List<Object> postIdsObj = (List<Object>) request.get("postIds");

            if (postIdsObj == null || postIdsObj.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không có bài viết nào được chọn"));
            }

            // Convert to Long list
            List<Long> postIds = postIdsObj.stream()
                    .map(obj -> {
                        if (obj instanceof Number) {
                            return ((Number) obj).longValue();
                        } else if (obj instanceof String) {
                            return Long.parseLong((String) obj);
                        }
                        throw new IllegalArgumentException("Cannot convert to Long: " + obj);
                    })
                    .toList();

            System.out.println("DEBUG: PostIds to delete: " + postIds);

            int deletedCount = 0;
            for (Long postId : postIds) {
                try {
                    postService.deleteById(postId);
                    deletedCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa " + deletedCount + " bài viết thành công",
                    "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Lỗi khi xóa bài viết: " + e.getMessage()));
        }
    }

    // ========== GROUP POST MANAGEMENT ==========

    @GetMapping("/admin/groups/{groupId}/posts")
    public String groupPostManagementPage(@PathVariable Long groupId,
                                          Model model,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false, defaultValue = "") String status) {
        try {
            // Lấy thông tin nhóm
            UserGroup group = groupService.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm"));

            // Tạo PageRequest
            PageRequest pageRequest = PageRequest.of(page - 1, size);

            // Lấy bài viết theo nhóm với phân trang
            Page<Post> postPage = postService.findPostsByGroup(groupId, keyword, status, pageRequest);

            model.addAttribute("group", group);
            model.addAttribute("postPage", postPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("status", status);
            model.addAttribute("allStatuses", PostStatus.values());

            // Add maps for status and privacy display
            model.addAttribute("statusTextMap", STATUS_TEXT_MAP);
            model.addAttribute("statusClassMap", STATUS_CLASS_MAP);
            model.addAttribute("privacyTextMap", PRIVACY_TEXT_MAP);
            model.addAttribute("privacyClassMap", PRIVACY_CLASS_MAP);

            return "admin/group-post-management";
        } catch (Exception e) {
            return "redirect:/admin/groups?error=" + e.getMessage();
        }
    }

    @PostMapping("/admin/groups/{groupId}/posts/{postId}/approve")
    public String approveGroupPost(@PathVariable Long groupId,
                                   @PathVariable Long postId,
                                   RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(postId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));

            if (post.getStatus() == PostStatus.PENDING) {
                post.setStatus(PostStatus.APPROVED);
                redirectAttributes.addFlashAttribute("successMessage", "Duyệt bài viết thành công.");
            } else if (post.getStatus() == PostStatus.REJECTED) {
                post.setStatus(PostStatus.APPROVED);
                redirectAttributes.addFlashAttribute("successMessage", "Duyệt lại bài viết thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể duyệt bài viết ở trạng thái hiện tại.");
            }

            postService.save(post);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi duyệt bài viết: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }

    @PostMapping("/admin/groups/{groupId}/posts/{postId}/toggle-status")
    public String toggleGroupPostStatus(@PathVariable Long groupId,
                                        @PathVariable Long postId,
                                        RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(postId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));

            if (post.getStatus() == PostStatus.APPROVED) {
                post.setStatus(PostStatus.PENDING);
                redirectAttributes.addFlashAttribute("successMessage", "Chuyển bài viết về trạng thái chờ duyệt.");
            } else if (post.getStatus() == PostStatus.PENDING) {
                post.setStatus(PostStatus.APPROVED);
                redirectAttributes.addFlashAttribute("successMessage", "Duyệt bài viết thành công.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể thay đổi trạng thái bài viết ở trạng thái hiện tại.");
            }

            postService.save(post);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }

    @PostMapping("/admin/groups/{groupId}/posts/{postId}/reject")
    public String rejectGroupPost(@PathVariable Long groupId,
                                  @PathVariable Long postId,
                                  RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.findById(postId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
            post.setStatus(PostStatus.REJECTED);
            postService.save(post);
            redirectAttributes.addFlashAttribute("successMessage", "Từ chối bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi từ chối bài viết: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }

    @PostMapping("/admin/groups/{groupId}/posts/{postId}/delete")
    public String deleteGroupPost(@PathVariable Long groupId,
                                  @PathVariable Long postId,
                                  RedirectAttributes redirectAttributes) {
        try {
            postService.deleteById(postId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa bài viết: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }

    @PostMapping("/admin/groups/{groupId}/posts/bulk-delete")
    public String bulkDeleteGroupPosts(@PathVariable Long groupId,
                                       @RequestParam("postIds") List<Long> postIds,
                                       RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = 0;
            for (Long postId : postIds) {
                try {
                    postService.deleteById(postId);
                    deletedCount++;
                } catch (Exception e) {
                    System.err.println("Error deleting post " + postId + ": " + e.getMessage());
                }
            }

            if (deletedCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành công " + deletedCount + " bài viết!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa bài viết nào!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa hàng loạt: " + e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId + "/posts";
    }

    @GetMapping("/admin/groups/{groupId}/posts/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGroupPostStats(@PathVariable Long groupId) {
        try {
            List<Post> groupPosts = postService.findPostsByGroup(groupId, null, null, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

            long totalPosts = groupPosts.size();
            long pendingPosts = groupPosts.stream().filter(p -> p.getStatus() == PostStatus.PENDING).count();
            long approvedPosts = groupPosts.stream().filter(p -> p.getStatus() == PostStatus.APPROVED).count();
            long rejectedPosts = groupPosts.stream().filter(p -> p.getStatus() == PostStatus.REJECTED).count();

            Map<String, Object> stats = Map.of(
                    "totalPosts", totalPosts,
                    "pendingPosts", pendingPosts,
                    "approvedPosts", approvedPosts,
                    "rejectedPosts", rejectedPosts
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
