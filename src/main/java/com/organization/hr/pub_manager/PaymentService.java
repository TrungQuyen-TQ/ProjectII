package com.organization.hr.pub_manager;

public class PaymentService {

    private final OrderDAO orderDAO;
    private final TableDAO tableDAO;

    // 1. Dependency Injection: Nhận các DAO cần thiết qua constructor
    public PaymentService(OrderDAO orderDAO, TableDAO tableDAO) {
        this.orderDAO = orderDAO;
        this.tableDAO = tableDAO;
    }

    /**
     * Hoàn tất bất kỳ giao dịch nào (Tiền mặt, Thẻ)
     * BƯỚC QUAN TRỌNG: Cập nhật CSDL cho Orders và Tables.
     * @param tableId ID bàn đang thanh toán
     */
    public void completeTransaction(int tableId) {
        if (tableId <= 0) {
            System.err.println("Lỗi: Không thể hoàn tất giao dịch vì không có ID bàn hợp lệ.");
            return;
        }

        try {
            // 2. [LỆNH CỐT LÕI] Cập nhật TẤT CẢ đơn hàng đang hoạt động của bàn đó thành 'PAID'
            orderDAO.payAllActiveOrders(tableId, "PAID");

            // 3. [LỆNH CỐT LÕI] Cập nhật trạng thái bàn về 'Trống'
            tableDAO.updateTableStatus(tableId, "Trống");

            System.out.println("✅ PaymentService: Đã hoàn tất giao dịch và dọn bàn " + tableId);
        } catch (Exception e) {
            System.err.println("❌ PaymentService: Lỗi khi hoàn tất giao dịch cho bàn " + tableId);
            e.printStackTrace();
            // Trong ứng dụng thực tế, bạn nên ném ngoại lệ lên (throw exception)
        }
    }
}