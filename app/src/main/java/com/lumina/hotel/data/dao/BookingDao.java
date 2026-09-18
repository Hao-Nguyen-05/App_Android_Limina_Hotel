package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.BookingStatus;
import com.lumina.hotel.data.DbContract.BookingTable;
import com.lumina.hotel.data.DbContract.PaymentTable;
import com.lumina.hotel.data.DbContract.RoomStatus;
import com.lumina.hotel.data.DbContract.RoomTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.Booking;
import com.lumina.hotel.model.BookingWithDetail;
import com.lumina.hotel.model.RevenuePoint;
import com.lumina.hotel.model.TypeShare;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.PriceUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thao tác với bảng bookings — DAO quan trọng nhất của dự án.
 * Dùng cho: Thanh toán, Đơn của tôi, Quản lý đơn đặt (admin), Dashboard.
 */
public class BookingDao {

    private final DatabaseHelper helper;

    public BookingDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    /* ================================================================
     * 1. KIỂM TRA CÒN PHÒNG (quy tắc nghiệp vụ cốt lõi)
     * ================================================================ */

    /**
     * Đếm số đơn đang chiếm phòng của loại {@code typeId} trong khoảng ngày yêu cầu.
     *
     * Hai khoảng ngày KHÔNG giao nhau khi:
     *     checkOut_mới <= check_in_date    HOẶC    checkIn_mới >= check_out_date
     * Phủ định điều kiện đó ra được các đơn CÓ giao nhau.
     *
     * Chỉ tính các đơn còn hiệu lực (pending, confirmed, checked_in) — đơn đã huỷ
     * hoặc đã hoàn tất thì phòng đã được giải phóng.
     */
    public int getOverlappingCount(int typeId, long checkIn, long checkOut) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT COUNT(*) FROM bookings " +
                            "WHERE room_type_id = ? " +
                            "  AND booking_status IN ('pending','confirmed','checked_in') " +
                            "  AND NOT (? <= check_in_date OR ? >= check_out_date)",
                    new String[]{ String.valueOf(typeId),
                            String.valueOf(DateUtils.atStartOfDay(checkOut)),
                            String.valueOf(DateUtils.atStartOfDay(checkIn)) });
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Loại phòng còn nhận đặt trong khoảng ngày này không?
     * So số đơn đang chiếm với tổng số phòng vật lý của loại đó.
     */
    public boolean isAvailable(int typeId, int totalRooms, long checkIn, long checkOut) {
        return getOverlappingCount(typeId, checkIn, checkOut) < totalRooms;
    }

    /* ================================================================
     * 2. TẠO ĐƠN
     * ================================================================ */

    /**
     * Tạo đơn mới kèm bản ghi thanh toán, trong một giao dịch.
     * Nếu bất kỳ bước nào hỏng thì toàn bộ bị huỷ, không để lại đơn mồ côi.
     *
     * @return id đơn vừa tạo, hoặc -1 nếu thất bại.
     */
    public long createBooking(Booking b, String paymentMethod) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            if (b.getCreatedAt() <= 0) b.setCreatedAt(now);
            String code = uniqueCode(db, PriceUtils.generateBookingCode(b.getCreatedAt()));

            ContentValues v = new ContentValues();
            v.put(BookingTable.BOOKING_CODE, code);
            v.put(BookingTable.USER_ID, b.getUserId());
            v.put(BookingTable.ROOM_TYPE_ID, b.getRoomTypeId());
            if (b.getRoomId() > 0) v.put(BookingTable.ROOM_ID, b.getRoomId());
            v.put(BookingTable.CHECK_IN_DATE, DateUtils.atStartOfDay(b.getCheckInDate()));
            v.put(BookingTable.CHECK_OUT_DATE, DateUtils.atStartOfDay(b.getCheckOutDate()));
            v.put(BookingTable.NUM_NIGHTS, b.getNumNights());
            v.put(BookingTable.NUM_GUESTS, b.getNumGuests());
            v.put(BookingTable.ROOM_SUBTOTAL, b.getRoomSubtotal());
            v.put(BookingTable.SERVICE_FEE, b.getServiceFee());
            v.put(BookingTable.VAT_AMOUNT, b.getVatAmount());
            v.put(BookingTable.DISCOUNT_AMOUNT, b.getDiscountAmount());
            v.put(BookingTable.TOTAL_AMOUNT, b.getTotalAmount());
            v.put(BookingTable.BOOKING_STATUS, b.getBookingStatus() == null
                    ? BookingStatus.PENDING : b.getBookingStatus());
            v.put(BookingTable.NOTE, b.getNote());
            v.put(BookingTable.CREATED_AT, b.getCreatedAt());

            long bookingId = db.insert(BookingTable.TABLE_NAME, null, v);
            if (bookingId == -1) return -1;   // finally vẫn chạy, giao dịch bị huỷ

            if (paymentMethod != null) {
                ContentValues p = new ContentValues();
                p.put("booking_id", bookingId);
                p.put("method", paymentMethod);
                p.put("amount", b.getTotalAmount());
                p.put("payment_status", "paid");
                p.put("paid_at", now);
                p.put("txn_ref", "TXN" + now);
                if (db.insert("payments", null, p) == -1) return -1;
            }

            db.setTransactionSuccessful();
            b.setId((int) bookingId);
            b.setBookingCode(code);
            return bookingId;
        } finally {
            db.endTransaction();
        }
    }

    /* ================================================================
     * 3. ĐỌC
     * ================================================================ */

    private static final String SELECT_WITH_DETAIL =
            "SELECT b.*, t.type_name AS type_name, t.image_url AS image_url, " +
                    "       r.room_number AS room_number, u.full_name AS customer_name, " +
                    "       u.phone AS customer_phone, " +
                    "       (SELECT COUNT(*) FROM reviews rv WHERE rv.booking_id = b.id) AS reviewed " +
                    "FROM bookings b " +
                    "JOIN room_types t ON t.id = b.room_type_id " +
                    "JOIN users u      ON u.id = b.user_id " +
                    "LEFT JOIN rooms r ON r.id = b.room_id ";

    /** Đơn của một khách — màn "Đơn của tôi". */
    public List<BookingWithDetail> getByUser(int userId) {
        return queryDetail(SELECT_WITH_DETAIL + "WHERE b.user_id = ? ORDER BY b.created_at DESC",
                new String[]{ String.valueOf(userId) });
    }

    /** Toàn bộ đơn — màn Quản lý đơn đặt của admin. */
    public List<BookingWithDetail> getAllWithDetail(String status) {
        if (status == null || status.isEmpty() || "all".equals(status)) {
            return queryDetail(SELECT_WITH_DETAIL + "ORDER BY b.created_at DESC", null);
        }
        return queryDetail(SELECT_WITH_DETAIL + "WHERE b.booking_status = ? " +
                "ORDER BY b.created_at DESC", new String[]{ status });
    }

    /** 4 đơn gần nhất — khối "Đơn mới nhất" trên Dashboard. */
    public List<BookingWithDetail> getLatest(int limit) {
        return queryDetail(SELECT_WITH_DETAIL + "ORDER BY b.created_at DESC LIMIT ?",
                new String[]{ String.valueOf(limit) });
    }

    public BookingWithDetail getDetailById(int bookingId) {
        List<BookingWithDetail> list = queryDetail(
                SELECT_WITH_DETAIL + "WHERE b.id = ? LIMIT 1",
                new String[]{ String.valueOf(bookingId) });
        return list.isEmpty() ? null : list.get(0);
    }

    public Booking getByCode(String code) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM bookings WHERE booking_code = ? LIMIT 1",
                    new String[]{ code });
            return c.moveToFirst() ? cursorToBooking(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Đếm đơn theo từng trạng thái — dùng cho các chip lọc có số. */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> map = new LinkedHashMap<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        int total = 0;
        try {
            c = db.rawQuery("SELECT booking_status, COUNT(*) AS cnt FROM bookings " +
                    "GROUP BY booking_status", null);
            while (c.moveToNext()) {
                int cnt = c.getInt(c.getColumnIndexOrThrow("cnt"));
                map.put(c.getString(c.getColumnIndexOrThrow("booking_status")), cnt);
                total += cnt;
            }
        } finally {
            if (c != null) c.close();
        }
        map.put("all", total);
        return map;
    }

    /* ================================================================
     * 4. CHUYỂN TRẠNG THÁI (phần CRUD của admin)
     * ================================================================ */

    public int updateStatus(int bookingId, String status) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(BookingTable.BOOKING_STATUS, status);
        return db.update(BookingTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(bookingId) });
    }

    /** Khách tự huỷ đơn. Chỉ cho phép khi đơn còn ở trạng thái chờ/đã xác nhận. */
    public int cancelByCustomer(int bookingId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(BookingTable.BOOKING_STATUS, BookingStatus.CANCELLED);
        return db.update(BookingTable.TABLE_NAME, v,
                "id = ? AND booking_status IN ('pending','confirmed')",
                new String[]{ String.valueOf(bookingId) });
    }

    /** Admin từ chối đơn, có ghi lý do vào cột note. */
    public int rejectBooking(int bookingId, String reason) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(BookingTable.BOOKING_STATUS, BookingStatus.CANCELLED);
        v.put(BookingTable.NOTE, reason);
        return db.update(BookingTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(bookingId) });
    }

    /**
     * Check-in: gán phòng vật lý cho đơn và đổi trạng thái phòng sang "đang ở".
     * Hai thao tác này phải cùng thành công hoặc cùng thất bại.
     */
    public boolean checkIn(int bookingId, int roomId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues b = new ContentValues();
            b.put(BookingTable.ROOM_ID, roomId);
            b.put(BookingTable.BOOKING_STATUS, BookingStatus.CHECKED_IN);
            int r1 = db.update(BookingTable.TABLE_NAME, b, "id = ?",
                    new String[]{ String.valueOf(bookingId) });

            ContentValues rm = new ContentValues();
            rm.put("room_status", RoomStatus.OCCUPIED);
            int r2 = db.update("rooms", rm, "id = ?",
                    new String[]{ String.valueOf(roomId) });

            if (r1 > 0 && r2 > 0) {
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** Check-out: đơn chuyển sang hoàn tất, phòng chuyển sang "cần dọn". */
    public boolean checkOut(int bookingId, int roomId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues b = new ContentValues();
            b.put(BookingTable.BOOKING_STATUS, BookingStatus.COMPLETED);
            int r1 = db.update(BookingTable.TABLE_NAME, b, "id = ?",
                    new String[]{ String.valueOf(bookingId) });

            if (roomId > 0) {
                ContentValues rm = new ContentValues();
                rm.put("room_status", RoomStatus.CLEANING);
                db.update("rooms", rm, "id = ?", new String[]{ String.valueOf(roomId) });
            }

            if (r1 > 0) {
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public int delete(int bookingId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(BookingTable.TABLE_NAME, "id = ?",
                new String[]{ String.valueOf(bookingId) });
    }

    /* ================================================================
     * 5. THỐNG KÊ CHO DASHBOARD
     * ================================================================ */

    /** Chỉ tính đơn không bị huỷ. */
    private static final String REVENUE_FILTER =
            "booking_status IN ('confirmed','checked_in','completed')";

    public long revenueBetween(long from, long to) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT IFNULL(SUM(total_amount), 0) FROM bookings " +
                            "WHERE " + REVENUE_FILTER + " AND created_at BETWEEN ? AND ?",
                    new String[]{ String.valueOf(from), String.valueOf(to) });
            return c.moveToFirst() ? c.getLong(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    public int countBookingsBetween(long from, long to) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE created_at BETWEEN ? AND ?",
                    new String[]{ String.valueOf(from), String.valueOf(to) });
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Doanh thu gom theo ngày — dữ liệu cho biểu đồ đường.
     *
     * SQLite không hiểu epoch millis, phải chia 1000 và khai báo 'unixepoch',
     * thêm 'localtime' để gom theo ngày ở múi giờ Việt Nam thay vì UTC.
     */
    public List<RevenuePoint> revenueByDay(long from, long to) {
        List<RevenuePoint> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT strftime('%d/%m', created_at/1000, 'unixepoch', 'localtime') AS label, " +
                            "       SUM(total_amount) AS revenue, MIN(created_at) AS first_at " +
                            "FROM bookings " +
                            "WHERE " + REVENUE_FILTER + " AND created_at BETWEEN ? AND ? " +
                            "GROUP BY label ORDER BY first_at ASC",
                    new String[]{ String.valueOf(from), String.valueOf(to) });
            while (c.moveToNext()) {
                list.add(new RevenuePoint(
                        c.getString(c.getColumnIndexOrThrow("label")),
                        c.getLong(c.getColumnIndexOrThrow("revenue"))));
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    /** Tỷ trọng số đơn theo loại phòng — 5 thanh ngang trên Dashboard. */
    public List<TypeShare> shareByRoomType(long from, long to) {
        List<TypeShare> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT t.type_name AS type_name, COUNT(b.id) AS cnt " +
                            "FROM bookings b JOIN room_types t ON t.id = b.room_type_id " +
                            "WHERE b." + REVENUE_FILTER + " AND b.created_at BETWEEN ? AND ? " +
                            "GROUP BY t.id ORDER BY cnt DESC",
                    new String[]{ String.valueOf(from), String.valueOf(to) });

            int total = 0;
            List<String> names = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            while (c.moveToNext()) {
                int cnt = c.getInt(c.getColumnIndexOrThrow("cnt"));
                names.add(c.getString(c.getColumnIndexOrThrow("type_name")));
                counts.add(cnt);
                total += cnt;
            }
            for (int i = 0; i < names.size(); i++) {
                int percent = total == 0 ? 0 : Math.round(counts.get(i) * 100f / total);
                list.add(new TypeShare(names.get(i), counts.get(i), percent));
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    /**
     * Tỷ lệ lấp phòng: số phòng đang có khách hoặc đã được giữ / tổng số phòng.
     * @return số phần trăm đã làm tròn (0..100)
     */
    public int occupancyRate() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT (SELECT COUNT(*) FROM rooms WHERE room_status IN ('booked','occupied')) " +
                            "AS busy, (SELECT COUNT(*) FROM rooms) AS total", null);
            if (c.moveToFirst()) {
                int busy = c.getInt(c.getColumnIndexOrThrow("busy"));
                int total = c.getInt(c.getColumnIndexOrThrow("total"));
                return total == 0 ? 0 : Math.round(busy * 100f / total);
            }
            return 0;
        } finally {
            if (c != null) c.close();
        }
    }

    /* ================================================================
     * 6. HÀM DÙNG CHUNG TRONG LỚP
     * ================================================================ */

    /** Mã đơn "LH"+ngày có thể trùng khi nhiều đơn cùng ngày → thêm hậu tố 01, 02... */
    private String uniqueCode(SQLiteDatabase db, String base) {
        String code = base;
        int suffix = 1;
        while (codeExists(db, code)) {
            code = base + String.format(Locale.US, "%02d", suffix++);
        }
        return code;
    }

    private boolean codeExists(SQLiteDatabase db, String code) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM bookings WHERE booking_code = ? LIMIT 1",
                    new String[]{ code });
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    private List<BookingWithDetail> queryDetail(String sql, String[] args) {
        List<BookingWithDetail> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            while (c.moveToNext()) {
                BookingWithDetail d = new BookingWithDetail();
                d.setBooking(cursorToBooking(c));
                d.setTypeName(c.getString(c.getColumnIndexOrThrow("type_name")));
                d.setImageUrl(c.getString(c.getColumnIndexOrThrow("image_url")));
                d.setRoomNumber(c.getString(c.getColumnIndexOrThrow("room_number")));
                d.setCustomerName(c.getString(c.getColumnIndexOrThrow("customer_name")));
                d.setCustomerPhone(c.getString(c.getColumnIndexOrThrow("customer_phone")));
                d.setReviewed(c.getInt(c.getColumnIndexOrThrow("reviewed")) > 0);
                list.add(d);
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    private Booking cursorToBooking(Cursor c) {
        Booking b = new Booking();
        b.setId(c.getInt(c.getColumnIndexOrThrow(BookingTable.ID)));
        b.setBookingCode(c.getString(c.getColumnIndexOrThrow(BookingTable.BOOKING_CODE)));
        b.setUserId(c.getInt(c.getColumnIndexOrThrow(BookingTable.USER_ID)));
        b.setRoomTypeId(c.getInt(c.getColumnIndexOrThrow(BookingTable.ROOM_TYPE_ID)));
        int roomIdx = c.getColumnIndexOrThrow(BookingTable.ROOM_ID);
        b.setRoomId(c.isNull(roomIdx) ? 0 : c.getInt(roomIdx));   // cột này cho phép NULL
        b.setCheckInDate(c.getLong(c.getColumnIndexOrThrow(BookingTable.CHECK_IN_DATE)));
        b.setCheckOutDate(c.getLong(c.getColumnIndexOrThrow(BookingTable.CHECK_OUT_DATE)));
        b.setNumNights(c.getInt(c.getColumnIndexOrThrow(BookingTable.NUM_NIGHTS)));
        b.setNumGuests(c.getInt(c.getColumnIndexOrThrow(BookingTable.NUM_GUESTS)));
        b.setRoomSubtotal(c.getLong(c.getColumnIndexOrThrow(BookingTable.ROOM_SUBTOTAL)));
        b.setServiceFee(c.getLong(c.getColumnIndexOrThrow(BookingTable.SERVICE_FEE)));
        b.setVatAmount(c.getLong(c.getColumnIndexOrThrow(BookingTable.VAT_AMOUNT)));
        b.setDiscountAmount(c.getLong(c.getColumnIndexOrThrow(BookingTable.DISCOUNT_AMOUNT)));
        b.setTotalAmount(c.getLong(c.getColumnIndexOrThrow(BookingTable.TOTAL_AMOUNT)));
        b.setBookingStatus(c.getString(c.getColumnIndexOrThrow(BookingTable.BOOKING_STATUS)));
        b.setNote(c.getString(c.getColumnIndexOrThrow(BookingTable.NOTE)));
        b.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(BookingTable.CREATED_AT)));
        return b;
    }
}