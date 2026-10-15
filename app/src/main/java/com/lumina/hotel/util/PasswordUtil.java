package com.lumina.hotel.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Băm và kiểm tra mật khẩu bằng PBKDF2 kèm salt ngẫu nhiên riêng từng tài khoản.
 * Định dạng lưu trong cột password: pbkdf2$<số vòng lặp>$<salt hex>$<hash hex>
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1"; // chạy được từ API 24
    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 160;

    private PasswordUtil() { }

    /** Băm mật khẩu thô, mỗi lần gọi ra chuỗi khác nhau (salt ngẫu nhiên). */
    public static String hash(String plain) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] h = derive(plain.toCharArray(), salt, ITERATIONS, HASH_BITS);
        return PREFIX + ITERATIONS + "$" + toHex(salt) + "$" + toHex(h);
    }

    /** true nếu chuỗi đã ở dạng băm của lớp này (tránh băm hai lần). */
    public static boolean isHashed(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * Kiểm tra mật khẩu người dùng gõ với chuỗi đã lưu.
     * Trả về false nếu chuỗi đã lưu null (tài khoản Google) hoặc sai định dạng.
     */
    public static boolean verify(String plain, String stored) {
        if (plain == null || !isHashed(stored)) return false;
        String[] p = stored.split("\\$");
        if (p.length != 4) return false;
        try {
            int iterations = Integer.parseInt(p[1]);
            byte[] salt = fromHex(p[2]);
            byte[] expected = fromHex(p[3]);
            byte[] actual = derive(plain.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int bits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bits);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không băm được mật khẩu", e);
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}