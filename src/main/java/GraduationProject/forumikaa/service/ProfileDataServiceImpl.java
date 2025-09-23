package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.GroupMemberDao;
import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.FriendDto;
import GraduationProject.forumikaa.dto.GroupMemberDto;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.entity.GroupMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileDataServiceImpl implements ProfileDataService {

    @Autowired private PostService postService;
    @Autowired private FriendshipService friendshipService;
    @Autowired private GroupMemberDao groupMemberDao;

    @Override
    public ProfileDataResult getProfileData(User user) {
        // Lấy các bài viết của người dùng
        List<PostResponse> userPosts = postService.getUserPosts(user.getId());
        
        // Lấy danh sách bạn bè
        List<FriendDto> friends = getFriendsList(user.getId());
        
        // Lấy danh sách nhóm
        List<GroupMemberDto> userGroups = getUserGroups(user.getId());
        
        // Lấy tất cả tài liệu của user từ posts
        List<FileUploadResponse> userDocuments = getUserDocuments(userPosts);
        
        return new ProfileDataResult(userPosts, friends, userGroups, userDocuments);
    }

    @Override
    public List<FriendDto> getFriendsList(Long userId) {
        List<User> friendsList = friendshipService.listFriends(userId);
        return friendsList.stream()
                .map(friend -> FriendDto.builder()
                        .id(friend.getId())
                        .username(friend.getUsername())
                        .firstName(friend.getUsername())
                        .lastName(friend.getLastName())
                        .email(friend.getEmail())
                        .avatar(friend.getUserProfile() != null && friend.getUserProfile().getAvatar() != null ?
                                friend.getUserProfile().getAvatar() :
                                "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_640.png")
                        .profileLink(friend.getUserProfile() != null ? friend.getUserProfile().getSocialLinks() : null)
                        .isOnline(true) // Có thể thêm logic kiểm tra online status
                        .friendDate("01/01/2025") // Sử dụng createdAt thay vì hardcode
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupMemberDto> getUserGroups(Long userId) {
        List<GroupMember> groupMembers = groupMemberDao.findByUserId(userId);
        return groupMembers.stream()
                .map(member -> GroupMemberDto.builder()
                        .id(member.getGroup().getId())
                        .userId(member.getUser().getId())
                        .username(member.getGroup().getName())
                        .firstName(member.getGroup().getName())
                        .lastName("")
                        .fullName(member.getGroup().getName())
                        .avatar(member.getGroup().getAvatar() != null ? 
                                member.getGroup().getAvatar() : 
                                "https://ui-avatars.com/api/?name=" + member.getGroup().getName() + "&background=007bff&color=ffffff&size=60")
                        .role(member.getRole().name()) // Convert enum to string
                        .isOnline(false) // Groups không có online status
                        .joinedAt(member.getJoinedAt())
                        .memberCount(postService.getNewPostCountByGroupToday(member.getGroup().getId()))
                        .postCount(0L)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadResponse> getUserDocuments(List<PostResponse> userPosts) {
        return userPosts.stream()
                .filter(post -> post.getDocuments() != null && !post.getDocuments().isEmpty())
                .flatMap(post -> post.getDocuments().stream()
                    .filter(doc -> doc.getFileName() != null)
                    .map(doc -> {
                        // Refine fileType based on file extension for better categorization
                        String fileName = doc.getFileName().toLowerCase();
                        String currentFileType = doc.getFileType();
                        
                        // Only refine if current fileType is generic "document"
                        if ("document".equals(currentFileType)) {
                            if (fileName.endsWith(".pdf")) {
                                doc.setFileType("pdf");
                            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                                doc.setFileType("doc");
                            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                                doc.setFileType("xls");
                            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                                doc.setFileType("ppt");
                            } else if (fileName.endsWith(".txt")) {
                                doc.setFileType("text");
                            }
                            // Keep "document" for other application/* types
                        }
                        // Keep existing fileType for "image", "video", "other"
                        
                        return doc;
                    }))
                .collect(Collectors.toList());
    }
}


