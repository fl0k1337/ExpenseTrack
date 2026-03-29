package com.example.expensetracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public double amount;
    public String category;
    public String description;
    public long date;
    public boolean isRecurring;

    public Expense(String title, double amount, String category, String description, long date, boolean isRecurring) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.isRecurring = isRecurring;
    }
}