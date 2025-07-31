package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Topic;

import java.util.List;

public interface TopicService {
    
    List<Topic> getAllTopics();
    
    Topic getTopicById(Long id);
    
    Topic createTopic(Topic topic);
    
    Topic updateTopic(Topic topic);
    
    void deleteTopic(Long id);
} 