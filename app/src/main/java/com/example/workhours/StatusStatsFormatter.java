package com.example.workhours;

/**
 * Single source of truth for the four day counters shown across the app.
 * Change ordering, labels, spacing or separators here to update every caller.
 */
public final class StatusStatsFormatter {
    private static final String SEPARATOR = " ｜ ";

    private StatusStatsFormatter() { }

    public static String format(int workDays, int leaveDays, int holidayDays, int restDays) {
        return "工作 " + workDays + "天"
                + SEPARATOR + "请假 " + leaveDays + "天"
                + SEPARATOR + "公共假日 " + holidayDays + "天"
                + SEPARATOR + "休息 " + restDays + "天";
    }
}
