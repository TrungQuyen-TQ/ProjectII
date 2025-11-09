// Tệp: OrderDAO.java
package com.organization.hr.pub_manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    /**
     * Lấy đơn hàng đang hoạt động (PENDING hoặc SERVED) của một bàn cụ thể.
     */
    public Order getActiveOrderByTableId(int tableId) {
        Order order = null;
        // Lấy từ CSDL
        String sql = "SELECT id, table_id, status, total_amount " +
                "FROM orders " +
                "WHERE table_id = ? AND (status = 'PENDING' OR status = 'SERVED') " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order = new Order(
                            rs.getInt("id"),
                            rs.getInt("table_id"),
                            rs.getString("status"),
                            rs.getDouble("total_amount")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return order; // Trả về null nếu không tìm thấy
    }

    /**
     * Lấy chi tiết các món ăn (OrderItem) của một đơn hàng.
     */
    public List<OrderItem> getOrderDetailsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        // Join 3 bảng CSDL
        String sql = "SELECT od.product_id, p.name, od.price, od.quantity, p.image_url " +
                "FROM order_details od " +
                "JOIN products p ON od.product_id = p.id " +
                "WHERE od.order_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity"),
                            rs.getString("image_url") // Lấy image_url từ bảng products
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}