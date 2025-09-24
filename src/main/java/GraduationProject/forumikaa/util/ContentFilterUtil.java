package GraduationProject.forumikaa.util;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ContentFilterUtil {
    // Các từ không phù hợp cần lọc
    private static final Set<String> INAPPROPRIATE_KEYWORDS = Set.of(
            "đéo", "đéo biết", "đéo hiểu", "đéo quan tâm",
            "chó", "chó má", "chó đẻ", "chó chết", "chó cái",
            "thằng chó", "con chó", "đồ chó"
    );


    /**
     * Kiểm tra xem title có liên quan đến topic không
     */
    public boolean isTitleRelevantToTopic(String title, String topicName) {
        if (title == null || topicName == null) return false;

        // Xóa dấu _ và chuyển về lowercase
        String normalizedTopic = topicName.toLowerCase().replace("_", " ");
        String normalizedTitle = title.toLowerCase();
        
        // So sánh không phân biệt dấu
        String topicNoAccents = removeVietnameseAccents(normalizedTopic);
        String titleNoAccents = removeVietnameseAccents(normalizedTitle);
        
        return titleNoAccents.contains(topicNoAccents);
    }

    /**
     * Kiểm tra chất lượng title
     */
    public boolean isTitleQuality(String title) {
        if (title == null || title.trim().isEmpty()) return false;

        String lowerTitle = title.toLowerCase().trim();

        // Kiểm tra độ dài tối thiểu
        if (lowerTitle.length() < 10) return false;

        // Kiểm tra độ dài tối đa
        if (lowerTitle.length() > 200) return false;

        // Kiểm tra từ không phù hợp (không phân biệt dấu)
        String titleNoAccents = removeVietnameseAccents(lowerTitle);
        for (String inappropriate : INAPPROPRIATE_KEYWORDS) {
            String inappropriateNoAccents = removeVietnameseAccents(inappropriate);
            if (titleNoAccents.contains(inappropriateNoAccents)) {
                return false;
            }
        }


        // Kiểm tra có quá nhiều ký tự đặc biệt
        long specialCharCount = title.chars().filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)).count();
        if (specialCharCount > title.length() * 0.3) return false;

        return true;
    }

    /**
     * Kiểm tra chất lượng content
     */
    public boolean isContentQuality(String content) {
        if (content == null || content.trim().isEmpty()) return false;

        String lowerContent = content.toLowerCase().trim();

        // Kiểm tra độ dài tối thiểu
        if (lowerContent.length() < 20) return false;

        // Kiểm tra độ dài tối đa
        if (lowerContent.length() > 5000) return false;

        // Kiểm tra từ không phù hợp (không phân biệt dấu)
        String contentNoAccents = removeVietnameseAccents(lowerContent);
        for (String inappropriate : INAPPROPRIATE_KEYWORDS) {
            String inappropriateNoAccents = removeVietnameseAccents(inappropriate);
            if (contentNoAccents.contains(inappropriateNoAccents)) {
                return false;
            }
        }


        return true;
    }


    /**
     * Tính điểm chất lượng tổng thể
     */
    public int calculateQualityScore(String title, String content, String link, String topicName) {
        int score = 0;

        // Title quality (40 điểm)
        if (isTitleQuality(title)) {
            score += 20;
            if (isTitleRelevantToTopic(title, topicName)) {
                score += 20;
            }
        }

        // Content quality (30 điểm)
        if (isContentQuality(content)) {
            score += 30;
        }

        // Link quality (20 điểm) - chỉ cần có link
        if (link != null && !link.trim().isEmpty()) {
            score += 20;
        }

        // Bonus points (10 điểm)
        if (title != null && content != null &&
                title.length() > 20 && content.length() > 50) {
            score += 10;
        }

        return score;
    }

    /**
     * Kiểm tra xem article có đủ chất lượng để lưu không
     */
    public boolean isArticleAcceptable(String title, String content, String link, String topicName) {
        System.out.println("🔍 DEBUG: Checking article: " + title);
        System.out.println("🔍 DEBUG: Topic: " + topicName);
        
        // Phải có title và title phải liên quan đến topic
        boolean isRelevant = isTitleRelevantToTopic(title, topicName);
        System.out.println("🔍 DEBUG: Title relevant to topic: " + isRelevant);
        if (title == null || !isRelevant) {
            return false;
        }

        // Title phải có chất lượng
        boolean isTitleGood = isTitleQuality(title);
        System.out.println("🔍 DEBUG: Title quality: " + isTitleGood);
        if (!isTitleGood) {
            return false;
        }

        // Content phải có chất lượng (nếu có)
        if (content != null) {
            boolean isContentGood = isContentQuality(content);
            System.out.println("🔍 DEBUG: Content quality: " + isContentGood);
            if (!isContentGood) {
                return false;
            }
        }

        // Tính điểm tổng thể, phải >= 60/100
        int score = calculateQualityScore(title, content, link, topicName);
        System.out.println("🔍 DEBUG: Quality score: " + score + "/100");
        boolean isAcceptable = score >= 60;
        System.out.println("🔍 DEBUG: Final acceptable: " + isAcceptable);
        return isAcceptable;
    }
    
    /**
     * Xóa dấu tiếng Việt
     */
    private String removeVietnameseAccents(String text) {
        if (text == null) return "";
        
        return text.replace("à", "a").replace("á", "a").replace("ạ", "a").replace("ả", "a").replace("ã", "a")
                  .replace("â", "a").replace("ầ", "a").replace("ấ", "a").replace("ậ", "a").replace("ẩ", "a").replace("ẫ", "a")
                  .replace("ă", "a").replace("ằ", "a").replace("ắ", "a").replace("ặ", "a").replace("ẳ", "a").replace("ẵ", "a")
                  .replace("è", "e").replace("é", "e").replace("ẹ", "e").replace("ẻ", "e").replace("ẽ", "e")
                  .replace("ê", "e").replace("ề", "e").replace("ế", "e").replace("ệ", "e").replace("ể", "e").replace("ễ", "e")
                  .replace("ì", "i").replace("í", "i").replace("ị", "i").replace("ỉ", "i").replace("ĩ", "i")
                  .replace("ò", "o").replace("ó", "o").replace("ọ", "o").replace("ỏ", "o").replace("õ", "o")
                  .replace("ô", "o").replace("ồ", "o").replace("ố", "o").replace("ộ", "o").replace("ổ", "o").replace("ỗ", "o")
                  .replace("ơ", "o").replace("ờ", "o").replace("ớ", "o").replace("ợ", "o").replace("ở", "o").replace("ỡ", "o")
                  .replace("ù", "u").replace("ú", "u").replace("ụ", "u").replace("ủ", "u").replace("ũ", "u")
                  .replace("ư", "u").replace("ừ", "u").replace("ứ", "u").replace("ự", "u").replace("ử", "u").replace("ữ", "u")
                  .replace("ỳ", "y").replace("ý", "y").replace("ỵ", "y").replace("ỷ", "y").replace("ỹ", "y")
                  .replace("đ", "d");
    }
}
