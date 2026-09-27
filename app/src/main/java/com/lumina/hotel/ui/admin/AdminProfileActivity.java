package com.lumina.hotel.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ActivityAdminProfileBinding;
import com.lumina.hotel.ui.customer.LoginActivity;
import com.lumina.hotel.util.SessionManager;

/**
 * Hồ sơ quản trị — mở từ avatar ở Dashboard (SPEC 14).
 * Chỉ hiển thị thông tin phiên hiện tại (tên/email) + nút đăng xuất.
 * Không có SPEC anatomy riêng trong bộ prompt gốc, dựng tối giản theo Design System chung.
 */
public class AdminProfileActivity extends AppCompatActivity {

    private ActivityAdminProfileBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAdminProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);

        String fullName = sessionManager.getFullName();
        binding.tvProfileName.setText(fullName);
        binding.tvProfileEmail.setText(sessionManager.getEmail());
        binding.tvProfileAvatarInitial.setText(initialOf(fullName));

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_menu_logout)
                .setMessage(R.string.profile_confirm_logout)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_logout, (d, w) -> doLogout())
                .show();
    }

    /** Xóa sạch phiên + xóa sạch back stack, để bấm Back từ màn Đăng nhập không quay lại được Admin. */
    private void doLogout() {
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Chữ cái đầu tên, hiển thị trong avatar — SessionManager chỉ lưu chuỗi tên
     * (không có sẵn hàm getInitial() như model User bên khách).
     */
    private String initialOf(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "A";
        return fullName.trim().substring(0, 1).toUpperCase();
    }
}