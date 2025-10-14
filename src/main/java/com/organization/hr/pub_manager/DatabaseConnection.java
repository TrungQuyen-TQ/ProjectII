package com.organization.hr.pub_manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // ⚡ Thông tin kết nối MySQL
    private static final String URL =
            "jdbc:mysql://localhost:3306/mydb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "appuser";       // hoặc root
    private static final String PASSWORD = "app123!";   // hoặc root123!

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Test thử kết nối
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✅ Kết nối MySQL thành công!");
                System.out.println("✅ Connected to: " + conn.getCatalog());
            }
        } catch (SQLException e) {
            System.out.println("❌ Kết nối thất bại!");
            e.printStackTrace();
        }
    }
}
