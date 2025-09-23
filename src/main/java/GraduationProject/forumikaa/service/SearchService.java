package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Group;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    List<Post> searchPosts(String query, Pageable pageable);
    List<User> searchUsers(String query, Pageable pageable);
    List<Group> searchGroups(String query, Pageable pageable);
}
