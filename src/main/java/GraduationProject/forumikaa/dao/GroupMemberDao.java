package GraduationProject.forumikaa.dao;

import GraduationProject.forumikaa.entity.GroupMember;
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
    
    // Get user's joined group IDs
    @Query("SELECT gm.group.id FROM GroupMember gm WHERE gm.user.id = :userId")
    List<Long> findGroupIdsByUserId(@Param("userId") Long userId);
}
