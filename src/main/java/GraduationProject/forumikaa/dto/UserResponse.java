package GraduationProject.forumikaa.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String avatar;
    private String profileInfo;
}


