// File: src/main/java/module-info.java

module com.organization.hr.pub_manager {

    // Modules Java tích hợp sẵn
    requires java.sql;
    requires java.net.http;
    requires java.desktop; // Đã thêm để khắc phục lỗi BufferedImage

    // --> DÒNG CẦN THIẾT ĐỂ KHẮC PHỤC LỖI NÀY (embed.swing) <--
    requires javafx.swing;

    requires javafx.controls;
    requires javafx.fxml;

    // Modules Thư viện ngoài
    requires com.google.gson;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // Khai báo Mở Gói (Opens/Exports)
    opens com.organization.hr.pub_manager to javafx.fxml;
    exports com.organization.hr.pub_manager;

    opens com.organization.payment.vnpay to com.google.gson;
}