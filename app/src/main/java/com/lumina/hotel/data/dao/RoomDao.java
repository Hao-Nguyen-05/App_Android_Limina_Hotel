package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.lumina.hotel.model.RoomFilter;
import com.lumina.hotel.model.RoomType;


import com.lumina.hotel.data.DbContract.RoomTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.Room;
import com.lumina.hotel.model.RoomWithType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thao tác với bảng rooms (phòng vật lý).
 * Dùng cho: màn Quản lý phòng của admin và bước gán phòng khi check-in.
 */
public class RoomDao {

    private final DatabaseHelper helper;

    public RoomDao(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    // ---------------- ĐỌC ----------------

    /** Giữ lại để các chỗ gọi cũ không lỗi: không áp bộ lọc nâng cao. */
    public List<RoomWithType> getRoomsWithType(String status) {
        return getRoomsWithType(status, null);
    }

    /**
     * Danh sách phòng kèm tên loại và giá, sắp theo tầng rồi số phòng.
     * @param status lọc theo trạng thái; null hoặc "all" để lấy tất cả.
     * @param filter bộ lọc nâng cao (loại phòng, tầng); null hoặc rỗng = không lọc thêm.
     */
    public List<RoomWithType> getRoomsWithType(String status, RoomFilter filter) {
        List<RoomWithType> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            conditions.add("r.room_status = ?");
            args.add(status);
        }
        addFilterConditions(conditions, args, filter);

        String sql = "SELECT r.*, t.type_name AS type_name, t.base_price AS base_price " +
                "FROM rooms r JOIN room_types t ON t.id = r.room_type_id" +
                buildWhere(conditions) +
                " ORDER BY r.floor ASC, r.room_number ASC";

        Cursor c = null;
        try {
            c = db.rawQuery(sql, args.toArray(new String[0]));
            while (c.moveToNext()) {
                RoomWithType rt = new RoomWithType();
                rt.setRoom(cursorToRoom(c));
                rt.setTypeName(c.getString(c.getColumnIndexOrThrow("type_name")));
                rt.setBasePrice(c.getLong(c.getColumnIndexOrThrow("base_price")));
                list.add(rt);
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    public Room getById(int roomId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM rooms WHERE id = ? LIMIT 1",
                    new String[]{ String.valueOf(roomId) });
            return c.moveToFirst() ? cursorToRoom(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Đếm toàn bộ kho phòng (không áp bộ lọc nâng cao). */
    public Map<String, Integer> countByStatus() {
        return countByStatus(null);
    }

    /**
     * Đếm số phòng theo từng trạng thái bằng MỘT truy vấn GROUP BY,
     * chỉ tính những phòng khớp bộ lọc loại phòng / tầng (filter null = đếm tất cả).
     * @return map trạng thái → số lượng, kèm khoá "all" là tổng.
     */
    public Map<String, Integer> countByStatus(RoomFilter filter) {
        Map<String, Integer> map = new LinkedHashMap<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();
        addFilterConditions(conditions, args, filter);

        String sql = "SELECT r.room_status AS room_status, COUNT(*) AS cnt FROM rooms r" +
                buildWhere(conditions) + " GROUP BY r.room_status";

        Cursor c = null;
        int total = 0;
        try {
            c = db.rawQuery(sql, args.toArray(new String[0]));
            while (c.moveToNext()) {
                int cnt = c.getInt(c.getColumnIndexOrThrow("cnt"));
                map.put(c.getString(c.getColumnIndexOrThrow(RoomTable.ROOM_STATUS)), cnt);
                total += cnt;
            }
        } finally {
            if (c != null) c.close();
        }
        map.put("all", total);
        return map;
    }

    public int countAllRooms() {
        return querySingleInt("SELECT COUNT(*) FROM rooms", null);
    }

    public int countByOneStatus(String status) {
        return querySingleInt("SELECT COUNT(*) FROM rooms WHERE room_status = ?",
                new String[]{ status });
    }

    /**
     * Danh sách phòng trống thuộc một loại — dùng ở bước check-in để admin chọn phòng.
     * Trả về rỗng nghĩa là không còn phòng nào để gán.
     */
    public List<Room> getAvailableRoomsOfType(int typeId) {
        List<Room> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM rooms WHERE room_type_id = ? " +
                            "AND room_status IN ('available','booked') " +
                            "ORDER BY room_number ASC",
                    new String[]{ String.valueOf(typeId) });
            while (c.moveToNext()) list.add(cursorToRoom(c));
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    /** Kiểm tra số phòng đã tồn tại chưa (gọi trước khi thêm phòng mới). */
    public boolean isRoomNumberExists(String roomNumber, int excludeId) {
        return querySingleInt(
                "SELECT COUNT(*) FROM rooms WHERE room_number = ? AND id <> ?",
                new String[]{ roomNumber, String.valueOf(excludeId) }) > 0;
    }

    /** Các loại phòng đang có phòng vật lý (id + tên) — để sinh chip "Loại phòng" trong bộ lọc. */
    public List<RoomType> getTypeOptions() {
        List<RoomType> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT DISTINCT t.id AS id, t.type_name AS type_name " +
                    "FROM room_types t JOIN rooms r ON r.room_type_id = t.id " +
                    "ORDER BY t.type_name ASC", null);
            while (c.moveToNext()) {
                RoomType t = new RoomType();
                t.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                t.setTypeName(c.getString(c.getColumnIndexOrThrow("type_name")));
                list.add(t);
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    /** Các tầng đang có phòng, tăng dần — để sinh chip "Tầng" trong bộ lọc. */
    public List<Integer> getFloorOptions() {
        List<Integer> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT DISTINCT floor FROM rooms ORDER BY floor ASC", null);
            while (c.moveToNext()) list.add(c.getInt(0));
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    // ---------------- GHI ----------------

    public long insert(Room r) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.insert(RoomTable.TABLE_NAME, null, toValues(r));
    }

    public int update(Room r) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.update(RoomTable.TABLE_NAME, toValues(r), "id = ?",
                new String[]{ String.valueOf(r.getId()) });
    }

    public int updateStatus(int roomId, String status) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(RoomTable.ROOM_STATUS, status);
        return db.update(RoomTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(roomId) });
    }

    public int delete(int roomId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(RoomTable.TABLE_NAME, "id = ?",
                new String[]{ String.valueOf(roomId) });
    }

    // ---------------- HÀM DÙNG CHUNG ----------------

    /** Thêm điều kiện loại phòng / tầng (IN (?,?,...)) vào danh sách điều kiện và tham số. */
    private void addFilterConditions(List<String> conditions, List<String> args, RoomFilter filter) {
        if (filter == null) return;
        if (!filter.getTypeIds().isEmpty()) {
            conditions.add("r.room_type_id IN (" + placeholders(filter.getTypeIds().size()) + ")");
            for (Integer id : filter.getTypeIds()) args.add(String.valueOf(id));
        }
        if (!filter.getFloors().isEmpty()) {
            conditions.add("r.floor IN (" + placeholders(filter.getFloors().size()) + ")");
            for (Integer f : filter.getFloors()) args.add(String.valueOf(f));
        }
    }

    /** Ghép các điều kiện bằng AND; rỗng thì không có WHERE. */
    private String buildWhere(List<String> conditions) {
        return conditions.isEmpty() ? "" : " WHERE " + TextUtils.join(" AND ", conditions);
    }

    /** n dấu hỏi cách nhau bởi dấu phẩy: "?,?,?" (chỉ chứa ký tự '?', giá trị thật vẫn đi qua args). */
    private String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private ContentValues toValues(Room r) {
        ContentValues v = new ContentValues();
        v.put(RoomTable.ROOM_NUMBER, r.getRoomNumber());
        v.put(RoomTable.FLOOR, r.getFloor());
        v.put(RoomTable.ROOM_TYPE_ID, r.getRoomTypeId());
        v.put(RoomTable.ROOM_STATUS, r.getRoomStatus());
        v.put(RoomTable.NOTE, r.getNote());
        return v;
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

    private Room cursorToRoom(Cursor c) {
        Room r = new Room();
        r.setId(c.getInt(c.getColumnIndexOrThrow(RoomTable.ID)));
        r.setRoomNumber(c.getString(c.getColumnIndexOrThrow(RoomTable.ROOM_NUMBER)));
        r.setFloor(c.getInt(c.getColumnIndexOrThrow(RoomTable.FLOOR)));
        r.setRoomTypeId(c.getInt(c.getColumnIndexOrThrow(RoomTable.ROOM_TYPE_ID)));
        r.setRoomStatus(c.getString(c.getColumnIndexOrThrow(RoomTable.ROOM_STATUS)));
        r.setNote(c.getString(c.getColumnIndexOrThrow(RoomTable.NOTE)));
        return r;
    }
}
