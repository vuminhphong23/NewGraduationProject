package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.CrawlingConfig;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserGroup;

public interface DynamicCrawlingService {
    
    /**
     * Crawl tất cả configs đang active
     */
    void crawlAllActiveConfigs();
    
    /**
     * Crawl theo config cụ thể
     */
    void crawlConfig(CrawlingConfig config);

    void createPostFromCrawledData(CrawlingConfig config, String title, String content, String link);

    void createPostForGroup(User adminUser, UserGroup group, String title, String content, String topicName);
}
