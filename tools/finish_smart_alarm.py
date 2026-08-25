from pathlib import Path

# SettingsActivity: permission flow, better copy, resync on returning from exact-alarm settings.
p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()

s = s.replace(
    'TextView alarmInfo = text("开启后会根据上班时间和每周工作日自动同步系统闹钟。修改工作时间或每周休息日后，保存设置会重新同步。", 13, false);',
    'TextView alarmInfo = text("开启后按上班时间自动安排下一次有效工作日闹钟，并自动跳过公共假日、请假、手动休息和每月固定休息日。修改这些规则后会重新计算。", 13, false);'
)

old = '''        if (workAlarmCheck.isChecked()) {
            if (WorkAlarmManager.sync(this)) {
                Toast.makeText(this, "上班闹钟已同步", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法同步系统闹钟，请确认手机有可用的时钟应用", Toast.LENGTH_LONG).show();
            }
        }
'''
new = '''        if (workAlarmCheck.isChecked()) {
            if (!WorkAlarmManager.canScheduleExact(this)) {
                Toast.makeText(this, "请允许精确闹钟权限，返回 App 后会自动完成设置", Toast.LENGTH_LONG).show();
                WorkAlarmManager.requestExactAlarmPermission(this);
            } else if (WorkAlarmManager.sync(this)) {
                Toast.makeText(this, "下一次上班闹钟已安排", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法安排上班闹钟，请检查上班时间和工作日设置", Toast.LENGTH_LONG).show();
            }
        } else {
            WorkAlarmManager.cancel(this);
        }
'''
if old not in s:
    raise SystemExit('Settings alarm save block not found')
s = s.replace(old, new)

oncreate = '''        setContentView(buildUi());
        loadSettings();
    }

    private ScrollView buildUi() {'''
oncreate_new = '''        setContentView(buildUi());
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null
                && prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)
                && WorkAlarmManager.canScheduleExact(this)) {
            WorkAlarmManager.sync(this);
        }
    }

    private ScrollView buildUi() {'''
if oncreate not in s:
    raise SystemExit('Settings onCreate anchor not found')
s = s.replace(oncreate, oncreate_new)
p.write_text(s)

# MainActivity: any per-day status edit immediately recalculates the next smart alarm.
p = Path('app/src/main/java/com/example/workhours/MainActivity.java')
s = p.read_text()
old1 = 'e.apply();dialog.dismiss();refreshAll();Toast.makeText(this,"修改已保存",Toast.LENGTH_SHORT).show();'
new1 = 'e.apply();WorkAlarmManager.sync(this);dialog.dismiss();refreshAll();Toast.makeText(this,"修改已保存",Toast.LENGTH_SHORT).show();'
if old1 not in s:
    raise SystemExit('Main save-day anchor not found')
s = s.replace(old1, new1)

old2 = '.remove(restKey(date)).apply();dialog.dismiss();refreshAll();}).show());});'
new2 = '.remove(restKey(date)).apply();WorkAlarmManager.sync(this);dialog.dismiss();refreshAll();}).show());});'
if old2 not in s:
    raise SystemExit('Main restore-day anchor not found')
s = s.replace(old2, new2)
p.write_text(s)
