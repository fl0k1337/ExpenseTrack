// Замени весь файл PreferenceManager.java на этот:
package com.example.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;

public class PreferenceManager {
    private static final String PREF_NAME = "finance_prefs";
    private static final String KEY_BUDGET = "monthly_budget";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_DARK_MODE = "dark_mode";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setBudget(Context context, float budget) {
        getPrefs(context).edit().putFloat(KEY_BUDGET, budget).apply();
    }

    public static float getBudget(Context context) {
        return getPrefs(context).getFloat(KEY_BUDGET, 50000f);
    }

    public static void setUserName(Context context, String name) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply();
    }

    public static String getUserName(Context context) {
        return getPrefs(context).getString(KEY_USER_NAME, "Пользователь");
    }

    public static void setCurrency(Context context, String currency) {
        getPrefs(context).edit().putString(KEY_CURRENCY, currency).apply();
    }

    public static String getCurrency(Context context) {
        return getPrefs(context).getString(KEY_CURRENCY, "₽");
    }

    public static void setDarkMode(Context context, boolean isDark) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, isDark).apply();
    }

    public static boolean isDarkMode(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false);
    }
}