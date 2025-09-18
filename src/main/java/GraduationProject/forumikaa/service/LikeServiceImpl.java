package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.LikeDao;
import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.LikeableType;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeDao likeDao;

    @Autowired
    private UserService userService;
    


    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long likeableId, LikeableType likeableType) {
        // Like/Unlike là quyền cơ bản của user đã đăng nhập - không cần kiểm tra quyền đặc biệt
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // Kiểm tra xem user đã like content này chưa
        Like existingLike = likeDao.findByUserIdAndLikeableIdAndLikeableType(userId, likeableId, likeableType)
                .orElse(null);
        
        if (existingLike != null) {
            // Nếu đã like thì unlike
            likeDao.delete(existingLike);
            return false;
        } else {
            // Nếu chưa like thì like
            Like newLike = new Like();
            newLike.setUser(user);
            newLike.setLikeableId(likeableId);
            newLike.setLikeableType(likeableType);
            likeDao.save(newLike);
            return true;
        }
    }

    @Override
    public Long getLikeCount(Long likeableId, LikeableType likeableType) {
        return likeDao.countByLikeableIdAndLikeableType(likeableId, likeableType);
    }

    @Override
    public boolean isLikedByUser(Long userId, Long likeableId, LikeableType likeableType) {
        return likeDao.existsByUserIdAndLikeableIdAndLikeableType(userId, likeableId, likeableType);
    }

    @Override
    public Long getUserLikeCount(Long userId, LikeableType likeableType) {
        return likeDao.countByUserIdAndLikeableType(userId, likeableType);
    }

    @Override
    public Long getUserLikeCountInDateRange(Long userId, LikeableType likeableType, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return likeDao.countByUserIdAndLikeableTypeAndDateRange(userId, likeableType, startDate, endDate);
    }
    

}
