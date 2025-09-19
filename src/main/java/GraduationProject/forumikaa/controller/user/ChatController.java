package GraduationProject.forumikaa.controller.user;

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
    public ResponseEntity<Map<String, Object>> createOrFindPrivateChat(@RequestBody Map<String, Object> request) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            
            // Validate request
            if (!request.containsKey("userId") || request.get("userId") == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "userId is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long otherUserId;
            try {
                otherUserId = Long.valueOf(request.get("userId").toString());
            } catch (NumberFormatException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid userId format");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate otherUserId
            if (otherUserId.equals(currentUserId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cannot create chat with yourself");
                return ResponseEntity.badRequest().body(response);
            }
            
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

    // Tạo group chat
    @PostMapping("/group-chat")
    public ResponseEntity<Map<String, Object>> createGroupChat(@RequestBody Map<String, Object> request) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            
            // Validate request
            if (!request.containsKey("groupName") || request.get("groupName") == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "groupName is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!request.containsKey("userIds") || request.get("userIds") == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "userIds is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            String groupName = request.get("groupName").toString();
            @SuppressWarnings("unchecked")
            List<Integer> userIds = (List<Integer>) request.get("userIds");
            
            // Convert to Long list
            List<Long> userIdsLong = userIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
            
            // Add current user to the list
            if (!userIdsLong.contains(currentUserId)) {
                userIdsLong.add(currentUserId);
            }
            
            ChatRoomDto room = chatService.createGroupChat(groupName, currentUserId, userIdsLong);
            
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

    // Xóa chat room
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> deleteChatRoom(@PathVariable Long roomId) {
        try {
            Long currentUserId = securityUtil.getCurrentUserId();
            
            // Kiểm tra quyền truy cập
            if (!chatService.hasAccessToRoom(roomId, currentUserId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Bạn không có quyền xóa cuộc trò chuyện này");
                return ResponseEntity.status(403).body(response);
            }
            
            chatService.deleteChatRoom(roomId, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cuộc trò chuyện đã được xóa");
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
            Long currentUserId = securityUtil.getCurrentUserId();
            
            // Kiểm tra quyền truy cập
            if (!chatService.hasAccessToRoom(roomId, currentUserId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return ResponseEntity.status(403).body(response);
            }
            
            List<ChatMessageDto> messages = chatService.getRoomMessages(roomId, 0, 50);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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