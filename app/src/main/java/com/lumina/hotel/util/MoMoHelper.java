package com.lumina.hotel.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

/**
 * PROMPT #10 (PHỤ LỤC E) — Tích hợp thanh toán MoMo Test thật, dùng bộ khóa
 * test công khai chính thức của MoMo (không phải thông tin riêng của Vio).
 * Khác VNPay: phải POST xin payUrl trước, không build URL tĩnh được.
 */
public class MoMoHelper {

    private static final String PARTNER_CODE = "MOMO";
    private static final String ACCESS_KEY = "F8BBA842ECF85";
    private static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private static final String CREATE_URL = "https://test-payment.momo.vn/v2/gateway/api/create";
    static final String REDIRECT_URL = "lumina://momo-return";
    private static final String IPN_URL = "https://momo.vn";

    /**
     * Gọi API tạo giao dịch MoMo — BẮT BUỘC gọi trên luồng nền (AppExecutors.io()).
     * @return payUrl nếu thành công, null nếu lỗi mạng hoặc resultCode != 0.
     */
    public String createPaymentRequest(long amount, String orderId, String orderInfo) {
        try {
            String requestId = orderId;
            String extraData = "";
            String requestType = "payWithMethod";

            String rawSignature = "accessKey=" + ACCESS_KEY
                    + "&amount=" + amount
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + IPN_URL
                    + "&orderId=" + orderId
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + PARTNER_CODE
                    + "&redirectUrl=" + REDIRECT_URL
                    + "&requestId=" + requestId
                    + "&requestType=" + requestType;
            String signature = hmacSHA256(SECRET_KEY, rawSignature);

            JSONObject body = new JSONObject();
            body.put("partnerCode", PARTNER_CODE);
            body.put("partnerName", "Lumina Hotel");
            body.put("storeId", "LuminaHotelStore");
            body.put("requestId", requestId);
            body.put("amount", amount);
            body.put("orderId", orderId);
            body.put("orderInfo", orderInfo);
            body.put("redirectUrl", REDIRECT_URL);
            body.put("ipnUrl", IPN_URL);
            body.put("lang", "vi");
            body.put("extraData", extraData);
            body.put("requestType", requestType);
            body.put("signature", signature);

            URL url = new URL(CREATE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 200 && code < 300
                    ? conn.getInputStream() : conn.getErrorStream();
            String response = readAll(is);
            conn.disconnect();

            JSONObject json = new JSONObject(response);
            if (json.optInt("resultCode", -1) == 0) {
                return json.optString("payUrl", null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** So lại chữ ký MoMo redirect về — KHÔNG tin resultCode nếu sai chữ ký. */
    public boolean verifyReturnParams(Map<String, String> p) {
        String receivedSignature = p.get("signature");
        if (receivedSignature == null) return false;

        String rawSignature = "accessKey=" + ACCESS_KEY
                + "&amount=" + nz(p.get("amount"))
                + "&extraData=" + nz(p.get("extraData"))
                + "&message=" + nz(p.get("message"))
                + "&orderId=" + nz(p.get("orderId"))
                + "&orderInfo=" + nz(p.get("orderInfo"))
                + "&orderType=" + nz(p.get("orderType"))
                + "&partnerCode=" + nz(p.get("partnerCode"))
                + "&payType=" + nz(p.get("payType"))
                + "&requestId=" + nz(p.get("requestId"))
                + "&responseTime=" + nz(p.get("responseTime"))
                + "&resultCode=" + nz(p.get("resultCode"))
                + "&transId=" + nz(p.get("transId"));

        String calculated = hmacSHA256(SECRET_KEY, rawSignature);
        return calculated.equalsIgnoreCase(receivedSignature);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String readAll(java.io.InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        is.close();
        return sb.toString();
    }

    private static String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}