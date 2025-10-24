package com.organization.hr.pub_manager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

// OrderItem đại diện cho một dòng trong giỏ hàng (cartTable)
public class OrderItem {
    private final SimpleStringProperty name;
    private final SimpleDoubleProperty price;
    private final SimpleIntegerProperty quantity;
    private final SimpleDoubleProperty total;
    private final SimpleStringProperty imagePath; // THÊM MỚI: Đường dẫn ảnh để hiển thị trong TableView
    private final SimpleIntegerProperty mealId;   // THÊM MỚI: ID món ăn để so sánh chính xác


    public OrderItem(int mealId, String name, double price, int quantity, String imagePath) {
        this.mealId = new SimpleIntegerProperty(mealId);
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.total = new SimpleDoubleProperty(price * quantity);
        this.imagePath = new SimpleStringProperty(imagePath);
    }

    // --- Getters cho TableView ---
    public int getMealId() { return mealId.get(); }
    public SimpleIntegerProperty mealIdProperty() { return mealId; }

    public String getName() { return name.get(); }
    public SimpleStringProperty nameProperty() { return name; }

    public double getPrice() { return price.get(); }
    public SimpleDoubleProperty priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public SimpleIntegerProperty quantityProperty() { return quantity; }

    public double getTotal() { return total.get(); }
    public SimpleDoubleProperty totalProperty() { return total; }

    public String getImagePath() { return imagePath.get(); } // GETTER MỚI
    public SimpleStringProperty imagePathProperty() { return imagePath; }

    // --- Setters để cập nhật giỏ hàng ---
    public void setQuantity(int newQuantity) {
        if (newQuantity >= 0) { // Đảm bảo số lượng không âm
            quantity.set(newQuantity);
            // Tự động cập nhật Total khi Quantity thay đổi
            updateTotal();
        }
    }

    private void updateTotal() {
        total.set(getPrice() * getQuantity());
    }
}
