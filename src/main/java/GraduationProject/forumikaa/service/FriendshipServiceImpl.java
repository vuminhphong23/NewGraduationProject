package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.FriendshipDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.FriendshipStatus;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Transactional
public class FriendshipServiceImpl implements FriendshipService {

    @Autowired private FriendshipDao friendshipDao;
    @Autowired private UserDao userDao;
    @Autowired private NotificationService notificationService;

    @Override
    public Friendship sendFriendRequest(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new IllegalArgumentException("Không thể kết bạn với chính mình");
        }

        User requester = userDao.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User target = userDao.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        Optional<Friendship> existing = friendshipDao.findBetweenUsers(requesterId, targetUserId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            // Nếu trước đó bị BLOCKED hoặc ACCEPTED/PENDING, không tạo mới
            throw new IllegalStateException("Trạng thái kết bạn hiện tại: " + f.getStatus());
        }

        Friendship friendship = new Friendship();
        friendship.setUser(requester);
        friendship.setFriend(target);
        friendship.setStatus(FriendshipStatus.PENDING);
        Friendship saved = friendshipDao.save(friendship);

        // Tạo thông báo cho người nhận
        notificationService.createNotification(
            target, requester,
            requester.getUsername() + " đã gửi yêu cầu kết bạn",
            "/profile/" + requester.getUsername()
        );

        return saved;
    }

    @Override
    public Friendship acceptFriendRequest(Long currentUserId, Long requesterId) {
        Friendship friendship = friendshipDao.findByUserIdAndFriendId(requesterId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu kết bạn không tồn tại"));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Không thể chấp nhận với trạng thái: " + friendship.getStatus());
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        Friendship saved = friendshipDao.save(friendship);

        // Tạo thông báo cho người gửi yêu cầu
        User currentUser = userDao.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        notificationService.createNotification(
            friendship.getUser(), currentUser,
            currentUser.getUsername() + " đã chấp nhận yêu cầu kết bạn",
            "/profile/" + currentUser.getUsername()
        );

        return saved;
    }

    @Override
    public void declineFriendRequest(Long currentUserId, Long requesterId) {
        Friendship friendship = friendshipDao.findByUserIdAndFriendId(requesterId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu kết bạn không tồn tại"));
        friendshipDao.delete(friendship);

        // Tạo thông báo cho người gửi yêu cầu
        User currentUser = userDao.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        notificationService.createNotification(
            friendship.getUser(), currentUser,
            currentUser.getUsername() + " đã từ chối yêu cầu kết bạn",
            "/profile/" + currentUser.getUsername()
        );
    }

    @Override
    public void cancelFriendRequest(Long currentUserId, Long targetUserId) {
        Friendship friendship = friendshipDao.findByUserIdAndFriendId(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu kết bạn không tồn tại"));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Không thể hủy ở trạng thái: " + friendship.getStatus());
        }
        friendshipDao.delete(friendship);
    }

    @Override
    public void unfriend(Long currentUserId, Long friendUserId) {
        Friendship friendship = friendshipDao.findBetweenUsers(currentUserId, friendUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Quan hệ bạn bè không tồn tại"));
        friendshipDao.delete(friendship);
    }

    @Override
    public Optional<Friendship> getFriendshipBetween(Long userId, Long otherUserId) {
        return friendshipDao.findBetweenUsers(userId, otherUserId);
    }

    @Override
    public List<User> listFriends(Long userId) {
        return friendshipDao.findFriendsOf(userId);
    }

    @Override
    public List<Map<String, Object>> listPendingRequests(Long userId) {
        List<Friendship> incoming = friendshipDao.findByFriendIdAndStatus(userId, FriendshipStatus.PENDING);
        return incoming.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("requestId", f.getId());
            m.put("fromUserId", f.getUser().getId());
            m.put("fromUsername", f.getUser().getUsername());
            m.put("createdAt", f.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }
}


