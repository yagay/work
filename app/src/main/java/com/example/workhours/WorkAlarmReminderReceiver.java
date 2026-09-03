package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WorkAlarmReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_SHOW_UPCOMING = "com.example.workhours.ALARM_SHOW_UPCOMING";
    public static final String ACTION_CANCEL_PENDING = "com.example.workhours.ALARM_CANCEL_PENDING";
    public static final String EXTRA_ALARM_DATE = "alarm_date";
    public static final String EXTRA_TRIGGER_AT = "trigger_at";
    public static final String EXTRA_IS_SNOOZE = "is_snooze";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_SHOW_UPCOMING.equals(action)) {
            WorkAlarmReminderNotification.showUpcoming(
                    context,
                    intent.getStringExtra(EXTRA_ALARM_DATE),
                    intent.getLongExtra(EXTRA_TRIGGER_AT, 0L));
            return;
        }
        if (ACTION_CANCEL_PENDING.equals(action)) {
            boolean snooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false);
            String alarmDate = intent.getStringExtra(EXTRA_ALARM_DATE);
            if (snooze) WorkAlarmManager.cancelSnooze(context);
            else WorkAlarmManager.cancelDate(context, alarmDate);
            WorkAlarmReminderNotification.cancel(context);
        }
    }
}
