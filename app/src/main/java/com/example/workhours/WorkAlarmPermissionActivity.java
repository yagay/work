package com.example.workhours;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Coordinates all permissions required for reliable alarm presentation.
 *
 * The settings page previously opened exact-alarm and full-screen-intent settings
 * independently. If both were missing, Android only showed the first page and the
 * second permission was never requested. This tiny activity serializes the flow.
 */
public class WorkAlarmPermissionActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 7401;
    private static final int REQUEST_EXACT_ALARM = 7402;
    private static final int REQUEST_FULL_SCREEN = 7403;

    public static void request(Activity activity) {
        Intent intent = new Intent(activity, WorkAlarmPermissionActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        continuePermissionFlow();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        continuePermissionFlow();
    }

    private void continuePermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            try {
                startActivityForResult(
                        new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())),
                        REQUEST_EXACT_ALARM);
                return;
            } catch (Exception ignored) { }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && !canUseFullScreenIntent()) {
            try {
                startActivityForResult(
                        new Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:" + getPackageName())),
                        REQUEST_FULL_SCREEN);
                return;
            } catch (Exception ignored) { }
        }

        finish();
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) return;
        if (WorkAlarmNotification.canPostNotifications(this)) {
            continuePermissionFlow();
        } else {
            Toast.makeText(
                    this,
                    "未允许通知：闹钟仍可能响铃，但看不到停止通知",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXACT_ALARM) {
            if (canScheduleExactAlarms()) {
                continuePermissionFlow();
            } else {
                Toast.makeText(
                        this,
                        "需要允许精确闹钟，才能保证按时响铃",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == REQUEST_FULL_SCREEN) {
            if (canUseFullScreenIntent()) {
                finish();
            } else {
                Toast.makeText(
                        this,
                        "未允许全屏提醒：锁屏时可能只显示顶部闹钟通知",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private boolean canScheduleExactAlarms() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        return manager != null
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms());
    }

    private boolean canUseFullScreenIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true;
        NotificationManager manager = getSystemService(NotificationManager.class);
        return manager != null && manager.canUseFullScreenIntent();
    }
}
