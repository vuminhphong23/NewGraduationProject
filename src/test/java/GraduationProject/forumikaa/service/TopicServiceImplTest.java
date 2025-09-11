package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.TopicDao;
import GraduationProject.forumikaa.entity.Topic;
import GraduationProject.forumikaa.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceImplTest {

    @Mock
    private TopicDao topicDao;

    @InjectMocks
    private TopicServiceImpl topicService;

    private User testUser;
    private Topic testTopic;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testTopic = new Topic();
        testTopic.setId(1L);
        testTopic.setName("test_topic");
        testTopic.setUsageCount(5);
        testTopic.setCreatedBy(testUser);
    }

    @Test
    void findOrCreateTopic_WhenTopicExists_ShouldReturnExistingTopic() {
        // Given
        String topicName = "test_topic";
        when(topicDao.findByName(topicName)).thenReturn(Optional.of(testTopic));

        // When
        Topic result = topicService.findOrCreateTopic(topicName, testUser);

        // Then
        assertNotNull(result);
        assertEquals(testTopic.getId(), result.getId());
        assertEquals(topicName, result.getName());
        verify(topicDao).findByName(topicName);
        verify(topicDao, never()).save(any(Topic.class));
    }

    @Test
    void findOrCreateTopic_WhenTopicNotExists_ShouldCreateNewTopic() {
        // Given
        String topicName = "new_topic";
        when(topicDao.findByName(topicName)).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(2L);
            return topic;
        });

        // When
        Topic result = topicService.findOrCreateTopic(topicName, testUser);

        // Then
        assertNotNull(result);
        assertEquals(topicName, result.getName());
        assertEquals(testUser, result.getCreatedBy());
        assertEquals(0, result.getUsageCount());
        verify(topicDao).findByName(topicName);
        verify(topicDao).save(any(Topic.class));
    }

    @Test
    void findOrCreateTopic_WithHashtagPrefix_ShouldCleanName() {
        // Given
        String topicName = "#hashtag_topic";
        String expectedCleanName = "hashtag_topic";
        when(topicDao.findByName(expectedCleanName)).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(3L);
            return topic;
        });

        // When
        Topic result = topicService.findOrCreateTopic(topicName, testUser);

        // Then
        assertNotNull(result);
        assertEquals(expectedCleanName, result.getName());
        verify(topicDao).findByName(expectedCleanName);
    }

    @Test
    void findOrCreateTopic_WithSpecialCharacters_ShouldCleanName() {
        // Given
        String topicName = "Topic with Spaces & Special!@#";
        String expectedCleanName = "topic_with_spaces__special";
        when(topicDao.findByName(expectedCleanName)).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(4L);
            return topic;
        });

        // When
        Topic result = topicService.findOrCreateTopic(topicName, testUser);

        // Then
        assertNotNull(result);
        assertEquals(expectedCleanName, result.getName());
        verify(topicDao).findByName(expectedCleanName);
    }

    @Test
    void processTopicsFromInput_WithValidTopics_ShouldReturnProcessedTopics() {
        // Given
        List<String> topicNames = Arrays.asList("topic1", "topic2", "topic3");
        when(topicDao.findByName(anyString())).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(System.currentTimeMillis());
            return topic;
        });

        // When
        List<Topic> result = topicService.processTopicsFromInput(topicNames, testUser);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(topicDao, times(3)).findByName(anyString());
        verify(topicDao, times(3)).save(any(Topic.class));
    }

    @Test
    void processTopicsFromInput_WithNullInput_ShouldReturnEmptyList() {
        // When
        List<Topic> result = topicService.processTopicsFromInput(null, testUser);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(topicDao, never()).findByName(anyString());
        verify(topicDao, never()).save(any(Topic.class));
    }

    @Test
    void processTopicsFromInput_WithEmptyInput_ShouldReturnEmptyList() {
        // When
        List<Topic> result = topicService.processTopicsFromInput(Collections.emptyList(), testUser);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(topicDao, never()).findByName(anyString());
        verify(topicDao, never()).save(any(Topic.class));
    }

    @Test
    void processTopicsFromInput_WithNullAndEmptyNames_ShouldFilterThem() {
        // Given
        List<String> topicNames = Arrays.asList("valid_topic", null, "", "  ", "another_valid");
        when(topicDao.findByName(anyString())).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(System.currentTimeMillis());
            return topic;
        });

        // When
        List<Topic> result = topicService.processTopicsFromInput(topicNames, testUser);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(topicDao, times(2)).findByName(anyString());
        verify(topicDao, times(2)).save(any(Topic.class));
    }

    @Test
    void getTopTopics_ShouldReturnLimitedTopics() {
        // Given
        Topic topic1 = new Topic();
        topic1.setId(1L);
        topic1.setName("topic1");
        topic1.setUsageCount(10);

        Topic topic2 = new Topic();
        topic2.setId(2L);
        topic2.setName("topic2");
        topic2.setUsageCount(5);

        Topic topic3 = new Topic();
        topic3.setId(3L);
        topic3.setName("topic3");
        topic3.setUsageCount(0);

        List<Topic> allTopics = Arrays.asList(topic1, topic2, topic3);
        when(topicDao.findTopTopics()).thenReturn(allTopics);

        // When
        List<Topic> result = topicService.getTopTopics(2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(topic -> topic.getUsageCount() > 0));
    }

    @Test
    void incrementUsageCount_WhenUsageCountIsNull_ShouldSetToOne() {
        // Given
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("test_topic");
        topic.setUsageCount(null);
        when(topicDao.save(any(Topic.class))).thenReturn(topic);

        // When
        topicService.incrementUsageCount(topic);

        // Then
        assertEquals(1, topic.getUsageCount());
        verify(topicDao).save(topic);
    }

    @Test
    void incrementUsageCount_WhenUsageCountExists_ShouldIncrementByOne() {
        // Given
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("test_topic");
        topic.setUsageCount(5);
        when(topicDao.save(any(Topic.class))).thenReturn(topic);

        // When
        topicService.incrementUsageCount(topic);

        // Then
        assertEquals(6, topic.getUsageCount());
        verify(topicDao).save(topic);
    }

    @Test
    void decrementUsageCount_WhenUsageCountIsNull_ShouldNotChange() {
        // Given
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("test_topic");
        topic.setUsageCount(null);

        // When
        topicService.decrementUsageCount(topic);

        // Then
        assertNull(topic.getUsageCount());
        verify(topicDao, never()).save(any(Topic.class));
    }

    @Test
    void decrementUsageCount_WhenUsageCountIsZero_ShouldNotChange() {
        // Given
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("test_topic");
        topic.setUsageCount(0);

        // When
        topicService.decrementUsageCount(topic);

        // Then
        assertEquals(0, topic.getUsageCount());
        verify(topicDao, never()).save(any(Topic.class));
    }

    @Test
    void decrementUsageCount_WhenUsageCountIsPositive_ShouldDecrementByOne() {
        // Given
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setName("test_topic");
        topic.setUsageCount(5);
        when(topicDao.save(any(Topic.class))).thenReturn(topic);

        // When
        topicService.decrementUsageCount(topic);

        // Then
        assertEquals(4, topic.getUsageCount());
        verify(topicDao).save(topic);
    }

    @Test
    void cleanHashtagName_WithNullInput_ShouldReturnEmptyString() {
        // This tests the private method indirectly through findOrCreateTopic
        // Given
        when(topicDao.findByName("")).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(1L);
            return topic;
        });

        // When
        Topic result = topicService.findOrCreateTopic(null, testUser);

        // Then
        assertNotNull(result);
        assertEquals("", result.getName());
    }

    @Test
    void cleanHashtagName_WithEmptyInput_ShouldReturnEmptyString() {
        // Given
        when(topicDao.findByName("")).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(1L);
            return topic;
        });

        // When
        Topic result = topicService.findOrCreateTopic("   ", testUser);

        // Then
        assertNotNull(result);
        assertEquals("", result.getName());
    }

    @Test
    void cleanHashtagName_WithVietnameseCharacters_ShouldPreserveThem() {
        // Given
        String topicName = "chủ_đề_tiếng_việt";
        when(topicDao.findByName(topicName)).thenReturn(Optional.empty());
        when(topicDao.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(1L);
            return topic;
        });

        // When
        Topic result = topicService.findOrCreateTopic(topicName, testUser);

        // Then
        assertNotNull(result);
        assertEquals(topicName, result.getName());
    }
}
