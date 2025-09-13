package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.GroupDao;
import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.DocumentDTO;
import GraduationProject.forumikaa.dto.LinkDTO;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.GroupMemberRole;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupDao groupDao;
    
    @Autowired
    private GroupMemberDao groupMemberDao;
    
    @Autowired
    private UserDao userDao;

    @Override
    @Transactional
    public UserGroup save(UserGroup group) {
        return groupDao.save(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserGroup> findById(Long id) {
        return groupDao.findById(id);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        groupDao.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGroup> findAll() {
        return groupDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserGroup> findPaginated(String keyword, String status, String privacy, Pageable pageable) {
        return groupDao.findPaginated(keyword, status, privacy, pageable);
    }

    @Override
    @Transactional
    public UserGroup createGroup(String name, String description, Long createdById) {
        User creator = userDao.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserGroup group = new UserGroup();
        group.setName(name);
        group.setDescription(description);
        group.setCreatedBy(creator);
        
        UserGroup savedGroup = groupDao.save(group);
        
        // Note: Creator is not automatically added as member
        // Admin can manually add members later
        
        return savedGroup;
    }

    @Override
    @Transactional
    public UserGroup updateGroup(Long groupId, String name, String description, Long userId) {
        UserGroup group = groupDao.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        if (!canEditGroup(groupId, userId)) {
            throw new UnauthorizedException("You can only edit groups you created");
        }
        
        group.setName(name);
        group.setDescription(description);
        
        return groupDao.save(group);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        // Check if group exists
        groupDao.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        if (!canDeleteGroup(groupId, userId)) {
            throw new UnauthorizedException("You can only delete groups you created");
        }
        
        // Delete all group members first
        groupMemberDao.deleteByGroupId(groupId);
        
        // Delete the group
        groupDao.deleteById(groupId);
    }

    @Override
    @Transactional
    public void addMember(Long groupId, Long userId, String role) {
        UserGroup group = groupDao.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if user is already a member
        if (groupMemberDao.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }
        
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMemberRole.valueOf(role));
        
        groupMemberDao.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        if (!groupMemberDao.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ResourceNotFoundException("User is not a member of this group");
        }
        
        groupMemberDao.deleteByGroupIdAndUserId(groupId, userId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long groupId, Long userId, String role) {
        GroupMember member = groupMemberDao.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group"));
        
        member.setRole(GroupMemberRole.valueOf(role));
        groupMemberDao.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGroup> findByCreatedById(Long userId) {
        return groupDao.findByCreatedById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGroup> findByNameContaining(String name) {
        return groupDao.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByCreatedById(Long userId) {
        return groupDao.countByCreatedById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditGroup(Long groupId, Long userId) {
        return groupDao.findById(groupId)
                .map(group -> group.getCreatedBy().getId().equals(userId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDeleteGroup(Long groupId, Long userId) {
        return canEditGroup(groupId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGroupMember(Long groupId, Long userId) {
        return groupMemberDao.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGroupAdmin(Long groupId, Long userId) {
        return groupMemberDao.findByGroupIdAndUserId(groupId, userId)
                .map(member -> member.getRole() == GroupMemberRole.ADMIN)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getMemberCount(Long groupId) {
        return groupMemberDao.countByGroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberDao.findByGroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDTO> getGroupDocuments(Long groupId) {
        // For now, return empty list - can be implemented later with actual document storage
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkDTO> getGroupLinks(Long groupId) {
        // For now, return empty list - can be implemented later with group settings
        return List.of();
    }
}
