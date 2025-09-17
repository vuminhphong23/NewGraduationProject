package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.FriendshipDao;
import GraduationProject.forumikaa.dao.NotificationDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Friendship;
import GraduationProject.forumikaa.entity.FriendshipStatus;
import GraduationProject.forumikaa.entity.Notification;
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
    @Autowired private NotificationDao notificationDao;

    @Override
    public void sendFriendRequest(Long requesterId, Long targetUserId) {
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
        friendshipDao.save(friendship);

        // Tạo thông báo cho người nhận
        notificationService.createFriendshipRequestNotification(target.getId(), requester.getId());

    }

    @Override
    public void acceptFriendRequest(Long currentUserId, Long requesterId) {
        Friendship friendship = friendshipDao.findByUserIdAndFriendId(requesterId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu kết bạn không tồn tại"));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Không thể chấp nhận với trạng thái: " + friendship.getStatus());
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipDao.save(friendship);

        // Cập nhật notification gốc (FRIENDSHIP_REQUEST) thành FRIENDSHIP_ACCEPTED
        updateOriginalNotification(requesterId, currentUserId, 
            Notification.NotificationType.FRIENDSHIP_REQUEST, 
            Notification.NotificationType.FRIENDSHIP_ACCEPTED,
            "đã chấp nhận lời mời kết bạn");

        // Tạo thông báo cho người gửi yêu cầu
        notificationService.createFriendshipAcceptedNotification(friendship.getUser().getId(), currentUserId);
    }

    @Override
    public void declineFriendRequest(Long currentUserId, Long requesterId) {
        Friendship friendship = friendshipDao.findByUserIdAndFriendId(requesterId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu kết bạn không tồn tại"));
        friendshipDao.delete(friendship);

        // Cập nhật notification gốc (FRIENDSHIP_REQUEST) thành FRIENDSHIP_REJECTED
        updateOriginalNotification(requesterId, currentUserId, 
            Notification.NotificationType.FRIENDSHIP_REQUEST, 
            Notification.NotificationType.FRIENDSHIP_REJECTED,
            "đã từ chối lời mời kết bạn");

        // Tạo thông báo cho người gửi yêu cầu
        notificationService.createFriendshipRejectedNotification(friendship.getUser().getId(), currentUserId);
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
        
        // Tạo thông báo cho người bị hủy kết bạn
        notificationService.createFriendshipCancelledNotification(friendUserId, currentUserId);
    }

    @Override
    public Optional<Friendship> getFriendshipBetween(Long userId, Long otherUserId) {
        return friendshipDao.findBetweenUsers(userId, otherUserId);
    }

    @Override
    public List<User> listFriends(Long userId) {
        // Load luôn userProfile để có avatar, tránh LazyInitialization
        return friendshipDao.findFriendsOfWithProfile(userId);
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

    // Helper method để cập nhật notification gốc
    private void updateOriginalNotification(Long senderId, Long recipientId, 
                                         Notification.NotificationType originalType, 
                                         Notification.NotificationType newType, 
                                         String newMessage) {
        try {
            // Tìm notification gốc (FRIENDSHIP_REQUEST) từ senderId đến recipientId
            Optional<Notification> originalNotification = notificationDao
                .findBySenderIdAndRecipientIdAndType(senderId, recipientId, originalType);
            
            if (originalNotification.isPresent()) {
                Notification notification = originalNotification.get();
                // Lấy tên người gửi để tạo message mới
                Optional<User> sender = userDao.findById(senderId);
                String senderName = sender.map(User::getUsername).orElse("Người dùng");
                String updatedMessage = senderName + " " + newMessage;
                
                // Cập nhật notification
                notificationService.updateNotificationType(
                    notification.getId(), 
                    newType, 
                    updatedMessage
                );
            }
        } catch (Exception e) {
            // Log lỗi nhưng không throw để không ảnh hưởng đến flow chính
            System.err.println("Lỗi khi cập nhật notification gốc: " + e.getMessage());
        }
    }
}


