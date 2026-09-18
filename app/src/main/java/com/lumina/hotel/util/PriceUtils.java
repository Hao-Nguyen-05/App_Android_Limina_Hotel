package com.lumina.hotel.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Công thức tính tiền và định dạng hiển thị — nơi DUY NHẤT trong app được phép
 * tính tiền. Mọi màn hình (Chi tiết phòng, Thanh toán, Đơn của tôi, Dashboard)
 * đều gọi vào đây để không bao giờ lệch số.
 *
 * Kiểm chứng theo bản thiết kế:
 *   4.200.000 x 3 đêm = 12.600.000
 *   + phí dịch vụ 200.000        = 12.800.000
 *   + VAT 8% (1.024.000)         = 13.824.000đ
 */
public final class PriceUtils {

    /** Phí dịch vụ cố định cho mỗi đơn. */
    public static final long SERVICE_FEE = 200_000L;

    /** Thuế VAT 8%. */
    public static final double VAT_RATE = 0.08;

    /** Phụ thu đêm cuối tuần (Thứ 7, Chủ nhật): +20%. */
    public static final double WEEKEND_SURCHARGE = 0.20;

    private PriceUtils() { }

    /** Tiền phòng đơn giản: giá/đêm x số đêm. */
    public static long calcSubtotal(long basePrice, int nights) {
        return basePrice * nights;
    }

    /**
     * Tiền phòng có tính phụ thu cuối tuần: mỗi đêm rơi vào Thứ 7 hoặc Chủ nhật
     * được nhân thêm 20%. Đêm thứ n tính theo ngày checkIn + n.
     */
    public static long calcSubtotalWithWeekend(long basePrice, long checkIn, int nights) {
        long total = 0;
        for (int i = 0; i < nights; i++) {
            long night = checkIn + i * DateUtils.ONE_DAY;
            total += DateUtils.isWeekendNight(night)
                    ? Math.round(basePrice * (1 + WEEKEND_SURCHARGE))
                    : basePrice;
        }
        return total;
    }

    /** Số tiền được giảm theo mã khuyến mãi, có chặn trần. */
    public static long calcDiscount(long subtotal, int discountPercent, long maxDiscount) {
        if (discountPercent <= 0) return 0;
        long raw = subtotal * discountPercent / 100;
        return (maxDiscount > 0) ? Math.min(raw, maxDiscount) : raw;
    }

    /** VAT tính trên (tiền phòng + phí dịch vụ − giảm giá). */
    public static long calcVat(long subtotal, long serviceFee, long discount) {
        long taxable = subtotal + serviceFee - discount;
        if (taxable < 0) taxable = 0;
        return Math.round(taxable * VAT_RATE);
    }

    /** Tổng tiền cuối cùng khách phải trả. */
    public static long calcTotal(long subtotal, long serviceFee, long discount, long vat) {
        return subtotal + serviceFee - discount + vat;
    }

    /** "13.824.000đ" */
    public static String formatVnd(long amount) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        return new DecimalFormat("#,###", sym).format(amount) + "đ";
    }

    /** "13.824.000" (không có chữ đ) */
    public static String formatNumber(long amount) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        return new DecimalFormat("#,###", sym).format(amount);
    }

    /**
     * Rút gọn cho thẻ chỉ số và biểu đồ:
     *   2.500.000     → "2.500K"
     *   285.444.000   → "285.4M"
     *   1.250.000.000 → "1.25B"
     */
    public static String formatShort(long amount) {
        if (amount >= 1_000_000_000L) {
            return trimZero(String.format(Locale.US, "%.2f", amount / 1_000_000_000d)) + "B";
        }
        if (amount >= 1_000_000L) {
            return trimZero(String.format(Locale.US, "%.1f", amount / 1_000_000d)) + "M";
        }
        if (amount >= 1_000L) {
            return formatNumber(amount / 1_000L) + "K";
        }
        return String.valueOf(amount);
    }

    private static String trimZero(String s) {
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** Mã đơn cơ sở: "LH" + yyyyMMdd. DAO sẽ thêm hậu tố nếu trùng. */
    public static String generateBookingCode(long createdAt) {
        return "LH" + DateUtils.formatCodeDate(createdAt);
    }

    /** Dạng hiển thị có dấu thăng: "#LH20250913". */
    public static String displayCode(String bookingCode) {
        return "#" + bookingCode;
    }
}
