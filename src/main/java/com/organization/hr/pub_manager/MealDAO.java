package com.organization.hr.pub_manager;
import com.organization.hr.pub_manager.DatabaseConnection;
import com.organization.hr.pub_manager.Meal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealDAO {

    public List<Meal> getAllMeals(){
        List<Meal> meals = new ArrayList<>();
        String query = "SELECT id, name, price, image_path FROM meals";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)){
            while (rs.next()) {
                meals.add(new Meal(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("image_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return meals;
    }


    /**
     * Lấy danh sách món ăn theo tên danh mục, sử dụng JOIN với bảng categories.
     * @param categoryName Tên danh mục ("All" để lấy tất cả).
     */
    public List<Meal> getMealsByCategory(String categoryName) {
        List<Meal> meals = new ArrayList<>();
        String sql;

        if ("All".equalsIgnoreCase(categoryName)) {
            // Lấy tất cả món ăn
            sql = "SELECT m.id, m.name, m.price, m.image_path FROM meals m";
        } else {
            // JOIN meals (m) với categories (c) và lọc theo tên danh mục (c.name)
            sql = "SELECT m.id, m.name, m.price, m.image_path " +
                    "FROM meals m JOIN categories c ON m.category_id = c.id " +
                    "WHERE c.name = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Thiết lập tham số chỉ khi không phải là "All"
            if (!"All".equalsIgnoreCase(categoryName)) {
                ps.setString(1, categoryName);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    meals.add(new Meal(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("image_path")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi truy vấn món ăn theo danh mục: " + categoryName);
            e.printStackTrace();
        }
        return meals;
    }


    public void updateMealImage(int id, String imagePath) {
        String sql = "UPDATE meals SET image_path = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, imagePath);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}