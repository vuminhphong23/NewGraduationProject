package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.*;
import GraduationProject.forumikaa.service.RecommendationService;
import GraduationProject.forumikaa.service.CrawledContentRecommendationService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;
    
    @Autowired
    private CrawledContentRecommendationService crawledContentService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * Gợi ý người dùng để kết bạn
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserRecommendationResponse>> recommendUsers(
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            List<UserRecommendationResponse> recommendations;
            
            if ("INTERESTS".equals(type)) {
                recommendations = recommendationService.recommendUsersByInterests(currentUserId, limit);
            } else if ("MUTUAL_FRIENDS".equals(type)) {
                recommendations = recommendationService.recommendUsersByMutualFriends(currentUserId, limit);
            } else if ("RECENT_INTERACTIONS".equals(type)) {
                recommendations = recommendationService.recommendUsersByRecentInteractions(currentUserId, limit);
            } else {
                // Default: tổng hợp cả ba
                recommendations = recommendationService.recommendUsers(currentUserId, limit);
            }
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gợi ý crawled content dựa trên mối quan tâm cá nhân
     */
    @GetMapping("/crawled-content")
    public ResponseEntity<List<PostResponse>> getRecommendedCrawledContent(
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            List<PostResponse> recommendations = crawledContentService
                    .getRecommendedCrawledContent(currentUserId, limit);
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gợi ý crawled content trending
     */
    @GetMapping("/crawled-content/trending")
    public ResponseEntity<List<PostResponse>> getTrendingCrawledContent(
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            List<PostResponse> recommendations = crawledContentService
                    .getTrendingCrawledContent(currentUserId, limit);
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gợi ý crawled content theo mối quan tâm cụ thể
     */
    @GetMapping("/crawled-content/interest/{interest}")
    public ResponseEntity<List<PostResponse>> getCrawledContentByInterest(
            @PathVariable String interest,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            List<PostResponse> recommendations = crawledContentService
                    .getCrawledContentByInterest(currentUserId, interest, limit);
            
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
