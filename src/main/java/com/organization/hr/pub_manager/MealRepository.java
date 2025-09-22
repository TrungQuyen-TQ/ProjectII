package com.organization.hr.pub_manager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealRepository {
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=HRManagement;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "Sa123456!";

    public static List<Meal> getAllMeals() {
        List<Meal> meals = new ArrayList<>();
        String sql = "SELECT id, name, price FROM meals";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Meal meal = new Meal(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price")
                );
                meals.add(meal);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return meals;
    }

    public static void main(String[] args) {
        List<Meal> list = getAllMeals();
        list.forEach(System.out::println);
    }
}
