package com.lumina.hotel.data;

/**
 * Nơi khai báo TẤT CẢ tên bảng và tên cột của CSDL.
 * Nguyên tắc: không bao giờ gõ tay chuỗi tên cột ở DAO — luôn tham chiếu qua lớp này,
 * để khi đổi tên cột chỉ phải sửa duy nhất một chỗ.
 */
public final class DbContract {

    // Ghi chú: tên file CSDL ("HotelBooking.db") và số phiên bản được khai báo
    // TRỰC TIẾP làm hằng số riêng trong DatabaseHelper.java, không đặt ở đây —
    // mở đúng 1 file DatabaseHelper.java là thấy ngay tên file CSDL.
    //
    // LƯU Ý: mỗi khi sửa cấu trúc bảng (thêm/xoá/đổi cột) PHẢI tăng DATABASE_VERSION
    // lên 1 trong DatabaseHelper.java, nếu không app sẽ tiếp tục dùng file CSDL cũ
    // và báo lỗi "no such column". Cách khác khi đang phát triển: gỡ cài đặt app
    // rồi cài lại.

    private DbContract() { }

    public static final class UserTable {
        public static final String TABLE_NAME = "users";
        public static final String ID         = "id";
        public static final String FULL_NAME  = "full_name";
        public static final String EMAIL      = "email";
        public static final String PHONE      = "phone";
        public static final String PASSWORD   = "password";
        public static final String DOB        = "dob";
        public static final String AVATAR_URL = "avatar_url";
        public static final String ROLE       = "role";
        public static final String IS_VIP     = "is_vip";
        public static final String CREATED_AT = "created_at";
        public static final String AUTH_PROVIDER = "auth_provider";
        public static final String GOOGLE_ID     = "google_id";
    }

    public static final class RoomTypeTable {
        public static final String TABLE_NAME   = "room_types";
        public static final String ID           = "id";
        public static final String TYPE_NAME    = "type_name";
        public static final String DESCRIPTION  = "description";
        public static final String AREA_M2      = "area_m2";
        public static final String MAX_GUESTS   = "max_guests";
        public static final String BASE_PRICE   = "base_price";
        public static final String IMAGE_URL    = "image_url";
        public static final String TOTAL_ROOMS  = "total_rooms";
        public static final String IS_ACTIVE    = "is_active";
        public static final String AVG_RATING   = "avg_rating";
        public static final String REVIEW_COUNT = "review_count";
    }

    public static final class RoomTable {
        public static final String TABLE_NAME   = "rooms";
        public static final String ID           = "id";
        public static final String ROOM_NUMBER  = "room_number";
        public static final String FLOOR        = "floor";
        public static final String ROOM_TYPE_ID = "room_type_id";
        public static final String ROOM_STATUS  = "room_status";
        public static final String NOTE         = "note";
    }

    public static final class AmenityTable {
        public static final String TABLE_NAME   = "amenities";
        public static final String ID           = "id";
        public static final String AMENITY_NAME = "amenity_name";
        public static final String ICON_KEY     = "icon_key";
    }

    public static final class RoomTypeAmenityTable {
        public static final String TABLE_NAME   = "room_type_amenities";
        public static final String ROOM_TYPE_ID = "room_type_id";
        public static final String AMENITY_ID   = "amenity_id";
    }

    public static final class BookingTable {
        public static final String TABLE_NAME      = "bookings";
        public static final String ID              = "id";
        public static final String BOOKING_CODE    = "booking_code";
        public static final String USER_ID         = "user_id";
        public static final String ROOM_TYPE_ID    = "room_type_id";
        public static final String ROOM_ID         = "room_id";
        public static final String CHECK_IN_DATE   = "check_in_date";
        public static final String CHECK_OUT_DATE  = "check_out_date";
        public static final String NUM_NIGHTS      = "num_nights";
        public static final String NUM_GUESTS      = "num_guests";
        public static final String ROOM_SUBTOTAL   = "room_subtotal";
        public static final String SERVICE_FEE     = "service_fee";
        public static final String VAT_AMOUNT      = "vat_amount";
        public static final String DISCOUNT_AMOUNT = "discount_amount";
        public static final String TOTAL_AMOUNT    = "total_amount";
        public static final String BOOKING_STATUS  = "booking_status";
        public static final String NOTE            = "note";
        public static final String CREATED_AT      = "created_at";
    }

    public static final class PaymentTable {
        public static final String TABLE_NAME     = "payments";
        public static final String ID             = "id";
        public static final String BOOKING_ID     = "booking_id";
        public static final String METHOD         = "method";
        public static final String AMOUNT         = "amount";
        public static final String PAYMENT_STATUS = "payment_status";
        public static final String PAID_AT        = "paid_at";
        public static final String TXN_REF        = "txn_ref";
    }

    public static final class ReviewTable {
        public static final String TABLE_NAME   = "reviews";
        public static final String ID           = "id";
        public static final String BOOKING_ID   = "booking_id";
        public static final String USER_ID      = "user_id";
        public static final String ROOM_TYPE_ID = "room_type_id";
        public static final String RATING       = "rating";
        public static final String COMMENT      = "comment";
        public static final String CREATED_AT   = "created_at";
    }

    public static final class PromotionTable {
        public static final String TABLE_NAME       = "promotions";
        public static final String ID               = "id";
        public static final String CODE             = "code";
        public static final String TITLE            = "title";
        public static final String DISCOUNT_PERCENT = "discount_percent";
        public static final String MAX_DISCOUNT     = "max_discount";
        public static final String START_DATE       = "start_date";
        public static final String END_DATE         = "end_date";
        public static final String IS_ACTIVE        = "is_active";
    }

    public static final class FavoriteTable {
        public static final String TABLE_NAME   = "favorites";
        public static final String ID           = "id";
        public static final String USER_ID      = "user_id";
        public static final String ROOM_TYPE_ID = "room_type_id";
    }

    public static final class RecentViewTable {
        public static final String TABLE_NAME   = "recent_views";
        public static final String ID           = "id";
        public static final String USER_ID      = "user_id";
        public static final String ROOM_TYPE_ID = "room_type_id";
        public static final String VIEWED_AT    = "viewed_at";
    }

    /* ===== Giá trị hằng cho các cột trạng thái (tránh gõ sai chuỗi) ===== */

    public static final class BookingStatus {
        public static final String PENDING    = "pending";
        public static final String CONFIRMED  = "confirmed";
        public static final String CHECKED_IN = "checked_in";
        public static final String COMPLETED  = "completed";
        public static final String CANCELLED  = "cancelled";
    }

    public static final class RoomStatus {
        public static final String AVAILABLE   = "available";
        public static final String BOOKED      = "booked";
        public static final String OCCUPIED    = "occupied";
        public static final String CLEANING    = "cleaning";
        public static final String MAINTENANCE = "maintenance";
    }

    public static final class Role {
        public static final String ADMIN    = "admin";
        public static final String CUSTOMER = "customer";
    }

    public static final class PaymentMethod {
        public static final String VNPAY = "vnpay";
        public static final String MOMO  = "momo";
        public static final String CARD  = "card";
    }

    public static final class AuthProvider {
        public static final String LOCAL  = "local";
        public static final String GOOGLE = "google";
    }
}
