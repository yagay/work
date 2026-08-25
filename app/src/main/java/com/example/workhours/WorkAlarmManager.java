package com.example.workhours;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.AlarmClock;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;

public final class WorkAlarmManager {
    public static final String ENABLED_KEY = "work_alarm_enabled";
    public static final String ALARM_LABEL = "上班闹钟";
    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";

    private WorkAlarmManager() { }

    public static boolean sync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(ENABLED_KEY, false)) return false;

        LocalTime time = parseTime(prefs.getString(START_TIME_KEY, "09:00"));
        if (time == null) return false;

        ArrayList<Integer> days = new ArrayList<>();
        int[] calendarDays = {
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        };
        for (int i = 0; i < 7; i++) {
            if (prefs.getBoolean("day_" + i, i < 5)) days.add(calendarDays[i]);
        }
        if (days.isEmpty()) return false;

        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, ALARM_LABEL)
                .putExtra(AlarmClock.EXTRA_HOUR, time.getHour())
                .putExtra(AlarmClock.EXTRA_MINUTES, time.getMinute())
                .putExtra(AlarmClock.EXTRA_DAYS, days)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(context.getPackageManager()) == null) return false;
        context.startActivity(intent);
        return true;
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
