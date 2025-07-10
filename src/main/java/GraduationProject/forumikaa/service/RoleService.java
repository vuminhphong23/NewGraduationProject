package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    Optional<Role> findByName(String name);
    List<Role> findAll();
}
