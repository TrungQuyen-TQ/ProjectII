// Tệp: Order.java
package com.organization.hr.pub_manager;

import java.sql.Timestamp;

// Model cho bảng 'orders'
public class Order {
    private int id;
    private int tableId;
    private int userId;
    private String status;
    private double totalAmount;
    private Timestamp createdAt;

    // Constructors
    public Order() {}

    public Order(int id, int tableId, String status, double totalAmount) {
        this.id = id;
        this.tableId = tableId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    // Getters
    public int getId() { return id; }
    public int getTableId() { return tableId; }
    public String getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }

    // ... (Thêm các setters nếu cần)
}