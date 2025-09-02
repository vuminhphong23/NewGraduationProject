package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRecommendationResponse {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String userAvatar;
    private LocalDateTime createdAt;
    private Double score;
    private String reason; // "Mutual friends", "Similar interests", "Same topics", etc.
    private Integer mutualFriendsCount;
    private List<String> commonTopics;
    private String recommendationType; // SOCIAL, INTEREST_BASED, MUTUAL_FRIENDS
}
