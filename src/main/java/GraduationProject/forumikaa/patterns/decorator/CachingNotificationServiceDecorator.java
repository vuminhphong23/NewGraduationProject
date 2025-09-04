package GraduationProject.forumikaa.patterns.decorator;

import GraduationProject.forumikaa.entity.Notification;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Caching Decorator - Thêm tính năng cache để tăng performance
 * Cache các notification thường xuyên được truy cập
 */
public class CachingNotificationServiceDecorator extends BaseNotificationServiceDecorator {
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long cacheExpirationMinutes;
    
    public CachingNotificationServiceDecorator(NotificationServiceComponent wrappedService) {
        this(wrappedService, 5); // Default: 5 phút
    }
    
    public CachingNotificationServiceDecorator(NotificationServiceComponent wrappedService, long cacheExpirationMinutes) {
        super(wrappedService);
        this.cacheExpirationMinutes = cacheExpirationMinutes;
        System.out.println("💾 Caching Decorator: Cache expiration set to " + cacheExpirationMinutes + " minutes");
    }
    
    @Override
    public List<Notification> getUserNotifications(Long userId) {
        String cacheKey = "notifications_" + userId;
        
        // Kiểm tra cache trước
        if (cache.containsKey(cacheKey)) {
            CacheEntry entry = cache.get(cacheKey);
            if (!isExpired(entry)) {
                System.out.println("🎯 Caching Decorator: Cache HIT for user " + userId + " - Returning from cache!");
                return (List<Notification>) entry.data;
            } else {
                System.out.println("⏰ Caching Decorator: Cache EXPIRED for user " + userId + " - Removing old cache");
                cache.remove(cacheKey);
            }
        }
        
        // Cache miss hoặc expired - lấy từ wrapped service
        System.out.println("💾 Caching Decorator: Cache MISS for user " + userId + " - Fetching from database");
        List<Notification> notifications = wrappedService.getUserNotifications(userId);
        
        // Lưu vào cache
        long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cacheExpirationMinutes);
        cache.put(cacheKey, new CacheEntry(notifications, expirationTime));
        System.out.println("💾 Caching Decorator: Cached notifications for user " + userId + " (expires in " + cacheExpirationMinutes + " minutes)");
        
        return notifications;
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        String cacheKey = "unread_count_" + userId;
        
        // Kiểm tra cache
        if (cache.containsKey(cacheKey)) {
            CacheEntry entry = cache.get(cacheKey);
            if (!isExpired(entry)) {
                System.out.println("🎯 Caching Decorator: Cache HIT for unread count of user " + userId);
                return (Long) entry.data;
            } else {
                cache.remove(cacheKey);
            }
        }
        
        // Cache miss - lấy từ wrapped service
        Long unreadCount = wrappedService.getUnreadCount(userId);
        
        // Cache với thời gian ngắn hơn (1 phút) vì unread count thay đổi thường xuyên
        long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        cache.put(cacheKey, new CacheEntry(unreadCount, expirationTime));
        
