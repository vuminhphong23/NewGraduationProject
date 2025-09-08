# 🕷️ ADMIN CRAWLING MANAGEMENT SYSTEM

## 🎯 TỔNG QUAN

Hệ thống quản lý Web Crawling linh hoạt cho phép admin cấu hình và quản lý các nguồn crawling một cách dễ dàng thông qua giao diện web.

## ✨ TÍNH NĂNG CHÍNH

### **1. Dynamic Configuration**
- **Cấu hình linh hoạt**: Admin có thể thêm/sửa/xóa configs
- **CSS Selectors**: Tùy chỉnh selectors cho từng website
- **Multiple Sources**: Hỗ trợ nhiều nguồn crawling khác nhau
- **Real-time Management**: Quản lý trực tiếp qua web interface

### **2. Advanced Selectors**
- **Title Selector**: Lấy tiêu đề bài viết
- **Content Selector**: Lấy nội dung
- **Link Selector**: Lấy link gốc
- **Image Selector**: Lấy hình ảnh
- **Author Selector**: Lấy tác giả
- **Date Selector**: Lấy ngày đăng
- **Topic Selector**: Lấy chủ đề động

### **3. Smart Scheduling**
- **Custom Intervals**: Tùy chỉnh thời gian crawl (5-1440 phút)
- **Auto Crawling**: Tự động crawl theo lịch
- **Manual Trigger**: Crawl ngay lập tức
- **Error Handling**: Xử lý lỗi thông minh

### **4. Monitoring & Analytics**
- **Real-time Stats**: Thống kê trực tiếp
- **Success/Error Tracking**: Theo dõi kết quả
- **Performance Metrics**: Đo lường hiệu suất
- **Log Management**: Quản lý logs

## 🏗️ KIẾN TRÚC HỆ THỐNG

### **Backend Components**
```
CrawlingConfig (Entity)
├── CrawlingConfigDao (Data Access)
├── CrawlingConfigService (Business Logic)
├── DynamicCrawlingService (Crawling Engine)
└── AdminCrawlingController (REST API)
```

### **Frontend Components**
```
Admin Template
├── Statistics Dashboard
├── Config Management
├── Real-time Monitoring
└── Test Interface
```

## 📊 CẤU TRÚC DỮ LIỆU

### **CrawlingConfig Entity**
```java
- id: Long
- name: String (Tên config)
- description: String (Mô tả)
- baseUrl: String (URL nguồn)
- titleSelector: String (CSS selector cho title)
- contentSelector: String (CSS selector cho content)
- linkSelector: String (CSS selector cho link)
- imageSelector: String (CSS selector cho image)
- authorSelector: String (CSS selector cho author)
- dateSelector: String (CSS selector cho date)
- topicName: String (Tên topic cố định)
- topicSelector: String (CSS selector cho topic)
- maxPosts: Integer (Số posts tối đa)
- intervalMinutes: Integer (Khoảng thời gian crawl)
- enabled: Boolean (Trạng thái kích hoạt)
- userAgent: String (User agent)
- timeout: Integer (Timeout)
- additionalHeaders: String (Headers bổ sung)
- status: String (ACTIVE/ERROR/INACTIVE)
- statistics: (totalCrawled, successCount, errorCount)
```

## 🚀 CÁCH SỬ DỤNG

### **1. Truy cập Admin Panel**
```
URL: http://localhost:8080/api/admin/crawling/manage
```

### **2. Tạo Config Mới**
1. Click "Thêm Config"
2. Điền thông tin cơ bản:
   - **Tên Config**: Tên mô tả
   - **URL nguồn**: Website cần crawl
   - **Topic Name**: Chủ đề cố định
3. Cấu hình CSS Selectors:
   - **Title Selector**: `h3 a`, `.title`, `article h2`
   - **Content Selector**: `.content`, `p`, `.description`
   - **Link Selector**: `a`, `.link`
   - **Image Selector**: `img`, `.image`
4. Thiết lập thông số:
   - **Max Posts**: 1-100
   - **Interval**: 5-1440 phút
   - **Timeout**: 1000-60000ms
5. Click "Lưu Config"

### **3. Quản lý Configs**
- **Edit**: Chỉnh sửa config
- **Test**: Test config trước khi chạy
- **Crawl**: Crawl ngay lập tức
- **Toggle**: Bật/tắt config
- **Delete**: Xóa config

### **4. Monitoring**
- **Statistics**: Xem thống kê tổng quan
- **Real-time**: Cập nhật real-time
- **Error Tracking**: Theo dõi lỗi
- **Performance**: Đo lường hiệu suất

## 🔧 API ENDPOINTS

### **Config Management**
```bash
# Lấy tất cả configs
GET /api/admin/crawling/configs

# Lấy configs đang active
GET /api/admin/crawling/configs/active

# Lấy config theo ID
GET /api/admin/crawling/configs/{id}

# Tạo config mới
POST /api/admin/crawling/configs

# Cập nhật config
PUT /api/admin/crawling/configs/{id}

# Xóa config
DELETE /api/admin/crawling/configs/{id}

# Toggle enable/disable
POST /api/admin/crawling/configs/{id}/toggle
```

