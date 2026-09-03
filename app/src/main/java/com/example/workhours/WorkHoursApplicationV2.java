package com.example.workhours;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Keeps custom alarm minute values directly editable inside SettingsActivity.
 * The original fixed-choice buttons are replaced in-place with numeric inputs.
 */
public class WorkHoursApplicationV2 extends WorkHoursApplication {
    private static final String PREFS = "work_hours_prefs";
    private static final String INLINE_TAG_PREFIX = "alarm_inline_minutes:";
    private static final int MAX_MINUTES = 10080; // 7 days

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof SettingsActivity) {
                    installAlarmMinuteEditors((SettingsActivity) activity);
                }
            }

            @Override
            public void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
                if (activity instanceof SettingsActivity) {
                    View root = activity.findViewById(android.R.id.content);
                    if (root != null) {
                        root.post(() -> installAlarmMinuteEditors((SettingsActivity) activity));
                    }
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

    private void installAlarmMinuteEditors(SettingsActivity activity) {
        replaceButtonWithInlineInput(
                activity,
                "autoStopButton",
                WorkAlarmOptions.AUTO_STOP_MINUTES_KEY,
                WorkAlarmOptions.DEFAULT_AUTO_STOP_MINUTES,
                true);

        replaceButtonWithInlineInput(
                activity,
                "snoozeMinutesButton",
                WorkAlarmOptions.SNOOZE_MINUTES_KEY,
                WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES,
                false);
    }

    private void replaceButtonWithInlineInput(
            SettingsActivity activity,
            String fieldName,
            String prefKey,
            int defaultValue,
            boolean allowZero) {
        try {
            Field field = SettingsActivity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(activity);
            if (!(value instanceof Button)) return;

            Button oldButton = (Button) value;
            if (!(oldButton.getParent() instanceof ViewGroup)) return;
            ViewGroup parent = (ViewGroup) oldButton.getParent();

            String tag = INLINE_TAG_PREFIX + prefKey;
            for (int i = 0; i < parent.getChildCount(); i++) {
                if (tag.equals(parent.getChildAt(i).getTag())) return;
            }

            int index = parent.indexOfChild(oldButton);
            if (index < 0) return;
            ViewGroup.LayoutParams oldParams = oldButton.getLayoutParams();

            SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int minimum = allowZero ? 0 : 1;
            int saved = prefs.getInt(prefKey, defaultValue);
            if (saved < minimum || saved > MAX_MINUTES) saved = defaultValue;

            LinearLayout holder = new LinearLayout(activity);
            holder.setTag(tag);
            holder.setOrientation(LinearLayout.HORIZONTAL);
            holder.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

            EditText input = new EditText(activity);
            input.setSingleLine(true);
            input.setText(String.valueOf(saved));
            input.setSelectAllOnFocus(true);
            input.setTextSize(14);
            input.setGravity(Gravity.CENTER);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setContentDescription(allowZero
                    ? "自动停止分钟数，0 表示不自动停止"
                    : "稍后提醒间隔分钟数");

            LinearLayout.LayoutParams inputParams =
                    new LinearLayout.LayoutParams(dp(activity, 86), dp(activity, 42));
            holder.addView(input, inputParams);

            TextView unit = new TextView(activity);
            unit.setText(" 分钟");
            unit.setTextSize(14);
            unit.setGravity(Gravity.CENTER_VERTICAL);
            holder.addView(unit, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

            parent.removeViewAt(index);
            parent.addView(holder, index, oldParams);

            final int fallbackDefault = defaultValue;
            final int minValue = minimum;

            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable editable) {
                    String raw = editable == null ? "" : editable.toString().trim();
                    if (raw.isEmpty()) return;
                    try {
                        int minutes = Integer.parseInt(raw);
                        if (minutes < minValue || minutes > MAX_MINUTES) return;
                        prefs.edit().putInt(prefKey, minutes).apply();
                    } catch (NumberFormatException ignored) { }
                }
            });

            input.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) return;
                String raw = input.getText() == null ? "" : input.getText().toString().trim();
                int fallback = prefs.getInt(prefKey, fallbackDefault);
                if (fallback < minValue || fallback > MAX_MINUTES) fallback = fallbackDefault;
                try {
                    int minutes = Integer.parseInt(raw);
                    if (minutes < minValue || minutes > MAX_MINUTES) {
                        input.setText(String.valueOf(fallback));
                    }
                } catch (NumberFormatException e) {
                    input.setText(String.valueOf(fallback));
                }
            });
        } catch (Exception ignored) { }
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
