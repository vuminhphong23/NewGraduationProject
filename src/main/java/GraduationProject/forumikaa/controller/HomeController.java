package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.TopicService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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

        List<PostResponse> posts = (userId != null)
                ? postService.getUserFeed(userId)
                : List.of();

        // Đơn giản hóa logic - chỉ lấy trending topics
        List<Topic> trendingTopics = topicService.getTrendingTopics();

        // Nếu không có trending topics, lấy top topics
        if (trendingTopics.isEmpty()) {
            trendingTopics = topicService.getTopTopics(10);
        }

        model.addAttribute("userName", userName);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("trendingTopics", trendingTopics);
        return "user/index";
    }



    @GetMapping("/recommendations")
    public String recommendations(Model model) {
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
        return "user/recommendations";
    }


}