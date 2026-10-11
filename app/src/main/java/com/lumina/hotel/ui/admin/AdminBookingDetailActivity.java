package com.lumina.hotel.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.lumina.hotel.R;
import com.lumina.hotel.data.dao.BookingDao;
import com.lumina.hotel.data.dao.PaymentDao;
import com.lumina.hotel.databinding.ActivityAdminBookingDetailBinding;
import com.lumina.hotel.model.Booking;
import com.lumina.hotel.model.BookingWithDetail;
import com.lumina.hotel.model.IntentKeys;
import com.lumina.hotel.model.Payment;
import com.lumina.hotel.util.AppExecutors;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.StatusUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** SPEC 11 — Chi tiết đơn đặt phòng (phương án A: chỉ xem, không có nút hành động). */
public class AdminBookingDetailActivity extends AppCompatActivity {

    private ActivityAdminBookingDetailBinding binding;
    private BookingDao bookingDao;
    private PaymentDao paymentDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBookingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupInsets();
        binding.ivBack.setOnClickListener(v -> finish());

        int bookingId = getIntent() != null
                ? getIntent().getIntExtra(IntentKeys.EXTRA_BOOKING_ID, -1) : -1;
        if (bookingId <= 0) {
            Toast.makeText(this, R.string.admin_booking_detail_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bookingDao = new BookingDao(getApplicationContext());
        paymentDao = new PaymentDao(getApplicationContext());
        loadDetail(bookingId);
    }

    // ---------------- GIAO DIỆN ----------------

    /** Cộng inset thanh trạng thái vào header, inset thanh điều hướng vào đáy vùng cuộn. */
    private void setupInsets() {
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false); // header navy → icon trạng thái màu sáng

        final int headerTop = binding.layoutHeader.getPaddingTop();
        final int scrollBottom = binding.svContent.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.layoutHeader.setPadding(binding.layoutHeader.getPaddingLeft(),
                    headerTop + bars.top,
                    binding.layoutHeader.getPaddingRight(),
                    binding.layoutHeader.getPaddingBottom());
            binding.svContent.setPadding(binding.svContent.getPaddingLeft(),
                    binding.svContent.getPaddingTop(),
                    binding.svContent.getPaddingRight(),
                    scrollBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.layoutRoot);
    }

    // ---------------- TẢI DỮ LIỆU (chạy nền) ----------------

