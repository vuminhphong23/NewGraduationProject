package GraduationProject.forumikaa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String avatar;
    private String role;
    private String joinedAt;
}