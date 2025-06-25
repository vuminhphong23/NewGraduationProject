package GraduationProject.forumikaa.dto;

import GraduationProject.forumikaa.entity.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class AdminUserDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password; // Only for adding new user
    private boolean enabled;
    private Set<Role> roles;
} 