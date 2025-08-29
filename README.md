# ⚙️ Globy Backend - API và Dịch vụ cho Nền tảng Du lịch

Đây là dự án backend cho Globy, cung cấp toàn bộ API và logic nghiệp vụ cần thiết cho trang web du lịch. Dự án được xây dựng trên nền tảng Java với Spring Boot và MongoDB, được thiết kế để có hiệu suất cao, dễ dàng mở rộng và tích hợp các công nghệ AI tiên tiến.

**API phục vụ cho trang web:** [https://www.globy-travel.website/](https://www.globy-travel.website/)

## ✨ Tính năng cốt lõi

-   **Quản lý RESTful API:** Cung cấp đầy đủ các endpoint cho việc quản lý Tour, Điểm đến, Người dùng, Booking, Đánh giá và Bài viết Blog.
-   **Tìm kiếm Thông minh với AI:**
    -   **Tìm kiếm Ngữ nghĩa (Semantic Search):** Sử dụng Google Gemini `text-embedding-004` để chuyển đổi dữ liệu tour thành vector, cho phép tìm kiếm dựa trên ý nghĩa và ngữ cảnh thay vì chỉ từ khóa khớp chính xác.
    -   **Chatbot Tư vấn:** Tích hợp Google Gemini API để phân tích yêu cầu của người dùng, trích xuất thông tin (địa điểm, giá, loại hình tour) và đưa ra gợi ý tour phù hợp nhất.
-   **Hệ thống Xác thực & Phân quyền:**
    -   Đăng ký/Đăng nhập bằng số điện thoại (sử dụng Spring Security với BCrypt).
    -   Đăng nhập bằng Google (OAuth2).
    -   Phân quyền cho Admin và người dùng thường.
-   **Quản lý Booking & Thanh toán:**
    -   Xử lý logic đặt tour, duyệt và từ chối yêu cầu.
    -   Tích hợp cổng thanh toán **PayOS** và xử lý webhook để xác nhận thanh toán tự động.
-   **Hệ thống Thông báo:** Tự động gửi thông báo cho admin khi có booking mới và cho người dùng khi trạng thái booking thay đổi.
-   **Dashboard cho Admin:** Cung cấp API thống kê dữ liệu về doanh thu, số lượng booking, người dùng mới để hiển thị trên trang quản trị.

## 🛠️ Công nghệ sử dụng

-   **Ngôn ngữ & Framework:** Java 17, Spring Boot 3
-   **Cơ sở dữ liệu:** MongoDB (sử dụng Spring Data MongoDB)
-   **Bảo mật:** Spring Security (JWT cho xác thực Admin)
-   **AI & Machine Learning:**
    -   Google Generative AI (Gemini) cho Chatbot và tạo Embedding.
    -   Dịch vụ Embedding riêng biệt viết bằng Python (Flask).
-   **Thanh toán:** Tích hợp API của PayOS
-   **Deployment:** Cấu hình sẵn với Dockerfile để dễ dàng triển khai.

## 🚀 Cài đặt và Chạy dự án

1.  **Yêu cầu:**
    -   Java JDK 17 hoặc cao hơn.
    -   Maven 3.8+.
    -   MongoDB instance (có thể là local hoặc trên Atlas).
    -   Python 3.x và pip (cho dịch vụ embedding).

2.  **Clone a repository:**
    ```bash
    git clone <URL_CUA_REPOSITORY_CUA_BAN>
    cd <TEN_THU_MUC_BACKEND>
    ```

3.  **Cấu hình môi trường:**
    Mở file `src/main/resources/application.properties` và cấu hình các thông tin sau:
    -   `spring.data.mongodb.uri`: Chuỗi kết nối đến MongoDB của bạn.
    -   `GOOGLE_CLIENT_ID`: Google Client ID cho chức năng đăng nhập.
    -   `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`: Các khóa API từ PayOS.
    -   *(Quan trọng)* Đặt biến môi trường `GOOGLE_API_KEY` cho dịch vụ embedding.

4.  **Chạy dịch vụ Embedding (Python):**
    ```bash
    # Di chuyển vào thư mục chứa file python
    cd <THU_MUC_CHUA_FILE_PYTHON> 
    
    # Cài đặt thư viện
    pip install -r requirements.txt
    
    # Chạy dịch vụ (đảm bảo đã set biến môi trường GOOGLE_API_KEY)
    python embedding_service.py
    ```
    Dịch vụ này sẽ chạy mặc định ở cổng `5001`.

5.  **Chạy ứng dụng Spring Boot:**
    ```bash
    ./mvnw spring-boot:run
    ```
    Ứng dụng backend sẽ khởi chạy mặc định ở cổng `4000`.
