package com.organization.hr.pub_manager;

import com.organization.hr.pub_manager.Meal;
import com.organization.hr.pub_manager.MealDAO;
import com.organization.hr.pub_manager.OrderItem;

import com.organization.hr.pub_manager.Order;
import com.organization.hr.pub_manager.OrderDAO;
import com.organization.hr.pub_manager.TableManagerController;

// Các Import cần thiết cho MainController (logic ứng dụng)
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import javafx.scene.layout.HBox;

// --- IMPORT CHO TÍCH HỢP VNPAY ---
import com.organization.payment.vnpay.QrGenerator;
import com.organization.payment.vnpay.VnpayService;
import com.organization.payment.vnpay.PollingService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.io.IOException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
// -----------------------------------------------------------

public class MainController {

    @FXML private BorderPane mainBorderPane; // Thêm fx:id cho BorderPane gốc

    private Node menuView;

    private final OrderDAO orderDAO = new OrderDAO();

    private PaymentPopupController popupController;

    // --- @FXML CÁC THÀNH PHẦN ỨNG DỤNG CƠ BẢN ---
    @FXML private TilePane menuGrid;
    @FXML private TableView<OrderItem> cartTable;

    // --- @FXML CÁC THÀNH PHẦN TÓM TẮT ĐƠN HÀNG ---
    @FXML private TextField txtDiscount;
    @FXML private TextField txtTax;
    @FXML private TextField txtServiceFee;
    @FXML private Label subTotalLabel;
    @FXML private Label grandTotalLabel;

    // --- @FXML CÁC THÀNH PHẦN VNPAY ---
    @FXML private ImageView qrImageView;
    @FXML private Label statusLabel;
    @FXML private Label welcomeText;

    // --- @FXML CÁC THÀNH PHẦN THANH TOÁN TIỀN MẶT ---
    @FXML private TextField txtCashReceived;
    @FXML private Label lblChangeDue;

    // --- DỊCH VỤ VÀ POLLING ---
    private final MealDAO mealDao = new MealDAO();
    private final VnpayService vnpayService = new VnpayService();
    private PollingService pollingService;

    // --- CALLBACK & DỊCH VỤ VNPAY ---
    private final Consumer<String> statusCallback = this::updateVnpayStatus;

    // Phương thức cho nút "Bàn làm việc"
    @FXML
    private void showTableView() {
        try {
            // Tải file FXML quản lý bàn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Table_manager.fxml"));
            Node tableView = loader.load();
            // Lấy controller của Table_manager.fxml
            TableManagerController tableManagerController = loader.getController();
            // Đưa MainController (this) cho TableManagerController biết
            tableManagerController.setMainController(this);
            // Đặt giao diện quản lý bàn vào khu vực trung tâm của BorderPane
            mainBorderPane.setCenter(tableView);
        } catch (IOException e) {
            e.printStackTrace();
            // Hiển thị lỗi cho người dùng nếu cần
        }
    }

    // Phương thức cho nút "Thực đơn" để quay lại
    @FXML
    private void showMenuView() {
        if (menuView != null) {
            mainBorderPane.setCenter(menuView);
        }
    }

    public void loadOrderForTable(int tableId) {
        // 1. Tìm đơn hàng đang hoạt động (PENDING/SERVED) của bàn
        // (Giả sử bạn đã tạo Order.java và OrderDAO.java)
        Order order = orderDAO.getActiveOrderByTableId(tableId);

        if (order == null) {
            // Nếu không có đơn hàng (ví dụ: bàn trống), chỉ thông báo
            showAlert("Thông báo", "Bàn này hiện không có đơn hàng nào đang mở.", Alert.AlertType.INFORMATION);
            // Xóa giỏ hàng cũ (nếu có)
            clearCart();
            return;
        }

        // 2. Lấy tất cả các món ăn chi tiết của đơn hàng đó
        // (OrderDAO sẽ trả về List<OrderItem>)
        List<OrderItem> items = orderDAO.getOrderDetailsByOrderId(order.getId());

        // 3. Xóa giỏ hàng hiện tại và nạp các món mới vào
        clearCart(); // clearCart() đã bao gồm cả việc reset summary
        cartTable.getItems().addAll(items);

        // 4. Cập nhật lại tổng tiền (clearCart() có thể đã gọi, nhưng gọi lại cho chắc)
        updateCartSummary();
        calculateChangeDue();

        // 5. Tự động chuyển về giao diện thực đơn/thanh toán
        showMenuView();

        System.out.println("✅ Đã tải đơn hàng " + order.getId() + " của bàn " + tableId + " vào giỏ hàng.");
    }

