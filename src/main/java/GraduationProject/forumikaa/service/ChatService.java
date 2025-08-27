package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.ChatMessage;

import java.util.List;

public interface ChatService {

    // Lưu message vào DB và trả lại object vừa lưu
    <T extends ChatMessage> T saveMessage(T message);

    // Lấy lịch sử chat giữa 2 user
    List<ChatMessage> getChatHistory(String user1, String user2);
}
