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
            android.content.Context context = binding.getRoot().getContext();

            String phone = cleanContact(stat.getUser().getPhone());
            String email = cleanContact(stat.getUser().getEmail());
            String contact;
            if (phone != null && email != null) {
                contact = context.getString(
                        com.lumina.hotel.R.string.admin_customer_contact_format, phone, email);
            } else if (phone != null) {
                contact = phone;
            } else if (email != null) {
                contact = email;
            } else {
                contact = context.getString(com.lumina.hotel.R.string.admin_customer_contact_empty);
            }
            binding.tvCustomerContact.setText(contact);

            binding.tvBookingCount.setText(
                    context.getString(
                            com.lumina.hotel.R.string.customer_booking_count, stat.getTotalBookings()));
            binding.tvTotalSpent.setText(PriceUtils.formatVnd(stat.getTotalSpent()));

            boolean vip = stat.getUser().isVip();
            binding.tvVipBadge.setVisibility(vip ? View.VISIBLE : View.GONE);
            binding.ivVipStar.setVisibility(vip ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onCustomerClick(stat);
            });
        }
        /** Trả về null nếu rỗng, chỉ có khoảng trắng hoặc là chữ "null" lưu nhầm trong dữ liệu. */
        private static String cleanContact(String value) {
            if (value == null) return null;
            String trimmed = value.trim();
            if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) return null;
            return trimmed;
        }
    }
}