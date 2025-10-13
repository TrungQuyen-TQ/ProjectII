package com.organization.hr.pub_manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // ⚡ Thông tin kết nối SQL Server
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;"   // server + port
                    + "databaseName=HRManagement;"      // 👉 tạm thời dùng master, sau tạo DB thì đổi
                    + "encrypt=true;"
                    + "trustServerCertificate=true;";
    private static final String USER = "sa";             // user mặc định
    private static final String PASSWORD = "Sa123456!";  // password khi bạn run container

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Test thử kết nối
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✅ Kết nối SQL Server thành công!");
                System.out.println("✅ Connected to: " + conn.getCatalog());
            }
        } catch (SQLException e) {
            System.out.println("❌ Kết nối thất bại!");
            e.printStackTrace();
        }

    }
}
