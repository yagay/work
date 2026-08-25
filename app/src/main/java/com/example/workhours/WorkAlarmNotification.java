package com.example.workhours;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class WorkAlarmNotification {
    private static final String CHANNEL_ID = "work_alarm_sync";
    private static final int NOTIFICATION_ID = 7201;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 7202;

    private WorkAlarmNotification() { }

    public static void requestPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    public static void notifySyncResult(Context context, boolean success, boolean automatic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "上班闹钟同步",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("通知系统闹钟自动更新是否成功");
            manager.createNotificationChannel(channel);
        }

        String title;
        String text;
        if (automatic) {
            title = success ? "下周闹钟自动更新成功" : "下周闹钟自动更新失败";
            text = success
                    ? "系统闹钟写入请求已成功发送。"
                    : "无法写入系统时钟，请打开 App 检查设置。";
        } else {
            title = success ? "上班闹钟设置成功" : "上班闹钟设置失败";
            text = success
                    ? "系统闹钟写入请求已成功发送。"
                    : "无法写入系统时钟，请确认时钟应用可用。";
        }

        Intent openApp = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
