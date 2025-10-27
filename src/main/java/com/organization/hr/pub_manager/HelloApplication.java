package com.organization.hr.pub_manager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        showLoginScreen(stage);
    }

    public void showLoginScreen(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 450, 350);
        stage.setTitle("Đăng Nhập");
        stage.setScene(scene);
        stage.show();
    }

    public void showMainStage(Stage stage, String role) throws IOException {
        String fxmlFile;
        String title;

        if ("ADMIN".equals(role)) {
            // Tạm thời dùng lại giao diện bán hàng cho Admin, bạn sẽ thay bằng Dashboard.fxml
            fxmlFile = "Dashboard.fxml";
            title = "Bảng Điều Khiển - Admin";
        } else { // EMPLOYEE
            fxmlFile = "The_manager_interface.fxml";
            title = "Hệ Thống Bán Hàng - Nhân Viên";
        }

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}