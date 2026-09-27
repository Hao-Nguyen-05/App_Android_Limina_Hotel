package com.lumina.hotel.ui.customer;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.lumina.hotel.R;
import com.lumina.hotel.data.dao.BookingDao;
import com.lumina.hotel.databinding.ActivityBookingSuccessBinding;
import com.lumina.hotel.model.Booking;
import com.lumina.hotel.model.BookingWithDetail;
import com.lumina.hotel.model.IntentKeys;
import com.lumina.hotel.util.AppExecutors;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * SPEC 13b — Màn "Đặt phòng thành công", mở từ PaymentActivity sau khi
 * createBooking() thành công. Nhận EXTRA_BOOKING_ID / EXTRA_BOOKING_CODE.
 * Nút Back vật lý và 2 nút điều hướng đều đi qua MainActivity (không quay
 * lại PaymentActivity/RoomDetailActivity).
 */
public class BookingSuccessActivity extends AppCompatActivity {

    private ActivityBookingSuccessBinding binding;
    private BookingDao bookingDao;
    private SessionManager sessionManager;

    private int bookingId;
    private String bookingCode;
    private String paymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityBookingSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), bars.bottom);
            return insets;
        });

        bookingId = getIntent().getIntExtra(IntentKeys.EXTRA_BOOKING_ID, -1);
        bookingCode = getIntent().getStringExtra(IntentKeys.EXTRA_BOOKING_CODE);
        paymentMethod = getIntent().getStringExtra(PaymentActivity.EXTRA_PAYMENT_METHOD);

        if (bookingId <= 0) {
            Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hiện mã đơn ngay (đã có từ Intent) trong lúc chờ tải chi tiết đầy đủ.
        if (bookingCode != null) {
            binding.tvBookingCode.setText("#" + bookingCode);
        }

        bookingDao = new BookingDao(this);
        sessionManager = new SessionManager(this);

        setupClicks();
        setupBackHandler();
        bindPaymentBadge();
        loadData();
    }

    private void bindPaymentBadge() {
        if ("cash".equals(paymentMethod)) {
            binding.tvPaidBadge.setText(R.string.success_status_unpaid);
            binding.tvPaidBadge.setBackground(
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_status_orange));
            binding.tvPaidBadge.setTextColor(getColor(R.color.status_orange_text));
        }
    }

    /* ================== TẢI DỮ LIỆU ================== */

    private void loadData() {
        AppExecutors.io(() -> {
            BookingWithDetail detail = bookingDao.getDetailById(bookingId);
            AppExecutors.runOnUiSafe(this, () -> {
                if (detail == null) {
                    Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                bind(detail);
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void bind(BookingWithDetail d) {
        Booking b = d.getBooking();

        binding.tvBookingCode.setText("#" + b.getBookingCode());
        binding.tvRoomName.setText(d.getTypeName());

        SimpleDateFormat df = new SimpleDateFormat("dd/MM", Locale.getDefault());
        binding.tvDateRange.setText("📅 " + df.format(b.getCheckInDate()) + " → "
                + df.format(b.getCheckOutDate()) + " · "
                + getString(R.string.night_count_format, b.getNumNights()));

        binding.tvGuestCheckIn.setText("👤 " + b.getNumGuests() + " khách · Check-in 14:00");

        binding.tvTotalAmount.setText(PriceUtils.formatVnd(b.getTotalAmount()));

        binding.tvEmailNote.setText(getString(R.string.success_email_note, sessionManager.getEmail()));
    }

    /* ================== CLICK ================== */

    private void setupClicks() {
        binding.tvBookingCode.setOnLongClickListener(v -> {
            copyCodeToClipboard();
            return true;
        });
        binding.btnViewBookings.setOnClickListener(v -> goToTab(2, false));
        binding.btnBackHome.setOnClickListener(v -> goToTab(0, true));
    }

    private void copyCodeToClipboard() {
        String text = binding.tvBookingCode.getText().toString();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.success_booking_code_prefix), text));
        Toast.makeText(this, R.string.success_code_copied, Toast.LENGTH_SHORT).show();
    }

    /* ================== ĐIỀU HƯỚNG ================== */

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToTab(0, true);
            }
        });
    }

    /**
     * @param tabIndex   0 = Trang chủ, 2 = Đơn của tôi
     * @param clearTask  true = xoá hết task cũ (về Trang chủ, không cho back lại màn thành công/thanh toán);
     *                   false = chỉ đưa MainActivity lên đầu (xem Đơn của tôi)
     */
    private void goToTab(int tabIndex, boolean clearTask) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(IntentKeys.EXTRA_TAB_INDEX, tabIndex);
        if (clearTask) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        startActivity(intent);
        finish();
    }
}
