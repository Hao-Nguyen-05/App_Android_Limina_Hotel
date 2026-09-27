package com.lumina.hotel.ui.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.lumina.hotel.R;
import com.lumina.hotel.data.dao.BookingDao;
import com.lumina.hotel.data.dao.PromotionDao;
import com.lumina.hotel.data.dao.RoomTypeDao;
import com.lumina.hotel.databinding.ActivityPaymentBinding;
import com.lumina.hotel.model.Booking;
import com.lumina.hotel.model.IntentKeys;
import com.lumina.hotel.model.Promotion;
import com.lumina.hotel.model.RoomType;
import com.lumina.hotel.util.AppExecutors;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.PriceUtils;
import com.lumina.hotel.util.SessionManager;
import com.lumina.hotel.util.VnPayHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private static final long HOLD_MILLIS = 15 * 60 * 1000L; // 15 phút
    private static final String VNPAY_RETURN_PREFIX = "lumina://payment-return";
    static final String EXTRA_PAYMENT_METHOD = "extra_payment_method";
    private ActivityPaymentBinding binding;

    private RoomTypeDao roomTypeDao;
    private BookingDao bookingDao;
    private PromotionDao promotionDao;
    private SessionManager sessionManager;
    private VnPayHelper vnPayHelper;

    private int typeId;
    private long checkIn;
    private long checkOut;
    private int numGuests;
    private int nights;

    private RoomType roomType;
    private String selectedMethod = "vnpay";

    private long subtotal;
    private long serviceFee;
    private long discount;
    private long vat;
    private long total;

    private CountDownTimer holdTimer;
    private boolean expired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        typeId = getIntent().getIntExtra(IntentKeys.EXTRA_ROOM_TYPE_ID, -1);
        checkIn = getIntent().getLongExtra(IntentKeys.EXTRA_CHECK_IN, 0L);
        checkOut = getIntent().getLongExtra(IntentKeys.EXTRA_CHECK_OUT, 0L);
        numGuests = getIntent().getIntExtra(IntentKeys.EXTRA_NUM_GUESTS, 1);
        if (numGuests <= 0) numGuests = 1;

        if (typeId <= 0 || checkIn <= 0 || checkOut <= 0) {
            Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        nights = (int) ((checkOut - checkIn) / DateUtils.ONE_DAY);
        if (nights <= 0) {
            Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomTypeDao = new RoomTypeDao(this);
        bookingDao = new BookingDao(this);
        promotionDao = new PromotionDao(this);
        sessionManager = new SessionManager(this);
        vnPayHelper = new VnPayHelper(getString(R.string.vnp_tmn_code), getString(R.string.vnp_hash_secret));

        setupInsets();
        setupClicks();
        loadData();
    }

    /* ================== INSETS ================== */

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.tbPayment.setPadding(
                    binding.tbPayment.getPaddingLeft(), bars.top,
                    binding.tbPayment.getPaddingRight(), binding.tbPayment.getPaddingBottom());
            binding.layoutBottomBar.setPadding(
                    binding.layoutBottomBar.getPaddingLeft(), binding.layoutBottomBar.getPaddingTop(),
                    binding.layoutBottomBar.getPaddingRight(),
                    bars.bottom > 0 ? bars.bottom : binding.layoutBottomBar.getPaddingBottom());
            return insets;
        });
    }

    /* ================== TẢI DỮ LIỆU ================== */

    private void loadData() {
        AppExecutors.io(() -> {
            RoomType t = roomTypeDao.getById(typeId);
            if (t == null) {
                AppExecutors.runOnUiSafe(this, () -> {
                    Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }
            int overlapping = bookingDao.getOverlappingCount(typeId, checkIn, checkOut);

            AppExecutors.runOnUiSafe(this, () -> {
                roomType = t;
                if (overlapping >= t.getTotalRooms()) {
                    Snackbar.make(binding.getRoot(), R.string.room_detail_sold_out_note,
                            Snackbar.LENGTH_LONG).setAnchorView(binding.layoutBottomBar).show();
                    binding.btnPayNow.setEnabled(false);
                    binding.btnPayNow.setAlpha(0.4f);
                    return;
                }
                bindSummary(t);
                recalcPrices();
                startHoldTimer();
            });
        });
    }

    private void bindSummary(RoomType t) {
        binding.tvRoomName.setText(t.getTypeName());
        Glide.with(binding.ivRoomThumb)
                .load(t.getImageUrl())
                .placeholder(R.color.surface_variant)
                .error(R.drawable.ic_error)
                .into(binding.ivRoomThumb);

        SimpleDateFormat df = new SimpleDateFormat("dd/MM", Locale.getDefault());
        binding.tvDateRange.setText("📅 " + df.format(checkIn) + " → " + df.format(checkOut));
        binding.tvNightGuest.setText(nights + " đêm · " + numGuests + " khách");
    }

    /* ================== ĐẾM NGƯỢC GIỮ CHỖ ================== */

    private void startHoldTimer() {
        holdTimer = new CountDownTimer(HOLD_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSec = millisUntilFinished / 1000;
                long min = totalSec / 60;
                long sec = totalSec % 60;
                binding.tvCountdown.setText(String.format(Locale.getDefault(), "%s %02d:%02d",
                        getString(R.string.payment_hold_timer_label), min, sec));
            }

            @Override
            public void onFinish() {
                expired = true;
                binding.tvCountdown.setText(String.format(Locale.getDefault(), "%s 00:00",
                        getString(R.string.payment_hold_timer_label)));
                binding.btnPayNow.setEnabled(false);
                binding.btnPayNow.setAlpha(0.4f);

                new MaterialAlertDialogBuilder(PaymentActivity.this)
                        .setTitle(R.string.payment_expired_title)
                        .setMessage(R.string.payment_error_expired)
                        .setCancelable(false)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> finish())
                        .show();
            }
        }.start();
    }

    /* ================== TÍNH TIỀN ================== */

    private void recalcPrices() {
        subtotal = PriceUtils.calcSubtotal(roomType.getBasePrice(), nights);
        serviceFee = PriceUtils.SERVICE_FEE;
        vat = PriceUtils.calcVat(subtotal, serviceFee, discount);
        total = PriceUtils.calcTotal(subtotal, serviceFee, discount, vat);

        binding.tvPriceLineLabel.setText(PriceUtils.formatVnd(roomType.getBasePrice()) + " × "
                + getString(R.string.night_count_format, nights));
        binding.tvPriceLineValue.setText(PriceUtils.formatVnd(subtotal));
        binding.tvServiceFeeValue.setText(PriceUtils.formatVnd(serviceFee));
        binding.tvVatValue.setText(PriceUtils.formatVnd(vat));

        if (discount > 0) {
            binding.layoutDiscountLine.setVisibility(View.VISIBLE);
            binding.tvDiscountValue.setText("-" + PriceUtils.formatVnd(discount));
        } else {
            binding.layoutDiscountLine.setVisibility(View.GONE);
        }

        binding.tvTotalValue.setText(PriceUtils.formatVnd(total));
        binding.btnPayNow.setText(getString(R.string.payment_btn_submit) + " · " + PriceUtils.formatVnd(total));
    }

    /* ================== MÃ GIẢM GIÁ ================== */

    private void applyPromoCode() {
        String code = binding.edtPromoCode.getText() == null
                ? "" : binding.edtPromoCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) return;

        AppExecutors.io(() -> {
            Promotion promo = promotionDao.findValidCode(code, System.currentTimeMillis());
            AppExecutors.runOnUiSafe(this, () -> {
                binding.tvPromoResult.setVisibility(View.VISIBLE);
                if (promo == null) {
                    discount = 0;
                    binding.tvPromoResult.setTextColor(getColor(R.color.status_red_text));
                    binding.tvPromoResult.setText(getString(R.string.payment_error_promo_invalid));
                } else {
                    discount = PriceUtils.calcDiscount(subtotal, promo.getDiscountPercent(), promo.getMaxDiscount());
                    binding.tvPromoResult.setTextColor(getColor(R.color.status_green_text));
                    binding.tvPromoResult.setText("Đã áp dụng mã " + promo.getCode()
                            + " (-" + promo.getDiscountPercent() + "%)");
                }
                recalcPrices();
            });
        });
    }

    /* ================== CHỌN PHƯƠNG THỨC ================== */

    private void selectMethod(String method) {
        selectedMethod = method;
        binding.cvVnpay.setSelected("vnpay".equals(method));
        binding.cvMomo.setSelected("momo".equals(method));
        binding.cvCard.setSelected("card".equals(method));
        binding.cvCash.setSelected("cash".equals(method));
        binding.rbVnpay.setChecked("vnpay".equals(method));
        binding.rbMomo.setChecked("momo".equals(method));
        binding.rbCard.setChecked("card".equals(method));
        binding.rbCash.setChecked("cash".equals(method));
    }

    /* ================== THANH TOÁN ================== */

    private void submitPayment() {
        if (expired) {
            Snackbar.make(binding.getRoot(), R.string.payment_error_expired, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.layoutBottomBar).show();
            return;
        }
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnPayNow.setEnabled(false);
        binding.pbLoading.setVisibility(View.VISIBLE);

        AppExecutors.io(() -> {
            int overlapping = bookingDao.getOverlappingCount(typeId, checkIn, checkOut);
            if (overlapping >= roomType.getTotalRooms()) {
                AppExecutors.runOnUiSafe(this, () -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    binding.btnPayNow.setEnabled(true);
                    Snackbar.make(binding.getRoot(), R.string.room_detail_sold_out_note,
                            Snackbar.LENGTH_LONG).setAnchorView(binding.layoutBottomBar).show();
                });
                return;
            }

            AppExecutors.runOnUiSafe(this, () -> {
                binding.pbLoading.setVisibility(View.GONE);
                if ("vnpay".equals(selectedMethod)) {
                    openVnPayWebView();
                } else {
                    finalizeBooking(selectedMethod, null);
                }
            });
        });
    }

    /* ================== VNPAY THẬT (PROMPT #8) ================== */

    private void openVnPayWebView() {
        String orderId = "PAY" + System.currentTimeMillis();
        String orderInfo = "Thanh toan dat phong " + roomType.getTypeName();
        String url = vnPayHelper.buildPaymentUrl(total, orderId, orderInfo);
        android.util.Log.d("VNPAY_DEBUG", "URL = " + url);

        binding.svContent.setVisibility(View.GONE);
        binding.layoutBottomBar.setVisibility(View.GONE);
        binding.webViewVnpay.setVisibility(View.VISIBLE);
        android.webkit.WebSettings ws = binding.webViewVnpay.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        binding.webViewVnpay.setWebChromeClient(new android.webkit.WebChromeClient());
        binding.webViewVnpay.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String requestUrl) {
                if (requestUrl.startsWith(VNPAY_RETURN_PREFIX)) {
                    handleVnPayReturn(requestUrl);
                    return true;
                }
                return false;
            }
        });
        binding.webViewVnpay.loadUrl(url);
    }

    private void handleVnPayReturn(String returnUrl) {
        Uri uri = Uri.parse(returnUrl);
        Map<String, String> params = new HashMap<>();
        for (String name : uri.getQueryParameterNames()) {
            params.put(name, uri.getQueryParameter(name));
        }
        boolean validSignature = vnPayHelper.verifyReturnParams(params);
        String responseCode = params.get("vnp_ResponseCode");

        closeVnPayWebView();

        if (validSignature && "00".equals(responseCode)) {
            finalizeBooking("vnpay", params.get("vnp_TransactionNo"));
        } else {
            binding.btnPayNow.setEnabled(true);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.payment_expired_title)
                    .setMessage(getString(R.string.error_network) + " (mã lỗi: " + responseCode + ")")
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
        }
    }

    private void closeVnPayWebView() {
        binding.webViewVnpay.setVisibility(View.GONE);
        binding.webViewVnpay.loadUrl("about:blank");
        binding.svContent.setVisibility(View.VISIBLE);
        binding.layoutBottomBar.setVisibility(View.VISIBLE);
    }

    /** Ghi booking + payments thật — dùng chung cho VNPay (sau khi verify) và MoMo/Thẻ/Tiền mặt (demo cũ). */
    private void finalizeBooking(String method, String txnRef) {
        binding.btnPayNow.setEnabled(false);
        binding.pbLoading.setVisibility(View.VISIBLE);

        AppExecutors.io(() -> {
            Booking b = new Booking();
            b.setUserId(sessionManager.getUserId());
            b.setRoomTypeId(typeId);
            b.setCheckInDate(checkIn);
            b.setCheckOutDate(checkOut);
            b.setNumNights(nights);
            b.setNumGuests(numGuests);
            b.setRoomSubtotal(subtotal);
            b.setServiceFee(serviceFee);
            b.setVatAmount(vat);
            b.setDiscountAmount(discount);
            b.setTotalAmount(total);
            b.setBookingStatus("confirmed");
            long bookingId = txnRef != null
                    ? bookingDao.createBooking(b, method, txnRef)
                    : bookingDao.createBooking(b, method);

            AppExecutors.runOnUiSafe(this, () -> {
                binding.pbLoading.setVisibility(View.GONE);
                if (bookingId == -1) {
                    binding.btnPayNow.setEnabled(true);
                    Toast.makeText(this, R.string.feature_not_ready, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (holdTimer != null) holdTimer.cancel();
                Intent intent = new Intent(PaymentActivity.this, BookingSuccessActivity.class);
                intent.putExtra(IntentKeys.EXTRA_BOOKING_ID, b.getId());
                intent.putExtra(IntentKeys.EXTRA_BOOKING_CODE, b.getBookingCode());
                intent.putExtra(EXTRA_PAYMENT_METHOD, method);
                startActivity(intent);
                finish();
            });
        });
    }

    /* ================== CLICK CHUNG ================== */

    private void setupClicks() {
        binding.tbPayment.setNavigationOnClickListener(v -> finish());
        binding.cvVnpay.setOnClickListener(v -> selectMethod("vnpay"));
        binding.cvMomo.setOnClickListener(v -> selectMethod("momo"));
        binding.cvCard.setOnClickListener(v -> selectMethod("card"));
        binding.cvCash.setOnClickListener(v -> selectMethod("cash"));
        binding.btnApplyCode.setOnClickListener(v -> applyPromoCode());
        binding.btnPayNow.setOnClickListener(v -> submitPayment());
    }

    @Override
    public void onBackPressed() {
        if (binding.webViewVnpay.getVisibility() == View.VISIBLE) {
            closeVnPayWebView();
            binding.btnPayNow.setEnabled(true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (holdTimer != null) holdTimer.cancel();
    }
}