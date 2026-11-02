# Lumina Hotel — Ứng dụng đặt phòng khách sạn (Android)

Ứng dụng Android đặt phòng khách sạn với hai vai trò **Khách** và **Admin**, viết bằng **Java** và **SQLite thuần** (`SQLiteOpenHelper`, không dùng Room, không dùng Kotlin). Giao diện theo phong cách Navy / Gold / Cream.

> Đồ án môn Phát triển ứng dụng di động

<!--
## Ảnh chụp màn hình
Thêm ảnh vào docs/screenshots/ rồi bỏ comment các dòng dưới:
![Trang chủ](docs/screenshots/home.png)
![Chi tiết phòng](docs/screenshots/room_detail.png)
![Thanh toán](docs/screenshots/payment.png)
![Dashboard Admin](docs/screenshots/admin_dashboard.png)
-->

## Tính năng

### Khách
- Đăng ký, đăng nhập (email/số điện thoại + mật khẩu) và **đăng nhập bằng Google** (Google Sign-In).
- Trang chủ: phòng nổi bật, khuyến mãi, phòng đã xem gần đây.
- Tìm phòng theo ngày, số khách, loại phòng, khoảng giá, tiện nghi; kết quả có kiểm tra **trùng lịch** (hết phòng trong khoảng ngày chọn).
- Chi tiết phòng: ảnh, tiện nghi, đánh giá, yêu thích.
- Thanh toán có đồng hồ giữ chỗ 15 phút, mã giảm giá (`LUMINA20`), tính tiền theo công thức chuẩn (phí dịch vụ + VAT 8%).
- Phương thức thanh toán: **VNPay Sandbox**, **MoMo (test)**, **Tiền mặt** (trả khi trả phòng), Thẻ tín dụng (demo).
- Đơn của tôi: lọc theo trạng thái, hủy đơn, đánh giá sau khi hoàn tất.
- Thông tin cá nhân, đổi mật khẩu, giới thiệu khách sạn.

### Admin
- **Dashboard**: doanh thu, số đơn, tỷ lệ lấp phòng, biểu đồ doanh thu (MPAndroidChart), tỷ trọng loại phòng, đơn mới nhất.
- Quản lý **Loại phòng**, **Phòng** (CRUD đầy đủ), **Đơn đặt phòng** (Xác nhận → Check-in → Check-out, Từ chối), **Khách hàng** (tìm kiếm, đánh dấu VIP).
- **Bộ lọc nâng cao** cho 3 màn Phòng / Đơn đặt phòng / Khách hàng (lưu bằng SharedPreferences).

## Công nghệ

| Hạng mục | Chi tiết |
|---|---|
| Ngôn ngữ | Java 11 |
| SDK | minSdk 24 · targetSdk/compileSdk theo `app/build.gradle` |
| Giao diện | Material Components 1.11.0, ViewBinding, ConstraintLayout |
| CSDL | SQLite tích hợp sẵn của Android (`HotelBooking.db`, 11 bảng, có seed dữ liệu mẫu) |
| Ảnh | Glide 4.16.0 |
| Biểu đồ | MPAndroidChart v3.1.0 |
| Đăng nhập | `play-services-auth` 21.2.0 (Google Sign-In) |
| Thanh toán | VNPay Sandbox (WebView), MoMo test (Chrome Custom Tabs, `androidx.browser` 1.8.0) |

## Cấu trúc mã nguồn

```
app/src/main/java/com/lumina/hotel/
├── data/          DatabaseHelper (singleton), DbContract
│   └── dao/       UserDao, RoomTypeDao, RoomDao, BookingDao, DashboardDao, ...
├── model/         POJO + các lớp bộ lọc (RoomFilter, BookingFilter, CustomerFilter)
├── adapter/       RecyclerView adapter (ListAdapter + DiffUtil)
├── ui/customer/   Màn hình phía khách
├── ui/admin/      Màn hình phía admin
└── util/          SessionManager, PriceUtils, DateUtils, StatusUtils, VnPayHelper, MoMoHelper, ...
```

Mọi truy vấn danh sách/thống kê chạy trên luồng nền; câu SQL dùng tham số `?`; `Cursor` luôn đóng trong `finally`.

## Cách chạy

1. Clone repo và mở bằng **Android Studio** (bản mới), chờ Gradle Sync.
2. Chạy trên **máy ảo có Google Play** (cần cho Google Sign-In) hoặc thiết bị thật.
3. Lần cài đầu tiên app tự tạo CSDL và nạp dữ liệu mẫu (5 loại phòng, 120 phòng, 7 khách, 31 đơn). Muốn nạp lại dữ liệu sạch: **gỡ app rồi cài lại**.

> Google Sign-In cần SHA-1 của máy build đã đăng ký trên Google Cloud Console. Nếu gặp lỗi `ApiException: 10`, kiểm tra lại SHA-1 và package name `com.lumina.hotel`.

## Tài khoản dùng thử

| Vai trò | Tài khoản | Mật khẩu |
|---|---|---|
| Admin | `admin@lumina.vn` | `admin123` |
| Khách VIP | `khoa.tran@email.com` | `123456` |
| Khách thường | `an.nguyen@email.com` | `123456` |

### Thẻ test cổng thanh toán (môi trường sandbox)
- **VNPay**: chọn "Thẻ nội địa và tài khoản ngân hàng" → NCB → số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
- **MoMo (ATM test)**: `9704 0000 0000 0018`, `NGUYEN VAN A`, hết hạn `03/07`, OTP `OTP`.

## Hạn chế đã biết

- **VNPay Sandbox** chạy trọn luồng thanh toán.
- **MoMo** đã tích hợp (tạo giao dịch, mở cổng bằng Chrome Custom Tabs, xác minh chữ ký khi quay về) nhưng chưa hoàn tất được giao dịch thành công do giới hạn của bộ khóa test công khai của MoMo.
- **Thẻ tín dụng** chỉ là demo (ghi "Đã thanh toán" ngay, không có cổng thật).
- Chữ ký thanh toán được tạo ngay trong app (phù hợp môi trường học tập/sandbox); ứng dụng thực tế cần tạo chữ ký và nhận IPN ở máy chủ.

## Quy trình đặt phòng

`Tìm phòng → Chi tiết → Thanh toán → Thành công` → Admin: `Chờ xác nhận → Đã xác nhận → Đang ở → Đã hoàn tất` (hoặc `Đã hủy`).

- Thanh toán online: đơn tạo ở trạng thái **Đã xác nhận** và **Đã thanh toán**.
- Tiền mặt: đơn tạo ở **Chờ xác nhận**, chuyển **Đã thanh toán** khi Admin Check-out.
