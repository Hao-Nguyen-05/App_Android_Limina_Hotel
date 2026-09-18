package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.AmenityTable;
import com.lumina.hotel.data.DbContract.RoomTypeAmenityTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.Amenity;

import java.util.ArrayList;
import java.util.List;

/**
 * Thao tác với bảng amenities và bảng nối room_type_amenities.
 * Dùng cho: ChipGroup "Tiện nghi mong muốn" ở màn Tìm kiếm và form Thêm/Sửa loại phòng.
 */
public class AmenityDao {

    private final DatabaseHelper helper;

    public AmenityDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    public List<Amenity> getAll() {
        List<Amenity> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM amenities ORDER BY id ASC", null);
            while (c.moveToNext()) {
                list.add(new Amenity(
                        c.getInt(c.getColumnIndexOrThrow(AmenityTable.ID)),
                        c.getString(c.getColumnIndexOrThrow(AmenityTable.AMENITY_NAME)),
                        c.getString(c.getColumnIndexOrThrow(AmenityTable.ICON_KEY))));
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    /** Id các tiện nghi đang gán cho một loại phòng — dùng để tick sẵn chip khi sửa. */
    public List<Integer> getAmenityIdsOfType(int typeId) {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT amenity_id FROM room_type_amenities WHERE room_type_id = ?",
                    new String[]{ String.valueOf(typeId) });
            while (c.moveToNext()) ids.add(c.getInt(0));
        } finally {
            if (c != null) c.close();
        }
        return ids;
    }

    /**
     * Ghi lại toàn bộ tiện nghi của một loại phòng: xoá hết rồi chèn lại danh sách mới.
     * Bọc trong giao dịch để không có trạng thái nửa vời nếu lỗi giữa chừng.
     */
    public void setAmenitiesForType(int typeId, List<Integer> amenityIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(RoomTypeAmenityTable.TABLE_NAME, "room_type_id = ?",
                    new String[]{ String.valueOf(typeId) });
            if (amenityIds != null) {
                for (Integer id : amenityIds) {
                    ContentValues v = new ContentValues();
                    v.put(RoomTypeAmenityTable.ROOM_TYPE_ID, typeId);
                    v.put(RoomTypeAmenityTable.AMENITY_ID, id);
                    db.insert(RoomTypeAmenityTable.TABLE_NAME, null, v);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
