from pathlib import Path

# SettingsActivity
p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()

s = s.replace(
    '    private TextView previewText;\n    private Button workStartDateButton;\n',
    '    private TextView previewText;\n    private Button workStartDateButton;\n    private CheckBox workAlarmCheck;\n'
)

anchor = '''        breakInput = input("30", InputType.TYPE_CLASS_NUMBER);\n        root.addView(breakInput);\n\n        previewText = text("", 15, true);'''
insert = '''        breakInput = input("30", InputType.TYPE_CLASS_NUMBER);\n        root.addView(breakInput);\n\n        TextView alarmTitle = text("上班闹钟", 17, true);\n        alarmTitle.setPadding(0, dp(22), 0, dp(4));\n        root.addView(alarmTitle);\n        TextView alarmInfo = text("开启后会根据上班时间和每周工作日自动同步系统闹钟。修改工作时间或每周休息日后，保存设置会重新同步。", 13, false);\n        alarmInfo.setPadding(0, 0, 0, dp(4));\n        root.addView(alarmInfo);\n        workAlarmCheck = new CheckBox(this);\n        workAlarmCheck.setText("自动设置上班闹钟");\n        workAlarmCheck.setTextSize(16);\n        root.addView(workAlarmCheck);\n\n        previewText = text("", 15, true);'''
if anchor not in s:
    raise SystemExit('alarm UI anchor not found')
s = s.replace(anchor, insert)

load_anchor = '''        monthlyRestInput.setText(prefs.getString(MONTHLY_REST_DAYS_KEY, ""));\n\n        workStartDate = null;'''
load_new = '''        monthlyRestInput.setText(prefs.getString(MONTHLY_REST_DAYS_KEY, ""));\n        workAlarmCheck.setChecked(prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false));\n\n        workStartDate = null;'''
if load_anchor not in s:
    raise SystemExit('load anchor not found')
s = s.replace(load_anchor, load_new)

save_anchor = '''                .putInt(BREAK_MINUTES_KEY, breakMinutes)\n                .putFloat("daily_hours", dailyHours)\n                .putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays);'''
save_new = '''                .putInt(BREAK_MINUTES_KEY, breakMinutes)\n                .putFloat("daily_hours", dailyHours)\n                .putString(MONTHLY_REST_DAYS_KEY, monthlyRestDays)\n                .putBoolean(WorkAlarmManager.ENABLED_KEY, workAlarmCheck.isChecked());'''
if save_anchor not in s:
    raise SystemExit('save prefs anchor not found')
s = s.replace(save_anchor, save_new)

apply_anchor = '''        editor.apply();\n\n        startInput.setText(start);'''
apply_new = '''        editor.apply();\n\n        if (workAlarmCheck.isChecked()) {\n            if (WorkAlarmManager.sync(this)) {\n                Toast.makeText(this, "上班闹钟已同步", Toast.LENGTH_SHORT).show();\n            } else {\n                Toast.makeText(this, "无法同步系统闹钟，请确认手机有可用的时钟应用", Toast.LENGTH_LONG).show();\n            }\n        }\n\n        startInput.setText(start);'''
if apply_anchor not in s:
    raise SystemExit('sync anchor not found')
s = s.replace(apply_anchor, apply_new)
p.write_text(s)

# Manifest permission
p = Path('app/src/main/AndroidManifest.xml')
s = p.read_text()
needle = '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n'
replacement = needle + '    <uses-permission android:name="com.android.alarm.permission.SET_ALARM" />\n'
if 'com.android.alarm.permission.SET_ALARM' not in s:
    if needle not in s:
        raise SystemExit('manifest anchor not found')
    s = s.replace(needle, replacement)
p.write_text(s)
