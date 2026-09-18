package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.RoomTypeTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.Amenity;
import com.lumina.hotel.model.RoomType;

import java.util.ArrayList;
import java.util.List;

/**
 * Thao tác với bảng room_types.
 * Dùng cho: Trang chủ, Tìm kiếm, Danh sách phòng, Chi tiết phòng,
 * và màn Quản lý loại phòng của admin.
 */
public class RoomTypeDao {

    private final DatabaseHelper helper;

    public RoomTypeDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    // ---------------- ĐỌC ----------------

    /** Tất cả loại phòng, kể cả loại đang tắt — dùng cho màn admin. */
    public List<RoomType> getAll() {
        return queryList("SELECT * FROM room_types ORDER BY base_price ASC", null);
    }

    /** Chỉ loại phòng đang nhận đặt — dùng cho phía khách. */
    public List<RoomType> getActiveTypes() {
        return queryList("SELECT * FROM room_types WHERE is_active = 1 " +
                "ORDER BY avg_rating DESC", null);
    }

    /** Phòng nổi bật trên Trang chủ: điểm đánh giá cao nhất. */
    public List<RoomType> getFeatured(int limit) {
        return queryList("SELECT * FROM room_types WHERE is_active = 1 " +
                        "ORDER BY avg_rating DESC, review_count DESC LIMIT ?",
                new String[]{ String.valueOf(limit) });
    }

    public RoomType getById(int typeId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM room_types WHERE id = ? LIMIT 1",
                    new String[]{ String.valueOf(typeId) });
            return c.moveToFirst() ? cursorToRoomType(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Tìm kiếm + lọc cho màn Danh sách phòng. Mọi tham số đều có thể bỏ qua:
     * truyền null cho chuỗi và 0 cho số nếu không muốn lọc theo tiêu chí đó.
     *
     * Ở đây phải dựng câu WHERE động, nên dùng StringBuilder + danh sách tham số
     * song song. Tuyệt đối không nối thẳng giá trị vào chuỗi SQL.
     *
     * @param sortBy "price_asc" | "price_desc" | "rating" | "popular"
     */
    public List<RoomType> searchAndFilter(String keyword, String typeName,
                                          long minPrice, long maxPrice,
                                          int numGuests, List<Integer> amenityIds,
                                          String sortBy) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.* FROM room_types t WHERE t.is_active = 1");
        List<String> args = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND t.type_name LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        if (typeName != null && !typeName.trim().isEmpty() && !"all".equals(typeName)) {
            // Chip lọc gửi lên "Deluxe", tên đầy đủ là "Deluxe Ocean View" → dùng LIKE
            sql.append(" AND t.type_name LIKE ?");
            args.add("%" + typeName.trim() + "%");
        }
        if (minPrice > 0) {
            sql.append(" AND t.base_price >= ?");
            args.add(String.valueOf(minPrice));
        }
        if (maxPrice > 0) {
            sql.append(" AND t.base_price <= ?");
            args.add(String.valueOf(maxPrice));
        }
        if (numGuests > 0) {
            sql.append(" AND t.max_guests >= ?");
            args.add(String.valueOf(numGuests));
        }
        if (amenityIds != null && !amenityIds.isEmpty()) {
            // Loại phòng phải có ĐỦ tất cả tiện nghi đã chọn
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < amenityIds.size(); i++) {
                placeholders.append(i == 0 ? "?" : ",?");
                args.add(String.valueOf(amenityIds.get(i)));
            }
            sql.append(" AND (SELECT COUNT(*) FROM room_type_amenities ra ")
               .append("WHERE ra.room_type_id = t.id AND ra.amenity_id IN (")
               .append(placeholders).append(")) = ?");
            args.add(String.valueOf(amenityIds.size()));
        }

        if ("price_desc".equals(sortBy))      sql.append(" ORDER BY t.base_price DESC");
        else if ("rating".equals(sortBy))     sql.append(" ORDER BY t.avg_rating DESC");
        else if ("popular".equals(sortBy))    sql.append(" ORDER BY t.review_count DESC");
        else                                  sql.append(" ORDER BY t.base_price ASC");

