package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.*;
import GraduationProject.forumikaa.service.RecommendationService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

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



}
