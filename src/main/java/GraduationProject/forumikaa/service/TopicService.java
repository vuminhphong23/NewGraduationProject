package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.TopicDao.TopicSummaryProjection;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;

import java.util.List;
import java.util.Set;

public interface TopicService {

    // Tìm hoặc tạo hashtag mới
    Topic findOrCreateTopic(String name, User createdBy);

    // Xử lý danh sách hashtag từ string input
    List<Topic> processTopicsFromInput(List<String> topicNames, User createdBy);

    // Lấy trending hashtags
    List<Topic> getTrendingTopics();

    // Lấy top hashtags
    List<Topic> getTopTopics(int limit);

    // Tăng usage count cho hashtag
    void incrementUsageCount(Topic topic);

    // Giảm usage count cho hashtag
    void decrementUsageCount(Topic topic);

    // Tìm hashtags theo keyword
    List<Topic> searchTopics(String keyword);

    // Cập nhật trending status
    void updateTrendingStatus();

    // Lấy tất cả hashtags
    List<Topic> getAllTopics();



    // Các phương thức mới sử dụng projection (hiệu suất cao hơn)
    List<TopicSummaryProjection> getTopTopicsProjection(int limit);
    List<TopicSummaryProjection> getTrendingTopicsProjection();

}
