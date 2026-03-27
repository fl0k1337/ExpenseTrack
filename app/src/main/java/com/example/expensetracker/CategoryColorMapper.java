package com.example.expensetracker;

import android.graphics.Color;
import java.util.HashMap;
import java.util.Map;

public class CategoryColorMapper {
    private static final Map<String, CategoryInfo> categoryMap = new HashMap<>();

    public static class CategoryInfo {
        public int color;
        public int iconRes;
        public CategoryInfo(int color, int iconRes) {
            this.color = color;
            this.iconRes = iconRes;
        }
    }

    static {
        categoryMap.put("Еда", new CategoryInfo(Color.parseColor("#FF9800"), android.R.drawable.ic_menu_report_image));
        categoryMap.put("Транспорт", new CategoryInfo(Color.parseColor("#2196F3"), android.R.drawable.ic_menu_directions));
        categoryMap.put("Развлечения", new CategoryInfo(Color.parseColor("#9C27B0"), android.R.drawable.ic_menu_slideshow));
        categoryMap.put("Жилье", new CategoryInfo(Color.parseColor("#4CAF50"), android.R.drawable.ic_menu_myplaces));
        categoryMap.put("Здоровье", new CategoryInfo(Color.parseColor("#F44336"), android.R.drawable.ic_menu_add));
        categoryMap.put("Покупки", new CategoryInfo(Color.parseColor("#FFEB3B"), android.R.drawable.ic_menu_manage));
        categoryMap.put("Прочее", new CategoryInfo(Color.parseColor("#607D8B"), android.R.drawable.ic_menu_help));
    }

    public static int getColor(String category) {
        CategoryInfo info = categoryMap.get(category);
        return (info != null) ? info.color : Color.parseColor("#E91E63");
    }

    public static int getIcon(String category) {
        CategoryInfo info = categoryMap.get(category);
        return (info != null) ? info.iconRes : android.R.drawable.ic_menu_info_details;
    }
}