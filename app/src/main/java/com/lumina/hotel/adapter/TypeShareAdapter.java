package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemTypeShareBinding;
import com.lumina.hotel.model.TypeShareItem;

/** Danh sách "Tỷ trọng theo loại phòng" trên Dashboard Admin. */
public class TypeShareAdapter extends ListAdapter<TypeShareItem, TypeShareAdapter.VH> {

    public TypeShareAdapter() {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull TypeShareItem a, @NonNull TypeShareItem b) {
                return a.getTypeName().equals(b.getTypeName());
            }

            @Override
            public boolean areContentsTheSame(@NonNull TypeShareItem a, @NonNull TypeShareItem b) {
                return a.getPercent() == b.getPercent() && a.getCount() == b.getCount();
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemTypeShareBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TypeShareItem item = getItem(position);
        ItemTypeShareBinding b = h.binding;
        b.tvTypeName.setText(item.getTypeName());
        b.tvTypePercent.setText(b.getRoot().getContext().getString(R.string.dash_percent, item.getPercent()));
        b.tvTypeCount.setText(b.getRoot().getContext().getString(R.string.dash_type_count, item.getCount()));
        b.lpiTypeShare.setProgress(item.getPercent());
    }

    public static class VH extends RecyclerView.ViewHolder {
        final ItemTypeShareBinding binding;
        VH(ItemTypeShareBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}