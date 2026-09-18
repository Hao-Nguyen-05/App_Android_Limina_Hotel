package com.lumina.hotel.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Tiện ích ngày tháng dùng chung.
 *
 * QUY ƯỚC CỦA DỰ ÁN: mọi cột ngày trong CSDL lưu kiểu long (epoch millis) và đã được
 * chuẩn hoá về 00:00:00 giờ địa phương. Nhờ vậy phép trừ 2 mốc ngày luôn ra số đêm
 * tròn, không bị lệch vì giờ phút.
 */
public final class DateUtils {

    public static final long ONE_DAY = 24L * 60 * 60 * 1000;

    @SuppressWarnings("deprecation")
    private static final Locale VN = new Locale("vi", "VN");private DateUtils() { }

    /** Đưa một mốc thời gian bất kỳ về 00:00:00 cùng ngày. */
    public static long atStartOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** 00:00:00 của hôm nay. */
    public static long today() {
        return atStartOfDay(System.currentTimeMillis());
    }

    /** 00:00:00 của ngày cách hôm nay {@code days} ngày (số âm = quá khứ). */
    public static long daysFromToday(int days) {
        return today() + days * ONE_DAY;
    }

    /** Mốc 00:00:00 của một ngày cụ thể. month tính từ 1 (1 = tháng Một). */
    public static long dateOf(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /** Mốc 00:00:00 của ngày cách hôm nay {@code months} tháng về trước. */
    public static long monthsAgo(int months) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(today());
        c.add(Calendar.MONTH, -months);
        return c.getTimeInMillis();
    }

    /** Ngày đầu tiên của tháng chứa mốc {@code millis}, lúc 00:00:00. */
    public static long startOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(atStartOfDay(millis));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    /** Mốc cuối cùng của tháng chứa {@code millis} (23:59:59.999). */
    public static long endOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfMonth(millis));
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis() - 1;
    }

    /**
     * Số đêm giữa 2 mốc ngày. Ví dụ 20/09 → 23/09 trả về 3.
     * Luôn chuẩn hoá trước khi trừ để không bị lệch 1 đêm do chênh giờ.
     */
    public static int nightsBetween(long checkIn, long checkOut) {
        long diff = atStartOfDay(checkOut) - atStartOfDay(checkIn);
        return (int) Math.max(0, Math.round((double) diff / ONE_DAY));
    }

    /** "20/09" */
    public static String formatDayMonth(long millis) {
        return new SimpleDateFormat("dd/MM", VN).format(new Date(millis));
    }

    /** "20/09/2025" */
    public static String formatFull(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", VN).format(new Date(millis));
    }

    /** "01/2024" — dùng cho dòng "Thành viên từ ...". */
    public static String formatMonthYear(long millis) {
        return new SimpleDateFormat("MM/yyyy", VN).format(new Date(millis));
    }

    /** "20250913" — phần ngày trong mã đơn. */
    public static String formatCodeDate(long millis) {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(millis));
    }

    /** "Thứ 7", "Chủ nhật"... */
    public static String weekdayVi(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return "Thứ 2";
            case Calendar.TUESDAY:   return "Thứ 3";
            case Calendar.WEDNESDAY: return "Thứ 4";
            case Calendar.THURSDAY:  return "Thứ 5";
            case Calendar.FRIDAY:    return "Thứ 6";
            case Calendar.SATURDAY:  return "Thứ 7";
            default:                 return "Chủ nhật";
        }
    }

    /** Đêm cuối tuần (Thứ 7 hoặc Chủ nhật) — dùng cho phụ thu +20%. */
    public static boolean isWeekendNight(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int d = c.get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SATURDAY || d == Calendar.SUNDAY;
    }

    /** "Chào buổi sáng," / "Chào buổi chiều," / "Chào buổi tối," theo giờ hiện tại. */
    public static String greetingByHour() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return "Chào buổi sáng,";
        if (hour < 17) return "Chào buổi chiều,";
        return "Chào buổi tối,";
    }
}
