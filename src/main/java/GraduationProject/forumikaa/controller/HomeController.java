package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.dto.SuggestedPostDto;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.TopicService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
public class HomeController {

    private PostService postService;

    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    private TopicService topicService;

    @Autowired
    public void setTopicService(TopicService topicService) {
        this.topicService = topicService;
    }

    private SecurityUtil securityUtil;

    @Autowired
    public void setSecurityUtil(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @GetMapping("/")
    public String home(Model model) {
        String userName = "Khách";
        Long userId = null;
        GraduationProject.forumikaa.entity.User user = null;
        try {
            userName = securityUtil.getCurrentUsername();
            userId = securityUtil.getCurrentUserId();
            if (userId != null) {
                user = securityUtil.getCurrentUser();
            }
        } catch (RuntimeException ignored) {}

        List<PostDto> posts = (userId != null)
                ? postService.getUserFeed(userId)
                : List.of();

        List<Topic> trendingTopics = topicService.getTrendingTopics();
        
        if (trendingTopics.isEmpty()) {
            trendingTopics = topicService.getTopTopics(10);
        }
        
        trendingTopics = trendingTopics.stream()
                .filter(topic -> topic.getUsageCount() != null && topic.getUsageCount() > 0)
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("userName", userName);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("trendingTopics", trendingTopics);
        return "user/index";
    }

    @GetMapping("/suggested-posts")
    public String suggestedPosts(Model model) {
        String userName = "Khách";
        Long userId = null;
        GraduationProject.forumikaa.entity.User user = null;
        try {
            userName = securityUtil.getCurrentUsername();
            userId = securityUtil.getCurrentUserId();
            if (userId != null) {
                user = securityUtil.getCurrentUser();
            }
        } catch (RuntimeException ignored) {}

        model.addAttribute("userName", userName);
        model.addAttribute("user", user);
        return "user/suggested-posts";
    }

    /**
     * API endpoint để lấy suggested posts dựa trên mối quan hệ bạn bè
     */
    @GetMapping("/api/suggested-posts")
    public ResponseEntity<List<SuggestedPostDto>> getSuggestedPosts(
            @RequestParam(value = "maxLevel", defaultValue = "3") Integer maxLevel,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        
        try {
            Long userId = null;
            try {
                userId = securityUtil.getCurrentUserId();
            } catch (RuntimeException e) {
                System.err.println("Error getting current user ID: " + e.getMessage());
                return ResponseEntity.badRequest().build();
            }
            
            if (userId == null) {
                System.err.println("User ID is null");
                return ResponseEntity.badRequest().build();
            }
            
            System.out.println("Processing request for userId: " + userId + ", maxLevel: " + maxLevel + ", limit: " + limit);
            
            List<SuggestedPostDto> suggestedPosts = postService.getSuggestedPosts(userId, maxLevel, limit);
            return ResponseEntity.ok(suggestedPosts);
        } catch (Exception e) {
            System.err.println("Error in getSuggestedPosts API: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}