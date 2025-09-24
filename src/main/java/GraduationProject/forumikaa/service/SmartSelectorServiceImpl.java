package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.util.ContentFilterUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SmartSelectorServiceImpl implements SmartSelectorService {
    
    @Autowired
    private ContentFilterUtil contentFilterUtil;

    // Các selector phổ biến cho title
    private static final String[] TITLE_SELECTORS = {
            "h1", "h2", "h3", "h4", "h5", "h6",
            ".title", "title-news", ".headline", ".post-title", ".article-title",
            ".entry-title", ".news-title", ".item-title",
            "a[title]", ".title a", "h1 a", "h2 a", "h3 a",
            ".card-title", ".content-title", ".main-title",
            ".story-title", ".post-header h1", ".page-title",
            ".post-heading", ".entry-header h1"
    };

    // Các selector phổ biến cho content
    private static final String[] CONTENT_SELECTORS = {
            ".content", ".description", ".excerpt", ".summary",
            ".post-content", ".article-content", ".entry-content",
            ".news-content", ".item-content", ".card-content",
            ".text", ".body", ".main-content", ".content-body",
            "p", ".paragraph", ".intro", ".lead",
            ".post-body", ".story-content", ".entry-text",
            ".article-body", ".post-description", ".content-detail",
            ".post-entry"
    };

    // Các selector phổ biến cho link
    private static final String[] LINK_SELECTORS = {
            "a", ".link", ".read-more", ".more-link",
            ".post-link", ".article-link", ".item-link",
            ".title a", "h1 a", "h2 a", "h3 a",
            ".card-link", ".news-link", ".entry-link",
            ".story-link", ".headline a", ".post-title a"
    };

    // Các selector phổ biến cho container của bài viết
    private static final String[] ARTICLE_CONTAINERS = {
            "article", ".post", ".item", ".news-item",
            ".card", ".entry", ".story", ".article-item",
            ".content-item", ".blog-post", ".news-article",
            ".list-item", ".feed-item", ".post-wrapper",
            ".post-block", ".entry-item", ".article-block",
            ".story-item", ".result-item", ".post-card",
            "entry-content"
    };


    /**
     * Tự động detect và extract thông tin từ trang web với topic filter
     */
    @Override
    public List<Map<String, String>> extractArticles(Document doc, int maxArticles, String topicName) {
        List<Map<String, String>> articles = new ArrayList<>();
        
        System.out.println("🔍 DEBUG: Starting article extraction with topic: " + topicName);
        
        // Tìm container chứa các bài viết
        Elements containers = findArticleContainers(doc);
        System.out.println("🔍 DEBUG: Found " + containers.size() + " containers");
        
        int count = 0;
        int processedCount = 0;
        for (Element container : containers) {
            if (count >= maxArticles) break;
            
            Map<String, String> article = extractArticleFromContainer(container);
            String title = article.get("title");
            String content = article.get("content");
            String link = article.get("link");
            
            processedCount++;
            System.out.println("🔍 DEBUG: Processing article " + processedCount + ": " + title);
            
            // Kiểm tra chất lượng và relevance với topic
            if (title != null && !title.trim().isEmpty()) {
                if (topicName != null) {
                    // Có topic filter - kiểm tra relevance và quality
                    boolean isAcceptable = contentFilterUtil.isArticleAcceptable(title, content, link, topicName);
                    System.out.println("🔍 DEBUG: Article acceptable: " + isAcceptable);
                    if (isAcceptable) {
                        articles.add(article);
                        count++;
                        System.out.println("✅ DEBUG: Added article: " + title);
                    }
                } else {
                    // Không có topic filter - chỉ kiểm tra quality cơ bản
                    if (contentFilterUtil.isTitleQuality(title)) {
                        articles.add(article);
                        count++;
                        System.out.println("✅ DEBUG: Added article: " + title);
                    }
                }
            } else {
                System.out.println("❌ DEBUG: Article has no title");
            }
        }
        
        System.out.println("🔍 DEBUG: Final result: " + articles.size() + " articles extracted");
        return articles;
    }
    
    /**
     * Tìm các container chứa bài viết
     */
    private Elements findArticleContainers(Document doc) {
        Elements containers = new Elements();
        
        // Thử từng selector container
        for (String selector : ARTICLE_CONTAINERS) {
            Elements found = doc.select(selector);
            if (!found.isEmpty()) {
                containers.addAll(found);
                break; // Chỉ lấy loại container đầu tiên tìm thấy
            }
        }
        
        // Nếu không tìm thấy container, lấy tất cả elements có thể chứa title
        if (containers.isEmpty()) {
            for (String selector : TITLE_SELECTORS) {
                Elements found = doc.select(selector);
                if (!found.isEmpty()) {
                    containers.addAll(found);
                    break;
                }
            }
        }
        
        return containers;
    }
    
    /**
     * Extract thông tin từ một container
     */
    private Map<String, String> extractArticleFromContainer(Element container) {
        Map<String, String> article = new HashMap<>();
        
        // Extract title
        String title = extractTitle(container);
        article.put("title", title);
        
        // Extract content
        String content = extractContent(container);
        article.put("content", content);
        
        // Extract link
        String link = extractLink(container);
        article.put("link", link);
        
        return article;
    }
    
    /**
     * Extract title từ element
     */
    private String extractTitle(Element element) {
        for (String selector : TITLE_SELECTORS) {
            try {
                Element titleElement = element.selectFirst(selector);
                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    if (!title.isEmpty() && title.length() > 5) { // Title phải có ít nhất 5 ký tự
                        return normalizeText(title);
                    }
                }
            } catch (Exception e) {
                // Bỏ qua lỗi và thử selector tiếp theo
            }
        }
        return null;
    }
    
    /**
     * Extract content từ element
     */
    private String extractContent(Element element) {
        for (String selector : CONTENT_SELECTORS) {
            try {
                Element contentElement = element.selectFirst(selector);
                if (contentElement != null) {
                    String content = contentElement.text().trim();
                    if (!content.isEmpty() && content.length() > 10) { // Content phải có ít nhất 10 ký tự
                        return normalizeText(content);
                    }
                }
            } catch (Exception e) {
                // Bỏ qua lỗi và thử selector tiếp theo
            }
        }
        return null;
    }
    
    /**
     * Extract link từ element
     */
    private String extractLink(Element element) {
        for (String selector : LINK_SELECTORS) {
            try {
                Element linkElement = element.selectFirst(selector);
                if (linkElement != null) {
                    String href = linkElement.attr("href");
                    if (!href.isEmpty()) {
                        // Convert relative URL to absolute
                        if (href.startsWith("/")) {
                            String baseUrl = element.baseUri();
                            if (baseUrl != null && !baseUrl.isEmpty()) {
                                return baseUrl.replaceAll("/$", "") + href;
                            }
                        }
                        return href;
                    }
                }
            } catch (Exception e) {
                // Bỏ qua lỗi và thử selector tiếp theo
            }
        }
        return null;
    }
    
    /**
     * Normalize text - loại bỏ khoảng trắng thừa, xuống dòng
     */
    private String normalizeText(String text) {
        if (text == null) return null;
        
        text = text.replaceAll("\\s+", " "); // Thay thế nhiều khoảng trắng bằng 1 khoảng
        text = text.replaceAll("\\n+", " "); // Thay thế xuống dòng bằng khoảng trắng
        text = text.trim();
        
        return text.isEmpty() ? null : text;
    }

}
