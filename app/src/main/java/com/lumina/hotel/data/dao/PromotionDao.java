package com.lumina.hotel.data.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.PromotionTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.Promotion;

/**
 * Thao tác với bảng promotions.
 * Dùng cho: banner ưu đãi ở Trang chủ và ô "Mã giảm giá" ở màn Thanh toán.
 */
public class PromotionDao {

    private final DatabaseHelper helper;

    public PromotionDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    /**
     * Tìm mã giảm giá còn hiệu lực. Không phân biệt hoa thường để khách gõ kiểu nào
     * cũng nhận (UPPER áp cho cả 2 vế).
     * @return null nếu mã sai, đã tắt, hoặc ngoài khoảng ngày hiệu lực.
     */
    public Promotion findValidCode(String code, long now) {
        if (code == null) return null;
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT * FROM promotions WHERE UPPER(code) = UPPER(?) " +
                    "AND is_active = 1 AND ? BETWEEN start_date AND end_date LIMIT 1",
                    new String[]{ code.trim(), String.valueOf(now) });
            return c.moveToFirst() ? cursorToPromotion(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Khuyến mãi mới nhất còn hiệu lực — hiển thị trên banner Trang chủ. */
    public Promotion getCurrentBanner(long now) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT * FROM promotions WHERE is_active = 1 " +
                    "AND ? BETWEEN start_date AND end_date " +
                    "ORDER BY discount_percent DESC LIMIT 1",
                    new String[]{ String.valueOf(now) });
            return c.moveToFirst() ? cursorToPromotion(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    private Promotion cursorToPromotion(Cursor c) {
        Promotion p = new Promotion();
        p.setId(c.getInt(c.getColumnIndexOrThrow(PromotionTable.ID)));
        p.setCode(c.getString(c.getColumnIndexOrThrow(PromotionTable.CODE)));
        p.setTitle(c.getString(c.getColumnIndexOrThrow(PromotionTable.TITLE)));
        p.setDiscountPercent(c.getInt(c.getColumnIndexOrThrow(PromotionTable.DISCOUNT_PERCENT)));
        p.setMaxDiscount(c.getLong(c.getColumnIndexOrThrow(PromotionTable.MAX_DISCOUNT)));
        p.setStartDate(c.getLong(c.getColumnIndexOrThrow(PromotionTable.START_DATE)));
        p.setEndDate(c.getLong(c.getColumnIndexOrThrow(PromotionTable.END_DATE)));
        p.setActive(c.getInt(c.getColumnIndexOrThrow(PromotionTable.IS_ACTIVE)) == 1);
        return p;
    }
}
