package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.CrawlingConfigDao;
import GraduationProject.forumikaa.dto.CrawlingConfigRequest;
import GraduationProject.forumikaa.dto.CrawlingConfigResponse;
import GraduationProject.forumikaa.entity.CrawlingConfig;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;

@Service
@Transactional
public class CrawlingConfigServiceImpl implements CrawlingConfigService {
    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private CrawlingConfigDao crawlingConfigDao;
    
    @Autowired
    private GroupService groupService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Tạo config mới
     */
    @Override
    public CrawlingConfigResponse createConfig(CrawlingConfigRequest request, Long userId) {
        User adminUser = securityUtil.getAdminUser();
        
        CrawlingConfig config = new CrawlingConfig();
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setBaseUrl(request.getBaseUrl());
        config.setTopicName(request.getTopicName());
        config.setMaxPosts(request.getMaxPosts());
        config.setEnabled(request.getEnabled());
        config.setStatus("ACTIVE");
        config.setCreatedBy(adminUser);
        
        // Convert groupIds to JSON string
        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            try {
                String groupIdsJson = objectMapper.writeValueAsString(request.getGroupIds());
                config.setGroupIds(groupIdsJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting groupIds to JSON", e);
            }
        }
        
        CrawlingConfig savedConfig = crawlingConfigDao.save(config);
        
        return convertToResponse(savedConfig);
    }
    
    /**
     * Cập nhật config
     */
    @Override
    public CrawlingConfigResponse updateConfig(Long configId, CrawlingConfigRequest request) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setBaseUrl(request.getBaseUrl());
        config.setTopicName(request.getTopicName());
        config.setMaxPosts(request.getMaxPosts());
        config.setEnabled(request.getEnabled());
        
        // Convert groupIds to JSON string
        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            try {
                config.setGroupIds(objectMapper.writeValueAsString(request.getGroupIds()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting groupIds to JSON", e);
            }
        } else {
            config.setGroupIds(null);
        }
        
        CrawlingConfig savedConfig = crawlingConfigDao.save(config);
        return convertToResponse(savedConfig);
    }
    
    /**
     * Lấy tất cả configs
     */
    @Override
    public List<CrawlingConfigResponse> getAllConfigs() {
        return crawlingConfigDao.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy configs đang active
     */
    @Override
    public List<CrawlingConfigResponse> getActiveConfigs() {
        return crawlingConfigDao.findByEnabledTrueAndStatusOrderByCreatedAtDesc(true, "ACTIVE").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy configs cần crawl
     */
    @Override
    public List<CrawlingConfig> getConfigsReadyForCrawling() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1);
        return crawlingConfigDao.findConfigsReadyForCrawling(cutoffTime);
    }
    
    /**
     * Lấy config theo ID
     */
    @Override
    public CrawlingConfigResponse getConfigById(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        return convertToResponse(config);
    }
    
    /**
     * Xóa config
     */
    @Override
    public void deleteConfig(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        crawlingConfigDao.delete(config);
    }
    
    /**
     * Toggle enable/disable config
     */
    @Override
    public CrawlingConfigResponse toggleConfig(Long configId) {
        CrawlingConfig config = crawlingConfigDao.findById(configId)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        
        config.setEnabled(!config.getEnabled());
        CrawlingConfig savedConfig = crawlingConfigDao.save(config);
        return convertToResponse(savedConfig);
    }

    
    /**
     * Cập nhật thống kê sau khi crawl
     */
    @Override
    public void updateCrawlingStats(Long configId, int crawledCount, boolean success) {
        try {
            CrawlingConfig config = crawlingConfigDao.findById(configId)
                    .orElseThrow(() -> new RuntimeException("Config not found"));
            
            config.setLastCrawledAt(LocalDateTime.now());
            config.setTotalCrawled(config.getTotalCrawled() + crawledCount);
            
            if (success) {
                config.setStatus("ACTIVE");
                System.out.println("✅ Updated stats for config " + configId + ": SUCCESS, crawled " + crawledCount + " posts");
            } else {
                config.setStatus("ERROR");
                System.out.println("❌ Updated stats for config " + configId + ": ERROR, failed to crawl");
            }
            
            crawlingConfigDao.save(config);
        } catch (Exception e) {
            System.err.println("❌ Failed to update crawling stats for config " + configId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy thống kê tổng quan
     */
    @Override
    public Object getStatistics() {
        return new Object() {
            public final Long totalConfigs = crawlingConfigDao.count();
            public final Long activeConfigs = crawlingConfigDao.countActiveConfigs();
            public final Long errorConfigs = crawlingConfigDao.countErrorConfigs();
            public final Long totalCrawledPosts = crawlingConfigDao.getTotalCrawledPosts();
        };
    }
    
    /**
     * Lấy danh sách groups để chọn trong crawling config
     */
    @Override
    public List<Map<String, Object>> getGroupsForSelection() {
        return groupService.findAll().stream()
            .map(group -> {
                Map<String, Object> groupData = new java.util.HashMap<>();
                groupData.put("id", group.getId());
                groupData.put("name", group.getName());
                groupData.put("description", group.getDescription());
                return groupData;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    private CrawlingConfigResponse convertToResponse(CrawlingConfig config) {
        CrawlingConfigResponse response = new CrawlingConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setDescription(config.getDescription());
        response.setBaseUrl(config.getBaseUrl());
        response.setTopicName(config.getTopicName());
        response.setMaxPosts(config.getMaxPosts());
        response.setEnabled(config.getEnabled());
        response.setStatus(config.getStatus());
        response.setLastCrawledAt(config.getLastCrawledAt());
        response.setTotalCrawled(config.getTotalCrawled());
        response.setCreatedAt(config.getCreatedAt());
        response.setCreatedBy(config.getCreatedBy() != null ? config.getCreatedBy().getUsername() : null);

        // Parse groupIds from JSON string
        if (config.getGroupIds() != null && !config.getGroupIds().isEmpty()) {
            try {
                List<Long> groupIds = objectMapper.readValue(config.getGroupIds(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
                response.setGroupIds(groupIds);
            } catch (JsonProcessingException e) {
                response.setGroupIds(new ArrayList<>());
            }
        } else {
            response.setGroupIds(new ArrayList<>());
        }

        return response;
    }
}


