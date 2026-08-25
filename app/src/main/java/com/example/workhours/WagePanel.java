package com.example.workhours;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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

public class WagePanel extends LinearLayout {

    private final Activity host;

    private static final String PREFS = "work_hours_prefs";
    private static final String WAGE_MODE_KEY = "wage_mode";
    private static final String HOURLY_RATE_KEY = "hourly_rate";
    private static final String MONTHLY_SALARY_KEY = "monthly_salary";
    private static final String WAGE_DEDUCT_PREFIX = "wage_deduct_";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String REST_PREFIX = "rest_";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";
    private static final String BREAK_MINUTES_KEY = "break_minutes";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";

    private SharedPreferences prefs;
    private RadioButton hourlyMode;
    private RadioButton monthlyMode;
    private EditText hourlyRateInput;
    private EditText monthlySalaryInput;
    private LinearLayout hourlySettingsGroup;
    private LinearLayout monthlySettingsGroup;
    private TextView monthTitle;
    private TextView monthSummary;
    private LinearLayout monthDetails;
    private TextView weekTitle;
    private TextView weekSummary;
    private LinearLayout weekDetails;
    private Button nextMonthButton;
    private Button previousMonthButton;
    private Button nextWeekButton;
    private Button previousWeekButton;
    private Button selectedDateButton;
    private Button rangeStartButton;
    private Button rangeEndButton;
    private TextView daySummary;
    private LinearLayout dayDetails;
    private CheckBox deductCheck;
    private LinearLayout weekContent;
    private LinearLayout dayContent;
    private LinearLayout monthContent;
    private Button weekTabButton;
    private Button dayTabButton;
    private Button monthTabButton;

    private YearMonth displayedMonth;
    private LocalDate displayedWeekStart;
    private LocalDate selectedDate;
    private LocalDate rangeStartDate;
    private LocalDate rangeEndDate;

    public WagePanel(Activity host) {
        super(host);
        this.host = host;
        setOrientation(VERTICAL);
        prefs = host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        LocalDate today = LocalDate.now();
        displayedMonth = YearMonth.from(today);
        displayedWeekStart = mondayOf(today);
        selectedDate = today;
        rangeStartDate = mondayOf(today);
        rangeEndDate = today;
        buildUi();
        refreshAll();
    }

    public void refresh() {
        refreshAll();
    }

    public void setDisplayedMonth(YearMonth month) {
        if (month == null) return;
        displayedMonth = month;
        refreshMonth();
    }

    public float getWageForDateValue(LocalDate date) {
        return getWageForDate(date);
    }

