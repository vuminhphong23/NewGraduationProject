package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.CommentDao;
import GraduationProject.forumikaa.entity.Comment;
import GraduationProject.forumikaa.entity.LikeableType;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import GraduationProject.forumikaa.exception.UnauthorizedException;
import GraduationProject.forumikaa.patterns.proxy.VirtualNotificationServiceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;


    @Override
    @Transactional
    public boolean toggleLike(Long commentId, Long userId) {
        Comment comment = commentDao.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment không tồn tại"));

        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        boolean wasLiked = likeService.isLikedByUser(userId, commentId, LikeableType.COMMENT);
        boolean isLiked = likeService.toggleLike(userId, commentId, LikeableType.COMMENT);
        
        // Gửi notification khi like comment (chỉ khi chưa like trước đó)
        if (isLiked && !comment.getUser().getId().equals(userId)) { // Không gửi notification cho chính mình
            notificationService.createCommentLikeNotification(commentId, comment.getUser().getId(), userId);
        }
        
        return isLiked;
    }

    @Override
    public int getCommentLikeCount(Long commentId) {
        return likeService.getLikeCount(commentId, LikeableType.COMMENT).intValue();
    }

    @Override
    public boolean isCommentLikedByUser(Long commentId, Long userId) {
        return likeService.isLikedByUser(userId, commentId, LikeableType.COMMENT);
    }

    @Override
    @Transactional
    public Comment updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentDao.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment không tồn tại"));

        // Kiểm tra quyền: chỉ user tạo comment mới được sửa
        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền sửa bình luận này");
        }

        comment.setContent(content);
        return commentDao.save(comment);
    }
}
