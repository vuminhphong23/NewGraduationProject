package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/posts")
public class PostViewController {

    @Autowired
    private PostService postService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/{postId}")
    public String viewPost(@PathVariable Long postId, Model model) {

        Long userId = securityUtil.getCurrentUserId();
        PostResponse post = postService.getPostById(postId, userId);
            
        model.addAttribute("post", post);
        model.addAttribute("user", securityUtil.getCurrentUser());
        model.addAttribute("userName", securityUtil.getCurrentUsername());
            
        return "user/post-detail";

    }
}
