package com.lumina.hotel.data.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DatabaseHelper;
import com.lumina.hotel.model.BookingBrief;
import com.lumina.hotel.model.TypeShareItem;
import com.lumina.hotel.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardDao {
    private final DatabaseHelper helper;
    public DashboardDao(Context context) { this.helper = new DatabaseHelper(context); }

    private static final String REVENUE_STATUSES = "('confirmed','checked_in','completed')";

    public long getRevenue(long from, long to) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT COALESCE(SUM(total_amount),0) FROM bookings " +
                            "WHERE booking_status IN " + REVENUE_STATUSES + " AND created_at BETWEEN ? AND ?",
                    new String[]{ String.valueOf(from), String.valueOf(to) });
            return c.moveToFirst() ? c.getLong(0) : 0;
        } finally { if (c != null) c.close(); }
    }

    public int getNewBookingsCount(long from, long to) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE created_at BETWEEN ? AND ?",
                    new String[]{ String.valueOf(from), String.valueOf(to) });
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally { if (c != null) c.close(); }
    }

    /** [0]=tỷ lệ lấp phòng %, [1]=số phòng trống — trạng thái hiện tại, không phụ thuộc khoảng ngày. */
    public float[] getOccupancyAndVacant() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT " +
                    "(SELECT COUNT(*) FROM rooms), " +
                    "(SELECT COUNT(*) FROM rooms WHERE room_status IN ('booked','occupied')), " +
                    "(SELECT COUNT(*) FROM rooms WHERE room_status = 'available')", null);
            if (c.moveToFirst()) {
                int total = c.getInt(0), occupied = c.getInt(1), vacant = c.getInt(2);
                float rate = total > 0 ? (occupied * 100f / total) : 0f;
                return new float[]{ rate, vacant };
            }
            return new float[]{ 0f, 0f };
        } finally { if (c != null) c.close(); }
    }

    /** Doanh thu theo ngày trong khoảng, cho LineChart (2 list song song: nhãn ngày + giá trị). */
    public void getRevenueSeries(long from, long to, List<String> outLabels, List<Long> outValues) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT strftime('%d/%m', created_at/1000, 'unixepoch', 'localtime') AS label, " +
                            "SUM(total_amount) AS revenue FROM bookings " +
                            "WHERE booking_status IN " + REVENUE_STATUSES + " AND created_at BETWEEN ? AND ? " +
                            "GROUP BY label ORDER BY MIN(created_at) ASC",
                    new String[]{ String.valueOf(from), String.valueOf(to) });
            while (c.moveToNext()) {
                outLabels.add(c.getString(c.getColumnIndexOrThrow("label")));
                outValues.add(c.getLong(c.getColumnIndexOrThrow("revenue")));
            }
        } finally { if (c != null) c.close(); }
    }

    public List<TypeShareItem> getTypeShare(long from, long to) {
        List<TypeShareItem> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT rt.type_name AS name, COUNT(*) AS cnt, " +
                            "ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM bookings WHERE created_at BETWEEN ? AND ?)) AS pct " +
                            "FROM bookings b JOIN room_types rt ON b.room_type_id = rt.id " +
                            "WHERE b.created_at BETWEEN ? AND ? " +
                            "GROUP BY rt.id ORDER BY cnt DESC",
                    new String[]{ String.valueOf(from), String.valueOf(to), String.valueOf(from), String.valueOf(to) });
            while (c.moveToNext()) {
                list.add(new TypeShareItem(
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getInt(c.getColumnIndexOrThrow("pct")),
                        c.getInt(c.getColumnIndexOrThrow("cnt"))));
            }
        } finally { if (c != null) c.close(); }
        return list;
    }

    public List<BookingBrief> getLatestBookings(int limit) {
        List<BookingBrief> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT b.id, u.full_name AS guest_name, rt.type_name AS type_name, " +
                            "b.check_in_date, b.num_nights, b.total_amount, b.booking_status " +
                            "FROM bookings b JOIN users u ON b.user_id = u.id " +
                            "JOIN room_types rt ON b.room_type_id = rt.id " +
                            "ORDER BY b.created_at DESC LIMIT ?",
                    new String[]{ String.valueOf(limit) });
            while (c.moveToNext()) {
                BookingBrief item = new BookingBrief();
                item.setId(c.getInt(c.getColumnIndexOrThrow("id")));
                item.setGuestName(c.getString(c.getColumnIndexOrThrow("guest_name")));
                item.setRoomTypeName(c.getString(c.getColumnIndexOrThrow("type_name")));
                item.setCheckInLabel(DateUtils.formatDayMonth(c.getLong(c.getColumnIndexOrThrow("check_in_date"))));
                item.setStatus(c.getString(c.getColumnIndexOrThrow("booking_status")));
                item.setNumNights(c.getInt(c.getColumnIndexOrThrow("num_nights")));
                item.setTotalAmount(c.getLong(c.getColumnIndexOrThrow("total_amount")));
                list.add(item);
            }
        } finally { if (c != null) c.close(); }
        return list;
    }
}