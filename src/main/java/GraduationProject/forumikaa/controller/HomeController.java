package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.GroupMemberDto;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.service.PostService;
import GraduationProject.forumikaa.service.TopicService;
import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

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


    private GroupMemberDao groupMemberDao;

    @Autowired
    public void setGroupMemberDao(GroupMemberDao groupMemberDao) {
        this.groupMemberDao = groupMemberDao;
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
        List<Topic> trendingTopics = topicService.getTopTopics(10);

        // Lấy danh sách nhóm của user (nếu đã đăng nhập)
        List<GroupMemberDto> userGroups = List.of();
        if (userId != null) {
            try {
                List<GroupMember> groupMembers = groupMemberDao.findByUserId(userId);
                userGroups = groupMembers.stream()
                        .map(member -> GroupMemberDto.builder()
                                .id(member.getGroup().getId())
                                .userId(member.getUser().getId())
                                .username(member.getGroup().getName())
                                .firstName(member.getGroup().getName())
                                .lastName("")
                                .fullName(member.getGroup().getName())
                                .avatar(member.getGroup().getAvatar() != null ? 
                                        member.getGroup().getAvatar() : 
                                        "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60")
                                .role(member.getRole().name())
                                .isOnline(false) // Groups không có online status
                                .joinedAt(member.getJoinedAt())
                                .memberCount(postService.getNewPostCountByGroupToday(member.getGroup().getId()))
                                .postCount(0L)
                                .build())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Nếu có lỗi, để danh sách rỗng
                userGroups = List.of();
            }
        }

        model.addAttribute("userName", userName);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("trendingTopics", trendingTopics);
        model.addAttribute("userGroups", userGroups);
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

    @GetMapping("/chat")
    public String chat(Model model) {
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
        return "user/chat";
    }

}