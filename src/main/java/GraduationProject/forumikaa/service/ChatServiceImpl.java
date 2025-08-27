package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.ChatMessageDao;
import GraduationProject.forumikaa.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageDao chatMessageDao;

    @Override
    public <T extends ChatMessage> T saveMessage(T message) {
        return chatMessageDao.save(message);
    }

    @Override
    public List<ChatMessage> getChatHistory(String user1, String user2) {
        return chatMessageDao.getChatHistory(user1, user2);
    }
}

