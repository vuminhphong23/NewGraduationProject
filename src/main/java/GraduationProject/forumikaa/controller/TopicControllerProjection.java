package GraduationProject.forumikaa.controller;
import GraduationProject.forumikaa.dao.TopicDao.TopicSummaryProjection;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/api/topics/projection")
public class TopicControllerProjection {

    private TopicService topicService;

    @Autowired
    public void setTopicService(TopicService topicService){
        this.topicService = topicService;
    }

    //Lấy top topics sử dụng projection (hiệu suất cao hơn)
    @GetMapping("/top")
    public ResponseEntity<List<TopicSummaryProjection>> getTopTopics(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopicSummaryProjection> topics = topicService.getTopTopicsProjection(limit);
        return ResponseEntity.ok(topics);
    }

    //Lấy trending topics sử dụng projection
    @GetMapping("/trending")
    public ResponseEntity<List<TopicSummaryProjection>> getTrendingTopics() {
        List<TopicSummaryProjection> topics = topicService.getTrendingTopicsProjection();
        return ResponseEntity.ok(topics);
    }

    //So sánh hiệu suất: Lấy top topics với projection vs không có projection
    @GetMapping("/compare-performance")
    public ResponseEntity<Map<String, Object>> comparePerformance() {
        long startTime = System.currentTimeMillis();
        List<TopicSummaryProjection> withProjection = topicService.getTopTopicsProjection(10);
        long projectionTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        List<Topic> withoutProjection = topicService.getTopTopics(10);
        long withoutProjectionTime = System.currentTimeMillis() - startTime;

        Map<String, Object> result = Map.of(
                "withProjection", Map.of(
                        "time", projectionTime + "ms",
                        "dataSize", withProjection.size(),
                        "data", withProjection
                ),
                "withoutProjection", Map.of(
                        "time", withoutProjectionTime + "ms",
                        "dataSize", withoutProjection.size(),
                        "data", withoutProjection
                )
        );

        return ResponseEntity.ok(result);
    }
}
