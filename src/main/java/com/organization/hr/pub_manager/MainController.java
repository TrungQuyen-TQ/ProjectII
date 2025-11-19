package com.organization.hr.pub_manager;

// Các Import cần thiết cho MainController (logic ứng dụng)
import javafx.collections.FXCollections;
        import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
        import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.geometry.Pos;
        import javafx.stage.Stage;

        import java.net.URL;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import javafx.scene.layout.HBox;

import javafx.scene.control.Pagination;
import javafx.scene.Node;
import javafx.geometry.Insets;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
// -----------------------------------------------------------

public class MainController {

    @FXML private BorderPane mainBorderPane; // Thêm fx:id cho BorderPane gốc

//    private Node menuView;

    private final OrderDAO orderDAO = new OrderDAO();
    private final TableDAO tableDAO = new TableDAO(); // DAO để cập nhật bàn
    private PaymentService paymentService;

    private PaymentPopupController popupController;

    // --- @FXML CÁC THÀNH PHẦN ỨNG DỤNG CƠ BẢN ---
//    @FXML private TilePane menuGrid;
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

    private int currentTableId = -1;


    private List<Table> allTablesList; // Lưu trữ TẤT CẢ các bàn
    private final int tablesPerPage = 20; // Đặt số bàn mỗi trang (bạn có thể đổi số 20)

    private final VnpayService vnpayService = new VnpayService();
    private PollingService pollingService;



    // --- CALLBACK & DỊCH VỤ VNPAY ---
    private final Consumer<String> statusCallback = this::updateVnpayStatus;

    @FXML private Label tableNameLabel;
    @FXML private Label tableStatusLabel;
    @FXML private Label totalAmountLabel;

    @FXML private Pagination tablePagination;

    // === BIẾN MỚI CHO POLLING BÀN ===
    private ScheduledExecutorService tablePoller;
    private final int POLLING_INTERVAL_SECONDS = 5; // Kiểm tra CSDL mỗi 5 giây

    // === HÀM TẢI MÓN (ĐÃ SỬA ĐỔI CHO GỘP ĐƠN) ===
    public void loadOrderForTable(int tableId) {
        // 1. Lấy danh sách món ăn GỘP (dùng hàm mới getCombinedOrderItems trong OrderDAO)
        List<OrderItem> items = orderDAO.getCombinedOrderItems(tableId);

        if (items.isEmpty()) {
            // Nếu không có món nào, chỉ báo bàn trống
            showAlert("Thông báo", "Bàn này hiện không có món nào đang phục vụ.", Alert.AlertType.INFORMATION);
            clearCart();
            return;
        }

        // 2. Đưa vào giỏ hàng
        clearCart(); // Xóa giỏ hàng cũ và reset các trường
        cartTable.getItems().addAll(items);

        // 3. [QUAN TRỌNG] Lưu lại ID bàn để lát thanh toán
        this.currentTableId = tableId;

        // 4. Cập nhật lại tính toán tiền
        updateCartSummary();
        calculateChangeDue();

        System.out.println("✅ Đã tải tổng hợp " + items.size() + " món của bàn " + tableId);
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
                    // === THÊM KHỐI NÀY (TRƯỚC KHI CLEARCART) ===
                    if (this.currentTableId > 0) {

                        paymentService.completeTransaction(currentTableId);

                        System.out.println("✅ Đã thanh toán gộp cho bàn: " + currentTableId);
                    }
                    // ===========================================
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



        // === KHỞI TẠO POLLING SERVICE (VNPAY) ===
        // === KHỞI TẠO POLLING SERVICE (VNPAY) ===
        // Luôn khởi tạo PollingService vì nó cần thiết cho chức năng thanh toán popup
        this.pollingService = new PollingService(vnpayService, statusCallback);

        this.paymentService = new PaymentService(orderDAO, tableDAO);

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

        // 1. Khởi tạo bộ hẹn giờ
        tablePoller = Executors.newSingleThreadScheduledExecutor();

        // 2. Tạo một TÁC VỤ (task) để chạy lặp lại
        Runnable pollTask = () -> {
            try {
                // Lấy dữ liệu MỚI NHẤT từ CSDL
                List<Table> freshTables = tableDAO.getAllTables();

                refreshCurrentOrder();

                // Cập nhật giao diện trên luồng JavaFX chính
                Platform.runLater(() -> {
                    // Cập nhật danh sách bàn
                    this.allTablesList = freshTables;

                    // Vẽ lại giao diện Pagination với dữ liệu mới
                    refreshPaginationView();
                });

            } catch (Exception e) {
                System.err.println("Lỗi khi polling trạng thái bàn: " + e.getMessage());
                e.printStackTrace();
            }
        };

        // 3. Bắt đầu hẹn giờ: Chạy pollTask sau 0 giây, và lặp lại
        // mỗi 5 giây (POLLING_INTERVAL_SECONDS)
        tablePoller.scheduleAtFixedRate(pollTask, 0, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);

    }

