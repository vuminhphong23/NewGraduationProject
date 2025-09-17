package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.ApiResponse;
import GraduationProject.forumikaa.dto.CrawlingConfigRequest;
import GraduationProject.forumikaa.dto.CrawlingConfigResponse;
import GraduationProject.forumikaa.entity.CrawlingConfig;
import GraduationProject.forumikaa.service.CrawlingConfigService;
import GraduationProject.forumikaa.service.DynamicCrawlingService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/crawling")
@CrossOrigin(origins = "*")
public class AdminCrawlingController {
    
    @Autowired
    private CrawlingConfigService crawlingConfigService;
    
    @Autowired
    private DynamicCrawlingService dynamicCrawlingService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    /**
     * Lấy danh sách groups để chọn
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGroups() {
        try {
            List<Map<String, Object>> groups = crawlingConfigService.getGroupsForSelection();
            return ResponseEntity.ok(ApiResponse.success("Groups loaded successfully", groups));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load groups", e.getMessage()));
        }
    }
    
    /**
     * Lấy tất cả configs
     */
    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<CrawlingConfigResponse>>> getAllConfigs() {
        try {
            List<CrawlingConfigResponse> configs = crawlingConfigService.getAllConfigs();
            return ResponseEntity.ok(ApiResponse.success("Configs loaded successfully", configs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load configs", e.getMessage()));
        }
    }
    
    /**
     * Lấy configs đang active
     */
    @GetMapping("/configs/active")
    public ResponseEntity<List<CrawlingConfigResponse>> getActiveConfigs() {
        try {
            List<CrawlingConfigResponse> configs = crawlingConfigService.getActiveConfigs();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Lấy config theo ID
     */
    @GetMapping("/configs/{id}")
    public ResponseEntity<CrawlingConfigResponse> getConfigById(@PathVariable Long id) {
        try {
            CrawlingConfigResponse config = crawlingConfigService.getConfigById(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Tạo config mới
     */
    @PostMapping("/configs")
    public ResponseEntity<CrawlingConfigResponse> createConfig(@Valid @RequestBody CrawlingConfigRequest request) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            CrawlingConfigResponse config = crawlingConfigService.createConfig(request, currentUserId);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Cập nhật config
     */
    @PutMapping("/configs/{id}")
    public ResponseEntity<CrawlingConfigResponse> updateConfig(@PathVariable Long id, @Valid @RequestBody CrawlingConfigRequest request) {
        try {
            CrawlingConfigResponse config = crawlingConfigService.updateConfig(id, request);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Xóa config
     */
    @DeleteMapping("/configs/{id}")
    public ResponseEntity<String> deleteConfig(@PathVariable Long id) {
        try {
            crawlingConfigService.deleteConfig(id);
            return ResponseEntity.ok("Config deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Toggle enable/disable config
     */
    @PostMapping("/configs/{id}/toggle")
    public ResponseEntity<CrawlingConfigResponse> toggleConfig(@PathVariable Long id) {
        try {
            CrawlingConfigResponse config = crawlingConfigService.toggleConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Crawl config ngay lập tức
     */
    @PostMapping("/configs/{id}/crawl")
    public ResponseEntity<String> crawlConfig(@PathVariable Long id) {
        try {
            CrawlingConfigResponse configResponse = crawlingConfigService.getConfigById(id);
            
            // Convert response to entity for crawling
            CrawlingConfig config = new CrawlingConfig();
            config.setId(configResponse.getId());
            config.setName(configResponse.getName());
            config.setBaseUrl(configResponse.getBaseUrl());
            config.setTopicName(configResponse.getTopicName());
            config.setMaxPosts(configResponse.getMaxPosts());
            
            // Convert groupIds from response to JSON string for entity
            if (configResponse.getGroupIds() != null && !configResponse.getGroupIds().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String groupIdsJson = objectMapper.writeValueAsString(configResponse.getGroupIds());
                    config.setGroupIds(groupIdsJson);
                    System.out.println("🔍 DEBUG: Set groupIds for crawling: " + groupIdsJson);
                } catch (Exception e) {
                    System.err.println("❌ Error converting groupIds: " + e.getMessage());
                }
            }
            
            dynamicCrawlingService.crawlConfig(config);
            return ResponseEntity.ok("Crawling started successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Crawling failed: " + e.getMessage());
        }
    }
    
    /**
     * Crawl tất cả configs active
     */
    @PostMapping("/crawl-all")
    public ResponseEntity<String> crawlAllActive() {
        try {
            dynamicCrawlingService.crawlAllActiveConfigs();
            return ResponseEntity.ok("All active configs crawled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Crawling failed: " + e.getMessage());
        }
    }
    
    /**
     * Lấy thống kê
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object> getStatistics() {
        try {
            Object stats = crawlingConfigService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // ===== LEGACY ENDPOINTS FOR BACKWARD COMPATIBILITY =====
    
    /**
     * Legacy endpoint - Start education news crawling
     */
    @PostMapping("/start-education-news")
    public ResponseEntity<String> startEducationNewsCrawling() {
        try {
            dynamicCrawlingService.crawlAllActiveConfigs();
            return ResponseEntity.ok("Education news crawling started successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Legacy endpoint - Start trending topics crawling
     */
    @PostMapping("/start-trending-topics")
    public ResponseEntity<String> startTrendingTopicsCrawling() {
        try {
            dynamicCrawlingService.crawlAllActiveConfigs();
            return ResponseEntity.ok("Trending topics crawling started successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Legacy endpoint - Start academic content crawling
     */
    @PostMapping("/start-academic-content")
    public ResponseEntity<String> startAcademicContentCrawling() {
        try {
            dynamicCrawlingService.crawlAllActiveConfigs();
            return ResponseEntity.ok("Academic content crawling started successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
    
    