        return unreadCount;
    }
    
    @Override
    public List<Map<String, Object>> getNotificationDtos(Long userId) {
        String cacheKey = "notification_dtos_" + userId;
        
        // Kiểm tra cache
        if (cache.containsKey(cacheKey)) {
            CacheEntry entry = cache.get(cacheKey);
            if (!isExpired(entry)) {
                System.out.println("🎯 Caching Decorator: Cache HIT for notification DTOs of user " + userId);
                return (List<Map<String, Object>>) entry.data;
            } else {
                cache.remove(cacheKey);
            }
        }
        
        // Cache miss - lấy từ wrapped service
        List<Map<String, Object>> dtos = wrappedService.getNotificationDtos(userId);
        
        // Cache DTOs
        long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cacheExpirationMinutes);
        cache.put(cacheKey, new CacheEntry(dtos, expirationTime));
        
        return dtos;
    }
    
    @Override
    public void markAsRead(Long notificationId) {
        try {
            System.out.println("🔄 Caching Decorator: Starting markAsRead(" + notificationId + ")");
            
            // Gọi wrapped service trước
            System.out.println("🔄 Caching Decorator: Calling wrapped service markAsRead(" + notificationId + ")");
            wrappedService.markAsRead(notificationId);
            System.out.println("✅ Caching Decorator: Wrapped service markAsRead(" + notificationId + ") completed successfully");
            
            // Invalidate cache một cách an toàn
            System.out.println("🗑️ Caching Decorator: Invalidating cache after markAsRead(" + notificationId + ")");
            invalidateAllNotificationCache();
            System.out.println("✅ Caching Decorator: Cache invalidation completed for markAsRead(" + notificationId + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Caching Decorator: Error in markAsRead(" + notificationId + "): " + e.getMessage());
            e.printStackTrace(); // In stack trace để debug
            
            // Nếu có lỗi, vẫn cố gắng invalidate cache
            try {
                System.out.println("🔄 Caching Decorator: Attempting cache invalidation despite error");
                invalidateAllNotificationCache();
                System.out.println("✅ Caching Decorator: Cache invalidation completed despite error");
            } catch (Exception cacheError) {
                System.err.println("❌ Caching Decorator: Failed to invalidate cache: " + cacheError.getMessage());
                cacheError.printStackTrace();
            }
            throw e; // Re-throw để caller biết có lỗi
        }
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        try {
            // Gọi wrapped service trước
            wrappedService.markAllAsRead(userId);
            
            // Invalidate cache cho user cụ thể
            System.out.println("🗑️ Caching Decorator: Invalidating cache for user " + userId + " after markAllAsRead");
            invalidateUserCache(userId);
            
        } catch (Exception e) {
            System.err.println("❌ Caching Decorator: Error in markAllAsRead(" + userId + "): " + e.getMessage());
            // Nếu có lỗi, vẫn cố gắng invalidate cache
            try {
                invalidateUserCache(userId);
            } catch (Exception cacheError) {
                System.err.println("❌ Caching Decorator: Failed to invalidate cache: " + cacheError.getMessage());
            }
            throw e; // Re-throw để caller biết có lỗi
        }
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message, String link) {
        // Gọi wrapped service trước
        Notification result = wrappedService.createNotification(recipientId, senderId, message, link);
        
        // Invalidate cache cho recipient user
        System.out.println("🗑️ Caching Decorator: Invalidating cache for recipient " + recipientId + " after creating notification");
        invalidateUserCache(recipientId);
        
        return result;
    }
    
    @Override
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        // Gọi wrapped service trước
        Notification result = wrappedService.createNotification(recipientId, senderId, message);
        
        // Invalidate cache cho recipient user
        System.out.println("🗑️ Caching Decorator: Invalidating cache for recipient " + recipientId + " after creating notification");
        invalidateUserCache(recipientId);
        
        return result;
    }
    
    @Override
    public Notification createPostLikeNotification(Long postId, Long postAuthorId, Long likerId) {
        // Gọi wrapped service trước
        Notification result = wrappedService.createPostLikeNotification(postId, postAuthorId, likerId);
        
        // Invalidate cache cho post author
        System.out.println("🗑️ Caching Decorator: Invalidating cache for post author " + postAuthorId + " after creating post like notification");
        invalidateUserCache(postAuthorId);
        
        return result;
    }
    
    @Override
    public Notification createCommentLikeNotification(Long commentId, Long commentAuthorId, Long likerId) {
        // Gọi wrapped service trước
        Notification result = wrappedService.createCommentLikeNotification(commentId, commentAuthorId, likerId);
        
        // Invalidate cache cho comment author
        System.out.println("🗑️ Caching Decorator: Invalidating cache for comment author " + commentAuthorId + " after creating comment like notification");
        invalidateUserCache(commentAuthorId);
        
        return result;
    }
    
    // Method để invalidate cache khi có thay đổi
    public void invalidateUserCache(Long userId) {
        String[] keysToRemove = {
            "notifications_" + userId,
            "unread_count_" + userId,
            "notification_dtos_" + userId
        };
        
        for (String key : keysToRemove) {
            if (cache.remove(key) != null) {
                System.out.println("🗑️ Caching Decorator: Invalidated cache key: " + key);
            }
        }
    }
    
    // Method để invalidate tất cả notification cache một cách an toàn
    private void invalidateAllNotificationCache() {
        try {
            // Lấy tất cả keys trong cache
            Set<String> allKeys = new HashSet<>(cache.keySet());
            
            // Chỉ invalidate các key liên quan đến notification
            for (String key : allKeys) {
                if (key.startsWith("notifications_") || 
                    key.startsWith("unread_count_") || 
                    key.startsWith("notification_dtos_")) {
                    if (cache.remove(key) != null) {
                        System.out.println("🗑️ Caching Decorator: Invalidated notification cache key: " + key);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Caching Decorator: Error invalidating notification cache: " + e.getMessage());
            // Fallback: clear toàn bộ cache nếu có lỗi
            try {
                clearAllCache();
            } catch (Exception fallbackError) {
                System.err.println("❌ Caching Decorator: Fallback cache clear also failed: " + fallbackError.getMessage());
            }
        }
    }
    
    // Method để clear toàn bộ cache
    public void clearAllCache() {
        int size = cache.size();
        cache.clear();
        System.out.println("🗑️ Caching Decorator: Cleared all cache entries (" + size + " entries)");
    }
    
    // Method để xem thống kê cache
    public void printCacheStats() {
        long expiredCount = cache.values().stream()
            .filter(this::isExpired)
            .count();
        
        System.out.println("📊 Caching Decorator: Cache Stats - Total: " + cache.size() + 
                          ", Expired: " + expiredCount + 
                          ", Valid: " + (cache.size() - expiredCount));
    }
    
    private boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() > entry.expirationTime;
    }
    
    private static class CacheEntry {
        final Object data;
        final long expirationTime;
        
        CacheEntry(Object data, long expirationTime) {
            this.data = data;
            this.expirationTime = expirationTime;
        }
    }
}
