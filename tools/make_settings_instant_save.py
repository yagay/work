from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

# loading guard
s=s.replace('''    private boolean appliedDarkTheme;\n    private final Button[] restDayButtons = new Button[7];''','''    private boolean appliedDarkTheme;\n    private boolean loadingSettings = false;\n    private final Button[] restDayButtons = new Button[7];''')

# attach listeners after initial load
s=s.replace('''        AppThemeManager.applySystemBars(this);\n        loadSettings();\n    }''','''        AppThemeManager.applySystemBars(this);\n        loadSettings();\n        setupInstantSave();\n    }''',1)

# Remove explicit save button block
old='''        Button save = new Button(this);\n        UiStyle.button(this, save, true);\n        save.setText("保存设置");\n        save.setTextSize(UI_LABEL_SP);\n        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ACTION_DP));\n        saveParams.topMargin = dp(22);\n        root.addView(save, saveParams);\n        save.setOnClickListener(v -> save());\n\n'''
if old not in s: raise SystemExit('save button block missing')
s=s.replace(old,'',1)

# Work alarm listener: keep visibility/permission but instant persist
old='''        workAlarmCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            if (isChecked) { WorkAlarmNotification.requestPermissionIfNeeded(this); WorkAlarmManager.requestAlarmPermissions(this); }\n            if (alarmOptionsGroup != null) alarmOptionsGroup.setVisibility(\n                    isChecked ? android.view.View.VISIBLE : android.view.View.GONE);\n        });'''
new='''        workAlarmCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            if (alarmOptionsGroup != null) alarmOptionsGroup.setVisibility(\n                    isChecked ? android.view.View.VISIBLE : android.view.View.GONE);\n            if (loadingSettings) return;\n            if (isChecked) { WorkAlarmNotification.requestPermissionIfNeeded(this); WorkAlarmManager.requestAlarmPermissions(this); }\n            persistSettings(false, false);\n        });'''
if old not in s: raise SystemExit('work alarm listener missing')
s=s.replace(old,new,1)

old='''        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());'''
new='''        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            updateAlarmTimeVisibility.run();\n            if (!loadingSettings) persistSettings(false, false);\n        });'''
if old not in s: raise SystemExit('alarm follow listener missing')
s=s.replace(old,new,1)

# Rest day buttons persist immediately
old='''            day.setOnClickListener(v -> { day.setSelected(!day.isSelected()); updateRestDayButtonStyle(day); });'''
new='''            day.setOnClickListener(v -> {\n                day.setSelected(!day.isSelected());\n                updateRestDayButtonStyle(day);\n                if (!loadingSettings) persistSettings(false, false);\n            });'''
if old not in s: raise SystemExit('rest day listener missing')
s=s.replace(old,new)

# clear start date: immediate save, update toast
s=s.replace('''                    workStartDate = null;\n                    updateWorkStartDateButton();\n                    Toast.makeText(this, "工作开始日期已清除，点击保存设置后生效", Toast.LENGTH_SHORT).show();''','''                    workStartDate = null;\n                    updateWorkStartDateButton();\n                    persistSettings(false, false);\n                    Toast.makeText(this, "工作开始日期已清除", Toast.LENGTH_SHORT).show();''',1)

# Date picker immediate save
s=s.replace('''                    workStartDate = LocalDate.of(year, month + 1, dayOfMonth);\n                    updateWorkStartDateButton();''','''                    workStartDate = LocalDate.of(year, month + 1, dayOfMonth);\n                    updateWorkStartDateButton();\n                    if (!loadingSettings) persistSettings(false, false);''',1)

# Rest rule mode persist after UI update
anchor='''        if (monthly && monthlyRestCalendarGrid != null) rebuildMonthlyRestCalendar();\n    }\n\n    private void updateRestModeButtonStyle'''
replace='''        if (monthly && monthlyRestCalendarGrid != null) rebuildMonthlyRestCalendar();\n        if (!loadingSettings) persistSettings(false, false);\n    }\n\n    private void updateRestModeButtonStyle'''
if anchor not in s: raise SystemExit('setRestRuleMode end missing')
s=s.replace(anchor,replace,1)

# monthly rest selections persist immediately
old='''        monthlyRestInput.setText(out.toString());\n        updateMonthlyRestButton();\n    }'''
new='''        monthlyRestInput.setText(out.toString());\n        updateMonthlyRestButton();\n        if (!loadingSettings) persistSettings(false, false);\n    }'''
# first occurrence should be writeSelectedMonthlyRestDays; safe exact occurrence near method
idx=s.find('private void writeSelectedMonthlyRestDays')
if idx<0: raise SystemExit('writeSelectedMonthlyRestDays missing')
pos=s.find(old,idx)
if pos<0: raise SystemExit('writeSelectedMonthlyRestDays end missing')
s=s[:pos]+s[pos:].replace(old,new,1)

