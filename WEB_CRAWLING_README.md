# 🕷️ WEB CRAWLING SYSTEM - FORUMIKAA PROJECT

## 🎯 TỔNG QUAN

Hệ thống Web Crawling đã được tích hợp thành công vào dự án Forumikaa, cho phép tự động thu thập nội dung từ các nguồn bên ngoài và gợi ý cho người dùng dựa trên sở thích cá nhân.

## 🚀 TÍNH NĂNG CHÍNH

### 1. **Automatic Content Crawling**
- **Tin tức giáo dục** từ VnExpress (mỗi giờ)
- **Trending topics** từ Reddit (mỗi 30 phút)
- **Nội dung học thuật** từ arXiv (mỗi ngày lúc 2:00 AM)

### 2. **Smart Recommendation System**
- **Personalized Content**: Gợi ý dựa trên sở thích cá nhân
- **Trending Content**: Nội dung đang hot
- **Interest-based**: Theo từng chủ đề cụ thể

### 3. **AI-Powered Scoring**
- Sở thích cá nhân (40%)
- Engagement (30%)
- Lịch sử tương tác (20%)
- Thời gian (10%)

## 📁 CÁC FILE ĐÃ TẠO/CẬP NHẬT

### **Backend Services**
- `SystemUserService.java` - Quản lý admin user
- `WebCrawlingService.java` - Service crawling chính
- `CrawledContentRecommendationService.java` - Recommendation system
- `CrawlingController.java` - REST API endpoints

### **Data Access Layer**
- `PostDao.java` - Thêm methods cho crawled content
- `TopicDao.java` - Thêm methods trending topics
- `LikeDao.java` - Thêm methods cho recommendation
- `CommentDao.java` - Thêm methods cho recommendation

### **DTOs**
- `UserResponse.java` - Response cho user info
- `PostResponse.java` - Thêm fields cho crawled content

### **Frontend**
- `recommendations.html` - Trang gợi ý với tabs
- `recommendations.js` - JavaScript cho dynamic loading
- `index.html` - Hiển thị crawled content với badges

### **Configuration**
- `pom.xml` - Thêm dependencies (JSoup, WebFlux, Quartz)
- `application.properties` - Cấu hình crawling
- `ForumikaaApplication.java` - Enable Async processing

## 🔧 CÁCH SỬ DỤNG

### **1. API Endpoints**

#### **Crawling APIs**
```bash
# Bắt đầu crawl tin tức giáo dục
POST /api/crawling/start-education-news

# Bắt đầu crawl trending topics
POST /api/crawling/start-trending-topics

# Bắt đầu crawl nội dung học thuật
POST /api/crawling/start-academic-content
```

#### **Recommendation APIs**
```bash
# Gợi ý nội dung cá nhân
GET /api/recommendations/crawled-content?limit=20

# Gợi ý nội dung trending
GET /api/recommendations/crawled-content/trending?limit=20

# Gợi ý theo chủ đề
GET /api/recommendations/crawled-content/interest/technology?limit=20
```

### **2. Trang Web**

#### **Trang chủ** (`/`)
- Hiển thị crawled content với badges "🤖 AI Content"
- Hiển thị recommendation score
- Phân biệt rõ ràng với user-generated content

#### **Trang Recommendations** (`/recommendations`)
- **Tab Personalized**: Nội dung gợi ý cá nhân
- **Tab Trending**: Nội dung đang hot
- **Tab By Interests**: Nội dung theo chủ đề

## ⚙️ CẤU HÌNH

### **Application Properties**
```properties
# Web Crawling Settings
crawling.enabled=true
crawling.user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
crawling.timeout=10000
crawling.rate-limit=1000

# Scheduler Configuration
spring.task.scheduling.pool.size=5
```

### **Dependencies**
```xml
<!-- Web Crawling Dependencies -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

## 🎨 GIAO DIỆN

### **Crawled Content Indicators**
- **🤖 AI Content** badge cho nội dung được crawl
- **Score: X.X** badge cho điểm recommendation
- **Màu sắc phân biệt**: Xanh dương cho AI content, xanh lá cho score

### **Recommendation Tabs**
- **Personalized**: Nội dung phù hợp với sở thích
- **Trending**: Nội dung đang hot
- **By Interests**: Filter theo chủ đề cụ thể

## 🔄 WORKFLOW

### **1. Crawling Process**
```
1. Scheduler trigger → WebCrawlingService
2. Crawl từ external sources (VnExpress, Reddit, arXiv)
3. Parse và clean data
4. Tạo Post với admin user
5. Lưu vào database
```

### **2. Recommendation Process**
```
1. User truy cập /recommendations
2. JavaScript gọi API endpoints
3. CrawledContentRecommendationService tính toán score
4. Trả về danh sách gợi ý
5. Frontend hiển thị với UI đẹp
```

## 📊 MONITORING

### **Logs**
- Crawling progress và errors
- Recommendation calculations
- API call statistics

### **Console Output**
```
🕷️ Starting education news crawling...
✅ Education news crawling completed. Crawled 15 articles.
🕷️ Starting trending topics crawling...
✅ Trending topics crawling completed. Found 25 topics.
```

## ⚠️ LƯU Ý QUAN TRỌNG

### **Legal & Ethical**
- Tuân thủ robots.txt của các website
- Rate limiting để tránh spam
- Respect terms of service

### **Performance**
- Async processing cho crawling
- Caching cho recommendations
- Database optimization

### **Security**
- Admin user cho crawled content
- Input validation
- Error handling

## 🚀 DEPLOYMENT

### **1. Build Project**
```bash
mvn clean package
```

### **2. Run Application**
```bash
java -jar target/forumikaa-0.0.1-SNAPSHOT.jar
```

### **3. Test Crawling**
```bash
# Test education news crawling
curl -X POST http://localhost:8080/api/crawling/start-education-news

# Test recommendations
curl http://localhost:8080/api/recommendations/crawled-content?limit=10
```

## 🎯 KẾT QUẢ MONG ĐỢI

1. **Tự động thu thập** nội dung chất lượng từ internet
2. **Gợi ý thông minh** dựa trên sở thích cá nhân
3. **Tăng engagement** với nội dung mới
4. **Giảm tải** cho người dùng tạo content
5. **Cải thiện UX** với recommendation system

## 🔮 TƯƠNG LAI

### **Planned Features**
- Crawl từ nhiều nguồn hơn (Twitter, Facebook, LinkedIn)
- Machine Learning cho recommendation
- Real-time crawling với WebSocket
- Content summarization
- Sentiment analysis

### **Scalability**
- Distributed crawling
- Redis caching
- Message queues
- Microservices architecture

---

**🎉 Web Crawling System đã sẵn sàng sử dụng!**


