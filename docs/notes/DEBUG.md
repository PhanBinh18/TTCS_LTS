#  Nhật ký Debug: Null Product trong Spring Boot

**Ngày ghi nhận:** 21/03/2026
**Dự án:** E-commerce Microservices (Spring Boot & React)
**Vấn đề:** Dữ liệu sản phẩm (`product`) liên tục trả về `null` trong API Giỏ hàng mặc dù Backend đã truy vấn thành công.

---

## 🛑 1. Ý tưởng ban đầu & Cách tiếp cận (Hacky Way)
* **Kiến trúc:** Liên kết lỏng (Loose Coupling). Bảng `cart_items` chỉ lưu `productId` (Long) thay vì dùng khóa ngoại `@ManyToOne` trực tiếp tới bảng `products`. Mục tiêu là giảm sự phụ thuộc giữa các module.
* **Cách làm:** Thêm một biến `private Product product;` vào thẳng Entity `CartItem` và đánh dấu nó là `@Transient` để Hibernate bỏ qua cột này khi map với Database.
* **Kỳ vọng:** Viết hàm `enrichCartWithProducts()` trong `CartService`. Hàm này chạy vòng lặp, dùng `productId` gọi sang `ProductService` để lấy chi tiết sản phẩm, nhét vào biến `@Transient` rồi ném thẳng Entity `Cart` về cho Frontend qua Controller.

## 🌪️ 2. Những "cú lừa" và Diễn biến lỗi
Dù logic code Java hoàn toàn không sai, nhưng dữ liệu trả về Frontend liên tục bị `"product": null`. Quá trình debug đã phơi bày hàng loạt vấn đề ngầm của Framework:

1. **Cú lừa của Jackson & Lombok:** Thư viện Jackson (chuyển Java thành JSON) mặc định phớt lờ các biến `@Transient`. Ngay cả khi dùng `@JsonProperty`, việc xài `@Data` của Lombok đôi khi sinh ra Getter/Setter không tương thích, khiến Jackson "mù màu" với biến này.
2. **Cú lừa của IDE (IntelliJ Cache):** Sửa code xong, thêm lệnh `System.out.println` nhưng Console không hề in ra log. Nguyên nhân do IDE không biên dịch lại code mới (recompile). Phải dùng "vũ khí hạt nhân" `mvn clean install` mới ép hệ thống chạy code mới.
3. **Trùm cuối - Sự kìm kẹp của Hibernate:** Đây là nguyên nhân cốt lõi. Vì `Cart` và `CartItem` là các **Entity đang bị Hibernate quản lý (Persistent State)**. Trước khi Controller biến chúng thành JSON, Hibernate Session thực hiện kiểm tra và "làm sạch" đối tượng. Nó nhận thấy biến `@Transient` chứa dữ liệu không thuộc về Database, nên đã âm thầm **reset nó về null** (hoặc trả về bản Proxy cũ) để đảm bảo tính toàn vẹn với DB.

## 💡 3. Cách khắc phục triệt để: DTO Pattern
Thay vì cố gắng "chiến đấu" và ép Hibernate/Jackson phải làm theo ý mình trực tiếp trên file Entity, giải pháp dứt điểm là tách bạch hoàn toàn Database Model và API Response.

* **Tạo DTO (Data Transfer Object):** Tạo ra `CartDto` và `CartItemDto`. Đây là những class Java thuần túy (POJO), hoàn toàn "trắng tinh", không có `@Entity` hay `@Transient`, không dính dáng gì đến Hibernate.
* **Áp dụng Mapping:** * Entity (`Cart`) chỉ làm nhiệm vụ giao tiếp với Database để lấy cấu trúc gốc (gồm `id`, `productId`, `quantity`).
    * Trong `CartService`, map toàn bộ dữ liệu từ Entity sang DTO.
    * Bơm thông tin `Product` thật vào biến `product` của `CartItemDto`.
* **Kết quả:** Controller trả về `CartDto`. Vì DTO không bị Hibernate quản lý, dữ liệu được giữ nguyên vẹn 100% từ RAM ra đến Frontend. Cấu trúc JSON trả về hoàn hảo, sạch sẽ và an toàn.

## 🎯 4. Bài học rút ra (Key Takeaways)
1. **Không bao giờ dùng Entity làm API Response:** Entity sinh ra để map với Database. Việc ép nó gánh thêm các trường dữ liệu tạm (`@Transient`) để phục vụ UI là một Bad Practice, dễ gây xung đột thư viện (Hibernate vs Jackson).
2. **DTO là bắt buộc trong kiến trúc chuẩn:** Luôn tạo lớp DTO làm trung gian vận chuyển dữ liệu. Tuy tốn công viết thêm class và hàm mapping, nhưng code sẽ cực kỳ dễ bảo trì, dễ debug và không bao giờ gặp lỗi "bóng ma" dữ liệu.
3. **Cẩn thận với Lombok:** Khi gặp các annotation phức tạp kết hợp với nhau (`@Transient`, `@JsonProperty`), đôi khi tự viết Getter/Setter tay (hoặc dùng DTO) sẽ an toàn hơn là phó mặc cho `@Data`.
4. **Luôn dọn đường băng (Clean Build):** Nếu code logic chắc chắn đúng mà máy chạy sai, log không nhảy, hãy nghĩ ngay đến Cache. Đừng ngại gõ lệnh `mvn clean install -DskipTests`!