# Guard loadSettings and refresh listeners
s=s.replace('''    private void loadSettings() {\n        startInput.setText''','''    private void loadSettings() {\n        loadingSettings = true;\n        startInput.setText''',1)
anchor='''        updatePreview(false);\n    }\n\n    private JSONArray readWageHistory()'''
replace='''        updatePreview(false);\n        loadingSettings = false;\n    }\n\n    private JSONArray readWageHistory()'''
if anchor not in s: raise SystemExit('loadSettings end missing')
s=s.replace(anchor,replace,1)

# Replace save() with silent-capable persistSettings()
start=s.find('    private void save() {')
if start<0: raise SystemExit('save method missing')
end=s.find('    private String normalizeMonthlyRestDays', start)
if end<0: raise SystemExit('save method end missing')
new_method='''    private boolean persistSettings(boolean showError, boolean showToast) {\n        if (loadingSettings) return false;\n        Float dailyHours = calculate(showError);\n        if (dailyHours == null) return false;\n        String monthlyRestDays = normalizeMonthlyRestDays(showError);\n        if (monthlyRestDays == null) return false;\n\n        int breakMinutes;\n        try { breakMinutes = Integer.parseInt(breakInput.getText().toString().trim()); }\n        catch (Exception e) { if (showError) breakInput.setError("请输入正确的分钟数"); return false; }\n        String start = normalizeTime(startInput.getText().toString());\n        String end = normalizeTime(endInput.getText().toString());\n        boolean followWorkTime = alarmFollowWorkTimeCheck.isChecked();\n        String alarmTime = normalizeTime(alarmTimeInput.getText().toString());\n        if (workAlarmCheck.isChecked() && !followWorkTime && parseTime(alarmTime) == null) {\n            if (showError) alarmTimeInput.setError("请输入 HH:mm，例如 07:30");\n            return false;\n        }\n        if (parseTime(alarmTime) == null) alarmTime = "07:30";\n        String alarmUpdateTime = normalizeTime(alarmUpdateTimeInput.getText().toString());\n        if (workAlarmCheck.isChecked() && parseTime(alarmUpdateTime) == null) {\n            if (showError) alarmUpdateTimeInput.setError("请输入 HH:mm，例如 12:00");\n            return false;\n        }\n        if (parseTime(alarmUpdateTime) == null) alarmUpdateTime = "12:00";\n\n        SharedPreferences.Editor editor = prefs.edit()\n                .putString(START_TIME_KEY, start)\n                .putString(END_TIME_KEY, end)\n                .putInt(BREAK_MINUTES_KEY, breakMinutes)\n                .putFloat("daily_hours", dailyHours)\n                .putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays)\n                .putString(REST_RULE_MODE_KEY, restRuleMode)\n                .putString(HolidayCalendar.REGION_KEY, holidayRegion)\n                .putBoolean(WorkAlarmManager.ENABLED_KEY, workAlarmCheck.isChecked())\n                .putBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, followWorkTime)\n                .putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime)\n                .putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime)\n                .putBoolean(WorkAlarmOptions.VIBRATE_KEY, alarmVibrateCheck.isChecked())\n                .putBoolean(WorkAlarmOptions.SNOOZE_ENABLED_KEY, snoozeEnabledCheck.isChecked());\n\n        if (workStartDate == null) editor.remove(WORK_START_DATE_KEY);\n        else editor.putString(WORK_START_DATE_KEY, workStartDate.toString());\n        for (int i = 0; i < 7; i++) editor.putBoolean("day_" + i, !restDayButtons[i].isSelected());\n        editor.apply();\n\n        if (workAlarmCheck.isChecked()) {\n            WorkAlarmUpdateScheduler.schedule(this);\n            WorkAlarmManager.forceSync(this);\n        } else {\n            WorkAlarmManager.cancel(this);\n            WorkAlarmUpdateScheduler.cancel(this);\n        }\n\n        loadingSettings = true;\n        startInput.setText(start);\n        endInput.setText(end);\n        alarmTimeInput.setText(alarmTime);\n        alarmUpdateTimeInput.setText(alarmUpdateTime);\n        monthlyRestInput.setText(monthlyRestDays);\n        loadingSettings = false;\n        updateMonthlyRestButton();\n        updatePreview(false);\n        if (showToast) Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();\n        return true;\n    }\n\n    private void setupInstantSave() {\n        android.view.View.OnFocusChangeListener saver = (v, hasFocus) -> {\n            if (!hasFocus && !loadingSettings) persistSettings(true, false);\n        };\n        startInput.setOnFocusChangeListener(saver);\n        endInput.setOnFocusChangeListener(saver);\n        breakInput.setOnFocusChangeListener(saver);\n        alarmTimeInput.setOnFocusChangeListener(saver);\n        alarmUpdateTimeInput.setOnFocusChangeListener(saver);\n\n        alarmVibrateCheck.setOnCheckedChangeListener((b, checked) -> {\n            if (!loadingSettings) persistSettings(false, false);\n        });\n        snoozeEnabledCheck.setOnCheckedChangeListener((b, checked) -> {\n            if (!loadingSettings) persistSettings(false, false);\n        });\n    }\n\n'''
s=s[:start]+new_method+s[end:]

p.write_text(s)
