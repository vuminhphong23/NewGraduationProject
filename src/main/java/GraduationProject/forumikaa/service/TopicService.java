package GraduationProject.forumikaa.service;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;

import java.util.List;


public interface TopicService {
    
    // Tìm hoặc tạo hashtag mới
    Topic findOrCreateTopic(String name, User createdBy);
    
    // Xử lý danh sách hashtag từ string input
    List<Topic> processTopicsFromInput(List<String> topicNames, User createdBy);
    
    // Lấy top hashtags
    List<Topic> getTopTopics(int limit);
    
    // Tăng usage count cho hashtag
    void incrementUsageCount(Topic topic);
    
    // Giảm usage count cho hashtag
    void decrementUsageCount(Topic topic);

}
