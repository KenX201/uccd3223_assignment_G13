package com.example.assignment2;

import androidx.annotation.NonNull;
import androidx.room.PrimaryKey;

import java.io.Serializable;

public class Expense implements Serializable {

    @PrimaryKey
    private String expenseID;

    private String username;
    private String note;
    private String category;
    private double amount;
    private String date; // Format: yyyy-MM-dd
    private String time; // Format: yyyy-MM
    private String uid;

    // Default constructor
    public Expense() {
    }

    // Parameterized constructor
    public Expense(String expenseID, String username, String note, String category, double amount, String date, String time, String uid) {
        this.expenseID = expenseID;
        this.username = username;
        this.note = note;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.uid = uid;
    }

    // Getters and Setters
    public String getExpenseID() {
        return expenseID;
    }

    public void setExpenseID(String expenseID) {
        this.expenseID = expenseID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
