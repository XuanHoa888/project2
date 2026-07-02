# XH Store Cosmetic E-Commerce Shop

Dự án website thương mại điện tử chuyên cung cấp mỹ phẩm và thời trang công sở dành cho nữ, được xây dựng trên nền tảng **Spring Boot** (Backend) kết hợp kiến trúc 3 tầng và giao diện responsive (Frontend).

## 1. Yêu cầu hệ thống
* **Java**: Phiên bản 17 trở lên.
* **Maven**: Phiên bản 3.8+ (để build và quản lý thư viện).
* **Cơ sở dữ liệu**: Microsoft SQL Server (khuyên dùng bản SQL Express).
* **IDE khuyên dùng**: IntelliJ IDEA, Eclipse, hoặc VS Code.

---

## 2. Hướng dẫn cấu hình Cơ sở dữ liệu (SQL Server)

Trước khi khởi chạy ứng dụng, bạn cần hoàn thành các bước cấu hình sau trên SQL Server của máy mới:

1. **Tạo Database**:
   * Mở SQL Server Management Studio (SSMS).
   * Tạo một cơ sở dữ liệu trống có tên là: `cosmetics_db`.

2. **Cấu hình tài khoản kết nối**:
   * **Cách 1 (Mặc định)**: Tạo một Login tên là `beautyapp` với mật khẩu là `Beauty@2024!` và phân quyền làm chủ (db_owner) trên database `cosmetics_db`.
   * **Cách 2 (Tùy chỉnh)**: Nếu bạn dùng tài khoản khác (ví dụ: `sa` hoặc tài khoản Windows của bạn), hãy truy cập file `src/main/resources/application.properties` và sửa đổi thông tin đăng nhập:
     ```properties
     spring.datasource.username=TÊN_TÀI_KHOẢN
     spring.datasource.password=MẬT_KHẨU
     ```

3. **Kích hoạt giao thức TCP/IP** (Quan trọng cho SQL Server):
   * Mở **SQL Server Configuration Manager** trên máy.
   * Chọn **SQL Server Network Configuration** -> **Protocols for SQLEXPRESS** (hoặc tên instance SQL Server của bạn).
   * Bật (**Enable**) giao thức **TCP/IP**.
   * Click đúp vào **TCP/IP**, chọn tab **IP Addresses**, kéo xuống cuối và cấu hình mục **TCP Port** là `1433` (ở phần *IPAll*).
   * Khởi động lại dịch vụ SQL Server trong mục **SQL Server Services** -> Nhấp chuột phải chọn **Restart**.

---

## 3. Khởi chạy ứng dụng

Sau khi hoàn thành cấu hình cơ sở dữ liệu ở trên:

### Cách 1: Sử dụng dòng lệnh (Terminal)
Mở terminal tại thư mục gốc của dự án (`prjxh2`) và chạy lệnh:
```bash
# Biên dịch và chạy ứng dụng
mvn spring-boot:run
```

### Cách 2: Chạy trực tiếp từ IDE
* Import dự án vào IntelliJ IDEA hoặc Eclipse dưới dạng dự án Maven.
* Tìm đến file khởi chạy chính tại đường dẫn: `src/main/java/com/xhstore/cosmetic/CosmeticApplication.java`.
* Click chuột phải vào file và chọn **Run 'CosmeticApplication'**.

---

## 4. Tự động hóa Khởi tạo dữ liệu (Database Seeder)
Dự án được tích hợp sẵn cơ chế tự động hóa dữ liệu:
* **Tự động tạo bảng**: Khi khởi chạy lần đầu, Spring Data JPA/Hibernate sẽ tự động quét các thực thể Java (Entities) và tạo đầy đủ 12 bảng vào database trống `cosmetics_db`.
* **Tự động chèn dữ liệu mẫu**: Khi phát hiện database chưa có tài khoản nào, lớp `DatabaseSeeder` sẽ tự động chèn các sản phẩm mẫu, danh mục, voucher khuyến mãi và các tài khoản mẫu vào database để hệ thống hoạt động ngay mà không cần import dữ liệu bằng tay.

### Danh sách tài khoản thử nghiệm có sẵn:
* **Tài khoản Admin (Quản trị viên)**:
  * **Username**: `admin`
  * **Password**: `admin123`
* **Tài khoản Client (Khách hàng)**:
  * **Username**: `user`
  * **Password**: `user123`

---

## 5. Truy cập hệ thống
Khi ứng dụng khởi động thành công và hiển thị thông báo `Started CosmeticApplication` trên log:
* Giao diện khách hàng: [http://localhost:8080/index.html](http://localhost:8080/index.html)
* Trang đăng nhập: [http://localhost:8080/login.html](http://localhost:8080/login.html)
* Giao diện Admin: Đăng nhập bằng tài khoản `admin` để tự động chuyển hướng đến trang quản trị hoặc truy cập trực tiếp [http://localhost:8080/admin/index.html](http://localhost:8080/admin/index.html).
