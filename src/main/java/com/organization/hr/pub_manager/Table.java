package com.organization.hr.pub_manager;

public class Table {
    private int id;
    private String name;
    private String status;

    public Table(int id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    // --- Getters ---
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    // --- Setters (nếu cần) ---
    public void setStatus(String status) {
        this.status = status;
    }
}