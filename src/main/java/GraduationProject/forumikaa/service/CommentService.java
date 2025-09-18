package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Comment;
import java.time.LocalDateTime;

public interface CommentService {
    
    Long getUserCommentCount(Long userId);
    
    Long getUserCommentCountInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    boolean toggleLike(Long commentId, Long userId);
    

    int getCommentLikeCount(Long commentId);
    

    Comment updateComment(Long commentId, Long userId, String content);
    

    boolean isCommentLikedByUser(Long commentId, Long userId);
}