    private void refreshPaginationView() {
        if (allTablesList == null || allTablesList.isEmpty()) {
            tablePagination.setPageCount(1);
            tablePagination.setPageFactory(pageIndex -> new Label("Không tìm thấy bàn nào."));
            return;
        }

        // 1. Tính toán lại số lượng trang
        int pageCount = (int) Math.ceil((double) allTablesList.size() / tablesPerPage);

        // Lấy trang hiện tại để tránh bị reset về trang 1
        int currentPage = tablePagination.getCurrentPageIndex();

        tablePagination.setPageCount(pageCount);

        // 2. "Dạy" (lại) cho Pagination cách tạo trang
        tablePagination.setPageFactory(this::createTablePage); // Tái sử dụng hàm createTablePage

        // 3. Đặt lại về trang hiện tại (nếu trang đó còn tồn tại)
        if (currentPage < pageCount) {
            tablePagination.setCurrentPageIndex(currentPage);
        }
    }

    private Node createTablePage(int pageIndex) {
        // 1. Tạo một TilePane mới cho mỗi trang
        TilePane pageGrid = new TilePane();
        pageGrid.setHgap(15);
        pageGrid.setVgap(15);
        pageGrid.setPadding(new Insets(15));

        // 2. Tính toán vị trí bàn
        int fromIndex = pageIndex * tablesPerPage;
        int toIndex = Math.min(fromIndex + tablesPerPage, allTablesList.size());

        // 3. Lấy danh sách con
        if (allTablesList == null) return pageGrid; // DÒNG BẢO VỆ MỚI
        List<Table> tablesForPage = allTablesList.subList(fromIndex, toIndex);

        // 4. Tạo thẻ VBox cho từng bàn
        for (Table table : tablesForPage) {
            pageGrid.getChildren().add(createTableCard(table));
        }

        // 5. (ĐÃ SỬA) Trả về TRỰC TIẾP TilePane
        return pageGrid; // Trả về TilePane (không bọc trong ScrollPane)
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
        String orderInfo = "Thanh toan ban " + currentTableId;

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

        // === THÊM KHỐI NÀY (TRƯỚC KHI BÁO THÀNH CÔNG) ===
        if (this.currentTableId > 0) {

            paymentService.completeTransaction(currentTableId);

            System.out.println("✅ Đã thanh toán gộp cho bàn: " + currentTableId);
        }
        // ===============================================

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
            this.currentTableId = -1; // Reset đơn hàng đang xử lý
        }
    }

    // --- CÁC HÀM XỬ LÝ ỨNG DỤNG KHÁC (GIỮ NGUYÊN) ---

//    @FXML
//    public void reloadMeals() {
//        loadMeals("All");
//    }










    @FXML
    protected void onHelloButtonClick() {
        if (welcomeText != null) {
            welcomeText.setText("Welcome to VNPAY Integration!");
        }
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
        // 1. Luôn cập nhật thông tin chi tiết của bàn
//        tableNameLabel.setText(table.getName());
//        tableStatusLabel.setText("Trạng thái: " + table.getStatus());

        // 2. Lấy đơn hàng đang hoạt động (PENDING hoặc SERVED)
        Order order = orderDAO.getActiveOrderByTableId(table.getId());

        if (table.getStatus().equals("Có khách") && order != null) {
            // [LOGIC ĐÚNG] Khách đang ngồi ăn -> Tải toàn bộ món lên giỏ
//            totalAmountLabel.setText(String.format("%,.0fđ", order.getTotalAmount()));
            loadOrderForTable(table.getId());

        } else if (order != null) {
            // [LOGIC ĐÚNG] Bàn đặt trước (chưa ăn) -> Chỉ hiện tiền cọc/đặt, KHÔNG tải món
            totalAmountLabel.setText(String.format("%,.0fđ", order.getTotalAmount()));
            clearCart();

        } else {
            // Bàn trống
            totalAmountLabel.setText("0đ");
            clearCart();
        }
    }
    private void refreshCurrentOrder() {
        // Chỉ chạy nếu đang chọn một bàn cụ thể
        if (this.currentTableId <= 0) return;

        try {
            // 1. Lấy dữ liệu mới nhất từ DB
            List<OrderItem> freshItems = orderDAO.getCombinedOrderItems(this.currentTableId);

            // 2. Cập nhật giao diện (Bắt buộc dùng Platform.runLater)
            Platform.runLater(() -> {
                // Lưu lại các giá trị nhập liệu hiện tại để không bị reset
                String currentCash = txtCashReceived.getText();
                String currentDiscount = txtDiscount.getText();

                // Cập nhật giỏ hàng
                cartTable.getItems().setAll(freshItems);

                // Tính toán lại tổng tiền
                updateCartSummary();
                calculateChangeDue();

                // Cập nhật lại tổng tiền bên sidebar bàn (cho đồng bộ)
                totalAmountLabel.setText(grandTotalLabel.getText());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}