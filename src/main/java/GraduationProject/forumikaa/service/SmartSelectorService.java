package GraduationProject.forumikaa.service;

import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;

public interface SmartSelectorService {
    
    /**
     * Tự động detect và extract thông tin từ trang web với topic filter
     */
    List<Map<String, String>> extractArticles(Document doc, int maxArticles, String topicName);

}
