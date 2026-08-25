package com.example.workhours;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

public final class WorkAlarmManager {
    public static final String ENABLED_KEY = "work_alarm_enabled";
    public static final String ALARM_LABEL = "上班闹钟";

    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String REST_PREFIX = "rest_";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final int REQUEST_CODE = 7001;

    private WorkAlarmManager() { }

    public static boolean sync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(ENABLED_KEY, false)) {
            cancel(context);
            return true;
        }
        if (!canScheduleExact(context)) return false;

        LocalTime workTime = parseTime(prefs.getString(START_TIME_KEY, "09:00"));
        if (workTime == null) return false;

        LocalDateTime now = LocalDateTime.now();
        LocalDate next = findNextWorkDate(prefs, now, workTime);
        if (next == null) return false;

        LocalDateTime triggerDateTime = LocalDateTime.of(next, workTime);
        long triggerAtMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return false;

        PendingIntent operation = alarmPendingIntent(context);
        Intent showIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent show = PendingIntent.getActivity(
                context, REQUEST_CODE + 1, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(operation);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, show);
        alarmManager.setAlarmClock(info, operation);
        return true;
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(alarmPendingIntent(context));
    }

    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    public static void requestExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(android.net.Uri.parse("package:" + context.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) { }
    }

    private static PendingIntent alarmPendingIntent(Context context) {
        Intent intent = new Intent(context, WorkAlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static LocalDate findNextWorkDate(SharedPreferences prefs, LocalDateTime now, LocalTime workTime) {
        LocalDate date = now.toLocalDate();
        for (int i = 0; i < 370; i++, date = date.plusDays(1)) {
            if (i == 0 && !LocalDateTime.of(date, workTime).isAfter(now)) continue;
            if (isWorkAlarmDay(prefs, date)) return date;
        }
        return null;
    }

    private static boolean isWorkAlarmDay(SharedPreferences prefs, LocalDate date) {
        LocalDate workStart = parseDate(prefs.getString(WORK_START_DATE_KEY, ""));
        if (workStart != null && date.isBefore(workStart)) return false;
        if (isBankHoliday(date)) return false;
        if (prefs.getBoolean(LEAVE_PREFIX + date, false)) return false;
        if (prefs.getBoolean(REST_PREFIX + date, false)) return false;

        // A manually configured normal work day overrides automatic weekly/monthly rest rules.
        if (prefs.contains(OVERRIDE_PREFIX + date)) return true;

        int dayIndex = date.getDayOfWeek().getValue() - 1;
        if (!prefs.getBoolean("day_" + dayIndex, dayIndex < 5)) return false;
        return !getMonthlyRestDays(prefs).contains(date.getDayOfMonth());
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

    private static LocalTime parseTime(String raw) {
        if (raw == null) return null;
        try {
            return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try { return LocalDate.parse(raw.trim()); }
        catch (DateTimeParseException e) { return null; }
    }

    private static boolean isBankHoliday(LocalDate date) {
        int year = date.getYear();
        if (date.equals(observedDate(LocalDate.of(year, Month.JANUARY, 1)))) return true;
        LocalDate easter = easterSunday(year);
        if (date.equals(easter.minusDays(2)) || date.equals(easter.plusDays(1))) return true;
        LocalDate earlyMay = LocalDate.of(year, Month.MAY, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        if (date.equals(earlyMay)) return true;
        LocalDate spring = LocalDate.of(year, Month.MAY, 31)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (date.equals(spring)) return true;
        LocalDate summer = LocalDate.of(year, Month.AUGUST, 31)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (date.equals(summer)) return true;

        LocalDate christmas = LocalDate.of(year, Month.DECEMBER, 25);
        LocalDate boxing = LocalDate.of(year, Month.DECEMBER, 26);
        LocalDate observedChristmas;
        LocalDate observedBoxing;
        if (christmas.getDayOfWeek() == DayOfWeek.SATURDAY) {
            observedChristmas = LocalDate.of(year, Month.DECEMBER, 27);
            observedBoxing = LocalDate.of(year, Month.DECEMBER, 28);
        } else if (christmas.getDayOfWeek() == DayOfWeek.SUNDAY) {
            observedChristmas = LocalDate.of(year, Month.DECEMBER, 27);
            observedBoxing = LocalDate.of(year, Month.DECEMBER, 26);
        } else {
            observedChristmas = christmas;
            observedBoxing = boxing.getDayOfWeek() == DayOfWeek.SATURDAY
                    ? LocalDate.of(year, Month.DECEMBER, 28) : boxing;
        }
        return date.equals(observedChristmas) || date.equals(observedBoxing);
    }

    private static LocalDate observedDate(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) return date.plusDays(2);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) return date.plusDays(1);
        return date;
    }

    private static LocalDate easterSunday(int year) {
        int a = year % 19, b = year / 100, c = year % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