    private void updateVnpayStatus(String status) {
        // Cập nhật cho popup nếu nó đang tồn tại
        if (popupController != null) {
            popupController.updateStatus(status);
        }

        // Xử lý logic chính khi có kết quả cuối cùng
        Platform.runLater(() -> {
            switch (status.toUpperCase()) {
                case "PAID":
                    showAlert("Thành công", "Giao dịch đã hoàn tất. Đơn hàng đã được lưu.", Alert.AlertType.INFORMATION);
                    if (pollingService != null) pollingService.stopPolling("STOPPED");
                    clearCart();
                    break;
                case "FAILED":
                    showAlert("Thất bại", "Giao dịch thất bại.", Alert.AlertType.WARNING);
                    if (pollingService != null) pollingService.stopPolling("STOPPED");
                    break;
                case "EXPIRED":
                    showAlert("Hết hạn", "Giao dịch đã quá thời gian cho phép.", Alert.AlertType.WARNING);
                    if (pollingService != null) pollingService.stopPolling("STOPPED");
                    break;
                // Bỏ các case khác chỉ để hiển thị status, vì popup đã làm việc đó
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    // --- HÀM KHỞI TẠO (INITIALIZE) ---
    @FXML
    public void initialize() {
        System.out.println("🟢 initialize() in MainController is running (FULL COMBINED VERSION)...");

        // === CẤU HÌNH CÁC CỘT CHO GIỎ HÀNG (Bổ sung cột Ảnh) ===
        cartTable.getColumns().clear();

        // 1. Cột STT của món ăn
        TableColumn<OrderItem, String> sttCol = new TableColumn<>("STT");
        sttCol.setPrefWidth(30);
        sttCol.setResizable(false);
        sttCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(cartTable.getItems().indexOf(data.getValue()) + 1)));

        // 2. Cột Ảnh món ăn
        TableColumn<OrderItem, String> imageCol = new TableColumn<>("Ảnh");
        imageCol.setPrefWidth(60);
        imageCol.setResizable(false);
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        imageCol.setCellFactory(new Callback<TableColumn<OrderItem, String>, TableCell<OrderItem, String>>() {
            @Override
            public TableCell<OrderItem, String> call(TableColumn<OrderItem, String> param) {
                return new TableCell<OrderItem, String>() {
                    private final ImageView imageView = new ImageView();
                    {
                        imageView.setFitWidth(40);
                        imageView.setFitHeight(40);
                        imageView.setPreserveRatio(true);
                        imageView.setImage(new Image(getClass().getResource("/com/organization/hr/pub_manager/images/default.png").toExternalForm()));
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || item.isEmpty()) {
                            setGraphic(null);
                        } else {
                            try {
                                String path = item.replace("\\", "/");
                                if (!path.startsWith("/")) path = "/" + path;
                                URL imageUrl = getClass().getResource("/com/organization/hr/pub_manager" + path);

                                if (imageUrl == null) {
                                    imageView.setImage(new Image(getClass().getResource("/com/organization/hr/pub_manager/images/default.png").toExternalForm()));
                                } else {
                                    imageView.setImage(new Image(imageUrl.toExternalForm()));
                                }
                            } catch (Exception e) {
                                System.err.println("Lỗi tải ảnh cho OrderItem: " + item + " - " + e.getMessage());
                                imageView.setImage(new Image(getClass().getResource("/com/organization/hr/pub_manager/images/default.png").toExternalForm()));
                            }
                            setGraphic(imageView);
                            setAlignment(Pos.CENTER);
                        }
                    }
                };
            }
        });


        // 3. Cột Tên món
        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Tên món");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);

        // 4. Cột Giá bán
        TableColumn<OrderItem, Double> priceCol = new TableColumn<>("Giá bán");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // 5. Cột Số lượng (Tích hợp nút +/-)
        TableColumn<OrderItem, Integer> quantityCol = new TableColumn<>("SL");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(120);
        quantityCol.setStyle("-fx-alignment: CENTER;");

        // CellFactory tùy chỉnh để thêm Button
        quantityCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<OrderItem, Integer> call(TableColumn<OrderItem, Integer> param) {
                return new TableCell<>() {
                    private final HBox container = new HBox(5);
                    private final Button btnMinus = new Button("-");
                    private final Label lblQuantity = new Label();
                    private final Button btnPlus = new Button("+");

                    {
                        btnMinus.setStyle("-fx-padding: 2 5 2 5; -fx-font-size: 10px;");
                        btnPlus.setStyle("-fx-padding: 2 5 2 5; -fx-font-size: 10px;");

                        container.setAlignment(Pos.CENTER);
                        container.getChildren().addAll(btnMinus, lblQuantity, btnPlus);

                        btnMinus.setOnAction(event -> {
                            OrderItem item = getTableView().getItems().get(getIndex());
                            int newQuantity = item.getQuantity() - 1;
                            if (newQuantity > 0) {
                                item.setQuantity(newQuantity);
                            } else {
                                getTableView().getItems().remove(item);
                            }
                            getTableView().refresh();
                            updateCartSummary();
                            calculateChangeDue();
                        });

                        btnPlus.setOnAction(event -> {
                            OrderItem item = getTableView().getItems().get(getIndex());
                            item.setQuantity(item.getQuantity() + 1);
                            getTableView().refresh();
                            updateCartSummary();
                            calculateChangeDue();
                        });
                    }

                    @Override
                    protected void updateItem(Integer quantity, boolean empty) {
                        super.updateItem(quantity, empty);
                        if (empty || quantity == null) {
                            setGraphic(null);
                        } else {
                            lblQuantity.setText(String.valueOf(quantity));
                            setGraphic(container);
                        }
                    }
                };
            }
        });

