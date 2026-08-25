package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class WorkAlarmRescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("work_hours_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {
            WorkAlarmManager.sync(context);
        }
    }
}
