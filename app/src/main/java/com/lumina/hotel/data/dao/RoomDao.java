package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
        this.helper = new DatabaseHelper(context);
    }

    // ---------------- ĐỌC ----------------

    /**
     * Danh sách phòng kèm tên loại và giá, sắp theo số phòng.
     * @param status lọc theo trạng thái; truyền null hoặc "all" để lấy tất cả.
     */
    public List<RoomWithType> getRoomsWithType(String status) {
        List<RoomWithType> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder sql = new StringBuilder(
                "SELECT r.*, t.type_name AS type_name, t.base_price AS base_price " +
                        "FROM rooms r JOIN room_types t ON t.id = r.room_type_id");
        List<String> args = new ArrayList<>();
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            sql.append(" WHERE r.room_status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY r.floor ASC, r.room_number ASC");

        Cursor c = null;
        try {
            c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
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

    /**
     * Đếm số phòng theo từng trạng thái bằng MỘT truy vấn GROUP BY.
     * Dùng cho các chip "Trống · 45", "Đã đặt · 35"...
     * @return map trạng thái → số lượng, kèm khoá "all" là tổng.
     */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> map = new LinkedHashMap<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        int total = 0;
        try {
            c = db.rawQuery("SELECT room_status, COUNT(*) AS cnt FROM rooms " +
                    "GROUP BY room_status", null);
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
