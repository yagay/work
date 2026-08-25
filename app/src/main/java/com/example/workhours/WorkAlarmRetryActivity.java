package com.example.workhours;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;
import android.widget.Toast;

public class WorkAlarmRetryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean success = WorkAlarmManager.forceSyncNextWeek(this);
        if (success) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(7201);
            Toast.makeText(this, "下周上班闹钟已同步", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "仍无法写入系统时钟，请打开 App 检查设置", Toast.LENGTH_LONG).show();
            WorkAlarmNotification.notifyRetryRequired(this);
        }
        finish();
    }
}
