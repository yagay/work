package com.example.workhours;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class WorkAlarmReminderNotification {
    private static final String CHANNEL = "work_alarm_pending_v1";
    private static final int NOTIFICATION_ID = 7310;

    private WorkAlarmReminderNotification() { }

    public static void showUpcoming(Context context, String alarmDate, long triggerAtMillis) {
        show(context, "上班闹钟即将响铃", "可从这里随时关闭这次闹钟", alarmDate, triggerAtMillis, false);
    }

    public static void showSnoozed(Context context, int minutes, int snoozeCount, long triggerAtMillis) {
        show(context,
                "上班闹钟已稍后提醒",
                minutes + " 分钟后再次响铃 · 第 " + snoozeCount + " 次",
                "",
                triggerAtMillis,
                true);
    }

    private static void show(Context context, String title, String text, String alarmDate,
                             long triggerAtMillis, boolean snooze) {
        // Android 13+ cannot display this notification after the user revokes the
        // notification permission. Treat that as a supported degraded state rather
        // than relying on NotificationManager to silently reject the post.
        if (!WorkAlarmNotification.canPostNotifications(context)) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL,
                "上班闹钟待响提醒",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("闹钟接近响铃或稍后提醒期间持续显示，方便随时关闭");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(
                context, 7311, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, WorkAlarmReminderReceiver.class)
                .setAction(WorkAlarmReminderReceiver.ACTION_CANCEL_PENDING)
                .putExtra(WorkAlarmReminderReceiver.EXTRA_ALARM_DATE, alarmDate)
                .putExtra(WorkAlarmReminderReceiver.EXTRA_IS_SNOOZE, snooze);
        PendingIntent stop = PendingIntent.getBroadcast(
                context, 7312, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(content)
                .addAction(new Notification.Action.Builder(null, "关闭这次闹钟", stop).build());

        if (triggerAtMillis > 0L) {
            builder.setWhen(triggerAtMillis).setShowWhen(true).setUsesChronometer(false);
        }

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    public static void cancel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(NOTIFICATION_ID);
    }
}
