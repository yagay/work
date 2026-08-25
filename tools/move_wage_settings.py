from pathlib import Path

# SettingsActivity: add wage settings to main settings page.
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('import android.widget.LinearLayout;\n','import android.widget.LinearLayout;\nimport android.widget.RadioButton;\nimport android.widget.RadioGroup;\n',1)
s=s.replace('''    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";''','''    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";\n    private static final String WAGE_MODE_KEY = "wage_mode";\n    private static final String HOURLY_RATE_KEY = "hourly_rate";\n    private static final String MONTHLY_SALARY_KEY = "monthly_salary";''',1)
s=s.replace('''    private LocalDate workStartDate;\n    private final Button[] restDayButtons = new Button[7];''','''    private LocalDate workStartDate;\n    private RadioButton hourlyWageMode;\n    private RadioButton monthlyWageMode;\n    private EditText hourlyRateInput;\n    private EditText monthlySalaryInput;\n    private LinearLayout hourlyWageGroup;\n    private LinearLayout monthlyWageGroup;\n    private final Button[] restDayButtons = new Button[7];''',1)

anchor='''        Button save = new Button(this);\n'''
wage_ui='''        TextView wageTitle = text("工资设置", 17, true);\n        wageTitle.setPadding(0, dp(26), 0, dp(6));\n        root.addView(wageTitle);\n        TextView wageInfo = text("设置工资计算方式。工资统计页面只负责查看，不再修改工资规则。", 13, false);\n        wageInfo.setPadding(0, 0, 0, dp(8));\n        root.addView(wageInfo);\n\n        RadioGroup wageModes = new RadioGroup(this);\n        wageModes.setOrientation(RadioGroup.HORIZONTAL);\n        hourlyWageMode = new RadioButton(this);\n        hourlyWageMode.setText("按小时工资");\n        monthlyWageMode = new RadioButton(this);\n        monthlyWageMode.setText("按月固定工资");\n        wageModes.addView(hourlyWageMode, new RadioGroup.LayoutParams(0, -2, 1f));\n        wageModes.addView(monthlyWageMode, new RadioGroup.LayoutParams(0, -2, 1f));\n        root.addView(wageModes);\n\n        hourlyWageGroup = new LinearLayout(this);\n        hourlyWageGroup.setOrientation(LinearLayout.VERTICAL);\n        LinearLayout hourlyRow = settingInputRow("每小时工资（£）", dp(126));\n        hourlyRateInput = input("12.50", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);\n        hourlyRow.addView(hourlyRateInput, compactInputParams(dp(126)));\n        hourlyWageGroup.addView(hourlyRow);\n        root.addView(hourlyWageGroup);\n\n        monthlyWageGroup = new LinearLayout(this);\n        monthlyWageGroup.setOrientation(LinearLayout.VERTICAL);\n        LinearLayout monthlySalaryRow = settingInputRow("每月固定工资（£）", dp(126));\n        monthlySalaryInput = input("2200", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);\n        monthlySalaryRow.addView(monthlySalaryInput, compactInputParams(dp(126)));\n        monthlyWageGroup.addView(monthlySalaryRow);\n        TextView monthlyRule = text("月薪按当月计划上班日平均分摊。公共假日和请假默认保留工资；只有明确标记“扣工资”的日期才扣除当天份额。", 13, false);\n        monthlyRule.setPadding(0, dp(4), 0, dp(4));\n        monthlyWageGroup.addView(monthlyRule);\n        root.addView(monthlyWageGroup);\n\n        wageModes.setOnCheckedChangeListener((group, checkedId) -> updateWageModeVisibility());\n\n'''
if anchor not in s: raise SystemExit('settings save anchor missing')
s=s.replace(anchor,wage_ui+anchor,1)

# Load wage settings.
needle='''        updateMonthlyRestButton();\n\n        workStartDate = null;'''
rep='''        updateMonthlyRestButton();\n        String wageMode = prefs.getString(WAGE_MODE_KEY, "hourly");\n        hourlyWageMode.setChecked(!"monthly".equals(wageMode));\n        monthlyWageMode.setChecked("monthly".equals(wageMode));\n        hourlyRateInput.setText(trimMoney(prefs.getFloat(HOURLY_RATE_KEY, 0f)));\n        monthlySalaryInput.setText(trimMoney(prefs.getFloat(MONTHLY_SALARY_KEY, 0f)));\n        updateWageModeVisibility();\n\n        workStartDate = null;'''
if needle not in s: raise SystemExit('settings load anchor missing')
s=s.replace(needle,rep,1)

