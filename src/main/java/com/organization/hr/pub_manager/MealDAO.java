package com.organization.hr.pub_manager;
import com.organization.hr.pub_manager.DatabaseConnection;
import com.organization.hr.pub_manager.Meal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealDAO {
//    Trả về một danh sách (List) các món ăn (Meal).
//    Mục tiêu: Lấy tất cả món ăn từ bảng meals.
    public List<Meal> getAllMeals(){

//        Tạo một danh sách trống để chứa các món ăn lấy từ database.
        List<Meal> meals = new ArrayList<>();

//        Câu SQL này lấy ba cột (id, name, price) từ bảng meals.
        String query = "SELECT id, name, price, image_path FROM meals";

//        Kết nối tới database và thực thi truy vấn
//        Connection: kết nối tới database.
//→ DatabaseConnection.getConnection() là hàm khác (do em hoặc hệ thống viết) để mở kết nối.
//
//        Statement: dùng để thực thi câu lệnh SQL.
//                ResultSet: chứa kết quả truy vấn (giống như bảng dữ liệu tạm thời trong Java).
//✅ Lưu ý:
//        Khối try (...) { ... } ở đây dùng try-with-resources, giúp tự động đóng kết nối sau khi xong việc (rất tốt về hiệu năng và tránh lỗi rò rỉ tài nguyên).
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
//        rs.next() di chuyển con trỏ đến dòng kế tiếp trong kết quả truy vấn.
//                rs.getInt("id"), rs.getString("name"), rs.getDouble("price") → lấy giá trị từ cột tương ứng.
//                new Meal(...) → tạo đối tượng Meal từ các giá trị đó.
//                meals.add(meal) → thêm món ăn vừa tạo vào danh sách.

//        Tóm tắt để ghi nhớ
//        MealDAO là lớp “trung gian” giúp chương trình Java nói chuyện với database.
//        Phương thức getAllMeals() mở kết nối → chạy câu SQL → đọc kết quả → tạo đối tượng Meal → trả về danh sách món ăn.

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
