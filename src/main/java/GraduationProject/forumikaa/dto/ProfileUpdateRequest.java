package GraduationProject.forumikaa.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String birthDate;
    private String gender;
    private String profileInfo;
    private String bio;
    private String avatar;
    private String cover;
    private String socialLinks;
}
