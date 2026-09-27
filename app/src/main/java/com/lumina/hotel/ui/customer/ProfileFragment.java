package com.lumina.hotel.ui.customer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.lumina.hotel.R;
import com.lumina.hotel.data.dao.UserDao;
import com.lumina.hotel.databinding.DialogChangePasswordBinding;
import com.lumina.hotel.databinding.FragmentProfileBinding;
import com.lumina.hotel.model.User;
import com.lumina.hotel.util.AppExecutors;
import com.lumina.hotel.util.DateUtils;
import com.lumina.hotel.util.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * SPEC 08 — Cá nhân.
 * Hiển thị/sửa hồ sơ (họ tên, SĐT, ngày sinh), đổi avatar (chọn ảnh từ thư viện,
 * copy vào bộ nhớ trong), đổi mật khẩu (dialog), đăng xuất.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserDao userDao;
    private SessionManager sessionManager;
    private User currentUser;
    private long dob;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userDao = new UserDao(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupListeners();
        loadUserData();
    }

    private void setupListeners() {
        binding.btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.edtDob.setOnClickListener(v -> openDobPicker());
        binding.btnSaveProfile.setOnClickListener(v -> onSaveClicked());
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnAboutHotel.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AboutActivity.class)));
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirm());
    }

    // ===================== TẢI DỮ LIỆU =====================

    private void loadUserData() {
        int userId = sessionManager.getUserId();
        AppExecutors.io(() -> {
            User user = userDao.getById(userId);
            AppExecutors.runOnUiSafe(getActivity(), () -> bindUser(user));
        });
    }

    private void bindUser(User user) {
        if (user == null || binding == null) return;
        currentUser = user;

        binding.tvFullName.setText(user.getFullName());
        binding.tvMemberSince.setText(
                getString(R.string.profile_member_since, DateUtils.formatMonthYear(user.getCreatedAt())));

        binding.edtFullName.setText(user.getFullName());
        binding.edtEmail.setText(user.getEmail());
        binding.edtPhone.setText(user.getPhone());

        bindDob(user.getDob());
        bindAvatar(user);
    }

    private void bindDob(long dobValue) {
        dob = dobValue;
        binding.edtDob.setText(dob > 0 ? DateUtils.formatFull(dob) : "");
    }

    private void bindAvatar(User user) {
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            binding.tvAvatarInitial.setVisibility(View.VISIBLE);
            binding.tvAvatarInitial.setText(user.getInitial());
            binding.ivAvatar.setImageDrawable(null);
        } else {
            binding.tvAvatarInitial.setVisibility(View.GONE);
            Glide.with(this)
                    .load(new File(avatarUrl))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.ic_error)
                    .into(binding.ivAvatar);
        }
    }

    // ===================== NGÀY SINH =====================

    private void openDobPicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.profile_label_dob)
                .setCalendarConstraints(constraints)
                .setSelection(toUtcMillis(dob > 0 ? dob : DateUtils.today()))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            dob = fromUtcMillis(selection);
            binding.edtDob.setText(DateUtils.formatFull(dob));
            binding.tilDob.setError(null);
        });

        picker.show(getParentFragmentManager(), "dob_picker");
    }

    /** MaterialDatePicker làm việc theo mốc UTC — quy đổi sang đúng ngày lịch ở múi giờ máy. */
    private long toUtcMillis(long localStartOfDay) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localStartOfDay);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    private long fromUtcMillis(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        return DateUtils.atStartOfDay(
                new GregorianCalendar(
                        utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH)
                ).getTimeInMillis());
    }

    // ===================== LƯU HỒ SƠ =====================

    private void onSaveClicked() {
        if (currentUser == null) return;

        String fullName = binding.edtFullName.getText() == null ? "" :
                binding.edtFullName.getText().toString().trim();
        String phone = binding.edtPhone.getText() == null ? "" :
                binding.edtPhone.getText().toString().trim();

        boolean valid = true;

        if (fullName.isEmpty()) {
            binding.tilFullName.setError(getString(R.string.profile_error_fullname_empty));
            valid = false;
        } else {
            binding.tilFullName.setError(null);
        }

        if (!phone.matches("\\d{10}")) {
            binding.tilPhone.setError(getString(R.string.profile_error_phone_invalid));
            valid = false;
        } else {
            binding.tilPhone.setError(null);
        }

        if (dob > 0 && dob >= DateUtils.today()) {
            binding.tilDob.setError(getString(R.string.profile_error_dob_future));
            valid = false;
        } else {
            binding.tilDob.setError(null);
        }

        if (!valid) return;

        int userId = sessionManager.getUserId();
        long dobToSave = dob;
        setLoading(true);

        AppExecutors.io(() -> {
            userDao.updateProfile(userId, fullName, phone, dobToSave);
            AppExecutors.runOnUiSafe(getActivity(), () -> {
                setLoading(false);
                sessionManager.updateName(fullName);
                currentUser.setFullName(fullName);
                currentUser.setPhone(phone);
                currentUser.setDob(dobToSave);
                binding.tvFullName.setText(fullName);
                Snackbar.make(binding.getRoot(), R.string.profile_snackbar_update_success, Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    private void setLoading(boolean loading) {
        binding.pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!loading);
    }

    // ===================== ĐỔI AVATAR =====================

    private void onImagePicked(Uri uri) {
        if (uri == null || binding == null) return;

        Context appContext = requireContext().getApplicationContext();
        int userId = sessionManager.getUserId();

        AppExecutors.io(() -> {
            String path = copyImageToInternalStorage(appContext, uri, userId);
            if (path == null) {
                AppExecutors.runOnUiSafe(getActivity(), () ->
                        Snackbar.make(binding.getRoot(), R.string.error_default_message, Snackbar.LENGTH_SHORT).show());
                return;
            }
            userDao.updateAvatar(userId, path);
            AppExecutors.runOnUiSafe(getActivity(), () -> {
                if (currentUser != null) currentUser.setAvatarUrl(path);
                if (currentUser != null) bindAvatar(currentUser);
            });
        });
    }

    private String copyImageToInternalStorage(Context context, Uri uri, int userId) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            File outFile = new File(context.getFilesDir(), "avatar_" + userId + ".jpg");
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    // ===================== ĐỔI MẬT KHẨU =====================

    private void showChangePasswordDialog() {
        DialogChangePasswordBinding dialogBinding =
                DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_change_password_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.dialog_change_password_submit, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = dialogBinding.edtCurrentPassword.getText() == null ? "" :
                    dialogBinding.edtCurrentPassword.getText().toString();
            String newPass = dialogBinding.edtNewPassword.getText() == null ? "" :
                    dialogBinding.edtNewPassword.getText().toString();
            String confirm = dialogBinding.edtConfirmPassword.getText() == null ? "" :
                    dialogBinding.edtConfirmPassword.getText().toString();

            boolean valid = true;

            if (newPass.length() < 6) {
                dialogBinding.tilNewPassword.setError(getString(R.string.dialog_change_password_error_new_short));
                valid = false;
            } else {
                dialogBinding.tilNewPassword.setError(null);
            }
            if (!newPass.equals(confirm)) {
                dialogBinding.tilConfirmPassword.setError(getString(R.string.dialog_change_password_error_confirm_mismatch));
                valid = false;
            } else {
                dialogBinding.tilConfirmPassword.setError(null);
            }

            if (!valid) return;

            int userId = sessionManager.getUserId();
            AppExecutors.io(() -> {
                int rows = userDao.updatePassword(userId, current, newPass);
                AppExecutors.runOnUiSafe(getActivity(), () -> {
                    if (rows > 0) {
                        Snackbar.make(binding.getRoot(), R.string.snackbar_change_password_success, Snackbar.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        dialogBinding.tilCurrentPassword.setError(
                                getString(R.string.dialog_change_password_error_current_wrong));
                    }
                });
            });
        }));

        dialog.show();
    }

    // ===================== ĐĂNG XUẤT =====================

    private void showLogoutConfirm() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_menu_logout)
                .setMessage(R.string.profile_confirm_logout)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_logout, (d, which) -> doLogout())
                .show();
    }

    private void doLogout() {
        sessionManager.clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}