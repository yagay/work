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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "work_hours_prefs";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String LEAVE_NOTE_PREFIX = "leave_note_";
    private static final String REST_PREFIX = "rest_";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";
    private static final String BREAK_MINUTES_KEY = "break_minutes";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";

    private SharedPreferences prefs;
    private YearMonth displayedMonth;
    private LocalDate displayedWeekStart;
    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    private TextView monthTitle, totalHoursText, workDaysText, leaveDaysText, holidayDaysText;
    private Button previousMonthButton, nextMonthButton;
    private GridLayout calendarGrid;
    private LinearLayout exceptionsContainer;

    private TextView weekTitle, weekSummaryText;
    private LinearLayout weekDetailsContainer;
    private Button previousWeekButton, nextWeekButton;

    private Button rangeStartButton, rangeEndButton;
    private TextView rangeSummaryText;
    private LinearLayout rangeDetailsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LocalDate today = LocalDate.now();
        LocalDate workStart = getWorkStartDate();
        displayedMonth = YearMonth.now();
        displayedWeekStart = mondayOf(today);
        rangeStart = today.withDayOfMonth(1);
        if (workStart != null && rangeStart.isBefore(workStart)) rangeStart = workStart;
        rangeEnd = today;
        setContentView(buildUi());
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (calendarGrid == null) return;
        LocalDate workStart = getWorkStartDate();
        if (workStart != null) {
            if (rangeStart.isBefore(workStart)) rangeStart = workStart;
            if (displayedMonth.isBefore(YearMonth.from(workStart))) displayedMonth = YearMonth.from(workStart);
            LocalDate firstWeek = mondayOf(workStart);
            if (displayedWeekStart.isBefore(firstWeek)) displayedWeekStart = firstWeek;
        }
        refreshAll();
    }

    private void refreshAll() {
        updateRangeButtons();
        refreshMonth();
        refreshWeek();
        calculateRange();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(root);

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);
        top.addView(text("上班总时间", 26, true), new LinearLayout.LayoutParams(0, -2, 1f));
        Button settings = button("设置");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        top.addView(settings, new LinearLayout.LayoutParams(dp(86), dp(48)));

        buildMonthSection(root);
        buildWeekSection(root);
        buildRangeSection(root);

        TextView exceptionTitle = text("本月异常记录", 19, true);
        exceptionTitle.setPadding(0, dp(24), 0, dp(8));
        root.addView(exceptionTitle);
        exceptionsContainer = vertical();
        root.addView(exceptionsContainer);
        return scroll;
    }

    private void buildMonthSection(LinearLayout root) {
        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(54));
        np.topMargin = dp(14);
        root.addView(nav, np);

        previousMonthButton = button("‹");
        previousMonthButton.setTextSize(24);
        previousMonthButton.setOnClickListener(v -> {
            YearMonth target = displayedMonth.minusMonths(1);
            LocalDate start = getWorkStartDate();
            if (start == null || !target.isBefore(YearMonth.from(start))) {
                displayedMonth = target;
                refreshMonth();
            }
        });
        nav.addView(previousMonthButton, new LinearLayout.LayoutParams(dp(58), dp(48)));

        monthTitle = text("", 20, true);
        monthTitle.setGravity(Gravity.CENTER);
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        nextMonthButton = button("›");
        nextMonthButton.setTextSize(24);
        nextMonthButton.setOnClickListener(v -> {
            if (displayedMonth.isBefore(YearMonth.now())) {
                displayedMonth = displayedMonth.plusMonths(1);
                refreshMonth();
            }
        });
        nav.addView(nextMonthButton, new LinearLayout.LayoutParams(dp(58), dp(48)));

        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundColor(0xFFF4F5F7);
        root.addView(card);
        card.addView(text("本月总工时", 13, false));
        totalHoursText = text("", 32, true);
        totalHoursText.setPadding(0, dp(3), 0, dp(10));
        card.addView(totalHoursText);

        LinearLayout stats = horizontal();
        card.addView(stats);
        workDaysText = statText(); leaveDaysText = statText(); holidayDaysText = statText();
        stats.addView(workDaysText, new LinearLayout.LayoutParams(0, -2, 1f));
        stats.addView(leaveDaysText, new LinearLayout.LayoutParams(0, -2, 1f));
        stats.addView(holidayDaysText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView calendarTitle = text("月历", 19, true);
        calendarTitle.setPadding(0, dp(22), 0, dp(8));
        root.addView(calendarTitle);
        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        root.addView(calendarGrid);
        TextView hint = text("自动休息日显示“休息”；周六、周日使用不同底色。点击日期可手动覆盖。", 13, false);
        hint.setPadding(0, dp(8), 0, 0);
        root.addView(hint);
    }

    private void buildWeekSection(LinearLayout root) {
        TextView title = text("按星期查看", 19, true);
        title.setPadding(0, dp(24), 0, dp(8));
        root.addView(title);
        LinearLayout nav = horizontal(); nav.setGravity(Gravity.CENTER_VERTICAL); root.addView(nav);

        previousWeekButton = button("‹"); previousWeekButton.setTextSize(24);
        previousWeekButton.setOnClickListener(v -> {
            LocalDate target = displayedWeekStart.minusWeeks(1);
            LocalDate start = getWorkStartDate();
            LocalDate first = start == null ? null : mondayOf(start);
            if (first == null || !target.isBefore(first)) { displayedWeekStart = target; refreshWeek(); }
        });
        nav.addView(previousWeekButton, new LinearLayout.LayoutParams(dp(58), dp(48)));
        weekTitle = text("", 17, true); weekTitle.setGravity(Gravity.CENTER);
        nav.addView(weekTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        nextWeekButton = button("›"); nextWeekButton.setTextSize(24);
        nextWeekButton.setOnClickListener(v -> {
            LocalDate current = mondayOf(LocalDate.now());
            if (displayedWeekStart.isBefore(current)) { displayedWeekStart = displayedWeekStart.plusWeeks(1); refreshWeek(); }
        });
        nav.addView(nextWeekButton, new LinearLayout.LayoutParams(dp(58), dp(48)));

        LinearLayout card = vertical(); card.setPadding(dp(14), dp(12), dp(14), dp(12)); card.setBackgroundColor(0xFFF8F9FA); root.addView(card);
        weekSummaryText = text("", 16, true); card.addView(weekSummaryText);
        weekDetailsContainer = vertical(); card.addView(weekDetailsContainer);
    }

    private void buildRangeSection(LinearLayout root) {
        TextView title = text("日期范围统计", 19, true); title.setPadding(0, dp(24), 0, dp(8)); root.addView(title);
        LinearLayout row = horizontal(); row.setGravity(Gravity.CENTER_VERTICAL); root.addView(row);
        rangeStartButton = button(""); rangeStartButton.setOnClickListener(v -> showDatePicker(true));
        row.addView(rangeStartButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView to = text(" 至 ", 14, false); to.setGravity(Gravity.CENTER); row.addView(to, new LinearLayout.LayoutParams(dp(38), dp(50)));
        rangeEndButton = button(""); rangeEndButton.setOnClickListener(v -> showDatePicker(false));
        row.addView(rangeEndButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button calc = button("统计这段时间"); calc.setOnClickListener(v -> calculateRange());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(50)); cp.topMargin = dp(8); root.addView(calc, cp);
        LinearLayout card = vertical(); card.setPadding(dp(14), dp(12), dp(14), dp(12)); card.setBackgroundColor(0xFFF8F9FA);
        LinearLayout.LayoutParams cardp = new LinearLayout.LayoutParams(-1, -2); cardp.topMargin = dp(8); root.addView(card, cardp);
        rangeSummaryText = text("", 16, true); card.addView(rangeSummaryText);
        rangeDetailsContainer = vertical(); card.addView(rangeDetailsContainer);
    }

    private void refreshMonth() {
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.from(today);
        LocalDate workStart = getWorkStartDate();
        YearMonth firstAllowed = workStart == null ? null : YearMonth.from(workStart);
        if (displayedMonth.isAfter(current)) displayedMonth = current;
        if (firstAllowed != null && displayedMonth.isBefore(firstAllowed)) displayedMonth = firstAllowed;
        monthTitle.setText(displayedMonth.getYear() + "年" + displayedMonth.getMonthValue() + "月");
        nextMonthButton.setEnabled(displayedMonth.isBefore(current));
        previousMonthButton.setEnabled(firstAllowed == null || displayedMonth.isAfter(firstAllowed));

        LocalDate start = displayedMonth.atDay(1);
        if (workStart != null && start.isBefore(workStart)) start = workStart;
        LocalDate end = displayedMonth.equals(current) ? today : displayedMonth.atEndOfMonth();
        Stats s = collectStats(start, end);
        totalHoursText.setText(formatDurationHours(s.totalHours));
        workDaysText.setText("工作\n" + s.workDays + "天");
        leaveDaysText.setText("请假\n" + s.leaveDays + "天");
        holidayDaysText.setText("公共假日\n" + s.holidayDays + "天");
        rebuildCalendar(today);
        rebuildExceptions(end);
    }

    private void refreshWeek() {
        LocalDate today = LocalDate.now();
        LocalDate currentWeek = mondayOf(today);
        LocalDate workStart = getWorkStartDate();
        LocalDate firstWeek = workStart == null ? null : mondayOf(workStart);
        if (displayedWeekStart.isAfter(currentWeek)) displayedWeekStart = currentWeek;
        if (firstWeek != null && displayedWeekStart.isBefore(firstWeek)) displayedWeekStart = firstWeek;
        LocalDate weekEnd = displayedWeekStart.plusDays(6);
        LocalDate start = displayedWeekStart;
        if (workStart != null && start.isBefore(workStart)) start = workStart;
        LocalDate end = weekEnd.isAfter(today) ? today : weekEnd;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M月d日");
        weekTitle.setText(displayedWeekStart.format(f) + " - " + weekEnd.format(f));
        previousWeekButton.setEnabled(firstWeek == null || displayedWeekStart.isAfter(firstWeek));
        nextWeekButton.setEnabled(displayedWeekStart.isBefore(currentWeek));
        weekDetailsContainer.removeAllViews();
        if (start.isAfter(end)) {
            weekSummaryText.setText("本周尚未开始工作");
            weekDetailsContainer.addView(text("工作开始日期之后才会统计。", 13, false));
            return;
        }
        Stats s = collectStats(start, end);
        weekSummaryText.setText("总工时：" + formatDurationHours(s.totalHours)
                + "\n工作：" + s.workDays + "天 · 请假：" + s.leaveDays + "天 · 公共假日：" + s.holidayDays + "天"
                + " · 休息：" + s.restDays + "天 · 自定义：" + s.overrideDays + "天");
        addPeriodDetails(weekDetailsContainer, start, end, true, true);
    }

    private void rebuildCalendar(LocalDate today) {
        calendarGrid.removeAllViews();
        String[] headers = {"一", "二", "三", "四", "五", "六", "日"};
        for (int i = 0; i < 7; i++) {
            TextView h = text(headers[i], 13, true); h.setGravity(Gravity.CENTER);
            if (i >= 5) h.setTextColor(0xFF5F6368);
            calendarGrid.addView(h, gridParams());
        }
        LocalDate workStart = getWorkStartDate();
        int leading = displayedMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int days = displayedMonth.lengthOfMonth();
        int cells = ((leading + days + 6) / 7) * 7;
        float dailyHours = getConfiguredDailyHours();

        for (int i = 0; i < cells; i++) {
            int day = i - leading + 1;
            if (day < 1 || day > days) { calendarGrid.addView(text("", 13, false), gridParams()); continue; }
            LocalDate date = displayedMonth.atDay(day);
            boolean future = date.isAfter(today);
            boolean beforeStart = workStart != null && date.isBefore(workStart);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean holiday = !beforeStart && isBankHoliday(date);
            boolean leave = !holiday && !beforeStart && isLeave(date);
            boolean manualRest = !holiday && !leave && !beforeStart && isManualRest(date);
            boolean configured = !beforeStart && isConfiguredWorkDay(date) && !holiday;
            boolean override = !holiday && !leave && !manualRest && !beforeStart && hasOverride(date);
            boolean autoRest = !holiday && !leave && !manualRest && !override && !beforeStart && !configured;
            float hours = (future || beforeStart) ? 0f : getHoursForDate(date, dailyHours, configured);

            LinearLayout cell = vertical(); cell.setGravity(Gravity.CENTER); cell.setPadding(dp(2), dp(6), dp(2), dp(5));
            if (date.equals(today)) cell.setBackgroundColor(0xFFE8F0FE);
            else if (leave) cell.setBackgroundColor(0xFFEAF2FF);
            else if (override) cell.setBackgroundColor(0xFFFFF4E5);
            else if (holiday) cell.setBackgroundColor(0xFFFFEBEE);
            else if (autoRest || manualRest || weekend) cell.setBackgroundColor(0xFFF1F3F4);

            TextView dt = text(String.valueOf(day), 14, date.equals(today)); dt.setGravity(Gravity.CENTER);
            if (future || beforeStart) dt.setTextColor(0xFF9AA0A6); else if (weekend && !leave && !override && !holiday) dt.setTextColor(0xFF5F6368);
            cell.addView(dt);

            String status = "";
            if (beforeStart) status = "未开始";
            else if (!future) {
                if (holiday) status = "公共假日";
                else if (leave) status = "请假";
                else if (manualRest || autoRest) status = "休息";
                else if (hours > 0f) status = shortHours(hours) + (override ? "*" : "");
            }
            TextView st = text(status, 10, leave || holiday || manualRest || override || autoRest); st.setGravity(Gravity.CENTER);
            if ((weekend || autoRest) && !leave && !override && !holiday) st.setTextColor(0xFF5F6368);
            cell.addView(st);

            if (!future && !beforeStart) {
                cell.setOnClickListener(v -> {
                    if (isBankHoliday(date)) Toast.makeText(this, getBankHolidayName(date) + "：公共假日不计工时", Toast.LENGTH_SHORT).show();
                    else showEditDayDialog(date);
                });
            }
            calendarGrid.addView(cell, gridParams());
        }
    }

    private void rebuildExceptions(LocalDate end) {
        exceptionsContainer.removeAllViews();
        LocalDate start = displayedMonth.atDay(1);
        LocalDate ws = getWorkStartDate();
        if (ws != null && start.isBefore(ws)) start = ws;
        if (start.isAfter(end)) { exceptionsContainer.addView(text("本月尚未开始工作", 14, false)); return; }
        addPeriodDetails(exceptionsContainer, start, end, false, false);
    }

    private void addPeriodDetails(LinearLayout container, LocalDate start, LocalDate end, boolean includeNormal, boolean includeAutoRest) {
        container.removeAllViews();
        float dailyHours = getConfiguredDailyHours();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);
        int count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            boolean holiday = isBankHoliday(date);
            boolean leave = !holiday && isLeave(date);
            boolean manualRest = !holiday && !leave && isManualRest(date);
            boolean configured = isConfiguredWorkDay(date) && !holiday;
            boolean override = !holiday && !leave && !manualRest && hasOverride(date);
            boolean autoRest = !holiday && !leave && !manualRest && !override && !configured;
            float hours = getHoursForDate(date, dailyHours, configured);
            String line = null;
            if (holiday) line = date.format(fmt) + " · 公共假日 · " + getBankHolidayName(date) + " · 0 小时";
            else if (leave) {
                String note = getLeaveNote(date);
                line = date.format(fmt) + " · 请假" + (note.isEmpty() ? "" : " · " + note) + " · 0 小时";
            } else if (manualRest) line = date.format(fmt) + " · 休息 · 手动修改 · 0 小时";
            else if (override) line = date.format(fmt) + " · 自定义工时 · " + formatDurationHours(hours);
            else if (includeAutoRest && autoRest) line = date.format(fmt) + " · 休息 · 自动规则 · 0 小时";
            else if (includeNormal && hours > 0f) line = date.format(fmt) + " · 正常上班 · " + formatDurationHours(hours);
            if (line != null) {
                TextView row = text(line, 13, holiday || leave || manualRest || override); row.setPadding(0, dp(5), 0, dp(5)); container.addView(row); count++;
            }
        }
        if (count == 0) {
            String value = includeNormal ? "这一周没有可显示的记录。" : "本月无公共假日、请假、手动休息或自定义工时记录。";
            TextView none = text(value, 13, false); none.setPadding(0, dp(8), 0, 0); container.addView(none);
        }
    }

    private void showDatePicker(boolean startPicker) {
        LocalDate initial = startPicker ? rangeStart : rangeEnd;
        LocalDate today = LocalDate.now();
        LocalDate ws = getWorkStartDate();
        if (ws != null && initial.isBefore(ws)) initial = ws;
        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            LocalDate selected = LocalDate.of(y, m + 1, d);
            if (selected.isAfter(today)) selected = today;
            if (ws != null && selected.isBefore(ws)) selected = ws;
            if (startPicker) rangeStart = selected; else rangeEnd = selected;
            updateRangeButtons();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        if (ws != null) dialog.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.show();
    }

    private void updateRangeButtons() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        rangeStartButton.setText(rangeStart.format(f)); rangeEndButton.setText(rangeEnd.format(f));
    }

    private void calculateRange() {
        rangeDetailsContainer.removeAllViews();
        LocalDate today = LocalDate.now();
        LocalDate ws = getWorkStartDate();
        LocalDate start = rangeStart;
        LocalDate end = rangeEnd.isAfter(today) ? today : rangeEnd;
        if (ws != null && start.isBefore(ws)) start = ws;
        if (start.isAfter(end)) {
            rangeSummaryText.setText("开始日期不能晚于结束日期");
            rangeDetailsContainer.addView(text("请重新选择日期范围。", 13, false));
            return;
        }
        Stats s = collectStats(start, end);
        long days = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        rangeSummaryText.setText(start + " 至 " + end + "\n总工时：" + formatDurationHours(s.totalHours)
                + "\n工作：" + s.workDays + "天 · 请假：" + s.leaveDays + "天 · 公共假日：" + s.holidayDays + "天"
                + " · 休息：" + s.restDays + "天 · 自定义：" + s.overrideDays + "天\n范围共 " + days + " 天");
        addPeriodDetails(rangeDetailsContainer, start, end, false, true);
    }

    private Stats collectStats(LocalDate start, LocalDate end) {
        Stats s = new Stats();
        if (start == null || end == null || start.isAfter(end)) return s;
        float dailyHours = getConfiguredDailyHours();
        LocalDate ws = getWorkStartDate();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (ws != null && date.isBefore(ws)) continue;
            boolean holiday = isBankHoliday(date);
            boolean leave = !holiday && isLeave(date);
            boolean manualRest = !holiday && !leave && isManualRest(date);
            boolean configured = isConfiguredWorkDay(date) && !holiday;
            boolean override = !holiday && !leave && !manualRest && hasOverride(date);
            boolean autoRest = !holiday && !leave && !manualRest && !override && !configured;
            float hours = getHoursForDate(date, dailyHours, configured);
            s.totalHours += hours;
            if (hours > 0f) s.workDays++;
            if (leave) s.leaveDays++;
            if (holiday) s.holidayDays++;
            if (manualRest || autoRest) s.restDays++;
            if (override) s.overrideDays++;
        }
        return s;
    }

    private static class Stats { float totalHours; int workDays, leaveDays, holidayDays, restDays, overrideDays; }

    private void showEditDayDialog(LocalDate date) {
        LocalDate ws = getWorkStartDate();
        if (ws != null && date.isBefore(ws)) return;
        float dailyHours = getConfiguredDailyHours();
        boolean configured = isConfiguredWorkDay(date);
        float automatic = configured ? dailyHours : 0f;
        float current = hasOverride(date) ? prefs.getFloat(overrideKey(date), automatic) : automatic;
        if (current <= 0f) current = dailyHours;

        LinearLayout box = vertical(); box.setPadding(dp(22), dp(8), dp(22), 0); box.addView(text("当天状态", 14, true));
        RadioGroup group = new RadioGroup(this);
        RadioButton normal = new RadioButton(this); normal.setText("正常上班");
        RadioButton leave = new RadioButton(this); leave.setText("请假（0 小时）");
        RadioButton rest = new RadioButton(this); rest.setText("休息（0 小时）");
        group.addView(normal); group.addView(leave); group.addView(rest); box.addView(group);
        if (isLeave(date)) leave.setChecked(true);
        else if (isManualRest(date) || (!configured && !hasOverride(date))) rest.setChecked(true);
        else normal.setChecked(true);

        TextView reasonLabel = text("请假原因 / 描述（可选）", 14, false); reasonLabel.setPadding(0, dp(8), 0, 0); box.addView(reasonLabel);
        EditText reason = new EditText(this); reason.setMinLines(2); reason.setMaxLines(4); reason.setHint("例如：看医生、年假、家庭原因"); reason.setText(getLeaveNote(date)); box.addView(reason);
        TextView hoursLabel = text("当天工时（小时）", 14, false); hoursLabel.setPadding(0, dp(10), 0, 0); box.addView(hoursLabel);
        EditText hours = new EditText(this); hours.setSingleLine(true); hours.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); hours.setText(trimNumber(current)); box.addView(hours);

        Runnable controls = () -> {
            boolean n = normal.isChecked(), l = leave.isChecked();
            hours.setEnabled(n); hoursLabel.setEnabled(n); reason.setEnabled(l); reasonLabel.setEnabled(l);
            if (!n) hours.setText("0"); else if ("0".equals(hours.getText().toString().trim())) hours.setText(trimNumber(dailyHours));
        };
        group.setOnCheckedChangeListener((g, id) -> controls.run()); controls.run();

        DateTimeFormatter titleFmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(date.format(titleFmt)).setView(box)
                .setPositiveButton("保存", null).setNeutralButton("恢复自动", null).setNegativeButton("取消", null).create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String note = reason.getText().toString().trim();
                float value = 0f; String target;
                if (normal.isChecked()) {
                    target = "正常上班";
                    try { value = Float.parseFloat(hours.getText().toString().trim()); }
                    catch (NumberFormatException e) { hours.setError("请输入正确的工时"); return; }
                    if (value < 0 || value > 24) { hours.setError("工时应在 0～24 之间"); return; }
                } else if (leave.isChecked()) target = "请假"; else target = "休息";
                final float confirmed = value;
                StringBuilder msg = new StringBuilder().append("日期：").append(date.format(titleFmt)).append("\n状态：").append(target)
                        .append("\n工时：").append(formatDurationHours(confirmed));
                if (leave.isChecked() && !note.isEmpty()) msg.append("\n原因：").append(note);
                new AlertDialog.Builder(this).setTitle("确认修改？").setMessage(msg.toString()).setNegativeButton("取消", null)
                        .setPositiveButton("确认修改", (d, which) -> {
                            SharedPreferences.Editor e = prefs.edit();
                            if (leave.isChecked()) {
                                e.putBoolean(leaveKey(date), true).remove(restKey(date)).remove(overrideKey(date));
                                if (note.isEmpty()) e.remove(leaveNoteKey(date)); else e.putString(leaveNoteKey(date), note);
                            } else if (rest.isChecked()) {
                                e.putBoolean(restKey(date), true).remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(overrideKey(date));
                            } else {
                                e.remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(restKey(date));
                                if (configured && Math.abs(confirmed - dailyHours) < 0.0001f) e.remove(overrideKey(date)); else e.putFloat(overrideKey(date), confirmed);
                            }
                            e.apply(); dialog.dismiss(); refreshAll(); Toast.makeText(this, "修改已保存", Toast.LENGTH_SHORT).show();
                        }).show();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("确认恢复自动？").setMessage("将清除当天手动状态和工时，重新按每周/每月休息规则自动计算。")
                    .setNegativeButton("取消", null).setPositiveButton("确认恢复", (d, which) -> {
                        prefs.edit().remove(overrideKey(date)).remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(restKey(date)).apply();
                        dialog.dismiss(); refreshAll(); Toast.makeText(this, "已恢复自动计算", Toast.LENGTH_SHORT).show();
                    }).show());
        });
        dialog.show();
    }

    private boolean isConfiguredWorkDay(LocalDate date) {
        int index = date.getDayOfWeek().getValue() - 1;
        boolean weeklyWorkDay = prefs.getBoolean("day_" + index, index < 5);
        return weeklyWorkDay && !isMonthlyRestDay(date);
    }

    private boolean isMonthlyRestDay(LocalDate date) {
        return getMonthlyRestDays().contains(date.getDayOfMonth());
    }

    private Set<Integer> getMonthlyRestDays() {
        Set<Integer> result = new HashSet<>();
        String raw = prefs.getString(MONTHLY_REST_DAYS_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return result;
        for (String part : raw.replace('，', ',').split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 31) result.add(value);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private float getConfiguredDailyHours() {
        LocalTime start = parseTime(prefs.getString(START_TIME_KEY, "09:00"));
        LocalTime end = parseTime(prefs.getString(END_TIME_KEY, "17:30"));
        int breakMinutes = prefs.getInt(BREAK_MINUTES_KEY, 30);
        if (start == null || end == null) return prefs.getFloat("daily_hours", 8f);
        long minutes = Duration.between(start, end).toMinutes(); if (minutes <= 0) minutes += 1440;
        return Math.max(0, minutes - breakMinutes) / 60f;
    }

    private LocalTime parseTime(String raw) {
        try { return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm")); }
        catch (DateTimeParseException | IllegalArgumentException e) { return null; }
    }

    private LocalDate getWorkStartDate() {
        String saved = prefs.getString(WORK_START_DATE_KEY, "");
        if (saved == null || saved.isEmpty()) return null;
        try { return LocalDate.parse(saved); } catch (DateTimeParseException e) { return null; }
    }

    private LocalDate mondayOf(LocalDate date) { return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }

    private float getHoursForDate(LocalDate date, float dailyHours, boolean configuredWorkDay) {
        LocalDate ws = getWorkStartDate();
        if (ws != null && date.isBefore(ws)) return 0f;
        if (isBankHoliday(date) || isLeave(date) || isManualRest(date)) return 0f;
        return prefs.getFloat(overrideKey(date), configuredWorkDay ? dailyHours : 0f);
    }

    private boolean hasOverride(LocalDate d) { return prefs.contains(overrideKey(d)); }
    private boolean isLeave(LocalDate d) { return prefs.getBoolean(leaveKey(d), false); }
    private boolean isManualRest(LocalDate d) { return prefs.getBoolean(restKey(d), false); }
    private String getLeaveNote(LocalDate d) { return prefs.getString(leaveNoteKey(d), ""); }
    private String overrideKey(LocalDate d) { return OVERRIDE_PREFIX + d; }
    private String leaveKey(LocalDate d) { return LEAVE_PREFIX + d; }
    private String leaveNoteKey(LocalDate d) { return LEAVE_NOTE_PREFIX + d; }
    private String restKey(LocalDate d) { return REST_PREFIX + d; }
    private boolean isBankHoliday(LocalDate d) { return getBankHolidayName(d) != null; }

    private String getBankHolidayName(LocalDate date) {
        int year = date.getYear();
        LocalDate newYear = observedDate(LocalDate.of(year, Month.JANUARY, 1)); if (date.equals(newYear)) return "New Year’s Day";
        LocalDate easter = easterSunday(year); if (date.equals(easter.minusDays(2))) return "Good Friday"; if (date.equals(easter.plusDays(1))) return "Easter Monday";
        LocalDate earlyMay = LocalDate.of(year, Month.MAY, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)); if (date.equals(earlyMay)) return "Early May bank holiday";
        LocalDate spring = LocalDate.of(year, Month.MAY, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); if (date.equals(spring)) return "Spring bank holiday";
        LocalDate summer = LocalDate.of(year, Month.AUGUST, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); if (date.equals(summer)) return "Summer bank holiday";
        LocalDate christmas = LocalDate.of(year, Month.DECEMBER, 25), boxing = LocalDate.of(year, Month.DECEMBER, 26);
        LocalDate oc, ob;
        if (christmas.getDayOfWeek() == DayOfWeek.SATURDAY) { oc = LocalDate.of(year, Month.DECEMBER, 27); ob = LocalDate.of(year, Month.DECEMBER, 28); }
        else if (christmas.getDayOfWeek() == DayOfWeek.SUNDAY) { oc = LocalDate.of(year, Month.DECEMBER, 27); ob = LocalDate.of(year, Month.DECEMBER, 26); }
        else { oc = christmas; ob = boxing.getDayOfWeek() == DayOfWeek.SATURDAY ? LocalDate.of(year, Month.DECEMBER, 28) : boxing; }
        if (date.equals(oc)) return "Christmas Day"; if (date.equals(ob)) return "Boxing Day"; return null;
    }

    private LocalDate observedDate(LocalDate d) { if (d.getDayOfWeek() == DayOfWeek.SATURDAY) return d.plusDays(2); if (d.getDayOfWeek() == DayOfWeek.SUNDAY) return d.plusDays(1); return d; }

    private LocalDate easterSunday(int year) {
        int a=year%19,b=year/100,c=year%100,d=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3,h=(19*a+b-d-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k)%7,m=(a+11*h+22*l)/451;
        int month=(h+l-7*m+114)/31, day=((h+l-7*m+114)%31)+1; return LocalDate.of(year,month,day);
    }

    private String formatDurationHours(float hours) {
        int mins = Math.round(hours * 60f), h = mins / 60, m = mins % 60;
        if (m == 0) return h + " 小时"; if (h == 0) return m + " 分钟"; return h + " 小时 " + m + " 分钟";
    }
    private String shortHours(float h) { int m=Math.round(h*60f); return m%60==0 ? (m/60)+"h" : String.format(Locale.US,"%.1fh",h); }
    private String trimNumber(float v) { if (Math.abs(v-Math.round(v))<0.0001f) return String.valueOf(Math.round(v)); return String.format(Locale.US,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$",""); }

    private TextView statText() { TextView v=text("",14,true); v.setGravity(Gravity.CENTER); return v; }
    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private Button button(String s) { Button b=new Button(this); b.setText(s); return b; }
    private TextView text(String s,int sp,boolean bold) { TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(0xFF202124); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v; }
    private GridLayout.LayoutParams gridParams() { GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=0; p.height=dp(58); p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); p.setMargins(dp(1),dp(1),dp(1),dp(1)); return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
