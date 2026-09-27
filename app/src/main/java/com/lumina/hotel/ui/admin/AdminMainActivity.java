package com.lumina.hotel.ui.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ActivityAdminMainBinding;

public class AdminMainActivity extends AppCompatActivity {

    private ActivityAdminMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.frameAdminContainer.setPadding(0, systemBars.top, 0, 0);
            binding.bnvAdmin.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) switchTab(R.id.nav_admin_dashboard);

        binding.bnvAdmin.setOnItemSelectedListener(item -> {
            switchTab(item.getItemId());
            return true;
        });
    }

    /** Chuyển tab từ code (vd nút "Xem tất cả" ở Dashboard): tự cập nhật luôn tab đang sáng ở bottom nav. */
    public void openTab(int itemId) {
        binding.bnvAdmin.setSelectedItemId(itemId); // kích hoạt listener ở trên → gọi switchTab()
    }

    public void switchTab(int itemId) {
        Fragment f;
        if (itemId == R.id.nav_admin_dashboard) f = new AdminDashboardFragment();
        else if (itemId == R.id.nav_admin_room_type) f = new AdminRoomTypeFragment();
        else if (itemId == R.id.nav_admin_room) f = new AdminRoomFragment();
        else if (itemId == R.id.nav_admin_booking) f = new AdminBookingFragment();
        else f = new AdminCustomerFragment();

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.frameAdminContainer, f);
        tx.commit();
    }
}