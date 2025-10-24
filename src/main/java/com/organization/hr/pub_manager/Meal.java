package com.organization.hr.pub_manager;

public class Meal {
    private int id;
    private String name;
    private double price;
    private String imagePath;
    private int categoryId; // Khóa ngoại mới

    public Meal() {
    }

    // Constructor hiện tại (4 tham số)
    public Meal(int id, String name, double price, String imagePath) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
    }

    // Constructor ĐƯỢC THÊM (5 tham số)
    public Meal(int id, String name, double price, String imagePath, int categoryId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
        this.categoryId = categoryId;
    }

    // ... (các getter/setter khác giữ nguyên) ...
    public int getId() {return id;}
    public String getName() {return name;}
    public double getPrice() {return price;}
    public String getImagePath() { return imagePath; }
    public int getCategoryId() { return categoryId; }

    public void setId(int id) {this.id = id;}
    public void setName(String name) {this.name = name;}
    public void setPrice(double price) {this.price = price;}
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
}