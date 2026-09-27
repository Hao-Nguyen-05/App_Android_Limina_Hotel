package com.lumina.hotel.util;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lumina.hotel.R;

/**
 * Ánh xạ trạng thái booking/room (lưu trong CSDL bằng mã tiếng Anh, VD: "pending")
 * sang nhãn tiếng Việt + màu chữ + màu nền, dùng CHUNG cho:
 * item_booking_card, item_admin_booking, item_admin_room.
 *
 * Bảng ánh xạ đúng theo BLOCK DB — KHÔNG tự thêm/bớt trạng thái.
 */
public final class StatusUtils {

    private StatusUtils() {
        // no instance
    }

    /**
     * @param tv     TextView badge trạng thái (VD: tvStatus, tvBookingStatus)
     * @param status Giá trị cột booking_status trong CSDL:
     *               pending | confirmed | checked_in | completed | cancelled
     */
    public static void applyBookingStatus(@NonNull TextView tv, @Nullable String status) {
        if (status == null) status = "";

        String label;
        int textColorRes;
        int bgRes;

        switch (status) {
            case "pending":
                label = tv.getContext().getString(R.string.booking_status_pending); // "Chờ xác nhận"
                textColorRes = R.color.status_orange_text;
                bgRes = R.drawable.bg_status_orange;
                break;
            case "confirmed":
                label = tv.getContext().getString(R.string.booking_status_confirmed); // "Đã xác nhận"
                textColorRes = R.color.status_blue_text;
                bgRes = R.drawable.bg_status_blue;
                break;
            case "checked_in":
                label = tv.getContext().getString(R.string.booking_status_checked_in); // "Đang ở"
                textColorRes = R.color.status_purple_text;
                bgRes = R.drawable.bg_status_purple;
                break;
            case "completed":
                label = tv.getContext().getString(R.string.booking_status_completed); // "Đã hoàn tất"
                textColorRes = R.color.status_green_text;
                bgRes = R.drawable.bg_status_green;
                break;
            case "cancelled":
                label = tv.getContext().getString(R.string.booking_status_cancelled); // "Đã hủy"
                textColorRes = R.color.status_red_text;
                bgRes = R.drawable.bg_status_red;
                break;
            default:
                // Trạng thái lạ/không xác định — vẫn hiện gì đó, không để badge trống hoặc crash
                label = status;
                textColorRes = R.color.muted;
                bgRes = R.drawable.bg_status_orange;
                break;
        }

        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), textColorRes));
        tv.setBackgroundResource(bgRes);
    }

    /**
     * @param tv     TextView badge trạng thái phòng (VD: tvRoomStatus)
     * @param status Giá trị cột room_status trong CSDL:
     *               available | booked | occupied | cleaning | maintenance
     */
    public static void applyRoomStatus(@NonNull TextView tv, @Nullable String status) {
        if (status == null) status = "";

        String label;
        int textColorRes;
        int bgRes;

        switch (status) {
            case "available":
                label = tv.getContext().getString(R.string.room_status_available); // "TRỐNG"
                textColorRes = R.color.status_green_text;
                bgRes = R.drawable.bg_status_green;
                break;
            case "booked":
                label = tv.getContext().getString(R.string.room_status_booked); // "ĐÃ ĐẶT"
                textColorRes = R.color.status_blue_text;
                bgRes = R.drawable.bg_status_blue;
                break;
            case "occupied":
                label = tv.getContext().getString(R.string.room_status_occupied); // "ĐANG Ở"
                textColorRes = R.color.status_purple_text;
                bgRes = R.drawable.bg_status_purple;
                break;
            case "cleaning":
                label = tv.getContext().getString(R.string.room_status_cleaning); // "CẦN DỌN"
                textColorRes = R.color.status_orange_text;
                bgRes = R.drawable.bg_status_orange;
                break;
            case "maintenance":
                label = tv.getContext().getString(R.string.room_status_maintenance); // "BẢO TRÌ"
                textColorRes = R.color.status_red_text;
                bgRes = R.drawable.bg_status_red;
                break;
            default:
                label = status;
                textColorRes = R.color.muted;
                bgRes = R.drawable.bg_status_orange;
                break;
        }

        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), textColorRes));
        tv.setBackgroundResource(bgRes);
    }
}