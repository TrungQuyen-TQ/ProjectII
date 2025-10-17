package com.organization.hr.pub_manager;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.geometry.Pos;
import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;

import java.net.URL;
import java.util.List;

public class MainController {
    //    @FXML dùng để liên kết biến này với phần tử trong file FXML (ví dụ MainView.fxml).
//menuGrid là một lưới (TilePane) — nơi các “thẻ món ăn” (card) sẽ được thêm vào để hiển thị.
    @FXML
    private TilePane menuGrid;

    // LIÊN KẾT MỚI: Liên kết với TableView giỏ hàng trong FXML
    @FXML
    private TableView<OrderItem> cartTable;


    //    Tạo một đối tượng DAO để lấy dữ liệu từ database.
//Mỗi khi cần danh sách món ăn → gọi mealDAO.getAllMeals().
    private final MealDAO mealDao = new MealDAO();

    //    Phương thức này tự động được gọi khi giao diện FXML được tải.
    @FXML
    public void initialize() {
        System.out.println("🟢 initialize() in MainController is running...");

        // === BƯỚC MỚI: CẤU HÌNH CÁC CỘT CHO GIỎ HÀNG (cartTable) ===
        // 1. Cột Tên món
        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Tên món");
        // "name" phải khớp với tên thuộc tính trong OrderItem (getName())
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);

        // 2. Cột Số lượng
        TableColumn<OrderItem, Integer> quantityCol = new TableColumn<>("SL");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(50);
        quantityCol.setStyle("-fx-alignment: CENTER;");

        // 3. Cột Tổng tiền
        TableColumn<OrderItem, Double> totalCol = new TableColumn<>("Tổng tiền");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        // Format giá trị hiển thị (tùy chọn)

        // Thêm các cột vào TableView (xóa cột mặc định nếu có)
        cartTable.getColumns().clear();
        cartTable.getColumns().addAll(nameCol, quantityCol, totalCol);

        loadMeals();
    }


    //    Có thể được gắn vào nút “Làm mới” trong giao diện.
    @FXML
    public void reloadMeals() {
        loadMeals();
    }


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


    // === LOGIC MỚI: Thêm món vào giỏ hàng ===
    private void addToCart(Meal meal) {
        ObservableList<OrderItem> items = cartTable.getItems();
        boolean found = false;

        // 1. Kiểm tra xem món ăn đã có trong giỏ chưa
        for (OrderItem item : items) {
            // Dùng tên món để so sánh đơn giản
            if (item.getName().equals(meal.getName())) {
                // 2. Nếu tìm thấy: Tăng số lượng lên 1
                item.setQuantity(item.getQuantity() + 1);
                // Cần gọi cartTable.refresh() để buộc TableView cập nhật lại dữ liệu/tổng tiền
                cartTable.refresh();
                found = true;
                System.out.println("✔️ Đã tăng số lượng món: " + meal.getName());
                break;
            }
        }

        // 3. Nếu không tìm thấy: Thêm món mới vào giỏ hàng
        if (!found) {
            OrderItem newItem = new OrderItem(meal.getName(), meal.getPrice(), 1);
            items.add(newItem);
            System.out.println("➕ Đã thêm món mới: " + meal.getName());
        }

        // Tự động cuộn xuống cuối bảng (tùy chọn)
        cartTable.scrollTo(items.size() - 1);
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
                imageUrl = getClass().getResource("/com/organization/hr/pub_manager/images/default.png");
            }

            img = new Image(imageUrl.toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
            img = new Image(getClass().getResource("/com/organization/hr/pub_manager/images/default.png").toExternalForm());
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

        // THAY ĐỔI LOGIC CLICK:
        // Click chuột TRÁI (PRIMARY) -> Thêm vào giỏ hàng
        // Click chuột PHẢI (SECONDARY) -> Đổi ảnh (giữ lại chức năng cũ)
        box.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                addToCart(meal);
            } else if (e.getButton() == MouseButton.SECONDARY) {
                changeMealImage(meal);
            }
        });

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
