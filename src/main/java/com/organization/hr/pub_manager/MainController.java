package com.organization.hr.pub_manager;

// Các Import cần thiết cho MainController (logic ứng dụng)
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Random; // Dùng cho MealDAO placeholder

// --- IMPORT CHO TÍCH HỢP VNPAY (ĐÃ GỘP TỪ HELLOCONTROLLER) ---
import com.organization.payment.vnpay.QrGenerator;
import com.organization.payment.vnpay.VnpayService;
import com.organization.payment.vnpay.PollingService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.io.IOException;
// -----------------------------------------------------------

public class MainController {

    // --- @FXML CÁC THÀNH PHẦN ỨNG DỤNG CƠ BẢN ---
    @FXML private TilePane menuGrid;
    @FXML private TableView<OrderItem> cartTable;

    // --- @FXML CÁC THÀNH PHẦN VNPAY (ĐÃ THÊM VÀO FXML) ---
    @FXML private ImageView qrImageView;
    @FXML private Label statusLabel;
    @FXML private Label welcomeText; // Component mặc định

    // --- DỊCH VỤ VÀ POLLING ---
    private final MealDAO mealDao = new MealDAO();
    private final VnpayService vnpayService = new VnpayService();
    private PollingService pollingService;

    // --- CALLBACK & DỊCH VỤ VNPAY ---
    private final Consumer<String> statusCallback = this::updateVnpayStatus;

