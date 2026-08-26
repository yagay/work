package com.example.workhours;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class WorkAlarmRingService extends Service {
    public static final String ACTION_START = "com.example.workhours.ALARM_START";
    public static final String ACTION_STOP = "com.example.workhours.ALARM_STOP";
    public static final String ACTION_SNOOZE = "com.example.workhours.ALARM_SNOOZE";
    private static final String CHANNEL = "work_alarm_ringing";
    private static final int NOTIFICATION_ID = 7301;
    private Ringtone ringtone;
    private Vibrator vibrator;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "上班闹钟", NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("工作日上班闹钟响铃");
            c.setSound(null, null);
            c.enableVibration(false);
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) { stopAlarm(); return START_NOT_STICKY; }
        if (ACTION_SNOOZE.equals(action)) { WorkAlarmManager.scheduleSnooze(this, 10); stopAlarm(); return START_NOT_STICKY; }
        startForeground(NOTIFICATION_ID, buildNotification());
        startSoundAndVibration();
        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        Intent screen = new Intent(this, WorkAlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent full = PendingIntent.getActivity(this, 7302, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 7303,
                new Intent(this, WorkAlarmRingService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent snooze = PendingIntent.getService(this, 7304,
                new Intent(this, WorkAlarmRingService.class).setAction(ACTION_SNOOZE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("上班闹钟")
                .setContentText("到上班时间了")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
                .addAction(new Notification.Action.Builder(null, "稍后10分钟", snooze).build())
                .addAction(new Notification.Action.Builder(null, "停止", stop).build())
                .build();
    }

    private void startSoundAndVibration() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
                ringtone.setLooping(true);
                ringtone.play();
            }
        } catch (Exception ignored) { }
        try {
            VibratorManager vm = getSystemService(VibratorManager.class);
            vibrator = vm == null ? null : vm.getDefaultVibrator();
            if (vibrator != null && vibrator.hasVibrator())
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,700,500,700,500}, 0));
        } catch (Exception ignored) { }
    }

    private void stopAlarm() {
        try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); } catch (Exception ignored) { }
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) { }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public void onDestroy() { stopAlarmResources(); super.onDestroy(); }
    private void stopAlarmResources() {
        try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); } catch (Exception ignored) { }
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) { }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
