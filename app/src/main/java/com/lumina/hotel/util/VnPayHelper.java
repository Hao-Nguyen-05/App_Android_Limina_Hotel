package com.lumina.hotel.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * PROMPT #8 — Tích hợp thanh toán VNPay Sandbox thật qua WebView.
 * Ký HMAC-SHA512 được tạo NGAY TRONG APP (chấp nhận rủi ro bảo mật vì chỉ
 * chạy Sandbox demo, không dùng cho sản phẩm thật).
 */
public class VnPayHelper {

    private static final String PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String RETURN_URL = "lumina://payment-return";

    private final String tmnCode;
    private final String hashSecret;

    public VnPayHelper(String tmnCode, String hashSecret) {
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
    }

    /**
     * Ghép URL thanh toán đầy đủ, đã ký vnp_SecureHash.
     * amount: số tiền VNĐ THẬT (chưa nhân 100) — hàm tự nhân 100 vì VNPay tính theo xu.
     */
    public String buildPaymentUrl(long amount, String orderId, String orderInfo) {
        Map<String, String> params = new TreeMap<>();
        java.text.SimpleDateFormat fmt =
                new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US);
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CreateDate", fmt.format(new java.util.Date()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", RETURN_URL);
        params.put("vnp_TxnRef", orderId);

        List<String> fieldNames = new ArrayList<>(params.keySet());
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> it = fieldNames.iterator();
        while (it.hasNext()) {
            String name = it.next();
            String value = params.get(name);
            if (value != null && !value.isEmpty()) {
                hashData.append(name).append('=').append(encode(value));
                query.append(encode(name)).append('=').append(encode(value));
                if (it.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }
        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        return PAY_URL + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /** So lại chữ ký từ tham số VNPay trả về — KHÔNG tin vnp_ResponseCode nếu sai chữ ký. */
    public boolean verifyReturnParams(Map<String, String> returnParams) {
        Map<String, String> params = new TreeMap<>(returnParams);
        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        if (receivedHash == null) return false;

        List<String> fieldNames = new ArrayList<>(params.keySet());
        StringBuilder hashData = new StringBuilder();
        Iterator<String> it = fieldNames.iterator();
        while (it.hasNext()) {
            String name = it.next();
            String value = params.get(name);
            if (value != null && !value.isEmpty()) {
                hashData.append(name).append('=').append(encode(value));
                if (it.hasNext()) hashData.append('&');
            }
        }
        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return "";
        }
    }
}