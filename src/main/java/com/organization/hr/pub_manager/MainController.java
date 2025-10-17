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
import javafx.scene.layout.HBox; // Import cho HBox trong TableCell
import javafx.beans.value.ChangeListener; // Import cho change listener
import java.net.URL;
import java.util.List;
import java.util.Optional; // Import cho Optional và ButtonType
import javafx.event.ActionEvent; // <<< ĐÃ THÊM IMPORT NÀY

public class MainController {

    @FXML
    private TilePane menuGrid;

    @FXML
    private TableView<OrderItem> cartTable;

    @FXML private Label lblSubTotal;
    @FXML private TextField txtDiscount;
    @FXML private TextField txtServiceFee;
    @FXML private TextField txtTax;
    @FXML private Label lblGrandTotal;

    //    Tạo một đối tượng DAO để lấy dữ liệu từ database.
    private final MealDAO mealDao = new MealDAO();


    //    Phương thức này tự động được gọi khi giao diện FXML được tải.
    @FXML
    public void initialize() {
        System.out.println("🟢 initialize() in MainController is running...");

        // === CẤU HÌNH CÁC CỘT CHO GIỎ HÀNG (cartTable) ===

        // 0. Cột Ảnh
        TableColumn<OrderItem, String> imageCol = new TableColumn<>("Ảnh");
        imageCol.setPrefWidth(60);
        imageCol.setResizable(false);
        imageCol.setCellFactory(col -> new TableCell<OrderItem, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    OrderItem orderItem = getTableView().getItems().get(getIndex());
                    Image img = loadImageFromPath(orderItem.getImagePath());
                    imageView.setImage(img);
                    setGraphic(imageView);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));


        // 1. Cột Tên món
        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Tên món");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);


        // 2. Cột Số lượng (Tích hợp nút +/-)
        TableColumn<OrderItem, Integer> quantityActionCol = new TableColumn<>("Số lượng");
        quantityActionCol.setPrefWidth(120);
        quantityActionCol.setResizable(false);

