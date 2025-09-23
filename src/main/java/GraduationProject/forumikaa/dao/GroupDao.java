package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupDao extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {

    // Admin pagination with filters
    @Query("""
        SELECT DISTINCT g FROM Group g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY g.createdAt DESC
    """)
    Page<Group> findPaginated(@Param("keyword") String keyword,
                              @Param("status") String status,
                              @Param("privacy") String privacy,
                              Pageable pageable);
    
    // Explore groups methods
    @Query("""
        SELECT DISTINCT g FROM Group g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY g.createdAt DESC
    """)
    Page<Group> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        @Param("keyword") String keyword1, 
        @Param("keyword") String keyword2, 
        Pageable pageable);
    
    @Query("""
        SELECT DISTINCT g FROM Group g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        LEFT JOIN g.topics t
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:category IS NULL OR :category = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :category, '%')))
        ORDER BY g.createdAt DESC
    """)
    Page<Group> findGroupsWithKeywordAndCategory(
        @Param("keyword") String keyword, 
        @Param("category") String category, 
        Pageable pageable);

    Long countById(Long groupId);
}
