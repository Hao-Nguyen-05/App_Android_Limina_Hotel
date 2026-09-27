package com.lumina.hotel.util;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

/**
 * Bọc luồng đăng nhập Google (Sign-In). Chỉ xử lý phía Google, KHÔNG động vào
 * CSDL — việc tìm/tạo user cục bộ nằm ở UserDao.loginOrRegisterWithGoogle().
 */
public class GoogleAuthHelper {

    private final GoogleSignInClient client;

    public GoogleAuthHelper(@NonNull Activity activity, @NonNull String webClientId) {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        client = GoogleSignIn.getClient(activity, options);
    }

    /** Intent để launcher.launch(...) mở màn chọn tài khoản Google. */
    public Intent getSignInIntent() {
        return client.getSignInIntent();
    }

    @Nullable
    /**
     * Gọi trong callback của ActivityResultLauncher, truyền data từ result.getData().
     * Ném ApiException nếu lỗi (kể cả huỷ) để Activity tự phân biệt theo statusCode.
     */
    public GoogleSignInAccount handleSignInResult(@Nullable Intent data) throws ApiException {
        return GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
    }

    /** Đăng xuất khỏi Google (dùng khi Đăng xuất trong app, nếu tài khoản là 'google'). */
    public void signOut() {
        client.signOut();
    }
}