        quantityActionCol.setCellFactory(col -> new TableCell<OrderItem, Integer>() {
            private final Button btnMinus = new Button("-");
            private final Button btnPlus = new Button("+");
            private final Label lblQuantity = new Label();
            private final HBox controls = new HBox(5, btnMinus, lblQuantity, btnPlus);

            {
                controls.setAlignment(Pos.CENTER);

                // Logic Nút Trừ (-): Giảm số lượng, nếu về 0 thì xóa khỏi giỏ
                btnMinus.setOnAction(event -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    item.setQuantity(item.getQuantity() - 1);
                    if (item.getQuantity() == 0) {
                        getTableView().getItems().remove(item);
                    }
                    getTableView().refresh();
                    updateCartSummary(); // Cập nhật tóm tắt sau khi thay đổi số lượng
                });

                // Logic Nút Cộng (+): Tăng số lượng
                btnPlus.setOnAction(event -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    item.setQuantity(item.getQuantity() + 1);
                    getTableView().refresh();
                    updateCartSummary(); // Cập nhật tóm tắt sau khi thay đổi số lượng
                });
            }

            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty || quantity == null) {
                    setGraphic(null);
                } else {
                    lblQuantity.setText(String.valueOf(quantity));
                    setGraphic(controls);
                }
            }
        });
        quantityActionCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        // 3. Cột Giá bán (Giá mặc định của 1 món)
        TableColumn<OrderItem, Double> priceCol = new TableColumn<>("Giá bán");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceCol.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0fđ", price));
                }
            }
        });

        // 4. Cột Tổng tiền (Tổng = Giá * SL)
        TableColumn<OrderItem, Double> totalCol = new TableColumn<>("Tổng tiền");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        totalCol.setCellFactory(col -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0fđ", total));
                }
            }
        });


        // Thêm các cột vào TableView
        cartTable.getColumns().clear();
        cartTable.getColumns().addAll(imageCol, nameCol, quantityActionCol, priceCol, totalCol);


        // CHỨC NĂNG MỚI: Gắn Listener để cập nhật tóm tắt khi giỏ hàng thay đổi (thêm/xóa món)
        cartTable.getItems().addListener((javafx.collections.ListChangeListener<OrderItem>) c -> updateCartSummary());

        // THAY ĐỔI TỐI ƯU: Gắn Listener cho các TextField để cập nhật tóm tắt ngay khi nhập liệu
        ChangeListener<String> summaryListener = (obs, oldVal, newVal) -> updateCartSummary();
        txtDiscount.textProperty().addListener(summaryListener);
        txtServiceFee.textProperty().addListener(summaryListener);
        txtTax.textProperty().addListener(summaryListener);


        // <<< THAY ĐỔI 1: Tải tất cả món ăn khi khởi động
        loadMeals("All");
        updateCartSummary(); // Khởi tạo tóm tắt khi tải ứng dụng
    }

    // === PHƯƠNG THỨC LỌC DANH MỤC (MỚI) ===
    /**
     * Xử lý sự kiện khi người dùng bấm vào các nút danh mục.
     * Được liên kết với onAction="#handleCategorySelection" trong FXML.
     */
    // Trong MainController.java
    @FXML
    private void handleCategorySelection(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String categoryName = clickedButton.getText();

        // Loại bỏ ký tự xuống dòng (\n) và khoảng trắng đầu/cuối.
        // Giữ nguyên ký tự '&' và dấu cách vì nó có trong tên DB: 'Đồ uống & Thuốc lá'
        categoryName = categoryName.replace("\n", "").trim();

        // Nếu bạn muốn kiểm tra, thêm dòng này:
        // System.out.println("DEBUG: Lọc theo danh mục: [" + categoryName + "]");

        loadMeals(categoryName);
    }
    // =====================================

    //    Có thể được gắn vào nút “Làm mới” trong giao diện.
    @FXML
    public void reloadMeals() {
        // <<< THAY ĐỔI 2: Tải lại tất cả món ăn khi làm mới
        loadMeals("All");
    }


    /**
     * Tải và hiển thị món ăn lên menuGrid, có hỗ trợ lọc theo danh mục.
     * @param category Tên danh mục cần lọc. Truyền "All" để hiển thị tất cả.
     */
    private void loadMeals(String category) {
        System.out.println("🟡 loadMeals() called. Category: " + category);
        menuGrid.getChildren().clear();

        new Thread(() -> {
            // <<< THAY ĐỔI 3: Sử dụng DAO để lọc trực tiếp trên database
            List<Meal> meals = mealDao.getMealsByCategory(category);

            System.out.println("🟢 Số lượng món sau khi lọc: " + meals.size());

            Platform.runLater(() -> {
                for (Meal meal : meals) {
                    menuGrid.getChildren().add(createMealCard(meal));
                }
            });
        }).start();
    }

    // === PHƯƠNG THỨC TIỆN ÍCH TẢI ẢNH (Giữ nguyên) ===
    private Image loadImageFromPath(String relativePath) {
        try {
            String path = relativePath.replace("\\", "/");
            if (!path.startsWith("/")) path = "/" + path;

            URL imageUrl = getClass().getResource("/com/organization/hr/pub_manager" + path);
            if (imageUrl == null) {
                imageUrl = getClass().getResource("/com/organization/hr/pub_manager/images/default.png");
            }

            return new Image(imageUrl.toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
            return new Image(getClass().getResource("/com/organization/hr/pub_manager/images/default.png").toExternalForm());
        }
    }


    // === LOGIC THÊM MÓN VÀO GIỎ HÀNG (Giữ nguyên) ===
    private void addToCart(Meal meal) {
        ObservableList<OrderItem> items = cartTable.getItems();
        boolean found = false;

        // 1. Kiểm tra xem món ăn đã có trong giỏ chưa (SỬ DỤNG ID để so sánh)
        for (OrderItem item : items) {
            if (item.getMealId() == meal.getId()) {
                // 2. Nếu tìm thấy: Tăng số lượng lên 1
                item.setQuantity(item.getQuantity() + 1);
                cartTable.refresh();
                found = true;
                System.out.println("✔️ Đã tăng số lượng món: " + meal.getName());
                break;
            }
        }

        // 3. Nếu không tìm thấy: Thêm món mới vào giỏ hàng
        if (!found) {
            OrderItem newItem = new OrderItem(meal.getId(), meal.getName(), meal.getPrice(), 1, meal.getImagePath());
            items.add(newItem);
            System.out.println("➕ Đã thêm món mới: " + meal.getName());
        }

        cartTable.scrollTo(items.size() - 1);
        updateCartSummary(); // Cập nhật tóm tắt sau khi thêm món
    }


    // === Tạo thẻ món ăn (Giữ nguyên) ===
    private VBox createMealCard(Meal meal) {
        Image img = loadImageFromPath(meal.getImagePath());

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

        // Logic click giữ nguyên: TRÁI -> Add to Cart, PHẢI -> Change Image
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
            // Ở đây bạn có thể muốn lưu đường dẫn tương đối thay vì tuyệt đối (file.getAbsolutePath())
            mealDao.updateMealImage(meal.getId(), file.getAbsolutePath());
            reloadMeals();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("✅ Cập nhật thành công!");
            alert.setContentText("Ảnh món \"" + meal.getName() + "\" đã được thay đổi.");
            alert.showAndWait();
        }
    }

    // === LOGIC MỚI: TÍNH TOÁN VÀ CẬP NHẬT TÓM TẮT GIỎ HÀNG (Giữ nguyên) ===
    private void updateCartSummary() {
        double subTotal = cartTable.getItems().stream()
                .mapToDouble(OrderItem::getTotal)
                .sum();

        // --- 1. Lấy giá trị từ TextFields (Giảm giá, Phí dịch vụ, Thuế) ---
        double discount = parseSummaryValue(txtDiscount.getText(), subTotal);
        double serviceFee = parseNumericValue(txtServiceFee.getText());
        double tax = parseSummaryValue(txtTax.getText(), subTotal);

        // --- 2. Tính Tổng thanh toán cuối cùng ---
        double grandTotal = subTotal - discount + serviceFee + tax;

        // Đảm bảo tổng thanh toán không âm
        if (grandTotal < 0) {
            grandTotal = 0;
        }

        // --- 3. Cập nhật giao diện ---
        lblSubTotal.setText(String.format("%,.0fđ", subTotal));
        lblGrandTotal.setText(String.format("%,.0fđ", grandTotal));
    }

    // Hàm tiện ích để chuyển đổi chuỗi thành số tiền tuyệt đối (Xử lý cả % và số tiền)
    private double parseSummaryValue(String input, double baseValue) {
        if (input == null || input.trim().isEmpty()) {
            return 0.0;
        }
        String cleanedInput = input.trim().replace(",", "").replace("đ", "").replace("VNĐ", "").trim();

        try {
            double value = Double.parseDouble(cleanedInput.replace("%", ""));

            if (input.trim().endsWith("%")) {
                // Nếu là %
                return baseValue * (value / 100.0);
            } else {
                // Nếu là số tiền tuyệt đối
                return value;
            }
        } catch (NumberFormatException e) {
            // Nếu không phải số hợp lệ, mặc định là 0
            return 0.0;
        }
    }

    // Hàm tiện ích chỉ chuyển đổi số tuyệt đối
    private double parseNumericValue(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0.0;
        }
        String cleanedInput = input.trim().replace(",", "").replace("đ", "").replace("VNĐ", "").trim();

        try {
            return Double.parseDouble(cleanedInput);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


    // === CHỨC NĂNG TẠM TÍNH (LƯU ĐƠN HÀNG) (Giữ nguyên) ===
    @FXML
    public void handleSaveOrder() {
        if (cartTable.getItems().isEmpty()) {
            showActionAlert(Alert.AlertType.WARNING, "⚠️ Lỗi", "Giỏ hàng trống", "Vui lòng chọn món trước khi tạm tính.");
            return;
        }

        Optional<ButtonType> result = showConfirmationAlert("Xác nhận Tạm tính", "Bạn có chắc chắn muốn lưu đơn hàng này vào tạm tính?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // (Thực tế): Gọi OrderDAO.saveOrder(cartTable.getItems());
            cartTable.getItems().clear();
            updateCartSummary(); // Cập nhật tóm tắt sau khi xóa giỏ hàng
            showActionAlert(Alert.AlertType.INFORMATION, "✅ Thành công", "Đơn hàng đã được tạm tính", "Đơn hàng hiện tại đã được lưu và giỏ hàng được làm sạch.");
        }
    }


    // === CHỨC NĂNG THANH TOÁN (KẾT THÚC GIAO DỊCH) (Giữ nguyên) ===
    @FXML
    public void handleCheckout() {
        if (cartTable.getItems().isEmpty()) {
            showActionAlert(Alert.AlertType.WARNING, "⚠️ Lỗi", "Giỏ hàng trống", "Vui lòng chọn món trước khi thanh toán.");
            return;
        }

        Optional<ButtonType> result = showConfirmationAlert("Xác nhận Thanh toán", "Bạn có chắc chắn muốn tiến hành thanh toán cho đơn hàng này?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // (Thực tế): Gọi OrderDAO.completeTransaction(cartTable.getItems());
            cartTable.getItems().clear();
            updateCartSummary(); // Cập nhật tóm tắt sau khi xóa giỏ hàng
            showActionAlert(Alert.AlertType.INFORMATION, "✅ Thành công", "Thanh toán hoàn tất", "Giao dịch đã được ghi nhận. Cảm ơn!");
        }
    }


    // === PHƯƠNG THỨC TIỆN ÍCH CHO ALERT (Giữ nguyên) ===
    private void showActionAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirmationAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}