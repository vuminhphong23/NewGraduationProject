package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.PostResponse;
import java.util.*;


public interface PostRecommendationService {
    List<PostResponse> getRecommendedCrawledContent(Long userId, Integer limit);

    /**
     * Gợi ý crawled content theo trending topics
     */
    List<PostResponse> getTrendingCrawledContent(Long userId, Integer limit);

    /**
     * Gợi ý crawled content theo mối quan tâm cụ thể
     */
    List<PostResponse> getCrawledContentByInterest(Long userId, String interest, Integer limit);
}
