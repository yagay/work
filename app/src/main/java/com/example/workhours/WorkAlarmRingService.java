package com.example.workhours;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class WorkAlarmRingService extends Service {
    public static final String ACTION_START = "com.example.workhours.ALARM_START";
    public static final String ACTION_STOP = "com.example.workhours.ALARM_STOP";
    public static final String ACTION_SNOOZE = "com.example.workhours.ALARM_SNOOZE";
    public static final String EXTRA_SNOOZE_COUNT = "snooze_count";
    private static final String PREFS = "work_hours_prefs";
    private static final String CHANNEL = "work_alarm_ringing_v2";
    private static final int NOTIFICATION_ID = 7301;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Ringtone ringtone;
    private Vibrator vibrator;
    private int currentSnoozeCount;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "上班闹钟响铃", NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("上班闹钟正在响铃");
            c.setSound(null, null);
            c.enableVibration(false);
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) { stopAlarm(); return START_NOT_STICKY; }
        if (ACTION_SNOOZE.equals(action)) { snooze(); return START_NOT_STICKY; }

        currentSnoozeCount = intent == null ? 0 : intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0);
        startForeground(NOTIFICATION_ID, buildNotification());
        startSoundAndVibration();
        scheduleAutoStop();
        return START_NOT_STICKY;
    }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private Notification buildNotification() {
        SharedPreferences p = prefs();
        int snoozeMinutes = p.getInt(WorkAlarmOptions.SNOOZE_MINUTES_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES);
        boolean canSnooze = p.getBoolean(WorkAlarmOptions.SNOOZE_ENABLED_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_ENABLED)
                && currentSnoozeCount < p.getInt(WorkAlarmOptions.SNOOZE_MAX_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MAX);
        Intent screen = new Intent(this, WorkAlarmActivity.class)
                .putExtra(EXTRA_SNOOZE_COUNT, currentSnoozeCount)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent full = PendingIntent.getActivity(this, 7302, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 7303,
                new Intent(this, WorkAlarmRingService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("上班闹钟")
                .setContentText("到上班时间了")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(full)
                .setFullScreenIntent(full, true);
        if (canSnooze) {
            PendingIntent snooze = PendingIntent.getService(this, 7304,
                    new Intent(this, WorkAlarmRingService.class).setAction(ACTION_SNOOZE),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(new Notification.Action.Builder(null, "稍后" + snoozeMinutes + "分钟", snooze).build());
        }
        b.addAction(new Notification.Action.Builder(null, "停止", stop).build());
        return b.build();
    }

    private void startSoundAndVibration() {
        SharedPreferences p = prefs();
        try {
            String saved = p.getString(WorkAlarmOptions.RINGTONE_URI_KEY, "");
            Uri uri = saved == null || saved.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : Uri.parse(saved);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
                ringtone.setLooping(true);
                int fadeSeconds = p.getInt(WorkAlarmOptions.FADE_SECONDS_KEY, WorkAlarmOptions.DEFAULT_FADE_SECONDS);
                if (fadeSeconds > 0) {
                    ringtone.setVolume(0.08f);
                    ringtone.play();
                    startFade(fadeSeconds);
                } else {
                    ringtone.setVolume(1f);
                    ringtone.play();
                }
            }
        } catch (Exception ignored) { }

        if (!p.getBoolean(WorkAlarmOptions.VIBRATE_KEY, WorkAlarmOptions.DEFAULT_VIBRATE)) return;
        try {
            VibratorManager vm = getSystemService(VibratorManager.class);
            vibrator = vm == null ? null : vm.getDefaultVibrator();
            if (vibrator != null && vibrator.hasVibrator()) {
                String pattern = p.getString(WorkAlarmOptions.VIBRATE_PATTERN_KEY, WorkAlarmOptions.DEFAULT_VIBRATE_PATTERN);
                long[] wave;
                if ("strong".equals(pattern)) wave = new long[]{0,900,250,900,250};
                else if ("pulse".equals(pattern)) wave = new long[]{0,250,200,250,700};
                else wave = new long[]{0,700,500,700,500};
                vibrator.vibrate(VibrationEffect.createWaveform(wave, 0));
            }
        } catch (Exception ignored) { }
    }

    private void startFade(int seconds) {
        final int steps = Math.max(1, seconds);
        for (int i=1; i<=steps; i++) {
            final float volume = 0.08f + (0.92f * i / steps);
            handler.postDelayed(() -> {
                try { if (ringtone != null && ringtone.isPlaying()) ringtone.setVolume(Math.min(1f, volume)); }
                catch (Exception ignored) { }
            }, i * 1000L);
        }
    }

    private void scheduleAutoStop() {
        int minutes = prefs().getInt(WorkAlarmOptions.AUTO_STOP_MINUTES_KEY, WorkAlarmOptions.DEFAULT_AUTO_STOP_MINUTES);
        if (minutes > 0) handler.postDelayed(this::stopAlarm, minutes * 60_000L);
    }

    private void snooze() {
        SharedPreferences p = prefs();
        boolean enabled = p.getBoolean(WorkAlarmOptions.SNOOZE_ENABLED_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_ENABLED);
        int max = p.getInt(WorkAlarmOptions.SNOOZE_MAX_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MAX);
        if (enabled && currentSnoozeCount < max) {
            int minutes = p.getInt(WorkAlarmOptions.SNOOZE_MINUTES_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES);
            WorkAlarmManager.scheduleSnooze(this, minutes, currentSnoozeCount + 1);
        }
        stopAlarm();
    }

    private void stopAlarm() {
        handler.removeCallbacksAndMessages(null);
        stopAlarmResources();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); stopAlarmResources(); super.onDestroy(); }
    private void stopAlarmResources() {
        try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); } catch (Exception ignored) { }
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) { }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
