package com.organization.hr.pub_manager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Tải trực tiếp giao diện thu ngân
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("The_manager_interface.fxml"));
        // Sử dụng kích thước mặc định hoặc bạn có thể điều chỉnh
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("Hệ Thống Thu Ngân"); // Đặt tiêu đề cho cửa sổ
        stage.setScene(scene);
        stage.setMaximized(true); // Tùy chọn: làm cho cửa sổ tối đa hóa
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}