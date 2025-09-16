package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.PostDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.GroupDao;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.PostStatus;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SearchServiceImpl implements SearchService {

    @Autowired
    private PostDao postDao;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private GroupDao groupDao;
    
    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public List<Post> searchPosts(String query, Pageable pageable) {
        try {
            Specification<Post> spec = (root, criteriaQuery, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Search in title and content
                String likePattern = "%" + query.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likePattern)
                ));
                
                // Only show approved posts
                predicates.add(criteriaBuilder.equal(root.get("status"), PostStatus.APPROVED));
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            return postDao.findAll(spec, pageable).getContent();
        } catch (Exception e) {
            System.err.println("Error searching posts: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<User> searchUsers(String query, Pageable pageable) {
        try {
            Specification<User> spec = (root, criteriaQuery, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Search in username, first name, last name, and email
                String likePattern = "%" + query.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern)
                ));
                
                // Only show enabled users
                predicates.add(criteriaBuilder.isTrue(root.get("enabled")));
                
                // Exclude current user (only if user is logged in)
                try {
                    Long currentUserId = securityUtil.getCurrentUserId();
                    if (currentUserId != null) {
                        predicates.add(criteriaBuilder.notEqual(root.get("id"), currentUserId));
                    }
                } catch (Exception e) {
                    System.out.println("Could not get current user ID: " + e.getMessage());
                    // Continue without excluding current user
                }
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            return userDao.findAll(spec, pageable).getContent();
        } catch (Exception e) {
            System.err.println("Error searching users: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<UserGroup> searchGroups(String query, Pageable pageable) {
        try {
            Specification<UserGroup> spec = (root, criteriaQuery, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Search in name and description
                String likePattern = "%" + query.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
                ));
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            return groupDao.findAll(spec, pageable).getContent();
        } catch (Exception e) {
            System.err.println("Error searching groups: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
