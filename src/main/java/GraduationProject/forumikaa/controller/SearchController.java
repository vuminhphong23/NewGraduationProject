package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.service.SearchService;
import GraduationProject.forumikaa.service.GroupService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/api/search/live")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> liveSearch(@RequestParam String q,
                                                                @RequestParam(defaultValue = "all") String category) {
        try {
            System.out.println("Live search request - Query: " + q + ", Category: " + category);
            
            if (q == null || q.trim().length() < 2) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            Pageable pageable = PageRequest.of(0, 5); // Limit to 5 results for live search
            List<Map<String, Object>> results = new ArrayList<>();
            
            if ("all".equals(category) || "posts".equals(category)) {
                List<Post> posts = searchService.searchPosts(q, pageable);
                for (Post post : posts) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", "post");
                    result.put("id", post.getId());
                    result.put("title", post.getTitle() != null ? post.getTitle() : "");
                    result.put("content", post.getContent() != null ? post.getContent() : "");
                    result.put("author", post.getUser() != null ? 
                        (post.getUser().getFirstName() != null ? post.getUser().getFirstName() : "") + " " + 
                        (post.getUser().getLastName() != null ? post.getUser().getLastName() : "") : "Unknown");
                    result.put("likeCount", post.getLikeCount() != null ? post.getLikeCount() : 0);
                    result.put("commentCount", post.getCommentCount() != null ? post.getCommentCount() : 0);
                    results.add(result);
                }
            }
            
            if ("all".equals(category) || "users".equals(category)) {
                List<User> users = searchService.searchUsers(q, pageable);
                for (User user : users) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", "user");
                    result.put("id", user.getId());
                    result.put("username", user.getUsername() != null ? user.getUsername() : "");
                    result.put("fullName", (user.getFirstName() != null ? user.getFirstName() : "") + " " + 
                        (user.getLastName() != null ? user.getLastName() : ""));
                    result.put("avatar", user.getUserProfile() != null && user.getUserProfile().getAvatar() != null ? 
                        user.getUserProfile().getAvatar() : null);
                    results.add(result);
                }
            }
            
            if ("all".equals(category) || "groups".equals(category)) {
                List<UserGroup> groups = searchService.searchGroups(q, pageable);
                for (UserGroup group : groups) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", "group");
                    result.put("id", group.getId());
                    result.put("name", group.getName() != null ? group.getName() : "");
                    result.put("description", group.getDescription() != null ? group.getDescription() : "");
                    result.put("memberCount", group.getMemberCount() != null ? group.getMemberCount() : 0);
                    results.add(result);
                }
            }
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            System.err.println("Live search error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "category", defaultValue = "all") String category,
            Model model) {
        
        try {
            System.out.println("Search request - Query: " + query + ", Category: " + category);
            
            long startTime = System.currentTimeMillis();
            
            if (query == null || query.trim().isEmpty()) {
                model.addAttribute("query", "");
                model.addAttribute("hasResults", false);
                model.addAttribute("totalResults", 0);
                model.addAttribute("searchTime", 0);
                return "user/search-results";
            }
            
            query = query.trim();
            model.addAttribute("query", query);
            
            List<Post> posts = new ArrayList<>();
            List<User> users = new ArrayList<>();
            List<UserGroup> groups = new ArrayList<>();
            
            // Search based on category
            Pageable pageable = PageRequest.of(0, 20);
            
            if ("all".equals(category) || "posts".equals(category)) {
                System.out.println("Searching posts...");
                posts = searchService.searchPosts(query, pageable);
                System.out.println("Found " + posts.size() + " posts");
            }
            
            if ("all".equals(category) || "users".equals(category)) {
                System.out.println("Searching users...");
                users = searchService.searchUsers(query, pageable);
                System.out.println("Found " + users.size() + " users");
            }
            
            if ("all".equals(category) || "groups".equals(category)) {
                System.out.println("Searching groups...");
                groups = searchService.searchGroups(query, pageable);
                System.out.println("Found " + groups.size() + " groups");
            }
            
            // Check membership for groups if user is logged in
            Long currentUserId = null;
            Map<Long, Boolean> groupMembership = new HashMap<>();
            try {
                currentUserId = securityUtil.getCurrentUserId();
                if (currentUserId != null && !groups.isEmpty()) {
                    for (UserGroup group : groups) {
                        boolean isMember = groupService.isGroupMember(group.getId(), currentUserId);
                        groupMembership.put(group.getId(), isMember);
                    }
                }
            } catch (Exception e) {
                System.out.println("User not logged in, skipping membership check");
            }
            
            long endTime = System.currentTimeMillis();
            long searchTime = endTime - startTime;
            
            int totalResults = posts.size() + users.size() + groups.size();
            boolean hasResults = totalResults > 0;
            
            System.out.println("Total results: " + totalResults + ", Search time: " + searchTime + "ms");
            
            model.addAttribute("posts", posts);
            model.addAttribute("users", users);
            model.addAttribute("groups", groups);
            model.addAttribute("groupMembership", groupMembership);
            model.addAttribute("hasResults", hasResults);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("searchTime", searchTime);
            model.addAttribute("category", category);
            
            return "user/search-results";
            
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            
            // Return error page or fallback
            model.addAttribute("query", query != null ? query : "");
            model.addAttribute("hasResults", false);
            model.addAttribute("totalResults", 0);
            model.addAttribute("searchTime", 0);
            model.addAttribute("error", "Có lỗi xảy ra khi tìm kiếm: " + e.getMessage());
            
            return "user/search-results";
        }
    }
    
}
