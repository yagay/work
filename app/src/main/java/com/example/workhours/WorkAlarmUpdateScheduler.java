package com.example.workhours;

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
