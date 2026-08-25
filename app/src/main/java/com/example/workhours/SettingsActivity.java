package com.example.workhours;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends Activity {

    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";
    private static final String BREAK_MINUTES_KEY = "break_minutes";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";
    private static final String REST_RULE_MODE_KEY = "rest_rule_mode";
    private static final String WAGE_MODE_KEY = "wage_mode";
    private static final String HOURLY_RATE_KEY = "hourly_rate";
    private static final String MONTHLY_SALARY_KEY = "monthly_salary";
    private static final String WAGE_HISTORY_KEY = "wage_history";
    private static final int REQUEST_EXPORT = 2001;
    private static final int REQUEST_IMPORT = 2002;
    private static final int BACKUP_FORMAT_VERSION = 1;

    private SharedPreferences prefs;
    private EditText startInput;
    private EditText endInput;
    private EditText breakInput;
    private EditText monthlyRestInput;
    private Button monthlyRestButton;
    private Button weeklyRestModeButton;
    private Button monthlyRestModeButton;
    private LinearLayout weeklyRestGroup;
    private LinearLayout monthlyRestGroup;
    private GridLayout monthlyRestCalendarGrid;
    private TextView monthlyRestMonthTitle;
    private java.time.YearMonth displayedRestMonth;
    private String restRuleMode = "weekly";
    private String holidayRegion = HolidayCalendar.DEFAULT_REGION;
    private Button holidayRegionButton;
    private LinearLayout holidayHistoryContainer;
    private LinearLayout alarmOptionsGroup;
    private TextView previewText;
    private Button workStartDateButton;
    private CheckBox workAlarmCheck;
    private CheckBox alarmFollowWorkTimeCheck;
    private EditText alarmTimeInput;
    private LinearLayout alarmTimeGroup;
    private EditText alarmUpdateTimeInput;
    private LocalDate workStartDate;
    private TextView currentWageText;
    private LinearLayout wageHistoryContainer;
    private Button themeSystemButton;
    private Button themeLightButton;
    private Button themeDarkButton;
    private boolean appliedDarkTheme;
    private final Button[] restDayButtons = new Button[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppThemeManager.apply(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        AppThemeManager.applySystemBars(this);
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedDarkTheme != AppThemeManager.isDark(this)) { recreate(); return; }
        if (prefs != null && prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {
            WorkAlarmManager.sync(this);
        }
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);
        UiStyle.page(scroll);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(24);
        UiStyle.navButton(this, back);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(48)));
        TextView title = text("工作时间设置", 24, true);
        top.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView info = text("设置一次后，App 会按工作时间和休息规则自动计算。", 14, false);
        info.setPadding(0, dp(18), 0, dp(18));
        root.addView(info);

        LinearLayout appearanceSection = createCollapsibleSection(root, "外观主题", false);
        TextView appearanceInfo = text("选择 App 的显示主题。跟随系统会自动使用手机当前的浅色或深色模式。", 13, false);
        appearanceInfo.setPadding(0, 0, 0, dp(8));
        appearanceSection.addView(appearanceInfo);
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        appearanceSection.addView(themeRow);
        themeSystemButton = new Button(this); themeSystemButton.setText("跟随系统");
        themeLightButton = new Button(this); themeLightButton.setText("浅色");
        themeDarkButton = new Button(this); themeDarkButton.setText("深色");
        themeSystemButton.setOnClickListener(v -> setAppTheme(AppThemeManager.SYSTEM));
        themeLightButton.setOnClickListener(v -> setAppTheme(AppThemeManager.LIGHT));
        themeDarkButton.setOnClickListener(v -> setAppTheme(AppThemeManager.DARK));
        themeRow.addView(themeSystemButton, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(46), 1f); tlp.leftMargin=dp(6); themeRow.addView(themeLightButton, tlp);
        LinearLayout.LayoutParams tdp = new LinearLayout.LayoutParams(0, dp(46), 1f); tdp.leftMargin=dp(6); themeRow.addView(themeDarkButton, tdp);
        refreshThemeButtons();

        LinearLayout basicSection = createCollapsibleSection(root, "基本工作设置", true);
        basicSection.addView(text("工作开始日期（可选）", 17, true));
        TextView startDateInfo = text("开始日期之前不会计入工时。", 13, false);
        startDateInfo.setPadding(0, dp(4), 0, dp(8));
        basicSection.addView(startDateInfo);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        basicSection.addView(dateRow);
        workStartDateButton = new Button(this);
        UiStyle.button(this, workStartDateButton, false);
        workStartDateButton.setOnClickListener(v -> chooseWorkStartDate());
        dateRow.addView(workStartDateButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button clearDate = new Button(this);
        UiStyle.button(this, clearDate, false);
        clearDate.setText("清除");
        clearDate.setOnClickListener(v -> confirmClearWorkStartDate());
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(86), dp(50));
        clearParams.leftMargin = dp(8);
        dateRow.addView(clearDate, clearParams);

        TextView timeTitle = text("每天工作时间", 17, true);
        timeTitle.setPadding(0, dp(24), 0, dp(8));
        basicSection.addView(timeTitle);
        LinearLayout startRow = settingInputRow("上班时间", dp(126));
        startInput = input("09:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        startRow.addView(startInput, compactInputParams(dp(126)));
        basicSection.addView(startRow);

        LinearLayout endRow = settingInputRow("下班时间", dp(126));
        endInput = input("17:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        endRow.addView(endInput, compactInputParams(dp(126)));
        basicSection.addView(endRow);

        LinearLayout breakRow = settingInputRow("休息时间", dp(126));
        breakInput = input("30", InputType.TYPE_CLASS_NUMBER);
        breakRow.addView(breakInput, compactInputParams(dp(126)));
        basicSection.addView(breakRow);

        LinearLayout alarmSection = createCollapsibleSection(root, "上班闹钟", false);
        TextView alarmInfo = text("开启后按本周实际工作日期同步到手机系统时钟。公共假日、请假、手动休息和每月固定休息日会从本周闹钟星期中排除；App 本身不响铃。", 13, false);
        alarmInfo.setPadding(0, 0, 0, dp(4));
        alarmSection.addView(alarmInfo);
        workAlarmCheck = new CheckBox(this);
        workAlarmCheck.setText("自动设置上班闹钟");
        workAlarmCheck.setTextSize(16);
        workAlarmCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) WorkAlarmNotification.requestPermissionIfNeeded(this);
            if (alarmOptionsGroup != null) alarmOptionsGroup.setVisibility(
                    isChecked ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        alarmSection.addView(workAlarmCheck);

        alarmOptionsGroup = new LinearLayout(this);
        alarmOptionsGroup.setOrientation(LinearLayout.VERTICAL);
        alarmSection.addView(alarmOptionsGroup);

        alarmFollowWorkTimeCheck = new CheckBox(this);
        alarmFollowWorkTimeCheck.setText("闹钟时间跟随上班时间");
        alarmFollowWorkTimeCheck.setTextSize(16);
        alarmOptionsGroup.addView(alarmFollowWorkTimeCheck);

        alarmTimeGroup = settingInputRow("自定义闹钟时间", dp(126));
        alarmTimeInput = input("07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        alarmTimeGroup.addView(alarmTimeInput, compactInputParams(dp(126)));
        alarmOptionsGroup.addView(alarmTimeGroup);

        Runnable updateAlarmTimeVisibility = () -> alarmTimeGroup.setVisibility(
                alarmFollowWorkTimeCheck.isChecked() ? android.view.View.GONE : android.view.View.VISIBLE);
        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());

        LinearLayout updateTimeRow = settingInputRow("每周同步时间", dp(126));
        alarmUpdateTimeInput = input("12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        updateTimeRow.addView(alarmUpdateTimeInput, compactInputParams(dp(126)));
        alarmOptionsGroup.addView(updateTimeRow);
        TextView updateTimeInfo = text("每周日到这个时间重新计算下一周工作日并同步系统时钟。", 13, false);
        updateTimeInfo.setPadding(0, dp(4), 0, 0);
        alarmOptionsGroup.addView(updateTimeInfo);

        LinearLayout dailyHoursRow = settingInputRow("每天工时（自动）", dp(126));
        previewText = text("", 16, true);
        previewText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        dailyHoursRow.addView(previewText, compactInputParams(dp(126)));
        alarmSection.addView(dailyHoursRow);
        TextWatcher hoursWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePreview(false); }
            @Override public void afterTextChanged(Editable s) { }
        };
        startInput.addTextChangedListener(hoursWatcher);
        endInput.addTextChangedListener(hoursWatcher);
        breakInput.addTextChangedListener(hoursWatcher);

        LinearLayout holidaySection = createCollapsibleSection(root, "公共假日", false);
        TextView holidayInfo = text("选择国家或地区后，公共假日会自动从工时、工资和上班闹钟中排除。", 13, false);
        holidayInfo.setPadding(0, 0, 0, dp(8));
        holidaySection.addView(holidayInfo);
        holidayRegionButton = new Button(this);
        UiStyle.button(this, holidayRegionButton, false);
        holidayRegionButton.setOnClickListener(v -> chooseHolidayRegion());
        holidaySection.addView(holidayRegionButton, new LinearLayout.LayoutParams(-1, dp(50)));
        TextView holidayHistoryTitle = text("公共假日历史", 14, true);
        holidayHistoryTitle.setPadding(0, dp(10), 0, dp(5));
        holidaySection.addView(holidayHistoryTitle);
        holidayHistoryContainer = new LinearLayout(this);
        holidayHistoryContainer.setOrientation(LinearLayout.VERTICAL);
        holidaySection.addView(holidayHistoryContainer);

        LinearLayout restSection = createCollapsibleSection(root, "休息规则", true);
        TextView restRuleInfo = text("每周休息日和每月固定休息日二选一，只会使用当前选中的规则。", 13, false);
        restRuleInfo.setPadding(0, 0, 0, dp(8));
        restSection.addView(restRuleInfo);

        LinearLayout restModeRow = new LinearLayout(this);
        restModeRow.setOrientation(LinearLayout.HORIZONTAL);
        restSection.addView(restModeRow);
        weeklyRestModeButton = new Button(this);
        weeklyRestModeButton.setText("每周休息日");
        weeklyRestModeButton.setOnClickListener(v -> setRestRuleMode("weekly"));
        restModeRow.addView(weeklyRestModeButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        monthlyRestModeButton = new Button(this);
        monthlyRestModeButton.setText("每月固定休息日");
        monthlyRestModeButton.setOnClickListener(v -> setRestRuleMode("monthly"));
        LinearLayout.LayoutParams mrmp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        mrmp.leftMargin = dp(8);
        restModeRow.addView(monthlyRestModeButton, mrmp);

        weeklyRestGroup = new LinearLayout(this);
        weeklyRestGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrgp = new LinearLayout.LayoutParams(-1, -2);
        wrgp.topMargin = dp(12);
        restSection.addView(weeklyRestGroup, wrgp);
        TextView weeklyInfo = text("点选固定休息日；高亮表示休息。默认周六、周日休息。", 13, false);
        weeklyInfo.setPadding(0, 0, 0, dp(8));
        weeklyRestGroup.addView(weeklyInfo);
        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        LinearLayout restRow1 = new LinearLayout(this);
        restRow1.setOrientation(LinearLayout.HORIZONTAL);
        weeklyRestGroup.addView(restRow1);
        LinearLayout restRow2 = new LinearLayout(this);
        restRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams restRow2Params = new LinearLayout.LayoutParams(-1, -2);
        restRow2Params.topMargin = dp(8);
        weeklyRestGroup.addView(restRow2, restRow2Params);
        for (int i = 0; i < 7; i++) {
            Button day = new Button(this);
            day.setText(names[i]); day.setTextSize(14); day.setAllCaps(false); day.setMinHeight(0); day.setMinWidth(0); day.setPadding(dp(6),0,dp(6),0);
            day.setOnClickListener(v -> { day.setSelected(!day.isSelected()); updateRestDayButtonStyle(day); });
            restDayButtons[i] = day;
            LinearLayout target = i < 4 ? restRow1 : restRow2;
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if ((i < 4 && i > 0) || (i >= 4 && i > 4)) dayParams.leftMargin = dp(8);
            target.addView(day, dayParams);
            updateRestDayButtonStyle(day);
        }

        monthlyRestGroup = new LinearLayout(this);
        monthlyRestGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mrgp = new LinearLayout.LayoutParams(-1, -2);
        mrgp.topMargin = dp(12);
        restSection.addView(monthlyRestGroup, mrgp);
        TextView monthlyInfo = text("点选日期作为每月固定休息日。月份用于查看日期对应星期和周末；所选日期会每月重复。", 13, false);
        monthlyInfo.setPadding(0, 0, 0, dp(8));
        monthlyRestGroup.addView(monthlyInfo);
        monthlyRestInput = input("", InputType.TYPE_CLASS_TEXT);
        monthlyRestInput.setVisibility(android.view.View.GONE);
        monthlyRestGroup.addView(monthlyRestInput);

        LinearLayout monthNav = new LinearLayout(this);
        monthNav.setOrientation(LinearLayout.HORIZONTAL); monthNav.setGravity(Gravity.CENTER_VERTICAL);
        monthlyRestGroup.addView(monthNav, new LinearLayout.LayoutParams(-1, dp(54)));
        Button prevRestMonth = new Button(this); prevRestMonth.setText("‹"); prevRestMonth.setTextSize(24); UiStyle.navButton(this, prevRestMonth);
        prevRestMonth.setOnClickListener(v -> { displayedRestMonth = displayedRestMonth.minusMonths(1); rebuildMonthlyRestCalendar(); });
        monthNav.addView(prevRestMonth, new LinearLayout.LayoutParams(dp(58), dp(48)));
        monthlyRestMonthTitle = text("", 18, true); monthlyRestMonthTitle.setGravity(Gravity.CENTER);
        monthNav.addView(monthlyRestMonthTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        Button nextRestMonth = new Button(this); nextRestMonth.setText("›"); nextRestMonth.setTextSize(24); UiStyle.navButton(this, nextRestMonth);
        nextRestMonth.setOnClickListener(v -> { displayedRestMonth = displayedRestMonth.plusMonths(1); rebuildMonthlyRestCalendar(); });
        monthNav.addView(nextRestMonth, new LinearLayout.LayoutParams(dp(58), dp(48)));

        monthlyRestCalendarGrid = new GridLayout(this);
        monthlyRestCalendarGrid.setColumnCount(7); monthlyRestCalendarGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        monthlyRestGroup.addView(monthlyRestCalendarGrid);
        monthlyRestButton = new Button(this); monthlyRestButton.setVisibility(android.view.View.GONE); monthlyRestGroup.addView(monthlyRestButton);

        LinearLayout wageSection = createCollapsibleSection(root, "工资设置", false);
        TextView wageInfo = text("工资按生效日期保存历史。修改工资不会重算成新工资；历史日期继续使用当时有效的工资。", 13, false);
        wageInfo.setPadding(0, 0, 0, dp(10));
        wageSection.addView(wageInfo);

        LinearLayout currentWageCard = new LinearLayout(this);
        currentWageCard.setOrientation(LinearLayout.VERTICAL);
        currentWageCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        currentWageCard.setBackground(UiStyle.roundRect(this, UiStyle.CARD_BG, 16, UiStyle.BORDER, 1));
        currentWageCard.addView(text("当前工资", 13, false));
        currentWageText = text("未设置", 20, true);
        currentWageText.setPadding(0, dp(4), 0, dp(2));
        currentWageCard.addView(currentWageText);
        TextView currentHint = text("按日期自动匹配最近一次已生效的工资记录", 12, false);
        currentWageCard.addView(currentHint);
        wageSection.addView(currentWageCard);

        Button addWageChange = new Button(this);
        addWageChange.setText("＋ 新增工资变更");
        UiStyle.button(this, addWageChange, true);
        LinearLayout.LayoutParams addWageParams = new LinearLayout.LayoutParams(-1, dp(50));
        addWageParams.topMargin = dp(10);
        wageSection.addView(addWageChange, addWageParams);
        addWageChange.setOnClickListener(v -> showAddWageChangeDialog());

        TextView historyTitle = text("工资历史", 15, true);
        historyTitle.setPadding(0, dp(14), 0, dp(6));
        wageSection.addView(historyTitle);
        wageHistoryContainer = new LinearLayout(this);
        wageHistoryContainer.setOrientation(LinearLayout.VERTICAL);
        wageSection.addView(wageHistoryContainer);

        Button save = new Button(this);
        UiStyle.button(this, save, true);
        save.setText("保存设置");
        save.setTextSize(16);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        saveParams.topMargin = dp(22);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> save());

        LinearLayout backupSection = createCollapsibleSection(root, "数据备份与迁移", false);
        TextView backupInfo = text("导出会保存全部设置、休息规则、请假原因、手动状态和单日上下班时间。可复制到另一台手机后导入恢复。", 13, false);
        backupInfo.setPadding(0, 0, 0, dp(8));
        backupSection.addView(backupInfo);

        Button export = new Button(this);
        UiStyle.button(this, export, false);
        export.setText("导出全部数据");
        export.setOnClickListener(v -> exportBackup());
        backupSection.addView(export, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        Button importButton = new Button(this);
        UiStyle.button(this, importButton, false);
        importButton.setText("导入 / 恢复数据");
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        importParams.topMargin = dp(8);
        backupSection.addView(importButton, importParams);
        importButton.setOnClickListener(v -> importBackup());

        TextView warning = text("导入会覆盖本机当前 App 数据；导入前建议先导出一份当前备份。", 13, true);
        warning.setPadding(0, dp(8), 0, 0);
        backupSection.addView(warning);

        return scroll;
    }

    private void confirmClearWorkStartDate() {
        if (workStartDate == null) {
            Toast.makeText(this, "当前没有设置工作开始日期", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认清除工作开始日期？")
                .setMessage("清除后，App 将恢复统计全部历史日期。现有历史记录不会被删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认清除", (dialog, which) -> {
                    workStartDate = null;
                    updateWorkStartDateButton();
                    Toast.makeText(this, "工作开始日期已清除，点击保存设置后生效", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void exportBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        intent.putExtra(Intent.EXTRA_TITLE, "WorkHoursApp-backup-" + date + ".json");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void importBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT) writeBackup(uri);
        else if (requestCode == REQUEST_IMPORT) readBackupForImport(uri);
    }

    private void writeBackup(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("无法打开目标文件");
            JSONObject backup = createBackupJson();
            out.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));
            out.flush();
            Toast.makeText(this, "数据已导出", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private JSONObject createBackupJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", "WorkHoursApp");
        root.put("formatVersion", BACKUP_FORMAT_VERSION);
        root.put("exportedAt", LocalDateTime.now().toString());

        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Object value = entry.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof String) {
                item.put("type", "string");
                item.put("value", value);
            } else if (value instanceof Boolean) {
                item.put("type", "boolean");
                item.put("value", value);
            } else if (value instanceof Integer) {
                item.put("type", "int");
                item.put("value", value);
            } else if (value instanceof Long) {
                item.put("type", "long");
                item.put("value", value);
            } else if (value instanceof Float) {
                item.put("type", "float");
                item.put("value", ((Float) value).doubleValue());
            } else if (value instanceof Set) {
                item.put("type", "stringSet");
                JSONArray array = new JSONArray();
                for (Object element : (Set<?>) value) array.put(String.valueOf(element));
                item.put("value", array);
            } else {
                continue;
            }
            values.put(entry.getKey(), item);
        }
        root.put("data", values);
        return root;
    }

    private void readBackupForImport(Uri uri) {
        try {
            String json = readText(uri);
            JSONObject backup = new JSONObject(json);
            validateBackup(backup);
            JSONObject values = backup.getJSONObject("data");
            int count = values.length();
            String exportedAt = backup.optString("exportedAt", "未知时间");

            new AlertDialog.Builder(this)
                    .setTitle("确认导入数据？")
                    .setMessage("备份时间：" + exportedAt
                            + "\n数据项目：" + count
                            + "\n\n导入后将覆盖本机当前的工作设置和历史记录。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确认覆盖并导入", (dialog, which) -> applyBackup(values))
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "无法导入：备份文件无效或已损坏\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("无法打开备份文件");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
                if (builder.length() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("备份文件过大");
                }
            }
        }
        return builder.toString();
    }

    private void validateBackup(JSONObject backup) throws JSONException {
        if (!"WorkHoursApp".equals(backup.optString("app"))) {
            throw new JSONException("不是 WorkHoursApp 备份文件");
        }
        int version = backup.optInt("formatVersion", -1);
        if (version < 1 || version > BACKUP_FORMAT_VERSION) {
            throw new JSONException("不支持的备份格式版本：" + version);
        }
        if (!backup.has("data") || !(backup.get("data") instanceof JSONObject)) {
            throw new JSONException("缺少数据内容");
        }

        JSONObject data = backup.getJSONObject("data");
        java.util.Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = data.getJSONObject(key);
            String type = item.getString("type");
            if (!item.has("value")) throw new JSONException("数据项缺少值：" + key);
            switch (type) {
                case "string": item.getString("value"); break;
                case "boolean": item.getBoolean("value"); break;
                case "int": item.getInt("value"); break;
                case "long": item.getLong("value"); break;
                case "float": item.getDouble("value"); break;
                case "stringSet": item.getJSONArray("value"); break;
                default: throw new JSONException("未知数据类型：" + type);
            }
        }
    }

    private void applyBackup(JSONObject values) {
        try {
            SharedPreferences.Editor editor = prefs.edit().clear();
            java.util.Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject item = values.getJSONObject(key);
                String type = item.getString("type");
                switch (type) {
                    case "string":
                        editor.putString(key, item.getString("value"));
                        break;
                    case "boolean":
                        editor.putBoolean(key, item.getBoolean("value"));
                        break;
                    case "int":
                        editor.putInt(key, item.getInt("value"));
                        break;
                    case "long":
                        editor.putLong(key, item.getLong("value"));
                        break;
                    case "float":
                        editor.putFloat(key, (float) item.getDouble("value"));
                        break;
                    case "stringSet":
                        JSONArray array = item.getJSONArray("value");
                        Set<String> set = new LinkedHashSet<>();
                        for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
                        editor.putStringSet(key, set);
                        break;
                }
            }
            if (!editor.commit()) throw new IllegalStateException("写入本机数据失败");
            loadSettings();
            Toast.makeText(this, "数据导入完成，可返回主页查看", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private JSONArray readHolidayHistory() {
        String raw = prefs.getString(HolidayCalendar.HISTORY_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return new JSONArray();
        try { return new JSONArray(raw); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void ensureHolidayHistoryMigrated() {
        JSONArray existing = readHolidayHistory();
        if (existing.length() > 0) return;
        String region = prefs.getString(HolidayCalendar.REGION_KEY, HolidayCalendar.DEFAULT_REGION);
        if (region == null) region = HolidayCalendar.DEFAULT_REGION;
        String start = prefs.getString(WORK_START_DATE_KEY, "");
        if (start == null || start.isEmpty()) start = "1970-01-01";
        try {
            JSONArray arr = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("effectiveDate", start);
            item.put("region", region);
            arr.put(item);
            prefs.edit().putString(HolidayCalendar.HISTORY_KEY, arr.toString()).apply();
        } catch (Exception ignored) { }
    }

    private void chooseHolidayRegion() {
        String[] regions = HolidayCalendar.regions();
        String[] labels = HolidayCalendar.labels();
        String current = HolidayCalendar.regionForDate(prefs, LocalDate.now());
        int selected = 0;
        for (int i=0;i<regions.length;i++) if (regions[i].equals(current)) { selected=i; break; }
        final int[] choice = {selected};
        new AlertDialog.Builder(this)
                .setTitle("选择新的公共假日国家 / 地区")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)
                .setNegativeButton("取消", null)
                .setPositiveButton("下一步", (dialog, which) -> chooseHolidayEffectiveDate(regions[choice[0]]))
                .show();
    }

    private void chooseHolidayEffectiveDate(String region) {
        LocalDate initial = LocalDate.now();
        DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
            LocalDate effective = LocalDate.of(y, m + 1, d);
            saveHolidayRule(effective, region);
        }, initial.getYear(), initial.getMonthValue()-1, initial.getDayOfMonth());
        LocalDate ws = getSavedWorkStartDate();
        if (ws != null) picker.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        picker.setTitle("选择生效日期");
        picker.show();
    }

    private void saveHolidayRule(LocalDate effective, String region) {
        JSONArray arr = readHolidayHistory();
        JSONArray out = new JSONArray();
        for (int i=0;i<arr.length();i++) {
            JSONObject old = arr.optJSONObject(i);
            if (old == null || effective.toString().equals(old.optString("effectiveDate", ""))) continue;
            out.put(old);
        }
        try {
            JSONObject item = new JSONObject();
            item.put("effectiveDate", effective.toString());
            item.put("region", region);
            out.put(item);
        } catch (Exception e) { return; }
        prefs.edit().putString(HolidayCalendar.HISTORY_KEY, out.toString()).putString(HolidayCalendar.REGION_KEY, region).apply();
        holidayRegion = HolidayCalendar.regionForDate(prefs, LocalDate.now());
        updateHolidayRegionButton();
        refreshHolidayHistoryUi();
        WorkAlarmManager.forceSync(this);
        Toast.makeText(this, "公共假日设置已保存，从 " + effective + " 起生效", Toast.LENGTH_LONG).show();
    }

    private void refreshHolidayHistoryUi() {
        if (holidayHistoryContainer == null) return;
        ensureHolidayHistoryMigrated();
        JSONArray arr = readHolidayHistory();
        java.util.ArrayList<JSONObject> rows = new java.util.ArrayList<>();
        for (int i=0;i<arr.length();i++) { JSONObject o=arr.optJSONObject(i); if(o!=null) rows.add(o); }
        rows.sort((a,b)->b.optString("effectiveDate","").compareTo(a.optString("effectiveDate","")));
        holidayHistoryContainer.removeAllViews();
        for (JSONObject item: rows) {
            String date=item.optString("effectiveDate","");
            String region=item.optString("region",HolidayCalendar.DEFAULT_REGION);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11),dp(8),dp(8),dp(8)); row.setBackground(UiStyle.roundRect(this,UiStyle.CARD_BG,12,UiStyle.BORDER,1));
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(HolidayCalendar.label(region),14,true));
            info.addView(text(date+" 起生效",12,false));
            row.addView(info,new LinearLayout.LayoutParams(0,-2,1f));
            Button del=new Button(this); del.setText("删除"); del.setTextSize(12); UiStyle.button(this,del,false);
            del.setOnClickListener(v->confirmDeleteHolidayRule(date));
            row.addView(del,new LinearLayout.LayoutParams(dp(70),dp(40)));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.bottomMargin=dp(6); holidayHistoryContainer.addView(row,rp);
        }
    }

    private void confirmDeleteHolidayRule(String date) {
        JSONArray arr=readHolidayHistory();
        if(arr.length()<=1){Toast.makeText(this,"至少保留一条公共假日历史",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("删除这条公共假日历史？").setMessage(date+" 起的规则将被删除，之后会使用更早一条有效规则。")
                .setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{
                    JSONArray out=new JSONArray();
                    for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null&&!date.equals(o.optString("effectiveDate","")))out.put(o);}
                    prefs.edit().putString(HolidayCalendar.HISTORY_KEY,out.toString()).apply();
                    holidayRegion=HolidayCalendar.regionForDate(prefs,LocalDate.now()); updateHolidayRegionButton(); refreshHolidayHistoryUi(); WorkAlarmManager.forceSync(this);
                }).show();
    }

    private void updateHolidayRegionButton() {
        if (holidayRegionButton != null) holidayRegionButton.setText("当前：" + HolidayCalendar.label(HolidayCalendar.regionForDate(prefs, LocalDate.now())) + "  ›");
    }

    private void setRestRuleMode(String mode) {
        restRuleMode = "monthly".equals(mode) ? "monthly" : "weekly";
        boolean monthly = "monthly".equals(restRuleMode);
        if (weeklyRestGroup != null) weeklyRestGroup.setVisibility(monthly ? android.view.View.GONE : android.view.View.VISIBLE);
        if (monthlyRestGroup != null) monthlyRestGroup.setVisibility(monthly ? android.view.View.VISIBLE : android.view.View.GONE);
        if (weeklyRestModeButton != null) { weeklyRestModeButton.setSelected(!monthly); updateRestModeButtonStyle(weeklyRestModeButton, !monthly); }
        if (monthlyRestModeButton != null) { monthlyRestModeButton.setSelected(monthly); updateRestModeButtonStyle(monthlyRestModeButton, monthly); }
        if (monthly && monthlyRestCalendarGrid != null) rebuildMonthlyRestCalendar();
    }

    private void updateRestModeButtonStyle(Button button, boolean selected) {
        button.setTextColor(selected ? android.graphics.Color.WHITE : UiStyle.TEXT);
        button.setBackground(UiStyle.roundRect(this, selected ? UiStyle.PRIMARY : UiStyle.CARD_BG, 14, selected ? UiStyle.PRIMARY : UiStyle.BORDER, 1));
    }

    private Set<Integer> selectedMonthlyRestDays() {
        Set<Integer> out = new LinkedHashSet<>();
        if (monthlyRestInput == null) return out;
        String raw = monthlyRestInput.getText().toString().trim();
        if (raw.isEmpty()) return out;
        for (String part : raw.replace('，', ',').split(",")) {
            try { int d = Integer.parseInt(part.trim()); if (d >= 1 && d <= 31) out.add(d); } catch (Exception ignored) { }
        }
        return out;
    }

    private void writeSelectedMonthlyRestDays(Set<Integer> days) {
        StringBuilder out = new StringBuilder();
        for (int d = 1; d <= 31; d++) if (days.contains(d)) { if (out.length() > 0) out.append(", "); out.append(d); }
        monthlyRestInput.setText(out.toString());
        updateMonthlyRestButton();
    }

    private void rebuildMonthlyRestCalendar() {
        if (monthlyRestCalendarGrid == null || displayedRestMonth == null) return;
        monthlyRestCalendarGrid.removeAllViews();
        monthlyRestMonthTitle.setText(displayedRestMonth.getYear() + "年" + displayedRestMonth.getMonthValue() + "月");
        String[] heads = {"一","二","三","四","五","六","日"};
        for (int i=0;i<7;i++) {
            TextView h=text(heads[i],13,true); h.setGravity(Gravity.CENTER);
            if (i>=5) h.setTextColor(0xFFB14A4A);
            GridLayout.LayoutParams hp=new GridLayout.LayoutParams(); hp.width=0; hp.height=dp(34); hp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            monthlyRestCalendarGrid.addView(h,hp);
        }
        int leading=displayedRestMonth.atDay(1).getDayOfWeek().getValue()-1;
        Set<Integer> selected=selectedMonthlyRestDays();
        int cells=((leading+displayedRestMonth.lengthOfMonth()+6)/7)*7;
        for(int i=0;i<cells;i++) {
            int day=i-leading+1;
            if(day<1||day>displayedRestMonth.lengthOfMonth()) {
                TextView blank=text("",13,false); GridLayout.LayoutParams bp=new GridLayout.LayoutParams(); bp.width=0; bp.height=dp(46); bp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); monthlyRestCalendarGrid.addView(blank,bp); continue;
            }
            java.time.LocalDate date=displayedRestMonth.atDay(day);
            boolean weekend=date.getDayOfWeek()==java.time.DayOfWeek.SATURDAY||date.getDayOfWeek()==java.time.DayOfWeek.SUNDAY;
            Button b=new Button(this); b.setText(String.valueOf(day)); b.setTextSize(13); b.setAllCaps(false); b.setMinWidth(0); b.setMinHeight(0); b.setPadding(0,0,0,0);
            boolean sel=selected.contains(day);
            b.setTextColor(sel ? android.graphics.Color.WHITE : (weekend ? 0xFFB14A4A : UiStyle.TEXT));
            b.setBackground(UiStyle.roundRect(this, sel ? UiStyle.PRIMARY : (weekend ? 0xFFFFF4F4 : UiStyle.CARD_BG), 10, sel ? UiStyle.PRIMARY : UiStyle.BORDER, 1));
            b.setOnClickListener(v -> { Set<Integer> cur=selectedMonthlyRestDays(); if(cur.contains(day))cur.remove(day);else cur.add(day); writeSelectedMonthlyRestDays(cur); rebuildMonthlyRestCalendar(); });
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams(); gp.width=0; gp.height=dp(46); gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); gp.setMargins(dp(2),dp(2),dp(2),dp(2));
            monthlyRestCalendarGrid.addView(b,gp);
        }
    }

    private void updateRestDayButtonStyle(Button button) {
        boolean rest = button.isSelected();
        button.setTextColor(rest ? android.graphics.Color.WHITE : UiStyle.TEXT);
        button.setBackground(UiStyle.roundRect(this,
                rest ? UiStyle.PRIMARY : UiStyle.CARD_BG,
                14, rest ? UiStyle.PRIMARY : UiStyle.BORDER, 1));
    }

    private void updateMonthlyRestButton() {
        if (monthlyRestButton == null || monthlyRestInput == null) return;
        String raw = monthlyRestInput.getText().toString().trim();
        monthlyRestButton.setText(raw.isEmpty() ? "未设置固定日期" : "已选：" + raw);
    }

    private void showMonthlyRestPicker() {
        final boolean[] selected = new boolean[32];
        String raw = monthlyRestInput.getText().toString().trim();
        if (!raw.isEmpty()) {
            for (String part : raw.replace('，', ',').split(",")) {
                try { int d = Integer.parseInt(part.trim()); if (d >= 1 && d <= 31) selected[d] = true; }
                catch (Exception ignored) { }
            }
        }
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        grid.setPadding(dp(12), dp(8), dp(12), dp(8));
        Button[] buttons = new Button[32];
        for (int d = 1; d <= 31; d++) {
            final int day = d;
            Button b = new Button(this);
            buttons[d] = b;
            b.setText(String.valueOf(d));
            b.setTextSize(14);
            b.setMinWidth(0); b.setMinHeight(0); b.setPadding(0,0,0,0);
            Runnable paint = () -> {
                b.setTextColor(selected[day] ? android.graphics.Color.WHITE : UiStyle.TEXT);
                b.setBackground(UiStyle.roundRect(this, selected[day] ? UiStyle.PRIMARY : UiStyle.CARD_BG,
                        12, selected[day] ? UiStyle.PRIMARY : UiStyle.BORDER, 1));
            };
            b.setOnClickListener(v -> { selected[day] = !selected[day]; paint.run(); });
            paint.run();
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0; gp.height = dp(44); gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.setMargins(dp(3), dp(3), dp(3), dp(3));
            grid.addView(b, gp);
        }
        new AlertDialog.Builder(this)
                .setTitle("每月固定休息日")
                .setView(grid)
                .setNegativeButton("取消", null)
                .setNeutralButton("清空", (d,w) -> { monthlyRestInput.setText(""); updateMonthlyRestButton(); })
                .setPositiveButton("确定", (d,w) -> {
                    StringBuilder out = new StringBuilder();
                    for (int i=1;i<=31;i++) if (selected[i]) { if (out.length()>0) out.append(", "); out.append(i); }
                    monthlyRestInput.setText(out.toString());
                    updateMonthlyRestButton();
                }).show();
    }

    private void setAppTheme(String mode) {
        if (mode.equals(AppThemeManager.mode(this))) return;
        AppThemeManager.setMode(this, mode);
        recreate();
    }

    private void refreshThemeButtons() {
        if (themeSystemButton == null) return;
        String mode = AppThemeManager.mode(this);
        paintThemeButton(themeSystemButton, AppThemeManager.SYSTEM.equals(mode));
        paintThemeButton(themeLightButton, AppThemeManager.LIGHT.equals(mode));
        paintThemeButton(themeDarkButton, AppThemeManager.DARK.equals(mode));
    }

    private void paintThemeButton(Button button, boolean selected) {
        UiStyle.button(this, button, selected);
    }

    private LinearLayout createCollapsibleSection(LinearLayout root, String title, boolean expanded) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(-1, -2);
        wrapperParams.topMargin = dp(10);
        root.addView(wrapper, wrapperParams);

        Button header = new Button(this);
        header.setAllCaps(false);
        header.setTextSize(16);
        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(14), 0);
        UiStyle.button(this, header, false);
        wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4), dp(8), dp(4), dp(6));
        wrapper.addView(content, new LinearLayout.LayoutParams(-1, -2));

        final boolean[] open = {expanded};
        Runnable paint = () -> {
            content.setVisibility(open[0] ? android.view.View.VISIBLE : android.view.View.GONE);
            header.setText((open[0] ? "▼  " : "▶  ") + title);
        };
        header.setOnClickListener(v -> { open[0] = !open[0]; paint.run(); });
        paint.run();
        return content;
    }

    private LinearLayout settingInputRow(String label, int inputWidth) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView title = text(label, 15, true);
        row.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private LinearLayout.LayoutParams compactInputParams(int width) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(48));
        params.leftMargin = dp(12);
        return params;
    }

    private EditText input(String hint, int type) {
        EditText view = new EditText(this);
        view.setSingleLine(true);
        view.setInputType(type);
        view.setTextSize(18);
        view.setHint(hint);
        UiStyle.input(this, view);
        return view;
    }

    private void loadSettings() {
        startInput.setText(prefs.getString(START_TIME_KEY, "09:00"));
        endInput.setText(prefs.getString(END_TIME_KEY, "17:30"));
        breakInput.setText(String.valueOf(prefs.getInt(BREAK_MINUTES_KEY, 30)));
        monthlyRestInput.setText(prefs.getString(MONTHLY_REST_DAYS_KEY, ""));
        restRuleMode = prefs.getString(REST_RULE_MODE_KEY, "weekly");
        ensureHolidayHistoryMigrated();
        holidayRegion = HolidayCalendar.regionForDate(prefs, LocalDate.now());
        if (!"monthly".equals(restRuleMode)) restRuleMode = "weekly";
        displayedRestMonth = java.time.YearMonth.now();
        workAlarmCheck.setChecked(prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false));
        alarmFollowWorkTimeCheck.setChecked(prefs.getBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, true));
        alarmTimeInput.setText(prefs.getString(WorkAlarmManager.ALARM_TIME_KEY, "07:30"));
        alarmUpdateTimeInput.setText(prefs.getString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, "12:00"));
        alarmTimeGroup.setVisibility(alarmFollowWorkTimeCheck.isChecked()
                ? android.view.View.GONE : android.view.View.VISIBLE);
        alarmOptionsGroup.setVisibility(workAlarmCheck.isChecked()
                ? android.view.View.VISIBLE : android.view.View.GONE);
        updateMonthlyRestButton();
        updateHolidayRegionButton();
        refreshHolidayHistoryUi();
        setRestRuleMode(restRuleMode);
        rebuildMonthlyRestCalendar();
        ensureWageHistoryMigrated();
        refreshWageHistoryUi();

        workStartDate = null;
        String savedStartDate = prefs.getString(WORK_START_DATE_KEY, "");
        if (!savedStartDate.isEmpty()) {
            try { workStartDate = LocalDate.parse(savedStartDate); }
            catch (DateTimeParseException ignored) { workStartDate = null; }
        }
        updateWorkStartDateButton();

        for (int i = 0; i < 7; i++) {
            boolean wasWorkDay = prefs.getBoolean("day_" + i, i < 5);
            restDayButtons[i].setSelected(!wasWorkDay);
            updateRestDayButtonStyle(restDayButtons[i]);
        }
        updatePreview(false);
    }

    private JSONArray readWageHistory() {
        String raw = prefs.getString(WAGE_HISTORY_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return new JSONArray();
        try { return new JSONArray(raw); }
        catch (JSONException e) { return new JSONArray(); }
    }

    private void ensureWageHistoryMigrated() {
        JSONArray existing = readWageHistory();
        if (existing.length() > 0) return;
        String mode = prefs.getString(WAGE_MODE_KEY, "hourly");
        float amount = "monthly".equals(mode)
                ? prefs.getFloat(MONTHLY_SALARY_KEY, 0f)
                : prefs.getFloat(HOURLY_RATE_KEY, 0f);
        if (amount <= 0f) return;
        String start = prefs.getString(WORK_START_DATE_KEY, "");
        if (start == null || start.isEmpty()) start = "1970-01-01";
        try {
            JSONArray arr = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("effectiveDate", start);
            item.put("mode", "monthly".equals(mode) ? "monthly" : "hourly");
            item.put("amount", amount);
            arr.put(item);
            prefs.edit().putString(WAGE_HISTORY_KEY, arr.toString()).apply();
        } catch (JSONException ignored) { }
    }

    private void refreshWageHistoryUi() {
        if (currentWageText == null || wageHistoryContainer == null) return;
        ensureWageHistoryMigrated();
        JSONArray arr = readWageHistory();
        wageHistoryContainer.removeAllViews();
        JSONObject current = findWageRule(LocalDate.now(), arr);
        if (current == null) currentWageText.setText("未设置");
        else currentWageText.setText(formatWageRule(current));

        if (arr.length() == 0) {
            TextView empty = text("暂无工资记录。点击“新增工资变更”设置第一条工资。", 13, false);
            empty.setPadding(0, dp(4), 0, dp(8));
            wageHistoryContainer.addView(empty);
            return;
        }

        java.util.ArrayList<JSONObject> rows = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) rows.add(o);
        }
        rows.sort((a, b) -> b.optString("effectiveDate", "").compareTo(a.optString("effectiveDate", "")));
        for (JSONObject item : rows) {
            String date = item.optString("effectiveDate", "");
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(8), dp(10));
            row.setBackground(UiStyle.roundRect(this, UiStyle.CARD_BG, 14, UiStyle.BORDER, 1));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            TextView amount = text(formatWageRule(item), 15, true);
            TextView dateText = text(date + " 起生效", 12, false);
            dateText.setPadding(0, dp(2), 0, 0);
            info.addView(amount);
            info.addView(dateText);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

            Button delete = new Button(this);
            delete.setText("删除");
            delete.setTextSize(13);
            UiStyle.button(this, delete, false);
            delete.setOnClickListener(v -> confirmDeleteWageRule(date));
            row.addView(delete, new LinearLayout.LayoutParams(dp(72), dp(42)));

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = dp(7);
            wageHistoryContainer.addView(row, rp);
        }
    }

    private JSONObject findWageRule(LocalDate date, JSONArray arr) {
        JSONObject best = null;
        LocalDate bestDate = null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            try {
                LocalDate d = LocalDate.parse(item.optString("effectiveDate", ""));
                if (d.isAfter(date)) continue;
                if (bestDate == null || d.isAfter(bestDate)) { bestDate = d; best = item; }
            } catch (Exception ignored) { }
        }
        return best;
    }

    private String formatWageRule(JSONObject item) {
        String mode = item.optString("mode", "hourly");
        double amount = item.optDouble("amount", 0d);
        return "monthly".equals(mode)
                ? String.format(Locale.UK, "£%.2f / 月", amount)
                : String.format(Locale.UK, "£%.2f / 小时", amount);
    }

    private void showAddWageChangeDialog() {
        final LocalDate[] effective = {LocalDate.now()};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        TextView dateLabel = text("生效日期", 14, true);
        box.addView(dateLabel);
        Button dateButton = new Button(this);
        UiStyle.button(this, dateButton, false);
        Runnable updateDate = () -> dateButton.setText(effective[0].format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        updateDate.run();
        dateButton.setOnClickListener(v -> {
            LocalDate initial = effective[0];
            DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
                effective[0] = LocalDate.of(y, m + 1, d);
                updateDate.run();
            }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
            LocalDate ws = getSavedWorkStartDate();
            if (ws != null) picker.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            picker.show();
        });
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, dp(48));
        dpv.bottomMargin = dp(12);
        box.addView(dateButton, dpv);

        TextView modeLabel = text("工资类型", 14, true);
        box.addView(modeLabel);
        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton hourly = new RadioButton(this); hourly.setText("时薪"); hourly.setChecked(true);
        RadioButton monthly = new RadioButton(this); monthly.setText("月薪");
        modes.addView(hourly, new RadioGroup.LayoutParams(0, -2, 1f));
        modes.addView(monthly, new RadioGroup.LayoutParams(0, -2, 1f));
        box.addView(modes);

        TextView amountLabel = text("金额（£）", 14, true);
        amountLabel.setPadding(0, dp(8), 0, 0);
        box.addView(amountLabel);
        EditText amount = input("例如 13.20", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView hint = text("同一生效日期只能保留一条记录；如果日期相同，新记录会替换旧记录。", 12, false);
        hint.setPadding(0, dp(8), 0, 0);
        box.addView(hint);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新增工资变更")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            float value;
            try { value = Float.parseFloat(amount.getText().toString().trim()); }
            catch (Exception e) { amount.setError("请输入正确的工资金额"); return; }
            if (value < 0f || value > 10000000f) { amount.setError("请输入正确的工资金额"); return; }
            saveWageRule(effective[0], monthly.isChecked() ? "monthly" : "hourly", value);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private LocalDate getSavedWorkStartDate() {
        String raw = prefs.getString(WORK_START_DATE_KEY, "");
        try { return raw == null || raw.isEmpty() ? null : LocalDate.parse(raw); }
        catch (Exception e) { return null; }
    }

    private void saveWageRule(LocalDate date, String mode, float amount) {
        JSONArray arr = readWageHistory();
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject old = arr.optJSONObject(i);
            if (old == null || date.toString().equals(old.optString("effectiveDate", ""))) continue;
            out.put(old);
        }
        try {
            JSONObject item = new JSONObject();
            item.put("effectiveDate", date.toString());
            item.put("mode", "monthly".equals(mode) ? "monthly" : "hourly");
            item.put("amount", amount);
            out.put(item);
        } catch (JSONException e) {
            Toast.makeText(this, "保存工资记录失败", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit().putString(WAGE_HISTORY_KEY, out.toString());
        JSONObject latest = findLatestWageRule(out);
        if (latest != null) {
            String latestMode = latest.optString("mode", "hourly");
            float latestAmount = (float) latest.optDouble("amount", 0d);
            editor.putString(WAGE_MODE_KEY, latestMode);
            if ("monthly".equals(latestMode)) editor.putFloat(MONTHLY_SALARY_KEY, latestAmount);
            else editor.putFloat(HOURLY_RATE_KEY, latestAmount);
        }
        editor.apply();
        refreshWageHistoryUi();
        Toast.makeText(this, "工资变更已保存", Toast.LENGTH_SHORT).show();
    }

    private JSONObject findLatestWageRule(JSONArray arr) {
        JSONObject best = null;
        String bestDate = "";
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            String d = item.optString("effectiveDate", "");
            if (d.compareTo(bestDate) > 0) { bestDate = d; best = item; }
        }
        return best;
    }

    private void confirmDeleteWageRule(String effectiveDate) {
        new AlertDialog.Builder(this)
                .setTitle("删除工资记录？")
                .setMessage(effectiveDate + " 起的工资记录将被删除，之后的历史工资计算会改用更早的一条记录。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    JSONArray arr = readWageHistory();
                    JSONArray out = new JSONArray();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.optJSONObject(i);
                        if (item == null || effectiveDate.equals(item.optString("effectiveDate", ""))) continue;
                        out.put(item);
                    }
                    SharedPreferences.Editor editor = prefs.edit().putString(WAGE_HISTORY_KEY, out.toString());
                    JSONObject latest = findLatestWageRule(out);
                    if (latest != null) {
                        String mode = latest.optString("mode", "hourly");
                        float value = (float) latest.optDouble("amount", 0d);
                        editor.putString(WAGE_MODE_KEY, mode);
                        if ("monthly".equals(mode)) editor.putFloat(MONTHLY_SALARY_KEY, value);
                        else editor.putFloat(HOURLY_RATE_KEY, value);
                    }
                    editor.apply();
                    refreshWageHistoryUi();
                }).show();
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
        boolean followWorkTime = alarmFollowWorkTimeCheck.isChecked();
        String alarmTime = normalizeTime(alarmTimeInput.getText().toString());
        if (workAlarmCheck.isChecked() && !followWorkTime && parseTime(alarmTime) == null) {
            alarmTimeInput.setError("请输入 HH:mm，例如 07:30");
            return;
        }
        if (parseTime(alarmTime) == null) alarmTime = "07:30";
        String alarmUpdateTime = normalizeTime(alarmUpdateTimeInput.getText().toString());
        if (workAlarmCheck.isChecked() && parseTime(alarmUpdateTime) == null) {
            alarmUpdateTimeInput.setError("请输入 HH:mm，例如 12:00");
            return;
        }
        if (parseTime(alarmUpdateTime) == null) alarmUpdateTime = "12:00";

        SharedPreferences.Editor editor = prefs.edit()
                .putString(START_TIME_KEY, start)
                .putString(END_TIME_KEY, end)
                .putInt(BREAK_MINUTES_KEY, breakMinutes)
                .putFloat("daily_hours", dailyHours)
                .putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays)
                .putString(REST_RULE_MODE_KEY, restRuleMode)
                .putString(HolidayCalendar.REGION_KEY, holidayRegion)
                .putBoolean(WorkAlarmManager.ENABLED_KEY, workAlarmCheck.isChecked())
                .putBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, followWorkTime)
                .putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime)
                .putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime);

        if (workStartDate == null) editor.remove(WORK_START_DATE_KEY);
        else editor.putString(WORK_START_DATE_KEY, workStartDate.toString());

        for (int i = 0; i < 7; i++) {
            editor.putBoolean("day_" + i, !restDayButtons[i].isSelected());
        }
        editor.apply();

        if (workAlarmCheck.isChecked()) {
            WorkAlarmNotification.requestPermissionIfNeeded(this);
            WorkAlarmUpdateScheduler.schedule(this);
            boolean syncSuccess = WorkAlarmManager.forceSync(this);
            WorkAlarmNotification.notifySyncResult(this, syncSuccess, false);
            if (syncSuccess) {
                Toast.makeText(this,
                        "设置已保存 ｜ 系统闹钟写入请求已成功发送",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "设置已保存 ｜ 系统闹钟写入失败，请确认 OxygenOS 时钟可用",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            WorkAlarmManager.cancel(this);
            WorkAlarmUpdateScheduler.cancel(this);
            Toast.makeText(this, "设置已保存 ｜ 自动闹钟已关闭", Toast.LENGTH_SHORT).show();
        }

        startInput.setText(start);
        endInput.setText(end);
        alarmTimeInput.setText(alarmTime);
        alarmUpdateTimeInput.setText(alarmUpdateTime);
        monthlyRestInput.setText(monthlyRestDays);
        updateMonthlyRestButton();
        updatePreview(false);
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
        if (value != null) previewText.setText(formatDurationHours(value));
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
            try { return String.format(Locale.US, "%02d:%02d",
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1])); }
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
        view.setTextColor(UiStyle.TEXT);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
