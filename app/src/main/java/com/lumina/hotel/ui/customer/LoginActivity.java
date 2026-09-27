package com.lumina.hotel.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lumina.hotel.data.dao.UserDao;
import com.lumina.hotel.databinding.ActivityLoginBinding;
import com.lumina.hotel.model.User;
import com.lumina.hotel.ui.admin.AdminMainActivity;
import com.lumina.hotel.util.AppExecutors;
import com.lumina.hotel.util.GoogleAuthHelper;
import com.lumina.hotel.util.SessionManager;
import com.lumina.hotel.R;

/**
 * SPEC 02 — Đăng nhập bằng email HOẶC số điện thoại, hoặc Google Sign-In thật.
 * Gọi UserDao.login trên luồng nền, thành công thì lưu phiên rồi chuyển theo role.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private UserDao userDao;
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userDao = new UserDao(getApplicationContext());

        googleAuthHelper = new GoogleAuthHelper(this, getString(R.string.google_web_client_id));
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleGoogleSignInResult(result.getData()));

        applyWindowInsets();
        setupInputs();
        setupClicks();
    }

    /** Chừa chỗ cho status bar, thanh điều hướng và bàn phím (không bị che nút/ô nhập). */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void setupInputs() {
        clearErrorOnType(binding.edtEmail, binding.tilEmail);
        clearErrorOnType(binding.edtPassword, binding.tilPassword);

        // Phím "Done" trên bàn phím = bấm nút Đăng nhập
        binding.edtPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void setupClicks() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoRegister.setOnClickListener(v -> openRegister());
        binding.btnRegisterLink.setOnClickListener(v -> openRegister());

        binding.btnGoogle.setOnClickListener(v -> googleSignInLauncher.launch(googleAuthHelper.getSignInIntent()));
        binding.btnForgotPassword.setOnClickListener(v -> showComingSoon());
    }

    // ---------------- ĐĂNG NHẬP ----------------

    private void attemptLogin() {
        String account = textOf(binding.edtEmail).trim();
        String password = textOf(binding.edtPassword);

        boolean hasAccount = !account.isEmpty();
        boolean hasPassword = !password.isEmpty();

        binding.tilEmail.setError(hasAccount
                ? null : getString(R.string.login_error_account_required));
        binding.tilPassword.setError(hasPassword
                ? null : getString(R.string.login_error_password_required));

        if (!hasAccount) {
            binding.edtEmail.requestFocus();
            return;
        }
        if (!hasPassword) {
            binding.edtPassword.requestFocus();
            return;
        }

        hideKeyboard();
        setLoading(true);

        AppExecutors.io(() -> {
            User user = null;
            boolean failed = false;
            try {
                user = userDao.login(account, password);
            } catch (Exception e) {
                failed = true;
                Log.e(TAG, "Lỗi khi đăng nhập", e);
            }

            final User result = user;
            final boolean hasError = failed;
            AppExecutors.runOnUiSafe(this, () -> {
                setLoading(false);
                if (hasError) {
                    Snackbar.make(binding.getRoot(), R.string.error_network,
                            Snackbar.LENGTH_LONG).show();
                } else if (result == null) {
                    // Không tiết lộ trường nào sai
                    Snackbar.make(binding.getRoot(), R.string.login_error_invalid,
                            Snackbar.LENGTH_LONG).show();
                } else {
                    onLoginSuccess(result);
                }
            });
        });
    }

    private void onLoginSuccess(User user) {
        new SessionManager(this).saveSession(user);

        Intent intent;
        if (com.lumina.hotel.data.DbContract.Role.ADMIN.equals(user.getRole())) {
            intent = new Intent(this, AdminMainActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    // ---------------- GOOGLE SIGN-IN ----------------

    private void handleGoogleSignInResult(Intent data) {
        GoogleSignInAccount account;
        try {
            account = googleAuthHelper.handleSignInResult(data);
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In thất bại, mã lỗi: " + e.getStatusCode(), e);
            if (e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Snackbar.make(binding.getRoot(), R.string.error_network,
                        Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        // ... phần còn lại giữ nguyên

        setLoading(true);
        AppExecutors.io(() -> {
            User user = null;
            boolean failed = false;
            try {
                user = userDao.loginOrRegisterWithGoogle(
                        account.getId(), account.getEmail(),
                        account.getDisplayName(),
                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null);
            } catch (Exception e) {
                failed = true;
                Log.e(TAG, "Lỗi khi đăng nhập Google", e);
            }

            final User result = user;
            final boolean hasError = failed;
            AppExecutors.runOnUiSafe(this, () -> {
                setLoading(false);
                if (hasError || result == null) {
                    Snackbar.make(binding.getRoot(), R.string.error_network,
                            Snackbar.LENGTH_LONG).show();
                } else {
                    onLoginSuccess(result);
                }
            });
        });
    }

    // ---------------- TIỆN ÍCH ----------------

    private void openRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.feature_coming_soon,
                Snackbar.LENGTH_SHORT).show();
    }

    /** Hiện vòng tải và khoá các điều khiển khi đang xử lý. */
    private void setLoading(boolean loading) {
        binding.pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean enabled = !loading;
        binding.btnLogin.setEnabled(enabled);
        binding.btnGoogle.setEnabled(enabled);
        binding.btnGoRegister.setEnabled(enabled);
        binding.btnRegisterLink.setEnabled(enabled);
        binding.btnForgotPassword.setEnabled(enabled);
        binding.edtEmail.setEnabled(enabled);
        binding.edtPassword.setEnabled(enabled);
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
    }

    private String textOf(TextInputEditText edt) {
        return edt.getText() == null ? "" : edt.getText().toString();
    }

    /** Người dùng gõ lại thì tự xoá thông báo lỗi ở ô đó. */
    private void clearErrorOnType(TextInputEditText edt, TextInputLayout til) {
        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { til.setError(null); }
        });
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}