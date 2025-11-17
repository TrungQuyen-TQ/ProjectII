package com.organization.hr.pub_manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    /**
     * [MỚI] Lấy TẤT CẢ món ăn từ TẤT CẢ các đơn hàng đang hoạt động của một bàn.
     * Hàm này gộp Order lần 1, Order lần 2... thành một danh sách chung để hiển thị.
     */
    public List<OrderItem> getCombinedOrderItems(int tableId) {
        List<OrderItem> items = new ArrayList<>();

        // SQL: Join 3 bảng để lấy món ăn từ các đơn hàng PENDING/COOKED/SERVED của bàn này
        String sql = "SELECT od.product_id, p.name, od.price, od.quantity, p.image_url " +
                "FROM order_details od " +
                "JOIN products p ON od.product_id = p.id " +
                "JOIN orders o ON od.order_id = o.id " +
                "WHERE o.table_id = ? " +
                "AND o.status IN ('PENDING', 'COOKED', 'SERVED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * [MỚI] Thanh toán TẤT CẢ các đơn hàng đang hoạt động của bàn này cùng lúc.
     * (Chuyển hết sang PAID cho cả bảng orders và order_details)
     */
    public void payAllActiveOrders(int tableId, String newStatus) {
        // SQL 1: Cập nhật bảng orders chính
        String sqlOrders = "UPDATE orders SET status = ? " +
                "WHERE table_id = ? AND status IN ('PENDING', 'COOKED', 'SERVED')";

        // SQL 2: Cập nhật bảng order_details (dựa trên các order vừa tìm được)
        String sqlDetails = "UPDATE order_details od " +
                "JOIN orders o ON od.order_id = o.id " +
                "SET od.status = ? " +
                "WHERE o.table_id = ? AND o.status IN ('PENDING', 'COOKED', 'SERVED')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Cập nhật chi tiết trước
            try (PreparedStatement ps2 = conn.prepareStatement(sqlDetails)) {
                ps2.setString(1, newStatus);
                ps2.setInt(2, tableId);
                ps2.executeUpdate();
            }

            // 2. Cập nhật bảng orders
            try (PreparedStatement ps1 = conn.prepareStatement(sqlOrders)) {
                ps1.setString(1, newStatus);
                ps1.setInt(2, tableId);
                ps1.executeUpdate();
            }

            conn.commit();
            System.out.println("✅ Đã thanh toán tất cả đơn hàng cho bàn " + tableId);

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Lấy đơn hàng đang hoạt động (PENDING, COOKED hoặc SERVED) của một bàn cụ thể.
     * (Giữ lại để lấy tổng tiền hoặc thông tin đơn lẻ nếu cần)
     */
    public Order getActiveOrderByTableId(int tableId) {
        Order order = null;
        // Lấy đơn hàng mới nhất
        String sql = "SELECT id, table_id, status, total_amount " +
                "FROM orders " +
                "WHERE table_id = ? AND status IN ('PENDING', 'COOKED', 'SERVED') " +
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
        return order;
    }

    /**
     * Lấy chi tiết các món ăn theo orderId cụ thể.
     */
    public List<OrderItem> getOrderDetailsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();
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
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // Hàm cập nhật trạng thái đơn lẻ (Giữ nguyên)
    public void updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        String sql2 = "UPDATE order_details SET status = ? WHERE order_id = ?";

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sql)) {
                ps1.setString(1, status);
                ps1.setInt(2, orderId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                ps2.setString(1, status);
                ps2.setInt(2, orderId);
                ps2.executeUpdate();
            }

            conn.commit();
            System.out.println("✅ Đã cập nhật trạng thái order " + orderId + " thành: " + status);

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    // Hàm đồng bộ lại chi tiết đơn hàng (Giữ nguyên)
    public void resyncOrderDetails(int orderId, List<OrderItem> finalItems, double finalTotal) {
        String deleteSql = "DELETE FROM order_details WHERE order_id = ?";
        String insertSql = "INSERT INTO order_details (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        String updateTotalSql = "UPDATE orders SET total_amount = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                psDelete.setInt(1, orderId);
                psDelete.executeUpdate();
            }

            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                for (OrderItem item : finalItems) {
                    psInsert.setInt(1, orderId);
                    psInsert.setInt(2, item.getMealId());
                    psInsert.setInt(3, item.getQuantity());
                    psInsert.setDouble(4, item.getPrice());
                    psInsert.addBatch();
                }
                psInsert.executeBatch();
            }

            try (PreparedStatement psUpdateTotal = conn.prepareStatement(updateTotalSql)) {
                psUpdateTotal.setDouble(1, finalTotal);
                psUpdateTotal.setInt(2, orderId);
                psUpdateTotal.executeUpdate();
            }

            conn.commit();
            System.out.println("✅ Đã đồng bộ hóa Order " + orderId + " với " + finalItems.size() + " món.");

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }
}