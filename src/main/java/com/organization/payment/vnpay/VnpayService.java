package com.organization.payment.vnpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VnpayService {

    public static String generatePaymentUrl(String orderId, long amount, String orderInfo) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", VnpayConfig.TMN_CODE);
        params.put("vnp_Amount", String.valueOf(amount * 100)); // nhân 100 theo quy định VNPAY
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", VnpayConfig.RETURN_URL);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        // Bước 2: Tạo chuỗi query
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) query.append("&");
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        String hashData = query.toString() + VnpayConfig.HASH_SECRET; // (demo đơn giản)
        String vnp_SecureHash = Integer.toHexString(hashData.hashCode());

        return VnpayConfig.VNP_URL + "?" + query + "&vnp_SecureHash=" + vnp_SecureHash;
    }
}
