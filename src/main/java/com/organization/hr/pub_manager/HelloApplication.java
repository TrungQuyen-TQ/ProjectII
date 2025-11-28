package com.organization.hr.pub_manager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Tải trực tiếp giao diện thu ngân
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("The_manager_interface.fxml"));
        // Sử dụng kích thước mặc định hoặc bạn có thể điều chỉnh
        Scene scene = new Scene(fxmlLoader.load());

        // --- PHẦN THÊM VÀO ĐỂ ĐỔI ICON ---
        try {
            // Lưu ý: Đường dẫn bắt đầu bằng dấu / để trỏ về thư mục resources gốc
            Image icon = new Image(getClass().getResourceAsStream("/com/organization/hr/pub_manager/images/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Lỗi không tìm thấy icon: " + e.getMessage());
        }
        // ---------------------------------

        stage.setTitle("Hệ Thống Thu Ngân"); // Đặt tiêu đề cho cửa sổ
        stage.setScene(scene);
        stage.setMaximized(true); // Tùy chọn: làm cho cửa sổ tối đa hóa
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("Đang đóng ứng dụng và dừng các dịch vụ...");
        // System.exit(0) là cách dứt khoát nhất để tắt
        // máy ảo Java (JVM) và tất cả các luồng của nó.
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}