package com.example.expensetracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "goals")
public class Goal {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public double targetAmount;
    public double savedAmount;

    public Goal(String title, double targetAmount, double savedAmount) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
    }
}