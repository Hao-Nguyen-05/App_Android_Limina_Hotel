package com.lumina.hotel.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lumina.hotel.data.DbContract.AuthProvider;
import com.lumina.hotel.data.DbContract.BookingStatus;
import com.lumina.hotel.data.DbContract.PaymentMethod;
import com.lumina.hotel.data.DbContract.Role;
import com.lumina.hotel.data.DbContract.RoomStatus;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.PriceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Lớp quản lý file CSDL SQLite của app.
 *
 * Vòng đời:
 *  - onConfigure : chạy TRƯỚC mọi thứ, dùng để bật ràng buộc khoá ngoại.
 *  - onCreate    : chỉ chạy ĐÚNG MỘT LẦN, khi file HotelBooking.db chưa tồn tại.
 *  - onUpgrade   : chạy khi DATABASE_VERSION tăng lên so với file đang có.
 *
 * Vì onCreate chỉ chạy một lần, nếu sửa phần seed mà muốn thấy dữ liệu mới thì phải
 * GỠ CÀI ĐẶT app rồi cài lại, hoặc tăng hằng số DATABASE_VERSION ở ngay trên.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "HotelBooking.db";
    // v2: thêm auth_provider + google_id vào users (cho Google Sign-In ở Giai đoạn 2),
    // password cho phép NULL, thêm UNIQUE(booking_id) vào payments.
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ====================================================================
    // 1. CÁC CÂU LỆNH TẠO BẢNG
    // ====================================================================

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE users (" +
                    "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  full_name  TEXT    NOT NULL," +
                    "  email      TEXT    NOT NULL UNIQUE," +
                    "  phone      TEXT," +
                    "  password   TEXT," +                          // cho phép NULL: tài khoản Google không tự đặt mật khẩu
                    "  dob        INTEGER," +                       // epoch millis
                    "  avatar_url TEXT," +
                    "  role       TEXT    NOT NULL DEFAULT 'customer'," +
                    "  is_vip     INTEGER NOT NULL DEFAULT 0," +    // SQLite không có BOOLEAN: 0/1
                    "  created_at INTEGER NOT NULL," +
                    "  auth_provider TEXT NOT NULL DEFAULT 'local'," +
                    "  google_id  TEXT    UNIQUE" +                 // NULL trừ khi đăng ký/đăng nhập qua Google
                    ")";

    private static final String SQL_CREATE_ROOM_TYPES =
            "CREATE TABLE room_types (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  type_name    TEXT    NOT NULL," +
                    "  description  TEXT," +
                    "  area_m2      INTEGER NOT NULL DEFAULT 0," +
                    "  max_guests   INTEGER NOT NULL DEFAULT 2," +
                    "  base_price   INTEGER NOT NULL," +            // VNĐ, không dùng REAL
                    "  image_url    TEXT," +
                    "  total_rooms  INTEGER NOT NULL DEFAULT 0," +
                    "  is_active    INTEGER NOT NULL DEFAULT 1," +
                    "  avg_rating   REAL    NOT NULL DEFAULT 0," +
                    "  review_count INTEGER NOT NULL DEFAULT 0" +
                    ")";

    private static final String SQL_CREATE_ROOMS =
            "CREATE TABLE rooms (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  room_number  TEXT    NOT NULL UNIQUE," +
                    "  floor        INTEGER NOT NULL DEFAULT 1," +
                    "  room_type_id INTEGER NOT NULL," +
                    "  room_status  TEXT    NOT NULL DEFAULT 'available'," +
                    "  note         TEXT," +
                    "  FOREIGN KEY(room_type_id) REFERENCES room_types(id) ON DELETE CASCADE" +
                    ")";

    private static final String SQL_CREATE_AMENITIES =
            "CREATE TABLE amenities (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  amenity_name TEXT NOT NULL," +
                    "  icon_key     TEXT NOT NULL" +
                    ")";

    private static final String SQL_CREATE_ROOM_TYPE_AMENITIES =
            "CREATE TABLE room_type_amenities (" +
                    "  room_type_id INTEGER NOT NULL," +
                    "  amenity_id   INTEGER NOT NULL," +
                    "  PRIMARY KEY(room_type_id, amenity_id)," +
                    "  FOREIGN KEY(room_type_id) REFERENCES room_types(id) ON DELETE CASCADE," +
                    "  FOREIGN KEY(amenity_id)   REFERENCES amenities(id)  ON DELETE CASCADE" +
                    ")";

    private static final String SQL_CREATE_BOOKINGS =
            "CREATE TABLE bookings (" +
                    "  id              INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  booking_code    TEXT    NOT NULL UNIQUE," +
                    "  user_id         INTEGER NOT NULL," +
                    "  room_type_id    INTEGER NOT NULL," +
                    "  room_id         INTEGER," +                  // NULL khi chưa gán phòng
                    "  check_in_date   INTEGER NOT NULL," +
                    "  check_out_date  INTEGER NOT NULL," +
                    "  num_nights      INTEGER NOT NULL," +
                    "  num_guests      INTEGER NOT NULL DEFAULT 1," +
                    "  room_subtotal   INTEGER NOT NULL," +
                    "  service_fee     INTEGER NOT NULL DEFAULT 0," +
                    "  vat_amount      INTEGER NOT NULL DEFAULT 0," +
                    "  discount_amount INTEGER NOT NULL DEFAULT 0," +
                    "  total_amount    INTEGER NOT NULL," +
                    "  booking_status  TEXT    NOT NULL DEFAULT 'pending'," +
                    "  note            TEXT," +
                    "  created_at      INTEGER NOT NULL," +
                    "  FOREIGN KEY(user_id)      REFERENCES users(id)      ON DELETE CASCADE," +
                    "  FOREIGN KEY(room_type_id) REFERENCES room_types(id) ON DELETE CASCADE," +
                    "  FOREIGN KEY(room_id)      REFERENCES rooms(id)      ON DELETE SET NULL" +
                    ")";

    private static final String SQL_CREATE_PAYMENTS =
            "CREATE TABLE payments (" +
                    "  id             INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  booking_id     INTEGER NOT NULL," +
                    "  method         TEXT    NOT NULL," +
                    "  amount         INTEGER NOT NULL," +
                    "  payment_status TEXT    NOT NULL DEFAULT 'unpaid'," +
                    "  paid_at        INTEGER," +
                    "  txn_ref        TEXT," +
                    "  UNIQUE(booking_id)," +                       // quan hệ 1-1 với bookings
                    "  FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE CASCADE" +
                    ")";

    // booking_id để NULL được: dữ liệu mẫu có nhiều đánh giá cũ không gắn với đơn nào.
    private static final String SQL_CREATE_REVIEWS =
            "CREATE TABLE reviews (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  booking_id   INTEGER," +
                    "  user_id      INTEGER NOT NULL," +
                    "  room_type_id INTEGER NOT NULL," +
                    "  rating       INTEGER NOT NULL," +
                    "  comment      TEXT," +
                    "  created_at   INTEGER NOT NULL," +
                    "  FOREIGN KEY(booking_id)   REFERENCES bookings(id)   ON DELETE SET NULL," +
                    "  FOREIGN KEY(user_id)      REFERENCES users(id)      ON DELETE CASCADE," +
                    "  FOREIGN KEY(room_type_id) REFERENCES room_types(id) ON DELETE CASCADE" +
                    ")";

    private static final String SQL_CREATE_PROMOTIONS =
            "CREATE TABLE promotions (" +
                    "  id               INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  code             TEXT    NOT NULL UNIQUE," +
                    "  title            TEXT," +
                    "  discount_percent INTEGER NOT NULL DEFAULT 0," +
                    "  max_discount     INTEGER NOT NULL DEFAULT 0," +
                    "  start_date       INTEGER NOT NULL," +
                    "  end_date         INTEGER NOT NULL," +
                    "  is_active        INTEGER NOT NULL DEFAULT 1" +
                    ")";

    private static final String SQL_CREATE_FAVORITES =
            "CREATE TABLE favorites (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  user_id      INTEGER NOT NULL," +
                    "  room_type_id INTEGER NOT NULL," +
                    "  UNIQUE(user_id, room_type_id)," +
                    "  FOREIGN KEY(user_id)      REFERENCES users(id)      ON DELETE CASCADE," +
                    "  FOREIGN KEY(room_type_id) REFERENCES room_types(id) ON DELETE CASCADE" +
                    ")";

    private static final String SQL_CREATE_RECENT_VIEWS =
            "CREATE TABLE recent_views (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  user_id      INTEGER NOT NULL," +
                    "  room_type_id INTEGER NOT NULL," +
                    "  viewed_at    INTEGER NOT NULL," +
                    "  UNIQUE(user_id, room_type_id)," +
                    "  FOREIGN KEY(user_id)      REFERENCES users(id)      ON DELETE CASCADE," +
                    "  FOREIGN KEY(room_type_id) REFERENCES room_types(id) ON DELETE CASCADE" +
                    ")";

    private static final String[] SQL_CREATE_INDEXES = {
            "CREATE INDEX idx_booking_user    ON bookings(user_id)",
            "CREATE INDEX idx_booking_type    ON bookings(room_type_id)",
            "CREATE INDEX idx_booking_status  ON bookings(booking_status)",
            "CREATE INDEX idx_booking_created ON bookings(created_at)",
            "CREATE INDEX idx_room_status     ON rooms(room_status)",
            "CREATE INDEX idx_room_type       ON rooms(room_type_id)",
            "CREATE INDEX idx_review_type     ON reviews(room_type_id)",
            "CREATE INDEX idx_payment_booking ON payments(booking_id)"
    };

    // Xoá theo thứ tự bảng con trước, bảng cha sau (tránh vướng khoá ngoại).
    private static final String[] SQL_DROP_ALL = {
            "DROP TABLE IF EXISTS recent_views",
            "DROP TABLE IF EXISTS favorites",
            "DROP TABLE IF EXISTS reviews",
            "DROP TABLE IF EXISTS payments",
            "DROP TABLE IF EXISTS bookings",
            "DROP TABLE IF EXISTS room_type_amenities",
            "DROP TABLE IF EXISTS amenities",
            "DROP TABLE IF EXISTS rooms",
            "DROP TABLE IF EXISTS room_types",
            "DROP TABLE IF EXISTS promotions",
            "DROP TABLE IF EXISTS users"
    };

    // ====================================================================
    // 2. VÒNG ĐỜI
    // ====================================================================

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // BẮT BUỘC: Android mặc định TẮT ràng buộc khoá ngoại.
        // Không bật dòng này thì ON DELETE CASCADE sẽ không có tác dụng.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_ROOM_TYPES);
        db.execSQL(SQL_CREATE_ROOMS);
        db.execSQL(SQL_CREATE_AMENITIES);
        db.execSQL(SQL_CREATE_ROOM_TYPE_AMENITIES);
        db.execSQL(SQL_CREATE_BOOKINGS);
        db.execSQL(SQL_CREATE_PAYMENTS);
        db.execSQL(SQL_CREATE_REVIEWS);
        db.execSQL(SQL_CREATE_PROMOTIONS);
        db.execSQL(SQL_CREATE_FAVORITES);
        db.execSQL(SQL_CREATE_RECENT_VIEWS);
        for (String sql : SQL_CREATE_INDEXES) db.execSQL(sql);

        // Dùng CHÍNH đối tượng db được truyền vào.
        // Gọi getWritableDatabase() ở đây sẽ gây lỗi "getDatabase called recursively".
        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Dự án học tập: chấp nhận xoá sạch và tạo lại khi đổi cấu trúc.
        // Sản phẩm thật sẽ phải viết lệnh ALTER TABLE để giữ dữ liệu người dùng.
        for (String sql : SQL_DROP_ALL) db.execSQL(sql);
        onCreate(db);
    }

    // ====================================================================
    // 3. DỮ LIỆU MẪU
    // ====================================================================

    /** Ảnh minh hoạ lấy từ Unsplash (cần quyền INTERNET để Glide tải về). */
    private static final String IMG_STANDARD =
            "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80";
    private static final String IMG_DELUXE =
            "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&q=80";
    private static final String IMG_SUITE =
            "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800&q=80";
    private static final String IMG_VILLA =
            "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80";
    private static final String IMG_FAMILY =
            "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800&q=80";

    private void seedData(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            long[] userIds  = seedUsers(db);
            long[] typeIds  = seedRoomTypes(db);
            seedAmenities(db, typeIds);
            seedRooms(db, typeIds);
            seedBookings(db, userIds, typeIds);
            seedReviews(db, userIds, typeIds);
            refreshRatingCache(db);
            seedPromotions(db);
            seedFavoritesAndViews(db, userIds, typeIds);

            db.setTransactionSuccessful();   // Không gọi dòng này = mọi thứ bị huỷ bỏ
        } finally {
            db.endTransaction();             // Luôn phải có, kể cả khi ném lỗi
        }
    }

    /* ---------- 3.1 Người dùng ---------- */

    /** @return mảng id theo thứ tự: [0]=admin, [1]=Khoa, [2]=An, [3]=Hồng, [4]=Bảo, [5]=Mai, [6]=Hà */
    private long[] seedUsers(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        long[] ids = new long[7];
        ids[0] = insertUser(db, "Quản trị viên", "admin@lumina.vn", "0900000000",
                "admin123", Role.ADMIN, false, DateUtils.dateOf(1990, 1, 1), now);
        ids[1] = insertUser(db, "Trần Minh Khoa", "khoa.tran@email.com", "0987654321",
                "123456", Role.CUSTOMER, true,  DateUtils.dateOf(1990, 3, 12), DateUtils.monthsAgo(26));
        ids[2] = insertUser(db, "Nguyễn Văn An", "an.nguyen@email.com", "0901234567",
                "123456", Role.CUSTOMER, false, DateUtils.dateOf(1995, 8, 15), DateUtils.monthsAgo(20));
        ids[3] = insertUser(db, "Lê Thị Hồng", "hong.le@email.com", "0912345678",
                "123456", Role.CUSTOMER, false, DateUtils.dateOf(1992, 5, 2),  DateUtils.monthsAgo(14));
        ids[4] = insertUser(db, "Phạm Quốc Bảo", "bao.pham@email.com", "0903111222",
                "123456", Role.CUSTOMER, true,  DateUtils.dateOf(1988, 11, 30), DateUtils.monthsAgo(30));
        ids[5] = insertUser(db, "Hoàng Mai", "mai.hoang@email.com", "0904555666",
                "123456", Role.CUSTOMER, false, DateUtils.dateOf(1998, 2, 18), DateUtils.monthsAgo(6));
        ids[6] = insertUser(db, "Đặng Thu Hà", "ha.dang@email.com", "0905777888",
                "123456", Role.CUSTOMER, false, DateUtils.dateOf(1996, 7, 9),  DateUtils.monthsAgo(9));
        return ids;
    }

    private long insertUser(SQLiteDatabase db, String name, String email, String phone,
                            String pass, String role, boolean vip, long dob, long createdAt) {
        ContentValues v = new ContentValues();
        v.put(DbContract.UserTable.FULL_NAME, name);
        v.put(DbContract.UserTable.EMAIL, email);
        v.put(DbContract.UserTable.PHONE, phone);
        v.put(DbContract.UserTable.PASSWORD, pass);
        v.put(DbContract.UserTable.DOB, dob);
        v.put(DbContract.UserTable.ROLE, role);
        v.put(DbContract.UserTable.IS_VIP, vip ? 1 : 0);
        v.put(DbContract.UserTable.CREATED_AT, createdAt);
        v.put(DbContract.UserTable.AUTH_PROVIDER, AuthProvider.LOCAL);
        return db.insert(DbContract.UserTable.TABLE_NAME, null, v);
    }

    /* ---------- 3.2 Loại phòng ---------- */

    /** @return [0]=Standard, [1]=Deluxe, [2]=Suite, [3]=Villa, [4]=Family */
    private long[] seedRoomTypes(SQLiteDatabase db) {
        long[] ids = new long[5];
        ids[0] = insertRoomType(db, "Standard Garden", 25, 2, 1_500_000L, 30, IMG_STANDARD,
                "Phòng Standard 25m² nhìn ra vườn, thiết kế tối giản ấm cúng với đầy đủ tiện "
                        + "nghi cơ bản. Lựa chọn hợp lý cho chuyến đi ngắn ngày của cặp đôi hoặc khách công tác.");
        ids[1] = insertRoomType(db, "Deluxe Ocean View", 35, 2, 2_500_000L, 40, IMG_DELUXE,
                "Phòng Deluxe 35m² với ban công hướng biển, giường King cỡ lớn và bồn tắm "
                        + "riêng. Đón bình minh trên vịnh ngay từ cửa sổ phòng.");
        ids[2] = insertRoomType(db, "Suite Executive", 55, 3, 4_200_000L, 25, IMG_SUITE,
                "Suite Executive 55m² mang tầm nhìn toàn cảnh biển, thiết kế nội thất sang "
                        + "trọng với phong cách đương đại. Phòng có phòng khách riêng, phòng tắm view biển và "
                        + "ban công rộng phù hợp cho gia đình hoặc cặp đôi muốn trải nghiệm nghỉ dưỡng đẳng cấp.");
        ids[3] = insertRoomType(db, "Pool Villa", 60, 4, 3_800_000L, 15, IMG_VILLA,
                "Biệt thự 60m² có hồ bơi riêng và sân vườn khép kín, tách biệt hoàn toàn khỏi "
                        + "khu trung tâm. Không gian lý tưởng cho kỳ nghỉ riêng tư.");
        ids[4] = insertRoomType(db, "Family Suite", 70, 5, 5_200_000L, 10, IMG_FAMILY,
                "Suite gia đình 70m² gồm hai phòng ngủ thông nhau, khu vui chơi nhỏ cho trẻ và "
                        + "bếp mini. Sức chứa tới 5 người lớn.");
        return ids;
    }

    private long insertRoomType(SQLiteDatabase db, String name, int area, int maxGuests,
                                long price, int totalRooms, String img, String desc) {
        ContentValues v = new ContentValues();
        v.put(DbContract.RoomTypeTable.TYPE_NAME, name);
        v.put(DbContract.RoomTypeTable.DESCRIPTION, desc);
        v.put(DbContract.RoomTypeTable.AREA_M2, area);
        v.put(DbContract.RoomTypeTable.MAX_GUESTS, maxGuests);
        v.put(DbContract.RoomTypeTable.BASE_PRICE, price);
        v.put(DbContract.RoomTypeTable.IMAGE_URL, img);
        v.put(DbContract.RoomTypeTable.TOTAL_ROOMS, totalRooms);
        v.put(DbContract.RoomTypeTable.IS_ACTIVE, 1);
        return db.insert(DbContract.RoomTypeTable.TABLE_NAME, null, v);
    }

    /* ---------- 3.3 Tiện nghi ---------- */

    private void seedAmenities(SQLiteDatabase db, long[] typeIds) {
        String[][] data = {
                {"Wifi free",  "wifi"},
                {"Điều hòa",   "ac"},
                {"Smart TV",   "tv"},
                {"Minibar",    "minibar"},
                {"Bữa sáng",   "breakfast"},
                {"Hồ bơi",     "pool"},
                {"Spa",        "spa"},
                {"Gym",        "gym"},
                {"Đỗ xe",      "parking"}
        };
        long[] amenityIds = new long[data.length];
        for (int i = 0; i < data.length; i++) {
            ContentValues v = new ContentValues();
            v.put(DbContract.AmenityTable.AMENITY_NAME, data[i][0]);
            v.put(DbContract.AmenityTable.ICON_KEY, data[i][1]);
            amenityIds[i] = db.insert(DbContract.AmenityTable.TABLE_NAME, null, v);
        }

        // Chỉ số tiện nghi gán cho từng loại phòng (0=Wifi ... 8=Đỗ xe)
        int[][] mapping = {
                {0, 1, 2, 8},                   // Standard
                {0, 1, 2, 3, 4, 5, 8},          // Deluxe
                {0, 1, 2, 3, 4, 5},             // Suite  (đúng 6 ô như bản thiết kế)
                {0, 1, 2, 3, 5, 6, 8},          // Villa
                {0, 1, 2, 3, 4, 5, 7, 8}        // Family
        };
        for (int t = 0; t < typeIds.length; t++) {
            for (int a : mapping[t]) {
                ContentValues v = new ContentValues();
                v.put(DbContract.RoomTypeAmenityTable.ROOM_TYPE_ID, typeIds[t]);
                v.put(DbContract.RoomTypeAmenityTable.AMENITY_ID, amenityIds[a]);
                db.insert(DbContract.RoomTypeAmenityTable.TABLE_NAME, null, v);
            }
        }
    }

    /* ---------- 3.4 Phòng vật lý (120 phòng) ---------- */

    /**
     * Sinh đủ 120 phòng bằng vòng lặp, phân bổ trạng thái đúng như bản thiết kế:
     * 45 trống · 35 đã đặt · 30 đang ở · 8 cần dọn · 2 bảo trì.
     *
     * Đồng thời ghi nhớ id của các phòng xuất hiện trong bản thiết kế
     * (P.312, P.205, P.601, P.101) vào trường specialRoomIds để gán cho đơn mẫu.
     */
    private void seedRooms(SQLiteDatabase db, long[] typeIds) {
        // Mỗi dòng: {chỉ số loại phòng, tầng, số phòng trên tầng đó}
        int[][] layout = {
                {0, 1, 30},   // Standard  → tầng 1, P.101..P.130
                {1, 2, 20},   // Deluxe    → tầng 2, P.201..P.220
                {1, 4, 20},   // Deluxe    → tầng 4, P.401..P.420
                {2, 3, 15},   // Suite     → tầng 3, P.301..P.315
                {2, 5, 10},   // Suite     → tầng 5, P.501..P.510
                {3, 6, 15},   // Villa     → tầng 6, P.601..P.615
                {4, 7, 10}    // Family    → tầng 7, P.701..P.710
        };

        // Danh sách trạng thái đúng số lượng, xáo trộn với hạt giống cố định
        // để lần chạy nào cũng cho kết quả giống nhau (dễ kiểm tra khi chấm bài).
        List<String> statuses = new ArrayList<>(120);
        addRepeat(statuses, RoomStatus.AVAILABLE, 45);
        addRepeat(statuses, RoomStatus.BOOKED, 35);
        addRepeat(statuses, RoomStatus.OCCUPIED, 30);
        addRepeat(statuses, RoomStatus.CLEANING, 8);
        addRepeat(statuses, RoomStatus.MAINTENANCE, 2);
        Collections.shuffle(statuses, new Random(42));

        // Danh sách số phòng theo đúng thứ tự sẽ chèn
        List<String> numbers = new ArrayList<>(120);
        List<Integer> typeIdx = new ArrayList<>(120);
        List<Integer> floors = new ArrayList<>(120);
        for (int[] block : layout) {
            for (int i = 1; i <= block[2]; i++) {
                numbers.add(String.format(Locale.US, "P.%d%02d", block[1], i));
                typeIdx.add(block[0]);
                floors.add(block[1]);
            }
        }

        // 4 phòng xuất hiện trong bản thiết kế phải có trạng thái đúng như mockup.
        // Dùng cách HOÁN ĐỔI để tổng số mỗi trạng thái không đổi.
        forceStatus(statuses, numbers.indexOf("P.101"), RoomStatus.AVAILABLE);
        forceStatus(statuses, numbers.indexOf("P.205"), RoomStatus.OCCUPIED);
        forceStatus(statuses, numbers.indexOf("P.312"), RoomStatus.BOOKED);
        forceStatus(statuses, numbers.indexOf("P.601"), RoomStatus.BOOKED);
        forceStatus(statuses, numbers.indexOf("P.408"), RoomStatus.CLEANING);
        forceStatus(statuses, numbers.indexOf("P.505"), RoomStatus.MAINTENANCE);

        long[] roomIds = new long[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            ContentValues v = new ContentValues();
            v.put(DbContract.RoomTable.ROOM_NUMBER, numbers.get(i));
            v.put(DbContract.RoomTable.FLOOR, floors.get(i));
            v.put(DbContract.RoomTable.ROOM_TYPE_ID, typeIds[typeIdx.get(i)]);
            v.put(DbContract.RoomTable.ROOM_STATUS, statuses.get(i));
            roomIds[i] = db.insert(DbContract.RoomTable.TABLE_NAME, null, v);
        }

        // Ghi nhớ id của 4 phòng đặc biệt để gán cho đơn mẫu ở bước sau
        specialRoomIds = new long[]{
                roomIds[numbers.indexOf("P.312")],
                roomIds[numbers.indexOf("P.205")],
                roomIds[numbers.indexOf("P.601")],
                roomIds[numbers.indexOf("P.101")]
        };
    }

    /** id của P.312, P.205, P.601, P.101 — dùng cho 4 đơn đặt mẫu. */
    private long[] specialRoomIds;

    private void addRepeat(List<String> list, String value, int times) {
        for (int i = 0; i < times; i++) list.add(value);
    }

    /** Đặt trạng thái cho vị trí index bằng cách đổi chỗ với một phần tử khác có sẵn. */
    private void forceStatus(List<String> statuses, int index, String target) {
        if (index < 0 || target.equals(statuses.get(index))) return;
        for (int i = 0; i < statuses.size(); i++) {
            if (i != index && target.equals(statuses.get(i))) {
                statuses.set(i, statuses.get(index));
                statuses.set(index, target);
                return;
            }
        }
    }

    /* ---------- 3.5 Đơn đặt phòng ---------- */

    private void seedBookings(SQLiteDatabase db, long[] userIds, long[] typeIds) {
        long[] prices = {1_500_000L, 2_500_000L, 4_200_000L, 3_800_000L, 5_200_000L};

        // --- 5 đơn xuất hiện trong bản thiết kế ---
        // Ngày tính tương đối so với hôm nay để app lúc nào cũng có đơn sắp tới,
        // đơn đang ở và đơn đã hoàn tất — thay vì cố định vào tháng 9/2025.
        insertBooking(db, userIds[2], typeIds[2], specialRoomIds[0], prices[2],
                DateUtils.daysFromToday(4), 3, 2,
                BookingStatus.PENDING, DateUtils.daysFromToday(-2), PaymentMethod.VNPAY, false);

        insertBooking(db, userIds[1], typeIds[1], specialRoomIds[1], prices[1],
                DateUtils.daysFromToday(-6), 5, 2,
                BookingStatus.CHECKED_IN, DateUtils.daysFromToday(-12), PaymentMethod.MOMO, true);

        insertBooking(db, userIds[3], typeIds[3], specialRoomIds[2], prices[3],
                DateUtils.daysFromToday(9), 3, 3,
                BookingStatus.CONFIRMED, DateUtils.daysFromToday(-40), PaymentMethod.VNPAY, true);

        insertBooking(db, userIds[4], typeIds[0], specialRoomIds[3], prices[0],
                DateUtils.daysFromToday(-32), 2, 2,
                BookingStatus.COMPLETED, DateUtils.daysFromToday(-45), PaymentMethod.CARD, true);

        insertBooking(db, userIds[5], typeIds[3], 0, prices[3],
                DateUtils.daysFromToday(-58), 2, 2,
                BookingStatus.CANCELLED, DateUtils.daysFromToday(-70), PaymentMethod.MOMO, false);

        // --- 26 đơn rải đều 30 ngày gần nhất, để Dashboard có dữ liệu vẽ biểu đồ ---
        // Hai mảng dưới đây được chọn sao cho tổng doanh thu tháng ≈ 285,4 triệu
        // và tổng số đơn trong tháng = 28, khớp với con số trên bản thiết kế.
        int[] typeIdxPattern  = {1, 2, 0, 3, 4, 1, 0, 2, 3, 1, 4, 0, 2, 1, 3, 0, 4, 2, 1, 3, 0, 2, 4, 1, 3, 2};
        int[] nightsPattern   = {7, 5, 4, 5, 2, 3, 5, 2, 1, 3, 2, 4, 3, 2, 1, 5, 2, 3, 2, 4, 1, 3, 2, 2, 3, 2};
        String[] methods = {PaymentMethod.VNPAY, PaymentMethod.MOMO, PaymentMethod.CARD};

        for (int i = 0; i < typeIdxPattern.length; i++) {
            int t = typeIdxPattern[i];
            int nights = nightsPattern[i];
            long createdAt = DateUtils.daysFromToday(-(i + 1));   // rải từ hôm qua về trước
            long checkIn = DateUtils.daysFromToday(-(i + 1) + 2);
            int userIdx = 1 + (i % 6);                            // luân phiên 6 khách
            String status = (checkIn + nights * DateUtils.ONE_DAY < System.currentTimeMillis())
                    ? BookingStatus.COMPLETED
                    : (checkIn < System.currentTimeMillis() ? BookingStatus.CHECKED_IN
                       : BookingStatus.CONFIRMED);
            insertBooking(db, userIds[userIdx], typeIds[t], 0, prices[t],
                    checkIn, nights, 2, status, createdAt, methods[i % 3], true);
        }
    }

    /**
     * Chèn 1 đơn + bản ghi thanh toán tương ứng.
     * Số tiền tính bằng PriceUtils để luôn khớp với công thức dùng ở màn Thanh toán.
     */
    private void insertBooking(SQLiteDatabase db, long userId, long typeId, long roomId,
                               long basePrice, long checkIn, int nights, int guests,
                               String status, long createdAt, String method, boolean paid) {
        long subtotal = basePrice * nights;
        long fee      = PriceUtils.SERVICE_FEE;
        long vat      = PriceUtils.calcVat(subtotal, fee, 0);
        long total    = subtotal + fee + vat;

        ContentValues v = new ContentValues();
        v.put(DbContract.BookingTable.BOOKING_CODE, uniqueCode(db, PriceUtils.generateBookingCode(createdAt)));
        v.put(DbContract.BookingTable.USER_ID, userId);
        v.put(DbContract.BookingTable.ROOM_TYPE_ID, typeId);
        if (roomId > 0) v.put(DbContract.BookingTable.ROOM_ID, roomId);
        v.put(DbContract.BookingTable.CHECK_IN_DATE, checkIn);
        v.put(DbContract.BookingTable.CHECK_OUT_DATE, checkIn + nights * DateUtils.ONE_DAY);
        v.put(DbContract.BookingTable.NUM_NIGHTS, nights);
        v.put(DbContract.BookingTable.NUM_GUESTS, guests);
        v.put(DbContract.BookingTable.ROOM_SUBTOTAL, subtotal);
        v.put(DbContract.BookingTable.SERVICE_FEE, fee);
        v.put(DbContract.BookingTable.VAT_AMOUNT, vat);
        v.put(DbContract.BookingTable.DISCOUNT_AMOUNT, 0);
        v.put(DbContract.BookingTable.TOTAL_AMOUNT, total);
        v.put(DbContract.BookingTable.BOOKING_STATUS, status);
        v.put(DbContract.BookingTable.CREATED_AT, createdAt);
        long bookingId = db.insert(DbContract.BookingTable.TABLE_NAME, null, v);

        if (bookingId > 0 && paid) {
            ContentValues p = new ContentValues();
            p.put(DbContract.PaymentTable.BOOKING_ID, bookingId);
            p.put(DbContract.PaymentTable.METHOD, method);
            p.put(DbContract.PaymentTable.AMOUNT, total);
            p.put(DbContract.PaymentTable.PAYMENT_STATUS, "paid");
            p.put(DbContract.PaymentTable.PAID_AT, createdAt);
            p.put(DbContract.PaymentTable.TXN_REF, "TXN" + createdAt);
            db.insert(DbContract.PaymentTable.TABLE_NAME, null, p);
        }
    }

    /**
     * Mã đơn cơ sở là "LH" + ngày tạo, nên nhiều đơn cùng ngày sẽ trùng nhau.
     * Hàm này thêm hậu tố 01, 02... cho tới khi tìm được mã chưa tồn tại.
     */
    private String uniqueCode(SQLiteDatabase db, String base) {
        String code = base;
        int suffix = 1;
        while (codeExists(db, code)) {
            code = base + String.format(Locale.US, "%02d", suffix++);
        }
        return code;
    }

    private boolean codeExists(SQLiteDatabase db, String code) {
        android.database.Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM bookings WHERE booking_code = ? LIMIT 1",
                    new String[]{ code });
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    /* ---------- 3.6 Đánh giá ---------- */

    /**
     * Số lượng đánh giá theo từng mức sao được chọn sao cho điểm trung bình tính ra
     * đúng bằng con số hiển thị trên bản thiết kế:
     *   Standard 4.7 (56) · Deluxe 4.9 (124) · Suite 4.8 (89) · Villa 5.0 (42) · Family 4.8 (30)
     */
    private void seedReviews(SQLiteDatabase db, long[] userIds, long[] typeIds) {
        // {số 5 sao, 4 sao, 3 sao, 2 sao, 1 sao}
        int[][] dist = {
                {45,  8, 2, 1, 0},   // Standard → 4.73
                {115, 8, 1, 0, 0},   // Deluxe   → 4.92
                {71, 13, 3, 0, 0},   // Suite    → 4.79 (cộng 2 đánh giá có tên bên dưới = 89 lượt)
                {42,  0, 0, 0, 0},   // Villa    → 5.00
                {25,  4, 1, 0, 0}    // Family   → 4.80
        };
        String[] good = {
                "Phòng sạch sẽ, nhân viên phục vụ chu đáo. Sẽ quay lại lần sau.",
                "Vị trí đẹp, view thoáng, bữa sáng phong phú.",
                "Không gian sang trọng, giường êm, cách âm tốt.",
                "Đáng đồng tiền. Hồ bơi rộng và sạch.",
                "Nhận phòng nhanh, lễ tân thân thiện."
        };
        String[] mid = {
                "Phòng ổn nhưng hơi ồn vào buổi sáng.",
                "Tiện nghi đầy đủ, chỉ tiếc là wifi hơi chậm.",
                "Nhìn chung hài lòng, mong cải thiện thêm bữa sáng."
        };

        long now = System.currentTimeMillis();
        int seq = 0;
        for (int t = 0; t < typeIds.length; t++) {
            for (int star = 5; star >= 1; star--) {
                int count = dist[t][5 - star];
                for (int i = 0; i < count; i++) {
                    String comment = star >= 4 ? good[seq % good.length] : mid[seq % mid.length];
                    long createdAt = now - (long) (seq % 300 + 3) * DateUtils.ONE_DAY;
                    insertReview(db, userIds[1 + (seq % 6)], typeIds[t], star, comment, createdAt);
                    seq++;
                }
            }
        }

        // 2 đánh giá có tên xuất hiện trong bản thiết kế — đặt mới nhất để hiện lên đầu
        insertReview(db, userIds[1], typeIds[2], 5,
                "Phòng tuyệt vời, view biển đẹp. Nhân viên nhiệt tình, bữa sáng đa dạng. Sẽ quay lại!",
                now - DateUtils.ONE_DAY);
        insertReview(db, userIds[3], typeIds[2], 5,
                "Không gian sang trọng, giường êm. Minibar đa dạng, sẽ giới thiệu cho bạn bè.",
                now - 2 * DateUtils.ONE_DAY);
    }

    private void insertReview(SQLiteDatabase db, long userId, long typeId,
                              int rating, String comment, long createdAt) {
        ContentValues v = new ContentValues();
        v.put(DbContract.ReviewTable.USER_ID, userId);
        v.put(DbContract.ReviewTable.ROOM_TYPE_ID, typeId);
        v.put(DbContract.ReviewTable.RATING, rating);
        v.put(DbContract.ReviewTable.COMMENT, comment);
        v.put(DbContract.ReviewTable.CREATED_AT, createdAt);
        db.insert(DbContract.ReviewTable.TABLE_NAME, null, v);
    }

    /** Tính lại avg_rating và review_count cho mọi loại phòng bằng 1 câu UPDATE. */
    private void refreshRatingCache(SQLiteDatabase db) {
        db.execSQL(
                "UPDATE room_types SET " +
                        "  avg_rating = IFNULL((SELECT ROUND(AVG(rating), 1) FROM reviews " +
                        "                       WHERE reviews.room_type_id = room_types.id), 0)," +
                        "  review_count = (SELECT COUNT(*) FROM reviews " +
                        "                  WHERE reviews.room_type_id = room_types.id)");
    }

    /* ---------- 3.7 Khuyến mãi, yêu thích, đã xem ---------- */

    private void seedPromotions(SQLiteDatabase db) {
        ContentValues v = new ContentValues();
        v.put(DbContract.PromotionTable.CODE, "LUMINA20");
        v.put(DbContract.PromotionTable.TITLE, "Ưu đãi mùa thu — giảm 20%");
        v.put(DbContract.PromotionTable.DISCOUNT_PERCENT, 20);
        v.put(DbContract.PromotionTable.MAX_DISCOUNT, 2_000_000L);
        v.put(DbContract.PromotionTable.START_DATE, DateUtils.daysFromToday(-30));
        v.put(DbContract.PromotionTable.END_DATE, DateUtils.daysFromToday(60));
        v.put(DbContract.PromotionTable.IS_ACTIVE, 1);
        db.insert(DbContract.PromotionTable.TABLE_NAME, null, v);

        ContentValues v2 = new ContentValues();
        v2.put(DbContract.PromotionTable.CODE, "DELUXE30");
        v2.put(DbContract.PromotionTable.TITLE, "Giảm 30% phòng Deluxe");
        v2.put(DbContract.PromotionTable.DISCOUNT_PERCENT, 30);
        v2.put(DbContract.PromotionTable.MAX_DISCOUNT, 3_000_000L);
        v2.put(DbContract.PromotionTable.START_DATE, DateUtils.daysFromToday(-10));
        v2.put(DbContract.PromotionTable.END_DATE, DateUtils.daysFromToday(45));
        v2.put(DbContract.PromotionTable.IS_ACTIVE, 1);
        db.insert(DbContract.PromotionTable.TABLE_NAME, null, v2);
    }

    private void seedFavoritesAndViews(SQLiteDatabase db, long[] userIds, long[] typeIds) {
        long an = userIds[2];
        addFavorite(db, an, typeIds[2]);
        addFavorite(db, an, typeIds[3]);

        long now = System.currentTimeMillis();
        addRecentView(db, an, typeIds[3], now - 3600_000L);
        addRecentView(db, an, typeIds[4], now - 7200_000L);
        addRecentView(db, an, typeIds[1], now - 10800_000L);
    }

    private void addFavorite(SQLiteDatabase db, long userId, long typeId) {
        ContentValues v = new ContentValues();
        v.put(DbContract.FavoriteTable.USER_ID, userId);
        v.put(DbContract.FavoriteTable.ROOM_TYPE_ID, typeId);
        db.insert(DbContract.FavoriteTable.TABLE_NAME, null, v);
    }

    private void addRecentView(SQLiteDatabase db, long userId, long typeId, long viewedAt) {
        ContentValues v = new ContentValues();
        v.put(DbContract.RecentViewTable.USER_ID, userId);
        v.put(DbContract.RecentViewTable.ROOM_TYPE_ID, typeId);
        v.put(DbContract.RecentViewTable.VIEWED_AT, viewedAt);
        db.insert(DbContract.RecentViewTable.TABLE_NAME, null, v);
    }
}
