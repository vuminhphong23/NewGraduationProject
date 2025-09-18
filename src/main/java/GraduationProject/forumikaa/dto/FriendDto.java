package GraduationProject.forumikaa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String avatar;
    private String profileLink;
    private String friendDate;
    private boolean isOnline;

}
