package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupDao extends JpaRepository<UserGroup, Long>, JpaSpecificationExecutor<UserGroup> {

    // Find groups by creator
    @Query("""
        SELECT DISTINCT g FROM UserGroup g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE g.createdBy.id = :userId
        ORDER BY g.createdAt DESC
    """)
    List<UserGroup> findByCreatedById(@Param("userId") Long userId);

    // Find groups by name containing
    @Query("""
        SELECT DISTINCT g FROM UserGroup g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY g.createdAt DESC
    """)
    List<UserGroup> findByNameContainingIgnoreCase(@Param("name") String name);

    // Admin pagination with filters
    @Query("""
        SELECT DISTINCT g FROM UserGroup g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
               LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY g.createdAt DESC
    """)
    Page<UserGroup> findPaginated(@Param("keyword") String keyword, 
                                 @Param("status") String status, 
                                 @Param("privacy") String privacy, 
                                 Pageable pageable);

    // Find groups by creator username
    @Query("""
        SELECT DISTINCT g FROM UserGroup g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE u.username = :username
        ORDER BY g.createdAt DESC
    """)
    List<UserGroup> findByCreatedByUsername(@Param("username") String username);

    // Count groups by creator
    @Query("SELECT COUNT(g) FROM UserGroup g WHERE g.createdBy.id = :userId")
    Long countByCreatedById(@Param("userId") Long userId);

    // Find groups created in date range
    @Query("""
        SELECT DISTINCT g FROM UserGroup g
        LEFT JOIN FETCH g.createdBy u
        LEFT JOIN FETCH u.userProfile
        WHERE g.createdAt BETWEEN :startDate AND :endDate
        ORDER BY g.createdAt DESC
    """)
    List<UserGroup> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                         @Param("endDate") java.time.LocalDateTime endDate);

    // Find groups with member count
    @Query("""
        SELECT g, COUNT(gm.id) as memberCount FROM UserGroup g
        LEFT JOIN g.createdBy u
        LEFT JOIN u.userProfile up
        LEFT JOIN GroupMember gm ON gm.group.id = g.id
        WHERE g.id = :groupId
        GROUP BY g.id
    """)
    Object[] findGroupWithMemberCount(@Param("groupId") Long groupId);

    default boolean canEdit(UserGroup group, Long userId) {
        return group != null && group.getCreatedBy().getId().equals(userId);
    }
}
