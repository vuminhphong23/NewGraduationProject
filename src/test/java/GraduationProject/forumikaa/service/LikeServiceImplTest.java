package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.LikeDao;
import GraduationProject.forumikaa.entity.Like;
import GraduationProject.forumikaa.entity.LikeableType;
import GraduationProject.forumikaa.entity.User;
import GraduationProject.forumikaa.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeDao likeDao;

    @Mock
    private UserService userService;

    @InjectMocks
    private LikeServiceImpl likeService;

    private User testUser;
    private Like testLike;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testLike = new Like();
        testLike.setId(1L);
        testLike.setUser(testUser);
        testLike.setLikeableId(1L);
        testLike.setLikeableType(LikeableType.POST);
        testLike.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void toggleLike_WhenUserNotExists_ShouldThrowException() {
        // Given
        when(userService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            likeService.toggleLike(999L, 1L, LikeableType.POST);
        });

        assertEquals("User không tồn tại", exception.getMessage());
        verify(userService).findById(999L);
        verify(likeDao, never()).findByUserIdAndLikeableIdAndLikeableType(anyLong(), anyLong(), any(LikeableType.class));
        verify(likeDao, never()).save(any(Like.class));
        verify(likeDao, never()).delete(any(Like.class));
    }

    @Test
    void toggleLike_WhenLikeNotExists_ShouldCreateNewLike() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST))
                .thenReturn(Optional.empty());
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(1L);
            return like;
        });

        // When
        boolean result = likeService.toggleLike(1L, 1L, LikeableType.POST);

        // Then
        assertTrue(result);
        verify(userService).findById(1L);
        verify(likeDao).findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST);
        verify(likeDao).save(any(Like.class));
        verify(likeDao, never()).delete(any(Like.class));
    }

    @Test
    void toggleLike_WhenLikeExists_ShouldDeleteLike() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST))
                .thenReturn(Optional.of(testLike));
        doNothing().when(likeDao).delete(testLike);

        // When
        boolean result = likeService.toggleLike(1L, 1L, LikeableType.POST);

        // Then
        assertFalse(result);
        verify(userService).findById(1L);
        verify(likeDao).findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST);
        verify(likeDao).delete(testLike);
        verify(likeDao, never()).save(any(Like.class));
    }

    @Test
    void toggleLike_WithCommentType_ShouldWorkCorrectly() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 2L, LikeableType.COMMENT))
                .thenReturn(Optional.empty());
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(2L);
            return like;
        });

        // When
        boolean result = likeService.toggleLike(1L, 2L, LikeableType.COMMENT);

        // Then
        assertTrue(result);
        verify(userService).findById(1L);
        verify(likeDao).findByUserIdAndLikeableIdAndLikeableType(1L, 2L, LikeableType.COMMENT);
        verify(likeDao).save(any(Like.class));
    }

    @Test
    void getLikeCount_WithValidParameters_ShouldReturnCount() {
        // Given
        when(likeDao.countByLikeableIdAndLikeableType(1L, LikeableType.POST)).thenReturn(5L);

        // When
        Long result = likeService.getLikeCount(1L, LikeableType.POST);

        // Then
        assertEquals(5L, result);
        verify(likeDao).countByLikeableIdAndLikeableType(1L, LikeableType.POST);
    }

    @Test
    void getLikeCount_WithZeroLikes_ShouldReturnZero() {
        // Given
        when(likeDao.countByLikeableIdAndLikeableType(2L, LikeableType.POST)).thenReturn(0L);

        // When
        Long result = likeService.getLikeCount(2L, LikeableType.POST);

        // Then
        assertEquals(0L, result);
        verify(likeDao).countByLikeableIdAndLikeableType(2L, LikeableType.POST);
    }

    @Test
    void getLikeCount_WithCommentType_ShouldReturnCount() {
        // Given
        when(likeDao.countByLikeableIdAndLikeableType(1L, LikeableType.COMMENT)).thenReturn(3L);

        // When
        Long result = likeService.getLikeCount(1L, LikeableType.COMMENT);

        // Then
        assertEquals(3L, result);
        verify(likeDao).countByLikeableIdAndLikeableType(1L, LikeableType.COMMENT);
    }

    @Test
    void isLikedByUser_WhenLiked_ShouldReturnTrue() {
        // Given
        when(likeDao.existsByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST))
                .thenReturn(true);

        // When
        boolean result = likeService.isLikedByUser(1L, 1L, LikeableType.POST);

        // Then
        assertTrue(result);
        verify(likeDao).existsByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST);
    }

    @Test
    void isLikedByUser_WhenNotLiked_ShouldReturnFalse() {
        // Given
        when(likeDao.existsByUserIdAndLikeableIdAndLikeableType(1L, 2L, LikeableType.POST))
                .thenReturn(false);

        // When
        boolean result = likeService.isLikedByUser(1L, 2L, LikeableType.POST);

        // Then
        assertFalse(result);
        verify(likeDao).existsByUserIdAndLikeableIdAndLikeableType(1L, 2L, LikeableType.POST);
    }

    @Test
    void isLikedByUser_WithCommentType_ShouldWorkCorrectly() {
        // Given
        when(likeDao.existsByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.COMMENT))
                .thenReturn(true);

        // When
        boolean result = likeService.isLikedByUser(1L, 1L, LikeableType.COMMENT);

        // Then
        assertTrue(result);
        verify(likeDao).existsByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.COMMENT);
    }

    @Test
    void toggleLike_WhenCreatingNewLike_ShouldSetCorrectProperties() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST))
                .thenReturn(Optional.empty());
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(1L);
            return like;
        });

        // When
        likeService.toggleLike(1L, 1L, LikeableType.POST);

        // Then
        verify(likeDao).save(argThat(like -> 
            like.getUser().equals(testUser) &&
            like.getLikeableId().equals(1L) &&
            like.getLikeableType() == LikeableType.POST
        ));
    }

    @Test
    void toggleLike_WithNullUserId_ShouldThrowException() {
        // Given
        when(userService.findById(null)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            likeService.toggleLike(null, 1L, LikeableType.POST);
        });

        assertEquals("User không tồn tại", exception.getMessage());
        verify(userService).findById(null);
    }

    @Test
    void toggleLike_WithNullLikeableId_ShouldWork() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, null, LikeableType.POST))
                .thenReturn(Optional.empty());
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(1L);
            return like;
        });

        // When
        boolean result = likeService.toggleLike(1L, null, LikeableType.POST);

        // Then
        assertTrue(result);
        verify(likeDao).save(argThat(like -> like.getLikeableId() == null));
    }

    @Test
    void toggleLike_WithNullLikeableType_ShouldWork() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 1L, null))
                .thenReturn(Optional.empty());
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(1L);
            return like;
        });

        // When
        boolean result = likeService.toggleLike(1L, 1L, null);

        // Then
        assertTrue(result);
        verify(likeDao).save(argThat(like -> like.getLikeableType() == null));
    }

    @Test
    void getLikeCount_WithNullLikeableId_ShouldWork() {
        // Given
        when(likeDao.countByLikeableIdAndLikeableType(null, LikeableType.POST)).thenReturn(0L);

        // When
        Long result = likeService.getLikeCount(null, LikeableType.POST);

        // Then
        assertEquals(0L, result);
        verify(likeDao).countByLikeableIdAndLikeableType(null, LikeableType.POST);
    }

    @Test
    void getLikeCount_WithNullLikeableType_ShouldWork() {
        // Given
        when(likeDao.countByLikeableIdAndLikeableType(1L, null)).thenReturn(0L);

        // When
        Long result = likeService.getLikeCount(1L, null);

        // Then
        assertEquals(0L, result);
        verify(likeDao).countByLikeableIdAndLikeableType(1L, null);
    }

    @Test
    void isLikedByUser_WithNullUserId_ShouldReturnFalse() {
        // Given
        when(likeDao.existsByUserIdAndLikeableIdAndLikeableType(null, 1L, LikeableType.POST))
                .thenReturn(false);

        // When
        boolean result = likeService.isLikedByUser(null, 1L, LikeableType.POST);

        // Then
        assertFalse(result);
        verify(likeDao).existsByUserIdAndLikeableIdAndLikeableType(null, 1L, LikeableType.POST);
    }

    @Test
    void isLikedByUser_WithNullLikeableId_ShouldReturnFalse() {
        // Given
        when(likeDao.existsByUserIdAndLikeableIdAndLikeableType(1L, null, LikeableType.POST))
                .thenReturn(false);

        // When
        boolean result = likeService.isLikedByUser(1L, null, LikeableType.POST);

        // Then
        assertFalse(result);
        verify(likeDao).existsByUserIdAndLikeableIdAndLikeableType(1L, null, LikeableType.POST);
    }

    @Test
    void isLikedByUser_WithNullLikeableType_ShouldReturnFalse() {
        // Given
        when(likeDao.existsByUserIdAndLikeableIdAndLikeableType(1L, 1L, null))
                .thenReturn(false);

        // When
        boolean result = likeService.isLikedByUser(1L, 1L, null);

        // Then
        assertFalse(result);
        verify(likeDao).existsByUserIdAndLikeableIdAndLikeableType(1L, 1L, null);
    }

    @Test
    void toggleLike_MultipleCalls_ShouldToggleCorrectly() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(testLike));
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(1L);
            return like;
        });
        doNothing().when(likeDao).delete(testLike);

        // When - First call (like)
        boolean result1 = likeService.toggleLike(1L, 1L, LikeableType.POST);
        
        // Then - First call
        assertTrue(result1);
        verify(likeDao).save(any(Like.class));

        // When - Second call (unlike)
        boolean result2 = likeService.toggleLike(1L, 1L, LikeableType.POST);
        
        // Then - Second call
        assertFalse(result2);
        verify(likeDao).delete(testLike);
    }

    @Test
    void getLikeCount_WithLargeNumber_ShouldReturnCorrectCount() {
        // Given
        when(likeDao.countByLikeableIdAndLikeableType(1L, LikeableType.POST)).thenReturn(999999L);

        // When
        Long result = likeService.getLikeCount(1L, LikeableType.POST);

        // Then
        assertEquals(999999L, result);
        verify(likeDao).countByLikeableIdAndLikeableType(1L, LikeableType.POST);
    }

    @Test
    void toggleLike_WithDifferentUsers_ShouldWorkIndependently() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.findById(2L)).thenReturn(Optional.of(user2));
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(1L, 1L, LikeableType.POST))
                .thenReturn(Optional.empty());
        when(likeDao.findByUserIdAndLikeableIdAndLikeableType(2L, 1L, LikeableType.POST))
                .thenReturn(Optional.empty());
        when(likeDao.save(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            like.setId(System.currentTimeMillis());
            return like;
        });

        // When
        boolean result1 = likeService.toggleLike(1L, 1L, LikeableType.POST);
        boolean result2 = likeService.toggleLike(2L, 1L, LikeableType.POST);

        // Then
        assertTrue(result1);
        assertTrue(result2);
        verify(likeDao, times(2)).save(any(Like.class));
    }
}
