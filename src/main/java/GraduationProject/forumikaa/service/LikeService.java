package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.LikeableType;

public interface LikeService {
    
    /**
     * Toggle like cho bất kỳ loại content nào
     * @param userId ID của user
     * @param likeableId ID của content (post, comment, etc.)
     * @param likeableType Loại content
     * @return true nếu đã like, false nếu đã unlike
     */
    boolean toggleLike(Long userId, Long likeableId, LikeableType likeableType);
    
    /**
     * Lấy số lượng like của content
     * @param likeableId ID của content
     * @param likeableType Loại content
     * @return Số lượng like
     */
    Long getLikeCount(Long likeableId, LikeableType likeableType);
    
    /**
     * Kiểm tra user đã like content chưa
     * @param userId ID của user
     * @param likeableId ID của content
     * @param likeableType Loại content
     * @return true nếu đã like
     */
    boolean isLikedByUser(Long userId, Long likeableId, LikeableType likeableType);
    
    // Convenience methods for Posts (backward compatibility)
    default boolean togglePostLike(Long userId, Long postId) {
        return toggleLike(userId, postId, LikeableType.POST);
    }
    
    default Long getPostLikeCount(Long postId) {
        return getLikeCount(postId, LikeableType.POST);
    }
    
    default boolean isPostLikedByUser(Long postId, Long userId) {
        return isLikedByUser(userId, postId, LikeableType.POST);
    }
    
    default boolean likePost(Long postId, Long userId) {
        return toggleLike(userId, postId, LikeableType.POST);
    }
    
    default boolean unlikePost(Long postId, Long userId) {
        return toggleLike(userId, postId, LikeableType.POST);
    }
}
