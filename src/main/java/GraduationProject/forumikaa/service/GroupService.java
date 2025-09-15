package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GroupService {
    
    // Basic CRUD operations
    UserGroup save(UserGroup group);
    Optional<UserGroup> findById(Long id);
    void deleteById(Long id);
    List<UserGroup> findAll();
    
    // Admin management methods
    Page<UserGroup> findPaginated(String keyword, String status, String privacy, Pageable pageable);
    
    // Member management
    void addMember(Long groupId, Long userId, String role);
    void removeMember(Long groupId, Long userId);
    void updateMemberRole(Long groupId, Long userId, String role);

    
    // Member count
    Long getMemberCount(Long groupId);
    
    // Get group members
    List<GroupMember> getGroupMembers(Long groupId);
    
    // Permission checks
    boolean canEditGroup(Long groupId, Long userId);
    boolean canDeleteGroup(Long groupId, Long userId);
    boolean isGroupMember(Long groupId, Long userId);
    boolean isGroupAdmin(Long groupId, Long userId);
    
    // Document management
    List<FileUploadResponse> getGroupDocuments(Long groupId);
    
    
    // Topic management
    List<Topic> getPopularTopicsInGroup(Long groupId, int limit);
    
    // Explore groups methods
    Page<UserGroup> findGroupsForExplore(String keyword, String category, Pageable pageable);
    List<Long> getUserJoinedGroupIds(Long userId);
    List<Topic> getPopularTopics(int limit);
    Long getTotalGroupCount();
    Long getTotalMemberCount();
    
}
