package com.lumina.hotel.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Bộ lọc nâng cao của màn Admin — Khách hàng (Phụ lục F).
 * Kết hợp AND với ô tìm kiếm tên/SĐT/email ở màn chính (từ khoá KHÔNG nằm trong filter này).
 * Lưu bằng chuỗi ghép thủ công (không dùng Gson).
 * Khoảng ngày áp dụng cho users.created_at (mốc thời gian đầy đủ, DAO tự mở rộng đến hết ngày "đến").
 */
public class CustomerFilter {

    /** Lịch sử đặt phòng. */
    public static final String HISTORY_ANY = "";
    public static final String HISTORY_HAS = "has";
    public static final String HISTORY_NONE = "none";

    /** Hạng khách. */
    public static final String VIP_ANY = "";
    public static final String VIP_YES = "yes";
    public static final String VIP_NO = "no";

    private static final String PREFS_NAME = "lumina_filters";
    private static final String PREF_KEY = "filter_admin_customer";

    private String history = HISTORY_ANY;
    private String vip = VIP_ANY;
    private long dateFrom;      // 0 = không giới hạn (00:00 giờ địa phương)
    private long dateTo;        // 0 = không giới hạn (00:00 giờ địa phương của ngày cuối)

    public String getHistory() { return history; }

    public void setHistory(String history) {
        this.history = (HISTORY_HAS.equals(history) || HISTORY_NONE.equals(history))
                ? history : HISTORY_ANY;
    }

    public String getVip() { return vip; }

    public void setVip(String vip) {
        this.vip = (VIP_YES.equals(vip) || VIP_NO.equals(vip)) ? vip : VIP_ANY;
    }

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

    public boolean isActive() {
        return !HISTORY_ANY.equals(history) || !VIP_ANY.equals(vip) || hasDateRange();
    }

    public void reset() {
        history = HISTORY_ANY;
        vip = VIP_ANY;
        clearDateRange();
    }

    public CustomerFilter copy() {
        CustomerFilter c = new CustomerFilter();
        c.history = history;
        c.vip = vip;
        c.dateFrom = dateFrom;
        c.dateTo = dateTo;
        return c;
    }

    // ---------------- LƯU / ĐỌC (chuỗi ghép thủ công) ----------------

    /** Ví dụ: history=has;vip=yes;from=1758300000000;to=1758900000000 */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        if (!HISTORY_ANY.equals(history)) {
            sb.append("history=").append(history);
        }
        if (!VIP_ANY.equals(vip)) {
            if (sb.length() > 0) sb.append(';');
            sb.append("vip=").append(vip);
        }
        if (dateFrom > 0) {
            if (sb.length() > 0) sb.append(';');
            sb.append("from=").append(dateFrom);
        }
        if (dateTo > 0) {
            if (sb.length() > 0) sb.append(';');
            sb.append("to=").append(dateTo);
        }
        return sb.toString();
    }

    public static CustomerFilter parse(String raw) {
        CustomerFilter f = new CustomerFilter();
        if (TextUtils.isEmpty(raw) || raw.trim().isEmpty()) return f;

        for (String part : raw.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String key = part.substring(0, eq);
            String value = part.substring(eq + 1);

            if ("history".equals(key)) {
                f.setHistory(value);
            } else if ("vip".equals(key)) {
                f.setVip(value);
            } else if ("from".equals(key)) {
                f.dateFrom = parseLong(value);
            } else if ("to".equals(key)) {
                f.dateTo = parseLong(value);
            }
        }
        return f;
    }

    public static CustomerFilter load(Context context) {
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

    private static long parseLong(String s) {
        try {
            return Math.max(Long.parseLong(s), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}