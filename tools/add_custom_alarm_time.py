from pathlib import Path

# SettingsActivity
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

s=s.replace('    private CheckBox workAlarmCheck;\n',
'''    private CheckBox workAlarmCheck;\n    private CheckBox alarmFollowWorkTimeCheck;\n    private EditText alarmTimeInput;\n    private LinearLayout alarmTimeGroup;\n''')

old='''        workAlarmCheck = new CheckBox(this);\n        workAlarmCheck.setText("自动设置上班闹钟");\n        workAlarmCheck.setTextSize(16);\n        root.addView(workAlarmCheck);\n\n        previewText = text("", 15, true);'''
new='''        workAlarmCheck = new CheckBox(this);\n        workAlarmCheck.setText("自动设置上班闹钟");\n        workAlarmCheck.setTextSize(16);\n        root.addView(workAlarmCheck);\n\n        alarmFollowWorkTimeCheck = new CheckBox(this);\n        alarmFollowWorkTimeCheck.setText("闹钟时间跟随上班时间");\n        alarmFollowWorkTimeCheck.setTextSize(16);\n        root.addView(alarmFollowWorkTimeCheck);\n\n        alarmTimeGroup = new LinearLayout(this);\n        alarmTimeGroup.setOrientation(LinearLayout.VERTICAL);\n        TextView alarmTimeLabel = text("自定义闹钟时间（HH:mm）", 15, true);\n        alarmTimeLabel.setPadding(0, dp(4), 0, 0);\n        alarmTimeGroup.addView(alarmTimeLabel);\n        alarmTimeInput = input("例如：07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);\n        alarmTimeGroup.addView(alarmTimeInput);\n        root.addView(alarmTimeGroup);\n\n        Runnable updateAlarmTimeVisibility = () -> alarmTimeGroup.setVisibility(\n                alarmFollowWorkTimeCheck.isChecked() ? android.view.View.GONE : android.view.View.VISIBLE);\n        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());\n\n        previewText = text("", 15, true);'''
if old not in s: raise SystemExit('alarm ui anchor not found')
s=s.replace(old,new)

old='''        workAlarmCheck.setChecked(prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false));\n\n        workStartDate = null;'''
new='''        workAlarmCheck.setChecked(prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false));\n        alarmFollowWorkTimeCheck.setChecked(prefs.getBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, true));\n        alarmTimeInput.setText(prefs.getString(WorkAlarmManager.ALARM_TIME_KEY, "07:30"));\n        alarmTimeGroup.setVisibility(alarmFollowWorkTimeCheck.isChecked()\n                ? android.view.View.GONE : android.view.View.VISIBLE);\n\n        workStartDate = null;'''
if old not in s: raise SystemExit('load anchor not found')
s=s.replace(old,new)

old='''        String start = normalizeTime(startInput.getText().toString());\n        String end = normalizeTime(endInput.getText().toString());\n\n        SharedPreferences.Editor editor = prefs.edit()'''
new='''        String start = normalizeTime(startInput.getText().toString());\n        String end = normalizeTime(endInput.getText().toString());\n        boolean followWorkTime = alarmFollowWorkTimeCheck.isChecked();\n        String alarmTime = normalizeTime(alarmTimeInput.getText().toString());\n        if (workAlarmCheck.isChecked() && !followWorkTime && parseTime(alarmTime) == null) {\n            alarmTimeInput.setError("请输入 HH:mm，例如 07:30");\n            return;\n        }\n        if (parseTime(alarmTime) == null) alarmTime = "07:30";\n\n        SharedPreferences.Editor editor = prefs.edit()'''
if old not in s: raise SystemExit('save validation anchor not found')
s=s.replace(old,new)

old='''.putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays)\n                .putBoolean(WorkAlarmManager.ENABLED_KEY, workAlarmCheck.isChecked());'''
new='''.putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays)\n                .putBoolean(WorkAlarmManager.ENABLED_KEY, workAlarmCheck.isChecked())\n                .putBoolean(WorkAlarmManager.FOLLOW_WORK_TIME_KEY, followWorkTime)\n                .putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime);'''
if old not in s: raise SystemExit('prefs anchor not found')
s=s.replace(old,new)

old='''        startInput.setText(start);\n        endInput.setText(end);\n        monthlyRestInput.setText(monthlyRestDays);'''
new='''        startInput.setText(start);\n        endInput.setText(end);\n        alarmTimeInput.setText(alarmTime);\n        monthlyRestInput.setText(monthlyRestDays);'''
if old not in s: raise SystemExit('normalize ui anchor not found')
s=s.replace(old,new)

p.write_text(s)

# WorkAlarmManager
p=Path('app/src/main/java/com/example/workhours/WorkAlarmManager.java')
s=p.read_text()
s=s.replace('''    public static final String ENABLED_KEY = "work_alarm_enabled";\n    public static final String ALARM_LABEL = "上班闹钟（WorkHoursApp）";''',
'''    public static final String ENABLED_KEY = "work_alarm_enabled";\n    public static final String FOLLOW_WORK_TIME_KEY = "work_alarm_follow_work_time";\n    public static final String ALARM_TIME_KEY = "work_alarm_time";\n    public static final String ALARM_LABEL = "上班闹钟（WorkHoursApp）";''')
old='''        LocalTime workTime = parseTime(prefs.getString(START_TIME_KEY, "09:00"));\n        if (workTime == null) return false;'''
new='''        boolean followWorkTime = prefs.getBoolean(FOLLOW_WORK_TIME_KEY, true);\n        String rawAlarmTime = followWorkTime\n                ? prefs.getString(START_TIME_KEY, "09:00")\n                : prefs.getString(ALARM_TIME_KEY, "07:30");\n        LocalTime workTime = parseTime(rawAlarmTime);\n        if (workTime == null) return false;'''
if old not in s: raise SystemExit('manager time anchor not found')
s=s.replace(old,new)
p.write_text(s)
