package com.example.workhours;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Small UI compatibility layer for alarm settings that should be edited directly
 * on the Settings page instead of through fixed-choice dialogs.
 */
public class EnhancedWorkHoursApplication extends WorkHoursApplication {
    private static final String PREFS = "work_hours_prefs";
    private static final String TAG_INLINE_ALARM_MINUTES = "inline_alarm_minutes_v1";

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                if (activity instanceof SettingsActivity) installInlineAlarmMinuteInputs(activity);
            }

            @Override public void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
                if (activity instanceof SettingsActivity) {
                    View root = activity.findViewById(android.R.id.content);
                    if (root != null) root.post(() -> installInlineAlarmMinuteInputs(activity));
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }

    private static void installInlineAlarmMinuteInputs(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        replaceSettingValue(activity, root, "自动停止",
                WorkAlarmOptions.AUTO_STOP_MINUTES_KEY,
                WorkAlarmOptions.DEFAULT_AUTO_STOP_MINUTES,
                0);
        replaceSettingValue(activity, root, "稍后提醒间隔",
                WorkAlarmOptions.SNOOZE_MINUTES_KEY,
                WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES,
                1);
    }

    private static boolean replaceSettingValue(
            Activity activity,
            View view,
            String rowLabel,
            String prefKey,
            int defaultValue,
            int minimum) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (isTargetSettingRow(group, rowLabel)) {
                if (TAG_INLINE_ALARM_MINUTES.equals(group.getTag())) return true;
                installInput(activity, group, prefKey, defaultValue, minimum);
                group.setTag(TAG_INLINE_ALARM_MINUTES);
                return true;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                if (replaceSettingValue(activity, group.getChildAt(i), rowLabel,
                        prefKey, defaultValue, minimum)) return true;
            }
        }
        return false;
    }

    private static boolean isTargetSettingRow(ViewGroup group, String label) {
        if (!(group instanceof LinearLayout) || group.getChildCount() < 2) return false;
        View first = group.getChildAt(0);
        if (!(first instanceof TextView)) return false;
        CharSequence text = ((TextView) first).getText();
        return text != null && label.contentEquals(text.toString().trim());
    }

    private static void installInput(
            Activity activity,
            ViewGroup row,
            String prefKey,
            int defaultValue,
            int minimum) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int saved = prefs.getInt(prefKey, defaultValue);
        if (saved < minimum) saved = minimum;

        View oldValue = row.getChildAt(1);
        ViewGroup.LayoutParams oldParams = oldValue.getLayoutParams();
        row.removeViewAt(1);

        LinearLayout holder = new LinearLayout(activity);
        holder.setOrientation(LinearLayout.HORIZONTAL);
        holder.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setText(String.valueOf(saved));
        input.setSelectAllOnFocus(true);
        input.setTextSize(14);
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(minimum == 0 ? "0" : String.valueOf(defaultValue));
        input.setContentDescription(prefKey);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(dp(activity, 92), dp(activity, 42));
        holder.addView(input, inputParams);

        TextView unit = new TextView(activity);
        unit.setText(" 分钟");
        unit.setTextSize(14);
        unit.setGravity(Gravity.CENTER_VERTICAL);
        holder.addView(unit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        row.addView(holder, 1, oldParams);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                String raw = s.toString().trim();
                if (raw.isEmpty()) return;
                try {
                    int value = Integer.parseInt(raw);
                    if (value < minimum) return;
                    // Keep the range practical while still allowing arbitrary user values.
                    if (value > 1440) return;
                    prefs.edit().putInt(prefKey, value).apply();
                } catch (NumberFormatException ignored) { }
            }
        });

        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) return;
            String raw = input.getText().toString().trim();
            int fallback = prefs.getInt(prefKey, defaultValue);
            try {
                int value = Integer.parseInt(raw);
                if (value < minimum || value > 1440) input.setText(String.valueOf(fallback));
            } catch (NumberFormatException e) {
                input.setText(String.valueOf(fallback));
            }
        });
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
