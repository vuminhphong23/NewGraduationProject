package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.LikeableType;

public interface LikeService {

    boolean toggleLike(Long userId, Long likeableId, LikeableType likeableType);

    Long getLikeCount(Long likeableId, LikeableType likeableType);

    boolean isLikedByUser(Long userId, Long likeableId, LikeableType likeableType);

    Long getUserLikeCount(Long userId, LikeableType likeableType);

    Long getUserLikeCountInDateRange(Long userId, LikeableType likeableType, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

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
