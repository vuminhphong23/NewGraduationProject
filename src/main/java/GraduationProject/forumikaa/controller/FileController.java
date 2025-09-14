package GraduationProject.forumikaa.controller;

import GraduationProject.forumikaa.dto.FileUploadResponse;
import GraduationProject.forumikaa.service.FileUploadService;
import GraduationProject.forumikaa.util.SecurityUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploadService fileUploadService;
    private final SecurityUtil securityUtil;

    public FileController(FileUploadService fileUploadService, SecurityUtil securityUtil) {
        this.fileUploadService = fileUploadService;
        this.securityUtil = securityUtil;
    }

    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("postId") Long postId) {
        
        Long userId = securityUtil.getCurrentUserId();
        return fileUploadService.uploadFile(file, postId, userId)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
            });
    }

    @PostMapping("/upload-multiple")
    public CompletableFuture<ResponseEntity<List<FileUploadResponse>>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("postId") Long postId) {
        
        Long userId = securityUtil.getCurrentUserId();
        List<MultipartFile> fileList = Arrays.asList(files);
        
        // Upload files in parallel
        List<CompletableFuture<FileUploadResponse>> uploadFutures = fileList.stream()
            .map(file -> fileUploadService.uploadFile(file, postId, userId))
            .toList();

        // Wait for all uploads to complete
        CompletableFuture<Void> allUploads = CompletableFuture.allOf(
            uploadFutures.toArray(new CompletableFuture[0])
        );

        return allUploads.thenApply(v -> {
            List<FileUploadResponse> responses = uploadFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        }).exceptionally(throwable -> {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        });
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<FileUploadResponse>> getFilesByPostId(@PathVariable Long postId) {
        List<FileUploadResponse> files = fileUploadService.getFilesByPostId(postId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public CompletableFuture<ResponseEntity<Void>> deleteFile(@PathVariable Long fileId) {
        Long userId = securityUtil.getCurrentUserId();
        return fileUploadService.deleteFile(fileId, userId)
            .thenApply(response -> {
                ResponseEntity<Void> result = ResponseEntity.noContent().build();
                return result;
            })
            .exceptionally(throwable -> {
                ResponseEntity<Void> result = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                return result;
            });
    }

    @GetMapping("/download/{fileId}")
    public CompletableFuture<ResponseEntity<ByteArrayResource>> downloadFile(@PathVariable Long fileId) {
        return fileUploadService.downloadFile(fileId)
            .thenApply(fileContent -> {
                try {
                    FileUploadResponse fileInfo = fileUploadService.getFileById(fileId);
                    ByteArrayResource resource = new ByteArrayResource(fileContent);

                    ResponseEntity<ByteArrayResource> result = ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + fileInfo.getOriginalName() + "\"")
                            .contentType(MediaType.parseMediaType(fileInfo.getMimeType()))
                            .contentLength(fileContent.length)
                            .body(resource);
                    return result;
                } catch (Exception e) {
                    ResponseEntity<ByteArrayResource> result = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                    return result;
                }
            })
            .exceptionally(throwable -> {
                ResponseEntity<ByteArrayResource> result = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                return result;
            });
    }

    @PostMapping("/{fileId}/download")
    @ResponseBody
    public ResponseEntity<String> updateDownloadCount(@PathVariable Long fileId) {
        try {
            System.out.println("API called: updateDownloadCount for fileId=" + fileId);
            fileUploadService.incrementDownloadCount(fileId);
            System.out.println("Download count updated successfully for file " + fileId);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            System.err.println("Error updating download count for file " + fileId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }

    @GetMapping("/download-all/{postId}")
    public CompletableFuture<ResponseEntity<ByteArrayResource>> downloadAllFilesAsZip(@PathVariable Long postId) {
        Long userId = securityUtil.getCurrentUserId();
        return fileUploadService.downloadAllFilesAsZip(postId, userId)
            .thenApply(zipContent -> {
                ByteArrayResource resource = new ByteArrayResource(zipContent);
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"post_" + postId + "_files.zip\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(zipContent.length)
                        .body(resource);
            })
            .exceptionally(throwable -> {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            });
    }
}
