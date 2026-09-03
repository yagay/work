package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class WorkAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_FIRE = "com.example.workhours.WORK_ALARM_FIRE";

    @Override public void onReceive(Context context, Intent intent) {
        int snoozeCount = intent == null ? 0
                : intent.getIntExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, 0);

        Intent service = new Intent(context, WorkAlarmRingService.class)
                .setAction(WorkAlarmRingService.ACTION_START)
                .putExtra("alarm_date", intent == null ? "" : intent.getStringExtra("alarm_date"))
                .putExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, snoozeCount);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }
}
