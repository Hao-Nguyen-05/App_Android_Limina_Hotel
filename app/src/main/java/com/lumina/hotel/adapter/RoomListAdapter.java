package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.bumptech.glide.Glide;
import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemRoomTypeCardBinding;
import com.lumina.hotel.model.RoomType;
import com.lumina.hotel.util.PriceUtils;
import java.util.Collections;
import java.util.Set;
import java.util.Locale;

/** Card ngang 96dp ảnh — dùng cho danh sách/kết quả tìm kiếm (SPEC 05), khác RoomTypeAdapter (dọc, Trang chủ). */
public class RoomListAdapter extends BaseAdapter<RoomType, ItemRoomTypeCardBinding> {

    public RoomListAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    protected ItemRoomTypeCardBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemRoomTypeCardBinding.inflate(inflater, parent, false);
    }

    private Set<Integer> soldOutIds = Collections.emptySet();

    /** Gọi từ Activity sau khi tính xong getOverlappingCount cho từng loại phòng. */
    public void setSoldOutIds(Set<Integer> ids) {
        this.soldOutIds = (ids != null) ? ids : Collections.emptySet();
        notifyDataSetChanged();
    }

    @Override
    protected void bind(@NonNull ItemRoomTypeCardBinding b, @NonNull RoomType item, int position) {
        Glide.with(b.getRoot())
                .load(item.getImageUrl())
                .centerCrop()
                .into(b.ivRoomImage);

        b.tvTypeName.setText(item.getTypeName());
        b.tvMeta.setText(b.getRoot().getContext().getString(
                R.string.room_meta_format, item.getAreaM2(), item.getMaxGuests()));
        b.tvRating.setText(String.format(Locale.getDefault(), "%.1f", item.getAvgRating()));
        b.tvReviewCount.setText(b.getRoot().getContext().getString(
                R.string.review_count_format, item.getReviewCount()));
        b.tvOldPrice.setVisibility(View.GONE);

        boolean isSoldOut = soldOutIds.contains(item.getId());
        if (isSoldOut) {
            b.tvBadge.setText(R.string.badge_sold_out);
            b.tvBadge.setBackgroundResource(R.drawable.bg_status_red);
            b.tvBadge.setVisibility(View.VISIBLE);
            b.getRoot().setAlpha(0.5f);
            b.getRoot().setClickable(false);
            b.tvPrice.setText(R.string.badge_sold_out);
        } else {
            boolean isHot = item.getAvgRating() >= 4.9f;
            b.tvBadge.setText(R.string.badge_hot);
            b.tvBadge.setBackgroundResource(R.drawable.bg_status_gold);
            b.tvBadge.setVisibility(isHot ? View.VISIBLE : View.GONE);
            b.getRoot().setAlpha(1f);
            b.getRoot().setClickable(true);
            b.tvPrice.setText(PriceUtils.formatVnd(item.getBasePrice()));
        }
    }

    private static final DiffUtil.ItemCallback<RoomType> DIFF_CALLBACK = new DiffUtil.ItemCallback<RoomType>() {
        @Override
        public boolean areItemsTheSame(@NonNull RoomType oldItem, @NonNull RoomType newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RoomType oldItem, @NonNull RoomType newItem) {
            return oldItem.getTypeName().equals(newItem.getTypeName())
                    && oldItem.getBasePrice() == newItem.getBasePrice()
                    && oldItem.getAvgRating() == newItem.getAvgRating()
                    && oldItem.getReviewCount() == newItem.getReviewCount()
                    && oldItem.getImageUrl().equals(newItem.getImageUrl());
        }
    };
}