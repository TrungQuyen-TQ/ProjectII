package com.organization.hr.pub_manager;

import com.organization.hr.pub_manager.DatabaseConnection;
import com.organization.hr.pub_manager.Meal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealDAO {

    // Lấy tất cả sản phẩm
    public List<Meal> getAllProducts() {
        List<Meal> products = new ArrayList<>();
        String query = "SELECT id, name, price, image_url FROM products";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                products.add(new Meal(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("image_url") // ✅ đổi image_path → image_url
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Lấy danh sách sản phẩm theo tên danh mục, sử dụng JOIN với bảng categories.
     * @param categoryName Tên danh mục ("All" để lấy tất cả).
     */
    public List<Meal> getMealsByCategory(String categoryName) {
        List<Meal> products = new ArrayList<>();
        String sql;

        if ("All".equalsIgnoreCase(categoryName)) {
            sql = "SELECT p.id, p.name, p.price, p.image_url FROM products p";
        } else {
            sql = "SELECT p.id, p.name, p.price, p.image_url " +
                    "FROM products p JOIN categories c ON p.category_id = c.id " +
                    "WHERE c.name = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!"All".equalsIgnoreCase(categoryName)) {
                ps.setString(1, categoryName);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(new Meal(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("image_url") // ✅ đổi image_path → image_url
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi truy vấn sản phẩm theo danh mục: " + categoryName);
            e.printStackTrace();
        }
        return products;
    }

    // Cập nhật ảnh sản phẩm
    public void updateProductImage(int id, String imageUrl) {
        String sql = "UPDATE products SET image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, imageUrl); // ✅ đổi imagePath → imageUrl
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
