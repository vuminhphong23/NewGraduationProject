# Chức Năng Đính Kèm File cho Bài Post

## Tổng Quan
Chức năng này cho phép người dùng đính kèm file (ảnh, video, tài liệu) vào bài post. File sẽ được lưu trữ trên thư mục local và thông tin được lưu vào database.

## Tính Năng

### 1. Hỗ Trợ File Types
- **Ảnh**: JPEG, PNG, GIF, WebP
- **Video**: MP4, AVI, MOV, WMV  
- **Tài liệu**: PDF, Word, Excel, PowerPoint, TXT

### 2. Giới Hạn
- Kích thước file tối đa: 10MB
- Số lượng file không giới hạn (có thể upload nhiều file cùng lúc)

### 3. Giao Diện
- Drag & Drop file vào khu vực upload
- Preview file trước khi đăng
- Hiển thị file đính kèm trong bài post
- Nút tải về và xem file

## Cấu Trúc Database

### Entity Document
```java
@Entity
@Table(name = "documents")
public class Document {
    private Long id;
    private String fileName;           // Tên file unique
    private String originalFileName;   // Tên file gốc
    private String fileType;           // Extension file
    private String filePath;           // Đường dẫn lưu trữ
    private Long fileSize;             // Kích thước file
    private String mimeType;           // MIME type
    private Post post;                 // Bài post chứa file
    private User user;                 // User upload file
    private LocalDateTime uploadedAt;  // Thời gian upload
    private Boolean isDeleted;         // Soft delete flag
}
```

### Mối Quan Hệ
- `Post` ↔ `Document`: One-to-Many
- `User` ↔ `Document`: One-to-Many

## API Endpoints

### Upload File
```
POST /api/files/upload
POST /api/files/upload-multiple
```

### Quản Lý File
```
GET /api/files/post/{postId}     // Lấy file theo post
GET /api/files/{fileId}          // Lấy thông tin file
DELETE /api/files/{fileId}       // Xóa file
```

### Download & Preview
```
GET /api/files/download/{fileId} // Tải file
GET /api/files/preview/{fileId}  // Lấy URL preview
```

## Cấu Hình

### application.properties
```properties
# File Storage
app.file.storage.strategy=local
app.file.upload.local.path=uploads
app.file.upload.local.url-prefix=/files

# File Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

### Thư Mục Upload
- Thư mục gốc: `uploads/`
- Cấu trúc: `uploads/YYYY/MM/filename.ext`
- URL access: `/files/YYYY/MM/filename.ext`

## Sử Dụng

### 1. Upload File
1. Click vào nút "Ảnh/Video" hoặc "Tài liệu" trong modal đăng bài
2. Chọn file hoặc kéo thả file vào khu vực upload
3. File sẽ được preview và validate
4. Đăng bài, file sẽ tự động được upload

### 2. Xem File
- File ảnh/video: Hiển thị trực tiếp trong post
- File tài liệu: Hiển thị icon và thông tin
- Click nút "Xem" để mở file trong tab mới
- Click nút "Tải về" để download file

### 3. Quản Lý File
- Chỉ user upload file hoặc chủ bài post mới có thể xóa
- File được soft delete (không xóa thực tế khỏi server)

## Bảo Mật

### Validation
- Kiểm tra file type (MIME type)
- Kiểm tra kích thước file
- Kiểm tra quyền xóa file

### Access Control
- File chỉ có thể được truy cập bởi user có quyền xem post
- Download link có thể được share nhưng cần authentication

## Performance

### Optimization
- File được lưu theo cấu trúc thư mục theo thời gian
- Sử dụng UUID để tránh conflict tên file
- Soft delete để tránh mất dữ liệu

### Caching
- File preview URL được cache
- Thông tin file được load cùng với post

## Troubleshooting

### Lỗi Thường Gặp
1. **File quá lớn**: Kiểm tra giới hạn 10MB
2. **File type không hỗ trợ**: Kiểm tra danh sách MIME type được phép
3. **Không có quyền xóa**: Chỉ chủ file hoặc chủ post mới xóa được

### Debug
- Kiểm tra log server để xem lỗi upload
- Kiểm tra quyền thư mục uploads
- Kiểm tra cấu hình database connection

## Tương Lai

### Tính Năng Dự Kiến
- Upload lên cloud storage (AWS S3, Google Cloud)
- Compression ảnh tự động
- Watermark cho ảnh
- Thumbnail generation cho video
- OCR cho tài liệu PDF
- Virus scanning cho file upload