        return queryList(sql.toString(), args.toArray(new String[0]));
    }

    /** Danh sách tiện nghi của một loại phòng — dùng cho lưới 3 cột ở màn Chi tiết. */
    public List<Amenity> getAmenitiesOfType(int typeId) {
        List<Amenity> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT a.* FROM amenities a " +
                    "JOIN room_type_amenities ra ON ra.amenity_id = a.id " +
                    "WHERE ra.room_type_id = ? ORDER BY a.id ASC",
                    new String[]{ String.valueOf(typeId) });
            while (c.moveToNext()) {
                list.add(new Amenity(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("amenity_name")),
                        c.getString(c.getColumnIndexOrThrow("icon_key"))));
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    public int countTypes() {
        return querySingleInt("SELECT COUNT(*) FROM room_types", null);
    }

    /** Tổng số phòng của khách sạn — thẻ thống kê ở màn Quản lý loại phòng. */
    public int sumTotalRooms() {
        return querySingleInt("SELECT IFNULL(SUM(total_rooms), 0) FROM room_types", null);
    }

    // ---------------- GHI ----------------

    public long insert(RoomType t) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insert(RoomTypeTable.TABLE_NAME, null, toValues(t));
    }

    public int update(RoomType t) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.update(RoomTypeTable.TABLE_NAME, toValues(t), "id = ?",
                new String[]{ String.valueOf(t.getId()) });
    }

    /** Xoá loại phòng. Nhờ ON DELETE CASCADE, các phòng thuộc loại này cũng bị xoá. */
    public int delete(int typeId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(RoomTypeTable.TABLE_NAME, "id = ?",
                new String[]{ String.valueOf(typeId) });
    }

    /** Có đơn đang hoạt động thuộc loại này không — gọi TRƯỚC khi cho phép xoá. */
    public boolean hasActiveBookings(int typeId) {
        return querySingleInt(
                "SELECT COUNT(*) FROM bookings WHERE room_type_id = ? " +
                "AND booking_status IN ('pending','confirmed','checked_in')",
                new String[]{ String.valueOf(typeId) }) > 0;
    }

    /** Cập nhật lại điểm trung bình và số lượt đánh giá sau khi có review mới. */
    public void refreshRatingCache(int typeId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(
                "UPDATE room_types SET " +
                "  avg_rating = IFNULL((SELECT ROUND(AVG(rating),1) FROM reviews " +
                "                       WHERE room_type_id = ?), 0), " +
                "  review_count = (SELECT COUNT(*) FROM reviews WHERE room_type_id = ?) " +
                "WHERE id = ?",
                new Object[]{ typeId, typeId, typeId });
    }

    public int setActive(int typeId, boolean active) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(RoomTypeTable.IS_ACTIVE, active ? 1 : 0);
        return db.update(RoomTypeTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(typeId) });
    }

    // ---------------- HÀM DÙNG CHUNG TRONG LỚP ----------------

    private ContentValues toValues(RoomType t) {
        ContentValues v = new ContentValues();
        v.put(RoomTypeTable.TYPE_NAME, t.getTypeName());
        v.put(RoomTypeTable.DESCRIPTION, t.getDescription());
        v.put(RoomTypeTable.AREA_M2, t.getAreaM2());
        v.put(RoomTypeTable.MAX_GUESTS, t.getMaxGuests());
        v.put(RoomTypeTable.BASE_PRICE, t.getBasePrice());
        v.put(RoomTypeTable.IMAGE_URL, t.getImageUrl());
        v.put(RoomTypeTable.TOTAL_ROOMS, t.getTotalRooms());
        v.put(RoomTypeTable.IS_ACTIVE, t.isActive() ? 1 : 0);
        return v;
    }

    private List<RoomType> queryList(String sql, String[] args) {
        List<RoomType> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            while (c.moveToNext()) list.add(cursorToRoomType(c));
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    private int querySingleInt(String sql, String[] args) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private RoomType cursorToRoomType(Cursor c) {
        RoomType t = new RoomType();
        t.setId(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.ID)));
        t.setTypeName(c.getString(c.getColumnIndexOrThrow(RoomTypeTable.TYPE_NAME)));
        t.setDescription(c.getString(c.getColumnIndexOrThrow(RoomTypeTable.DESCRIPTION)));
        t.setAreaM2(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.AREA_M2)));
        t.setMaxGuests(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.MAX_GUESTS)));
        t.setBasePrice(c.getLong(c.getColumnIndexOrThrow(RoomTypeTable.BASE_PRICE)));
        t.setImageUrl(c.getString(c.getColumnIndexOrThrow(RoomTypeTable.IMAGE_URL)));
        t.setTotalRooms(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.TOTAL_ROOMS)));
        t.setActive(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.IS_ACTIVE)) == 1);
        t.setAvgRating(c.getFloat(c.getColumnIndexOrThrow(RoomTypeTable.AVG_RATING)));
        t.setReviewCount(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.REVIEW_COUNT)));
        return t;
    }
}
