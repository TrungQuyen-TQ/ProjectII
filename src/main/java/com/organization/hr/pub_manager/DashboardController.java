package com.organization.hr.pub_manager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {

    @FXML
    private Button logoutButton;

    @FXML
    private void handleManageMeals() {
        // Tương lai: bạn có thể tải một FXML khác vào vùng <center> của BorderPane
        System.out.println("Chức năng 'Quản lý món ăn' đang được phát triển.");
        // Ví dụ: mainBorderPane.setCenter(loadFXML("ManageMeals.fxml"));
    }

    @FXML
    private void handleLogout() throws IOException {
        // Lấy Stage (cửa sổ) hiện tại từ nút logout
        Stage currentStage = (Stage) logoutButton.getScene().getWindow();

        // Khởi tạo lại ứng dụng để hiển thị màn hình đăng nhập
        HelloApplication app = new HelloApplication();
        // Tạo một Stage mới để tránh lỗi "Stage is already showing"
        app.start(new Stage());

        // Đóng cửa sổ dashboard hiện tại
        currentStage.close();
    }
}