package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.CrawlingConfigRequest;
import GraduationProject.forumikaa.dto.CrawlingConfigResponse;

import java.util.List;
import java.util.Map;

public interface CrawlingConfigService {
    
    /**
     * Tạo config mới
     */
    CrawlingConfigResponse createConfig(CrawlingConfigRequest request, Long userId);
    
    /**
     * Cập nhật config
     */
    CrawlingConfigResponse updateConfig(Long configId, CrawlingConfigRequest request);
    
    /**
     * Lấy tất cả configs
     */
    List<CrawlingConfigResponse> getAllConfigs();
    
    /**
     * Lấy configs đang active
     */
    List<CrawlingConfigResponse> getActiveConfigs();
    
    /**
     * Lấy config theo ID
     */
    CrawlingConfigResponse getConfigById(Long id);
    
    /**
     * Xóa config
     */
    void deleteConfig(Long id);
    
    /**
     * Toggle enable/disable config
     */
    CrawlingConfigResponse toggleConfig(Long id);
    
    /**
     * Lấy configs sẵn sàng để crawl
     */
    List<GraduationProject.forumikaa.entity.CrawlingConfig> getConfigsReadyForCrawling();
    
    /**
     * Cập nhật thống kê crawling
     */
    void updateCrawlingStats(Long configId, int crawledCount, boolean success);
    
    /**
     * Lấy thống kê
     */
    Object getStatistics();
    
    /**
     * Lấy danh sách groups để chọn trong crawling config
     */
    List<Map<String, Object>> getGroupsForSelection();
}
