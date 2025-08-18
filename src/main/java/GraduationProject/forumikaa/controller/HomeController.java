package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.PostDto;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.TopicService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        try {
            userName = securityUtil.getCurrentUsername();
            userId = securityUtil.getCurrentUserId();
        } catch (RuntimeException ignored) {}

        List<PostDto> posts = (userId != null)
                ? postService.getUserFeed(userId)
                : List.of(); // hoặc cho phép xem public feed

        List<Topic> topics = topicService.getAllTopics();

        model.addAttribute("userName", userName);
        model.addAttribute("posts", posts);
        model.addAttribute("topics", topics);
        return "user/index";
    }


} 