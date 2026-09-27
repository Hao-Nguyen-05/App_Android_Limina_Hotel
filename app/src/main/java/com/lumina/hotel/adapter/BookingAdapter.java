package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lumina.hotel.data.DbContract.BookingStatus;
import com.lumina.hotel.databinding.ItemBookingCardBinding;
import com.lumina.hotel.model.BookingWithDetail;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.StatusUtils;

/**
 * SPEC 07 — Đơn của tôi. Nút hành động đổi theo booking_status, đúng bảng trong SPEC:
 *   pending    → "Chi tiết →" + nút phụ "Hủy đơn" (đỏ)
 *   confirmed  → chỉ "Chi tiết →"
 *   completed  → "Đánh giá" (ẩn/disable nếu đã đánh giá)
 *   cancelled  → không có nút, card giảm alpha 0.7
 */
public class BookingAdapter extends ListAdapter<BookingWithDetail, BookingAdapter.VH> {

    public interface Listener {
        void onDetailClick(BookingWithDetail item);
        void onCancelClick(BookingWithDetail item);
        void onReviewClick(BookingWithDetail item);
    }

    private final Listener listener;

    public BookingAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<BookingWithDetail> DIFF =
            new DiffUtil.ItemCallback<BookingWithDetail>() {
        @Override
        public boolean areItemsTheSame(@NonNull BookingWithDetail a, @NonNull BookingWithDetail b) {
            return a.getBooking().getId() == b.getBooking().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BookingWithDetail a, @NonNull BookingWithDetail b) {
            return a.getBooking().getBookingStatus().equals(b.getBooking().getBookingStatus())
                    && a.isReviewed() == b.isReviewed()
                    && a.getBooking().getTotalAmount() == b.getBooking().getTotalAmount();
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookingCardBinding b = ItemBookingCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemBookingCardBinding b;

        VH(ItemBookingCardBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(BookingWithDetail item, Listener listener) {
            Glide.with(b.ivThumbnail).load(item.getImageUrl()).into(b.ivThumbnail);
            b.tvRoomName.setText(item.getTypeName());
            b.tvBookingCode.setText(b.getRoot().getContext()
                    .getString(com.lumina.hotel.R.string.bookings_code_prefix,
                            item.getBooking().getBookingCode()));
            b.tvDateRange.setText(b.getRoot().getContext()
                    .getString(com.lumina.hotel.R.string.bookings_date_range,
                            DateUtils.formatDayMonth(item.getBooking().getCheckInDate()),
                            DateUtils.formatDayMonth(item.getBooking().getCheckOutDate()),
                            item.getBooking().getNumNights()));
            b.tvTotal.setText(PriceUtils.formatVnd(item.getBooking().getTotalAmount()));
            StatusUtils.applyBookingStatus(b.tvStatus, item.getBooking().getBookingStatus());

            String status = item.getBooking().getBookingStatus();
            boolean isCancelled = BookingStatus.CANCELLED.equals(status);
            boolean isPending = BookingStatus.PENDING.equals(status);
            boolean isConfirmed = BookingStatus.CONFIRMED.equals(status);
            boolean isCompleted = BookingStatus.COMPLETED.equals(status);

            b.getRoot().setAlpha(isCancelled ? 0.7f : 1f);

            // Nút phụ "Hủy đơn": chỉ pending mới cho hủy trước khi bấm vào (còn điều
            // kiện check_in trong tương lai được lọc từ trước khi truyền dữ liệu vào đây).
            b.btnCancel.setVisibility(isPending ? View.VISIBLE : View.GONE);
            b.btnCancel.setOnClickListener(v -> listener.onCancelClick(item));

            if (isCancelled) {
                b.btnAction.setVisibility(View.GONE);
            } else if (isPending || isConfirmed) {
                b.btnAction.setVisibility(View.VISIBLE);
                b.btnAction.setText(b.getRoot().getContext()
                        .getString(com.lumina.hotel.R.string.bookings_btn_detail) + " →");
                b.btnAction.setEnabled(true);
                b.btnAction.setOnClickListener(v -> listener.onDetailClick(item));
            } else if (isCompleted) {
                b.btnAction.setVisibility(View.VISIBLE);
                if (item.isReviewed()) {
                    b.btnAction.setText(com.lumina.hotel.R.string.bookings_btn_reviewed);
                    b.btnAction.setEnabled(false);
                    b.btnAction.setOnClickListener(null);
                } else {
                    b.btnAction.setText(com.lumina.hotel.R.string.bookings_btn_review);
                    b.btnAction.setEnabled(true);
                    b.btnAction.setOnClickListener(v -> listener.onReviewClick(item));
                }
            } else {
                b.btnAction.setVisibility(View.GONE);
            }
        }
    }
}
