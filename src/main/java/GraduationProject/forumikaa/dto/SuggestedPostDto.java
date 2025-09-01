package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedPostDto {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private LocalDateTime createdAt;
    private String privacy;
    private Integer friendshipLevel;
}



