from pathlib import Path

# 1) Add scheduler class
Path('app/src/main/java/com/example/workhours/WorkAlarmUpdateScheduler.java').write_text(r'''package com.example.workhours;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

public final class WorkAlarmUpdateScheduler {
    public static final String UPDATE_TIME_KEY = "work_alarm_weekly_update_time";
    public static final String ACTION_WEEKLY_UPDATE = "com.example.workhours.WEEKLY_ALARM_UPDATE";
    private static final String PREFS = "work_hours_prefs";
    private static final int REQUEST_CODE = 7102;

    private WorkAlarmUpdateScheduler() { }

    public static void schedule(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {
            cancel(context);
            return;
        }

        LocalTime updateTime = parseTime(prefs.getString(UPDATE_TIME_KEY, "12:00"));
        if (updateTime == null) updateTime = LocalTime.NOON;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = LocalDateTime.of(
                now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                updateTime);
        if (!next.isAfter(now)) next = next.plusWeeks(1);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        long trigger = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        manager.cancel(pendingIntent(context));
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context));
    }

    public static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pendingIntent(context));
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, WorkAlarmRescheduleReceiver.class)
                .setAction(ACTION_WEEKLY_UPDATE);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static LocalTime parseTime(String raw) {
        if (raw == null) return null;
        try {
            return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }
}
''')

# 2) Receiver: weekly trigger syncs next week and reschedules itself; boot/time changes re-arm scheduler.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmRescheduleReceiver.java')
p.write_text(r'''package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class WorkAlarmRescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("work_hours_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {
            WorkAlarmUpdateScheduler.cancel(context);
            return;
        }

        String action = intent == null ? null : intent.getAction();
        if (WorkAlarmUpdateScheduler.ACTION_WEEKLY_UPDATE.equals(action)) {
            WorkAlarmManager.forceSyncNextWeek(context);
        } else {
            prefs.edit().remove("work_alarm_last_sync_signature").apply();
        }
        WorkAlarmUpdateScheduler.schedule(context);
    }
}
''')

# 3) WorkAlarmManager: generalize week sync and add next-week API.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmManager.java')
s=p.read_text()
start=s.index('    public static boolean sync(Context context) {')
end=s.index('    /** Force a resync after the user changes a date or work rule. */')
new=r'''    public static boolean sync(Context context) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return syncWeek(context, monday, true, false);
    }

    /** Synchronize the full next ISO week, intended for the Sunday automatic refresh. */
    public static boolean syncNextWeek(Context context) {
        LocalDate nextMonday = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return syncWeek(context, nextMonday, false, false);
    }

    public static boolean forceSyncNextWeek(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(LAST_SYNC_SIGNATURE_KEY).apply();
        return syncWeek(context,
                LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)),
                false, true);
    }

    private static boolean syncWeek(Context context, LocalDate monday,
                                    boolean skipPastDays, boolean force) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(ENABLED_KEY, false)) {
            prefs.edit().remove(LAST_SYNC_SIGNATURE_KEY).apply();
            return true;
        }

        boolean followWorkTime = prefs.getBoolean(FOLLOW_WORK_TIME_KEY, true);
        String rawAlarmTime = followWorkTime
                ? prefs.getString(START_TIME_KEY, "09:00")
                : prefs.getString(ALARM_TIME_KEY, "07:30");
        LocalTime workTime = parseTime(rawAlarmTime);
        if (workTime == null) return false;

        LocalDate today = LocalDate.now();
        LocalDate sunday = monday.plusDays(6);
        ArrayList<Integer> days = new ArrayList<>();
        int mask = 0;
        for (LocalDate d = monday; !d.isAfter(sunday); d = d.plusDays(1)) {
            if (skipPastDays) {
                if (d.isBefore(today)) continue;
                if (d.equals(today) && !workTime.isAfter(LocalTime.now())) continue;
            }
            if (!isWorkAlarmDay(prefs, d)) continue;
            days.add(toCalendarDay(d.getDayOfWeek()));
            mask |= 1 << (d.getDayOfWeek().getValue() - 1);
        }

        WeekFields wf = WeekFields.ISO;
        int week = monday.get(wf.weekOfWeekBasedYear());
        int weekYear = monday.get(wf.weekBasedYear());
        String signature = weekYear + "-W" + week + "|" + workTime + "|" + mask;
        if (!force && signature.equals(prefs.getString(LAST_SYNC_SIGNATURE_KEY, ""))) return true;

        if (days.isEmpty()) {
            prefs.edit().putString(LAST_SYNC_SIGNATURE_KEY, signature).apply();
            return true;
        }

        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, ALARM_LABEL)
                .putExtra(AlarmClock.EXTRA_HOUR, workTime.getHour())
                .putExtra(AlarmClock.EXTRA_MINUTES, workTime.getMinute())
                .putExtra(AlarmClock.EXTRA_DAYS, days)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) == null) return false;

        try {
            context.startActivity(intent);
            prefs.edit().putString(LAST_SYNC_SIGNATURE_KEY, signature).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

'''
s=s[:start]+new+s[end:]
p.write_text(s)