# Save/validate wage settings before editor creation.
needle='''        if (parseTime(alarmUpdateTime) == null) alarmUpdateTime = "12:00";\n\n        SharedPreferences.Editor editor = prefs.edit()'''
rep='''        if (parseTime(alarmUpdateTime) == null) alarmUpdateTime = "12:00";\n\n        boolean monthlyWage = monthlyWageMode.isChecked();\n        Float hourlyRate = parseMoney(hourlyRateInput, "请输入正确的时薪");\n        Float monthlySalary = parseMoney(monthlySalaryInput, "请输入正确的月薪");\n        if (hourlyRate == null || monthlySalary == null) return;\n\n        SharedPreferences.Editor editor = prefs.edit()'''
if needle not in s: raise SystemExit('settings save validation anchor missing')
s=s.replace(needle,rep,1)
needle='''.putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime);'''
rep='''.putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime)\n                .putString(WAGE_MODE_KEY, monthlyWage ? "monthly" : "hourly")\n                .putFloat(HOURLY_RATE_KEY, hourlyRate)\n                .putFloat(MONTHLY_SALARY_KEY, monthlySalary);'''
if needle not in s: raise SystemExit('settings editor anchor missing')
s=s.replace(needle,rep,1)

# Helpers before chooseWorkStartDate.
anchor='''    private void chooseWorkStartDate() {\n'''
helpers='''    private void updateWageModeVisibility() {\n        boolean monthly = monthlyWageMode != null && monthlyWageMode.isChecked();\n        if (hourlyWageGroup != null) hourlyWageGroup.setVisibility(monthly ? android.view.View.GONE : android.view.View.VISIBLE);\n        if (monthlyWageGroup != null) monthlyWageGroup.setVisibility(monthly ? android.view.View.VISIBLE : android.view.View.GONE);\n    }\n\n    private Float parseMoney(EditText input, String error) {\n        String raw = input.getText().toString().trim();\n        if (raw.isEmpty()) return 0f;\n        try {\n            float value = Float.parseFloat(raw);\n            if (value < 0 || value > 10000000f) throw new NumberFormatException();\n            return value;\n        } catch (NumberFormatException e) {\n            input.setError(error);\n            return null;\n        }\n    }\n\n    private String trimMoney(float value) {\n        if (Math.abs(value - Math.round(value)) < 0.0001f) return String.valueOf(Math.round(value));\n        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\\\.$", "");\n    }\n\n'''
if anchor not in s: raise SystemExit('settings helper anchor missing')
s=s.replace(anchor,helpers+anchor,1)
p.write_text(s)

# WagePanel: remove wage configuration UI and make panel statistics-only.
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text()
# Constructor no longer loads removed controls.
s=s.replace('''        buildUi();\n        loadSettings();\n        refreshAll();''','''        buildUi();\n        refreshAll();''',1)
# Remove UI from settings title through dayInfo; keep tabs.
start=s.find('        TextView settingsTitle = text("工资设置"')
end=s.find('        LinearLayout viewTabs = horizontal();', start)
if start < 0 or end < 0: raise SystemExit('wage settings UI block missing')
s=s[:start]+'''        TextView statsTitle = text("工资详情", 19, true);\n        statsTitle.setPadding(0, dp(14), 0, dp(4));\n        root.addView(statsTitle);\n        TextView statsInfo = text("工资规则请在设置页面修改。", 13, false);\n        statsInfo.setPadding(0, 0, 0, dp(4));\n        root.addView(statsInfo);\n\n'''+s[end:]
# Remove load/update/save/parse settings methods as they reference absent UI.
start=s.find('    private void loadSettings() {')
end=s.find('    private void chooseWeek() {', start)
if start < 0 or end < 0: raise SystemExit('wage settings methods block missing')
s=s[:start]+s[end:]
p.write_text(s)
