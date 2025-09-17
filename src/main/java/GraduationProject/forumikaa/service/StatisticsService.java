package GraduationProject.forumikaa.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {
    
    /**
     * Lấy thống kê bài viết theo khoảng thời gian
     * @param startDate ngày bắt đầu (format: yyyy-MM-dd)
     * @param endDate ngày kết thúc (format: yyyy-MM-dd)
     * @return Map chứa dữ liệu thống kê
     */
    Map<String, Object> getPostStatistics(String startDate, String endDate);
    
    /**
     * Lấy tóm tắt thống kê bài viết
     * @return Map chứa tổng quan thống kê
     */
    Map<String, Object> getPostSummary();
    
    /**
     * Lấy thống kê phân bố bài viết theo groups
     * @param startDate ngày bắt đầu (format: yyyy-MM-dd)
     * @param endDate ngày kết thúc (format: yyyy-MM-dd)
     * @return Map chứa dữ liệu phân bố theo groups
     */
    Map<String, Object> getPostDistributionByGroups(String startDate, String endDate);
    
    /**
     * Lấy hoạt động gần đây
     * @param activityType loại hoạt động (all, posts, groups, users)
     * @param limit số lượng hoạt động tối đa
     * @return List các hoạt động gần đây
     */
    List<Map<String, Object>> getRecentActivities(String activityType, int limit);
    
    /**
     * Lấy thống kê tổng quan dashboard
     * @return Map chứa thống kê tổng quan
     */
    Map<String, Object> getDashboardOverview();
}