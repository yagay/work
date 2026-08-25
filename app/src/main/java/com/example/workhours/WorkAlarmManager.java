package com.example.workhours;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.AlarmClock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WorkAlarmManager {
    public static final String ENABLED_KEY = "work_alarm_enabled";
    public static final String FOLLOW_WORK_TIME_KEY = "work_alarm_follow_work_time";
    public static final String ALARM_TIME_KEY = "work_alarm_time";
    public static final String ALARM_LABEL = "上班闹钟（WorkHoursApp）";

    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String REST_PREFIX = "rest_";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String LAST_SYNC_SIGNATURE_KEY = "work_alarm_last_sync_signature";

    private WorkAlarmManager() { }

    /**
     * Synchronizes the coming ISO week to the phone's alarm-clock app.
     * The WorkHours app never rings by itself; the clock app owns the actual alarm.
     */
    public static boolean sync(Context context) {
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

    /** Force a resync after the user changes a date or work rule. */
    public static boolean forceSync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(LAST_SYNC_SIGNATURE_KEY).apply();
        return sync(context);
    }

    /**
     * Public AlarmClock intents do not provide a reliable cross-vendor API for deleting a
     * repeating alarm. Disabling this feature therefore stops future automatic writes only.
     */
    public static void cancel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(LAST_SYNC_SIGNATURE_KEY).apply();
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

    private static int toCalendarDay(DayOfWeek day) {
        switch (day) {
            case MONDAY: return Calendar.MONDAY;
            case TUESDAY: return Calendar.TUESDAY;
            case WEDNESDAY: return Calendar.WEDNESDAY;
            case THURSDAY: return Calendar.THURSDAY;
            case FRIDAY: return Calendar.FRIDAY;
            case SATURDAY: return Calendar.SATURDAY;
            default: return Calendar.SUNDAY;
        }
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
