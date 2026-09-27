package com.lumina.hotel.ui.admin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.lumina.hotel.R;

/** Màn tạm cho các tab Admin chưa làm (Loại phòng, Phòng, Đơn đặt phòng, Khách hàng). */
public class AdminPlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";

    public static AdminPlaceholderFragment newInstance(String title) {
        AdminPlaceholderFragment f = new AdminPlaceholderFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cream));

        TextView tv = new TextView(requireContext());
        tv.setTextAppearance(R.style.TextAppearance_Lumina_H2);
        tv.setGravity(Gravity.CENTER);
        String title = getArguments() != null ? getArguments().getString(ARG_TITLE) : "";
        tv.setText(title);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(tv, lp);
        return root;
    }
}