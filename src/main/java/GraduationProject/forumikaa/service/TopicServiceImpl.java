package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TopicServiceImpl implements TopicService {

    @Autowired
    private TopicDao topicDao;

    @Override
    public Topic findOrCreateTopic(String name, User createdBy) {
        // Làm sạch tên hashtag (bỏ # nếu có, chuyển thành lowercase, thay space bằng _)
        String cleanName = cleanHashtagName(name);
        
        return topicDao.findByName(cleanName)
                .orElseGet(() -> {
                    Topic newTopic = new Topic(cleanName);
                    newTopic.setCreatedBy(createdBy);
                    newTopic.setUsageCount(0); // Khởi tạo usage count
                    return topicDao.save(newTopic);
                });
    }

    @Override
    public List<Topic> processTopicsFromInput(List<String> topicNames, User createdBy) {
        if (topicNames == null || topicNames.isEmpty()) {
            return List.of();
        }
        
        return topicNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(name -> findOrCreateTopic(name, createdBy))
                .distinct() // Tránh duplicate
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getTrendingTopics() {
        return topicDao.findTrendingTopics();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getTopTopics(int limit) {
        List<Topic> allTop = topicDao.findTopTopics();
        return allTop.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void incrementUsageCount(Topic topic) {
        if (topic.getUsageCount() == null) {
            topic.setUsageCount(1);
        } else {
            topic.setUsageCount(topic.getUsageCount() + 1);
        }
        topicDao.save(topic);
    }

    @Override
    public void decrementUsageCount(Topic topic) {
        if (topic.getUsageCount() != null && topic.getUsageCount() > 0) {
            topic.setUsageCount(topic.getUsageCount() - 1);
            topicDao.save(topic);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> searchTopics(String keyword) {
        String cleanKeyword = cleanHashtagName(keyword);
        return topicDao.findByNameContaining(cleanKeyword);
    }

    @Override
    public void updateTrendingStatus() {
        // Lấy top 10 hashtags có usage count cao nhất
        List<Topic> topTopics = getTopTopics(10);
        
        // Reset tất cả trending status
        List<Topic> allTopics = topicDao.findAll();
        allTopics.forEach(topic -> topic.setTrending(false));

        // Set trending cho top hashtags
        topTopics.forEach(topic -> topic.setTrending(true));

        topicDao.saveAll(allTopics);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getAllTopics() {
        return topicDao.getAllTopics();
    }

    /**
     * Helper method để làm sạch tên hashtag
     */
    private String cleanHashtagName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        // Bỏ dấu # nếu có
        String cleanName = name.trim();
        if (cleanName.startsWith("#")) {
            cleanName = cleanName.substring(1);
        }
        
        // Chuyển thành lowercase và làm sạch ký tự đặc biệt
        cleanName = cleanName.toLowerCase()
                .replaceAll("\\s+", "_") // Thay space bằng _
                .replaceAll("[^a-zA-Z0-9_àáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]", "");
        
        return cleanName;
    }
}
