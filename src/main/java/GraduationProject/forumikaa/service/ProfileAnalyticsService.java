package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.PostResponse;
import GraduationProject.forumikaa.dto.GroupMemberDto;
import GraduationProject.forumikaa.entity.LikeableType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ProfileAnalyticsService {
    
    /**
     * Tính toán dữ liệu analytics tổng quan cho user
     */
    Map<String, Object> calculateAnalyticsData(Long userId, List<PostResponse> userPosts, List<GroupMemberDto> userGroups);
    
    /**
     * Lấy dữ liệu topics analytics trong khoảng thời gian
     */
    Map<String, Object> getTopicsAnalytics(Long userId, String startDate, String endDate);
    
    /**
     * Lấy dữ liệu groups analytics trong khoảng thời gian
     */
    Map<String, Object> getGroupsAnalytics(Long userId, String startDate, String endDate);
    
    /**
     * Lấy dữ liệu activity timeline trong khoảng thời gian
     */
    Map<String, Object> getActivityAnalytics(Long userId, String startDate, String endDate);
}