    public float getDisplayedMonthWage() {
        YearMonth month = displayedMonth;
        LocalDate ws = getWorkStartDate();
        LocalDate start = month.atDay(1);
        if (ws != null && start.isBefore(ws)) start = ws;
        LocalDate end = month.equals(YearMonth.now()) ? LocalDate.now() : month.atEndOfMonth();
        if (start.isAfter(end)) return 0f;
        float wage = 0f;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) wage += getWageForDate(d);
        return wage;
    }

    private void buildUi() {
        LinearLayout root = this;
        root.setPadding(0, dp(8), 0, dp(28));

        TextView statsTitle = text("工资详情", 19, true);
        statsTitle.setPadding(0, dp(14), 0, dp(4));
        root.addView(statsTitle);
        TextView statsInfo = text("工资规则请在设置页面修改。", 13, false);
        statsInfo.setPadding(0, 0, 0, dp(4));
        root.addView(statsInfo);

        LinearLayout viewTabs = horizontal();
        LinearLayout.LayoutParams viewTabsParams = new LinearLayout.LayoutParams(-1, dp(46));
        viewTabsParams.topMargin = dp(18);
        root.addView(viewTabs, viewTabsParams);
        weekTabButton = button("星期");
        dayTabButton = button("日期");
        monthTabButton = button("月");
        viewTabs.addView(weekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams middleTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        middleTabParams.leftMargin = dp(6);
        viewTabs.addView(dayTabButton, middleTabParams);
        LinearLayout.LayoutParams lastTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lastTabParams.leftMargin = dp(6);
        viewTabs.addView(monthTabButton, lastTabParams);

        weekContent = vertical();
        dayContent = vertical();
        monthContent = vertical();
        root.addView(weekContent);
        root.addView(dayContent);
        root.addView(monthContent);

        TextView weekSection = text("按星期查看工资", 19, true);
        weekSection.setPadding(0, dp(18), 0, dp(8));
        weekContent.addView(weekSection);
        LinearLayout weekNav = horizontal();
        weekNav.setGravity(Gravity.CENTER_VERTICAL);
        weekContent.addView(weekNav);
        previousWeekButton = button("‹");
        previousWeekButton.setTextSize(24);
        previousWeekButton.setOnClickListener(v -> { displayedWeekStart = displayedWeekStart.minusWeeks(1); refreshWeek(); });
        weekNav.addView(previousWeekButton, new LinearLayout.LayoutParams(dp(58), dp(48)));
        weekTitle = text("", 17, true);
        weekTitle.setGravity(Gravity.CENTER);
        weekTitle.setPadding(dp(8), dp(12), dp(8), dp(12));
        weekTitle.setOnClickListener(v -> chooseWeek());
        weekNav.addView(weekTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        nextWeekButton = button("›");
        nextWeekButton.setTextSize(24);
        nextWeekButton.setOnClickListener(v -> {
            LocalDate current = mondayOf(LocalDate.now());
            if (displayedWeekStart.isBefore(current)) displayedWeekStart = displayedWeekStart.plusWeeks(1);
            refreshWeek();
        });
        weekNav.addView(nextWeekButton, new LinearLayout.LayoutParams(dp(58), dp(48)));
        LinearLayout weekCard = card();
        weekContent.addView(weekCard);
        weekSummary = text("", 16, true);
        weekCard.addView(weekSummary);
        weekDetails = vertical();
        weekCard.addView(weekDetails);

        TextView daySection = text("按日期范围查看工资", 19, true);
        daySection.setPadding(0, dp(18), 0, dp(8));
        dayContent.addView(daySection);
        LinearLayout rangeRow = horizontal();
        rangeRow.setGravity(Gravity.CENTER_VERTICAL);
        dayContent.addView(rangeRow);
        rangeStartButton = button("");
        rangeStartButton.setOnClickListener(v -> chooseRangeDate(true));
        rangeRow.addView(rangeStartButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView rangeTo = text(" 至 ", 15, true);
        rangeTo.setGravity(Gravity.CENTER);
        rangeRow.addView(rangeTo, new LinearLayout.LayoutParams(dp(42), dp(50)));
        rangeEndButton = button("");
        rangeEndButton.setOnClickListener(v -> chooseRangeDate(false));
        rangeRow.addView(rangeEndButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout dayCard = card();
        LinearLayout.LayoutParams dayCardParams = new LinearLayout.LayoutParams(-1, -2);
        dayCardParams.topMargin = dp(10);
        dayContent.addView(dayCard, dayCardParams);
        daySummary = text("", 16, true);
        dayCard.addView(daySummary);
        dayDetails = vertical();
        dayCard.addView(dayDetails);
        TextView dayHint = text("扣工资请回到工时统计后点击对应日期修改。", 13, false);
        dayHint.setPadding(0, dp(10), 0, 0);
        dayCard.addView(dayHint);

        TextView monthSection = text("按月查看工资详情", 19, true);
        monthSection.setPadding(0, dp(18), 0, dp(8));
        monthContent.addView(monthSection);
        TextView monthHint = text("使用上方月份选择器切换月份。", 13, false);
        monthHint.setPadding(0, 0, 0, dp(8));
        monthContent.addView(monthHint);
        LinearLayout monthCard = card();
        monthContent.addView(monthCard);
        monthSummary = text("", 16, true);
        monthCard.addView(monthSummary);
        monthDetails = vertical();
        monthCard.addView(monthDetails);

        weekTabButton.setOnClickListener(v -> showViewTab(0));
        dayTabButton.setOnClickListener(v -> showViewTab(1));
        monthTabButton.setOnClickListener(v -> showViewTab(2));
        showViewTab(0);

    }

    private void chooseWeek() {
        LocalDate initial = displayedWeekStart;
        DatePickerDialog dialog = new DatePickerDialog(host, (view, y, m, d) -> {
            LocalDate picked = LocalDate.of(y, m + 1, d);
            displayedWeekStart = mondayOf(picked);
            refreshWeek();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        LocalDate ws = getWorkStartDate();
        if (ws != null) dialog.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.show();
    }

    private void chooseMonth() {
        LocalDate ws = getWorkStartDate();
        int minYear = ws == null ? Math.max(2000, displayedMonth.getYear() - 20) : ws.getYear();
        int maxYear = YearMonth.now().getYear();

        LinearLayout box = horizontal();
        box.setPadding(dp(24), dp(12), dp(24), dp(4));
        box.setGravity(Gravity.CENTER);

        NumberPicker yearPicker = new NumberPicker(host);
        yearPicker.setMinValue(minYear);
        yearPicker.setMaxValue(maxYear);
        yearPicker.setValue(Math.max(minYear, Math.min(maxYear, displayedMonth.getYear())));
        box.addView(yearPicker, new LinearLayout.LayoutParams(0, -2, 1f));

        NumberPicker monthPicker = new NumberPicker(host);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(new String[]{"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"});
        monthPicker.setValue(displayedMonth.getMonthValue());
        box.addView(monthPicker, new LinearLayout.LayoutParams(0, -2, 1f));

        AlertDialog dialog = new AlertDialog.Builder(host)
                .setTitle("选择月份")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            YearMonth picked = YearMonth.of(yearPicker.getValue(), monthPicker.getValue());
            YearMonth now = YearMonth.now();
            YearMonth first = ws == null ? null : YearMonth.from(ws);
            if (picked.isAfter(now)) {
                Toast.makeText(host, "不能选择未来月份", Toast.LENGTH_SHORT).show();
                return;
            }
            if (first != null && picked.isBefore(first)) {
                Toast.makeText(host, "不能早于工作开始月份", Toast.LENGTH_SHORT).show();
                return;
            }
            displayedMonth = picked;
            refreshMonth();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void chooseRangeDate(boolean startDate) {
        LocalDate initial = startDate ? rangeStartDate : rangeEndDate;
        DatePickerDialog dialog = new DatePickerDialog(host, (view, y, m, d) -> {
            LocalDate picked = LocalDate.of(y, m + 1, d);
            if (startDate) {
                rangeStartDate = picked;
                if (rangeEndDate.isBefore(rangeStartDate)) rangeEndDate = rangeStartDate;
            } else {
                rangeEndDate = picked;
                if (rangeStartDate.isAfter(rangeEndDate)) rangeStartDate = rangeEndDate;
            }
            refreshDay();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        LocalDate ws = getWorkStartDate();
        if (ws != null) dialog.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.show();
    }

    private void saveDeduction() {
        SharedPreferences.Editor e = prefs.edit();
        if (deductCheck.isChecked()) e.putBoolean(deductKey(selectedDate), true);
        else e.remove(deductKey(selectedDate));
        e.apply();
        Toast.makeText(host, "扣工资设置已保存", Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    private void refreshAll() {
        LocalDate ws = getWorkStartDate();
        if (ws != null && selectedDate.isBefore(ws)) selectedDate = ws;
        if (ws != null && rangeStartDate.isBefore(ws)) rangeStartDate = ws;
        if (rangeEndDate.isBefore(rangeStartDate)) rangeEndDate = rangeStartDate;
        refreshWeek();
        refreshDay();
        refreshMonth();
    }

    private void showViewTab(int tab) {
        if (weekContent == null || dayContent == null || monthContent == null) return;
        weekContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        dayContent.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        monthContent.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        UiStyle.button(host, weekTabButton, tab == 0);
        UiStyle.button(host, dayTabButton, tab == 1);
        UiStyle.button(host, monthTabButton, tab == 2);
        weekTabButton.setEnabled(tab != 0);
        dayTabButton.setEnabled(tab != 1);
        monthTabButton.setEnabled(tab != 2);
        if (tab == 0) refreshWeek();
        else if (tab == 1) refreshDay();
        else refreshMonth();
    }

    private void refreshDay() {
        if (rangeStartButton == null || rangeEndButton == null || daySummary == null || dayDetails == null) return;
        LocalDate today = LocalDate.now();
        LocalDate ws = getWorkStartDate();
        if (rangeEndDate.isAfter(today)) rangeEndDate = today;
        if (ws != null && rangeStartDate.isBefore(ws)) rangeStartDate = ws;
        if (rangeStartDate.isAfter(rangeEndDate)) rangeEndDate = rangeStartDate;

        DateTimeFormatter buttonFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        rangeStartButton.setText("开始\n" + rangeStartDate.format(buttonFormat));
        rangeEndButton.setText("结束\n" + rangeEndDate.format(buttonFormat));
        dayDetails.removeAllViews();

        float hours = 0f, wage = 0f, deduction = 0f;
        int workDays = 0, deductionDays = 0;
        DateTimeFormatter rowFormat = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);
        for (LocalDate d = rangeStartDate; !d.isAfter(rangeEndDate); d = d.plusDays(1)) {
            float h = getHours(d);
            float before = getWageBeforeDeduction(d);
            float after = getWageForDate(d);
            hours += h;
            wage += after;
            if (h > 0) workDays++;
            if (isDeducted(d)) { deduction += before; deductionDays++; }
            if (h > 0 || before > 0 || isDeducted(d) || isLeave(d) || isBankHoliday(d) || isManualRest(d)) {
                TextView row = text(d.format(rowFormat) + " ｜ " + getDayStatus(d)
                        + " ｜ " + formatHours(h) + " ｜ " + money(after)
                        + (isDeducted(d) ? " ｜ 已扣工资" : ""), 13, isDeducted(d));
                row.setPadding(0, dp(4), 0, dp(4));
                dayDetails.addView(row);
            }
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(rangeStartDate, rangeEndDate) + 1;
        daySummary.setText("日期范围：" + rangeStartDate.format(buttonFormat) + " 至 " + rangeEndDate.format(buttonFormat)
                + "（" + days + "天）"
                + "\n总工时：" + formatHours(hours) + " ｜ 工作：" + workDays + "天"
                + "\n总工资：" + money(wage) + " ｜ 已扣：" + money(deduction)
                + " ｜ 扣工资：" + deductionDays + "天");
    }

    private void refreshWeek() {
        LocalDate today = LocalDate.now();
        LocalDate currentWeek = mondayOf(today);
        if (displayedWeekStart.isAfter(currentWeek)) displayedWeekStart = currentWeek;
        LocalDate ws = getWorkStartDate();
        LocalDate firstWeek = ws == null ? null : mondayOf(ws);
        if (firstWeek != null && displayedWeekStart.isBefore(firstWeek)) displayedWeekStart = firstWeek;
        previousWeekButton.setEnabled(firstWeek == null || displayedWeekStart.isAfter(firstWeek));
        nextWeekButton.setEnabled(displayedWeekStart.isBefore(currentWeek));
        LocalDate end = displayedWeekStart.plusDays(6);
        LocalDate effectiveStart = displayedWeekStart;
        if (ws != null && effectiveStart.isBefore(ws)) effectiveStart = ws;
        LocalDate effectiveEnd = end.isAfter(today) ? today : end;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M月d日");
        weekTitle.setText(displayedWeekStart.format(f) + " - " + end.format(f) + "\n点击选择星期");
        weekDetails.removeAllViews();
        if (effectiveStart.isAfter(effectiveEnd)) {
            weekSummary.setText("本周尚未开始工作");
            return;
        }
        float wage = 0f, deduction = 0f, hours = 0f;
        int deductionDays = 0;
        DateTimeFormatter df = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);
        for (LocalDate d = effectiveStart; !d.isAfter(effectiveEnd); d = d.plusDays(1)) {
            float h = getHours(d);
            float before = getWageBeforeDeduction(d);
            float after = getWageForDate(d);
            hours += h;
            wage += after;
            if (isDeducted(d)) { deduction += before; deductionDays++; }
            TextView row = text(d.format(df) + " ｜ " + getDayStatus(d)
                    + " ｜ " + formatHours(h) + " ｜ " + money(after)
                    + (isDeducted(d) ? " ｜ 已扣工资" : ""), 13, isDeducted(d));
            row.setPadding(0, dp(4), 0, dp(4));
            weekDetails.addView(row);
        }
        weekSummary.setText("本周工时：" + formatHours(hours)
                + "\n本周工资：" + money(wage)
                + " ｜ 扣工资：" + money(deduction)
                + " ｜ 扣工资天数：" + deductionDays + "天");
    }

    private void refreshMonth() {
        if (monthSummary == null || monthDetails == null) return;
        YearMonth now = YearMonth.now();
        if (displayedMonth.isAfter(now)) displayedMonth = now;
        LocalDate ws = getWorkStartDate();
        YearMonth first = ws == null ? null : YearMonth.from(ws);
        if (first != null && displayedMonth.isBefore(first)) displayedMonth = first;
        LocalDate start = displayedMonth.atDay(1);
        if (ws != null && start.isBefore(ws)) start = ws;
        LocalDate end = displayedMonth.equals(now) ? LocalDate.now() : displayedMonth.atEndOfMonth();
        monthDetails.removeAllViews();
        if (start.isAfter(end)) {
            monthSummary.setText("本月尚未开始工作");
            return;
        }
        float wage = 0f, deduction = 0f, hours = 0f;
        int workDays = 0, deductionDays = 0;
        DateTimeFormatter df = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            float h = getHours(d);
            float before = getWageBeforeDeduction(d);
            float after = getWageForDate(d);
            hours += h;
            wage += after;
            if (h > 0) workDays++;
            if (isDeducted(d)) { deduction += before; deductionDays++; }
            if (h > 0 || before > 0 || isDeducted(d) || isLeave(d) || isBankHoliday(d)) {
                TextView row = text(d.format(df) + " ｜ " + getDayStatus(d)
                        + " ｜ " + formatHours(h) + " ｜ " + money(after)
                        + (isDeducted(d) ? " ｜ 已扣工资" : ""), 13, isDeducted(d));
                row.setPadding(0, dp(4), 0, dp(4));
                monthDetails.addView(row);
            }
        }
        String modeText = isMonthlyMode() ? "月薪模式" : "时薪模式";
        monthSummary.setText(modeText
                + "\n本月工时：" + formatHours(hours) + " ｜ 工作：" + workDays + "天"
                + "\n本月工资：" + money(wage)
                + " ｜ 已扣：" + money(deduction)
                + " ｜ 扣工资：" + deductionDays + "天");
    }

    private float getWageForDate(LocalDate date) {
        if (isDeducted(date)) return 0f;
        return getWageBeforeDeduction(date);
    }

    private float getWageBeforeDeduction(LocalDate date) {
        LocalDate ws = getWorkStartDate();
        if (date.isAfter(LocalDate.now()) || (ws != null && date.isBefore(ws))) return 0f;
        if (!isMonthlyMode()) return getHours(date) * prefs.getFloat(HOURLY_RATE_KEY, 0f);
        if (!isPlannedPaidDay(date)) return 0f;
        float monthly = prefs.getFloat(MONTHLY_SALARY_KEY, 0f);
        int planned = getPlannedPaidDays(YearMonth.from(date));
        return planned <= 0 ? 0f : monthly / planned;
    }

    private boolean isMonthlyMode() {
        return "monthly".equals(prefs.getString(WAGE_MODE_KEY, "hourly"));
    }

    private int getPlannedPaidDays(YearMonth month) {
        int count = 0;
        LocalDate ws = getWorkStartDate();
        for (LocalDate d = month.atDay(1); !d.isAfter(month.atEndOfMonth()); d = d.plusDays(1)) {
            if (ws != null && d.isBefore(ws)) continue;
            if (isPlannedPaidDay(d)) count++;
        }
        return count;
    }

    private boolean isPlannedPaidDay(LocalDate date) {
        int index = date.getDayOfWeek().getValue() - 1;
        boolean weekly = prefs.getBoolean("day_" + index, index < 5);
        return weekly && !isMonthlyRestDay(date);
    }

    private float getHours(LocalDate date) {
        if (date.isAfter(LocalDate.now())) return 0f;
        LocalDate ws = getWorkStartDate();
        if (ws != null && date.isBefore(ws)) return 0f;
        if (isBankHoliday(date) || isLeave(date) || isManualRest(date)) return 0f;
        boolean configured = isPlannedPaidDay(date);
        return prefs.getFloat(OVERRIDE_PREFIX + date, configured ? getConfiguredDailyHours() : 0f);
    }

    private String getDayStatus(LocalDate d) {
        if (isBankHoliday(d)) return "公共假日";
        if (isLeave(d)) return "请假";
        if (isManualRest(d)) return "休息";
        if (prefs.contains(OVERRIDE_PREFIX + d) && getHours(d) > 0) return "自定义上班";
        if (getHours(d) > 0) return "正常上班";
        return "休息";
    }

    private boolean isDeducted(LocalDate d) { return prefs.getBoolean(deductKey(d), false); }
    private String deductKey(LocalDate d) { return WAGE_DEDUCT_PREFIX + d; }
    private boolean isLeave(LocalDate d) { return prefs.getBoolean(LEAVE_PREFIX + d, false); }
    private boolean isManualRest(LocalDate d) { return prefs.getBoolean(REST_PREFIX + d, false); }

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
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) minutes += 1440;
        return Math.max(0, minutes - breakMinutes) / 60f;
    }

    private LocalTime parseTime(String raw) {
        try { return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm")); }
        catch (DateTimeParseException | IllegalArgumentException e) { return null; }
    }

    private LocalDate getWorkStartDate() {
        String value = prefs.getString(WORK_START_DATE_KEY, "");
        if (value == null || value.isEmpty()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { return null; }
    }

    private LocalDate mondayOf(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean isBankHoliday(LocalDate d) { return getBankHolidayName(d) != null; }

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
        LocalDate oc, ob;
        if (christmas.getDayOfWeek() == DayOfWeek.SATURDAY) {
            oc = LocalDate.of(year, Month.DECEMBER, 27); ob = LocalDate.of(year, Month.DECEMBER, 28);
        } else if (christmas.getDayOfWeek() == DayOfWeek.SUNDAY) {
            oc = LocalDate.of(year, Month.DECEMBER, 27); ob = LocalDate.of(year, Month.DECEMBER, 26);
        } else {
            oc = christmas;
            ob = boxing.getDayOfWeek() == DayOfWeek.SATURDAY ? LocalDate.of(year, Month.DECEMBER, 28) : boxing;
        }
        if (date.equals(oc)) return "Christmas Day";
        if (date.equals(ob)) return "Boxing Day";
        return null;
    }

    private LocalDate observedDate(LocalDate d) {
        if (d.getDayOfWeek() == DayOfWeek.SATURDAY) return d.plusDays(2);
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) return d.plusDays(1);
        return d;
    }

    private LocalDate easterSunday(int year) {
        int a=year%19,b=year/100,c=year%100,d=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3;
        int h=(19*a+b-d-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k)%7,m=(a+11*h+22*l)/451;
        int month=(h+l-7*m+114)/31, day=((h+l-7*m+114)%31)+1;
        return LocalDate.of(year,month,day);
    }


    private EditText decimalInput(String hint) {
        EditText e = new EditText(host);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setTextSize(18);
        e.setHint(hint);
        UiStyle.input(host, e);
        return e;
    }

    private LinearLayout card() {
        LinearLayout l = vertical();
        l.setPadding(dp(16), dp(15), dp(16), dp(15));
        UiStyle.card(host, l);
        return l;
    }

    private String money(float value) { return String.format(Locale.UK, "£%.2f", value); }
    private String trimMoney(float value) { return value == 0f ? "" : String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private String formatHours(float h) {
        int mins = Math.round(h * 60f), hours = mins / 60, m = mins % 60;
        if (m == 0) return hours + "小时";
        if (hours == 0) return m + "分钟";
        return hours + "小时" + m + "分钟";
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(host); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(host); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private Button button(String s) { Button b = new Button(host); b.setText(s); UiStyle.button(host,b,false); return b; }
    private TextView text(String s, int sp, boolean bold) {
        TextView v = new TextView(host); v.setText(s); v.setTextSize(sp); v.setTextColor(UiStyle.TEXT);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
