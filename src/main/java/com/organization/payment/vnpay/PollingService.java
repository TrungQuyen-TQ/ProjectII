package com.organization.payment.vnpay;

import javafx.application.Platform;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PollingService {

    private final VnpayService vnpayService;
    private ScheduledExecutorService scheduler;
    private final Consumer<String> statusUpdateCallback;
    private final long maxWaitTimeMs = 15 * 60 * 1000; // 15 phút

    // Khởi tạo Polling Service
    public PollingService(VnpayService vnpayService, Consumer<String> statusUpdateCallback) {
        this.vnpayService = vnpayService;
        this.statusUpdateCallback = statusUpdateCallback;
    }

    public void startPolling(String orderId) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        final long startTime = System.currentTimeMillis();

        // Task Polling
        Runnable pollingTask = () -> {
            try {
                // Kiểm tra hết thời gian chờ
                if (System.currentTimeMillis() - startTime >= maxWaitTimeMs) {
                    stopPolling("EXPIRED");
                    return;
                }

                // Gọi API kiểm tra trạng thái
                String status = vnpayService.checkPaymentStatus(orderId);

                // Cập nhật trạng thái trên JavaFX Application Thread
                Platform.runLater(() -> statusUpdateCallback.accept(status));

                if ("PAID".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
                    stopPolling(status);
                }
            } catch (Exception e) {
                // Xử lý lỗi (ví dụ: mất kết nối Backend)
                System.err.println("Polling error: " + e.getMessage());
                Platform.runLater(() -> statusUpdateCallback.accept("CONNECTION_ERROR"));
            }
        };

        // Bắt đầu ngay, lặp lại mỗi 5 giây
        scheduler.scheduleAtFixedRate(pollingTask, 0, 5, TimeUnit.SECONDS);
        Platform.runLater(() -> statusUpdateCallback.accept("PENDING"));
    }

    public void stopPolling(String finalStatus) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown(); // Dừng Polling
            // Cập nhật trạng thái cuối cùng
            Platform.runLater(() -> statusUpdateCallback.accept(finalStatus));
        }
    }
}