package com.organization.hr.pub_manager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;

public class TableManagerController {

    @FXML
    private TilePane tableGrid;

    @FXML
    private Label tableNameLabel;

    @FXML
    private Label tableStatusLabel;

    @FXML
    private Label totalAmountLabel;

    // 1. Khởi tạo DAO để sử dụng
    private final TableDAO tableDAO = new TableDAO();

    @FXML
    public void initialize() {
        // Tải danh sách các bàn ngay khi giao diện được mở
        loadTables();
    }

    private void loadTables() {
        // 2. Xóa các bàn cũ trước khi tải lại (quan trọng)
        tableGrid.getChildren().clear();

        // 3. Sử dụng một luồng riêng để không làm đơ giao diện
        new Thread(() -> {
            // Lấy danh sách bàn từ database thông qua DAO
            List<Table> tablesFromDb = tableDAO.getAllTables();

            // Cập nhật giao diện trên luồng chính của JavaFX
            Platform.runLater(() -> {
                for (Table table : tablesFromDb) {
                    tableGrid.getChildren().add(createTableCard(table));
                }
            });
        }).start();
    }

    // 4. Sửa lại hàm createTableCard để nhận vào đối tượng Table
    private VBox createTableCard(Table table) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("menu-item"); // Tận dụng style của thẻ món ăn
        card.setPrefSize(120, 100);

        Label nameLabel = new Label(table.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label statusLabel = new Label(table.getStatus());

        // 5. Đổi màu nền dựa trên trạng thái từ database
        switch (table.getStatus()) {
            case "Trống":
                card.setStyle("-fx-background-color: #c8e6c9; -fx-background-radius: 12;"); // Xanh lá
                break;
            case "Có khách":
                card.setStyle("-fx-background-color: #ffcdd2; -fx-background-radius: 12;"); // Đỏ nhạt
                break;
            case "Đã đặt":
                card.setStyle("-fx-background-color: #bbdefb; -fx-background-radius: 12;"); // Xanh dương
                break;
            default:
                card.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 12;"); // Xám
                break;
        }

        card.getChildren().addAll(nameLabel, statusLabel);

        // 6. Thêm sự kiện click để xem chi tiết
        card.setOnMouseClicked(event -> {
            showTableDetails(table);
        });

        return card;
    }

    // 7. Sửa lại hàm showTableDetails để nhận vào đối tượng Table
    private void showTableDetails(Table table) {
        tableNameLabel.setText(table.getName());
        tableStatusLabel.setText("Trạng thái: " + table.getStatus());

        // TODO: Viết logic để tải tổng tiền của bàn từ database
        // Ví dụ: double total = orderDAO.getTotalForTable(table.getId());
        totalAmountLabel.setText("0đ"); // Tạm thời hiển thị 0đ
    }
}