        // 6. Cột Tổng tiền
        TableColumn<OrderItem, Double> totalCol = new TableColumn<>("Tổng tiền");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // THÊM CỘT ẢNH VÀO DANH SÁCH
        cartTable.getColumns().addAll(sttCol, imageCol, nameCol, priceCol, quantityCol, totalCol);
        // === HẾT CẤU HÌNH CỘT GIỎ HÀNG ===

        loadMeals("All");

        // === KHỞI TẠO POLLING SERVICE (VNPAY) ===
        // === KHỞI TẠO POLLING SERVICE (VNPAY) ===
        // Luôn khởi tạo PollingService vì nó cần thiết cho chức năng thanh toán popup
        this.pollingService = new PollingService(vnpayService, statusCallback);

        // Chỉ cập nhật statusLabel trên giao diện chính nếu nó tồn tại
        if (statusLabel != null) {
            statusLabel.setText("Sẵn sàng thanh toán.");
        }

        // === LOGIC TÍNH TOÁN VÀ CẬP NHẬT TÓM TẮT GIỎ HÀNG ===
        if (cartTable.getItems() == null) {
            cartTable.setItems(FXCollections.observableArrayList());
        }

        // 1. Gắn Listener để cập nhật tóm tắt khi giỏ hàng thay đổi
        cartTable.getItems().addListener((javafx.collections.ListChangeListener<OrderItem>) change -> {
            updateCartSummary();
            calculateChangeDue();
        });

        // 2. Gắn Listener cho các TextField giảm giá/thuế
        ChangeListener<String> summaryListener = (obs, oldValue, newValue) -> {
            updateCartSummary();
            calculateChangeDue();
        };
        if (txtDiscount != null) txtDiscount.textProperty().addListener(summaryListener);
        if (txtTax != null) txtTax.textProperty().addListener(summaryListener);
        if (txtServiceFee != null) txtServiceFee.textProperty().addListener(summaryListener);

