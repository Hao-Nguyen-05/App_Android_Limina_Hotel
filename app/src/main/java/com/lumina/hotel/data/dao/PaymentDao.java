package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract.PaymentTable;
import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.Payment;

/**
 * Thao tác với bảng payments.
 * Bản ghi thanh toán thường được tạo cùng lúc với đơn (xem BookingDao.createBooking),
 * lớp này dùng khi cần tra cứu hoặc hoàn tiền riêng lẻ.
 */
public class PaymentDao {

    private final DatabaseHelper helper;

    public PaymentDao(Context context) {
        this.helper = new DatabaseHelper(context);
    }

    public Payment getByBookingId(int bookingId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM payments WHERE booking_id = ? " +
                            "ORDER BY id DESC LIMIT 1",
                    new String[]{ String.valueOf(bookingId) });
            return c.moveToFirst() ? cursorToPayment(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    public long insert(int bookingId, String method, long amount, String status) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put(PaymentTable.BOOKING_ID, bookingId);
        v.put(PaymentTable.METHOD, method);
        v.put(PaymentTable.AMOUNT, amount);
        v.put(PaymentTable.PAYMENT_STATUS, status);
        v.put(PaymentTable.PAID_AT, now);
        v.put(PaymentTable.TXN_REF, "TXN" + now);
        return db.insert(PaymentTable.TABLE_NAME, null, v);
    }

    /** Đánh dấu hoàn tiền khi đơn bị huỷ. */
    public int markRefunded(int bookingId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(PaymentTable.PAYMENT_STATUS, "refunded");
        return db.update(PaymentTable.TABLE_NAME, v, "booking_id = ?",
                new String[]{ String.valueOf(bookingId) });
    }

    private Payment cursorToPayment(Cursor c) {
        Payment p = new Payment();
        p.setId(c.getInt(c.getColumnIndexOrThrow(PaymentTable.ID)));
        p.setBookingId(c.getInt(c.getColumnIndexOrThrow(PaymentTable.BOOKING_ID)));
        p.setMethod(c.getString(c.getColumnIndexOrThrow(PaymentTable.METHOD)));
        p.setAmount(c.getLong(c.getColumnIndexOrThrow(PaymentTable.AMOUNT)));
        p.setPaymentStatus(c.getString(c.getColumnIndexOrThrow(PaymentTable.PAYMENT_STATUS)));
        p.setPaidAt(c.getLong(c.getColumnIndexOrThrow(PaymentTable.PAID_AT)));
        p.setTxnRef(c.getString(c.getColumnIndexOrThrow(PaymentTable.TXN_REF)));
        return p;
    }
}
