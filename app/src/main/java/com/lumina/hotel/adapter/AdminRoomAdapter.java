package com.lumina.hotel.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemAdminRoomBinding;
import com.lumina.hotel.model.RoomWithType;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.StatusUtils;

/**
 * Danh sách phòng (lưới 2 cột) ở màn Quản lý phòng (admin).
 * Bấm cả card -> BottomSheet đổi trạng thái (qua setOnItemClickListener của BaseAdapter).
 * Nhấn giữ card -> xác nhận xóa (qua OnRoomLongClickListener riêng, BaseAdapter chưa hỗ trợ long-click).
 */
public class AdminRoomAdapter extends BaseAdapter<RoomWithType, ItemAdminRoomBinding> {

    public interface OnRoomLongClickListener {
        void onLongClick(@NonNull RoomWithType item);
    }

    private OnRoomLongClickListener longClickListener;

    private static final DiffUtil.ItemCallback<RoomWithType> DIFF =
            new DiffUtil.ItemCallback<RoomWithType>() {
                @Override
                public boolean areItemsTheSame(@NonNull RoomWithType a, @NonNull RoomWithType b) {
                    return a.getRoom().getId() == b.getRoom().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull RoomWithType a, @NonNull RoomWithType b) {
                    return a.getRoom().getFloor() == b.getRoom().getFloor()
                            && a.getBasePrice() == b.getBasePrice()
                            && TextUtils.equals(a.getRoom().getRoomNumber(), b.getRoom().getRoomNumber())
                            && TextUtils.equals(a.getRoom().getRoomStatus(), b.getRoom().getRoomStatus())
                            && TextUtils.equals(a.getRoom().getNote(), b.getRoom().getNote())
                            && TextUtils.equals(a.getTypeName(), b.getTypeName());
                }
            };

    public AdminRoomAdapter() {
        super(DIFF);
    }

    public void setOnRoomLongClickListener(OnRoomLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    protected ItemAdminRoomBinding inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return ItemAdminRoomBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(@NonNull ItemAdminRoomBinding binding, @NonNull RoomWithType item, int position) {
        Context ctx = binding.getRoot().getContext();

        binding.tvRoomNumber.setText(item.getRoom().getRoomNumber());
        binding.tvRoomTypeFloor.setText(ctx.getString(R.string.admin_room_meta,
                item.getTypeName(), item.getRoom().getFloor()));
        binding.tvRoomPrice.setText(ctx.getString(R.string.admin_room_type_price_per_night,
                PriceUtils.formatShort(item.getBasePrice())));

        StatusUtils.applyRoomStatus(binding.tvRoomStatus, item.getRoom().getRoomStatus());

        binding.getRoot().setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(item);
            }
            return true;
        });
    }
}