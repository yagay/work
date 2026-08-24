package com.example.workhours;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "work_hours_prefs";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String LEAVE_NOTE_PREFIX = "leave_note_";
    private static final String REST_PREFIX = "rest_";

    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";
    private static final String BREAK_MINUTES_KEY = "break_minutes";

    private final CheckBox[] dayChecks = new CheckBox[7];
    private EditText startTimeInput;
    private EditText endTimeInput;
    private EditText breakMinutesInput;
    private TextView calculatedDailyHoursText;
    private TextView monthText;
    private TextView workDaysText;
    private TextView totalHoursText;
    private TextView todayText;
    private LinearLayout holidaysContainer;
    private LinearLayout leaveContainer;
    private LinearLayout recordsContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        loadSettings();
        updateSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null && recordsContainer != null) updateSummary();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        scroll.addView(root);

        root.addView(text("上班总时间", 28, true));

        todayText = text("", 14, false);
        todayText.setPadding(0, dp(6), 0, dp(24));
        root.addView(todayText);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundColor(0xFFF4F5F7);
        root.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        monthText = text("", 18, true);
        card.addView(monthText);

        workDaysText = text("", 17, false);
        workDaysText.setPadding(0, dp(14), 0, 0);
        card.addView(workDaysText);

        totalHoursText = text("", 34, true);
        totalHoursText.setPadding(0, dp(10), 0, dp(4));
        card.addView(totalHoursText);

        card.addView(text("正常工作日按设置的上下班时间自动计算；公共假期、请假和休息不计工时", 13, false));

        TextView holidayTitle = text("本月公共假期", 15, true);
        holidayTitle.setPadding(0, dp(18), 0, dp(4));
        card.addView(holidayTitle);
        holidaysContainer = new LinearLayout(this);
        holidaysContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(holidaysContainer);

        TextView leaveTitle = text("本月请假", 15, true);
        leaveTitle.setPadding(0, dp(18), 0, dp(4));
        card.addView(leaveTitle);
        leaveContainer = new LinearLayout(this);
        leaveContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(leaveContainer);

        TextView recordsTitle = text("本月每日记录", 20, true);
        recordsTitle.setPadding(0, dp(28), 0, dp(8));
        root.addView(recordsTitle);

        TextView recordsHint = text("普通工作日无需每天设置；点击日期只用于请假、休息或修改当天工时", 13, false);
        recordsHint.setPadding(0, 0, 0, dp(8));
        root.addView(recordsHint);

        recordsContainer = new LinearLayout(this);
        recordsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(recordsContainer);

        TextView settings = text("工作时间设置", 20, true);
        settings.setPadding(0, dp(28), 0, dp(10));
        root.addView(settings);

        root.addView(text("上班时间（HH:mm）", 15, false));
        startTimeInput = timeInput("09:00");
        root.addView(startTimeInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        TextView endLabel = text("下班时间（HH:mm）", 15, false);
        endLabel.setPadding(0, dp(12), 0, 0);
        root.addView(endLabel);
        endTimeInput = timeInput("17:30");
        root.addView(endTimeInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        TextView breakLabel = text("休息时间（分钟）", 15, false);
        breakLabel.setPadding(0, dp(12), 0, 0);
        root.addView(breakLabel);
        breakMinutesInput = new EditText(this);
        breakMinutesInput.setSingleLine(true);
        breakMinutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        breakMinutesInput.setTextSize(18);
        breakMinutesInput.setHint("例如：30");
        breakMinutesInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(breakMinutesInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        calculatedDailyHoursText = text("", 14, true);
        calculatedDailyHoursText.setPadding(0, dp(8), 0, 0);
        root.addView(calculatedDailyHoursText);

        Button preview = new Button(this);
        preview.setText("计算每天工时");
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        previewParams.topMargin = dp(8);
        root.addView(preview, previewParams);
        preview.setOnClickListener(v -> previewDailyHours());

        TextView dayLabel = text("哪些天算工作日", 15, false);
        dayLabel.setPadding(0, dp(20), 0, dp(8));
        root.addView(dayLabel);

        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < 7; i++) {
            dayChecks[i] = new CheckBox(this);
            dayChecks[i].setText(names[i]);
            dayChecks[i].setTextSize(16);
            days.addView(dayChecks[i]);
        }
        root.addView(days);

        Button save = new Button(this);
        save.setText("保存设置并重新计算");
        save.setTextSize(16);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        saveParams.topMargin = dp(20);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> saveSettings());

        Button refresh = new Button(this);
        refresh.setText("刷新今天的数据");
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        refreshParams.topMargin = dp(10);
        root.addView(refresh, refreshParams);
        refresh.setOnClickListener(v -> updateSummary());

        return scroll;
    }

    private EditText timeInput(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        input.setTextSize(18);
        input.setHint(hint);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        return input;
    }

    private void loadSettings() {
        startTimeInput.setText(prefs.getString(START_TIME_KEY, "09:00"));
        endTimeInput.setText(prefs.getString(END_TIME_KEY, "17:30"));
        breakMinutesInput.setText(String.valueOf(prefs.getInt(BREAK_MINUTES_KEY, 30)));
        for (int i = 0; i < 7; i++) {
            dayChecks[i].setChecked(prefs.getBoolean("day_" + i, i < 5));
        }
        previewDailyHours();
    }

    private void previewDailyHours() {
        Float hours = calculateHoursFromInputs(true);
        if (hours != null) {
            calculatedDailyHoursText.setText("按当前设置：每天 " + formatDurationHours(hours));
        }
    }

    private void saveSettings() {
        Float dailyHours = calculateHoursFromInputs(true);
        if (dailyHours == null) return;

        int breakMinutes;
        try {
            breakMinutes = Integer.parseInt(breakMinutesInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            breakMinutesInput.setError("请输入正确的休息分钟数");
            return;
        }

        String start = normalizeTime(startTimeInput.getText().toString().trim());
        String end = normalizeTime(endTimeInput.getText().toString().trim());

        SharedPreferences.Editor editor = prefs.edit()
                .putString(START_TIME_KEY, start)
                .putString(END_TIME_KEY, end)
                .putInt(BREAK_MINUTES_KEY, breakMinutes);

        editor.putFloat("daily_hours", dailyHours);

        for (int i = 0; i < 7; i++) {
            editor.putBoolean("day_" + i, dayChecks[i].isChecked());
        }
        editor.apply();

        startTimeInput.setText(start);
        endTimeInput.setText(end);
        calculatedDailyHoursText.setText("按当前设置：每天 " + formatDurationHours(dailyHours));
        updateSummary();
        Toast.makeText(this, "工作时间设置已保存", Toast.LENGTH_SHORT).show();
    }

    private Float calculateHoursFromInputs(boolean showError) {
        String startRaw = startTimeInput.getText().toString().trim();
        String endRaw = endTimeInput.getText().toString().trim();

        LocalTime start = parseTime(startRaw);
        if (start == null) {
            if (showError) startTimeInput.setError("请输入 HH:mm，例如 09:00");
            return null;
        }

        LocalTime end = parseTime(endRaw);
        if (end == null) {
            if (showError) endTimeInput.setError("请输入 HH:mm，例如 17:30");
            return null;
        }

        int breakMinutes;
        try {
            breakMinutes = Integer.parseInt(breakMinutesInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            if (showError) breakMinutesInput.setError("请输入正确的休息分钟数");
            return null;
        }

        if (breakMinutes < 0 || breakMinutes >= 24 * 60) {
            if (showError) breakMinutesInput.setError("休息时间应在 0～1439 分钟之间");
            return null;
        }

        long totalMinutes = Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) totalMinutes += 24 * 60;

        long workMinutes = totalMinutes - breakMinutes;
        if (workMinutes < 0) {
            if (showError) breakMinutesInput.setError("休息时间不能超过上下班间隔");
            return null;
        }

        return workMinutes / 60f;
    }

    private LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(normalizeTime(raw), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeTime(String raw) {
        String value = raw.trim();
        if (value.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = value.split(":");
            try {
                return String.format(Locale.US, "%02d:%02d",
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return value;
    }

    private float getConfiguredDailyHours() {
        String startRaw = prefs.getString(START_TIME_KEY, "09:00");
        String endRaw = prefs.getString(END_TIME_KEY, "17:30");
        int breakMinutes = prefs.getInt(BREAK_MINUTES_KEY, 30);

        LocalTime start = parseTime(startRaw);
        LocalTime end = parseTime(endRaw);
        if (start == null || end == null) return prefs.getFloat("daily_hours", 8f);

        long totalMinutes = Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) totalMinutes += 24 * 60;
        long workMinutes = Math.max(0, totalMinutes - breakMinutes);
        return workMinutes / 60f;
    }

    private void updateSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate first = currentMonth.atDay(1);
        int automaticWorkDays = 0;
        float total = 0f;
        float dailyHours = getConfiguredDailyHours();

        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            boolean workDay = isConfiguredWorkDay(d) && !isBankHoliday(d);
            if (workDay) automaticWorkDays++;
            total += getHoursForDate(d, dailyHours, workDay);
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA);
        todayText.setText("今天：" + today.format(dateFmt));
        monthText.setText(currentMonth.getYear() + "年" + currentMonth.getMonthValue() + "月");
        workDaysText.setText("截至今天默认工作日：" + automaticWorkDays + " 天 · 每天 " + formatDurationHours(dailyHours));
        totalHoursText.setText(formatDurationHours(total));

        rebuildHolidays(currentMonth);
        rebuildLeaves(currentMonth, today);
        rebuildRecords(first, today, dailyHours);
    }

    private void rebuildRecords(LocalDate first, LocalDate today, float dailyHours) {
        recordsContainer.removeAllViews();
        DateTimeFormatter rowDate = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);

        for (LocalDate d = today; !d.isBefore(first); d = d.minusDays(1)) {
            final LocalDate date = d;
            boolean bankHoliday = isBankHoliday(date);
            boolean leave = !bankHoliday && isLeave(date);
            boolean rest = !bankHoliday && !leave && isManualRest(date);
            boolean configuredWorkDay = isConfiguredWorkDay(date) && !bankHoliday;
            boolean hasOverride = !bankHoliday && !leave && !rest && hasOverride(date);
            float hours = getHoursForDate(date, dailyHours, configuredWorkDay);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(11), dp(12), dp(11));
            row.setBackgroundColor(leave ? 0xFFE8F0FE :
                    (rest ? 0xFFF1F3F4 :
                            (hasOverride ? 0xFFFFF4E5 : 0xFFF8F9FA)));

            TextView dateText = text(date.format(rowDate), 15, date.equals(today));
            row.addView(dateText, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            String status;
            if (bankHoliday) {
                status = "公共假期 · 0 小时";
            } else if (leave) {
                String note = getLeaveNote(date);
                status = "请假" + (note.isEmpty() ? "" : " · " + note) + " · 0 小时";
            } else if (rest) {
                status = "休息 · 0 小时 · 已修改";
            } else if (hasOverride) {
                status = "正常 · " + formatDurationHours(hours) + " · 已修改";
            } else if (configuredWorkDay) {
                status = "正常 · " + formatDurationHours(hours);
            } else {
                status = "休息 · 0 小时";
            }

            row.addView(text(status, 14, leave || rest || hasOverride));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(5);
            recordsContainer.addView(row, rowParams);

            if (bankHoliday) {
                row.setOnClickListener(v -> Toast.makeText(this,
                        getBankHolidayName(date) + "：公共假期不计工时",
                        Toast.LENGTH_SHORT).show());
            } else {
                row.setOnClickListener(v -> showEditDayDialog(date));
            }
        }
    }

    private void showEditDayDialog(LocalDate date) {
        if (isBankHoliday(date)) {
            Toast.makeText(this,
                    getBankHolidayName(date) + "：公共假期不计工时",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        float dailyHours = getConfiguredDailyHours();
        boolean configuredWorkDay = isConfiguredWorkDay(date);
        float automaticHours = configuredWorkDay ? dailyHours : 0f;
        float currentHours = hasOverride(date)
                ? prefs.getFloat(overrideKey(date), automaticHours)
                : automaticHours;
        if (currentHours <= 0f) currentHours = dailyHours;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(8), dp(22), 0);
        box.addView(text("当天状态", 14, true));

        RadioGroup statusGroup = new RadioGroup(this);
        statusGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton normalRadio = new RadioButton(this);
        normalRadio.setText("正常上班");
        RadioButton leaveRadio = new RadioButton(this);
        leaveRadio.setText("请假（0 小时）");
        RadioButton restRadio = new RadioButton(this);
        restRadio.setText("休息（0 小时）");

        statusGroup.addView(normalRadio);
        statusGroup.addView(leaveRadio);
        statusGroup.addView(restRadio);
        box.addView(statusGroup);

        if (isLeave(date)) {
            leaveRadio.setChecked(true);
        } else if (isManualRest(date) || (!configuredWorkDay && !hasOverride(date))) {
            restRadio.setChecked(true);
        } else {
            normalRadio.setChecked(true);
        }

        TextView automaticHint = text(
                "默认工时：" + formatDurationHours(dailyHours) + "（由上下班时间自动计算）",
                13, false);
        automaticHint.setPadding(0, dp(6), 0, 0);
        box.addView(automaticHint);

        TextView reasonLabel = text("请假原因 / 描述（可选）", 14, false);
        reasonLabel.setPadding(0, dp(8), 0, 0);
        box.addView(reasonLabel);

        EditText reasonInput = new EditText(this);
        reasonInput.setSingleLine(false);
        reasonInput.setMinLines(2);
        reasonInput.setMaxLines(4);
        reasonInput.setHint("例如：看医生、年假、家庭原因");
        reasonInput.setText(getLeaveNote(date));
        box.addView(reasonInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView hoursLabel = text("当天工时（仅用于单日覆盖）", 14, false);
        hoursLabel.setPadding(0, dp(10), 0, 0);
        box.addView(hoursLabel);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(trimNumber(currentHours));
        input.setSelectAllOnFocus(true);
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        Runnable refreshControls = () -> {
            boolean normal = normalRadio.isChecked();
            boolean leave = leaveRadio.isChecked();
            input.setEnabled(normal);
            reasonInput.setEnabled(leave);
            reasonLabel.setEnabled(leave);
            hoursLabel.setEnabled(normal);
            if (!normal) {
                input.setText("0");
            } else if (input.getText().toString().trim().equals("0")) {
                input.setText(trimNumber(dailyHours));
            }
        };
        statusGroup.setOnCheckedChangeListener((group, checkedId) -> refreshControls.run());
        refreshControls.run();

        DateTimeFormatter titleFmt =
                DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(date.format(titleFmt))
                .setView(box)
                .setPositiveButton("保存", null)
                .setNeutralButton("恢复自动", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String targetStatus;
                String reason = reasonInput.getText().toString().trim();
                float value = 0f;

                if (normalRadio.isChecked()) {
                    targetStatus = "正常上班";
                    try {
                        value = Float.parseFloat(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        input.setError("请输入正确的工时");
                        return;
                    }
                    if (value < 0 || value > 24) {
                        input.setError("工时应在 0～24 之间");
                        return;
                    }
                } else if (leaveRadio.isChecked()) {
                    targetStatus = "请假";
                } else {
                    targetStatus = "休息";
                }

                final float confirmedHours = value;
                StringBuilder summary = new StringBuilder();
                summary.append("日期：").append(date.format(titleFmt)).append("\n");
                summary.append("状态：").append(targetStatus).append("\n");
                summary.append("工时：").append(formatDurationHours(confirmedHours));
                if (leaveRadio.isChecked() && !reason.isEmpty()) {
                    summary.append("\n原因：").append(reason);
                }

                new AlertDialog.Builder(this)
                        .setTitle("确认修改？")
                        .setMessage(summary.toString())
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确认修改", (confirmDialog, which) -> {
                            SharedPreferences.Editor editor = prefs.edit();

                            if (leaveRadio.isChecked()) {
                                editor.putBoolean(leaveKey(date), true);
                                if (reason.isEmpty()) {
                                    editor.remove(leaveNoteKey(date));
                                } else {
                                    editor.putString(leaveNoteKey(date), reason);
                                }
                                editor.remove(restKey(date));
                                editor.remove(overrideKey(date));
                            } else if (restRadio.isChecked()) {
                                editor.putBoolean(restKey(date), true);
                                editor.remove(leaveKey(date));
                                editor.remove(leaveNoteKey(date));
                                editor.remove(overrideKey(date));
                            } else {
                                if (configuredWorkDay
                                        && Math.abs(confirmedHours - dailyHours) < 0.0001f) {
                                    editor.remove(overrideKey(date));
                                } else {
                                    editor.putFloat(overrideKey(date), confirmedHours);
                                }
                                editor.remove(leaveKey(date));
                                editor.remove(leaveNoteKey(date));
                                editor.remove(restKey(date));
                            }

                            editor.apply();
                            dialog.dismiss();
                            updateSummary();
                            Toast.makeText(this, "修改已保存", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("确认恢复自动？")
                            .setMessage("将清除 " + date.getMonthValue() + "月"
                                    + date.getDayOfMonth()
                                    + "日的手动状态、工时和请假原因，恢复按工作日及上下班时间自动计算。")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("确认恢复", (confirmDialog, which) -> {
                                prefs.edit()
                                        .remove(overrideKey(date))
                                        .remove(leaveKey(date))
                                        .remove(leaveNoteKey(date))
                                        .remove(restKey(date))
                                        .apply();
                                dialog.dismiss();
                                updateSummary();
                                Toast.makeText(this, "已恢复自动计算", Toast.LENGTH_SHORT).show();
                            })
                            .show());
        });

        dialog.show();
    }

    private void rebuildHolidays(YearMonth month) {
        holidaysContainer.removeAllViews();
        boolean found = false;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            String name = getBankHolidayName(date);
            if (name != null) {
                found = true;
                TextView line = text(
                        date.format(fmt) + " · " + name + " · 0 小时",
                        13, false);
                line.setPadding(0, dp(3), 0, dp(3));
                holidaysContainer.addView(line);
            }
        }

        if (!found) {
            holidaysContainer.addView(text("本月无公共假期", 13, false));
        }
    }

    private void rebuildLeaves(YearMonth month, LocalDate today) {
        leaveContainer.removeAllViews();
        boolean found = false;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);
        int lastDay = month.equals(YearMonth.from(today))
                ? today.getDayOfMonth()
                : month.lengthOfMonth();

        for (int day = 1; day <= lastDay; day++) {
            LocalDate date = month.atDay(day);
            if (isLeave(date) && !isBankHoliday(date)) {
                found = true;
                String note = getLeaveNote(date);
                String value = date.format(fmt)
                        + " · 请假"
                        + (note.isEmpty() ? "" : " · " + note)
                        + " · 0 小时";
                TextView line = text(value, 13, true);
                line.setPadding(0, dp(3), 0, dp(3));
                leaveContainer.addView(line);
            }
        }

        if (!found) {
            leaveContainer.addView(text("本月无请假记录", 13, false));
        }
    }

    private boolean isBankHoliday(LocalDate date) {
        return getBankHolidayName(date) != null;
    }

    private String getBankHolidayName(LocalDate date) {
        int year = date.getYear();

        LocalDate newYear = observedDate(LocalDate.of(year, Month.JANUARY, 1));
        if (date.equals(newYear)) return "New Year’s Day";

        LocalDate easterSunday = easterSunday(year);
        if (date.equals(easterSunday.minusDays(2))) return "Good Friday";
        if (date.equals(easterSunday.plusDays(1))) return "Easter Monday";

        LocalDate earlyMay = LocalDate.of(year, Month.MAY, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        if (date.equals(earlyMay)) return "Early May bank holiday";

        LocalDate spring = LocalDate.of(year, Month.MAY, 31)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (date.equals(spring)) return "Spring bank holiday";

        LocalDate summer = LocalDate.of(year, Month.AUGUST, 31)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (date.equals(summer)) return "Summer bank holiday";

        LocalDate christmas = LocalDate.of(year, Month.DECEMBER, 25);
        LocalDate boxing = LocalDate.of(year, Month.DECEMBER, 26);
        LocalDate observedChristmas;
        LocalDate observedBoxing;

        if (christmas.getDayOfWeek() == DayOfWeek.SATURDAY) {
            observedChristmas = LocalDate.of(year, Month.DECEMBER, 27);
            observedBoxing = LocalDate.of(year, Month.DECEMBER, 28);
        } else if (christmas.getDayOfWeek() == DayOfWeek.SUNDAY) {
            observedChristmas = LocalDate.of(year, Month.DECEMBER, 27);
            observedBoxing = LocalDate.of(year, Month.DECEMBER, 26);
        } else {
            observedChristmas = christmas;
            observedBoxing = boxing.getDayOfWeek() == DayOfWeek.SATURDAY
                    ? LocalDate.of(year, Month.DECEMBER, 28)
                    : boxing;
        }

        if (date.equals(observedChristmas)) return "Christmas Day";
        if (date.equals(observedBoxing)) return "Boxing Day";

        return null;
    }

    private LocalDate observedDate(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) return date.plusDays(2);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) return date.plusDays(1);
        return date;
    }

    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private boolean isConfiguredWorkDay(LocalDate date) {
        int index = dayIndex(date.getDayOfWeek());
        return prefs.getBoolean("day_" + index, index < 5);
    }

    private float getHoursForDate(LocalDate date, float dailyHours, boolean configuredWorkDay) {
        if (isBankHoliday(date) || isLeave(date) || isManualRest(date)) return 0f;
        float automatic = configuredWorkDay ? dailyHours : 0f;
        return prefs.getFloat(overrideKey(date), automatic);
    }

    private boolean hasOverride(LocalDate date) {
        return prefs.contains(overrideKey(date));
    }

    private boolean isLeave(LocalDate date) {
        return prefs.getBoolean(leaveKey(date), false);
    }

    private boolean isManualRest(LocalDate date) {
        return prefs.getBoolean(restKey(date), false);
    }

    private String getLeaveNote(LocalDate date) {
        return prefs.getString(leaveNoteKey(date), "");
    }

    private String overrideKey(LocalDate date) {
        return OVERRIDE_PREFIX + date;
    }

    private String leaveKey(LocalDate date) {
        return LEAVE_PREFIX + date;
    }

    private String leaveNoteKey(LocalDate date) {
        return LEAVE_NOTE_PREFIX + date;
    }

    private String restKey(LocalDate date) {
        return REST_PREFIX + date;
    }

    private int dayIndex(DayOfWeek day) {
        return day.getValue() - 1;
    }

    private String trimNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private String formatDurationHours(float hours) {
        int minutes = Math.round(hours * 60f);
        int wholeHours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) return wholeHours + " 小时";
        if (wholeHours == 0) return remainingMinutes + " 分钟";
        return wholeHours + " 小时 " + remainingMinutes + " 分钟";
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(0xFF202124);
        view.setGravity(Gravity.START);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
