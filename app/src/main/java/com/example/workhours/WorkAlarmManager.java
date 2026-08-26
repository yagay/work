package com.example.workhours;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

public final class WorkAlarmManager {
    public static final String ENABLED_KEY = "work_alarm_enabled";
    public static final String FOLLOW_WORK_TIME_KEY = "work_alarm_follow_work_time";
    public static final String ALARM_TIME_KEY = "work_alarm_time";
    public static final String ALARM_LABEL = "上班闹钟";

    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";
    private static final String REST_RULE_MODE_KEY = "rest_rule_mode";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String REST_PREFIX = "rest_";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String SCHEDULED_DATES_KEY = "work_alarm_scheduled_dates";
    private static final int DAYS_AHEAD = 21;
    private static final int SNOOZE_REQUEST_CODE = 1909010;

    private WorkAlarmManager() { }

    public static boolean sync(Context context) { return rebuild(context); }
    public static boolean syncNextWeek(Context context) { return rebuild(context); }
    public static boolean forceSyncNextWeek(Context context) { return rebuild(context); }
    public static boolean forceSync(Context context) { return rebuild(context); }

    private static boolean rebuild(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cancelScheduled(context);
        if (!prefs.getBoolean(ENABLED_KEY, false)) return true;
        if (!canScheduleExact(context)) return false;

        LocalTime alarmTime = configuredAlarmTime(prefs);
        if (alarmTime == null) return false;

        LocalDate today = LocalDate.now();
        Set<String> scheduled = new HashSet<>();
        for (int i = 0; i < DAYS_AHEAD; i++) {
            LocalDate date = today.plusDays(i);
            if (!isWorkAlarmDay(prefs, date)) continue;
            LocalDateTime when = LocalDateTime.of(date, alarmTime);
            if (!when.isAfter(LocalDateTime.now())) continue;
            if (scheduleDate(context, date, when)) scheduled.add(date.toString());
        }
        prefs.edit().putStringSet(SCHEDULED_DATES_KEY, scheduled).apply();
        return true;
    }

    private static boolean scheduleDate(Context context, LocalDate date, LocalDateTime when) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null || !canScheduleExact(context)) return false;
        Intent fire = new Intent(context, WorkAlarmReceiver.class)
                .setAction(WorkAlarmReceiver.ACTION_FIRE)
                .putExtra("alarm_date", date.toString());
        PendingIntent operation = PendingIntent.getBroadcast(context, requestCode(date), fire,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent show = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showIntent = PendingIntent.getActivity(context, requestCode(date) + 30000000, show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAt = when.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public static void scheduleSnooze(Context context, int minutes, int snoozeCount) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null || !canScheduleExact(context)) return;
        Intent fire = new Intent(context, WorkAlarmReceiver.class)
                .setAction(WorkAlarmReceiver.ACTION_FIRE)
                .putExtra("alarm_date", LocalDate.now().toString())
                .putExtra("snooze", true)
                .putExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, snoozeCount);
        PendingIntent operation = PendingIntent.getBroadcast(context, SNOOZE_REQUEST_CODE, fire,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAt = System.currentTimeMillis() + minutes * 60_000L;
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, operation), operation);
        } catch (SecurityException ignored) { }
    }

    public static void cancel(Context context) {
        cancelScheduled(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            PendingIntent snooze = PendingIntent.getBroadcast(context, SNOOZE_REQUEST_CODE,
                    new Intent(context, WorkAlarmReceiver.class).setAction(WorkAlarmReceiver.ACTION_FIRE),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (snooze != null) { am.cancel(snooze); snooze.cancel(); }
        }
    }

    private static void cancelScheduled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> old = prefs.getStringSet(SCHEDULED_DATES_KEY, null);
        if (old != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) for (String raw : new HashSet<>(old)) {
                try {
                    LocalDate d = LocalDate.parse(raw);
                    PendingIntent pi = PendingIntent.getBroadcast(context, requestCode(d),
                            new Intent(context, WorkAlarmReceiver.class).setAction(WorkAlarmReceiver.ACTION_FIRE),
                            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                    if (pi != null) { am.cancel(pi); pi.cancel(); }
                } catch (Exception ignored) { }
            }
        }
        prefs.edit().remove(SCHEDULED_DATES_KEY).apply();
    }

    public static boolean canScheduleExact(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
    }

    public static boolean canUseFullScreen(Context context) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.canUseFullScreenIntent();
    }

    public static void requestAlarmPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= 31 && !canScheduleExact(activity)) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + activity.getPackageName())));
                return;
            } catch (Exception ignored) { }
        }
        if (Build.VERSION.SDK_INT >= 34 && !canUseFullScreen(activity)) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (Exception ignored) { }
        }
    }

    private static LocalTime configuredAlarmTime(SharedPreferences prefs) {
        boolean follow = prefs.getBoolean(FOLLOW_WORK_TIME_KEY, true);
        String raw = follow ? prefs.getString(START_TIME_KEY, "09:00") : prefs.getString(ALARM_TIME_KEY, "07:30");
        if (raw == null) return null;
        try { return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm")); }
        catch (DateTimeParseException | IllegalArgumentException e) { return null; }
    }

    private static int requestCode(LocalDate d) {
        return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
    }

    private static boolean isWorkAlarmDay(SharedPreferences prefs, LocalDate date) {
        LocalDate workStart = parseDate(prefs.getString(WORK_START_DATE_KEY, ""));
        if (workStart != null && date.isBefore(workStart)) return false;
        if (HolidayCalendar.isHoliday(prefs, date)) return false;
        if (prefs.getBoolean(LEAVE_PREFIX + date, false)) return false;
        if (prefs.getBoolean(REST_PREFIX + date, false)) return false;
        if (prefs.contains(OVERRIDE_PREFIX + date)) return true;
        if ("monthly".equals(prefs.getString(REST_RULE_MODE_KEY, "weekly")))
            return !getMonthlyRestDays(prefs).contains(date.getDayOfMonth());
        int dayIndex = date.getDayOfWeek().getValue() - 1;
        return prefs.getBoolean("day_" + dayIndex, dayIndex < 5);
    }

    private static Set<Integer> getMonthlyRestDays(SharedPreferences prefs) {
        Set<Integer> result = new HashSet<>();
        String raw = prefs.getString(MONTHLY_REST_DAYS_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return result;
        for (String part : raw.replace('，', ',').split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 31) result.add(value);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try { return LocalDate.parse(raw.trim()); }
        catch (DateTimeParseException e) { return null; }
    }
}
