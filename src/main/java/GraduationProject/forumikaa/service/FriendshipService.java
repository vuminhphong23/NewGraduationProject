package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FriendshipService {
    void sendFriendRequest(Long requesterId, Long targetUserId);
    void acceptFriendRequest(Long currentUserId, Long requesterId);
    void declineFriendRequest(Long currentUserId, Long requesterId);
    void cancelFriendRequest(Long currentUserId, Long targetUserId);
    void unfriend(Long currentUserId, Long friendUserId);
    Optional<Friendship> getFriendshipBetween(Long userId, Long otherUserId);
    List<User> listFriends(Long userId);
    List<Map<String, Object>> listPendingRequests(Long userId);
}



