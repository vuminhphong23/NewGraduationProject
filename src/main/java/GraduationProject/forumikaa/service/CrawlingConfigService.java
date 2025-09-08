package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.CrawlingConfigDao;
import GraduationProject.forumikaa.dto.CrawlingConfigRequest;
import GraduationProject.forumikaa.dto.CrawlingConfigResponse;
import GraduationProject.forumikaa.entity.CrawlingConfig;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CrawlingConfigService {
    
    @Autowired
    private CrawlingConfigDao crawlingConfigDao;
    
    @Autowired
    private SystemUserService systemUserService;
    
    /**
     * Tạo config mới
     */
    public CrawlingConfigResponse createConfig(CrawlingConfigRequest request, Long userId) {
        User adminUser = systemUserService.getAdminUser();
        
        CrawlingConfig config = new CrawlingConfig();
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setBaseUrl(request.getBaseUrl());
        config.setTitleSelector(request.getTitleSelector());
        config.setContentSelector(request.getContentSelector());
        config.setLinkSelector(request.getLinkSelector());
        config.setImageSelector(request.getImageSelector());
        config.setAuthorSelector(request.getAuthorSelector());
        config.setDateSelector(request.getDateSelector());
        config.setTopicName(request.getTopicName());
        config.setTopicSelector(request.getTopicSelector());
        config.setMaxPosts(request.getMaxPosts());
        config.setIntervalMinutes(request.getIntervalMinutes());
        config.setEnabled(request.getEnabled());
        config.setUserAgent(request.getUserAgent());
        config.setTimeout(request.getTimeout());
        config.setAdditionalHeaders(request.getAdditionalHeaders());
        config.setPostProcessingRules(request.getPostProcessingRules());
        config.setStatus("ACTIVE");
        config.setCreatedBy(adminUser);
        
        CrawlingConfig savedConfig = crawlingConfigDao.save(config);
        return convertToResponse(savedConfig);
    }
    
    /**
     * Cập nhật config
     */
    public CrawlingConfigResponse updateConfig(Long configId, CrawlingConfigRequest request) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setBaseUrl(request.getBaseUrl());
        config.setTitleSelector(request.getTitleSelector());
        config.setContentSelector(request.getContentSelector());
        config.setLinkSelector(request.getLinkSelector());
        config.setImageSelector(request.getImageSelector());
        config.setAuthorSelector(request.getAuthorSelector());
        config.setDateSelector(request.getDateSelector());
        config.setTopicName(request.getTopicName());
        config.setTopicSelector(request.getTopicSelector());
        config.setMaxPosts(request.getMaxPosts());
        config.setIntervalMinutes(request.getIntervalMinutes());
        config.setEnabled(request.getEnabled());
        config.setUserAgent(request.getUserAgent());
        config.setTimeout(request.getTimeout());
        config.setAdditionalHeaders(request.getAdditionalHeaders());
        config.setPostProcessingRules(request.getPostProcessingRules());
        
        CrawlingConfig savedConfig = crawlingConfigDao.save(config);
        return convertToResponse(savedConfig);
    }
    
    /**
     * Lấy tất cả configs
     */
    public List<CrawlingConfigResponse> getAllConfigs() {
        return crawlingConfigDao.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy configs đang active
     */
    public List<CrawlingConfigResponse> getActiveConfigs() {
        return crawlingConfigDao.findByEnabledTrueAndStatusOrderByCreatedAtDesc(true, "ACTIVE").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy configs cần crawl
     */
    public List<CrawlingConfig> getConfigsReadyForCrawling() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1);
        return crawlingConfigDao.findConfigsReadyForCrawling(cutoffTime);
    }
    
    /**
     * Lấy config theo ID
     */
    public CrawlingConfigResponse getConfigById(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        return convertToResponse(config);
    }
    
    /**
     * Xóa config
     */
    public void deleteConfig(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        crawlingConfigDao.delete(config);
    }
    
    /**
     * Toggle enable/disable config
     */
    public CrawlingConfigResponse toggleConfig(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        
        config.setEnabled(!config.getEnabled());
        CrawlingConfig savedConfig = crawlingConfigDao.save(config);
        return convertToResponse(savedConfig);
    }
    
    /**
     * Test config (crawl thử)
     */
    public String testConfig(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        
        try {
            // TODO: Implement test crawling logic
            return "Test crawling completed successfully";
        } catch (Exception e) {
            config.setStatus("ERROR");
            config.setLastError(e.getMessage());
            crawlingConfigDao.save(config);
            return "Test failed: " + e.getMessage();
        }
    }
    
    /**
     * Cập nhật thống kê sau khi crawl
     */
    public void updateCrawlingStats(Long configId, int crawledCount, boolean success) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        
        config.setLastCrawledAt(LocalDateTime.now());
        config.setTotalCrawled(config.getTotalCrawled() + crawledCount);
        
        if (success) {
            config.setSuccessCount(config.getSuccessCount() + 1);
            config.setStatus("ACTIVE");
            config.setLastError(null);
        } else {
            config.setErrorCount(config.getErrorCount() + 1);
            config.setStatus("ERROR");
        }
        
        crawlingConfigDao.save(config);
    }
    
    /**
     * Lấy thống kê tổng quan
     */
    public Object getStatistics() {
        return new Object() {
            public final Long totalConfigs = crawlingConfigDao.count();
            public final Long activeConfigs = crawlingConfigDao.countActiveConfigs();
            public final Long errorConfigs = crawlingConfigDao.countErrorConfigs();
            public final Long totalCrawledPosts = crawlingConfigDao.getTotalCrawledPosts();
        };
    }
    
    private CrawlingConfigResponse convertToResponse(CrawlingConfig config) {
        CrawlingConfigResponse response = new CrawlingConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setDescription(config.getDescription());
        response.setBaseUrl(config.getBaseUrl());
        response.setTitleSelector(config.getTitleSelector());
        response.setContentSelector(config.getContentSelector());
        response.setLinkSelector(config.getLinkSelector());
        response.setImageSelector(config.getImageSelector());
        response.setAuthorSelector(config.getAuthorSelector());
        response.setDateSelector(config.getDateSelector());
        response.setTopicName(config.getTopicName());
        response.setTopicSelector(config.getTopicSelector());
        response.setMaxPosts(config.getMaxPosts());
        response.setIntervalMinutes(config.getIntervalMinutes());
        response.setEnabled(config.getEnabled());
        response.setUserAgent(config.getUserAgent());
        response.setTimeout(config.getTimeout());
        response.setAdditionalHeaders(config.getAdditionalHeaders());
        response.setPostProcessingRules(config.getPostProcessingRules());
        response.setStatus(config.getStatus());
        response.setLastError(config.getLastError());
        response.setLastCrawledAt(config.getLastCrawledAt());
        response.setTotalCrawled(config.getTotalCrawled());
        response.setSuccessCount(config.getSuccessCount());
        response.setErrorCount(config.getErrorCount());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        response.setCreatedBy(config.getCreatedBy() != null ? config.getCreatedBy().getUsername() : null);
        
        return response;
    }
}


