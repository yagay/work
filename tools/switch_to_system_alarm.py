from pathlib import Path

# SettingsActivity: remove exact-alarm permission flow and update copy.
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('TextView alarmInfo = text("开启后按上班时间自动安排下一次有效工作日闹钟，并自动跳过公共假日、请假、手动休息和每月固定休息日。修改这些规则后会重新计算。", 13, false);','TextView alarmInfo = text("开启后按本周实际工作日期同步到手机系统时钟。公共假日、请假、手动休息和每月固定休息日会从本周闹钟星期中排除；App 本身不响铃。", 13, false);')
old='''    @Override\n    protected void onResume() {\n        super.onResume();\n        if (prefs != null\n                && prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)\n                && WorkAlarmManager.canScheduleExact(this)) {\n            WorkAlarmManager.sync(this);\n        }\n    }\n'''
new='''    @Override\n    protected void onResume() {\n        super.onResume();\n        if (prefs != null && prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {\n            WorkAlarmManager.sync(this);\n        }\n    }\n'''
s=s.replace(old,new)
old='''        if (workAlarmCheck.isChecked()) {\n            if (!WorkAlarmManager.canScheduleExact(this)) {\n                Toast.makeText(this, "请允许精确闹钟权限，返回 App 后会自动完成设置", Toast.LENGTH_LONG).show();\n                WorkAlarmManager.requestExactAlarmPermission(this);\n            } else if (WorkAlarmManager.sync(this)) {\n                Toast.makeText(this, "下一次上班闹钟已安排", Toast.LENGTH_SHORT).show();\n            } else {\n                Toast.makeText(this, "无法安排上班闹钟，请检查上班时间和工作日设置", Toast.LENGTH_LONG).show();\n            }\n        } else {\n            WorkAlarmManager.cancel(this);\n        }\n'''
new='''        if (workAlarmCheck.isChecked()) {\n            if (WorkAlarmManager.forceSync(this)) {\n                Toast.makeText(this, "本周上班闹钟已同步到系统时钟", Toast.LENGTH_SHORT).show();\n            } else {\n                Toast.makeText(this, "无法写入系统时钟，请确认手机有可用的时钟应用", Toast.LENGTH_LONG).show();\n            }\n        } else {\n            WorkAlarmManager.cancel(this);\n        }\n'''
s=s.replace(old,new)
p.write_text(s)

# MainActivity: date changes force the weekly system-clock rule to be recalculated.
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text().replace('WorkAlarmManager.sync(this);dialog.dismiss();','WorkAlarmManager.forceSync(this);dialog.dismiss();')
p.write_text(s)

# Receiver no longer launches a custom alarm. It only clears the weekly fingerprint;
# the next visible App session safely writes the current week's rule to system Clock.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmRescheduleReceiver.java')
p.write_text('''package com.example.workhours;\n\nimport android.content.BroadcastReceiver;\nimport android.content.Context;\nimport android.content.Intent;\nimport android.content.SharedPreferences;\n\npublic class WorkAlarmRescheduleReceiver extends BroadcastReceiver {\n    @Override\n    public void onReceive(Context context, Intent intent) {\n        SharedPreferences prefs = context.getSharedPreferences("work_hours_prefs", Context.MODE_PRIVATE);\n        if (prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {\n            prefs.edit().remove("work_alarm_last_sync_signature").apply();\n        }\n    }\n}\n''')

# Manifest: remove exact-alarm/vibrate/custom ringing activity while retaining SET_ALARM.
p=Path('app/src/main/AndroidManifest.xml')
s=p.read_text()
s=s.replace('    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />\n','')
s=s.replace('    <uses-permission android:name="android.permission.VIBRATE" />\n','')
import re
s=re.sub(r'\n\s*<activity\s+android:name="\.WorkAlarmActivity"[\s\S]*?/>' ,'',s)
p.write_text(s)
