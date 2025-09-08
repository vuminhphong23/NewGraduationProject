package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.CrawlingConfigDao;
import GraduationProject.forumikaa.dto.CreatePostRequest;
import GraduationProject.forumikaa.entity.CrawlingConfig;
import GraduationProject.forumikaa.entity.PostPrivacy;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DynamicCrawlingService {
    
    @Autowired
    private CrawlingConfigDao crawlingConfigDao;
    
    @Autowired
    private CrawlingConfigService crawlingConfigService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private TopicService topicService;
    
    @Autowired
    private SystemUserService systemUserService;
    
    /**
     * Crawl tất cả configs đang active
     */
    @Scheduled(fixedRate = 300000) // Mỗi 5 phút kiểm tra
    public void crawlAllActiveConfigs() {
        System.out.println("🕷️ Starting dynamic crawling...");
        
        List<CrawlingConfig> configs = crawlingConfigService.getConfigsReadyForCrawling();
        System.out.println("Found " + configs.size() + " configs ready for crawling");
        
        for (CrawlingConfig config : configs) {
            try {
                crawlConfig(config);
            } catch (Exception e) {
                System.err.println("❌ Error crawling config " + config.getName() + ": " + e.getMessage());
                crawlingConfigService.updateCrawlingStats(config.getId(), 0, false);
            }
        }
        
        System.out.println("✅ Dynamic crawling completed");
    }
    
    /**
     * Crawl theo config cụ thể
     */
    public void crawlConfig(CrawlingConfig config) {
        System.out.println("🕷️ Crawling config: " + config.getName());
        
        try {
            // Kết nối và lấy HTML
            Document doc = Jsoup.connect(config.getBaseUrl())
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .get();
            
            // Parse headers nếu có
            Map<String, String> headers = parseHeaders(config.getAdditionalHeaders());
            if (!headers.isEmpty()) {
                doc = Jsoup.connect(config.getBaseUrl())
                        .userAgent(config.getUserAgent())
                        .timeout(config.getTimeout())
                        .headers(headers)
                        .get();
            }
            
            // Lấy elements theo selector
            Elements elements = doc.select(config.getTitleSelector());
            int crawledCount = 0;
            
            for (Element element : elements) {
                if (crawledCount >= config.getMaxPosts()) {
                    break;
                }
                
                try {
                    String title = extractText(element, config.getTitleSelector());
                    String content = extractText(element, config.getContentSelector());
                    String link = extractAttribute(element, config.getLinkSelector(), "href");
                    String image = extractAttribute(element, config.getImageSelector(), "src");
                    String author = extractText(element, config.getAuthorSelector());
                    String date = extractText(element, config.getDateSelector());
                    String topic = extractText(element, config.getTopicSelector());
                    
                    if (title != null && !title.trim().isEmpty()) {
                        createPostFromCrawledData(config, title, content, link, image, author, date, topic);
                        crawledCount++;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error processing element: " + e.getMessage());
                }
            }
            
            // Cập nhật thống kê
            crawlingConfigService.updateCrawlingStats(config.getId(), crawledCount, true);
            System.out.println("✅ Crawled " + crawledCount + " posts from " + config.getName());
            
        } catch (IOException e) {
            System.err.println("❌ Error connecting to " + config.getBaseUrl() + ": " + e.getMessage());
            crawlingConfigService.updateCrawlingStats(config.getId(), 0, false);
        }
    }
    
    /**
     * Test crawl một config
     */
    public String testCrawlConfig(CrawlingConfig config) {
        try {
            Document doc = Jsoup.connect(config.getBaseUrl())
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .get();
            
            Elements elements = doc.select(config.getTitleSelector());
            StringBuilder result = new StringBuilder();
            result.append("✅ Test successful!\n");
            result.append("Found ").append(elements.size()).append(" elements matching selector: ").append(config.getTitleSelector()).append("\n\n");
            
            int count = 0;
            for (Element element : elements) {
                if (count >= 3) break; // Chỉ lấy 3 mẫu đầu
                
                String title = extractText(element, config.getTitleSelector());
                String content = extractText(element, config.getContentSelector());
                String link = extractAttribute(element, config.getLinkSelector(), "href");
                
                result.append("Sample ").append(count + 1).append(":\n");
                result.append("Title: ").append(title != null ? title : "N/A").append("\n");
                result.append("Content: ").append(content != null ? content.substring(0, Math.min(100, content.length())) + "..." : "N/A").append("\n");
                result.append("Link: ").append(link != null ? link : "N/A").append("\n\n");
                
                count++;
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "❌ Test failed: " + e.getMessage();
        }
    }
    
    private String extractText(Element element, String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            return null;
        }
        
        try {
            Element selected = element.selectFirst(selector);
            return selected != null ? selected.text().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractAttribute(Element element, String selector, String attribute) {
        if (selector == null || selector.trim().isEmpty()) {
            return null;
        }
        
        try {
            Element selected = element.selectFirst(selector);
            if (selected != null) {
                String attr = selected.attr(attribute);
                // Nếu là relative URL, convert thành absolute
                if (attribute.equals("href") || attribute.equals("src")) {
                    if (attr.startsWith("/")) {
                        // TODO: Convert relative URL to absolute
                        return attr;
                    }
                }
                return attr;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Map<String, String> parseHeaders(String headersString) {
        Map<String, String> headers = new HashMap<>();
        if (headersString == null || headersString.trim().isEmpty()) {
            return headers;
        }
        
        try {
            String[] lines = headersString.split("\n");
            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    headers.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing headers: " + e.getMessage());
        }
        
        return headers;
    }
    
    @Transactional
    private void createPostFromCrawledData(CrawlingConfig config, String title, String content, 
                                         String link, String image, String author, String date, String topic) {
        try {
            User adminUser = systemUserService.getAdminUser();
            
            // Tạo topic
            String topicName = topic != null && !topic.trim().isEmpty() ? topic : config.getTopicName();
            Topic topicEntity = topicService.findOrCreateTopic(topicName, adminUser);
            
            // Tạo content
            StringBuilder postContent = new StringBuilder();
            if (content != null && !content.trim().isEmpty()) {
                postContent.append(content);
            }
            
            if (author != null && !author.trim().isEmpty()) {
                postContent.append("\n\n**Tác giả:** ").append(author);
            }
            
            if (date != null && !date.trim().isEmpty()) {
                postContent.append("\n**Ngày:** ").append(date);
            }
            
            if (link != null && !link.trim().isEmpty()) {
                postContent.append("\n\n🔗 **Nguồn:** ").append(link);
            }
            
            if (image != null && !image.trim().isEmpty()) {
                postContent.append("\n\n🖼️ **Hình ảnh:** ").append(image);
            }
            
            // Tạo post request
            CreatePostRequest request = new CreatePostRequest();
            request.setTitle("📰 " + title);
            request.setContent(postContent.toString());
            request.setPrivacy(PostPrivacy.PUBLIC);
            request.setTopicNames(List.of(topicName));
            
            // Tạo post
            postService.createPost(request, adminUser.getId());
            
            System.out.println("✅ Created post: " + title);
            
        } catch (Exception e) {
            System.err.println("❌ Error creating post: " + e.getMessage());
        }
    }
}


