package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

/**
 * Adapter tổng quát dùng ListAdapter + DiffUtil, thay cho việc viết lại
 * mỗi Adapter riêng lẻ (RoomTypeAdapter, BookingAdapter, AdminRoomAdapter...).
 * Dùng chung cho toàn app — mọi Adapter khác kế thừa lớp này thay vì
 * RecyclerView.Adapter thẳng.
 *
 * @param <T>  kiểu dữ liệu (RoomType, Booking, Customer...)
 * @param <VB> ViewBinding của item_xxx.xml tương ứng
 */
public abstract class BaseAdapter<T, VB extends ViewBinding>
        extends ListAdapter<T, BaseAdapter<T, VB>.BaseViewHolder> {

    /** Callback khi bấm vào 1 item — Activity/Fragment set qua setOnItemClickListener. */
    public interface OnItemClickListener<T> {
        void onItemClick(@NonNull T item, int position);
    }

    private OnItemClickListener<T> itemClickListener;

    protected BaseAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
    }

    protected BaseAdapter(@NonNull AsyncDifferConfig<T> config) {
        super(config);
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.itemClickListener = listener;
    }

    /** Adapter con override để inflate đúng ViewBinding của item layout. */
    @NonNull
    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);

    /** Adapter con override để đổ dữ liệu vào view trong binding. */
    protected abstract void bind(@NonNull VB binding, @NonNull T item, int position);

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        VB binding = inflateBinding(inflater, parent);
        return new BaseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        T item = getItem(position);
        bind(holder.binding, item, position);
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                int adapterPos = holder.getAdapterPosition(); // đổi từ getBindingAdapterPosition()
                if (adapterPos != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(getItem(adapterPos), adapterPos);
                }
            }
        });
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        final VB binding;

        BaseViewHolder(@NonNull VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}