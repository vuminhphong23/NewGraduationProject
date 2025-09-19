package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.FriendDto;
import GraduationProject.forumikaa.dto.GroupMemberDto;
import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
public interface ProfileDataService {
    
    /**
     * Lấy tất cả dữ liệu cần thiết cho profile page
     */
    ProfileDataResult getProfileData(User user);
    
    /**
     * Lấy danh sách bạn bè của user
     */
    List<FriendDto> getFriendsList(Long userId);
    
    /**
     * Lấy danh sách nhóm của user
     */
    List<GroupMemberDto> getUserGroups(Long userId);
    
    /**
     * Lấy tất cả tài liệu của user từ posts
     */
    List<FileUploadResponse> getUserDocuments(List<PostResponse> userPosts);
    
    /**
     * Kết quả trả về cho profile data
     */
    class ProfileDataResult {
        private List<PostResponse> userPosts;
        private List<FriendDto> friends;
        private List<GroupMemberDto> userGroups;
        private List<FileUploadResponse> userDocuments;
        private int postCount;
        private int friendsCount;
        private int groupsCount;
        private int documentsCount;
        
        // Constructors
        public ProfileDataResult() {}
        
        public ProfileDataResult(List<PostResponse> userPosts, List<FriendDto> friends, 
                               List<GroupMemberDto> userGroups, List<FileUploadResponse> userDocuments) {
            this.userPosts = userPosts;
            this.friends = friends;
            this.userGroups = userGroups;
            this.userDocuments = userDocuments;
            this.postCount = userPosts.size();
            this.friendsCount = friends.size();
            this.groupsCount = userGroups.size();
            this.documentsCount = userDocuments.size();
        }
        
        // Getters and Setters
        public List<PostResponse> getUserPosts() { return userPosts; }
        public void setUserPosts(List<PostResponse> userPosts) { this.userPosts = userPosts; }
        
        public List<FriendDto> getFriends() { return friends; }
        public void setFriends(List<FriendDto> friends) { this.friends = friends; }
        
        public List<GroupMemberDto> getUserGroups() { return userGroups; }
        public void setUserGroups(List<GroupMemberDto> userGroups) { this.userGroups = userGroups; }
        
        public List<FileUploadResponse> getUserDocuments() { return userDocuments; }
        public void setUserDocuments(List<FileUploadResponse> userDocuments) { this.userDocuments = userDocuments; }
        
        public int getPostCount() { return postCount; }
        public void setPostCount(int postCount) { this.postCount = postCount; }
        
        public int getFriendsCount() { return friendsCount; }
        public void setFriendsCount(int friendsCount) { this.friendsCount = friendsCount; }
        
        public int getGroupsCount() { return groupsCount; }
        public void setGroupsCount(int groupsCount) { this.groupsCount = groupsCount; }
        
        public int getDocumentsCount() { return documentsCount; }
        public void setDocumentsCount(int documentsCount) { this.documentsCount = documentsCount; }
    }
}
