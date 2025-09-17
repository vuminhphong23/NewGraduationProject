package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> getPostStatistics(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            Map<String, Object> statistics = statisticsService.getPostStatistics(startDate, endDate);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy thống kê bài viết: " + e.getMessage()));
        }
    }

    @GetMapping("/posts/summary")
    public ResponseEntity<Map<String, Object>> getPostSummary() {
        try {
            Map<String, Object> summary = statisticsService.getPostSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy tóm tắt thống kê: " + e.getMessage()));
        }
    }

    @GetMapping("/posts/distribution-by-groups")
    public ResponseEntity<Map<String, Object>> getPostDistributionByGroups(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            Map<String, Object> distribution = statisticsService.getPostDistributionByGroups(startDate, endDate);
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy phân bố bài viết theo groups: " + e.getMessage()));
        }
    }

    @GetMapping("/activities/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivities(
            @RequestParam(defaultValue = "all") String activityType,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> activities = statisticsService.getRecentActivities(activityType, limit);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Lỗi khi lấy hoạt động gần đây: " + e.getMessage())));
        }
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        try {
            Map<String, Object> overview = statisticsService.getDashboardOverview();
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy thống kê tổng quan: " + e.getMessage()));
        }
    }
}
