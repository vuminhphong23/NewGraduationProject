package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Comment;

public interface CommentService {
    
    /**
     * Toggle like cho comment
     * @param commentId ID của comment
     * @param userId ID của user
     * @return true nếu đã like, false nếu đã unlike
     */
    boolean toggleLike(Long commentId, Long userId);
    
    /**
     * Lấy số lượng like của comment
     * @param commentId ID của comment
     * @return Số lượng like
     */
    int getCommentLikeCount(Long commentId);
    
    /**
     * Cập nhật nội dung comment
     * @param commentId ID của comment
     * @param userId ID của user (để kiểm tra quyền)
     * @param content Nội dung mới
     * @return Comment đã được cập nhật
     */
    Comment updateComment(Long commentId, Long userId, String content);
    
    /**
     * Kiểm tra user đã like comment chưa
     * @param commentId ID của comment
     * @param userId ID của user
     * @return true nếu đã like
     */
    boolean isCommentLikedByUser(Long commentId, Long userId);
}