    private void updateVnpayStatus(String status) {
        // Platform.runLater đảm bảo cập nhật giao diện trên JavaFX Application Thread
        Platform.runLater(() -> {
            switch (status.toUpperCase()) {
                case "PAID":
                    statusLabel.setText("Thanh toán: THÀNH CÔNG! ✅");
                    showAlert("Thành công", "Giao dịch đã hoàn tất.", Alert.AlertType.INFORMATION);
                    if (pollingService != null) pollingService.stopPolling("STOPPED");
                    break;
                case "FAILED":
                    statusLabel.setText("Thanh toán: THẤT BẠI. ❌");
                    showAlert("Thất bại", "Giao dịch thất bại.", Alert.AlertType.WARNING);
                    if (pollingService != null) pollingService.stopPolling("STOPPED");
                    break;
                case "EXPIRED":
                    statusLabel.setText("Thanh toán: HẾT HẠN. ⏱️");
                    showAlert("Hết hạn", "Giao dịch đã quá thời gian cho phép.", Alert.AlertType.WARNING);
                    if (pollingService != null) pollingService.stopPolling("STOPPED");
                    break;
                case "CONNECTION_ERROR":
                    statusLabel.setText("Lỗi kết nối Server.");
                    showAlert("Lỗi", "Không thể kết nối đến Server Backend.", Alert.AlertType.ERROR);
                    break;
                case "PENDING":
                    statusLabel.setText("Đang chờ quét mã VNPAY...");
                    break;
                case "STOPPED":
                    // Dừng
                    break;
                default:
                    statusLabel.setText("Trạng thái: " + status + " (Đang Polling...)");
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        // Đảm bảo alert được tạo trên JavaFX Thread
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- HÀM KHỞI TẠO (INITIALIZE) ---
    @FXML
    public void initialize() {
        System.out.println("🟢 initialize() in MainController is running...");

        // === Logic CẤU HÌNH CÁC CỘT CHO GIỎ HÀNG (GIỮ NGUYÊN) ===
        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Tên món");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);

        TableColumn<OrderItem, Integer> quantityCol = new TableColumn<>("SL");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(50);
        quantityCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderItem, Double> totalCol = new TableColumn<>("Tổng tiền");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        cartTable.getColumns().clear();
        cartTable.getColumns().addAll(nameCol, quantityCol, totalCol);

        loadMeals();

        // === KHỞI TẠO POLLING SERVICE (GỘP TỪ HELLOCONTROLLER) ===
        if (statusLabel != null) {
            // Khởi tạo PollingService sử dụng VnpayService và hàm callback
            this.pollingService = new PollingService(vnpayService, statusCallback);
            statusLabel.setText("Sẵn sàng thanh toán.");
        }
    }

    // --- HÀM XỬ LÝ THANH TOÁN VNPAY ---
    @FXML
    public void handlePaymentAction() {
        // Dừng Polling cũ nếu đang chạy
        if (pollingService != null) {
            pollingService.stopPolling("STOPPED");
        }

        // --- LOGIC TÍNH TỔNG TIỀN THỰC TẾ ---
        long totalAmount = (long) cartTable.getItems().stream()
                .mapToDouble(OrderItem::getTotal)
                .sum();

        // VNPAY cần số tiền dưới dạng Long (đồng)
        long amount = totalAmount > 0 ? totalAmount : 50000; // Sử dụng 50000 nếu giỏ hàng rỗng

        // --- Dữ liệu Thanh toán ---
        String orderId = "ORD" + System.currentTimeMillis();
        String orderInfo = "Thanh toan don hang " + orderId;

        // Chạy tác vụ gọi API trong luồng nền
        new Thread(() -> {
            try {
                // 1. GỌI SERVICE LẤY PAYMENT URL
                String paymentUrl = vnpayService.createPaymentUrl(amount, orderId, orderInfo);

                // 2. RENDER QR CODE
                BufferedImage qrImage = QrGenerator.generateQrCode(paymentUrl, 300);
                Image fxImage = SwingFXUtils.toFXImage(qrImage, null);

                // Cập nhật giao diện và Bắt đầu Polling trên JavaFX Thread
                Platform.runLater(() -> {
                    qrImageView.setImage(fxImage);
                    pollingService.startPolling(orderId);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khởi tạo thanh toán.");
                    showAlert("Lỗi", "Không thể khởi tạo QR Code: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    // --- CÁC HÀM XỬ LÝ ỨNG DỤNG KHÁC (GIỮ NGUYÊN) ---

    @FXML
    public void reloadMeals() {
        loadMeals();
    }

    private void loadMeals() {
        System.out.println("🟡 loadMeals() called...");
        if (menuGrid == null) return; // Bảo vệ nếu chưa được khởi tạo
        menuGrid.getChildren().clear();

        new Thread(() -> {
            List<Meal> meals = mealDao.getAllMeals();

            Platform.runLater(() -> {
                for (Meal meal : meals) {
                    menuGrid.getChildren().add(createMealCard(meal));
                }
            });
        }).start();
    }


    private void addToCart(Meal meal) {
        if (cartTable == null) return;

        ObservableList<OrderItem> items = cartTable.getItems();
        if (items == null) {
            items = FXCollections.observableArrayList();
            cartTable.setItems(items);
        }

        boolean found = false;

        for (OrderItem item : items) {
            if (item.getName().equals(meal.getName())) {
                item.setQuantity(item.getQuantity() + 1);
                cartTable.refresh();
                found = true;
                break;
            }
        }

        if (!found) {
            OrderItem newItem = new OrderItem(meal.getName(), meal.getPrice(), 1);
            items.add(newItem);
        }

        cartTable.scrollTo(items.size() - 1);
    }


    private VBox createMealCard(Meal meal) {
        // ... (Giữ nguyên logic tạo card)
        Image img;
        try {
            // Logic tải ảnh của bạn
            String path = meal.getImagePath().replace("\\", "/");
            if (!path.startsWith("/")) path = "/" + path;

            URL imageUrl = getClass().getResource("/com/organization/hr/pub_manager" + path);
            if (imageUrl == null) {
                imageUrl = getClass().getResource("/com/organization/hr/pub_manager/images/default.png");
            }

            img = new Image(imageUrl.toExternalForm());
        } catch (Exception e) {
            // Fallback nếu có lỗi
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
        // ... (Giữ nguyên logic đổi ảnh)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh cho món: " + meal.getName());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh PNG/JPG", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            // mealDao.updateMealImage(meal.getId(), file.getAbsolutePath()); // Sử dụng hàm thực tế của bạn
            reloadMeals();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("✅ Cập nhật thành công!");
            alert.setContentText("Ảnh món \"" + meal.getName() + "\" đã được thay đổi.");
            alert.showAndWait();
        }
    }

    @FXML
    protected void onHelloButtonClick() {
        if (welcomeText != null) {
            welcomeText.setText("Welcome to VNPAY Integration!");
        }
    }

    // ************************************************
    // PLACEHOLDER: BẠN CẦN ĐẢM BẢO CÓ CÁC CLASS NÀY Ở ĐÂU ĐÓ
    // (Thường là trong một file riêng)
    // ************************************************

    // Giả định class Meal
    public static class Meal {
        private final int id;
        private final String name;
        private final double price;
        private final String imagePath;

        public Meal(int id, String name, double price, String imagePath) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imagePath = imagePath;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getImagePath() { return imagePath; }
    }

    // Giả định class OrderItem
    public static class OrderItem {
        private final String name;
        private double price;
        private int quantity;

        public OrderItem(String name, double price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }

        public double getTotal() { return price * quantity; }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
            // Thông thường, TableView không cập nhật tự động khi thuộc tính thay đổi
            // nên cần gọi cartTable.refresh() hoặc dùng SimpleIntegerProperty.
        }
    }

    // Giả định class MealDAO (Data Access Object)
    public static class MealDAO {
        public List<Meal> getAllMeals() {
            // Trả về dữ liệu giả định nếu chưa kết nối database
            return List.of(
                    new Meal(1, "Phở Bò", 45000.0, "/images/pho.jpg"),
                    new Meal(2, "Bún Chả", 40000.0, "/images/buncha.jpg"),
                    new Meal(3, "Bún Bò Huế", 55000.0, "/images/bunbo.jpg"),
                    new Meal(4, "Cơm Tấm", 48000.0, "/images/comtam.jpg")
            );
        }
        // Giả định
        public void updateMealImage(int id, String newPath) {
            System.out.println("Đã gọi hàm cập nhật ảnh cho ID: " + id);
        }
        // Thêm vào MainController.java để xử lý các nút danh mục trong FXML

    }
    @FXML
    private void handleCategorySelection(javafx.event.ActionEvent event) {
        // Đây là phương thức bị thiếu trong Controller
        Button button = (Button) event.getSource();
        String category = button.getText();
        System.out.println("Đã chọn danh mục: " + category);

        // Bạn có thể thêm logic load lại món ăn theo danh mục tại đây
        // loadMealsByCategory(category);
    }
}