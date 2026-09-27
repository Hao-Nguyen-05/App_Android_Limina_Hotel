package com.lumina.hotel.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemAdminRoomTypeBinding;
import com.lumina.hotel.model.RoomType;
import com.lumina.hotel.util.PriceUtils;

/** Danh sách loại phòng ở màn Quản lý loại phòng (admin). */
public class AdminRoomTypeAdapter
        extends ListAdapter<RoomType, AdminRoomTypeAdapter.ViewHolder> {

    /** Độ mờ của loại phòng đã ngừng phục vụ (is_active = 0). */
    private static final float ALPHA_INACTIVE = 0.6f;

    public interface OnActionListener {
        void onEdit(RoomType roomType);
        void onDelete(RoomType roomType);
    }

    private final OnActionListener listener;

    public AdminRoomTypeAdapter(OnActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<RoomType> DIFF = new DiffUtil.ItemCallback<RoomType>() {
        @Override
        public boolean areItemsTheSame(@NonNull RoomType a, @NonNull RoomType b) {
            return a.getId() == b.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RoomType a, @NonNull RoomType b) {
            return a.isActive() == b.isActive()
                    && a.getAreaM2() == b.getAreaM2()
                    && a.getMaxGuests() == b.getMaxGuests()
                    && a.getBasePrice() == b.getBasePrice()
                    && a.getTotalRooms() == b.getTotalRooms()
                    && TextUtils.equals(a.getTypeName(), b.getTypeName())
                    && TextUtils.equals(a.getImageUrl(), b.getImageUrl());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminRoomTypeBinding binding = ItemAdminRoomTypeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminRoomTypeBinding binding;

        ViewHolder(ItemAdminRoomTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.btnEdit.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEdit(getItem(pos));
                }
            });
            binding.btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDelete(getItem(pos));
                }
            });
        }

        void bind(RoomType t) {
            android.content.Context ctx = binding.getRoot().getContext();

            binding.tvTypeName.setText(t.getTypeName());
            binding.tvTypeMeta.setText(ctx.getString(R.string.admin_room_type_meta,
                    t.getAreaM2(), t.getMaxGuests(), t.getTotalRooms()));
            binding.tvTypePrice.setText(ctx.getString(R.string.admin_room_type_price_per_night,
                    PriceUtils.formatVnd(t.getBasePrice())));

            // Loại phòng đã tắt: mờ 0.6 + badge "Ngừng"
            binding.tvBadgeInactive.setVisibility(t.isActive() ? View.GONE : View.VISIBLE);
            binding.getRoot().setAlpha(t.isActive() ? 1f : ALPHA_INACTIVE);

            // Nhãn cho trình đọc màn hình: nói rõ nút thuộc loại phòng nào
            binding.btnEdit.setContentDescription(
                    ctx.getString(R.string.admin_room_type_cd_edit, t.getTypeName()));
            binding.btnDelete.setContentDescription(
                    ctx.getString(R.string.admin_room_type_cd_delete, t.getTypeName()));

            String url = t.getImageUrl();
            if (url != null && !url.trim().isEmpty()) {
                Glide.with(binding.ivRoomTypeImage).load(url).centerCrop()
                        .into(binding.ivRoomTypeImage);
            } else {
                Glide.with(binding.ivRoomTypeImage).clear(binding.ivRoomTypeImage);
                binding.ivRoomTypeImage.setImageDrawable(null); // hiện nền surface_variant
            }
        }
    }
}