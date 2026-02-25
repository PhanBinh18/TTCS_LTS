# I. Các annotation trong Spring data JPA
### @Entity: Đánh dấu class là một thực thể JPA.
  - Giải thích: Báo cho Spring Boot biết class này map với một bảng trong CSDL.
  
### @Table(name = "orders"): Chỉ định tên bảng.

### @Data: Tự động sinh mã (Lombok). 
  - Giải thích: Nó tự động sinh ra các hàm Getter, Setter, toString(), equals(), và hashCode() ngầm bên dưới lúc biên dịch. 

### @Id & @GeneratedValue(strategy = GenerationType.IDENTITY): Định nghĩa Khóa chính tự tăng.

### @OneToMany(mappedBy = "order", cascade = CascadeType.ALL): Định nghĩa mối quan hệ 1-Nhiều.
- Giải thích: Một Đơn hàng (Order) có nhiều Chi tiết đơn hàng (OrderItem). 
- mappedBy = "order": Báo cho JPA biết rằng mối quan hệ này được quản lý bởi biến order nằm bên trong class OrderItem. Bảng orders sẽ không chứa cột khóa ngoại, khóa ngoại sẽ nằm bên bảng order_items. 
- cascade = CascadeType.ALL: Hiệu ứng dây chuyền. Nếu bạn lưu (save()) một cái Order mới, JPA sẽ tự động lưu tất cả các OrderItem nằm trong list items xuống DB mà không cần bạn phải gọi lệnh lưu cho từng cái item. Tương tự khi xóa (Delete).


# II. DB Design

## 1. Dữ liệu cần được lưu trữ

* **Dữ liệu Người dùng (Identity Data):**
    * Thông tin định danh và xác thực: Email (dùng làm tài khoản đăng nhập), mật khẩu.
    * Thông tin cá nhân: Họ tên, số điện thoại, địa chỉ giao hàng mặc định.
    * Thông tin hệ thống: Quyền hạn (Role), trạng thái khóa tài khoản (Active/Inactive), thời gian tạo.
* **Dữ liệu Sản phẩm (Product Data):**
    * Thông tin cơ bản của hàng hóa: Tên sản phẩm, giá bán hiện tại, số lượng tồn kho.
* **Dữ liệu Giỏ hàng (Cart Data):**
    * Thông tin phiên giỏ hàng: Mã giỏ hàng, ID người dùng sở hữu (mỗi người dùng chỉ có 1 giỏ hàng active), thời gian cập nhật cuối.
    * Chi tiết giỏ hàng: Danh sách các sản phẩm đang chọn, ID sản phẩm, số lượng dự kiến mua.
* **Dữ liệu Đơn hàng (Order Data):**
    * Thông tin tổng quan giao dịch: ID người dùng đặt hàng, tổng tiền cần thanh toán, trạng thái đơn hàng (chờ xử lý, đã thanh toán...), thời gian tạo đơn.
    * Chi tiết đơn hàng (Snapshot Data): ID sản phẩm, số lượng mua. Đặc biệt cần lưu trữ cứng tên sản phẩm và giá bán tại đúng thời điểm đặt hàng để tránh sai lệch dữ liệu kế toán khi giá sản phẩm thay đổi sau này.

---

## 2. Xây dựng lược đồ thực thể ERD

Trong bối cảnh hệ thống hướng tới phân tán dữ liệu (Database-per-service), lược đồ thực thể của dự án tồn tại hai loại mối quan hệ: **Quan hệ vật lý (bên trong cùng một module)** và **Quan hệ logic (giao tiếp chéo giữa các module)**.

Các thực thể và mối quan hệ khái niệm (Conceptual Relationships) bao gồm:

* **User (Người dùng)**
    * Có mối quan hệ **1-1 (Một - Một)** với **Cart**: Một người dùng chỉ sở hữu một giỏ hàng duy nhất tại một thời điểm.
    * Có mối quan hệ **1-N (Một - Nhiều)** với **Order**: Một người dùng có thể thực hiện nhiều đơn đặt hàng khác nhau theo thời gian.
* **Product (Sản phẩm)**
    * Có mối quan hệ **1-N (Một - Nhiều)** với **CartItem**: Một loại sản phẩm có thể nằm trong giỏ hàng của nhiều người khác nhau.
    * Có mối quan hệ **1-N (Một - Nhiều)** với **OrderItem**: Một loại sản phẩm có thể xuất hiện trong nhiều hóa đơn khác nhau.
* **Cart (Giỏ hàng)**
    * Có mối quan hệ **1-N (Một - Nhiều)** với **CartItem**: Một giỏ hàng chứa nhiều dòng chi tiết sản phẩm. (Đây là mối quan hệ vật lý, có tính ràng buộc tồn tại).
* **Order (Đơn hàng)**
    * Có mối quan hệ **1-N (Một - Nhiều)** với **OrderItem**: Một đơn hàng chứa nhiều dòng chi tiết hóa đơn. (Đây là mối quan hệ vật lý, có tính ràng buộc tồn tại).

---

## 3. Ánh xạ sang lược đồ quan hệ

Quá trình chuyển đổi từ ERD sang các bảng (Tables) trong cơ sở dữ liệu quan hệ được thực hiện như sau.

**Quy ước ký hiệu:**
* **PK (Primary Key):** Khóa chính vật lý.
* **FK (Foreign Key):** Khóa ngoại vật lý (Chỉ dùng trong cùng 1 module).
* **Logical-FK:** Khóa ngoại logic (Lưu ID dưới dạng `BIGINT` nhưng không tạo ràng buộc Constraint trong CSDL để phục vụ tách DB).

**Danh sách các lược đồ quan hệ (Relational Schemas):**

1. **Bảng `users`** (Module Identity)
    * `users` (**id** [PK], email [Unique], password, full_name, phone, address, role, is_active, created_at)
2. **Bảng `products`** (Module Product)
    * `products` (**id** [PK], name, price, stock)
3. **Bảng `carts`** (Module Cart)
    * `carts` (**id** [PK], user_id [Logical-FK, Unique], updated_at)
4. **Bảng `cart_items`** (Module Cart)
    * `cart_items` (**id** [PK], cart_id [FK trỏ về carts.id], product_id [Logical-FK trỏ về products.id], quantity)
5. **Bảng `orders`** (Module Order)
    * `orders` (**id** [PK], user_id [Logical-FK trỏ về users.id], total_price, status, created_at)
6. **Bảng `order_items`** (Module Order)
    * `order_items` (**id** [PK], order_id [FK trỏ về orders.id], product_id [Logical-FK trỏ về products.id], product_name, price, quantity)