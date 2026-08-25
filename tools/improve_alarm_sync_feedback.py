from pathlib import Path

p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()
old = '''        if (workAlarmCheck.isChecked()) {
            if (WorkAlarmManager.forceSync(this)) {
                Toast.makeText(this, "本周上班闹钟已同步到系统时钟", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法写入系统时钟，请确认手机有可用的时钟应用", Toast.LENGTH_LONG).show();
            }
        } else {
            WorkAlarmManager.cancel(this);
        }

        startInput.setText(start);
        endInput.setText(end);
        monthlyRestInput.setText(monthlyRestDays);
        updatePreview(false);
        Toast.makeText(this, "休息规则和工作设置已保存", Toast.LENGTH_SHORT).show();
'''
new = '''        if (workAlarmCheck.isChecked()) {
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
            Toast.makeText(this, "设置已保存 ｜ 自动闹钟已关闭", Toast.LENGTH_SHORT).show();
        }

        startInput.setText(start);
        endInput.setText(end);
        monthlyRestInput.setText(monthlyRestDays);
        updatePreview(false);
'''
if old not in s:
    raise SystemExit('target save block not found')
s = s.replace(old, new)
p.write_text(s)
