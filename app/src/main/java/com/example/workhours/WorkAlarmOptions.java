package com.example.workhours;

final class WorkAlarmOptions {
    static final String RINGTONE_URI_KEY = "work_alarm_ringtone_uri";
    static final String VIBRATE_KEY = "work_alarm_vibrate";
    static final String VIBRATE_PATTERN_KEY = "work_alarm_vibrate_pattern";
    static final String FADE_SECONDS_KEY = "work_alarm_fade_seconds";
    static final String AUTO_STOP_MINUTES_KEY = "work_alarm_auto_stop_minutes";
    static final String SNOOZE_ENABLED_KEY = "work_alarm_snooze_enabled";
    static final String SNOOZE_MINUTES_KEY = "work_alarm_snooze_minutes";
    static final String SNOOZE_MAX_KEY = "work_alarm_snooze_max";
    static final String KEY_ACTION_KEY = "work_alarm_key_action";
    static final String BACK_ACTION_KEY = "work_alarm_back_action";

    static final boolean DEFAULT_VIBRATE = true;
    static final String DEFAULT_VIBRATE_PATTERN = "normal";
    static final int DEFAULT_FADE_SECONDS = 30;
    static final int DEFAULT_AUTO_STOP_MINUTES = 15;
    static final boolean DEFAULT_SNOOZE_ENABLED = true;
    static final int DEFAULT_SNOOZE_MINUTES = 10;
    static final int DEFAULT_SNOOZE_MAX = 3;
    static final String DEFAULT_KEY_ACTION = "snooze";
    static final String DEFAULT_BACK_ACTION = "snooze";

    private WorkAlarmOptions() { }
}
