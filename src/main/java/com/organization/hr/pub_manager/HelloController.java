package com.organization.hr.pub_manager;

import com.organization.payment.vnpay.QrGenerator;
import com.organization.payment.vnpay.VnpayService;
import com.organization.payment.vnpay.PollingService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class HelloController {

    // --- KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN CẦN THIẾT ---

    // 1. Dùng để hiển thị ảnh QR Code (Cần fx:id="qrImageView" trong FXML)
    @FXML private ImageView qrImageView;
    // 2. Dùng để hiển thị trạng thái thanh toán (Cần fx:id="statusLabel" trong FXML)
    @FXML private Label statusLabel;

    // Component mặc định ban đầu của bạn (giữ lại nếu cần)
    @FXML private Label welcomeText;

    // --- KHAI BÁO CÁC LỚP DỊCH VỤ ---
    private final VnpayService vnpayService = new VnpayService();
    private PollingService pollingService;

    // --- Hàm Callback để Polling Service cập nhật giao diện ---
    private final Consumer<String> statusCallback = (status) -> {
        // Platform.runLater đảm bảo cập nhật giao diện trên JavaFX Application Thread
        Platform.runLater(() -> {
            switch (status.toUpperCase()) {
                case "PAID":
                    statusLabel.setText("Thanh toán: THÀNH CÔNG! ✅");
                    showAlert("Thành công", "Giao dịch đã hoàn tất.", Alert.AlertType.INFORMATION);
                    break;
                case "FAILED":
                    statusLabel.setText("Thanh toán: THẤT BẠI. ❌");
                    showAlert("Thất bại", "Giao dịch thất bại.", Alert.AlertType.WARNING);
                    break;
                case "EXPIRED":
                    statusLabel.setText("Thanh toán: HẾT HẠN. ⏱️");
                    showAlert("Hết hạn", "Giao dịch đã quá thời gian cho phép.", Alert.AlertType.WARNING);
                    break;
                case "CONNECTION_ERROR":
                    statusLabel.setText("Lỗi kết nối Server.");
                    showAlert("Lỗi", "Không thể kết nối đến Server Backend.", Alert.AlertType.ERROR);
                    break;
                case "PENDING":
                    statusLabel.setText("Đang chờ quét mã VNPAY...");
                    break;
                case "STOPPED":
                    // Không làm gì, chỉ dừng luồng
                    break;
                default:
                    statusLabel.setText("Trạng thái: " + status + " (Đang Polling...)");
            }
        });
    };

    @FXML
    public void initialize() {
        // Khởi tạo PollingService sau khi các FXML component đã được tải
        this.pollingService = new PollingService(vnpayService, statusCallback);
        // Thiết lập trạng thái ban đầu
        if (statusLabel != null) {
            statusLabel.setText("Sẵn sàng thanh toán.");
        }
    }

    @FXML
    public void handlePaymentAction() {
        // Dùng cho nút "Thanh toán" mới của bạn

        // Dừng Polling cũ nếu đang chạy
        if (pollingService != null) {
            pollingService.stopPolling("STOPPED");
        }

        // --- Dữ liệu Thanh toán Giả định (Cần lấy từ các input thực tế) ---
        long amount = 50000; // Số tiền (ví dụ: 50,000 VND)
        String orderId = "ORD" + System.currentTimeMillis(); // Mã đơn hàng duy nhất
        String orderInfo = "Thanh toan mon an " + orderId;

        // Chạy tác vụ gọi API trong luồng nền (JavaFX không cho phép chặn luồng chính)
        new Thread(() -> {
            try {
                // 1. GỌI SERVICE LẤY PAYMENT URL
                String paymentUrl = vnpayService.createPaymentUrl(amount, orderId, orderInfo);

                // 2. RENDER QR CODE
                BufferedImage qrImage = QrGenerator.generateQrCode(paymentUrl, 300);
                // Chuyển đổi AWT BufferedImage sang JavaFX Image
                Image fxImage = SwingFXUtils.toFXImage(qrImage, null);

                // Cập nhật giao diện và Bắt đầu Polling trên JavaFX Thread
                Platform.runLater(() -> {
                    qrImageView.setImage(fxImage);
                    pollingService.startPolling(orderId); // 3. BẮT ĐẦU POLLING
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khởi tạo thanh toán.");
                    showAlert("Lỗi", "Không thể khởi tạo QR Code: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    // Giữ lại hàm mặc định (chỉ để demo nếu bạn cần)
    @FXML
    protected void onHelloButtonClick() {
        if (welcomeText != null) {
            welcomeText.setText("Welcome to VNPAY Integration!");
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        // Đảm bảo alert được tạo trên JavaFX Thread
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}