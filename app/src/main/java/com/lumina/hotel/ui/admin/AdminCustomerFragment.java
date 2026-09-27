package com.lumina.hotel.ui.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.lumina.hotel.data.dao.UserDao;
import com.lumina.hotel.databinding.FragmentAdminCustomerBinding;
import com.lumina.hotel.adapter.CustomerAdapter;
import com.lumina.hotel.model.CustomerStat;
import com.lumina.hotel.util.AppExecutors;

import java.util.List;

/**
 * SPEC 12 — Admin · Quản lý Khách hàng.
 * Danh sách khách + tìm kiếm realtime (debounce 300ms) qua UserDao.getCustomerStats(keyword).
 */
public class AdminCustomerFragment extends Fragment {

    private FragmentAdminCustomerBinding binding;
    private UserDao userDao;
    private CustomerAdapter adapter;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private static final long SEARCH_DEBOUNCE_MS = 300;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCustomerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userDao = new UserDao(requireContext());

        adapter = new CustomerAdapter(this::showCustomerDetail);
        binding.rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomers.setAdapter(adapter);

        setupSearch();
        loadCustomers(null);
    }

    private void setupSearch() {
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClearSearch.setVisibility(
                        s.length() > 0 ? View.VISIBLE : View.GONE);

                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                String keyword = s.toString();
                pendingSearch = () -> loadCustomers(keyword);
                searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.edtSearch.setText(""));
    }

    private void loadCustomers(@Nullable String keyword) {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        AppExecutors.io(() -> {
            List<CustomerStat> data = userDao.getCustomerStats(keyword);
            if (getActivity() == null) return; // Fragment đã bị huỷ trong lúc chờ

            requireActivity().runOnUiThread(() -> {
                binding.pbLoading.setVisibility(View.GONE);
                binding.tvCustomerCount.setText(
                        getString(com.lumina.hotel.R.string.customer_count_format,
                                userDao.countCustomers()));

                if (data.isEmpty()) {
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    binding.rvCustomers.setVisibility(View.GONE);
                } else {
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.rvCustomers.setVisibility(View.VISIBLE);
                    adapter.submitList(data);
                }
            });
        });
    }

    private void showCustomerDetail(CustomerStat stat) {
        CustomerDetailBottomSheet.newInstance(stat,
                        () -> loadCustomers(binding.edtSearch.getText().toString()))
                .show(getParentFragmentManager(), "customer_detail");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        binding = null;
    }
}