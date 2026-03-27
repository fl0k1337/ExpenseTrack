package com.example.expensetracker;

public class Achievement {
    public String title, description, icon;
    public int currentProgress, maxProgress; // Для прогресс-бара
    public int color; // Цвет карточки (для ранга)

    public Achievement(String title, String description, String icon, int currentProgress, int maxProgress, int color) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.currentProgress = currentProgress;
        this.maxProgress = maxProgress;
        this.color = color;
    }

    public boolean isUnlocked() {
        return currentProgress >= maxProgress;
    }
}