# 4) Settings UI: update-time field, load/save/validate, schedule after save.
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('    private LinearLayout alarmTimeGroup;\n', '    private LinearLayout alarmTimeGroup;\n    private EditText alarmUpdateTimeInput;\n')
anchor='''        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());\n\n        previewText = text("", 15, true);'''
replacement='''        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());\n\n        TextView updateTimeLabel = text("每周日自动更新闹钟时间（HH:mm）", 15, true);\n        updateTimeLabel.setPadding(0, dp(12), 0, 0);\n        root.addView(updateTimeLabel);\n        alarmUpdateTimeInput = input("例如：12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);\n        root.addView(alarmUpdateTimeInput);\n        TextView updateTimeInfo = text("每周日到这个时间自动重新计算下一周工作日并同步系统时钟。系统省电策略可能让后台执行稍有延迟。", 13, false);\n        updateTimeInfo.setPadding(0, dp(4), 0, 0);\n        root.addView(updateTimeInfo);\n\n        previewText = text("", 15, true);'''
if anchor not in s: raise SystemExit('UI anchor missing')
s=s.replace(anchor,replacement)

anchor='''        alarmTimeInput.setText(prefs.getString(WorkAlarmManager.ALARM_TIME_KEY, "07:30"));\n        alarmTimeGroup.setVisibility(alarmFollowWorkTimeCheck.isChecked()'''
replacement='''        alarmTimeInput.setText(prefs.getString(WorkAlarmManager.ALARM_TIME_KEY, "07:30"));\n        alarmUpdateTimeInput.setText(prefs.getString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, "12:00"));\n        alarmTimeGroup.setVisibility(alarmFollowWorkTimeCheck.isChecked()'''
if anchor not in s: raise SystemExit('load anchor missing')
s=s.replace(anchor,replacement)

anchor='''        if (parseTime(alarmTime) == null) alarmTime = "07:30";\n\n        SharedPreferences.Editor editor = prefs.edit()'''
replacement='''        if (parseTime(alarmTime) == null) alarmTime = "07:30";\n        String alarmUpdateTime = normalizeTime(alarmUpdateTimeInput.getText().toString());\n        if (workAlarmCheck.isChecked() && parseTime(alarmUpdateTime) == null) {\n            alarmUpdateTimeInput.setError("请输入 HH:mm，例如 12:00");\n            return;\n        }\n        if (parseTime(alarmUpdateTime) == null) alarmUpdateTime = "12:00";\n\n        SharedPreferences.Editor editor = prefs.edit()'''
if anchor not in s: raise SystemExit('validation anchor missing')
s=s.replace(anchor,replacement)

anchor='''.putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime);'''
replacement='''.putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime)\n                .putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime);'''
if anchor not in s: raise SystemExit('prefs anchor missing')
s=s.replace(anchor,replacement)

# schedule/cancel alongside sync result
s=s.replace('''            if (WorkAlarmManager.forceSync(this)) {\n                Toast.makeText(this,''', '''            WorkAlarmUpdateScheduler.schedule(this);\n            if (WorkAlarmManager.forceSync(this)) {\n                Toast.makeText(this,''')
s=s.replace('''        } else {\n            WorkAlarmManager.cancel(this);\n            Toast.makeText(this, "设置已保存 ｜ 自动闹钟已关闭", Toast.LENGTH_SHORT).show();''', '''        } else {\n            WorkAlarmManager.cancel(this);\n            WorkAlarmUpdateScheduler.cancel(this);\n            Toast.makeText(this, "设置已保存 ｜ 自动闹钟已关闭", Toast.LENGTH_SHORT).show();''')
s=s.replace('''        alarmTimeInput.setText(alarmTime);\n        monthlyRestInput.setText(monthlyRestDays);''', '''        alarmTimeInput.setText(alarmTime);\n        alarmUpdateTimeInput.setText(alarmUpdateTime);\n        monthlyRestInput.setText(monthlyRestDays);''')
p.write_text(s)

# 5) Manifest receiver handles internal weekly update explicitly (component broadcast doesn't require filter,
# but adding action documents it and keeps intent handling visible).
p=Path('app/src/main/AndroidManifest.xml')
s=p.read_text()
s=s.replace('''                <action android:name="android.intent.action.TIMEZONE_CHANGED" />''', '''                <action android:name="android.intent.action.TIMEZONE_CHANGED" />\n                <action android:name="com.example.workhours.WEEKLY_ALARM_UPDATE" />''')
p.write_text(s)
