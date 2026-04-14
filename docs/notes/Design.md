# 📐 BẢN THIẾT KẾ CHUYỂN ĐỔI MICROSERVICES (TECHSTORE)

## Bước 1: Xác định Ranh giới & Bóc tách Database (Database Decomposition)

> **Triết lý:** "Database per Service" - Mỗi service sở hữu một Database riêng biệt. Các service tuyệt đối không được chọc trực tiếp vào DB của nhau.

* **(Đã hoàn thành) Nguyên tắc No-Foreign-Key:** Rất tuyệt vời là ở giai đoạn Monolith, bạn đã thiết kế các bảng lưu ID dưới dạng số (VD: `cart_items` lưu `productId` kiểu `BIGINT` chứ không tạo khóa ngoại ràng buộc cứng với bảng `products`). Điều này giúp việc "chặt" Database bây giờ cực kỳ dễ dàng.

**Phân bổ Database Schema (MySQL):**
Sẽ có 4 Schema (hoặc 4 DB riêng) chạy chung trên 1 server MySQL:
* **`techstore_identity`** (Identity Service - Port `8081`): Chứa bảng `users`, `roles`.
* **`techstore_product`** (Product Service - Port `8082`): Chứa bảng `products`, `categories`.
* **`techstore_cart`** (Cart Service - Port `8083`): Chứa bảng `carts`, `cart_items`.
* **`techstore_order`** (Order Service - Port `8084`): Chứa bảng `orders`, `order_items`.

---

## Bước 2: Thiết kế Hợp đồng Giao tiếp (API Contract Design)

> **Triết lý:** Khi bị tách ra, các service sẽ nói chuyện với nhau qua HTTP REST API bằng OpenFeign.

Chúng ta cần quy hoạch một không gian API nội bộ (chứa prefix `/api/internal/...`). API Gateway sẽ cấu hình **CHẶN** tất cả các request từ React (Frontend) gọi vào đường dẫn internal này, chỉ cho phép các Microservices gọi cho nhau.

**Các Hợp đồng (Contracts) cần xây dựng:**

1.  **Luồng Thanh toán (Order Service là trung tâm):**
    * **Order cần lấy Giỏ hàng:**
        * *Cart Service mở API:* `GET /api/internal/carts/users/{userId}`
        * *Order Service gọi:* Trả về list sản phẩm khách muốn mua.
    * **Order cần trừ tồn kho:**
        * *Product Service mở API:* `POST /api/internal/products/reduce-stock` (Body: List các `{productId, quantity}`)
        * *Order Service gọi:* Báo kho trừ hàng đi.
    * **Order cần xóa Giỏ hàng** (Sau khi tạo đơn thành công):
        * *Cart Service mở API:* `DELETE /api/internal/carts/users/{userId}/clear`

2.  **Luồng Hủy Đơn / Hoàn Kho:**
    * **Order bị Hủy (Admin cancel):**
        * *Product Service mở API:* `POST /api/internal/products/increase-stock` (Body: List `{productId, quantity}`)
        * *Order Service gọi:* Báo kho cộng trả lại hàng.

---

## Bước 3: Xử lý bài toán Phân tán (Distributed Transactions)

> **Triết lý:** `@Transactional` của Spring Data JPA giờ đây bị "phế võ công" nếu gọi liên service. Nếu Order gọi Product trừ kho xong, nhưng lúc Order lưu DB lại bị sập, Product không thể tự Rollback được.

**Chiến lược cho Giai đoạn 1 (Manual Compensation - Hoàn tác thủ công):**
Thay vì đâm đầu ngay vào Saga Pattern hay Kafka (rất phức tạp), ta sẽ dùng kỹ thuật Bắt lỗi và Hoàn tác thủ công (try-catch bù trừ) trong `OrderService`.

**Kịch bản code giả lập khi Khách bấm Đặt hàng:**

```java
// 1. Bắt đầu @Transactional (chỉ có tác dụng với bảng Order).
// 2. CartFeignClient.getCart(userId) -> Lấy danh sách hàng.
// 3. ProductFeignClient.reduceStock(...) -> Báo kho trừ đi.

try {
    // 4. Lưu đơn hàng vào DB.
    orderRepository.save(order);
} catch (Exception e) {
    // 5. (Lỗi xảy ra, ví dụ sập DB Order)
    // CỰC KỲ QUAN TRỌNG: Gọi ngay API hoàn kho để trả lại hàng đã trừ ở bước 3.
    ProductFeignClient.increaseStock(...);
    
    // 6. Báo lỗi cho Frontend.
    throw e;
}

```
## Bước 4: Quy hoạch Hạ tầng (Infrastructure & Docker)

> **Triết lý:** Dùng Docker Compose để "1 click" khởi chạy toàn bộ khu đô thị này. Đừng quên bạn đã thành thạo Docker từ lúc đóng gói Spring Boot + MySQL rồi, giờ chúng ta chỉ nâng cấp lên quy mô nhiều container hơn.

### 1. Hạ tầng lõi (Core Infrastructure)
Đây là các dịch vụ nền tảng, phải chạy lên trước để các dịch vụ nghiệp vụ có chỗ "tựa" vào:

* **`mysql-server` (Port 3306):** * **Nhiệm vụ:** Chạy một thực thể MySQL duy nhất nhưng chứa 4 database schema độc lập (đã xác định ở Bước 1).
* **`eureka-server` (Port 8761):** * **Nhiệm vụ:** Đóng vai trò "Sổ danh bạ". Tất cả các service khi boot lên sẽ tìm đến đây để điểm danh (Register) và cập nhật trạng thái hoạt động.
* **`api-gateway` (Port 8080):** * **Nhiệm vụ:** Đứng mũi chịu sào. Đây là cổng duy nhất tiếp nhận request từ React (localhost:8080). Nó chịu trách nhiệm định tuyến (Routing), kiểm tra JWT Token và đảm bảo bảo mật cho toàn hệ thống.

### 2. Các Service Nghiệp vụ (Business Services)
Các service này chứa logic xử lý chính của cửa hàng TechStore:

* **`identity-service`:** Quản lý người dùng, phân quyền và cấp phát Token.
* **`product-service`:** Quản lý kho hàng, danh mục và thông tin sản phẩm.
* **`cart-service`:** Quản lý giỏ hàng tạm thời của khách.
* **`order-service`:** Xử lý quy trình đặt hàng và thanh toán.

---

### ⚠️ Lưu ý quan trọng về kết nối mạng (Networking)
Trong mô hình này, chúng ta áp dụng quy tắc bảo mật nội bộ:

1.  **Chế độ chạy ngầm:** Các Service nghiệp vụ (Identity, Product, Cart, Order) sẽ **không** publish port ra ngoài máy host (Internal only).
2.  **Truy cập duy nhất:** Bạn sẽ không thể truy cập trực tiếp qua trình duyệt bằng `localhost:8082` hay `localhost:8084`.
3.  **Luồng đi hợp lệ:** Mọi yêu cầu bắt buộc phải đi qua Gateway:
    * *Đúng:* `localhost:8080/api/products/...`
    * *Sai:* `localhost:8082/products/...` (Sẽ bị từ chối kết nối).

Điều này giúp bảo vệ các API nội bộ (Internal API) khỏi các cuộc tấn công trực tiếp từ bên ngoài.