package com.organization.hr.pub_manager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLoginAction() throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đủ thông tin.");
            return;
        }

        String role = authService.authenticate(username, password);

        if (role != null) {
            // Đăng nhập thành công, đóng cửa sổ hiện tại
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // Mở giao diện chính dựa trên vai trò
            HelloApplication app = new HelloApplication();
            app.showMainStage(currentStage, role);

        } else {
            // Đăng nhập thất bại
            errorLabel.setText("Tên đăng nhập hoặc mật khẩu không đúng.");
        }
    }
}