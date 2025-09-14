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
        String cleanName = cleanHashtagName(name);
        System.out.println("DEBUG: TopicService.findOrCreateTopic() called with name: " + name + ", cleanName: " + cleanName);
        
        return topicDao.findByName(cleanName)
                .orElseGet(() -> {
                    System.out.println("DEBUG: Creating new topic: " + cleanName);
                    Topic newTopic = new Topic(cleanName);
                    newTopic.setCreatedBy(createdBy);
                    newTopic.setUsageCount(0);
                    Topic savedTopic = topicDao.save(newTopic);
                    System.out.println("DEBUG: New topic saved with ID: " + savedTopic.getId());
                    return savedTopic;
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
    public List<Topic> getTopTopics(int limit) {
        return topicDao.findTopTopics(limit);
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
        
        // Ensure we don't return empty string
        if (cleanName.isEmpty()) {
            cleanName = "general";
        }
        
        return cleanName;
    }
}
