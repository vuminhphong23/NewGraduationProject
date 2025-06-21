package GraduationProject.forumikaa;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Role;
import GraduationProject.forumikaa.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@SpringBootApplication
public class ForumikaaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForumikaaApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(UserDao userDao, RoleDao roleDao, PasswordEncoder passwordEncoder) {
		return args -> {
			// Create roles
			Role userRole = roleDao.findByName("ROLE_USER").orElseGet(() -> roleDao.save(new Role("ROLE_USER")));
			Role adminRole = roleDao.findByName("ROLE_ADMIN").orElseGet(() -> roleDao.save(new Role("ROLE_ADMIN")));

			// Create a test user
			if (userDao.findByUsername("user").isEmpty()) {
				User user = new User();
				user.setUsername("user");
				user.setPassword(passwordEncoder.encode("user123"));
				user.setEmail("user@example.com");
				user.setFirstName("Regular");
				user.setLastName("User");
				user.setRoles(Set.of(userRole));
				userDao.save(user);
			}

			// Create a test admin
			if (userDao.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setEmail("admin@example.com");
				admin.setFirstName("Admin");
				admin.setLastName("User");
				admin.setRoles(Set.of(adminRole));
				userDao.save(admin);
			}
		};
	}
}
