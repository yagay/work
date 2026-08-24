package com.example.workhours;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class SettingsActivity extends Activity {

    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";
    private static final String BREAK_MINUTES_KEY = "break_minutes";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";

    private SharedPreferences prefs;
    private EditText startInput;
    private EditText endInput;
    private EditText breakInput;
    private EditText monthlyRestInput;
    private TextView previewText;
    private Button workStartDateButton;
    private LocalDate workStartDate;
    private final CheckBox[] restDayChecks = new CheckBox[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        loadSettings();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(24);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(48)));
        TextView title = text("工作时间设置", 24, true);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView info = text("设置一次后，App 会按工作时间和休息规则自动计算。", 14, false);
        info.setPadding(0, dp(18), 0, dp(18));
        root.addView(info);

        root.addView(text("工作开始日期（可选）", 17, true));
        TextView startDateInfo = text("开始日期之前不会计入工时。", 13, false);
        startDateInfo.setPadding(0, dp(4), 0, dp(8));
        root.addView(startDateInfo);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(dateRow);
        workStartDateButton = new Button(this);
        workStartDateButton.setOnClickListener(v -> chooseWorkStartDate());
        dateRow.addView(workStartDateButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button clearDate = new Button(this);
        clearDate.setText("清除");
        clearDate.setOnClickListener(v -> { workStartDate = null; updateWorkStartDateButton(); });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(86), dp(50));
        clearParams.leftMargin = dp(8);
        dateRow.addView(clearDate, clearParams);

        TextView timeTitle = text("每天工作时间", 17, true);
        timeTitle.setPadding(0, dp(24), 0, dp(8));
        root.addView(timeTitle);
        root.addView(text("上班时间（HH:mm）", 15, true));
        startInput = input("09:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(startInput);
        TextView endLabel = text("下班时间（HH:mm）", 15, true);
        endLabel.setPadding(0, dp(14), 0, 0);
        root.addView(endLabel);
        endInput = input("17:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(endInput);
        TextView breakLabel = text("休息时间（分钟）", 15, true);
        breakLabel.setPadding(0, dp(14), 0, 0);
        root.addView(breakLabel);
        breakInput = input("30", InputType.TYPE_CLASS_NUMBER);
        root.addView(breakInput);

        previewText = text("", 15, true);
        previewText.setPadding(0, dp(12), 0, dp(8));
        root.addView(previewText);
        Button preview = new Button(this);
        preview.setText("计算每天工时");
        preview.setOnClickListener(v -> updatePreview(true));
        root.addView(preview, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

        TextView weeklyTitle = text("每周休息日", 17, true);
        weeklyTitle.setPadding(0, dp(24), 0, dp(4));
        root.addView(weeklyTitle);
        TextView weeklyInfo = text("勾选一星期中固定休息的日期。默认周六、周日休息。", 13, false);
        weeklyInfo.setPadding(0, 0, 0, dp(6));
        root.addView(weeklyInfo);
        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < 7; i++) {
            restDayChecks[i] = new CheckBox(this);
            restDayChecks[i].setText(names[i] + "休息");
            restDayChecks[i].setTextSize(16);
            root.addView(restDayChecks[i]);
        }

        TextView monthlyTitle = text("每月固定休息日期", 17, true);
        monthlyTitle.setPadding(0, dp(24), 0, dp(4));
        root.addView(monthlyTitle);
        TextView monthlyInfo = text("输入每月固定休息的日期，用逗号分隔，例如：5, 15, 28。留空表示不设置。", 13, false);
        monthlyInfo.setPadding(0, 0, 0, dp(6));
        root.addView(monthlyInfo);
        monthlyRestInput = input("例如：5, 15, 28", InputType.TYPE_CLASS_TEXT);
        root.addView(monthlyRestInput);

        Button save = new Button(this);
        save.setText("保存设置");
        save.setTextSize(16);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        saveParams.topMargin = dp(22);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> save());
        return scroll;
    }

    private EditText input(String hint, int type) {
        EditText view = new EditText(this);
        view.setSingleLine(true);
        view.setInputType(type);
        view.setTextSize(18);
        view.setHint(hint);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        return view;
    }

    private void loadSettings() {
        startInput.setText(prefs.getString(START_TIME_KEY, "09:00"));
        endInput.setText(prefs.getString(END_TIME_KEY, "17:30"));
        breakInput.setText(String.valueOf(prefs.getInt(BREAK_MINUTES_KEY, 30)));
        monthlyRestInput.setText(prefs.getString(MONTHLY_REST_DAYS_KEY, ""));

        String savedStartDate = prefs.getString(WORK_START_DATE_KEY, "");
        if (!savedStartDate.isEmpty()) {
            try { workStartDate = LocalDate.parse(savedStartDate); }
            catch (DateTimeParseException ignored) { workStartDate = null; }
        }
        updateWorkStartDateButton();

        for (int i = 0; i < 7; i++) {
            // Backward compatible: old day_i=true meant working day.
            boolean wasWorkDay = prefs.getBoolean("day_" + i, i < 5);
            restDayChecks[i].setChecked(!wasWorkDay);
        }
        updatePreview(false);
    }

    private void chooseWorkStartDate() {
        LocalDate initial = workStartDate != null ? workStartDate : LocalDate.now();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    workStartDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateWorkStartDateButton();
                }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void updateWorkStartDateButton() {
        workStartDateButton.setText(workStartDate == null ? "未设置（统计全部历史）"
                : workStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    private void save() {
        Float dailyHours = calculate(true);
        if (dailyHours == null) return;
        String monthlyRestDays = normalizeMonthlyRestDays(true);
        if (monthlyRestDays == null) return;

        int breakMinutes = Integer.parseInt(breakInput.getText().toString().trim());
        String start = normalizeTime(startInput.getText().toString());
        String end = normalizeTime(endInput.getText().toString());

        SharedPreferences.Editor editor = prefs.edit()
                .putString(START_TIME_KEY, start)
                .putString(END_TIME_KEY, end)
                .putInt(BREAK_MINUTES_KEY, breakMinutes)
                .putFloat("daily_hours", dailyHours)
                .putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays);

        if (workStartDate == null) editor.remove(WORK_START_DATE_KEY);
        else editor.putString(WORK_START_DATE_KEY, workStartDate.toString());

        for (int i = 0; i < 7; i++) {
            editor.putBoolean("day_" + i, !restDayChecks[i].isChecked());
        }
        editor.apply();

        startInput.setText(start);
        endInput.setText(end);
        monthlyRestInput.setText(monthlyRestDays);
        updatePreview(false);
        Toast.makeText(this, "休息规则和工作设置已保存", Toast.LENGTH_SHORT).show();
    }

    private String normalizeMonthlyRestDays(boolean showError) {
        String raw = monthlyRestInput.getText().toString().trim();
        if (raw.isEmpty()) return "";
        String[] parts = raw.replace('，', ',').split(",");
        Set<Integer> values = new LinkedHashSet<>();
        for (String part : parts) {
            String value = part.trim();
            if (value.isEmpty()) continue;
            try {
                int day = Integer.parseInt(value);
                if (day < 1 || day > 31) {
                    if (showError) monthlyRestInput.setError("日期只能是 1～31");
                    return null;
                }
                values.add(day);
            } catch (NumberFormatException e) {
                if (showError) monthlyRestInput.setError("请输入数字日期，例如 5, 15, 28");
                return null;
            }
        }
        StringBuilder result = new StringBuilder();
        for (int value : values) {
            if (result.length() > 0) result.append(", ");
            result.append(value);
        }
        return result.toString();
    }

    private void updatePreview(boolean showError) {
        Float value = calculate(showError);
        if (value != null) previewText.setText("正常工作日自动计入：" + formatDurationHours(value));
    }

    private Float calculate(boolean showError) {
        LocalTime start = parseTime(startInput.getText().toString());
        if (start == null) {
            if (showError) startInput.setError("请输入 HH:mm，例如 09:00");
            return null;
        }
        LocalTime end = parseTime(endInput.getText().toString());
        if (end == null) {
            if (showError) endInput.setError("请输入 HH:mm，例如 17:30");
            return null;
        }
        int breakMinutes;
        try { breakMinutes = Integer.parseInt(breakInput.getText().toString().trim()); }
        catch (NumberFormatException e) {
            if (showError) breakInput.setError("请输入正确的分钟数");
            return null;
        }
        if (breakMinutes < 0 || breakMinutes >= 24 * 60) {
            if (showError) breakInput.setError("应在 0～1439 分钟之间");
            return null;
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) minutes += 24 * 60;
        long work = minutes - breakMinutes;
        if (work < 0) {
            if (showError) breakInput.setError("休息时间不能超过上下班间隔");
            return null;
        }
        return work / 60f;
    }

    private LocalTime parseTime(String raw) {
        try { return LocalTime.parse(normalizeTime(raw), DateTimeFormatter.ofPattern("HH:mm")); }
        catch (DateTimeParseException | IllegalArgumentException e) { return null; }
    }

    private String normalizeTime(String raw) {
        String value = raw.trim();
        if (value.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = value.split(":");
            try { return String.format(Locale.US, "%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1])); }
            catch (NumberFormatException ignored) { }
        }
        return value;
    }

    private String formatDurationHours(float hours) {
        int totalMinutes = Math.round(hours * 60f);
        int h = totalMinutes / 60, m = totalMinutes % 60;
        if (m == 0) return h + " 小时";
        if (h == 0) return m + " 分钟";
        return h + " 小时 " + m + " 分钟";
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(0xFF202124);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
