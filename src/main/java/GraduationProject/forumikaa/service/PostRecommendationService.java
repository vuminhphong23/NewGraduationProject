package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.PostResponse;
import java.util.*;

public interface PostRecommendationService {
    
    /**
     * Tab 1: Cho riêng bạn - Phân tích điểm dựa trên topic quan tâm và tương tác
     */
    List<PostResponse> getPersonalizedContent(Long userId, Integer limit);

    /**
     * Tab 2: Được nhiều người quan tâm - Sắp xếp theo điểm Like(1) + Comment(2) + Share(3)
     */
    List<PostResponse> getTrendingContent(Long userId, Integer limit);
    
    /**
     * Lấy bài viết được nhiều người quan tâm với phân trang
     */
    List<PostResponse> getPopularPosts(Long userId, Integer limit, Integer offset);
    
    // Legacy methods for backward compatibility
    @Deprecated
    List<PostResponse> getRecommendedCrawledContent(Long userId, Integer limit);
    
    @Deprecated
    List<PostResponse> getTrendingCrawledContent(Long userId, Integer limit);

    @Deprecated
    List<PostResponse> getCrawledContentByInterest(Long userId, String interest, Integer limit);
}
