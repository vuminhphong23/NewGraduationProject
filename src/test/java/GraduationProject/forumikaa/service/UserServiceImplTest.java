package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.Role;
import GraduationProject.forumikaa.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1);
        testRole.setName("USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setGender("Male");
        testUser.setPhone("1234567890");
        testUser.setAddress("Test Address");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setProfileInfo("Test Profile");
        testUser.setRoles(Set.of(testRole));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPaginated_WithAllParameters_ShouldReturnFilteredUsers() {
        // Given
        String keyword = "test";
        String status = "active";
        String roleName = "USER";
        Pageable pageable = mock(Pageable.class);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));

        when(userDao.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<User> result = userService.findPaginated(keyword, status, roleName, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testUser.getId(), result.getContent().get(0).getId());
        verify(userDao).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPaginated_WithNullKeyword_ShouldNotFilterByKeyword() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));

        when(userDao.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<User> result = userService.findPaginated(null, "active", "USER", pageable);

        // Then
        assertNotNull(result);
        verify(userDao).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPaginated_WithEmptyKeyword_ShouldNotFilterByKeyword() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));

        when(userDao.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<User> result = userService.findPaginated("   ", "active", "USER", pageable);

        // Then
        assertNotNull(result);
        verify(userDao).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPaginated_WithBannedStatus_ShouldFilterByEnabledFalse() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));

        when(userDao.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<User> result = userService.findPaginated("test", "banned", "USER", pageable);

        // Then
        assertNotNull(result);
        verify(userDao).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPaginated_WithInvalidStatus_ShouldNotFilterByStatus() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));

        when(userDao.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<User> result = userService.findPaginated("test", "invalid", "USER", pageable);

        // Then
        assertNotNull(result);
        verify(userDao).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void findByUsername_WithExistingUsername_ShouldReturnUser() {
        // Given
        when(userDao.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        verify(userDao).findByUsername("testuser");
    }

    @Test
    void findByUsername_WithNonExistingUsername_ShouldReturnEmpty() {
        // Given
        when(userDao.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userDao).findByUsername("nonexistent");
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given
        when(userDao.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        verify(userDao).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_WithNonExistingEmail_ShouldReturnEmpty() {
        // Given
        when(userDao.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(result.isPresent());
        verify(userDao).findByEmail("nonexistent@example.com");
    }

    @Test
    void updateUserEnabledStatus_WithExistingUser_ShouldUpdateStatus() {
        // Given
        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserEnabledStatus(1L, false);

        // Then
        assertFalse(testUser.isEnabled());
        verify(userDao).findById(1L);
        verify(userDao).save(testUser);
    }

    @Test
    void updateUserEnabledStatus_WithNonExistingUser_ShouldThrowException() {
        // Given
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserEnabledStatus(999L, false);
        });

        assertTrue(exception.getMessage().contains("User not found with id: 999"));
        verify(userDao).findById(999L);
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldCallDeleteById() {
        // Given
        doNothing().when(userDao).deleteById(1L);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userDao).deleteById(1L);
    }

    @Test
    void findById_WithExistingId_ShouldReturnUser() {
        // Given
        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        verify(userDao).findById(1L);
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // Given
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userDao).findById(999L);
    }

    @Test
    void save_WithExistingUser_ShouldUpdateUser() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("User");
        updatedUser.setEnabled(false);
        updatedUser.setGender("Female");
        updatedUser.setPhone("0987654321");
        updatedUser.setAddress("Updated Address");
        updatedUser.setBirthDate(LocalDate.of(1995, 5, 5));
        updatedUser.setProfileInfo("Updated Profile");
        updatedUser.setPassword("newPassword");
        updatedUser.setRoles(Set.of(testRole));

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userDao.save(any(User.class))).thenReturn(updatedUser);

        // When
        userService.save(updatedUser);

        // Then
        verify(userDao).findById(1L);
        verify(passwordEncoder).encode("newPassword");
        verify(userDao).save(any(User.class));
    }

    @Test
    void save_WithExistingUserAndEncodedPassword_ShouldNotReEncode() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("$2a$10$encodedPassword");

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(any(User.class))).thenReturn(updatedUser);

        // When
        userService.save(updatedUser);

        // Then
        verify(userDao).findById(1L);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao).save(any(User.class));
    }

    @Test
    void save_WithExistingUserAndEmptyPassword_ShouldNotUpdatePassword() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("");

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(any(User.class))).thenReturn(updatedUser);

        // When
        userService.save(updatedUser);

        // Then
        verify(userDao).findById(1L);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao).save(any(User.class));
    }

    @Test
    void save_WithNonExistingUser_ShouldCreateNewUser() {
        // Given
        User newUser = new User();
        newUser.setId(null);
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("plainPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userDao.save(any(User.class))).thenReturn(newUser);

        // When
        userService.save(newUser);

        // Then
        verify(passwordEncoder).encode("plainPassword");
        verify(userDao).save(newUser);
    }

    @Test
    void save_WithNonExistingUserAndEncodedPassword_ShouldNotReEncode() {
        // Given
        User newUser = new User();
        newUser.setId(null);
        newUser.setPassword("$2a$10$encodedPassword");

        when(userDao.save(any(User.class))).thenReturn(newUser);

        // When
        userService.save(newUser);

        // Then
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao).save(newUser);
    }

    @Test
    void save_WithNonExistingUserAndEmptyPassword_ShouldNotEncode() {
        // Given
        User newUser = new User();
        newUser.setId(null);
        newUser.setPassword("");

        when(userDao.save(any(User.class))).thenReturn(newUser);

        // When
        userService.save(newUser);

        // Then
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao).save(newUser);
    }

    @Test
    void save_WithNonExistingUserAndNullPassword_ShouldNotEncode() {
        // Given
        User newUser = new User();
        newUser.setId(null);
        newUser.setPassword(null);

        when(userDao.save(any(User.class))).thenReturn(newUser);

        // When
        userService.save(newUser);

        // Then
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao).save(newUser);
    }

    @Test
    void save_WithExistingUserAndNullPassword_ShouldNotUpdatePassword() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword(null);

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(any(User.class))).thenReturn(updatedUser);

        // When
        userService.save(updatedUser);

        // Then
        verify(userDao).findById(1L);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao).save(any(User.class));
    }

    @Test
    void save_WithNonExistingUser_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        User updatedUser = new User();
        updatedUser.setId(999L);

        when(userDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.save(updatedUser);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userDao).findById(999L);
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userDao.findAll()).thenReturn(users);

        // When
        List<User> result = userService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getId(), result.get(0).getId());
        verify(userDao).findAll();
    }

    @Test
    void existsByUsername_WithExistingUsernameAndDifferentId_ShouldReturnTrue() {
        // Given
        when(userDao.findByUsername("existinguser")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.existsByUsername("existinguser", 2L);

        // Then
        assertTrue(result);
        verify(userDao).findByUsername("existinguser");
    }

    @Test
    void existsByUsername_WithExistingUsernameAndSameId_ShouldReturnFalse() {
        // Given
        when(userDao.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.existsByUsername("testuser", 1L);

        // Then
        assertFalse(result);
        verify(userDao).findByUsername("testuser");
    }

    @Test
    void existsByUsername_WithNonExistingUsername_ShouldReturnFalse() {
        // Given
        when(userDao.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        boolean result = userService.existsByUsername("nonexistent", 1L);

        // Then
        assertFalse(result);
        verify(userDao).findByUsername("nonexistent");
    }

    @Test
    void existsByEmail_WithExistingEmailAndDifferentId_ShouldReturnTrue() {
        // Given
        when(userDao.findByEmail("existing@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.existsByEmail("existing@example.com", 2L);

        // Then
        assertTrue(result);
        verify(userDao).findByEmail("existing@example.com");
    }

    @Test
    void existsByEmail_WithExistingEmailAndSameId_ShouldReturnFalse() {
        // Given
        when(userDao.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.existsByEmail("test@example.com", 1L);

        // Then
        assertFalse(result);
        verify(userDao).findByEmail("test@example.com");
    }

    @Test
    void existsByEmail_WithNonExistingEmail_ShouldReturnFalse() {
        // Given
        when(userDao.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        boolean result = userService.existsByEmail("nonexistent@example.com", 1L);

        // Then
        assertFalse(result);
        verify(userDao).findByEmail("nonexistent@example.com");
    }

    @Test
    void existsPhone_WithExistingPhoneAndDifferentId_ShouldReturnTrue() {
        // Given
        when(userDao.findByPhone("1234567890")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.existsPhone("1234567890", 2L);

        // Then
        assertTrue(result);
        verify(userDao).findByPhone("1234567890");
    }

    @Test
    void existsPhone_WithExistingPhoneAndSameId_ShouldReturnFalse() {
        // Given
        when(userDao.findByPhone("1234567890")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.existsPhone("1234567890", 1L);

        // Then
        assertFalse(result);
        verify(userDao).findByPhone("1234567890");
    }

    @Test
    void existsPhone_WithNonExistingPhone_ShouldReturnFalse() {
        // Given
        when(userDao.findByPhone("9999999999")).thenReturn(Optional.empty());

        // When
        boolean result = userService.existsPhone("9999999999", 1L);

        // Then
        assertFalse(result);
        verify(userDao).findByPhone("9999999999");
    }

    @Test
    void checkPassword_WithValidPassword_ShouldReturnTrue() {
        // When
        boolean result = userService.checkPassword("validPassword123");

        // Then
        assertTrue(result);
    }

    @Test
    void checkPassword_WithShortPassword_ShouldReturnFalse() {
        // When
        boolean result = userService.checkPassword("12345");

        // Then
        assertFalse(result);
    }

    @Test
    void checkPassword_WithNullPassword_ShouldReturnFalse() {
        // When
        boolean result = userService.checkPassword(null);

        // Then
        assertFalse(result);
    }

    @Test
    void checkPassword_WithEmptyPassword_ShouldReturnFalse() {
        // When
        boolean result = userService.checkPassword("");

        // Then
        assertFalse(result);
    }

    @Test
    void updateUserPassword_WithExistingUser_ShouldUpdatePassword() {
        // Given
        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userDao.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserPassword(1L, "newPassword");

        // Then
        verify(userDao).findById(1L);
        verify(passwordEncoder).encode("newPassword");
        verify(userDao).save(testUser);
    }

    @Test
    void updateUserPassword_WithNonExistingUser_ShouldThrowException() {
        // Given
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserPassword(999L, "newPassword");
        });

        assertTrue(exception.getMessage().contains("User not found with id: 999"));
        verify(userDao).findById(999L);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao, never()).save(any(User.class));
    }
}
