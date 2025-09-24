package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.UserRecommendationResponse;
import GraduationProject.forumikaa.dto.GroupRecommendationResponse;

import java.util.List;

public interface RecommendationService {
    
    /**
     * Gợi ý người dùng để kết bạn
     */
    List<UserRecommendationResponse> recommendUsers(Long userId, Integer limit);
    
    /**
     * Gợi ý người dùng dựa trên bạn bè chung
     */
    List<UserRecommendationResponse> recommendUsersByMutualFriends(Long userId, Integer limit);
    
    /**
     * Gợi ý người dùng dựa trên mối quan tâm tương tự
     */
    List<UserRecommendationResponse> recommendUsersByInterests(Long userId, Integer limit);
    
    /**
     * Gợi ý người dùng dựa trên tương tác gần đây
     */
    List<UserRecommendationResponse> recommendUsersByRecentInteractions(Long userId, Integer limit);
    
    /**
     * Tab 3: Gợi ý nhóm - Nhóm liên quan topic quan tâm và có nhiều bạn chung
     */
    List<GroupRecommendationResponse> recommendGroups(Long userId, Integer limit);
}
