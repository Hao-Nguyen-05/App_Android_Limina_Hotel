package com.lumina.hotel.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Set;
import java.util.TreeSet;

/**
 * Bộ lọc nâng cao của màn Admin — Đơn đặt phòng (Phụ lục F).
 * Kết hợp AND với chip trạng thái. Lưu bằng chuỗi ghép thủ công (không dùng Gson).
 * Khoảng ngày áp dụng cho bookings.check_in_date (đã chuẩn hoá 00:00 giờ địa phương).
 */
public class BookingFilter {

    public static final String METHOD_CASH = "cash";
    public static final String METHOD_VNPAY = "vnpay";
    public static final String METHOD_MOMO = "momo";
    public static final String METHOD_CARD = "card";

    /** Thứ tự hiển thị chip phương thức trong bottom sheet. */
    public static final String[] ALL_METHODS = {
            METHOD_CASH, METHOD_VNPAY, METHOD_MOMO, METHOD_CARD
    };

    private static final String PREFS_NAME = "lumina_filters";
    private static final String PREF_KEY = "filter_admin_booking";

    private final Set<String> methods = new TreeSet<>();
    private long dateFrom;      // 0 = không giới hạn
    private long dateTo;        // 0 = không giới hạn
    private String keyword = "";

    public Set<String> getMethods() { return methods; }

    public long getDateFrom() { return dateFrom; }
    public long getDateTo() { return dateTo; }
    public boolean hasDateRange() { return dateFrom > 0 || dateTo > 0; }

    /** Tự đổi chỗ nếu from > to; truyền 0 cho đầu không giới hạn. */
    public void setDateRange(long from, long to) {
        if (from > 0 && to > 0 && from > to) {
            long tmp = from;
            from = to;
            to = tmp;
        }
        this.dateFrom = Math.max(from, 0);
        this.dateTo = Math.max(to, 0);
    }

    public void clearDateRange() {
        dateFrom = 0;
        dateTo = 0;
    }

    public String getKeyword() { return keyword; }

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
    }

    public boolean isActive() {
        return !methods.isEmpty() || hasDateRange() || !keyword.isEmpty();
    }

    public void reset() {
        methods.clear();
        clearDateRange();
        keyword = "";
    }

    public BookingFilter copy() {
        BookingFilter c = new BookingFilter();
        c.methods.addAll(methods);
        c.dateFrom = dateFrom;
        c.dateTo = dateTo;
        c.keyword = keyword;
        return c;
    }

    // ---------------- LƯU / ĐỌC (chuỗi ghép thủ công) ----------------

    /** Ví dụ: methods=cash,momo;from=1758300000000;to=1758900000000;q=Nguy%E1%BB%85n */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        if (!methods.isEmpty()) {
            sb.append("methods=").append(TextUtils.join(",", methods));
        }
        if (dateFrom > 0) {
            if (sb.length() > 0) sb.append(';');
            sb.append("from=").append(dateFrom);
        }
        if (dateTo > 0) {
            if (sb.length() > 0) sb.append(';');
            sb.append("to=").append(dateTo);
        }
        if (!keyword.isEmpty()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("q=").append(Uri.encode(keyword));
        }
        return sb.toString();
    }

    public static BookingFilter parse(String raw) {
        BookingFilter f = new BookingFilter();
        if (raw == null || raw.trim().isEmpty()) return f;

        for (String part : raw.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String key = part.substring(0, eq);
            String value = part.substring(eq + 1);

            if ("methods".equals(key)) {
                for (String m : value.split(",")) {
                    if (isKnownMethod(m)) f.methods.add(m);
                }
            } else if ("from".equals(key)) {
                f.dateFrom = parseLong(value);
            } else if ("to".equals(key)) {
                f.dateTo = parseLong(value);
            } else if ("q".equals(key)) {
                f.setKeyword(Uri.decode(value));
            }
        }
        return f;
    }

    public static BookingFilter load(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return parse(prefs.getString(PREF_KEY, ""));
    }

    /** Filter rỗng thì xoá key (tương đương "Đặt lại"). */
    public void save(Context context) {
        SharedPreferences.Editor editor = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (isActive()) editor.putString(PREF_KEY, serialize());
        else editor.remove(PREF_KEY);
        editor.apply();
    }

    private static boolean isKnownMethod(String m) {
        for (String known : ALL_METHODS) {
            if (known.equals(m)) return true;
        }
        return false;
    }

    private static long parseLong(String s) {
        try {
            return Math.max(Long.parseLong(s), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}