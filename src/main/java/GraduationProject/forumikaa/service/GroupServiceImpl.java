package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.DocumentDao;
import GraduationProject.forumikaa.dao.GroupDao;
import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.dao.PostDao;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.Document;
import GraduationProject.forumikaa.entity.UserGroup;
import GraduationProject.forumikaa.entity.GroupMember;
import GraduationProject.forumikaa.entity.GroupMemberRole;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.Post;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.exception.UnauthorizedException;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupDao groupDao;
    
    @Autowired
    private GroupMemberDao groupMemberDao;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private TopicDao topicDao;
    
    @Autowired
    private PostDao postDao;
    
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private SecurityUtil securityUtil;

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
        
        // Update member count in group entity
        Long newMemberCount = groupMemberDao.countByGroupId(groupId);
        group.setMemberCount(newMemberCount);
        groupDao.save(group);
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        if (!groupMemberDao.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ResourceNotFoundException("User is not a member of this group");
        }
        
        groupMemberDao.deleteByGroupIdAndUserId(groupId, userId);
        
        // Update member count in group entity
        UserGroup group = groupDao.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        Long newMemberCount = groupMemberDao.countByGroupId(groupId);
        group.setMemberCount(newMemberCount);
        groupDao.save(group);
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
        // First try to get from entity field
        Optional<UserGroup> groupOpt = groupDao.findById(groupId);
        if (groupOpt.isPresent() && groupOpt.get().getMemberCount() != null) {
            return groupOpt.get().getMemberCount();
        }
        
        // Fallback to counting from database
        return groupMemberDao.countByGroupId(groupId);
    }
    
    /**
     * Sync member count for all groups (useful after adding memberCount field)
     */
    @Transactional
    public void syncAllMemberCounts() {
        List<UserGroup> allGroups = groupDao.findAll();
        for (UserGroup group : allGroups) {
            Long actualCount = groupMemberDao.countByGroupId(group.getId());
            group.setMemberCount(actualCount);
            groupDao.save(group);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberDao.findByGroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileUploadResponse> getGroupDocuments(Long groupId) {
        Long userId = securityUtil.getCurrentUserId();

        List<Post> posts = postDao.findByGroupIdWithUserAccess(groupId, userId);
        
        
        // Extract documents from posts
        List<FileUploadResponse> documents = new ArrayList<>();
        for (Post post : posts) {
            if (post.getDocuments() != null && !post.getDocuments().isEmpty()) {
                for (Document file : post.getDocuments()) {
                    FileUploadResponse doc = new FileUploadResponse();
                    doc.setId(file.getId());
                    doc.setFileName(file.getFileName());
                    doc.setFileSize(file.getFileSize());
                    doc.setFileType(getFileType(file.getMimeType()));
                    doc.setUploadDate(file.getUploadedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    doc.setDownloadCount(file.getDownloadCount() != null ? file.getDownloadCount() : 0);
                    doc.setOriginalName(file.getOriginalName());
                    doc.setDownloadUrl(file.getFilePath());
                    documents.add(doc);
                }
            }
        }
        
        System.out.println("Total documents found: " + documents.size());
        return documents;
    }
    

    
    @Override
    @Transactional(readOnly = true)
    public List<Topic> getPopularTopicsInGroup(Long groupId, int limit) {
        // Lấy các topic phổ biến nhất trong group cụ thể
        return topicDao.findTopTopicsByGroup(groupId, limit);
    }
    
    private String getFileType(String mimeType) {
        if (mimeType == null) return "unknown";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("text/")) return "text";
        
        // Specific application types
        if (mimeType.equals("application/pdf")) return "pdf";
        if (mimeType.equals("application/msword") || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) return "doc";
        if (mimeType.equals("application/vnd.ms-excel") || mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) return "xls";
        if (mimeType.equals("application/vnd.ms-powerpoint") || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) return "ppt";
        
        // Generic document for other application types
        if (mimeType.startsWith("application/")) return "document";
        return "other";
    }
    
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
    
    @Override
    public Page<UserGroup> findGroupsForExplore(String keyword, String category, Pageable pageable) {
        if ("all".equals(category)) {
            return groupDao.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            return groupDao.findGroupsWithKeywordAndCategory(keyword, category, pageable);
        }
    }
    
    @Override
    public List<Long> getUserJoinedGroupIds(Long userId) {
        return groupMemberDao.findGroupIdsByUserId(userId);
    }
    
    @Override
    public List<Topic> getPopularTopics(int limit) {
        return topicDao.findTopTopicsByUsage(limit);
    }
    
    @Override
    public Long getTotalGroupCount() {
        return groupDao.count();
    }
    
    @Override
    public Long getTotalMemberCount() {
        return groupMemberDao.count();
    }
    
    
}
