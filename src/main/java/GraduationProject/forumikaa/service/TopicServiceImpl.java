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
     * Helper method để làm sạch tên hashtag - normalize Vietnamese text
     */
    private String cleanHashtagName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        String cleanName = name.trim();
        if (cleanName.startsWith("#")) {
            cleanName = cleanName.substring(1);
        }
        
        // Normalize Vietnamese text - remove accents and convert to lowercase
        cleanName = normalizeVietnameseText(cleanName);
        
        // Replace spaces and special characters with underscores
        cleanName = cleanName.replaceAll("[^a-z0-9]", "_");
        
        // Remove multiple consecutive underscores
        cleanName = cleanName.replaceAll("_+", "_");
        
        // Remove leading/trailing underscores
        cleanName = cleanName.replaceAll("^_+|_+$", "");
        
        // Ensure we don't return empty string
        if (cleanName.isEmpty()) {
            cleanName = "general";
        }
        
        return cleanName;
    }
    
    /**
     * Normalize Vietnamese text - remove accents and convert to lowercase
     */
    private String normalizeVietnameseText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // Convert to lowercase
        String normalized = text.trim().toLowerCase();
        
        // Remove Vietnamese accents
        String[][] accents = {
            {"à", "á", "ạ", "ả", "ã", "â", "ầ", "ấ", "ậ", "ẩ", "ẫ", "ă", "ằ", "ắ", "ặ", "ẳ", "ẵ"},
            {"a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a"},
            {"è", "é", "ẹ", "ẻ", "ẽ", "ê", "ề", "ế", "ệ", "ể", "ễ"},
            {"e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e"},
            {"ì", "í", "ị", "ỉ", "ĩ"},
            {"i", "i", "i", "i", "i"},
            {"ò", "ó", "ọ", "ỏ", "õ", "ô", "ồ", "ố", "ộ", "ổ", "ỗ", "ơ", "ờ", "ớ", "ợ", "ở", "ỡ"},
            {"o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o"},
            {"ù", "ú", "ụ", "ủ", "ũ", "ư", "ừ", "ứ", "ự", "ử", "ữ"},
            {"u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u"},
            {"ỳ", "ý", "ỵ", "ỷ", "ỹ"},
            {"y", "y", "y", "y", "y"},
            {"đ"},
            {"d"}
        };
        
        // Replace accents
        for (int i = 0; i < accents.length; i += 2) {
            String[] from = accents[i];
            String[] to = accents[i + 1];
            for (int j = 0; j < from.length; j++) {
                normalized = normalized.replace(from[j], to[j]);
            }
        }
        
        return normalized;
    }
}
