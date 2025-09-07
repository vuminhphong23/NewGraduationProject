package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.handler.chat.ChatWebSocketHandler;
import GraduationProject.forumikaa.service.ChatAttachmentService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/files")
public class ChatFileController {

    @Autowired
    private ChatAttachmentService chatAttachmentService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    /**
     * Upload file cho chat message
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadChatFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("messageId") Long messageId,
            @RequestParam("roomId") Long roomId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            FileUploadResponse response = chatAttachmentService.uploadChatAttachment(file, messageId, roomId, userId);
            
            // Gửi file qua WebSocket để hiển thị trong chat
            Map<String, Object> fileMessage = new HashMap<>();
            fileMessage.put("type", "FILE_UPLOADED");
            fileMessage.put("file", response);
            fileMessage.put("roomId", roomId);
            fileMessage.put("userId", userId);
            fileMessage.put("timestamp", System.currentTimeMillis());
            
            // Broadcast cho tất cả user trong room
            chatWebSocketHandler.sendToRoom(roomId, fileMessage, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Upload file thành công");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Upload nhiều file cùng lúc
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleChatFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("messageId") Long messageId,
            @RequestParam("roomId") Long roomId) {
        try {
            System.out.println("ChatFileController: uploadMultipleChatFiles called with " + files.length + " files, messageId: " + messageId + ", roomId: " + roomId);
            Long userId = securityUtil.getCurrentUserId();
            System.out.println("ChatFileController: Current userId: " + userId);
            List<FileUploadResponse> responses = chatAttachmentService.uploadMultipleChatAttachments(files, messageId, roomId, userId);
            
            // Gửi files qua WebSocket để hiển thị trong chat
            Map<String, Object> filesMessage = new HashMap<>();
            filesMessage.put("type", "FILES_UPLOADED");
            filesMessage.put("files", responses);
            filesMessage.put("roomId", roomId);
            filesMessage.put("userId", userId);
            filesMessage.put("timestamp", System.currentTimeMillis());
            
            // Broadcast cho tất cả user trong room
            chatWebSocketHandler.sendToRoom(roomId, filesMessage, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", responses);
            result.put("message", "Upload " + responses.size() + " file thành công");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy danh sách tất cả attachment trong chat room
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Object>> getChatRoomFiles(@PathVariable Long roomId) {
        try {
            List<FileUploadResponse> files = chatAttachmentService.getChatRoomAttachments(roomId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", files);
            result.put("count", files.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy danh sách ảnh trong chat room
     */
    @GetMapping("/room/{roomId}/images")
    public ResponseEntity<Map<String, Object>> getChatRoomImages(@PathVariable Long roomId) {
        try {
            List<FileUploadResponse> images = chatAttachmentService.getChatRoomImages(roomId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", images);
            result.put("count", images.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy danh sách file tài liệu trong chat room
     */
    @GetMapping("/room/{roomId}/documents")
    public ResponseEntity<Map<String, Object>> getChatRoomDocuments(@PathVariable Long roomId) {
        try {
            List<FileUploadResponse> documents = chatAttachmentService.getChatRoomDocuments(roomId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", documents);
            result.put("count", documents.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy danh sách video trong chat room
     */
    @GetMapping("/room/{roomId}/videos")
    public ResponseEntity<Map<String, Object>> getChatRoomVideos(@PathVariable Long roomId) {
        try {
            List<FileUploadResponse> videos = chatAttachmentService.getChatRoomVideos(roomId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", videos);
            result.put("count", videos.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy danh sách audio trong chat room
     */
    @GetMapping("/room/{roomId}/audios")
    public ResponseEntity<Map<String, Object>> getChatRoomAudios(@PathVariable Long roomId) {
        try {
            List<FileUploadResponse> audios = chatAttachmentService.getChatRoomAudios(roomId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", audios);
            result.put("count", audios.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy danh sách attachment theo message ID
     */
    @GetMapping("/message/{messageId}")
    public ResponseEntity<Map<String, Object>> getAttachmentsByMessageId(@PathVariable Long messageId) {
        try {
            List<FileUploadResponse> attachments = chatAttachmentService.getAttachmentsByMessageId(messageId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", attachments);
            result.put("count", attachments.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy thông tin attachment theo ID
     */
    @GetMapping("/{attachmentId}")
    public ResponseEntity<Map<String, Object>> getChatAttachmentById(@PathVariable Long attachmentId) {
        try {
            FileUploadResponse attachment = chatAttachmentService.getChatAttachmentById(attachmentId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", attachment);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Xóa attachment khỏi chat
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Map<String, Object>> deleteChatAttachment(@PathVariable Long attachmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            chatAttachmentService.deleteChatAttachment(attachmentId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Xóa attachment thành công");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Lấy URL preview của attachment
     */
    @GetMapping("/{attachmentId}/preview")
    public ResponseEntity<Map<String, Object>> getChatAttachmentPreviewUrl(@PathVariable Long attachmentId) {
        try {
            String previewUrl = chatAttachmentService.getChatAttachmentPreviewUrl(attachmentId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("previewUrl", previewUrl);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Download attachment file
     */
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<?> downloadChatAttachment(@PathVariable Long attachmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            FileUploadResponse attachment = chatAttachmentService.getChatAttachmentById(attachmentId);
            
            // Kiểm tra quyền truy cập (có thể thêm logic kiểm tra room membership)
            
            // Redirect đến URL thực tế của file
            if (attachment.isCloudStorage()) {
                // Cloudinary - redirect đến URL
                return ResponseEntity.status(302)
                        .header("Location", attachment.getDownloadUrl())
                        .build();
            } else {
                // Local storage - redirect đến static file URL
                return ResponseEntity.status(302)
                        .header("Location", "/files/" + attachment.getFilePath())
                        .build();
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
