package com.example.workhours;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

final class AppThemeManager {
    static final String KEY = "app_theme";
    static final String SYSTEM = "system";
    static final String LIGHT = "light";
    static final String DARK = "dark";
    private static final String PREFS = "work_hours_prefs";

    private AppThemeManager() { }

    static String mode(Context context) {
        String mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM);
        return mode == null ? SYSTEM : mode;
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
        return dark;
    }

    static String label(String mode) {
        if (DARK.equals(mode)) return "深色";
        if (LIGHT.equals(mode)) return "浅色";
        return "跟随系统";
    }
}
