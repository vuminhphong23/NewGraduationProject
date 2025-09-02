package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.UserRecommendationResponse;
import GraduationProject.forumikaa.dto.SuggestedPostDto;

import java.util.List;
import java.util.Map;

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
     * Gợi ý người dùng dựa trên sở thích tương tự
     */
    List<UserRecommendationResponse> recommendUsersByInterests(Long userId, Integer limit);
    
    /**
     * Gợi ý người dùng dựa trên tương tác gần đây
     */
    List<UserRecommendationResponse> recommendUsersByRecentInteractions(Long userId, Integer limit);
}
