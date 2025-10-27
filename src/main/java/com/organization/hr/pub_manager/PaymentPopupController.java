package com.organization.hr.pub_manager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PaymentPopupController {

    @FXML
    private Label amountLabel;

    @FXML
    private ImageView qrImageView;

    @FXML
    private Label statusLabel;

    // Phương thức này được MainController gọi để truyền dữ liệu vào popup
    public void setPaymentInfo(Image qrImage, long amount) {
        qrImageView.setImage(qrImage);
        amountLabel.setText(String.format("Số tiền: %,dđ", amount));
    }

    // Phương thức này được MainController gọi để cập nhật trạng thái
    public void updateStatus(String status) {
        Platform.runLater(() -> {
            switch (status.toUpperCase()) {
                case "PAID":
                    statusLabel.setText("Thanh toán THÀNH CÔNG! ✅");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    break;
                case "FAILED":
                    statusLabel.setText("Thanh toán THẤT BẠI. ❌");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    break;
                case "EXPIRED":
                    statusLabel.setText("Giao dịch HẾT HẠN. ⏱️");
                    statusLabel.setStyle("-fx-text-fill: orange;");
                    break;
                case "PENDING":
                    statusLabel.setText("Đang chờ quét mã VNPAY...");
                    statusLabel.setStyle("-fx-text-fill: black;");
                    break;
                default:
                    statusLabel.setText(status);
                    statusLabel.setStyle("-fx-text-fill: black;");
            }
        });
    }
}