    private void loadDetail(int bookingId) {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.svContent.setVisibility(View.GONE);

        AppExecutors.io(() -> {
            BookingWithDetail detail = null;
            Payment payment = null;
            boolean failed = false;
            try {
                detail = bookingDao.getDetailById(bookingId);
                if (detail != null) payment = paymentDao.getByBookingId(bookingId);
            } catch (Exception e) {
                failed = true;
            }
            final BookingWithDetail fDetail = detail;
            final Payment fPayment = payment;
            final boolean fFailed = failed;

            AppExecutors.runOnUiSafe(this, () -> {
                binding.pbLoading.setVisibility(View.GONE);
                if (fFailed) {
                    Toast.makeText(this, R.string.admin_booking_load_error, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                if (fDetail == null) {
                    Toast.makeText(this, R.string.admin_booking_detail_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                bind(fDetail, fPayment);
                binding.svContent.setVisibility(View.VISIBLE);
            });
        });
    }

    // ---------------- HIỂN THỊ ----------------

    private void bind(BookingWithDetail d, @Nullable Payment p) {
        Booking b = d.getBooking();

        // Khối 1: mã đơn + trạng thái + thời điểm đặt
        binding.tvBookingCode.setText(PriceUtils.displayCode(b.getBookingCode()));
        StatusUtils.applyBookingStatus(binding.tvBookingStatus, b.getBookingStatus());
        binding.tvCreatedAt.setText(getString(R.string.admin_booking_detail_created_at,
                formatDateTime(b.getCreatedAt())));

        // Khối 2: khách hàng (SĐT trống thì ẩn dòng)
        binding.tvCustomerName.setText(orDash(d.getCustomerName()));
        String phone = d.getCustomerPhone();
        boolean hasPhone = phone != null && !phone.trim().isEmpty();
        binding.llRowPhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        if (hasPhone) binding.tvCustomerPhone.setText(phone);

        // Khối 3: phòng
        binding.tvRoomType.setText(orDash(d.getTypeName()));
        String roomNumber = d.getRoomNumber();
        binding.tvRoomNumber.setText(roomNumber == null || roomNumber.isEmpty()
                ? getString(R.string.admin_booking_no_room) : roomNumber);
        binding.tvGuests.setText(getString(R.string.admin_booking_detail_guests_value, b.getNumGuests()));

        // Khối 4: lưu trú
        binding.tvCheckIn.setText(weekdayDate(b.getCheckInDate()));
        binding.tvCheckOut.setText(weekdayDate(b.getCheckOutDate()));
        binding.tvNights.setText(getString(R.string.admin_booking_detail_nights_value, b.getNumNights()));

        // Khối 5: tiền (giảm giá = 0 thì ẩn dòng)
        binding.tvSubtotal.setText(PriceUtils.formatVnd(b.getRoomSubtotal()));
        binding.tvServiceFee.setText(PriceUtils.formatVnd(b.getServiceFee()));
        boolean hasDiscount = b.getDiscountAmount() > 0;
        binding.llRowDiscount.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
        if (hasDiscount) {
            binding.tvDiscount.setText(getString(R.string.admin_booking_detail_discount_value,
                    PriceUtils.formatVnd(b.getDiscountAmount())));
        }
        binding.tvVat.setText(PriceUtils.formatVnd(b.getVatAmount()));
        binding.tvTotal.setText(PriceUtils.formatVnd(b.getTotalAmount()));

        // Khối 6: thanh toán
        bindPayment(p);

        // Khối 7: ghi chú / lý do từ chối (chỉ hiện khi có nội dung)
        String note = b.getNote();
        boolean hasNote = note != null && !note.trim().isEmpty();
        binding.cvNote.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        if (hasNote) binding.tvNote.setText(note.trim());
    }

    private void bindPayment(@Nullable Payment p) {
        if (p == null) {
            // Đơn chưa có bản ghi thanh toán
            binding.tvPayMethod.setText(R.string.admin_booking_detail_empty_value);
            setPayBadge(R.string.admin_booking_detail_pay_none,
                    R.color.muted, R.drawable.bg_status_orange);
            binding.llRowPaidAt.setVisibility(View.GONE);
            binding.llRowTxn.setVisibility(View.GONE);
            return;
        }

        binding.tvPayMethod.setText(methodLabel(p.getMethod()));

        String status = p.getPaymentStatus();
        boolean paid = "paid".equals(status);
        if (paid) {
            setPayBadge(R.string.success_status_paid,
                    R.color.status_green_text, R.drawable.bg_status_green);
        } else if ("refunded".equals(status)) {
            setPayBadge(R.string.admin_booking_detail_pay_refunded,
                    R.color.status_blue_text, R.drawable.bg_status_blue);
        } else {
            setPayBadge(R.string.success_status_unpaid,
                    R.color.status_orange_text, R.drawable.bg_status_orange);
        }

        // Thời điểm thanh toán: chỉ hiện khi đã thanh toán và có mốc thời gian
        boolean showPaidAt = paid && p.getPaidAt() > 0;
        binding.llRowPaidAt.setVisibility(showPaidAt ? View.VISIBLE : View.GONE);
        if (showPaidAt) binding.tvPaidAt.setText(formatDateTime(p.getPaidAt()));

        // Mã giao dịch: ẩn nếu trống (đơn tiền mặt chưa thu)
        String txn = p.getTxnRef();
        boolean hasTxn = txn != null && !txn.trim().isEmpty()
                && !("cash".equals(p.getMethod()) && !paid);
        binding.llRowTxn.setVisibility(hasTxn ? View.VISIBLE : View.GONE);
        if (hasTxn) binding.tvTxnRef.setText(txn);
    }

    private void setPayBadge(int textRes, int colorRes, int bgRes) {
        TextView tv = binding.tvPayStatus;
        tv.setText(textRes);
        tv.setTextColor(ContextCompat.getColor(this, colorRes));
        tv.setBackgroundResource(bgRes);
    }

    // ---------------- TIỆN ÍCH ----------------

    private String methodLabel(@Nullable String method) {
        if (method == null) return getString(R.string.admin_booking_detail_empty_value);
        switch (method) {
            case "cash":  return getString(R.string.payment_method_cash);
            case "vnpay": return getString(R.string.payment_method_vnpay);
            case "momo":  return getString(R.string.payment_method_momo);
            case "card":  return getString(R.string.payment_method_card);
            default:      return method;
        }
    }

    private String weekdayDate(long millis) {
        return getString(R.string.admin_booking_detail_weekday_date,
                DateUtils.weekdayVi(millis), DateUtils.formatFull(millis));
    }

    private String formatDateTime(long millis) {
        return new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private String orDash(@Nullable String s) {
        return s == null || s.trim().isEmpty()
                ? getString(R.string.admin_booking_detail_empty_value) : s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}