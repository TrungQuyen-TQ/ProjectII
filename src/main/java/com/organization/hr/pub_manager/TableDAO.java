package com.organization.hr.pub_manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    public List<Table> getAllTables() {
        List<Table> tables = new ArrayList<>();
        String sql = "SELECT id, name, status FROM tables ORDER BY id";

        // Giả sử bạn đã có một lớp DatabaseConnection để lấy kết nối
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String status = rs.getString("status");

                Table table = new Table(id, name, status);
                tables.add(table);
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi tải danh sách bàn từ database:");
            e.printStackTrace();
        }
        return tables;
    }
}