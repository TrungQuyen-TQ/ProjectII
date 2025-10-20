package com.organization.payment.vnpay;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import java.io.IOException;

public class VnpayService {

    // ĐỊA CHỈ API CỦA BACKEND NEXT.JS (SỬ DỤNG CỔNG 8888)
    private static final String CREATE_PAYMENT_URL = "http://localhost:8888/order/create_payment_url";
    private static final String QUERY_DR_URL = "http://localhost:8888/order/querydr";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // --- Cấu trúc dữ liệu cho Request/Response ---

    // JSON Request gửi đến /order/create_payment_url
    private static class CreatePaymentRequest {
        long amount;
        String orderId;
        String orderInfo;
        String bankCode = "NCB"; // Hoặc Bank Code mặc định khác
        String language = "vn";
    }

    // JSON Response từ /order/create_payment_url
    private static class PaymentResponse {
        String paymentUrl;
        String message;
    }

    // JSON Response từ /order/querydr
    private static class QueryResponse {
        String status; // Ví dụ: 'PAID', 'PENDING', 'FAILED'
        // Bạn có thể thêm các trường khác như vnp_ResponseCode, vnp_TransactionStatus nếu Backend trả về
    }

    /**
     * Gửi yêu cầu đến Backend để lấy URL thanh toán VNPAY
     */
    public String createPaymentUrl(long amount, String orderId, String orderInfo) throws IOException, InterruptedException, RuntimeException {
        CreatePaymentRequest requestBody = new CreatePaymentRequest();
        requestBody.amount = amount;
        requestBody.orderId = orderId;
        requestBody.orderInfo = orderInfo;

        String jsonInput = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CREATE_PAYMENT_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            PaymentResponse resObject = gson.fromJson(response.body(), PaymentResponse.class);
            if ("OK".equals(resObject.message) && resObject.paymentUrl != null) {
                return resObject.paymentUrl;
            } else {
                throw new RuntimeException("Backend returned error: " + resObject.message);
            }
        } else {
            throw new RuntimeException("HTTP Error " + response.statusCode() + " from Backend.");
        }
    }

    /**
     * Gửi yêu cầu Polling đến Backend để kiểm tra trạng thái giao dịch
     */
    public String checkPaymentStatus(String orderId) throws IOException, InterruptedException {
        // Cấu trúc querydr request của bạn là POST và cần orderId
        String jsonInput = String.format("{\"orderId\": \"%s\"}", orderId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(QUERY_DR_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            QueryResponse resObject = gson.fromJson(response.body(), QueryResponse.class);
            return resObject.status;
        }
        return "ERROR"; // Trả về lỗi nếu không kết nối được
    }
}