### **Crawling Operations**
```bash
# Test config
POST /api/admin/crawling/configs/{id}/test

# Crawl config ngay lập tức
POST /api/admin/crawling/configs/{id}/crawl

# Crawl tất cả configs active
POST /api/admin/crawling/crawl-all
```

### **Statistics**
```bash
# Lấy thống kê
GET /api/admin/crawling/statistics
```

## 📝 VÍ DỤ CẤU HÌNH

### **1. VnExpress Education**
```json
{
  "name": "VnExpress Education",
  "baseUrl": "https://vnexpress.net/giao-duc",
  "titleSelector": "h3 a",
  "contentSelector": "p",
  "linkSelector": "h3 a",
  "topicName": "Giáo dục",
  "maxPosts": 10,
  "intervalMinutes": 60
}
```

### **2. Reddit College**
```json
{
  "name": "Reddit College",
  "baseUrl": "https://www.reddit.com/r/college/",
  "titleSelector": "h3[data-testid='post-title']",
  "contentSelector": "[data-testid='post-content']",
  "linkSelector": "a[data-testid='post-title']",
  "topicName": "College Life",
  "maxPosts": 15,
  "intervalMinutes": 30
}
```

### **3. arXiv Papers**
```json
{
  "name": "arXiv AI Papers",
  "baseUrl": "https://arxiv.org/list/cs.AI/recent",
  "titleSelector": "div.list-title",
  "contentSelector": "p.mathjax",
  "authorSelector": "div.list-authors",
  "topicName": "Trí tuệ nhân tạo",
  "maxPosts": 5,
  "intervalMinutes": 1440
}
```

## 🎨 GIAO DIỆN ADMIN

### **Dashboard**
- **Statistics Cards**: Tổng configs, active, errors, posts crawled
- **Config List**: Danh sách configs với thông tin chi tiết
- **Action Buttons**: Edit, Test, Crawl, Toggle, Delete

### **Config Form**
- **Basic Info**: Tên, mô tả, URL, topic
- **CSS Selectors**: Tất cả selectors cần thiết
- **Settings**: Max posts, interval, timeout, user agent
- **Advanced**: Headers, processing rules

### **Test Interface**
- **Real-time Testing**: Test config trước khi lưu
- **Sample Results**: Hiển thị kết quả mẫu
- **Error Reporting**: Báo cáo lỗi chi tiết

## ⚙️ CẤU HÌNH NÂNG CAO

### **Additional Headers**
```
Accept: text/html
Accept-Language: en-US,en;q=0.9
Accept-Encoding: gzip, deflate
Cache-Control: no-cache
```

### **User Agents**
```
Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36
Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36
```

### **CSS Selectors Examples**
```css
/* Title selectors */
h1, h2, h3
.title, .headline
article h2
[data-testid='post-title']

/* Content selectors */
p, .content, .description
article .text
[data-testid='post-content']

/* Link selectors */
a, .link
h3 a
[href*='article']

/* Image selectors */
img, .image
.thumbnail img
[src*='image']
```

## 🔍 TROUBLESHOOTING

### **Common Issues**
1. **Selector không hoạt động**: Kiểm tra CSS selector syntax
2. **Timeout errors**: Tăng timeout hoặc kiểm tra network
3. **Empty results**: Kiểm tra website structure
4. **Rate limiting**: Giảm frequency hoặc thêm delays

### **Debug Tips**
1. Sử dụng **Test** function trước khi lưu
2. Kiểm tra **Browser Developer Tools** để xem HTML structure
3. Sử dụng **CSS selector tester** online
4. Kiểm tra **Network tab** để xem requests

## 🚀 DEPLOYMENT

### **1. Database Migration**
```sql
-- Tạo bảng crawling_configs
CREATE TABLE crawling_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    base_url VARCHAR(500) NOT NULL,
    title_selector VARCHAR(200) NOT NULL,
    content_selector VARCHAR(200),
    -- ... other fields
);
```

### **2. Application Properties**
```properties
# Crawling Configuration
crawling.enabled=true
crawling.default-timeout=10000
crawling.max-concurrent=5
```

### **3. Security**
- Chỉ admin mới có thể truy cập
- Validate input data
- Rate limiting cho API calls
- Error handling và logging

## 📈 MONITORING

### **Metrics to Track**
- **Config Performance**: Success rate, error rate
- **Crawling Volume**: Posts per hour/day
- **Resource Usage**: CPU, memory, network
- **Error Patterns**: Common failures

### **Alerts**
- **High Error Rate**: > 50% errors
- **No Crawling**: No posts in 24h
- **Resource Issues**: High CPU/memory usage
- **Config Failures**: Invalid selectors

---

**🎉 Admin Crawling Management System đã sẵn sàng!**

Bây giờ admin có thể dễ dàng quản lý và cấu hình các nguồn crawling một cách linh hoạt và hiệu quả!


