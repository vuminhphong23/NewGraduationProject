package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicDao extends JpaRepository<Topic, Long> {

} 