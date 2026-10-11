package com.lumina.hotel.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.lumina.hotel.R;
import com.lumina.hotel.data.DbContract.BookingStatus;
import com.lumina.hotel.databinding.ItemAdminBookingBinding;
import com.lumina.hotel.model.BookingWithDetail;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.StatusUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Danh sách đơn đặt phòng (1 cột) ở màn Quản lý đơn đặt (admin) — SPEC 11.
 * Bấm cả card (không trúng nút) -> xem chi tiết (qua setOnItemClickListener của BaseAdapter).
 * 3 nút hành động (Xác nhận / Từ chối / Check-in|Check-out) hiện/ẩn theo booking_status,
 * qua OnBookingActionListener riêng (BaseAdapter không biết về các nút bên trong item).
 */
public class AdminBookingAdapter extends BaseAdapter<BookingWithDetail, ItemAdminBookingBinding> {

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN"));

    /** onActionClick dùng chung cho Check-in VÀ Check-out — Fragment tự phân biệt qua booking_status. */
    public interface OnBookingActionListener {
        void onConfirm(@NonNull BookingWithDetail item);
        void onReject(@NonNull BookingWithDetail item);
        void onActionClick(@NonNull BookingWithDetail item);
    }

    private OnBookingActionListener actionListener;

    private static final DiffUtil.ItemCallback<BookingWithDetail> DIFF =
            new DiffUtil.ItemCallback<BookingWithDetail>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookingWithDetail a, @NonNull BookingWithDetail b) {
                    return a.getBooking().getId() == b.getBooking().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookingWithDetail a, @NonNull BookingWithDetail b) {
                    return TextUtils.equals(a.getBooking().getBookingStatus(), b.getBooking().getBookingStatus())
                            && TextUtils.equals(a.getRoomNumber(), b.getRoomNumber())
                            && TextUtils.equals(a.getBooking().getNote(), b.getBooking().getNote());
                }
            };

    public AdminBookingAdapter() {
        super(DIFF);
    }

    public void setOnBookingActionListener(OnBookingActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    protected ItemAdminBookingBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemAdminBookingBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(@NonNull ItemAdminBookingBinding binding, @NonNull BookingWithDetail item, int position) {
        Context ctx = binding.getRoot().getContext();
        String status = item.getBooking().getBookingStatus();

        binding.tvBookingCode.setText(ctx.getString(R.string.admin_booking_code, item.getBooking().getBookingCode()));
        StatusUtils.applyBookingStatus(binding.tvBookingStatus, status);

        String phone = item.getCustomerPhone();
        binding.tvCustomerInfo.setText(TextUtils.isEmpty(phone)
                ? item.getCustomerName()
                : ctx.getString(R.string.admin_booking_customer_info,
                        item.getCustomerName(), phone));

        String roomPart = item.getRoomNumber() != null
                ? item.getRoomNumber()
                : ctx.getString(R.string.admin_booking_no_room);
        binding.tvRoomInfo.setText(ctx.getString(R.string.admin_booking_room_info,
                item.getTypeName(), roomPart));

        binding.tvDateRange.setText(ctx.getString(R.string.bookings_date_range,
                DATE_FMT.format(item.getBooking().getCheckInDate()),
                DATE_FMT.format(item.getBooking().getCheckOutDate()),
                item.getBooking().getNumNights()));

        binding.tvTotal.setText(PriceUtils.formatVnd(item.getBooking().getTotalAmount()));

        applyActionButtons(binding, item, status);

        // cancelled: card mờ đi, không có nút hành động nào (đã ẩn hết ở applyActionButtons)
        binding.getRoot().setAlpha(BookingStatus.CANCELLED.equals(status) ? 0.7f : 1f);
    }

    private void applyActionButtons(ItemAdminBookingBinding binding, BookingWithDetail item, String status) {
        boolean showConfirmReject = BookingStatus.PENDING.equals(status);
        boolean showAction = BookingStatus.CONFIRMED.equals(status) || BookingStatus.CHECKED_IN.equals(status);

        binding.btnConfirm.setVisibility(showConfirmReject ? View.VISIBLE : View.GONE);
        binding.btnReject.setVisibility(showConfirmReject ? View.VISIBLE : View.GONE);
        binding.btnAction.setVisibility(showAction ? View.VISIBLE : View.GONE);

        if (showConfirmReject) {
            binding.btnConfirm.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onConfirm(item);
            });
            binding.btnReject.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onReject(item);
            });
        }

        if (showAction) {
            binding.btnAction.setText(BookingStatus.CONFIRMED.equals(status)
                    ? R.string.admin_booking_action_checkin
                    : R.string.admin_booking_action_checkout);
            binding.btnAction.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(item);
            });
        }
    }
}
