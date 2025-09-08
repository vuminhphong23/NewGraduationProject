# 🕷️ CÁC CONFIG CRAWLING MẪU CHUẨN

## 📰 1. VnExpress Education News

### **Thông tin cơ bản:**
- **Tên Config**: VnExpress Education News
- **Mô tả**: Crawl tin tức giáo dục từ VnExpress
- **URL nguồn**: https://vnexpress.net/giao-duc
- **Tên Topic**: Giáo dục
- **Số posts tối đa**: 10
- **Interval**: 60 phút

### **CSS Selectors:**
- **Title Selector**: `h3 a`
- **Content Selector**: `p`
- **Link Selector**: `h3 a`
- **Image Selector**: `img`
- **Author Selector**: `.author`
- **Date Selector**: `.time`

### **Cài đặt nâng cao:**
- **Timeout**: 10000ms
- **User Agent**: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36

---

## 🔥 2. Reddit College Community

### **Thông tin cơ bản:**
- **Tên Config**: Reddit College Community
- **Mô tả**: Crawl bài viết từ r/college trên Reddit
- **URL nguồn**: https://www.reddit.com/r/college/
- **Tên Topic**: College Life
- **Số posts tối đa**: 15
- **Interval**: 30 phút

### **CSS Selectors:**
- **Title Selector**: `h3[data-testid='post-title']`
- **Content Selector**: `[data-testid='post-content']`
- **Link Selector**: `a[data-testid='post-title']`
- **Image Selector**: `img[alt*='image']`
- **Author Selector**: `[data-testid='post_author_link']`
- **Date Selector**: `time`

### **Cài đặt nâng cao:**
- **Timeout**: 15000ms
- **User Agent**: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36

---

## 📚 3. arXiv AI Papers

### **Thông tin cơ bản:**
- **Tên Config**: arXiv AI Papers
- **Mô tả**: Crawl bài báo AI từ arXiv
- **URL nguồn**: https://arxiv.org/list/cs.AI/recent
- **Tên Topic**: Trí tuệ nhân tạo
- **Số posts tối đa**: 5
- **Interval**: 1440 phút (24 giờ)

### **CSS Selectors:**
- **Title Selector**: `div.list-title`
- **Content Selector**: `p.mathjax`
- **Link Selector**: `a[href*='abs']`
- **Image Selector**: `img`
- **Author Selector**: `div.list-authors`
- **Date Selector**: `div.list-dateline`

### **Cài đặt nâng cao:**
- **Timeout**: 20000ms
- **User Agent**: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36

---

## 🎓 4. GitHub Trending Repositories

### **Thông tin cơ bản:**
- **Tên Config**: GitHub Trending Repos
- **Mô tả**: Crawl repositories trending trên GitHub
- **URL nguồn**: https://github.com/trending
- **Tên Topic**: Lập trình
- **Số posts tối đa**: 20
- **Interval**: 120 phút

### **CSS Selectors:**
- **Title Selector**: `h2 a`
- **Content Selector**: `p.text-gray`
- **Link Selector**: `h2 a`
- **Image Selector**: `img.avatar`
- **Author Selector**: `span.text-normal`
- **Date Selector**: `relative-time`

### **Cài đặt nâng cao:**
- **Timeout**: 10000ms
- **User Agent**: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36

---

## 📰 5. TechCrunch News

### **Thông tin cơ bản:**
- **Tên Config**: TechCrunch News
- **Mô tả**: Crawl tin tức công nghệ từ TechCrunch
- **URL nguồn**: https://techcrunch.com/
- **Tên Topic**: Công nghệ
- **Số posts tối đa**: 12
- **Interval**: 90 phút

### **CSS Selectors:**
- **Title Selector**: `h2.post-block__title a`
- **Content Selector**: `div.post-block__content`
- **Link Selector**: `h2.post-block__title a`
- **Image Selector**: `img.post-block__media__image`
- **Author Selector**: `span.river-byline__authors`
- **Date Selector**: `time.river-byline__time`

### **Cài đặt nâng cao:**
- **Timeout**: 12000ms
- **User Agent**: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36

---

## 🔧 CÁCH TẠO CONFIG:

### **Bước 1: Truy cập Admin Panel**
```
http://localhost:8080/admin
```

### **Bước 2: Click "Quản lý Web Crawling"**
- Trong sidebar, click menu "Quản lý Web Crawling"

### **Bước 3: Click "Thêm Config"**
- Click button "Thêm Config" ở góc phải

### **Bước 4: Điền thông tin**
- Copy thông tin từ các mẫu trên
- Paste vào form tương ứng

### **Bước 5: Test Config**
- Click "Test" để kiểm tra config
- Xem kết quả mẫu

### **Bước 6: Lưu và Kích hoạt**
- Click "Lưu Config"
- Config sẽ được tạo và kích hoạt

---

## ⚠️ LƯU Ý QUAN TRỌNG:

### **1. CSS Selectors:**
- Sử dụng **Browser Developer Tools** để kiểm tra
- Test selector trước khi lưu
- Một số website có cấu trúc phức tạp

### **2. Rate Limiting:**
- Không đặt interval quá thấp (< 5 phút)
- Một số website có rate limiting
- Sử dụng User Agent hợp lệ

### **3. Legal & Ethical:**
- Chỉ crawl nội dung công khai
- Tuân thủ robots.txt
- Không crawl quá nhiều

### **4. Testing:**
- Luôn test config trước khi lưu
- Kiểm tra kết quả mẫu
- Điều chỉnh selectors nếu cần

---

**🎉 Chúc bạn tạo config thành công!**


