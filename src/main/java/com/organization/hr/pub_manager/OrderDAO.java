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

    public void updateOrderStatus(int orderId, String status) {
        // Lấy từ CSDL
        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();

            System.out.println("✅ Đã cập nhật trạng thái order " + orderId + " thành: " + status);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resyncOrderDetails(int orderId, List<OrderItem> finalItems, double finalTotal) {
        String deleteSql = "DELETE FROM order_details WHERE order_id = ?";
        String insertSql = "INSERT INTO order_details (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        String updateTotalSql = "UPDATE orders SET total_amount = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Xóa tất cả chi tiết đơn hàng cũ
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                psDelete.setInt(1, orderId);
                psDelete.executeUpdate();
            }

            // 2. Thêm lại các chi tiết đơn hàng mới (5 món)
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                for (OrderItem item : finalItems) {
                    psInsert.setInt(1, orderId);
                    psInsert.setInt(2, item.getMealId()); // Dùng getMealId() từ OrderItem
                    psInsert.setInt(3, item.getQuantity());
                    psInsert.setDouble(4, item.getPrice());
                    psInsert.addBatch();
                }
                psInsert.executeBatch();
            }

            // 3. Cập nhật tổng tiền mới cho đơn hàng
            try (PreparedStatement psUpdateTotal = conn.prepareStatement(updateTotalSql)) {
                psUpdateTotal.setDouble(1, finalTotal);
                psUpdateTotal.setInt(2, orderId);
                psUpdateTotal.executeUpdate();
            }

            conn.commit(); // Hoàn tất Transaction
            System.out.println("✅ Đã đồng bộ hóa Order " + orderId + " với " + finalItems.size() + " món.");

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // Hoàn tác nếu có lỗi
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}