package com.example.workhours;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.Field;

/**
 * Small application-level extension for alarm settings that should stay independent
 * from the already large SettingsActivity implementation.
 */
public class WorkHoursApplicationV2 extends WorkHoursApplication {
    private static final String PREFS = "work_hours_prefs";

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof SettingsActivity) installAlarmTimeEditors((SettingsActivity) activity);
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }

    private void installAlarmTimeEditors(SettingsActivity activity) {
        bindEditor(activity, "autoStopButton", WorkAlarmOptions.AUTO_STOP_MINUTES_KEY,
                WorkAlarmOptions.DEFAULT_AUTO_STOP_MINUTES, "自动停止", true);
        bindEditor(activity, "snoozeMinutesButton", WorkAlarmOptions.SNOOZE_MINUTES_KEY,
                WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES, "稍后提醒间隔", false);
    }

    private void bindEditor(SettingsActivity activity, String fieldName, String prefKey,
                            int defaultValue, String title, boolean allowZero) {
        try {
            Field field = SettingsActivity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(activity);
            if (!(value instanceof Button)) return;
            Button button = (Button) value;
            button.setOnClickListener(v -> showMinuteInput(activity, button, prefKey,
                    defaultValue, title, allowZero));
        } catch (Exception ignored) { }
    }

    private void showMinuteInput(Activity activity, Button button, String prefKey,
                                 int defaultValue, String title, boolean allowZero) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int current = prefs.getInt(prefKey, defaultValue);

        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.length());
        int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        input.setPadding(pad, 0, pad, 0);

        String message = allowZero
                ? "输入分钟数。0 表示不自动停止。"
                : "输入稍后再次提醒的间隔分钟数，必须大于 0。";

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String raw = input.getText() == null ? "" : input.getText().toString().trim();
                    int minutes;
                    try {
                        minutes = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        Toast.makeText(activity, "请输入有效的分钟数", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if ((!allowZero && minutes <= 0) || (allowZero && minutes < 0)) {
                        Toast.makeText(activity,
                                allowZero ? "分钟数不能小于 0" : "提醒间隔必须大于 0 分钟",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (minutes > 10080) {
                        Toast.makeText(activity, "最多可设置 10080 分钟（7 天）", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    prefs.edit().putInt(prefKey, minutes).apply();
                    button.setText(formatMinutes(minutes, allowZero));
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private String formatMinutes(int minutes, boolean allowZero) {
        if (allowZero && minutes == 0) return "不自动停止";
        return minutes + " 分钟";
    }
}
