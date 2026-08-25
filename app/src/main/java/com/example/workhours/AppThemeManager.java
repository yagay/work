package com.example.workhours;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowInsetsController;

final class AppThemeManager {
    static final String KEY = "app_theme";
    static final String SYSTEM = "system";
    static final String LIGHT = "light";
    static final String DARK = "dark";
    private static final String PREFS = "work_hours_prefs";

    private AppThemeManager() { }

    static String mode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM);
    }

    static void setMode(Context context, String mode) {
        String safe = DARK.equals(mode) ? DARK : LIGHT.equals(mode) ? LIGHT : SYSTEM;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, safe).apply();
    }

    static boolean isDark(Context context) {
        String mode = mode(context);
        if (DARK.equals(mode)) return true;
        if (LIGHT.equals(mode)) return false;
        int night = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    static boolean apply(Activity activity) {
        boolean dark = isDark(activity);
        UiStyle.applyDark(dark);
        Window window = activity.getWindow();
        window.setStatusBarColor(UiStyle.PAGE_BG);
        window.setNavigationBarColor(UiStyle.PAGE_BG);
        WindowInsetsController controller = window.getInsetsController();
        if (controller != null) {
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(dark ? 0 : mask, mask);
        }
        return dark;
    }

    static String label(String mode) {
        if (DARK.equals(mode)) return "深色";
        if (LIGHT.equals(mode)) return "浅色";
        return "跟随系统";
    }
}
