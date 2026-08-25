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
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private static final int REQUEST_EXPORT = 2001;
    private static final int REQUEST_IMPORT = 2002;
    private static final int BACKUP_FORMAT_VERSION = 1;

    private SharedPreferences prefs;
    private EditText startInput;
    private EditText endInput;
    private EditText breakInput;
    private EditText monthlyRestInput;
    private TextView previewText;
    private Button workStartDateButton;
    private CheckBox workAlarmCheck;
    private CheckBox alarmFollowWorkTimeCheck;
    private EditText alarmTimeInput;
    private LinearLayout alarmTimeGroup;
    private EditText alarmUpdateTimeInput;
    private LocalDate workStartDate;
    private final CheckBox[] restDayChecks = new CheckBox[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        root.addView(text("工作开始日期（可选）", 17, true));
        TextView startDateInfo = text("开始日期之前不会计入工时。", 13, false);
        startDateInfo.setPadding(0, dp(4), 0, dp(8));
        root.addView(startDateInfo);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(dateRow);
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

        TextView alarmTitle = text("上班闹钟", 17, true);
        alarmTitle.setPadding(0, dp(22), 0, dp(4));
        root.addView(alarmTitle);
        TextView alarmInfo = text("开启后按本周实际工作日期同步到手机系统时钟。公共假日、请假、手动休息和每月固定休息日会从本周闹钟星期中排除；App 本身不响铃。", 13, false);
        alarmInfo.setPadding(0, 0, 0, dp(4));
        root.addView(alarmInfo);
        workAlarmCheck = new CheckBox(this);
        workAlarmCheck.setText("自动设置上班闹钟");
        workAlarmCheck.setTextSize(16);
        root.addView(workAlarmCheck);

        alarmFollowWorkTimeCheck = new CheckBox(this);
        alarmFollowWorkTimeCheck.setText("闹钟时间跟随上班时间");
        alarmFollowWorkTimeCheck.setTextSize(16);
        root.addView(alarmFollowWorkTimeCheck);

        alarmTimeGroup = new LinearLayout(this);
        alarmTimeGroup.setOrientation(LinearLayout.VERTICAL);
        TextView alarmTimeLabel = text("自定义闹钟时间（HH:mm）", 15, true);
        alarmTimeLabel.setPadding(0, dp(4), 0, 0);
        alarmTimeGroup.addView(alarmTimeLabel);
        alarmTimeInput = input("例如：07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        alarmTimeGroup.addView(alarmTimeInput);
        root.addView(alarmTimeGroup);

        Runnable updateAlarmTimeVisibility = () -> alarmTimeGroup.setVisibility(
                alarmFollowWorkTimeCheck.isChecked() ? android.view.View.GONE : android.view.View.VISIBLE);
        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());

        TextView updateTimeLabel = text("每周日自动更新闹钟时间（HH:mm）", 15, true);
        updateTimeLabel.setPadding(0, dp(12), 0, 0);
        root.addView(updateTimeLabel);
        alarmUpdateTimeInput = input("例如：12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(alarmUpdateTimeInput);
        TextView updateTimeInfo = text("每周日到这个时间自动重新计算下一周工作日并同步系统时钟。系统省电策略可能让后台执行稍有延迟。", 13, false);
        updateTimeInfo.setPadding(0, dp(4), 0, 0);
        root.addView(updateTimeInfo);

        previewText = text("", 15, true);
        previewText.setPadding(0, dp(12), 0, dp(8));
        root.addView(previewText);
        Button preview = new Button(this);
        UiStyle.button(this, preview, false);
        preview.setText("计算每天工时");
        preview.setOnClickListener(v -> updatePreview(true));
        root.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

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
        UiStyle.button(this, save, true);
        save.setText("保存设置");
        save.setTextSize(16);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        saveParams.topMargin = dp(22);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> save());

        TextView backupTitle = text("数据备份与迁移", 17, true);
        backupTitle.setPadding(0, dp(28), 0, dp(4));
        root.addView(backupTitle);
        TextView backupInfo = text("导出会保存全部设置、休息规则、请假原因、手动状态和单日上下班时间。可复制到另一台手机后导入恢复。", 13, false);
        backupInfo.setPadding(0, 0, 0, dp(8));
        root.addView(backupInfo);

        Button export = new Button(this);
        UiStyle.button(this, export, false);
        export.setText("导出全部数据");
        export.setOnClickListener(v -> exportBackup());
        root.addView(export, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        Button importButton = new Button(this);
        UiStyle.button(this, importButton, false);
        importButton.setText("导入 / 恢复数据");
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        importParams.topMargin = dp(8);
        root.addView(importButton, importParams);
        importButton.setOnClickListener(v -> importBackup());

        TextView warning = text("导入会覆盖本机当前 App 数据；导入前建议先导出一份当前备份。", 13, true);
        warning.setPadding(0, dp(8), 0, 0);
        root.addView(warning);

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
        workAlarmCheck.setChecked(prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false));
        alarmFollowWorkTimeCheck.setChecked(prefs.getBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, true));
        alarmTimeInput.setText(prefs.getString(WorkAlarmManager.ALARM_TIME_KEY, "07:30"));
        alarmUpdateTimeInput.setText(prefs.getString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, "12:00"));
        alarmTimeGroup.setVisibility(alarmFollowWorkTimeCheck.isChecked()
                ? android.view.View.GONE : android.view.View.VISIBLE);

        workStartDate = null;
        String savedStartDate = prefs.getString(WORK_START_DATE_KEY, "");
        if (!savedStartDate.isEmpty()) {
            try { workStartDate = LocalDate.parse(savedStartDate); }
            catch (DateTimeParseException ignored) { workStartDate = null; }
        }
        updateWorkStartDateButton();

        for (int i = 0; i < 7; i++) {
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
                .putBoolean(WorkAlarmManager.ENABLED_KEY, workAlarmCheck.isChecked())
                .putBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, followWorkTime)
                .putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime)
                .putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime);

        if (workStartDate == null) editor.remove(WORK_START_DATE_KEY);
        else editor.putString(WORK_START_DATE_KEY, workStartDate.toString());

        for (int i = 0; i < 7; i++) {
            editor.putBoolean("day_" + i, !restDayChecks[i].isChecked());
        }
        editor.apply();

        if (workAlarmCheck.isChecked()) {
            WorkAlarmUpdateScheduler.schedule(this);
            if (WorkAlarmManager.forceSync(this)) {
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
