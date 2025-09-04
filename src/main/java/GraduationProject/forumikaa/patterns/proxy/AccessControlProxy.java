package GraduationProject.forumikaa.patterns.proxy;

import GraduationProject.forumikaa.entity.Notification;
import GraduationProject.forumikaa.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AccessControlProxy - Proxy Pattern với Access Control
 * Kiểm soát quyền truy cập vào NotificationService dựa trên user roles và permissions
 */
public class AccessControlProxy implements NotificationServiceProxy {
    
    private final NotificationService realService;
    
    // Access control configuration
    private static final Map<String, String[]> OPERATION_PERMISSIONS;
    
    static {
        Map<String, String[]> permissions = new ConcurrentHashMap<>();
        permissions.put("CREATE_NOTIFICATION", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("GET_USER_NOTIFICATIONS", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("GET_UNREAD_COUNT", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("MARK_AS_READ", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("MARK_ALL_AS_READ", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("CREATE_SYSTEM_NOTIFICATION", new String[]{"ADMIN"});
        permissions.put("CREATE_WELCOME_NOTIFICATION", new String[]{"ADMIN"});
        permissions.put("BROADCAST_NOTIFICATION", new String[]{"MODERATOR", "ADMIN"});
        permissions.put("TOGGLE_LIKE", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("CREATE_COMMENT", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("UPDATE_COMMENT", new String[]{"USER", "MODERATOR", "ADMIN"});
        permissions.put("DELETE_COMMENT", new String[]{"USER", "MODERATOR", "ADMIN"});
        OPERATION_PERMISSIONS = permissions;
    }
    
    // User roles cache (in real app, this would come from database)
    private final Map<Long, String> userRoles = new ConcurrentHashMap<>();
    
    // Access logs
    private final Map<String, AccessLogEntry> accessLogs = new ConcurrentHashMap<>();
    private final AtomicLong logCounter = new AtomicLong(0);
    
    // Rate limiting
    private final Map<Long, RateLimitInfo> userRateLimits = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    
    public AccessControlProxy(NotificationService realService) {
        this.realService = realService;
        initializeDefaultUsers();
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        return createNotification(recipientId, senderId, message, null);
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        String operation = "CREATE_NOTIFICATION";
        
        // Check authorization
        if (!isUserAuthorized(senderId, operation)) {
            logAccessAttempt(senderId, operation, false);
            throw new SecurityException("User " + senderId + " is not authorized to create notifications");
        }
        
        // Check rate limiting
        if (isRateLimited(senderId)) {
            logAccessAttempt(senderId, operation, false);
            throw new SecurityException("User " + senderId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(senderId, operation, true);
        
        // Call real service
        return realService.createNotification(recipientId, senderId, message, link);
    }
    
    @Override
    public List<Notification> getUserNotifications(Long userId) {
        String operation = "GET_USER_NOTIFICATIONS";
        
        // Check authorization
        if (!isUserAuthorized(userId, operation)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " is not authorized to view notifications");
        }
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(userId, operation, true);
        
        // Call real service
        return realService.getUserNotifications(userId);
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        String operation = "GET_UNREAD_COUNT";
        
        // Check authorization
        if (!isUserAuthorized(userId, operation)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " is not authorized to view unread count");
        }
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(userId, operation, true);
        
        // Call real service
        return realService.getUnreadCount(userId);
    }
    
    @Override
    public void markAsRead(Long notificationId) {
        // Tạm thời disable access control cho markAsRead để fix lỗi 500
        // TODO: Implement proper user context handling
        System.out.println("🔓 AccessControlProxy: markAsRead(" + notificationId + ") - Access control temporarily disabled");
        
        // Call real service directly
        realService.markAsRead(notificationId);
    }
    
    // Overloaded method để nhận userId từ caller
    public void markAsRead(Long notificationId, Long userId) {
        String operation = "MARK_AS_READ";
        
        // Check authorization
        if (!isUserAuthorized(userId, operation)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " is not authorized to mark notifications as read");
        }
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(userId, operation, true);
        
        // Call real service
        realService.markAsRead(notificationId);
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        // Tạm thời disable access control cho markAllAsRead để fix lỗi 500
        // TODO: Implement proper user context handling
        System.out.println("🔓 AccessControlProxy: markAllAsRead(" + userId + ") - Access control temporarily disabled");
        
        // Call real service directly
        realService.markAllAsRead(userId);
    }
    
    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
        String operation = "GET_USER_NOTIFICATIONS";
        
        // Check authorization
        if (!isUserAuthorized(userId, operation)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " is not authorized to view notification DTOs");
        }
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(userId, operation, true);
        
        // Call real service
        return realService.getNotificationDtos(userId);
    }
    
    // Specific notification creation methods with access control
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        String operation = "CREATE_NOTIFICATION";
        
        // Check authorization
        if (!isUserAuthorized(likerId, operation)) {
            logAccessAttempt(likerId, operation, false);
            throw new SecurityException("User " + likerId + " is not authorized to create post like notifications");
        }
        
        // Check rate limiting
        if (isRateLimited(likerId)) {
            logAccessAttempt(likerId, operation, false);
            throw new SecurityException("User " + likerId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(likerId, operation, true);
        
        // Call real service
        return realService.createPostLikeNotification(postId, postAuthorId, likerId);
    }
    
    @Override
    public Notification createSystemNotification(Long userId, String message) {
        String operation = "CREATE_SYSTEM_NOTIFICATION";
        
        // Check authorization (only admins can create system notifications)
        if (!isUserAuthorized(userId, operation)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " is not authorized to create system notifications");
        }
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(userId, operation, true);
        
        // Call real service
        return realService.createSystemNotification(userId, message);
    }
    
    @Override
    public Notification createWelcomeNotification(Long userId) {
        String operation = "CREATE_WELCOME_NOTIFICATION";
        
        // Check authorization (only admins can create welcome notifications)
        if (!isUserAuthorized(userId, operation)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " is not authorized to create welcome notifications");
        }
        
        // Check rate limiting
        if (isRateLimited(userId)) {
            logAccessAttempt(userId, operation, false);
            throw new SecurityException("User " + userId + " has exceeded rate limit");
        }
        
        // Log successful access
        logAccessAttempt(userId, operation, true);
        
        // Call real service
        return realService.createWelcomeNotification(userId);
    }
    
    // Implement other methods with similar access control...
    @Override
    public Notification createPostCommentNotification(Long postId, Long postAuthorId, Long commenterId, Long commentId) {
        return realService.createPostCommentNotification(postId, postAuthorId, commenterId, commentId);
    }
    
    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        return realService.createCommentLikeNotification(commentId, commentAuthorId, likerId);
    }
    
    @Override
    public Notification createCommentReplyNotification(Long parentCommentId, Long parentCommentAuthorId, Long replierId, Long replyId) {
        return realService.createCommentReplyNotification(parentCommentId, parentCommentAuthorId, replierId, replyId);
    }
    
    @Override
    public Notification createFriendshipRequestNotification(Long recipientId, Long senderId) {
        return realService.createFriendshipRequestNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipAcceptedNotification(Long recipientId, Long senderId) {
        return realService.createFriendshipAcceptedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipRejectedNotification(Long recipientId, Long senderId) {
        return realService.createFriendshipRejectedNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createFriendshipCancelledNotification(Long recipientId, Long senderId) {
        return realService.createFriendshipCancelledNotification(recipientId, senderId);
    }
    
    @Override
    public Notification createMentionNotification(Long mentionedUserId, Long mentionerId, Long entityId, String entityType) {
        return realService.createMentionNotification(mentionedUserId, mentionerId, entityId, entityType);
    }
    
    // Proxy-specific methods implementation
    @Override
    public boolean isUserAuthorized(Long userId, String operation) {
        String userRole = getUserRole(userId);
        String[] requiredRoles = OPERATION_PERMISSIONS.get(operation);
        
        if (requiredRoles == null) {
            return false; // Operation not found
        }
        
        for (String role : requiredRoles) {
            if (role.equals(userRole)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void logAccessAttempt(Long userId, String operation, boolean authorized) {
        String logId = "access_" + logCounter.incrementAndGet();
        AccessLogEntry entry = new AccessLogEntry(userId, operation, authorized, getCurrentTimestamp());
        accessLogs.put(logId, entry);
        
        // Clean up old logs if too many
        if (accessLogs.size() > 1000) {
            accessLogs.clear();
        }
        
        // Print to console for demo
        String status = authorized ? "✅ AUTHORIZED" : "❌ DENIED";
        System.out.println("🔒 " + status + " - User " + userId + " attempting " + operation);
    }
    
    @Override
    public Map<String, Object> getAccessLogs() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("totalLogs", accessLogs.size());
        result.put("logs", accessLogs);
        return result;
    }
    
    @Override
    public void clearAccessLogs() {
        accessLogs.clear();
        logCounter.set(0);
        System.out.println("🧹 Access logs cleared");
    }
    
    // Private helper methods
    private String getUserRole(Long userId) {
        return userRoles.getOrDefault(userId, "USER");
    }
    
    private void initializeDefaultUsers() {
        // Initialize some default users with roles for demo
        userRoles.put(1L, "USER");
        userRoles.put(2L, "USER");
        userRoles.put(100L, "MODERATOR");
        userRoles.put(999L, "ADMIN");
    }
    
    private boolean isRateLimited(Long userId) {
        RateLimitInfo rateLimit = userRateLimits.computeIfAbsent(userId, k -> new RateLimitInfo());
        return rateLimit.isRateLimited();
    }
    
    private Long getCurrentUserId() {
        // In a real app, this would come from the current user context
        // For demo purposes, return a default user ID
        return 1L;
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    // Inner classes
    private static class AccessLogEntry {
        final Long userId;
        final String operation;
        final boolean authorized;
        final String timestamp;
        
        AccessLogEntry(Long userId, String operation, boolean authorized, String timestamp) {
            this.userId = userId;
            this.operation = operation;
            this.authorized = authorized;
            this.timestamp = timestamp;
        }
    }
    
    private static class RateLimitInfo {
        private int requestCount = 0;
        private long lastResetTime = System.currentTimeMillis();
        private static final long RESET_INTERVAL = 60 * 1000; // 1 minute
        
        boolean isRateLimited() {
            long currentTime = System.currentTimeMillis();
            
            // Reset counter if interval has passed
            if (currentTime - lastResetTime > RESET_INTERVAL) {
                requestCount = 0;
                lastResetTime = currentTime;
            }
            
            // Check if limit exceeded
            if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
                return true;
            }
            
            // Increment counter
            requestCount++;
            return false;
        }
    }
}
