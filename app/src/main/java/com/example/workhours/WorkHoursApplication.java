package com.example.workhours;

import android.app.ActionMode;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DatePickerDialog;
import android.app.SearchEvent;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkHoursApplication extends Application {

    private static final int TAG_MONTH_SELECTOR = 0x7f0a0001;
    private static final int TAG_WEEK_SELECTOR = 0x7f0a0002;
    private static final int TAG_GLOBAL_TEXT_FORMATTER = 0x7f0a0003;
    private static final int TAG_GLOBAL_TREE_FORMATTER = 0x7f0a0004;

    private static final int ALERT_COLOR = 0xFFC5221F;
    private static final int OVERTIME_COLOR = 0xFF7B1FA2;

    /*
     * All displays of these four statistics go through this single rule.
     * To change their layout/style in the future, change this block only.
     */
    private static final Pattern WORK_COUNT_PATTERN =
            Pattern.compile("工作\\s*[:：]?\\s*(\\d+)天");
    private static final Pattern LEAVE_COUNT_PATTERN =
            Pattern.compile("请假\\s*[:：]?\\s*(\\d+)天");
    private static final Pattern HOLIDAY_COUNT_PATTERN =
            Pattern.compile("公共假日\\s*[:：]?\\s*(\\d+)天");
    private static final Pattern REST_COUNT_PATTERN =
            Pattern.compile("休息\\s*[:：]?\\s*(\\d+)天");

    private static final Pattern LEAVE_STYLE_PATTERN =
            Pattern.compile("请假\\s*[:：]?\\s*\\d+天");
    private static final Pattern DEDUCT_PATTERN =
            Pattern.compile("(?:已扣|扣工资)(?:\\s*[:：]?\\s*-?£?[0-9.,]+|\\s*[:：]?\\s*\\d+天)?");
    private static final Pattern OVERTIME_PATTERN =
            Pattern.compile("加班(?:\\s*[:：]?\\s*[^\\n·）)]*)?");

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
                applySystemBarInsets(activity);
                installPageSwipe(activity);
                installGlobalTextFormatting(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                installPeriodSelectors(activity);
                installGlobalTextFormatting(activity);
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }

    private void applySystemBarInsets(Activity activity) {
        activity.getWindow().setDecorFitsSystemWindows(false);
        View content = activity.findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        content.requestApplyInsets();
    }

    /**
     * Installs one app-wide formatter instead of naming individual TextViews.
     * Newly created views are picked up automatically on the next layout pass.
     */
    private void installGlobalTextFormatting(Activity activity) {
        if (!(activity instanceof MainActivity) && !(activity instanceof WageActivity)) return;
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        scanAndInstallTextFormatters(root);
        if (root.getTag(TAG_GLOBAL_TREE_FORMATTER) == null) {
            root.setTag(TAG_GLOBAL_TREE_FORMATTER, Boolean.TRUE);
            root.getViewTreeObserver().addOnGlobalLayoutListener(
                    () -> scanAndInstallTextFormatters(root));
        }
    }

    private void scanAndInstallTextFormatters(View view) {
        if (view instanceof TextView && !(view instanceof EditText)) {
            installTextFormatter((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scanAndInstallTextFormatters(group.getChildAt(i));
            }
        }
    }

    private void installTextFormatter(TextView view) {
        if (view.getTag(TAG_GLOBAL_TEXT_FORMATTER) != null) return;
        String current = view.getText() == null ? "" : view.getText().toString();
        if (!needsGlobalFormatting(current)) return;

        view.setTag(TAG_GLOBAL_TEXT_FORMATTER, Boolean.TRUE);
        final boolean[] formatting = {false};
        view.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (formatting[0]) return;
                formatting[0] = true;
                applyGlobalTextFormatting(view, s.toString());
                formatting[0] = false;
            }
        });
        formatting[0] = true;
        applyGlobalTextFormatting(view, current);
        formatting[0] = false;
    }

    private boolean needsGlobalFormatting(String text) {
        if (text == null || text.isEmpty()) return false;
        return WORK_COUNT_PATTERN.matcher(text).find()
                || LEAVE_COUNT_PATTERN.matcher(text).find()
                || HOLIDAY_COUNT_PATTERN.matcher(text).find()
                || REST_COUNT_PATTERN.matcher(text).find()
                || DEDUCT_PATTERN.matcher(text).find()
                || OVERTIME_PATTERN.matcher(text).find();
    }

    private void applyGlobalTextFormatting(TextView view, String source) {
        String normalized = normalizeFourStatusStats(source == null ? "" : source);
        view.setText(colorizeSummary(normalized));
    }

    /**
     * When all four counters occur in the same TextView they always become:
     * 工作 x天 ｜ 请假 x天 ｜ 公共假日 x天 ｜ 休息 x天
     */
    private String normalizeFourStatusStats(String text) {
        Matcher work = WORK_COUNT_PATTERN.matcher(text);
        Matcher leave = LEAVE_COUNT_PATTERN.matcher(text);
        Matcher holiday = HOLIDAY_COUNT_PATTERN.matcher(text);
        Matcher rest = REST_COUNT_PATTERN.matcher(text);
        if (!work.find() || !leave.find() || !holiday.find() || !rest.find()) return text;

        int start = Math.min(Math.min(work.start(), leave.start()),
                Math.min(holiday.start(), rest.start()));
        int end = Math.max(Math.max(work.end(), leave.end()),
                Math.max(holiday.end(), rest.end()));

        String replacement = "工作 " + work.group(1) + "天 ｜ 请假 " + leave.group(1) + "天"
                + " ｜ 公共假日 " + holiday.group(1) + "天 ｜ 休息 " + rest.group(1) + "天";
        return text.substring(0, start) + replacement + text.substring(end);
    }

    private SpannableString colorizeSummary(String text) {
        SpannableString span = new SpannableString(text == null ? "" : text);
        applyPattern(span, LEAVE_STYLE_PATTERN, ALERT_COLOR);
        applyPattern(span, DEDUCT_PATTERN, ALERT_COLOR);
        applyPattern(span, OVERTIME_PATTERN, OVERTIME_COLOR);
        return span;
    }

    private void applyPattern(SpannableString span, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(span.toString());
        while (matcher.find()) {
            span.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void installPeriodSelectors(Activity activity) {
        if (!(activity instanceof MainActivity) && !(activity instanceof WageActivity)) return;
        try {
            TextView monthTitle = (TextView) getFieldValue(activity, "monthTitle");
            TextView weekTitle = (TextView) getFieldValue(activity, "weekTitle");

            if (monthTitle != null && monthTitle.getTag(TAG_MONTH_SELECTOR) == null) {
                monthTitle.setTag(TAG_MONTH_SELECTOR, Boolean.TRUE);
                monthTitle.setClickable(true);
                monthTitle.setFocusable(true);
                monthTitle.setOnClickListener(v -> showMonthPicker(activity));
            }

            if (weekTitle != null && weekTitle.getTag(TAG_WEEK_SELECTOR) == null) {
                weekTitle.setTag(TAG_WEEK_SELECTOR, Boolean.TRUE);
                weekTitle.setClickable(true);
                weekTitle.setFocusable(true);
                weekTitle.setOnClickListener(v -> showWeekPicker(activity));
                weekTitle.addOnLayoutChangeListener((v, left, top, right, bottom,
                        oldLeft, oldTop, oldRight, oldBottom) -> updateWeekTitle(activity));
            }
            updateWeekTitle(activity);
        } catch (Exception ignored) { }
    }

    private void showMonthPicker(Activity activity) {
        try {
            YearMonth current = (YearMonth) getFieldValue(activity, "displayedMonth");
            if (current == null) current = YearMonth.now();
            YearMonth now = YearMonth.now();
            LocalDate workStart = getWorkStartDate(activity);
            YearMonth first = workStart == null
                    ? YearMonth.of(now.getYear() - 20, 1)
                    : YearMonth.from(workStart);

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            int pad = dp(activity, 12);
            row.setPadding(pad, pad, pad, 0);

            NumberPicker yearPicker = new NumberPicker(activity);
            yearPicker.setMinValue(first.getYear());
            yearPicker.setMaxValue(now.getYear());
            yearPicker.setValue(current.getYear());
            yearPicker.setWrapSelectorWheel(false);

            NumberPicker monthPicker = new NumberPicker(activity);
            monthPicker.setMinValue(1);
            monthPicker.setMaxValue(12);
            monthPicker.setDisplayedValues(new String[]{
                    "1月", "2月", "3月", "4月", "5月", "6月",
                    "7月", "8月", "9月", "10月", "11月", "12月"
            });
            monthPicker.setValue(current.getMonthValue());
            monthPicker.setWrapSelectorWheel(true);

            row.addView(yearPicker,
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(monthPicker,
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            new AlertDialog.Builder(activity)
                    .setTitle("选择月份")
                    .setView(row)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", (dialog, which) -> {
                        YearMonth selected = YearMonth.of(
                                yearPicker.getValue(), monthPicker.getValue());
                        if (selected.isAfter(now)) selected = now;
                        if (selected.isBefore(first)) selected = first;
                        setFieldValue(activity, "displayedMonth", selected);
                        invokeNoArg(activity, "refreshMonth");
                    })
                    .show();
        } catch (Exception ignored) { }
    }

    private void showWeekPicker(Activity activity) {
        try {
            LocalDate current = (LocalDate) getFieldValue(activity, "displayedWeekStart");
            if (current == null) current = LocalDate.now();
            LocalDate today = LocalDate.now();
            LocalDate workStart = getWorkStartDate(activity);

            DatePickerDialog dialog = new DatePickerDialog(activity, (view, year, month, day) -> {
                LocalDate selected = LocalDate.of(year, month + 1, day);
                if (selected.isAfter(today)) selected = today;
                if (workStart != null && selected.isBefore(workStart)) selected = workStart;
                LocalDate monday = selected.with(
                        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                setFieldValue(activity, "displayedWeekStart", monday);
                invokeNoArg(activity, "refreshWeek");
                updateWeekTitle(activity);
            }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());

            dialog.setTitle("选择任意一天，查看所在星期");
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            if (workStart != null) {
                dialog.getDatePicker().setMinDate(workStart
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli());
            }
            dialog.show();
        } catch (Exception ignored) { }
    }

    private void updateWeekTitle(Activity activity) {
        try {
            TextView weekTitle = (TextView) getFieldValue(activity, "weekTitle");
            LocalDate weekStart = (LocalDate) getFieldValue(activity, "displayedWeekStart");
            if (weekTitle == null || weekStart == null) return;
            LocalDate weekEnd = weekStart.plusDays(6);
            WeekFields iso = WeekFields.ISO;
            int week = weekStart.get(iso.weekOfWeekBasedYear());
            int weekYear = weekStart.get(iso.weekBasedYear());
            DateTimeFormatter f = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA);
            String value = weekYear + "年第" + week + "周（"
                    + weekStart.format(f) + "–" + weekEnd.format(f) + "）";
            if (!value.contentEquals(weekTitle.getText())) weekTitle.setText(value);
        } catch (Exception ignored) { }
    }

    private LocalDate getWorkStartDate(Activity activity) {
        try {
            Method method = activity.getClass().getDeclaredMethod("getWorkStartDate");
            method.setAccessible(true);
            return (LocalDate) method.invoke(activity);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object getFieldValue(Activity activity, String name) throws Exception {
        Field field = activity.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(activity);
    }

    private void setFieldValue(Activity activity, String name, Object value) {
        try {
            Field field = activity.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(activity, value);
        } catch (Exception ignored) { }
    }

    private void invokeNoArg(Activity activity, String name) {
        try {
            Method method = activity.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Exception ignored) { }
    }

    private int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private void installPageSwipe(Activity activity) {
        if (!(activity instanceof MainActivity) && !(activity instanceof WageActivity)) return;
        Window window = activity.getWindow();
        Window.Callback original = window.getCallback();
        if (original instanceof SwipeWindowCallback) return;
        window.setCallback(new SwipeWindowCallback(activity, original));
    }

    private static class SwipeWindowCallback implements Window.Callback {
        private final Activity activity;
        private final Window.Callback delegate;
        private final GestureDetector detector;
        private boolean switching;

        SwipeWindowCallback(Activity activity, Window.Callback delegate) {
            this.activity = activity;
            this.delegate = delegate;
            detector = new GestureDetector(activity,
                    new GestureDetector.SimpleOnGestureListener() {
                private static final int MIN_DISTANCE = 120;
                private static final int MIN_VELOCITY = 180;

                @Override public boolean onDown(MotionEvent e) { return true; }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2,
                        float velocityX, float velocityY) {
                    if (switching || e1 == null || e2 == null) return false;
                    float dx = e2.getX() - e1.getX();
                    float dy = e2.getY() - e1.getY();
                    if (Math.abs(dx) < MIN_DISTANCE
                            || Math.abs(dx) <= Math.abs(dy) * 1.25f
                            || Math.abs(velocityX) < MIN_VELOCITY) return false;

                    if (dx < 0 && activity instanceof MainActivity) {
                        switching = true;
                        activity.startActivity(new Intent(activity, WageActivity.class));
                        activity.overridePendingTransition(
                                android.R.anim.slide_in_left, android.R.anim.fade_out);
                        return true;
                    }
                    if (dx > 0 && activity instanceof WageActivity) {
                        switching = true;
                        activity.finish();
                        activity.overridePendingTransition(
                                android.R.anim.fade_in, android.R.anim.slide_out_right);
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override public boolean dispatchTouchEvent(MotionEvent event) { if (event.getActionMasked() == MotionEvent.ACTION_DOWN) switching = false; detector.onTouchEvent(event); return delegate.dispatchTouchEvent(event); }
        @Override public boolean dispatchKeyEvent(KeyEvent event) { return delegate.dispatchKeyEvent(event); }
        @Override public boolean dispatchKeyShortcutEvent(KeyEvent event) { return delegate.dispatchKeyShortcutEvent(event); }
        @Override public boolean dispatchTrackballEvent(MotionEvent event) { return delegate.dispatchTrackballEvent(event); }
        @Override public boolean dispatchGenericMotionEvent(MotionEvent event) { return delegate.dispatchGenericMotionEvent(event); }
        @Override public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) { return delegate.dispatchPopulateAccessibilityEvent(event); }
        @Override public View onCreatePanelView(int featureId) { return delegate.onCreatePanelView(featureId); }
        @Override public boolean onCreatePanelMenu(int featureId, Menu menu) { return delegate.onCreatePanelMenu(featureId, menu); }
        @Override public boolean onPreparePanel(int featureId, View view, Menu menu) { return delegate.onPreparePanel(featureId, view, menu); }
        @Override public boolean onMenuOpened(int featureId, Menu menu) { return delegate.onMenuOpened(featureId, menu); }
        @Override public boolean onMenuItemSelected(int featureId, MenuItem item) { return delegate.onMenuItemSelected(featureId, item); }
        @Override public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) { delegate.onWindowAttributesChanged(attrs); }
        @Override public void onContentChanged() { delegate.onContentChanged(); }
        @Override public void onWindowFocusChanged(boolean hasFocus) { delegate.onWindowFocusChanged(hasFocus); }
        @Override public void onAttachedToWindow() { delegate.onAttachedToWindow(); }
        @Override public void onDetachedFromWindow() { delegate.onDetachedFromWindow(); }
        @Override public void onPanelClosed(int featureId, Menu menu) { delegate.onPanelClosed(featureId, menu); }
        @Override public boolean onSearchRequested() { return delegate.onSearchRequested(); }
        @Override public boolean onSearchRequested(SearchEvent searchEvent) { return delegate.onSearchRequested(searchEvent); }
        @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) { return delegate.onWindowStartingActionMode(callback); }
        @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) { return delegate.onWindowStartingActionMode(callback, type); }
        @Override public void onActionModeStarted(ActionMode mode) { delegate.onActionModeStarted(mode); }
        @Override public void onActionModeFinished(ActionMode mode) { delegate.onActionModeFinished(mode); }
        @Override public void onProvideKeyboardShortcuts(List<android.view.KeyboardShortcutGroup> data, Menu menu, int deviceId) { delegate.onProvideKeyboardShortcuts(data, menu, deviceId); }
        @Override public void onPointerCaptureChanged(boolean hasCapture) { delegate.onPointerCaptureChanged(hasCapture); }
    }
}
