package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class RoleServiceImpl implements RoleService{
    @Autowired
    private RoleDao roleDao;

    @Override
    public Optional<Role> findByName(String name) {
        return roleDao.findByName(name);
    }

    @Override
    public List<Role> findAll() {
        return roleDao.findAll();
    }
}