        // 3. Gắn Listener cho TextField tiền mặt nhận (CẬP NHẬT: Thêm dấu phân cách)
        if (txtCashReceived != null) {
            txtCashReceived.textProperty().addListener((obs, oldValue, newValue) -> {
                // 1. Loại bỏ tất cả các ký tự không phải số (và dấu phân cách cũ nếu có)
                String cleanText = newValue.replaceAll("[^\\d]", "");

                if (!cleanText.isEmpty()) {
                    try {
                        // 2. Chuyển đổi thành số và định dạng lại
                        double number = Double.parseDouble(cleanText);
                        String formattedText = String.format("%,.0f", number);

                        // 3. Cập nhật lại TextField
                        if (!formattedText.equals(txtCashReceived.getText())) {
                            Platform.runLater(() -> {
                                txtCashReceived.setText(formattedText);
                                // Giữ con trỏ ở cuối
                                txtCashReceived.positionCaret(formattedText.length());
                            });
                        }
                    } catch (NumberFormatException e) {
                        // Nếu không phải là số hợp lệ, reset về giá trị cũ
                        Platform.runLater(() -> txtCashReceived.setText(oldValue));
                    }
                } else {
                    Platform.runLater(() -> txtCashReceived.setText(""));
                }

                // Luôn gọi calculateChangeDue() sau khi thay đổi
                calculateChangeDue();
            });
        }

        // Đảm bảo cập nhật lần đầu tiên
        updateCartSummary();
        calculateChangeDue();

