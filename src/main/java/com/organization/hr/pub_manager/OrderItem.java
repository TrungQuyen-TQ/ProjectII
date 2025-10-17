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

    public OrderItem(String name, double price, int quantity) {
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.total = new SimpleDoubleProperty(price * quantity);
    }

    // --- Getters cho TableView ---
    public String getName() { return name.get(); }
    public SimpleStringProperty nameProperty() { return name; }

    public double getPrice() { return price.get(); }
    public SimpleDoubleProperty priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public SimpleIntegerProperty quantityProperty() { return quantity; }

    public double getTotal() { return total.get(); }
    public SimpleDoubleProperty totalProperty() { return total; }

    // --- Setters để cập nhật giỏ hàng ---
    public void setQuantity(int newQuantity) {
        quantity.set(newQuantity);
        // Tự động cập nhật Total khi Quantity thay đổi
        updateTotal();
    }

    private void updateTotal() {
        total.set(getPrice() * getQuantity());
    }
}
