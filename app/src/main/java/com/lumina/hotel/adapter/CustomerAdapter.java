package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lumina.hotel.databinding.ItemCustomerBinding;
import com.lumina.hotel.model.CustomerStat;
import com.lumina.hotel.util.PriceUtils;

/**
 * Adapter cho rvCustomers (SPEC 12 — Admin Quản lý khách hàng).
 * Dùng ListAdapter + DiffUtil theo đúng quy ước BLOCK 0.
 */
public class CustomerAdapter extends ListAdapter<CustomerStat, CustomerAdapter.CustomerViewHolder> {

    public interface OnCustomerClickListener {
        void onCustomerClick(CustomerStat stat);
    }

    private final OnCustomerClickListener listener;

    public CustomerAdapter(OnCustomerClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CustomerStat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CustomerStat>() {
                @Override
                public boolean areItemsTheSame(@NonNull CustomerStat oldItem, @NonNull CustomerStat newItem) {
                    return oldItem.getUser().getId() == newItem.getUser().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CustomerStat oldItem, @NonNull CustomerStat newItem) {
                    return oldItem.getUser().isVip() == newItem.getUser().isVip()
                            && oldItem.getTotalBookings() == newItem.getTotalBookings()
                            && oldItem.getTotalSpent() == newItem.getTotalSpent();
                }
            };

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCustomerBinding binding = ItemCustomerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CustomerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        private final ItemCustomerBinding binding;

        CustomerViewHolder(ItemCustomerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CustomerStat stat, OnCustomerClickListener listener) {
            binding.tvAvatar.setText(stat.getUser().getInitial());
            binding.tvCustomerName.setText(stat.getUser().getFullName());
            binding.tvCustomerContact.setText(
                    stat.getUser().getPhone() + " · " + stat.getUser().getEmail());
            binding.tvBookingCount.setText(
                    binding.getRoot().getContext().getString(
                            com.lumina.hotel.R.string.customer_booking_count, stat.getTotalBookings()));
            binding.tvTotalSpent.setText(PriceUtils.formatVnd(stat.getTotalSpent()));

            boolean vip = stat.getUser().isVip();
            binding.tvVipBadge.setVisibility(vip ? View.VISIBLE : View.GONE);
            binding.ivVipStar.setVisibility(vip ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onCustomerClick(stat);
            });
        }
    }
}