        if (this.mainBorderPane != null) {
            this.menuView = this.mainBorderPane.getCenter();
        }
    }

    // --- HÀM XỬ LÝ THANH TOÁN VNPAY ---
    @FXML
    public void handlePaymentAction() {
        System.out.println("✅ Nút Thanh toán Thẻ (F9) ĐÃ ĐƯỢC NHẤN!"); // <--- THÊM DÒNG NÀY
        if (pollingService != null) {
            pollingService.stopPolling("STOPPED");
        }

        long totalAmount = (long) Math.ceil(parseValue(grandTotalLabel.getText().replace("đ", "").replace(",", "").trim(), 0.0));

        if (totalAmount <= 0) {
            showAlert("Lỗi", "Giỏ hàng trống hoặc tổng tiền bằng 0. Không thể thanh toán.", Alert.AlertType.WARNING);
            return;
        }

        long amount = totalAmount > 0 ? totalAmount : 50000;
        String orderId = "ORD" + System.currentTimeMillis();
        String orderInfo = "Thanh toan don hang " + orderId;

        new Thread(() -> {
            try {
                String paymentUrl = vnpayService.createPaymentUrl(amount, orderId, orderInfo);
                BufferedImage qrImage = QrGenerator.generateQrCode(paymentUrl, 300);
                Image fxImage = SwingFXUtils.toFXImage(qrImage, null);

                Platform.runLater(() -> {
                    try {
                        // Tải FXML của popup
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("PaymentPopup.fxml"));
                        VBox popupRoot = loader.load();

                        // Lấy controller của popup
                        popupController = loader.getController();
                        popupController.setPaymentInfo(fxImage, amount);

                        // Tạo một Stage (cửa sổ) mới cho popup
                        Stage popupStage = new Stage();
                        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ chính
                        popupStage.setTitle("Thanh toán VNPAY");
                        popupStage.setScene(new javafx.scene.Scene(popupRoot));

                        // Bắt đầu polling ngay khi popup hiển thị
                        pollingService.startPolling(orderId);

                        // Hiển thị và chờ cho đến khi popup được đóng
                        popupStage.showAndWait();

                        // Khi popup đóng, dừng polling và reset controller
                        pollingService.stopPolling("STOPPED");
                        popupController = null;

                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlert("Lỗi", "Không thể mở cửa sổ thanh toán.", Alert.AlertType.ERROR);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Không thể khởi tạo QR Code: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    // --- LOGIC TÓM TẮT ĐƠN HÀNG (CHI TIẾT) ---
    private void updateCartSummary() {
        if (cartTable == null || subTotalLabel == null || grandTotalLabel == null) return;

        // 1. Tính Sub Total
        double subTotal = cartTable.getItems().stream()
                .mapToDouble(OrderItem::getTotal)
                .sum();

        double grandTotal = subTotal;

        // --- 1. Lấy giá trị từ TextFields (Giảm giá, Phí dịch vụ, Thuế) ---
        String discountText = txtDiscount != null ? txtDiscount.getText().trim() : "";
        String taxText = txtTax != null ? txtTax.getText().trim() : "";
        String serviceFeeText = txtServiceFee != null ? txtServiceFee.getText().trim() : "";

        // Giảm giá (Trừ)
        double discountAmount = parseValue(discountText, subTotal);
        grandTotal -= discountAmount;

        // Phí dịch vụ (Cộng)
        double serviceFeeAmount = parseValue(serviceFeeText, subTotal);
        grandTotal += serviceFeeAmount;

        // Thuế (Cộng)
        double taxAmount = parseValue(taxText, subTotal);
        grandTotal += taxAmount;

        // Đảm bảo tổng tiền không âm
        grandTotal = Math.max(0, grandTotal);

        // --- 2. Cập nhật giao diện ---
        subTotalLabel.setText(String.format("%,.0fđ", subTotal));
        grandTotalLabel.setText(String.format("%,.0fđ", grandTotal));
    }

    // --- HÀM TÍNH TOÁN TIỀN THỪA/THIẾU ---
    private void calculateChangeDue() {
        if (grandTotalLabel == null || txtCashReceived == null || lblChangeDue == null) return;

        // 1. Lấy Tổng tiền phải trả (Grand Total)
        double grandTotal = parseValue(grandTotalLabel.getText().replace("đ", "").trim(), 0.0);

        // 2. Lấy Số tiền mặt khách đưa (Xử lý chuỗi có dấu phân cách)
        double cashReceived;
        try {
            // Loại bỏ tất cả các ký tự không phải số
            String cashText = txtCashReceived.getText().replaceAll("[^\\d]", "").trim();
            cashReceived = cashText.isEmpty() ? 0.0 : Double.parseDouble(cashText);
        } catch (NumberFormatException e) {
            cashReceived = 0.0;
        }

        // 3. Tính toán Tiền trả lại (Change Due)
        double changeDue = cashReceived - grandTotal;

        // 4. Cập nhật giao diện chuyên nghiệp
        Platform.runLater(() -> {
            if (changeDue > 0) {
                // Tiền thừa (trả lại khách)
                lblChangeDue.setText(String.format("Trả lại: %,.0fđ", changeDue));
                lblChangeDue.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (changeDue < 0) {
                // Tiền thiếu
                double amountMissing = Math.abs(changeDue);
                lblChangeDue.setText(String.format("Còn thiếu: %,.0fđ", amountMissing));
                lblChangeDue.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else {
                // Đủ tiền
                lblChangeDue.setText("Đã nhận đủ (0đ)");
                lblChangeDue.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
            }
        });
    }

    // Hàm tiện ích để chuyển đổi chuỗi thành số tiền tuyệt đối (Xử lý cả % và số tiền)
    private double parseValue(String text, double base) {
        if (text.isEmpty()) return 0.0;
        text = text.replace(",", "").replace(".", ""); // Xóa dấu phân cách (nếu có)

        if (text.endsWith("%")) {
            // Xử lý phần trăm
            try {
                double percentage = Double.parseDouble(text.substring(0, text.length() - 1)) / 100.0;
                return base * percentage;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else {
            // Xử lý số tiền tuyệt đối
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
    }

    // --- CHỨC NĂNG TẠM TÍNH (LƯU ĐƠN HÀNG) ---
    @FXML
    public void handleSaveOrder() {
        showAlert("Thông báo", "Đơn hàng đã được TẠM TÍNH (LƯU) thành công.", Alert.AlertType.INFORMATION);
    }



    // --- HÀM XỬ LÝ THANH TOÁN BẰNG TIỀN MẶT ---
    @FXML
    public void handleCashPayment() {
        double grandTotal = parseValue(grandTotalLabel.getText().replace("đ", "").trim(), 0.0);
        double cashReceived;
        try {
            // Lấy giá trị từ TextField và loại bỏ tất cả các ký tự không phải số
            String cashText = txtCashReceived.getText().replaceAll("[^\\d]", "").trim();
            cashReceived = cashText.isEmpty() ? 0.0 : Double.parseDouble(cashText);
        } catch (NumberFormatException e) {
            showAlert("Lỗi nhập liệu", "Vui lòng nhập số tiền hợp lệ vào ô 'Tiền mặt nhận'.", Alert.AlertType.ERROR);
            return;
        }

        if (grandTotal <= 0) {
            showAlert("Lỗi", "Giỏ hàng trống hoặc tổng tiền bằng 0.", Alert.AlertType.WARNING);
            return;
        }

        if (cashReceived < grandTotal) {
            double amountMissing = grandTotal - cashReceived;
            showAlert("Thiếu tiền", String.format("Khách hàng còn thiếu %,.0fđ. Vui lòng nhận đủ tiền trước khi hoàn tất.", amountMissing), Alert.AlertType.WARNING);
            return;
        }

        // Hoàn tất giao dịch tiền mặt
        double change = cashReceived - grandTotal;
        String message = String.format("Thanh toán TIỀN MẶT đã hoàn tất.\nTổng tiền: %,.0fđ\nKhách đưa: %,.0fđ\nTiền trả lại: %,.0fđ",
                grandTotal, cashReceived, change);

        showAlert("Giao dịch thành công", message, Alert.AlertType.INFORMATION);
        clearCart(); // Xóa giỏ hàng và đặt lại các trường nhập liệu
    }


    public void clearCart() {
        if (cartTable != null) {
            cartTable.getItems().clear();
            // Đặt lại các trường nhập liệu
            if (txtDiscount != null) txtDiscount.setText("");
            if (txtTax != null) txtTax.setText("");
            if (txtServiceFee != null) txtServiceFee.setText("");
            if (txtCashReceived != null) txtCashReceived.setText(""); // ĐẶT LẠI TIỀN MẶT NHẬN
            updateCartSummary(); // Cập nhật lại tổng tiền về 0
            calculateChangeDue(); // Cập nhật lại tiền thừa về 0
        }
    }

    // --- CÁC HÀM XỬ LÝ ỨNG DỤNG KHÁC (GIỮ NGUYÊN) ---

    @FXML
    public void reloadMeals() {
        loadMeals("All");
    }

    private void loadMeals(String categoryName) {
        System.out.println("🟡 loadMeals() called for category: " + categoryName);
        if (menuGrid == null) return;
        menuGrid.getChildren().clear();

        new Thread(() -> {
//            List<Meal> meals = mealDao.getAllMeals();
            List<Meal> meals = mealDao.getMealsByCategory(categoryName);

            Platform.runLater(() -> {
                for (Meal meal : meals) {
                    menuGrid.getChildren().add(createMealCard(meal));
                }
                if (meals.isEmpty()) {
                    menuGrid.getChildren().add(new Label("Không tìm thấy món ăn trong danh mục này."));
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

        // 1. Kiểm tra xem món ăn đã có trong giỏ chưa (SỬ DỤNG ID để so sánh)
        for (OrderItem item : items) {
            if (item.getMealId() == meal.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                cartTable.refresh();
                found = true;
                break;
            }
        }

        if (!found) {
            OrderItem newItem = new OrderItem(
                    meal.getId(),
                    meal.getName(),
                    meal.getPrice(),
                    1,
                    meal.getImagePath()
            );
            items.add(newItem);
        }

        cartTable.scrollTo(items.size() - 1);
        updateCartSummary();
        calculateChangeDue();
    }


    private VBox createMealCard(Meal meal) {
        Image img;
        try {
            String path = meal.getImagePath().replace("\\", "/");
            if (!path.startsWith("/")) path = "/" + path;

            URL imageUrl = getClass().getResource("/com/organization/hr/pub_manager" + path);
            if (imageUrl == null) {
                imageUrl = getClass().getResource("/com/organization/hr/pub_manager/images/default.png");
            }

            img = new Image(imageUrl.toExternalForm());
        } catch (Exception e) {
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh cho món: " + meal.getName());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh PNG/JPG", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            // mealDao.updateMealImage(meal.getId(), file.getAbsolutePath());
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

    @FXML
    private void handleCategorySelection(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String category = button.getText();
        System.out.println("Đã chọn danh mục: " + category);
         loadMeals(category); // Giữ lại logic gọi DAO lọc theo category nếu cần

    }
}