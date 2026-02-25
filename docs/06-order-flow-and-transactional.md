# 📝 Phân tích Luồng dữ liệu (Flow) và @Transactional: Module Order

## 1. Luồng chạy tổng thể của một Request tạo Đơn hàng (Data Flow)



Khi người dùng bấm "Đặt hàng" trên Frontend, dữ liệu sẽ đi qua 3 "trạm kiểm soát" cốt lõi:

1. **Trạm 1: Controller (`OrderController`) - Người tiếp nhận**
    - Đón nhận HTTP Request (`POST /api/orders`).
    - Lấy chuỗi JSON từ body và map tự động thành đối tượng `OrderRequest` (chứa `userId` và danh sách các món hàng gồm `productId`, `quantity`).
    - Giao toàn bộ gói dữ liệu này cho `OrderService` xử lý.

2. **Trạm 2: Service (`OrderService`) - Bộ não nghiệp vụ**
    - **Khởi tạo:** Tạo một đối tượng `Order` mới, gán `userId` và set trạng thái là `CREATED`.
    - **Vòng lặp xử lý từng mặt hàng:** Duyệt qua danh sách các sản phẩm khách muốn mua:
        - *Giao tiếp liên module:* Gọi sang `ProductService.reduceStock()` để trừ số lượng tồn kho và lấy thông tin chi tiết sản phẩm.
        - *Snapshot dữ liệu:* Tạo `OrderItem`. Chép cứng `productName` và `price` tại thời điểm hiện tại vào chi tiết đơn. (Đảm bảo giá trị hóa đơn không bị thay đổi nếu sau này chủ shop đổi giá sản phẩm).
        - *Tính toán:* Nhân giá với số lượng (`subTotal`) và cộng dồn vào tổng tiền (`totalAmount`).
        - Đưa `OrderItem` vào danh sách của `Order`.
    - **Đóng gói:** Gắn tổng tiền vào `Order` và chuyển xuống tầng dưới.

3. **Trạm 3: Repository (`OrderRepository`) - Thủ kho**
    - Nhận đối tượng `Order` hoàn chỉnh từ Service.
    - Nhờ có cơ chế `cascade = CascadeType.ALL` ở Entity, Spring Data JPA sẽ tự động sinh ra các lệnh `INSERT INTO orders...` và `INSERT INTO order_items...`.
    - Lưu đồng loạt toàn bộ dữ liệu xuống cơ sở dữ liệu (MySQL).

## 2. Giải mã sức mạnh của @Transactional trong tạo Đơn hàng

Hàm `createOrder` là một giao dịch cực kỳ nhạy cảm về mặt dữ liệu. `@Transactional` ở đây đóng vai trò bảo vệ hệ thống theo nguyên tắc **All-or-Nothing (Thành công tất cả, hoặc không có gì)**.

### Kịch bản thực tế: Đặt 2 món hàng (Điện thoại và Ốp lưng)
```java
    @Transactional
    public Order createOrder(OrderRequest request) {
        // ... khởi tạo order
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.reduceStock(...); // Trừ kho
            // ... thêm vào danh sách
        }
        return orderRepository.save(order);
    }