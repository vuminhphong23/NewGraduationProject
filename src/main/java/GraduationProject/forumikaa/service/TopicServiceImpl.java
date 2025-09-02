package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
        String cleanName = cleanHashtagName(name);
        
        return topicDao.findByName(cleanName)
                .orElseGet(() -> {
                    Topic newTopic = new Topic(cleanName);
                    newTopic.setCreatedBy(createdBy);
                    newTopic.setUsageCount(0);
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
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getTrendingTopics() {
        // Lấy trending topics, nếu không có thì lấy top topics
        List<Topic> trendingTopics = topicDao.findTrendingTopics();
        if (trendingTopics.isEmpty()) {
            trendingTopics = getTopTopics(10);
        }
        
        // Lọc chỉ lấy topics có usageCount > 0
        return trendingTopics.stream()
                .filter(topic -> topic.getUsageCount() != null && topic.getUsageCount() > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getTopTopics(int limit) {
        List<Topic> allTop = topicDao.findTopTopics();
        return allTop.stream()
                .filter(topic -> topic.getUsageCount() != null && topic.getUsageCount() > 0)
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
    public void updateTrendingStatus() {
        List<Topic> topTopics = getTopTopics(10);
        
        List<Topic> allTopics = topicDao.findAll();
        allTopics.forEach(topic -> topic.setTrending(false));

        topTopics.forEach(topic -> topic.setTrending(true));

        topicDao.saveAll(allTopics);
    }
    
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void scheduledUpdateTrendingStatus() {
        try {
            updateTrendingStatus();
            System.out.println("Đã cập nhật trending status cho topics");
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trending status: " + e.getMessage());
        }
    }

    /**
     * Helper method để làm sạch tên hashtag
     */
    private String cleanHashtagName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        String cleanName = name.trim();
        if (cleanName.startsWith("#")) {
            cleanName = cleanName.substring(1);
        }
        
        cleanName = cleanName.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_àáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]", "");
        
        return cleanName;
    }
}
