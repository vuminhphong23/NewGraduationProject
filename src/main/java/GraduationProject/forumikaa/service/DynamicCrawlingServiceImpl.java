package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.CrawlingConfigDao;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.entity.CrawlingConfig;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class DynamicCrawlingServiceImpl implements DynamicCrawlingService {

    @Autowired
    private CrawlingConfigService crawlingConfigService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private TopicService topicService;
    
    @Autowired
    private SmartSelectorService smartSelectorService;
    
    @Autowired
    private GroupService groupService;

    @Autowired
    private SecurityUtil securityUtil;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Crawl tất cả configs đang active
     */
    @Scheduled(fixedRate = 300000) // Mỗi 5 phút kiểm tra
    @Override
    public void crawlAllActiveConfigs() {
        List<CrawlingConfig> configs = crawlingConfigService.getConfigsReadyForCrawling();

        for (CrawlingConfig config : configs) {
            try {
                crawlConfig(config);
            } catch (Exception e) {
                crawlingConfigService.updateCrawlingStats(config.getId(), 0, false);
            }
        }
    }
    
    @Override
    public void crawlConfig(CrawlingConfig config) {
        try {
            // Kết nối và lấy HTML
            Document doc = Jsoup.connect(config.getBaseUrl())
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .get();
            
            // Sử dụng Smart Selector với topic filter để tự động extract articles
            List<Map<String, String>> articles = smartSelectorService.extractArticles(doc, config.getMaxPosts(), config.getTopicName());
            int crawledCount = 0;
            
            for (Map<String, String> article : articles) {
                try {
                    String title = article.get("title");
                    String content = article.get("content");
                    String link = article.get("link");
                    
                    if (title != null && !title.trim().isEmpty()) {
                        createPostFromCrawledData(config, title, content, link);
                        crawledCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Cập nhật thống kê
            crawlingConfigService.updateCrawlingStats(config.getId(), crawledCount, true);

        } catch (IOException e) {
            crawlingConfigService.updateCrawlingStats(config.getId(), 0, false);
        }
    }
    
    /**
     * Tạo post từ dữ liệu đã crawl được
     */
    @Override
    public void createPostFromCrawledData(CrawlingConfig config, String title, String content, String link) {
        try {
            User adminUser = securityUtil.getAdminUser();
            
            // Tạo topic từ config
            topicService.findOrCreateTopic(config.getTopicName(), adminUser);
            
            // Tạo content đơn giản chỉ với 3 thông tin cần thiết
            StringBuilder postContent = new StringBuilder();
            if (content != null && !content.trim().isEmpty()) {
                postContent.append(content);
            }
            
            if (link != null && !link.trim().isEmpty()) {
                postContent.append("\n\n🔗 **Nguồn:** ").append(link);
            }
            
            // Parse groupIds từ config
            List<Long> groupIds = parseGroupIds(config.getGroupIds());
            
            if (groupIds != null && !groupIds.isEmpty()) {
                // Tạo post cho mỗi group đã chọn
                for (Long groupId : groupIds) {
                    try {
                        UserGroup group = groupService.findById(groupId).orElse(null);
                        if (group != null) {
                            // Check if admin is member of group
                            boolean isMember = groupService.isGroupMember(groupId, adminUser.getId());

                            if (isMember) {
                                createPostForGroup(adminUser, group, title, postContent.toString(), config.getTopicName());
                            } else {
                                try {
                                    groupService.addMember(groupId, adminUser.getId(), "ADMIN");
                                    createPostForGroup(adminUser, group, title, postContent.toString(), config.getTopicName());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Tạo post thông thường nếu không có group nào được chọn
                CreatePostRequest request = new CreatePostRequest();
                request.setTitle(title);
                request.setContent(postContent.toString());
                request.setPrivacy(PostPrivacy.PUBLIC);
                request.setTopicNames(List.of(config.getTopicName()));
                
                postService.createPost(request, adminUser.getId());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Parse groupIds từ JSON string
     */
    private List<Long> parseGroupIds(String groupIdsJson) {
        if (groupIdsJson == null || groupIdsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(groupIdsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Tạo post cho một group cụ thể
     */
    @Override
    public void createPostForGroup(User adminUser, UserGroup group, String title, String content, String topicName) {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setPrivacy(PostPrivacy.PUBLIC);
        request.setTopicNames(List.of(topicName));
        request.setGroupId(group.getId()); // Set groupId để tạo post trong group

        postService.createPost(request, adminUser.getId());
    }
}


