package com.example.workhours;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
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

    private SharedPreferences prefs;
    private YearMonth displayedMonth;
    private TextView monthTitle;
    private TextView totalHoursText;
    private TextView workDaysText;
    private TextView leaveDaysText;
    private TextView holidayDaysText;
    private Button nextMonthButton;
    private GridLayout calendarGrid;
    private LinearLayout exceptionsContainer;

    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private Button rangeStartButton;
    private Button rangeEndButton;
    private TextView rangeSummaryText;
    private LinearLayout rangeDetailsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        displayedMonth = YearMonth.now();
        LocalDate today = LocalDate.now();
        rangeStart = today.withDayOfMonth(1);
        rangeEnd = today;
        setContentView(buildUi());
        refreshMonth();
        updateRangeButtons();
        calculateRange();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (calendarGrid != null) {
            refreshMonth();
            calculateRange();
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        TextView title = text("上班总时间", 26, true);
        top.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button settings = new Button(this);
        settings.setText("设置");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        top.addView(settings, new LinearLayout.LayoutParams(dp(86), dp(48)));

        LinearLayout monthNav = new LinearLayout(this);
        monthNav.setOrientation(LinearLayout.HORIZONTAL);
        monthNav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        navParams.topMargin = dp(14);
        root.addView(monthNav, navParams);

        Button previous = new Button(this);
        previous.setText("‹");
        previous.setTextSize(24);
        previous.setOnClickListener(v -> {
            displayedMonth = displayedMonth.minusMonths(1);
            refreshMonth();
        });
        monthNav.addView(previous, new LinearLayout.LayoutParams(dp(58), dp(48)));

        monthTitle = text("", 20, true);
        monthTitle.setGravity(Gravity.CENTER);
        monthNav.addView(monthTitle, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        nextMonthButton = new Button(this);
        nextMonthButton.setText("›");
        nextMonthButton.setTextSize(24);
        nextMonthButton.setOnClickListener(v -> {
            if (displayedMonth.isBefore(YearMonth.now())) {
                displayedMonth = displayedMonth.plusMonths(1);
                refreshMonth();
            }
        });
        monthNav.addView(nextMonthButton, new LinearLayout.LayoutParams(dp(58), dp(48)));

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(dp(16), dp(14), dp(16), dp(14));
        summary.setBackgroundColor(0xFFF4F5F7);
        root.addView(summary, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        summary.addView(text("本月总工时", 13, false));
        totalHoursText = text("", 32, true);
        totalHoursText.setPadding(0, dp(3), 0, dp(10));
        summary.addView(totalHoursText);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        summary.addView(stats);
        workDaysText = statText();
        leaveDaysText = statText();
        holidayDaysText = statText();
        stats.addView(workDaysText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(leaveDaysText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(holidayDaysText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView calendarTitle = text("月历", 19, true);
        calendarTitle.setPadding(0, dp(22), 0, dp(8));
        root.addView(calendarTitle);

        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        root.addView(calendarGrid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView hint = text("点击日期可查看或修改。普通工作日会自动计入，无需每天设置。", 13, false);
        hint.setPadding(0, dp(8), 0, 0);
        root.addView(hint);

        TextView rangeTitle = text("日期范围统计", 19, true);
        rangeTitle.setPadding(0, dp(24), 0, dp(8));
        root.addView(rangeTitle);

        LinearLayout rangeRow = new LinearLayout(this);
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        rangeRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(rangeRow);

        rangeStartButton = new Button(this);
        rangeStartButton.setOnClickListener(v -> showDatePicker(true));
        rangeRow.addView(rangeStartButton, new LinearLayout.LayoutParams(0, dp(50), 1f));

        TextView to = text(" 至 ", 14, false);
        to.setGravity(Gravity.CENTER);
        rangeRow.addView(to, new LinearLayout.LayoutParams(dp(38), dp(50)));

        rangeEndButton = new Button(this);
        rangeEndButton.setOnClickListener(v -> showDatePicker(false));
        rangeRow.addView(rangeEndButton, new LinearLayout.LayoutParams(0, dp(50), 1f));

        Button rangeCalculate = new Button(this);
        rangeCalculate.setText("统计这段时间");
        LinearLayout.LayoutParams rangeButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        rangeButtonParams.topMargin = dp(8);
        root.addView(rangeCalculate, rangeButtonParams);
        rangeCalculate.setOnClickListener(v -> calculateRange());

        LinearLayout rangeCard = new LinearLayout(this);
        rangeCard.setOrientation(LinearLayout.VERTICAL);
        rangeCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        rangeCard.setBackgroundColor(0xFFF8F9FA);
        LinearLayout.LayoutParams rangeCardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rangeCardParams.topMargin = dp(8);
        root.addView(rangeCard, rangeCardParams);

        rangeSummaryText = text("", 16, true);
        rangeCard.addView(rangeSummaryText);
        rangeDetailsContainer = new LinearLayout(this);
        rangeDetailsContainer.setOrientation(LinearLayout.VERTICAL);
        rangeCard.addView(rangeDetailsContainer);

        TextView exceptionTitle = text("本月异常记录", 19, true);
        exceptionTitle.setPadding(0, dp(24), 0, dp(8));
        root.addView(exceptionTitle);
        exceptionsContainer = new LinearLayout(this);
        exceptionsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(exceptionsContainer);

        return scroll;
    }

    private TextView statText() {
        TextView view = text("", 14, true);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private void showDatePicker(boolean start) {
        LocalDate initial = start ? rangeStart : rangeEnd;
        LocalDate today = LocalDate.now();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    if (selected.isAfter(today)) selected = today;
                    if (start) rangeStart = selected;
                    else rangeEnd = selected;
                    updateRangeButtons();
                }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void updateRangeButtons() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        rangeStartButton.setText(rangeStart.format(fmt));
        rangeEndButton.setText(rangeEnd.format(fmt));
    }

    private void calculateRange() {
        rangeDetailsContainer.removeAllViews();
        LocalDate today = LocalDate.now();
        LocalDate start = rangeStart;
        LocalDate end = rangeEnd.isAfter(today) ? today : rangeEnd;

        if (start.isAfter(end)) {
            rangeSummaryText.setText("开始日期不能晚于结束日期");
            rangeDetailsContainer.addView(text("请重新选择日期范围。", 13, false));
            return;
        }

        float dailyHours = getConfiguredDailyHours();
        float total = 0f;
        int workDays = 0;
        int leaveDays = 0;
        int holidayDays = 0;
        int restDays = 0;
        int overrideDays = 0;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年M月d日 E", Locale.CHINA);
        LinearLayout details = rangeDetailsContainer;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            boolean holiday = isBankHoliday(date);
            boolean leave = !holiday && isLeave(date);
            boolean rest = !holiday && !leave && isManualRest(date);
            boolean configuredWorkDay = isConfiguredWorkDay(date) && !holiday;
            boolean override = !holiday && !leave && !rest && hasOverride(date);
            float hours = getHoursForDate(date, dailyHours, configuredWorkDay);

            total += hours;
            if (hours > 0f) workDays++;
            if (leave) leaveDays++;
            if (holiday) holidayDays++;
            if (rest) restDays++;
            if (override) overrideDays++;

            String line = null;
            if (holiday) {
                line = date.format(fmt) + " · 公共假期 · " + getBankHolidayName(date) + " · 0 小时";
            } else if (leave) {
                String note = getLeaveNote(date);
                line = date.format(fmt) + " · 请假" + (note.isEmpty() ? "" : " · " + note) + " · 0 小时";
            } else if (rest) {
                line = date.format(fmt) + " · 休息 · 0 小时";
            } else if (override) {
                line = date.format(fmt) + " · 自定义工时 · " + formatDurationHours(hours);
            }

            if (line != null) {
                TextView row = text(line, 13, true);
                row.setPadding(0, dp(5), 0, dp(5));
                details.addView(row);
            }
        }

        long days = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        rangeSummaryText.setText(
                start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 至 "
                        + end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        + "\n总工时：" + formatDurationHours(total)
                        + "\n工作：" + workDays + "天"
                        + " · 请假：" + leaveDays + "天"
                        + " · 假期：" + holidayDays + "天"
                        + " · 休息：" + restDays + "天"
                        + " · 自定义：" + overrideDays + "天"
                        + "\n范围共 " + days + " 天");

        if (details.getChildCount() == 0) {
            TextView none = text("这段时间没有公共假期、请假、手动休息或自定义工时记录。", 13, false);
            none.setPadding(0, dp(8), 0, 0);
            details.addView(none);
        }
    }

    private void refreshMonth() {
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.from(today);
        if (displayedMonth.isAfter(current)) displayedMonth = current;
        monthTitle.setText(displayedMonth.getYear() + "年" + displayedMonth.getMonthValue() + "月");
        nextMonthButton.setEnabled(displayedMonth.isBefore(current));

        LocalDate start = displayedMonth.atDay(1);
        LocalDate end = displayedMonth.equals(current) ? today : displayedMonth.atEndOfMonth();
        float dailyHours = getConfiguredDailyHours();
        int workDays = 0;
        int leaveDays = 0;
        int holidayDays = 0;
        float total = 0f;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean holiday = isBankHoliday(d);
            boolean configuredWorkDay = isConfiguredWorkDay(d) && !holiday;
            float hours = getHoursForDate(d, dailyHours, configuredWorkDay);
            total += hours;
            if (isLeave(d) && !holiday) leaveDays++;
            if (holiday) holidayDays++;
            if (hours > 0f) workDays++;
        }

        totalHoursText.setText(formatDurationHours(total));
        workDaysText.setText("工作\n" + workDays + "天");
        leaveDaysText.setText("请假\n" + leaveDays + "天");
        holidayDaysText.setText("假期\n" + holidayDays + "天");
        rebuildCalendar(today, dailyHours);
        rebuildExceptions(end, dailyHours);
    }

    private void rebuildCalendar(LocalDate today, float dailyHours) {
        calendarGrid.removeAllViews();
        String[] headers = {"一", "二", "三", "四", "五", "六", "日"};
        for (String header : headers) {
            TextView h = text(header, 13, true);
            h.setGravity(Gravity.CENTER);
            calendarGrid.addView(h, gridParams());
        }

        int leading = displayedMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int days = displayedMonth.lengthOfMonth();
        int cells = ((leading + days + 6) / 7) * 7;

        for (int i = 0; i < cells; i++) {
            int day = i - leading + 1;
            if (day < 1 || day > days) {
                calendarGrid.addView(text("", 13, false), gridParams());
                continue;
            }

            LocalDate date = displayedMonth.atDay(day);
            boolean future = date.isAfter(today);
            boolean holiday = isBankHoliday(date);
            boolean leave = !holiday && isLeave(date);
            boolean rest = !holiday && !leave && isManualRest(date);
            boolean configuredWorkDay = isConfiguredWorkDay(date) && !holiday;
            boolean override = !holiday && !leave && !rest && hasOverride(date);
            float hours = future ? 0f : getHoursForDate(date, dailyHours, configuredWorkDay);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2), dp(6), dp(2), dp(5));
            if (date.equals(today)) cell.setBackgroundColor(0xFFE8F0FE);
            else if (leave) cell.setBackgroundColor(0xFFEAF2FF);
            else if (override) cell.setBackgroundColor(0xFFFFF4E5);
            else if (holiday) cell.setBackgroundColor(0xFFFFEBEE);

            TextView dayText = text(String.valueOf(day), 14, date.equals(today));
            dayText.setGravity(Gravity.CENTER);
            if (future) dayText.setTextColor(0xFF9AA0A6);
            cell.addView(dayText);

            String status = "";
            if (!future) {
                if (holiday) status = "假期";
                else if (leave) status = "请假";
                else if (rest) status = "休息";
                else if (hours > 0f) status = shortHours(hours) + (override ? "*" : "");
            }
            TextView statusText = text(status, 11, leave || holiday || rest || override);
            statusText.setGravity(Gravity.CENTER);
            cell.addView(statusText);

            if (!future) {
                cell.setOnClickListener(v -> {
                    if (isBankHoliday(date)) {
                        Toast.makeText(this, getBankHolidayName(date) + "：公共假期不计工时", Toast.LENGTH_SHORT).show();
                    } else {
                        showEditDayDialog(date);
                    }
                });
            }
            calendarGrid.addView(cell, gridParams());
        }
    }

    private GridLayout.LayoutParams gridParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(58);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private void rebuildExceptions(LocalDate end, float dailyHours) {
        exceptionsContainer.removeAllViews();
        boolean found = false;
        LocalDate start = displayedMonth.atDay(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String line = null;
            boolean editable = true;
            if (isBankHoliday(date)) {
                line = date.format(fmt) + " · 公共假期 · " + getBankHolidayName(date);
                editable = false;
            } else if (isLeave(date)) {
                String note = getLeaveNote(date);
                line = date.format(fmt) + " · 请假" + (note.isEmpty() ? "" : " · " + note);
            } else if (isManualRest(date)) {
                line = date.format(fmt) + " · 休息 · 手动修改";
            } else if (hasOverride(date)) {
                float hours = getHoursForDate(date, dailyHours, isConfiguredWorkDay(date));
                line = date.format(fmt) + " · 自定义工时 · " + formatDurationHours(hours);
            }

            if (line != null) {
                found = true;
                final LocalDate targetDate = date;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(10), dp(12), dp(10));
                row.setBackgroundColor(0xFFF8F9FA);
                TextView value = text(line, 14, true);
                row.addView(value, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                if (editable) {
                    row.addView(text("修改", 13, true));
                    row.setOnClickListener(v -> showEditDayDialog(targetDate));
                }
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(6);
                exceptionsContainer.addView(row, rowParams);
            }
        }
        if (!found) {
            TextView none = text("本月无请假、手动休息或自定义工时记录", 14, false);
            none.setPadding(dp(12), dp(10), dp(12), dp(10));
            none.setBackgroundColor(0xFFF8F9FA);
            exceptionsContainer.addView(none);
        }
    }

    private void showEditDayDialog(LocalDate date) {
        float dailyHours = getConfiguredDailyHours();
        boolean configuredWorkDay = isConfiguredWorkDay(date);
        float automaticHours = configuredWorkDay ? dailyHours : 0f;
        float currentHours = hasOverride(date) ? prefs.getFloat(overrideKey(date), automaticHours) : automaticHours;
        if (currentHours <= 0f) currentHours = dailyHours;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(8), dp(22), 0);
        box.addView(text("当天状态", 14, true));

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        RadioButton normal = new RadioButton(this);
        normal.setText("正常上班");
        RadioButton leave = new RadioButton(this);
        leave.setText("请假（0 小时）");
        RadioButton rest = new RadioButton(this);
        rest.setText("休息（0 小时）");
        group.addView(normal);
        group.addView(leave);
        group.addView(rest);
        box.addView(group);

        if (isLeave(date)) leave.setChecked(true);
        else if (isManualRest(date) || (!configuredWorkDay && !hasOverride(date))) rest.setChecked(true);
        else normal.setChecked(true);

        TextView reasonLabel = text("请假原因 / 描述（可选）", 14, false);
        reasonLabel.setPadding(0, dp(8), 0, 0);
        box.addView(reasonLabel);
        EditText reasonInput = new EditText(this);
        reasonInput.setMinLines(2);
        reasonInput.setMaxLines(4);
        reasonInput.setHint("例如：看医生、年假、家庭原因");
        reasonInput.setText(getLeaveNote(date));
        box.addView(reasonInput);

        TextView hoursLabel = text("当天工时（小时）", 14, false);
        hoursLabel.setPadding(0, dp(10), 0, 0);
        box.addView(hoursLabel);
        EditText hoursInput = new EditText(this);
        hoursInput.setSingleLine(true);
        hoursInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        hoursInput.setText(trimNumber(currentHours));
        box.addView(hoursInput);

        Runnable controls = () -> {
            boolean isNormal = normal.isChecked();
            boolean isLeave = leave.isChecked();
            hoursInput.setEnabled(isNormal);
            hoursLabel.setEnabled(isNormal);
            reasonInput.setEnabled(isLeave);
            reasonLabel.setEnabled(isLeave);
            if (!isNormal) hoursInput.setText("0");
            else if ("0".equals(hoursInput.getText().toString().trim())) hoursInput.setText(trimNumber(dailyHours));
        };
        group.setOnCheckedChangeListener((g, checkedId) -> controls.run());
        controls.run();

        DateTimeFormatter titleFmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(date.format(titleFmt))
                .setView(box)
                .setPositiveButton("保存", null)
                .setNeutralButton("恢复自动", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reason = reasonInput.getText().toString().trim();
                float value = 0f;
                String target;
                if (normal.isChecked()) {
                    target = "正常上班";
                    try {
                        value = Float.parseFloat(hoursInput.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        hoursInput.setError("请输入正确的工时");
                        return;
                    }
                    if (value < 0 || value > 24) {
                        hoursInput.setError("工时应在 0～24 之间");
                        return;
                    }
                } else if (leave.isChecked()) target = "请假";
                else target = "休息";

                final float confirmed = value;
                StringBuilder message = new StringBuilder()
                        .append("日期：").append(date.format(titleFmt)).append("\n")
                        .append("状态：").append(target).append("\n")
                        .append("工时：").append(formatDurationHours(confirmed));
                if (leave.isChecked() && !reason.isEmpty()) message.append("\n原因：").append(reason);

                new AlertDialog.Builder(this)
                        .setTitle("确认修改？")
                        .setMessage(message.toString())
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确认修改", (d, which) -> {
                            SharedPreferences.Editor editor = prefs.edit();
                            if (leave.isChecked()) {
                                editor.putBoolean(leaveKey(date), true).remove(restKey(date)).remove(overrideKey(date));
                                if (reason.isEmpty()) editor.remove(leaveNoteKey(date));
                                else editor.putString(leaveNoteKey(date), reason);
                            } else if (rest.isChecked()) {
                                editor.putBoolean(restKey(date), true).remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(overrideKey(date));
                            } else {
                                editor.remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(restKey(date));
                                if (configuredWorkDay && Math.abs(confirmed - dailyHours) < 0.0001f) editor.remove(overrideKey(date));
                                else editor.putFloat(overrideKey(date), confirmed);
                            }
                            editor.apply();
                            dialog.dismiss();
                            refreshMonth();
                            calculateRange();
                            Toast.makeText(this, "修改已保存", Toast.LENGTH_SHORT).show();
                        }).show();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("确认恢复自动？")
                            .setMessage("将清除当天手动状态、工时和请假原因，恢复按工作日设置自动计算。")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("确认恢复", (d, which) -> {
                                prefs.edit().remove(overrideKey(date)).remove(leaveKey(date))
                                        .remove(leaveNoteKey(date)).remove(restKey(date)).apply();
                                dialog.dismiss();
                                refreshMonth();
                                calculateRange();
                                Toast.makeText(this, "已恢复自动计算", Toast.LENGTH_SHORT).show();
                            }).show());
        });
        dialog.show();
    }

    private float getConfiguredDailyHours() {
        String startRaw = prefs.getString(START_TIME_KEY, "09:00");
        String endRaw = prefs.getString(END_TIME_KEY, "17:30");
        int breakMinutes = prefs.getInt(BREAK_MINUTES_KEY, 30);
        LocalTime start = parseTime(startRaw);
        LocalTime end = parseTime(endRaw);
        if (start == null || end == null) return prefs.getFloat("daily_hours", 8f);
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) minutes += 24 * 60;
        return Math.max(0, minutes - breakMinutes) / 60f;
    }

    private LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isConfiguredWorkDay(LocalDate date) {
        int index = date.getDayOfWeek().getValue() - 1;
        return prefs.getBoolean("day_" + index, index < 5);
    }

    private float getHoursForDate(LocalDate date, float dailyHours, boolean configuredWorkDay) {
        if (isBankHoliday(date) || isLeave(date) || isManualRest(date)) return 0f;
        return prefs.getFloat(overrideKey(date), configuredWorkDay ? dailyHours : 0f);
    }

    private boolean hasOverride(LocalDate date) { return prefs.contains(overrideKey(date)); }
    private boolean isLeave(LocalDate date) { return prefs.getBoolean(leaveKey(date), false); }
    private boolean isManualRest(LocalDate date) { return prefs.getBoolean(restKey(date), false); }
    private String getLeaveNote(LocalDate date) { return prefs.getString(leaveNoteKey(date), ""); }
    private String overrideKey(LocalDate date) { return OVERRIDE_PREFIX + date; }
    private String leaveKey(LocalDate date) { return LEAVE_PREFIX + date; }
    private String leaveNoteKey(LocalDate date) { return LEAVE_NOTE_PREFIX + date; }
    private String restKey(LocalDate date) { return REST_PREFIX + date; }
    private boolean isBankHoliday(LocalDate date) { return getBankHolidayName(date) != null; }

    private String getBankHolidayName(LocalDate date) {
        int year = date.getYear();
        LocalDate newYear = observedDate(LocalDate.of(year, Month.JANUARY, 1));
        if (date.equals(newYear)) return "New Year’s Day";
        LocalDate easter = easterSunday(year);
        if (date.equals(easter.minusDays(2))) return "Good Friday";
        if (date.equals(easter.plusDays(1))) return "Easter Monday";
        LocalDate earlyMay = LocalDate.of(year, Month.MAY, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        if (date.equals(earlyMay)) return "Early May bank holiday";
        LocalDate spring = LocalDate.of(year, Month.MAY, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (date.equals(spring)) return "Spring bank holiday";
        LocalDate summer = LocalDate.of(year, Month.AUGUST, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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
            observedBoxing = boxing.getDayOfWeek() == DayOfWeek.SATURDAY ? LocalDate.of(year, Month.DECEMBER, 28) : boxing;
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
        int a = year % 19, b = year / 100, c = year % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private String formatDurationHours(float hours) {
        int totalMinutes = Math.round(hours * 60f);
        int h = totalMinutes / 60, m = totalMinutes % 60;
        if (m == 0) return h + " 小时";
        if (h == 0) return m + " 分钟";
        return h + " 小时 " + m + " 分钟";
    }

    private String shortHours(float hours) {
        int minutes = Math.round(hours * 60f);
        if (minutes % 60 == 0) return (minutes / 60) + "h";
        return String.format(Locale.US, "%.1fh", hours);
    }

    private String trimNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) return String.valueOf(Math.round(value));
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
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
