package com.lumina.hotel.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lumina.hotel.data.DbContract;
import com.lumina.hotel.data.DbContract.UserTable;
import com.lumina.hotel.data.DatabaseHelper;

import com.lumina.hotel.model.CustomerFilter;
import com.lumina.hotel.model.CustomerStat;
import com.lumina.hotel.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Thao tác với bảng users.
 * Dùng cho: Đăng nhập, Đăng ký, Thông tin cá nhân, Quản lý khách hàng (admin).
 */
public class UserDao {

    private final DatabaseHelper helper;

    public UserDao(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    // ---------------- ĐỌC ----------------

    /**
     * Đăng nhập bằng email HOẶC số điện thoại.
     * @return User nếu đúng, null nếu sai tài khoản hoặc mật khẩu.
     */
    public User login(String account, String password) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT * FROM users WHERE (email = ? OR phone = ?) AND password = ? LIMIT 1",
                    new String[]{ account, account, password });
            return c.moveToFirst() ? cursorToUser(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    public User getById(int userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM users WHERE id = ? LIMIT 1",
                    new String[]{ String.valueOf(userId) });
            return c.moveToFirst() ? cursorToUser(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Kiểm tra email đã được đăng ký chưa (gọi TRƯỚC khi insert ở màn Đăng ký). */
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM users WHERE email = ? LIMIT 1",
                    new String[]{ email });
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean isPhoneExists(String phone) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM users WHERE phone = ? LIMIT 1",
                    new String[]{ phone });
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    public int countCustomers() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = ?",
                    new String[]{ DbContract.Role.CUSTOMER });
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Giữ lại cho nơi gọi cũ: không dùng bộ lọc nâng cao. */
    public List<CustomerStat> getCustomerStats(String keyword) {
        return getCustomerStats(keyword, null);
    }

    /**
     * Danh sách khách hàng kèm thống kê, dùng cho màn Quản lý khách hàng.
     * Chỉ tính doanh thu từ các đơn không bị huỷ.
     * Các điều kiện kết hợp AND: từ khoá, lịch sử đặt phòng, hạng VIP, khoảng ngày đăng ký.
     *
     * @param keyword tìm theo tên, số điện thoại hoặc email; null/rỗng = không lọc.
     * @param filter  bộ lọc nâng cao; null hoặc rỗng = không lọc.
     */
    public List<CustomerStat> getCustomerStats(String keyword, CustomerFilter filter) {
        List<CustomerStat> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder sql = new StringBuilder(
                "SELECT u.*, " +
                        "       COUNT(b.id) AS total_bookings, " +
                        "       IFNULL(SUM(CASE WHEN b.booking_status IN ('confirmed','checked_in','completed') " +
                        "                       THEN b.total_amount ELSE 0 END), 0) AS total_spent " +
                        "FROM users u LEFT JOIN bookings b ON b.user_id = u.id " +
                        "WHERE u.role = ?");
        List<String> args = new ArrayList<>();
        args.add(DbContract.Role.CUSTOMER);

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (u.full_name LIKE ? ESCAPE '\\'")
                    .append(" OR u.phone LIKE ? ESCAPE '\\'")
                    .append(" OR u.email LIKE ? ESCAPE '\\')");
            String like = "%" + escapeLike(keyword.trim()) + "%";
            args.add(like); args.add(like); args.add(like);
        }

        if (filter != null) {
            // Lịch sử đặt phòng: EXISTS để không làm đổi COUNT/SUM của phép JOIN phía trên
            if (CustomerFilter.HISTORY_HAS.equals(filter.getHistory())) {
                sql.append(" AND EXISTS (SELECT 1 FROM bookings x WHERE x.user_id = u.id)");
            } else if (CustomerFilter.HISTORY_NONE.equals(filter.getHistory())) {
                sql.append(" AND NOT EXISTS (SELECT 1 FROM bookings x WHERE x.user_id = u.id)");
            }

            // Hạng khách
            if (CustomerFilter.VIP_YES.equals(filter.getVip())) {
                sql.append(" AND u.is_vip = 1");
            } else if (CustomerFilter.VIP_NO.equals(filter.getVip())) {
                sql.append(" AND u.is_vip = 0");
            }

            // Ngày đăng ký: [từ 00:00 ngày đầu, trước 00:00 ngày sau ngày cuối)
            if (filter.getDateFrom() > 0) {
                sql.append(" AND u.created_at >= ?");
                args.add(String.valueOf(filter.getDateFrom()));
            }
            if (filter.getDateTo() > 0) {
                sql.append(" AND u.created_at < ?");
                args.add(String.valueOf(nextDayMidnight(filter.getDateTo())));
            }
        }

        sql.append(" GROUP BY u.id ORDER BY total_spent DESC");

        Cursor c = null;
        try {
            c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
            while (c.moveToNext()) {
                CustomerStat s = new CustomerStat();
                s.setUser(cursorToUser(c));
                s.setTotalBookings(c.getInt(c.getColumnIndexOrThrow("total_bookings")));
                s.setTotalSpent(c.getLong(c.getColumnIndexOrThrow("total_spent")));
                list.add(s);
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    /** Escape ký tự đặc biệt của LIKE (đi kèm ESCAPE '\\' trong câu SQL). */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 00:00 của ngày liền sau ngày chứa mốc thời gian đã cho (giờ địa phương). */
    private static long nextDayMidnight(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    // ---------------- GHI ----------------

    /** @return id vừa tạo, hoặc -1 nếu thất bại (thường do email trùng). */
    public long insert(User u) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(UserTable.FULL_NAME, u.getFullName());
        v.put(UserTable.EMAIL, u.getEmail());
        v.put(UserTable.PHONE, u.getPhone());
        v.put(UserTable.PASSWORD, u.getPassword());
        v.put(UserTable.DOB, u.getDob());
        v.put(UserTable.AVATAR_URL, u.getAvatarUrl());
        v.put(UserTable.ROLE, u.getRole() == null ? DbContract.Role.CUSTOMER : u.getRole());
        v.put(UserTable.IS_VIP, u.isVip() ? 1 : 0);
        v.put(UserTable.CREATED_AT, u.getCreatedAt() > 0
                ? u.getCreatedAt() : System.currentTimeMillis());
        v.put(UserTable.AUTH_PROVIDER, u.getAuthProvider() == null
                ? DbContract.AuthProvider.LOCAL : u.getAuthProvider());
        v.put(UserTable.GOOGLE_ID, u.getGoogleId());
        return db.insert(UserTable.TABLE_NAME, null, v);
    }

    /** Cập nhật họ tên, số điện thoại, ngày sinh. Email không cho đổi. */
    public int updateProfile(int userId, String fullName, String phone, long dob) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(UserTable.FULL_NAME, fullName);
        v.put(UserTable.PHONE, phone);
        v.put(UserTable.DOB, dob);
        return db.update(UserTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(userId) });
    }

    public int updateAvatar(int userId, String avatarUrl) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(UserTable.AVATAR_URL, avatarUrl);
        return db.update(UserTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(userId) });
    }

    /** Đổi mật khẩu, chỉ thành công khi mật khẩu cũ khớp. @return số dòng bị sửa. */
    public int updatePassword(int userId, String oldPassword, String newPassword) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(UserTable.PASSWORD, newPassword);
        return db.update(UserTable.TABLE_NAME, v, "id = ? AND password = ?",
                new String[]{ String.valueOf(userId), oldPassword });
    }

    public int setVip(int userId, boolean vip) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(UserTable.IS_VIP, vip ? 1 : 0);
        return db.update(UserTable.TABLE_NAME, v, "id = ?",
                new String[]{ String.valueOf(userId) });
    }

    /** Tìm user đã từng đăng nhập Google trước đó (theo google_id). */
    public User findByGoogleId(String googleId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM users WHERE google_id = ? LIMIT 1",
                    new String[]{ googleId });
            return c.moveToFirst() ? cursorToUser(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Đăng nhập/Đăng ký bằng Google: tìm theo google_id trước, không có thì nối
     * vào tài khoản local cùng email, không có cả hai thì tạo user mới.
     * Toàn bộ trong 1 transaction để tránh race condition khi đăng nhập nhanh liên tiếp.
     */
    public User loginOrRegisterWithGoogle(String googleId, String email,
                                          String fullName, String avatarUrl) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            User existing = findByGoogleId(googleId);
            if (existing != null) {
                db.setTransactionSuccessful();
                return existing;
            }

            Cursor c = null;
            User byEmail = null;
            try {
                c = db.rawQuery("SELECT * FROM users WHERE email = ? LIMIT 1",
                        new String[]{ email });
                if (c.moveToFirst()) byEmail = cursorToUser(c);
            } finally {
                if (c != null) c.close();
            }

            if (byEmail != null) {
                ContentValues v = new ContentValues();
                v.put(UserTable.AUTH_PROVIDER, DbContract.AuthProvider.GOOGLE);
                v.put(UserTable.GOOGLE_ID, googleId);
                db.update(UserTable.TABLE_NAME, v, "id = ?",
                        new String[]{ String.valueOf(byEmail.getId()) });
                db.setTransactionSuccessful();
                return getById(byEmail.getId());
            }

            User newUser = new User();
            newUser.setFullName(fullName);
            newUser.setEmail(email);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setPassword(null);
            newUser.setRole(DbContract.Role.CUSTOMER);
            newUser.setVip(false);
            newUser.setCreatedAt(System.currentTimeMillis());
            newUser.setAuthProvider(DbContract.AuthProvider.GOOGLE);
            newUser.setGoogleId(googleId);

            long newId = insert(newUser);
            db.setTransactionSuccessful();
            return getById((int) newId);
        } finally {
            db.endTransaction();
        }
    }


    public int delete(int userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(UserTable.TABLE_NAME, "id = ?",
                new String[]{ String.valueOf(userId) });
    }



    // ---------------- CHUYỂN CURSOR → POJO ----------------

    private User cursorToUser(Cursor c) {
        User u = new User();
        u.setId(c.getInt(c.getColumnIndexOrThrow(UserTable.ID)));
        u.setFullName(c.getString(c.getColumnIndexOrThrow(UserTable.FULL_NAME)));
        u.setEmail(c.getString(c.getColumnIndexOrThrow(UserTable.EMAIL)));
        u.setPhone(c.getString(c.getColumnIndexOrThrow(UserTable.PHONE)));
        u.setPassword(c.getString(c.getColumnIndexOrThrow(UserTable.PASSWORD)));
        u.setDob(c.getLong(c.getColumnIndexOrThrow(UserTable.DOB)));
        u.setAvatarUrl(c.getString(c.getColumnIndexOrThrow(UserTable.AVATAR_URL)));
        u.setRole(c.getString(c.getColumnIndexOrThrow(UserTable.ROLE)));
        u.setVip(c.getInt(c.getColumnIndexOrThrow(UserTable.IS_VIP)) == 1);
        u.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(UserTable.CREATED_AT)));
        u.setAuthProvider(c.getString(c.getColumnIndexOrThrow(UserTable.AUTH_PROVIDER)));
        int gIdx = c.getColumnIndexOrThrow(UserTable.GOOGLE_ID);
        u.setGoogleId(c.isNull(gIdx) ? null : c.getString(gIdx));
        return u;
    }
}
