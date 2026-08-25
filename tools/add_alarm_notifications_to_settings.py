from pathlib import Path

p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()

old = '''        workAlarmCheck = new CheckBox(this);\n        workAlarmCheck.setText("自动设置上班闹钟");\n        workAlarmCheck.setTextSize(16);\n        root.addView(workAlarmCheck);\n'''
new = '''        workAlarmCheck = new CheckBox(this);\n        workAlarmCheck.setText("自动设置上班闹钟");\n        workAlarmCheck.setTextSize(16);\n        workAlarmCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            if (isChecked) WorkAlarmNotification.requestPermissionIfNeeded(this);\n        });\n        root.addView(workAlarmCheck);\n'''
if old not in s:
    raise SystemExit('workAlarmCheck block not found')
s = s.replace(old, new, 1)

old = '''        if (workAlarmCheck.isChecked()) {\n            WorkAlarmUpdateScheduler.schedule(this);\n            if (WorkAlarmManager.forceSync(this)) {\n                Toast.makeText(this,\n                        "设置已保存 ｜ 系统闹钟写入请求已成功发送",\n                        Toast.LENGTH_LONG).show();\n            } else {\n                Toast.makeText(this,\n                        "设置已保存 ｜ 系统闹钟写入失败，请确认 OxygenOS 时钟可用",\n                        Toast.LENGTH_LONG).show();\n            }\n        } else {\n'''
new = '''        if (workAlarmCheck.isChecked()) {\n            WorkAlarmNotification.requestPermissionIfNeeded(this);\n            WorkAlarmUpdateScheduler.schedule(this);\n            boolean syncSuccess = WorkAlarmManager.forceSync(this);\n            WorkAlarmNotification.notifySyncResult(this, syncSuccess, false);\n            if (syncSuccess) {\n                Toast.makeText(this,\n                        "设置已保存 ｜ 系统闹钟写入请求已成功发送",\n                        Toast.LENGTH_LONG).show();\n            } else {\n                Toast.makeText(this,\n                        "设置已保存 ｜ 系统闹钟写入失败，请确认 OxygenOS 时钟可用",\n                        Toast.LENGTH_LONG).show();\n            }\n        } else {\n'''
if old not in s:
    raise SystemExit('save sync block not found')
s = s.replace(old, new, 1)

p.write_text(s)
