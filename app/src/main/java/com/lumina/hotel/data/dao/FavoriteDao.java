package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.FavoriteTable;
import com.lumina.hotel.data.DatabaseHelper;

/**
 * Thao tác với bảng favorites — nút trái tim ở màn Chi tiết phòng.
 */
public class FavoriteDao {

    private final DatabaseHelper helper;

    public FavoriteDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    public boolean isFavorite(int userId, int typeId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM favorites WHERE user_id = ? AND room_type_id = ? LIMIT 1",
                    new String[]{ String.valueOf(userId), String.valueOf(typeId) });
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Bật/tắt yêu thích.
     * @return true nếu sau thao tác là ĐANG yêu thích (để đổi icon trái tim).
     */
    public boolean toggle(int userId, int typeId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (isFavorite(userId, typeId)) {
            db.delete(FavoriteTable.TABLE_NAME, "user_id = ? AND room_type_id = ?",
                    new String[]{ String.valueOf(userId), String.valueOf(typeId) });
            return false;
        }
        ContentValues v = new ContentValues();
        v.put(FavoriteTable.USER_ID, userId);
        v.put(FavoriteTable.ROOM_TYPE_ID, typeId);
        db.insert(FavoriteTable.TABLE_NAME, null, v);
        return true;
    }
}
