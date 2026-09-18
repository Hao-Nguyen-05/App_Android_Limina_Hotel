package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.RecentViewTable;
import com.lumina.hotel.data.DbContract.RoomTypeTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.RoomType;

import java.util.ArrayList;
import java.util.List;

/**
 * Thao tác với bảng recent_views — khối "Đã xem gần đây" ở Trang chủ.
 */
public class RecentViewDao {

    private final DatabaseHelper helper;

    public RecentViewDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    /**
     * Ghi nhận lượt xem. Bảng có ràng buộc UNIQUE(user_id, room_type_id) nên dùng
     * CONFLICT_REPLACE: xem lại một phòng cũ chỉ cập nhật thời điểm, không tạo dòng mới.
     */
    public void upsert(int userId, int typeId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(RecentViewTable.USER_ID, userId);
        v.put(RecentViewTable.ROOM_TYPE_ID, typeId);
        v.put(RecentViewTable.VIEWED_AT, System.currentTimeMillis());
        db.insertWithOnConflict(RecentViewTable.TABLE_NAME, null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Các loại phòng khách vừa xem, mới nhất trước. */
    public List<RoomType> getRecent(int userId, int limit) {
        List<RoomType> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT t.* FROM room_types t " +
                    "JOIN recent_views v ON v.room_type_id = t.id " +
                    "WHERE v.user_id = ? AND t.is_active = 1 " +
                    "ORDER BY v.viewed_at DESC LIMIT ?",
                    new String[]{ String.valueOf(userId), String.valueOf(limit) });
            while (c.moveToNext()) {
                RoomType t = new RoomType();
                t.setId(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.ID)));
                t.setTypeName(c.getString(c.getColumnIndexOrThrow(RoomTypeTable.TYPE_NAME)));
                t.setAreaM2(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.AREA_M2)));
                t.setMaxGuests(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.MAX_GUESTS)));
                t.setBasePrice(c.getLong(c.getColumnIndexOrThrow(RoomTypeTable.BASE_PRICE)));
                t.setImageUrl(c.getString(c.getColumnIndexOrThrow(RoomTypeTable.IMAGE_URL)));
                t.setAvgRating(c.getFloat(c.getColumnIndexOrThrow(RoomTypeTable.AVG_RATING)));
                t.setReviewCount(c.getInt(c.getColumnIndexOrThrow(RoomTypeTable.REVIEW_COUNT)));
                list.add(t);
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }
}
