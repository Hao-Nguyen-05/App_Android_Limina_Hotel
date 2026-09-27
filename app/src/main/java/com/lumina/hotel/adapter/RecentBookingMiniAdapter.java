package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lumina.hotel.databinding.ItemLatestBookingBinding;
import com.lumina.hotel.model.Booking;
import com.lumina.hotel.model.BookingWithDetail;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.PriceUtils;

import java.util.List;

/**
 * Tái sử dụng item_latest_booking.xml cho "5 đơn gần nhất" trong BottomSheet
 * hồ sơ khách hàng. Khác bản gốc (Dashboard, hiện tên khách): ở đây đang xem
 * trong hồ sơ 1 khách nên tvGuestInitial/tvGuestName đổi sang TÊN LOẠI PHÒNG,
 * tvBookingMeta đổi sang KHOẢNG NGÀY thay vì meta gốc.
 */
public class RecentBookingMiniAdapter extends RecyclerView.Adapter<RecentBookingMiniAdapter.VH> {

    private final List<BookingWithDetail> items;

    public RecentBookingMiniAdapter(List<BookingWithDetail> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLatestBookingBinding b = ItemLatestBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemLatestBookingBinding b;

        VH(ItemLatestBookingBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(BookingWithDetail d) {
            Booking booking = d.getBooking();
            String typeName = d.getTypeName();

            String initial = (typeName == null || typeName.trim().isEmpty())
                    ? "?" : typeName.trim().substring(0, 1).toUpperCase();
            b.tvGuestInitial.setText(initial);
            b.tvGuestName.setText(typeName);

            String meta = DateUtils.formatDayMonth(booking.getCheckInDate())
                    + " → " + DateUtils.formatDayMonth(booking.getCheckOutDate())
                    + " · " + DateUtils.nightsBetween(booking.getCheckInDate(),
                    booking.getCheckOutDate()) + " đêm";
            b.tvBookingMeta.setText(meta);

            b.tvBookingAmount.setText(PriceUtils.formatVnd(booking.getTotalAmount()));
            b.tvBookingStatus.setText(statusLabel(booking.getBookingStatus()));
        }

        private String statusLabel(String status) {
            if (status == null) return "";
            switch (status) {
                case "pending":    return "Chờ xác nhận";
                case "confirmed":  return "Đã xác nhận";
                case "checked_in": return "Đang ở";
                case "completed":  return "Hoàn tất";
                case "cancelled":  return "Đã huỷ";
                default:           return status;
            }
        }
    }
}