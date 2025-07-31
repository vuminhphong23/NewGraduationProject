package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TopicServiceImpl implements TopicService {

    private TopicDao topicDao;

    @Autowired
    public void setTopicDao(TopicDao topicDao) {
        this.topicDao = topicDao;
    }

    @Override
    public List<Topic> getAllTopics() {
        return topicDao.findAll();
    }

    @Override
    public Topic getTopicById(Long id) {
        return topicDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
    }

    @Override
    public Topic createTopic(Topic topic) {
        return topicDao.save(topic);
    }

    @Override
    public Topic updateTopic(Topic topic) {
        if (!topicDao.existsById(topic.getId())) {
            throw new ResourceNotFoundException("Topic not found with id: " + topic.getId());
        }
        return topicDao.save(topic);
    }

    @Override
    public void deleteTopic(Long id) {
        if (!topicDao.existsById(id)) {
            throw new ResourceNotFoundException("Topic not found with id: " + id);
        }
        topicDao.deleteById(id);
    }
} 