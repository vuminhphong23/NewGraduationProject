# 🚀 Concurrency File Upload & Processing Guide

## 📋 Tổng quan

Dự án đã được cải tiến để hỗ trợ **concurrency** trong File Upload & Processing, giúp tăng hiệu suất và cải thiện trải nghiệm người dùng.

## 🔧 Cấu hình đã thêm

### 1. **Thread Pool Configuration** (`application.properties`)
```properties
# File Processing Thread Pool
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=20
spring.task.execution.pool.queue-capacity=100
spring.task.execution.pool.keep-alive=60s
spring.task.execution.thread-name-prefix=file-processing-

# File Upload Thread Pool
file.upload.thread-pool.core-size=3
file.upload.thread-pool.max-size=10
file.upload.thread-pool.queue-capacity=50
file.upload.thread-pool.keep-alive=30s

# Batch Processing Thread Pool
file.batch.thread-pool.core-size=4
file.batch.thread-pool.max-size=15
file.batch.thread-pool.queue-capacity=75
file.batch.thread-pool.keep-alive=45s

# Enable Async Processing
spring.task.execution.async.enabled=true
```

### 2. **AsyncConfig** (`src/main/java/GraduationProject/forumikaa/config/AsyncConfig.java`)
- Cấu hình thread pools cho file upload và batch processing
- Quản lý tài nguyên thread hiệu quả

### 3. **AsyncFileUploadService** 
- Interface: `AsyncFileUploadService.java`
- Implementation: `AsyncFileUploadServiceImpl.java`
- Cung cấp các method async cho file operations

## 🎯 API Endpoints mới

### **Synchronous APIs** (giữ nguyên)
- `POST /api/files/upload` - Upload file đơn lẻ
- `POST /api/files/upload-multiple` - Upload nhiều file tuần tự
- `GET /api/files/download/{fileId}` - Download file
- `GET /api/files/download-all/{postId}` - Download tất cả file dưới dạng ZIP
- `DELETE /api/files/{fileId}` - Xóa file

### **Asynchronous APIs** (mới)
- `POST /api/files/upload-async` - Upload file đơn lẻ bất đồng bộ
- `POST /api/files/upload-multiple-async` - Upload nhiều file song song
- `GET /api/files/download-async/{fileId}` - Download file bất đồng bộ
- `GET /api/files/download-all-async/{postId}` - Download ZIP bất đồng bộ
- `DELETE /api/files/async/{fileId}` - Xóa file bất đồng bộ

## 💡 Cách sử dụng

### **1. Upload file đơn lẻ bất đồng bộ**
```javascript
// Frontend
const formData = new FormData();
formData.append('file', file);
formData.append('postId', postId);

const response = await fetch('/api/files/upload-async', {
    method: 'POST',
    body: formData
});

const result = await response.json();
```

### **2. Upload nhiều file song song**
```javascript
// Frontend
const formData = new FormData();
files.forEach(file => formData.append('files', file));
formData.append('postId', postId);

const response = await fetch('/api/files/upload-multiple-async', {
    method: 'POST',
    body: formData
});

const results = await response.json();
```

### **3. Download file bất đồng bộ**
```javascript
// Frontend
const response = await fetch(`/api/files/download-async/${fileId}`);
const blob = await response.blob();

// Tạo download link
const url = window.URL.createObjectURL(blob);
const a = document.createElement('a');
a.href = url;
a.download = 'filename';
a.click();
```

## 🔄 Cải tiến LocalStorageStrategy

### **Async Methods được thêm:**
- `uploadFileAsync()` - Upload file bất đồng bộ
- `uploadMultipleFilesAsync()` - Upload nhiều file song song

### **Thread Pool Usage:**
- `@Async("fileUploadExecutor")` - Cho single file upload
- `@Async("fileBatchExecutor")` - Cho batch operations

## 📊 Lợi ích

### **1. Hiệu suất**
- ✅ Upload nhiều file song song thay vì tuần tự
- ✅ Không block UI thread
- ✅ Tối ưu tài nguyên với thread pools

### **2. Trải nghiệm người dùng**
- ✅ Phản hồi nhanh hơn
- ✅ Có thể upload nhiều file cùng lúc
- ✅ Progress tracking dễ dàng hơn

### **3. Khả năng mở rộng**
- ✅ Dễ dàng scale theo nhu cầu
- ✅ Cấu hình thread pool linh hoạt
- ✅ Error handling tốt hơn

## ⚙️ Cấu hình Thread Pool

### **File Upload Executor**
- Core threads: 3
- Max threads: 10
- Queue capacity: 50
- Keep alive: 30s

### **File Batch Executor**
- Core threads: 4
- Max threads: 15
- Queue capacity: 75
- Keep alive: 45s

## 🚨 Lưu ý quan trọng

1. **Backward Compatibility**: Tất cả API cũ vẫn hoạt động bình thường
2. **Error Handling**: Async APIs có error handling tốt hơn
3. **Resource Management**: Thread pools được quản lý tự động
4. **Database Transactions**: Vẫn đảm bảo tính toàn vẹn dữ liệu

## 🔧 Tùy chỉnh

### **Thay đổi Thread Pool Size**
```properties
# Trong application.properties
file.upload.thread-pool.core-size=5
file.upload.thread-pool.max-size=15
file.batch.thread-pool.core-size=6
file.batch.thread-pool.max-size=20
```

### **Thêm Async Method mới**
```java
@Async("fileUploadExecutor")
public CompletableFuture<CustomResponse> customAsyncMethod() {
    // Implementation
}
```

## 📈 Monitoring

### **Thread Pool Metrics**
- Số lượng thread đang chạy
- Queue size
- Rejected tasks
- Execution time

### **File Upload Metrics**
- Upload success rate
- Average upload time
- Concurrent uploads
- Error rate

---

**🎉 Chúc mừng! Bạn đã thành công áp dụng concurrency vào File Upload & Processing!**
