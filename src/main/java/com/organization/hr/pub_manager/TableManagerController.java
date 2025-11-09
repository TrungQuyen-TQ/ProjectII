package com.organization.hr.pub_manager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;

public class TableManagerController {

    @FXML private TilePane tableGrid;
    @FXML private Label tableNameLabel;
    @FXML private Label tableStatusLabel;
    @FXML private Label totalAmountLabel;

    // 1. Khởi tạo DAO (Đã có)
    private final TableDAO tableDAO = new TableDAO();

    // === THÊM MỚI 1: Thêm OrderDAO ===
    // (Giả sử bạn đã tạo tệp OrderDAO.java)
    private final OrderDAO orderDAO = new OrderDAO();

    // === THÊM MỚI 2: Thêm liên kết đến MainController ===
    private MainController mainController;

    // === THÊM MỚI 3: Thêm phương thức để MainController "tiêm" chính nó vào ===
    /**
     * Phương thức này được MainController gọi để thiết lập liên kết.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // Tải danh sách các bàn ngay khi giao diện được mở
        loadTables();
    }

    private void loadTables() {
        // (Giữ nguyên logic loadTables của bạn, nó đã tốt rồi)
        tableGrid.getChildren().clear();
        new Thread(() -> {
            List<Table> tablesFromDb = tableDAO.getAllTables();
            Platform.runLater(() -> {
                for (Table table : tablesFromDb) {
                    tableGrid.getChildren().add(createTableCard(table));
                }
            });
        }).start();
    }

    // (GiHãy nguyên hàm createTableCard của bạn, nó đã tốt rồi)
    private VBox createTableCard(Table table) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("menu-item");
        card.setPrefSize(120, 100);

        Label nameLabel = new Label(table.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label statusLabel = new Label(table.getStatus());

        switch (table.getStatus()) {
            case "Trống":
                card.setStyle("-fx-background-color: #c8e6c9; -fx-background-radius: 12;");
                break;
            case "Có khách":
                card.setStyle("-fx-background-color: #ffcdd2; -fx-background-radius: 12;");
                break;
            case "Đã đặt":
                card.setStyle("-fx-background-color: #bbdefb; -fx-background-radius: 12;");
                break;
            default:
                card.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 12;");
                break;
        }

        card.getChildren().addAll(nameLabel, statusLabel);

        card.setOnMouseClicked(event -> {
            showTableDetails(table); // (Giữ nguyên)
        });

        return card;
    }

    // === SỬA ĐỔI PHƯƠNG THỨC NÀY ===
    // 7. Sửa lại hàm showTableDetails (PHIÊN BẢN HOÀN CHỈNH)
    private void showTableDetails(Table table) {
        // Cập nhật thông tin chi tiết trên sidebar (đã có)
        tableNameLabel.setText(table.getName());
        tableStatusLabel.setText("Trạng thái: " + table.getStatus());

        // === LOGIC MỚI ĐỂ GIẢI QUYẾT TODO ===

        // Thử lấy đơn hàng đang hoạt động (PENDING/SERVED) của bàn này
        // (Giả sử bạn đã tạo Order.java và OrderDAO.java)
        Order order = orderDAO.getActiveOrderByTableId(table.getId());

        if (order != null) {
            // 1. GIẢI QUYẾT TODO: Hiển thị tổng tiền từ đơn hàng
            totalAmountLabel.setText(String.format("%,.0fđ", order.getTotalAmount()));

            // 2. GỌI NGƯỢC LẠI MAINCONTROLLER
            // Chỉ tải đơn hàng nếu bàn "Có khách" và mainController đã được liên kết
            if (mainController != null && "Có khách".equals(table.getStatus())) {
                // Yêu cầu MainController tải đơn hàng này vào giỏ hàng
                mainController.loadOrderForTable(table.getId());
            }
        } else {
            // Nếu không có đơn hàng (bàn trống, đã đặt, đang dọn)
            totalAmountLabel.setText("0đ"); // Hiển thị 0đ (như code cũ của bạn)

            // (Tùy chọn) Nếu click vào bàn trống, bạn có thể muốn xóa giỏ hàng
            if (mainController != null) {
                mainController.clearCart(); // Gọi hàm clearCart() đã có trong MainController
            }
        }
    }
}