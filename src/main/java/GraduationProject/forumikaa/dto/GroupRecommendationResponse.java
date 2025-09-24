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
public class GroupRecommendationResponse {
    private Long id;
    private String name;
    private String description;
    private String avatar;
    private LocalDateTime createdAt;
    private Long memberCount;
    private Integer commonFriendsCount;
    private List<String> topics;
    private Double score;
    private String reason; // "Common interests", "Mutual friends", "Popular topics", etc.
    private String recommendationType; // INTEREST_BASED, MUTUAL_FRIENDS, POPULAR
    private Boolean isJoined; // Whether current user has joined this group
}
