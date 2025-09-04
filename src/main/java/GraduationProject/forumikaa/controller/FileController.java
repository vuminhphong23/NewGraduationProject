package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.service.FileUploadService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;
    private final SecurityUtil securityUtil;

    public FileController(FileUploadService fileUploadService, SecurityUtil securityUtil) {
        this.fileUploadService = fileUploadService;
        this.securityUtil = securityUtil;
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("postId") Long postId) throws IOException {
        
        Long userId = securityUtil.getCurrentUserId();
        List<FileUploadResponse> responses = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                FileUploadResponse response = fileUploadService.uploadFile(file, postId, userId);
                responses.add(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<FileUploadResponse>> getFilesByPostId(@PathVariable Long postId) {
        List<FileUploadResponse> files = fileUploadService.getFilesByPostId(postId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        Long userId = securityUtil.getCurrentUserId();
        fileUploadService.deleteFile(fileId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long fileId) throws IOException {
        FileUploadResponse fileInfo = fileUploadService.getFileById(fileId);
        byte[] fileContent = fileUploadService.downloadFile(fileId);
        
        ByteArrayResource resource = new ByteArrayResource(fileContent);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileInfo.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(fileInfo.getMimeType()))
                .contentLength(fileContent.length)
                .body(resource);
    }

    @GetMapping("/download-all/{postId}")
    public ResponseEntity<ByteArrayResource> downloadAllFilesAsZip(@PathVariable Long postId) throws IOException {
        Long userId = securityUtil.getCurrentUserId();
        byte[] zipContent = fileUploadService.downloadAllFilesAsZip(postId, userId);
        
        ByteArrayResource resource = new ByteArrayResource(zipContent);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"post_" + postId + "_files.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipContent.length)
                .body(resource);
    }
}
