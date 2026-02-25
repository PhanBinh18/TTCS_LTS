# 📝 Phân tích Luồng dữ liệu (Flow) và @Transactional: Module Cart

## 1. Luồng chạy tổng thể của một Request (Data Flow)

Khi một hệ thống (như Postman hoặc Frontend web) gửi yêu cầu thêm sản phẩm vào giỏ hàng, luồng dữ liệu sẽ đi qua 3 "trạm kiểm soát" cốt lõi theo thứ tự từ ngoài vào trong:

1. **Trạm 1: Controller (`CartController`) - Người tiếp tân**
    - Đón nhận HTTP Request (phương thức `POST /api/carts/add`).
    - Lấy chuỗi JSON từ body của request và ép kiểu (map) tự động thành đối tượng Java `CartRequest` nhờ annotation `@RequestBody`.
    - Giao việc (gọi hàm) cho `CartService` xử lý nghiệp vụ thực sự.

2. **Trạm 2: Service (`CartService`) - Bộ não nghiệp vụ**
    - Nhận đối tượng `CartRequest` từ Controller.
    - Bắt đầu tính toán logic:
        - Gọi sang `ProductService` để lấy thông tin sản phẩm và kiểm tra kho (Logic).
        - Gọi `cartRepository` để tìm giỏ hàng hiện tại của user (Data).
        - Kiểm tra xem sản phẩm đã có trong giỏ chưa để quyết định là "Cộng dồn số lượng" hay "Tạo mới CartItem" (Logic).
    - Đóng gói dữ liệu đã chỉnh sửa và chuyển xuống tầng dưới cùng.

3. **Trạm 3: Repository (`CartRepository`) - Thủ kho**
    - Nhận lệnh `save(cart)` từ Service.
    - Nhờ Spring Data JPA dịch lệnh này thành các câu truy vấn SQL (`INSERT` hoặc `UPDATE`).
    - Mở kết nối xuống cơ sở dữ liệu (MySQL) và thực thi lệnh.

## 2. Giải mã @Transactional trong thực tế dự án

`@Transactional` là một cơ chế đảm bảo tính toàn vẹn của dữ liệu, tuân theo nguyên tắc **All-or-Nothing (Thành công tất cả, hoặc không có gì)**. Nó sẽ gom một cụm các thao tác với Database vào chung một "Giao dịch" (Transaction).

### Áp dụng vào hàm `clearCart`:
```java
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear(); 
        cartRepository.save(cart);
    }