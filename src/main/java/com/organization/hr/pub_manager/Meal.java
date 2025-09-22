package com.organization.hr.pub_manager;

public class Meal {
    private int id;
    private String name;
    private double price;

    // Constructor
    public Meal(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getter
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    @Override
    public String toString() {
        return id + " | " + name + " | " + price;
    }
}
