package com.lumina.hotel.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.lumina.hotel.data.DbContract;
import com.lumina.hotel.model.User;

/**
 * Lưu phiên đăng nhập vào SharedPreferences để lần mở app sau không phải đăng nhập lại.
 *
 * Chỉ lưu những thông tin nhẹ và cần dùng thường xuyên (id, vai trò, tên, email).
 * Mọi dữ liệu khác luôn đọc lại từ CSDL để không bị cũ.
 */
public class SessionManager {

    private static final String PREF_NAME = "lumina_session";
    private static final String KEY_USER_ID   = "user_id";
    private static final String KEY_ROLE      = "role";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL     = "email";

    public static final int NO_USER = -1;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(User user) {
        prefs.edit()
                .putInt(KEY_USER_ID, user.getId())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_FULL_NAME, user.getFullName())
                .putString(KEY_EMAIL, user.getEmail())
                .apply();
    }

    public int getUserId()      { return prefs.getInt(KEY_USER_ID, NO_USER); }
    public String getRole()     { return prefs.getString(KEY_ROLE, DbContract.Role.CUSTOMER); }
    public String getFullName() { return prefs.getString(KEY_FULL_NAME, ""); }
    public String getEmail()    { return prefs.getString(KEY_EMAIL, ""); }

    public boolean isLoggedIn() { return getUserId() != NO_USER; }
    public boolean isAdmin()    { return DbContract.Role.ADMIN.equals(getRole()); }

    /** Gọi khi người dùng sửa hồ sơ, để Trang chủ hiển thị tên mới ngay. */
    public void updateName(String fullName) {
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    /** Đăng xuất: xoá sạch phiên. */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
