package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class WorkAlarmRescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                && !WorkAlarmUpdateScheduler.ACTION_WEEKLY_UPDATE.equals(action)) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
                "work_hours_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {
            WorkAlarmManager.cancel(context);
            WorkAlarmUpdateScheduler.cancel(context);
            return;
        }
        WorkAlarmManager.forceSync(context);
        WorkAlarmUpdateScheduler.schedule(context);
    }
}
