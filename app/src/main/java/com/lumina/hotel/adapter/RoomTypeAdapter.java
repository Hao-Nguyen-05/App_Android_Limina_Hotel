package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.bumptech.glide.Glide;
import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemRoomTypeCardVerticalBinding;
import com.lumina.hotel.model.RoomType;
import com.lumina.hotel.util.PriceUtils;

import java.util.Locale;

/** Card dọc 160dp — dùng cho carousel ngang ở Trang chủ (Phòng nổi bật, Đã xem gần đây). */
public class RoomTypeAdapter extends BaseAdapter<RoomType, ItemRoomTypeCardVerticalBinding> {

    public RoomTypeAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    protected ItemRoomTypeCardVerticalBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemRoomTypeCardVerticalBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(@NonNull ItemRoomTypeCardVerticalBinding b, @NonNull RoomType item, int position) {
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
        b.tvPrice.setText(PriceUtils.formatVnd(item.getBasePrice()));
        b.tvOldPrice.setVisibility(android.view.View.GONE);

        boolean isHot = item.getAvgRating() >= 4.9f;
        b.tvBadge.setText(R.string.badge_hot);
        b.tvBadge.setVisibility(isHot ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private static final DiffUtil.ItemCallback<RoomType> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {        @Override
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