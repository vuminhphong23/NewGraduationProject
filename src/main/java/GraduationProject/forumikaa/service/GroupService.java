package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.GroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GroupService {
    
    // Basic CRUD operations
    UserGroup save(UserGroup group);
    Optional<UserGroup> findById(Long id);
    void deleteById(Long id);
    List<UserGroup> findAll();
    
    // Admin management methods
    Page<UserGroup> findPaginated(String keyword, String status, String privacy, Pageable pageable);
    
    // Group management
    UserGroup createGroup(String name, String description, Long createdById);
    UserGroup updateGroup(Long groupId, String name, String description, Long userId);
    void deleteGroup(Long groupId, Long userId);
    
    // Member management
    void addMember(Long groupId, Long userId, String role);
    void removeMember(Long groupId, Long userId);
    void updateMemberRole(Long groupId, Long userId, String role);
    
    // Query methods
    List<UserGroup> findByCreatedById(Long userId);
    List<UserGroup> findByNameContaining(String name);
    Long countByCreatedById(Long userId);
    
    // Member count
    Long getMemberCount(Long groupId);
    
    // Get group members
    List<GroupMember> getGroupMembers(Long groupId);
    
    // Permission checks
    boolean canEditGroup(Long groupId, Long userId);
    boolean canDeleteGroup(Long groupId, Long userId);
    boolean isGroupMember(Long groupId, Long userId);
    boolean isGroupAdmin(Long groupId, Long userId);
}
