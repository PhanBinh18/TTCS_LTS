## 1. Hạ tầng cốt lõi (Infrastructure)

Thay vì một cục code chạy lên là xong, hệ thống của bạn giờ sẽ có các "nhân viên" hỗ trợ hạ tầng:

* **Service Registry (Cuốn danh bạ):** Spring Cloud Netflix Eureka
    * **Nhiệm vụ:** Là nơi tất cả các Microservices (Product, Order...) báo danh khi khởi động. Nó giúp các service biết nhau đang ở đâu (IP, Port) để gọi điện mà không cần code cứng (hard-code) địa chỉ mạng.
* **API Gateway (Người gác cổng / Proxy):** Spring Cloud Gateway
    * **Nhiệm vụ:** Đứng ở ngoài cùng, hứng toàn bộ request từ React. Dựa vào URL (ví dụ: `/api/products`), nó sẽ tra "danh bạ" Eureka và "đá" request về đúng service đang quản lý. Đây cũng là nơi lý tưởng để cấu hình CORS và chặn bắt JWT Token.
* **Môi trường chạy (Container hóa):** Docker & Docker Compose
    * **Nhiệm vụ:** Tránh việc phải chạy 4-5 cái project Spring Boot cùng một lúc bằng tay rất vất vả. Ta sẽ đóng gói mỗi service thành 1 image và dùng Docker Compose để "Bấm 1 nút là lên cả hệ thống" (bao gồm cả MySQL).

---

## 2. Kỹ thuật giao tiếp nội bộ (Inter-service Communication)

Vì các module đã bị tách ra các server khác nhau, chúng không thể gọi hàm của nhau trực tiếp nữa.

* **Gọi API nội bộ (Đồng bộ):** Spring Cloud OpenFeign
    * **Nhiệm vụ:** Khi Order Service muốn trừ kho, nó phải "nhấc máy" gọi cho Product Service. OpenFeign giúp bạn viết code gọi API qua mạng HTTP mà nhìn cứ như đang gọi một hàm Java bình thường, cực kỳ nhàn và sạch sẽ.

---

## 3. Quản lý Dữ liệu (Database Management)

**Database per Service (Một cơ sở dữ liệu cho một Service):**

* **Kiến thức:** Không dùng chung 1 cái schema to đùng nữa. Ta sẽ tạo các schema MySQL độc lập: `ecommerce_identity`, `ecommerce_product`, `ecommerce_order`.
* **Kỹ thuật:** Loại bỏ hoàn toàn khóa ngoại (Foreign Key) giữa các bảng thuộc 2 service khác nhau. (Phần này bạn đã làm xuất sắc ở giai đoạn Monolith rồi, giờ chỉ việc tách ra thôi).

---

## 4. Kiến thức thiết kế (Mindset)

* **Trạng thái độc lập (Stateless):** Đảm bảo không service nào lưu Session. Token JWT là chìa khóa duy nhất để nhận diện người dùng xuyên suốt các service.
* **Tâm lý "Mạng luôn chập chờn":** Khi dùng Feign gọi từ Order sang Product, phải luôn chuẩn bị tâm lý là Product Service có thể bị chết, hoặc mạng bị lag. Cần biết dùng khối `try-catch` cơ bản để bắt lỗi và báo về cho Frontend đàng hoàng, không để hệ thống sụp đổ dây chuyền.

---

## Lộ trình 3 bước thực hành cơ bản

Để đi từ 0 lên Microservices một cách êm ái, lộ trình chuẩn nhất sẽ là:

1.  **Bước 1 - Dựng móng:** Tạo 1 project Spring Boot mới tinh, cài Eureka Server và cho nó chạy lên.
2.  **Bước 2 - Lên khung:** Tạo 1 project API Gateway, cài đặt routing cơ bản và cho nó đăng ký vào Eureka.
3.  **Bước 3 - Tách ruột:** Bắt đầu bưng module Product ra thành 1 service riêng, nối database riêng, đăng ký vào Eureka, và cấu hình để Gateway trỏ request về nó.