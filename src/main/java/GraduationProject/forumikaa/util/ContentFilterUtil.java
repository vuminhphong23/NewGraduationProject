package GraduationProject.forumikaa.util;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ContentFilterUtil {
    // Các từ khóa spam/không mong muốn
    private static final Set<String> SPAM_KEYWORDS = Set.of(
            "advertisement", "ads", "sponsored", "promotion", "sale", "discount",
            "click here", "buy now", "order now", "free shipping", "limited time",
            "ad", "banner", "popup", "newsletter", "subscribe", "unsubscribe",
            "cookie", "privacy policy", "terms of service", "contact us",
            "about us", "home", "menu", "navigation", "footer", "header",
            "login", "register", "sign up", "sign in", "logout", "profile",
            "search", "filter", "sort", "category", "tag", "archive",
            "copyright", "all rights reserved", "powered by", "designed by"
    );

    // Các từ khóa chất lượng thấp
    private static final Set<String> LOW_QUALITY_KEYWORDS = Set.of(
            "click", "read more", "more", "continue", "next", "previous",
            "back", "top", "bottom", "here", "this", "that", "these", "those",
            "link", "url", "http", "www", "com", "org", "net", "edu",
            "page", "section", "part", "chapter", "item", "entry", "post"
    );

    // Pattern cho URL
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");

    // Pattern cho email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    // Pattern cho số điện thoại
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");

    /**
     * Kiểm tra xem title có liên quan đến topic không
     */
    public boolean isTitleRelevantToTopic(String title, String topicName) {
        if (title == null || topicName == null) return false;

        String lowerTitle = title.toLowerCase();
        String lowerTopic = topicName.toLowerCase();

        // Kiểm tra topic name có trong title không
        if (lowerTitle.contains(lowerTopic)) {
            return true;
        }

        // Kiểm tra các từ khóa liên quan đến topic
        String[] topicWords = lowerTopic.split("\\s+");
        int relevantWords = 0;
        for (String word : topicWords) {
            if (word.length() > 2 && lowerTitle.contains(word)) {
                relevantWords++;
            }
        }

        // Nếu có ít nhất 50% từ khóa topic trong title
        return relevantWords >= Math.ceil(topicWords.length * 0.5);
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

        // Kiểm tra spam keywords
        for (String spam : SPAM_KEYWORDS) {
            if (lowerTitle.contains(spam)) {
                return false;
            }
        }

        // Kiểm tra quá nhiều từ khóa chất lượng thấp
        int lowQualityCount = 0;
        for (String lowQuality : LOW_QUALITY_KEYWORDS) {
            if (lowerTitle.contains(lowQuality)) {
                lowQualityCount++;
            }
        }

        // Nếu có quá nhiều từ khóa chất lượng thấp
        if (lowQualityCount > 3) return false;

        // Kiểm tra có chứa URL không
        if (URL_PATTERN.matcher(title).find()) return false;

        // Kiểm tra có chứa email không
        if (EMAIL_PATTERN.matcher(title).find()) return false;

        // Kiểm tra có chứa số điện thoại không
        if (PHONE_PATTERN.matcher(title).find()) return false;

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

        // Kiểm tra spam keywords
        for (String spam : SPAM_KEYWORDS) {
            if (lowerContent.contains(spam)) {
                return false;
            }
        }

        // Kiểm tra có quá nhiều URL
        long urlCount = URL_PATTERN.matcher(content).results().count();
        if (urlCount > 5) return false;

        // Kiểm tra có quá nhiều email
        long emailCount = EMAIL_PATTERN.matcher(content).results().count();
        if (emailCount > 3) return false;

        // Kiểm tra có quá nhiều số điện thoại
        long phoneCount = PHONE_PATTERN.matcher(content).results().count();
        if (phoneCount > 2) return false;

        return true;
    }

    /**
     * Kiểm tra chất lượng link
     */
    public boolean isLinkQuality(String link) {
        if (link == null || link.trim().isEmpty()) return false;

        // Kiểm tra có phải URL hợp lệ không
        if (!URL_PATTERN.matcher(link).find()) return false;

        // Kiểm tra không phải link nội bộ không cần thiết
        String lowerLink = link.toLowerCase();
        if (lowerLink.contains("#") || lowerLink.contains("javascript:") ||
                lowerLink.contains("mailto:") || lowerLink.contains("tel:")) {
            return false;
        }

        // Kiểm tra không phải link đến file không cần thiết
        if (lowerLink.endsWith(".pdf") || lowerLink.endsWith(".doc") ||
                lowerLink.endsWith(".docx") || lowerLink.endsWith(".xls") ||
                lowerLink.endsWith(".xlsx") || lowerLink.endsWith(".ppt") ||
                lowerLink.endsWith(".pptx")) {
            return false;
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

        // Link quality (20 điểm)
        if (isLinkQuality(link)) {
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
        // Phải có title và title phải liên quan đến topic
        if (title == null || !isTitleRelevantToTopic(title, topicName)) {
            return false;
        }

        // Title phải có chất lượng
        if (!isTitleQuality(title)) {
            return false;
        }

        // Content phải có chất lượng (nếu có)
        if (content != null && !isContentQuality(content)) {
            return false;
        }

        // Link phải có chất lượng (nếu có)
        if (link != null && !isLinkQuality(link)) {
            return false;
        }

        // Tính điểm tổng thể, phải >= 60/100
        int score = calculateQualityScore(title, content, link, topicName);
        return score >= 60;
    }
}
