package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.GroupMemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberDao extends JpaRepository<GroupMember, Long> {

    // Find members by group ID
    @Query("""
        SELECT gm FROM GroupMember gm
        LEFT JOIN FETCH gm.user u
        LEFT JOIN FETCH u.userProfile
        WHERE gm.group.id = :groupId
        ORDER BY gm.joinedAt ASC
    """)
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);

    // Find groups by user ID
    @Query("""
        SELECT gm FROM GroupMember gm
        LEFT JOIN FETCH gm.group g
        LEFT JOIN FETCH g.createdBy u
        WHERE gm.user.id = :userId
        ORDER BY gm.joinedAt DESC
    """)
    List<GroupMember> findByUserId(@Param("userId") Long userId);

    // Find specific member
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    // Check if user is member
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    // Delete by group and user
    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    // Delete all members of a group
    void deleteByGroupId(Long groupId);

    // Count members by group
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
    Long countByGroupId(@Param("groupId") Long groupId);

    // Find members by role
    @Query("""
        SELECT gm FROM GroupMember gm
        LEFT JOIN FETCH gm.user u
        LEFT JOIN FETCH u.userProfile
        WHERE gm.group.id = :groupId AND gm.role = :role
        ORDER BY gm.joinedAt ASC
    """)
    List<GroupMember> findByGroupIdAndRole(@Param("groupId") Long groupId, @Param("role") String role);

    // Find admins of a group
    @Query("""
        SELECT gm FROM GroupMember gm
        LEFT JOIN FETCH gm.user u
        LEFT JOIN FETCH u.userProfile
        WHERE gm.group.id = :groupId AND gm.role = 'ADMIN'
        ORDER BY gm.joinedAt ASC
    """)
    List<GroupMember> findAdminsByGroupId(@Param("groupId") Long groupId);
    
    // Single optimized query for all member filtering needs
    @Query("""
        SELECT gm FROM GroupMember gm
        JOIN FETCH gm.user u
        LEFT JOIN FETCH u.userProfile up
        WHERE gm.group.id = :groupId
        AND (:search IS NULL OR :search = '' OR 
             LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:role IS NULL OR :role = '' OR gm.role = :roleEnum)
    """)
    List<GroupMember> findMembersWithFilters(@Param("groupId") Long groupId,
                                           @Param("search") String search,
                                           @Param("role") String role,
                                           @Param("roleEnum") GroupMemberRole roleEnum);
    
    // Count total posts by user in a group
    @Query("""
        SELECT COUNT(p) FROM Post p
        WHERE p.user.id = :userId AND p.group.id = :groupId
    """)
    Long countPostsByUserInGroup(@Param("userId") Long userId, 
                                @Param("groupId") Long groupId);
    
    // Get user's joined group IDs
    @Query("SELECT gm.group.id FROM GroupMember gm WHERE gm.user.id = :userId")
    List<Long> findGroupIdsByUserId(@Param("userId") Long userId);
}
