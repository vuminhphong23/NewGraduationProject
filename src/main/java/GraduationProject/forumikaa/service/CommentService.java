package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Comment;

public interface CommentService {
    

    boolean toggleLike(Long commentId, Long userId);
    

    int getCommentLikeCount(Long commentId);
    

    Comment updateComment(Long commentId, Long userId, String content);
    

    boolean isCommentLikedByUser(Long commentId, Long userId);
}
