package com.organization.hr.pub_manager;
import com.organization.hr.pub_manager.MealDAO;
import com.organization.hr.pub_manager.Meal;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.geometry.Pos;
import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.net.URL;

import java.util.List;

public class MainController {
//    @FXML dùng để liên kết biến này với phần tử trong file FXML (ví dụ MainView.fxml).
//menuGrid là một lưới (TilePane) — nơi các “thẻ món ăn” (card) sẽ được thêm vào để hiển thị.
//🧠 Hình dung:
//menuGrid giống như một khay đựng nhiều món ăn trên màn hình.
    @FXML
    private TilePane menuGrid;

//    Tạo một đối tượng DAO để lấy dữ liệu từ database.
//Mỗi khi cần danh sách món ăn → gọi mealDAO.getAllMeals().
    private final MealDAO mealDao = new MealDAO();

//    Phương thức này tự động được gọi khi giao diện FXML được tải.
//    Nó gọi loadMeals() để hiển thị danh sách món ăn ngay khi mở chương trình.
//    📍Ví dụ: Khi người dùng mở app “Menu quán ăn”, phần initialize() này sẽ lo tải và vẽ danh sách món lên ngay.
@FXML
public void initialize() {
    System.out.println("🟢 initialize() in MainController is running...");
    loadMeals();
}


//    Có thể được gắn vào nút “Làm mới” trong giao diện.
//    Khi người dùng nhấn nút đó → gọi lại loadMeals() để cập nhật danh sách món mới nhất.
    @FXML
    public void reloadMeals() {
        loadMeals();
    }

//    Đây là phần cốt lõi:
//            menuGrid.getChildren().clear();
//→ Xóa hết món cũ khỏi giao diện (để tránh trùng lặp khi tải lại).
//            new Thread(() -> { ... }).start();
//→ Tải dữ liệu trong luồng riêng (background thread) → tránh “đơ giao diện”.
//            Platform.runLater(() -> { ... });
//→ Khi có dữ liệu, quay lại luồng giao diện (UI thread) để vẽ các món ăn lên màn hình.
//            🧠 Nói nôm na:
//            “Dữ liệu được nấu trong bếp (thread nền),
//    rồi bưng ra bàn (UI) bằng người phục vụ Platform.runLater()”.

    private void loadMeals() {
        System.out.println("🟡 loadMeals() called...");
        menuGrid.getChildren().clear();

        new Thread(() -> {
            List<Meal> meals = mealDao.getAllMeals();
            System.out.println("🟢 Số lượng món lấy được: " + meals.size());
            for (Meal m : meals) {
                System.out.println(" - " + m.getName() + " | " + m.getPrice() + " | " + m.getImagePath());
            }

            Platform.runLater(() -> {
                for (Meal meal : meals) {
                    menuGrid.getChildren().add(createMealCard(meal));
                }
            });
        }).start();
    }



    // === Tạo thẻ món ăn ===
    private VBox createMealCard(Meal meal) {
        Image img;
        try {
            String path = meal.getImagePath().replace("\\", "/");
            if (!path.startsWith("/")) path = "/" + path;

            URL imageUrl = getClass().getResource("/com/organization/hr/pub_manager" + path);
            if (imageUrl == null) {
                System.out.println("⚠️ Không tìm thấy ảnh: " + path + " → dùng default.png");
                imageUrl = getClass().getResource("/com/organization/hr/pub_manager/icons/default.png");
            }

            img = new Image(imageUrl.toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
            img = new Image(getClass().getResource("/com/organization/hr/pub_manager/icons/default.png").toExternalForm());
        }



        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(meal.getName());
        Label priceLabel = new Label(String.format("%,.0fđ", meal.getPrice()));
        priceLabel.getStyleClass().add("price");

        VBox box = new VBox(8, imageView, nameLabel, priceLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("menu-item");

        // Khi click vào món => chọn ảnh mới
        box.setOnMouseClicked(e -> changeMealImage(meal));

        return box;
    }

    private void changeMealImage(Meal meal) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh cho món: " + meal.getName());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh PNG/JPG", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            mealDao.updateMealImage(meal.getId(), file.getAbsolutePath());
            reloadMeals();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("✅ Cập nhật thành công!");
            alert.setContentText("Ảnh món \"" + meal.getName() + "\" đã được thay đổi.");
            alert.showAndWait();
        }
    }



}
