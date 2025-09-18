package GraduationProject.forumikaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBasicDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String avatar;
    private String profileInfo;
}


