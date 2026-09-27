package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemLatestBookingBinding;
import com.lumina.hotel.model.BookingBrief;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.StatusUtils;

import java.util.Locale;
import java.util.Objects;

/** Danh sách "Đơn mới nhất" trên Dashboard Admin. */
public class LatestBookingAdapter extends ListAdapter<BookingBrief, LatestBookingAdapter.VH> {

    public interface OnItemClickListener { void onItemClick(BookingBrief item); }

    @Nullable private OnItemClickListener listener;

    public LatestBookingAdapter() {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull BookingBrief a, @NonNull BookingBrief b) {
                return a.getId() == b.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull BookingBrief a, @NonNull BookingBrief b) {
                return Objects.equals(a.getGuestName(), b.getGuestName())
                        && Objects.equals(a.getRoomTypeName(), b.getRoomTypeName())
                        && Objects.equals(a.getCheckInLabel(), b.getCheckInLabel())
                        && Objects.equals(a.getStatus(), b.getStatus())
                        && a.getNumNights() == b.getNumNights()
                        && a.getTotalAmount() == b.getTotalAmount();
            }
        });
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener l) { this.listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemLatestBookingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BookingBrief item = getItem(position);
        ItemLatestBookingBinding b = h.binding;

        String name = item.getGuestName() == null ? "" : item.getGuestName().trim();
        b.tvGuestName.setText(name);
        b.tvGuestInitial.setText(initialOf(name));

        b.tvBookingMeta.setText(b.getRoot().getContext().getString(R.string.dash_booking_meta,
                item.getRoomTypeName(), item.getCheckInLabel(), item.getNumNights()));
        b.tvBookingAmount.setText(PriceUtils.formatVnd(item.getTotalAmount()));

        if (item.getStatus() == null) {
            b.tvBookingStatus.setVisibility(View.GONE);
        } else {
            b.tvBookingStatus.setVisibility(View.VISIBLE);
            StatusUtils.applyBookingStatus(b.tvBookingStatus, item.getStatus());
        }

        b.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    /** Tên Việt Nam: lấy chữ cái đầu của TÊN (từ cuối), ví dụ "Nguyễn Văn An" → "A". */
    private static String initialOf(String fullName) {
        if (fullName.isEmpty()) return "?";
        String[] parts = fullName.split("\\s+");
        String last = parts[parts.length - 1];
        return last.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    public static class VH extends RecyclerView.ViewHolder {
        final ItemLatestBookingBinding binding;
        VH(ItemLatestBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}