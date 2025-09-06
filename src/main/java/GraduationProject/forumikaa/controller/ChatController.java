package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.ChatMessageDto;
import GraduationProject.forumikaa.dto.ChatRoomDto;
import GraduationProject.forumikaa.handler.chat.ChatWebSocketHandler;
import GraduationProject.forumikaa.service.ChatService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    // Lấy danh sách chat rooms của user
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getUserChatRooms() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<ChatRoomDto> rooms = chatService.getUserChatRooms(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rooms);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Tạo hoặc tìm chat room 1-1
    @PostMapping("/private-chat")
    public ResponseEntity<Map<String, Object>> createOrFindPrivateChat(@RequestParam Long otherUserId) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            ChatRoomDto room = chatService.findOrCreatePrivateChat(currentUserId, otherUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", room);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Lấy tin nhắn của room
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Map<String, Object>> getRoomMessages(@PathVariable Long roomId) {
        try {
            System.out.println("ChatController.getRoomMessages() - roomId: " + roomId);
            Long currentUserId = securityUtil.getCurrentUserId();
            System.out.println("ChatController.getRoomMessages() - currentUserId: " + currentUserId);
            
            // Kiểm tra quyền truy cập
            if (!chatService.hasAccessToRoom(roomId, currentUserId)) {
                System.out.println("ChatController.getRoomMessages() - No access to room");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.status(403).body(response);
            }
            
            List<ChatMessageDto> messages = chatService.getRoomMessages(roomId, 0, 50);
            System.out.println("ChatController.getRoomMessages() - Found " + messages.size() + " messages");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("ChatController.getRoomMessages() - Error: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Gửi tin nhắn
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> request) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            String content = request.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Nội dung tin nhắn không được để trống");
            return ResponseEntity.badRequest().body(response);
            }
            
            ChatMessageDto message = chatService.sendMessage(roomId, currentUserId, content, "TEXT");
            
            System.out.println("📤 ChatController: Message sent successfully, ID: " + message.getId());
            
            // Gửi WebSocket event cho tin nhắn mới
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("content", message.getContent());
            messageData.put("senderId", message.getSenderId());
            messageData.put("senderName", message.getSenderFullName() != null ? message.getSenderFullName() : message.getSenderUsername());
            messageData.put("senderAvatar", message.getSenderAvatar());
            messageData.put("roomId", message.getRoomId());
            messageData.put("messageType", message.getMessageType());
            messageData.put("createdAt", message.getCreatedAt());
            messageData.put("isRead", message.isRead());
            
            System.out.println("📤 ChatController: Broadcasting message to room " + roomId + ", excluding user " + currentUserId);
            chatWebSocketHandler.sendToRoom(roomId, messageData, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Đánh dấu tin nhắn đã đọc
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<Map<String, Object>> markMessageAsRead(@PathVariable Long messageId) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            chatService.markMessageAsRead(messageId, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã đánh dấu tin nhắn đã đọc");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Đánh dấu tất cả tin nhắn trong room đã đọc
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Map<String, Object>> markRoomAsRead(@PathVariable Long roomId) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            chatService.markRoomAsRead(roomId, currentUserId);
            
            // Gửi WebSocket event cho message read status
            Map<String, Object> readMessage = Map.of(
                "type", "MESSAGE_READ",
                "roomId", roomId,
                "userId", currentUserId,
                "timestamp", System.currentTimeMillis()
            );
            
            // Gửi đến tất cả user trong room
            chatWebSocketHandler.broadcastToAllUsers(readMessage, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã đánh dấu room đã đọc");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}