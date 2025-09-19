package GraduationProject.forumikaa.controller.user;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.service.ChatAttachmentService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for chat file management functionality
 */
@RestController
@RequestMapping("/api/chat/rooms")
@CrossOrigin(origins = "*")
public class ChatFileManagementController {

    @Autowired
    private ChatAttachmentService chatAttachmentService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * Get all media files (images, videos, audios) for a chat room
     */
    @GetMapping("/{roomId}/media")
    public ResponseEntity<?> getRoomMedia(@PathVariable Long roomId) {
        try {
            securityUtil.getCurrentUserId(); // Validate user authentication
            List<FileUploadResponse> mediaFiles = chatAttachmentService.getChatRoomMedia(roomId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", mediaFiles);
            result.put("count", mediaFiles.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get all document files for a chat room
     */
    @GetMapping("/{roomId}/documents")
    public ResponseEntity<?> getRoomDocuments(@PathVariable Long roomId) {
        try {
            securityUtil.getCurrentUserId(); // Validate user authentication
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
     * Get all files for a chat room
     */
    @GetMapping("/{roomId}/files")
    public ResponseEntity<?> getRoomFiles(@PathVariable Long roomId) {
        try {
            securityUtil.getCurrentUserId(); // Validate user authentication
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
     * Get links for a chat room (placeholder for future implementation)
     */
    @GetMapping("/{roomId}/links")
    public ResponseEntity<?> getRoomLinks(@PathVariable Long roomId) {
        try {
            securityUtil.getCurrentUserId(); // Validate user authentication
            // TODO: Implement link extraction from messages
            List<Map<String, Object>> links = List.of(); // Empty for now
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", links);
            result.put("count", links.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get files by type for a chat room
     */
    @GetMapping("/{roomId}/files/{fileType}")
    public ResponseEntity<?> getRoomFilesByType(@PathVariable Long roomId, @PathVariable String fileType) {
        try {
            securityUtil.getCurrentUserId(); // Validate user authentication
            List<FileUploadResponse> files;
            
            switch (fileType.toLowerCase()) {
                case "images":
                    files = chatAttachmentService.getChatRoomImages(roomId);
                    break;
                case "videos":
                    files = chatAttachmentService.getChatRoomVideos(roomId);
                    break;
                case "audios":
                    files = chatAttachmentService.getChatRoomAudios(roomId);
                    break;
                case "documents":
                    files = chatAttachmentService.getChatRoomDocuments(roomId);
                    break;
                default:
                    files = chatAttachmentService.getChatRoomAttachments(roomId);
                    break;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", files);
            result.put("count", files.size());
            result.put("type", fileType);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
