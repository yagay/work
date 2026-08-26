from pathlib import Path

base=Path('app/src/main')
java=base/'java/com/example/workhours'

# Replace WorkAlarmManager with AlarmManager-based unique per-date alarms.
(java/'WorkAlarmManager.java').write_text(r'''package com.example.workhours;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

public final class WorkAlarmManager {
    public static final String ENABLED_KEY = "work_alarm_enabled";
    public static final String FOLLOW_WORK_TIME_KEY = "work_alarm_follow_work_time";
    public static final String ALARM_TIME_KEY = "work_alarm_time";
    public static final String ALARM_LABEL = "上班闹钟";

    private static final String PREFS = "work_hours_prefs";
    private static final String START_TIME_KEY = "start_time";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";
    private static final String REST_RULE_MODE_KEY = "rest_rule_mode";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String REST_PREFIX = "rest_";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String SCHEDULED_DATES_KEY = "work_alarm_scheduled_dates";
    private static final int DAYS_AHEAD = 21;
    private static final int SNOOZE_REQUEST_CODE = 1909010;

    private WorkAlarmManager() { }

    public static boolean sync(Context context) { return rebuild(context); }
    public static boolean syncNextWeek(Context context) { return rebuild(context); }
    public static boolean forceSyncNextWeek(Context context) { return rebuild(context); }
    public static boolean forceSync(Context context) { return rebuild(context); }

    private static boolean rebuild(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cancelScheduled(context);
        if (!prefs.getBoolean(ENABLED_KEY, false)) return true;
        if (!canScheduleExact(context)) return false;

        LocalTime alarmTime = configuredAlarmTime(prefs);
        if (alarmTime == null) return false;

        LocalDate today = LocalDate.now();
        Set<String> scheduled = new HashSet<>();
        for (int i = 0; i < DAYS_AHEAD; i++) {
            LocalDate date = today.plusDays(i);
            if (!isWorkAlarmDay(prefs, date)) continue;
            LocalDateTime when = LocalDateTime.of(date, alarmTime);
            if (!when.isAfter(LocalDateTime.now())) continue;
            if (scheduleDate(context, date, when)) scheduled.add(date.toString());
        }
        prefs.edit().putStringSet(SCHEDULED_DATES_KEY, scheduled).apply();
        return true;
    }

    private static boolean scheduleDate(Context context, LocalDate date, LocalDateTime when) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null || !canScheduleExact(context)) return false;
        Intent fire = new Intent(context, WorkAlarmReceiver.class)
                .setAction(WorkAlarmReceiver.ACTION_FIRE)
                .putExtra("alarm_date", date.toString());
        PendingIntent operation = PendingIntent.getBroadcast(context, requestCode(date), fire,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent show = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showIntent = PendingIntent.getActivity(context, requestCode(date) + 30000000, show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAt = when.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public static void scheduleSnooze(Context context, int minutes) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null || !canScheduleExact(context)) return;
        Intent fire = new Intent(context, WorkAlarmReceiver.class)
                .setAction(WorkAlarmReceiver.ACTION_FIRE)
                .putExtra("alarm_date", LocalDate.now().toString())
                .putExtra("snooze", true);
        PendingIntent operation = PendingIntent.getBroadcast(context, SNOOZE_REQUEST_CODE, fire,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAt = System.currentTimeMillis() + minutes * 60_000L;
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, operation), operation);
        } catch (SecurityException ignored) { }
    }

    public static void cancel(Context context) {
        cancelScheduled(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            PendingIntent snooze = PendingIntent.getBroadcast(context, SNOOZE_REQUEST_CODE,
                    new Intent(context, WorkAlarmReceiver.class).setAction(WorkAlarmReceiver.ACTION_FIRE),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (snooze != null) { am.cancel(snooze); snooze.cancel(); }
        }
    }

    private static void cancelScheduled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> old = prefs.getStringSet(SCHEDULED_DATES_KEY, null);
        if (old != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) for (String raw : new HashSet<>(old)) {
                try {
                    LocalDate d = LocalDate.parse(raw);
                    PendingIntent pi = PendingIntent.getBroadcast(context, requestCode(d),
                            new Intent(context, WorkAlarmReceiver.class).setAction(WorkAlarmReceiver.ACTION_FIRE),
                            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                    if (pi != null) { am.cancel(pi); pi.cancel(); }
                } catch (Exception ignored) { }
            }
        }
        prefs.edit().remove(SCHEDULED_DATES_KEY).apply();
    }

    public static boolean canScheduleExact(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return am != null && (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
    }

    public static boolean canUseFullScreen(Context context) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.canUseFullScreenIntent();
    }

    public static void requestAlarmPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= 31 && !canScheduleExact(activity)) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + activity.getPackageName())));
                return;
            } catch (Exception ignored) { }
        }
        if (Build.VERSION.SDK_INT >= 34 && !canUseFullScreen(activity)) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (Exception ignored) { }
        }
    }

    private static LocalTime configuredAlarmTime(SharedPreferences prefs) {
        boolean follow = prefs.getBoolean(FOLLOW_WORK_TIME_KEY, true);
        String raw = follow ? prefs.getString(START_TIME_KEY, "09:00") : prefs.getString(ALARM_TIME_KEY, "07:30");
        if (raw == null) return null;
        try { return LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm")); }
        catch (DateTimeParseException | IllegalArgumentException e) { return null; }
    }

    private static int requestCode(LocalDate d) {
        return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
    }

    private static boolean isWorkAlarmDay(SharedPreferences prefs, LocalDate date) {
        LocalDate workStart = parseDate(prefs.getString(WORK_START_DATE_KEY, ""));
        if (workStart != null && date.isBefore(workStart)) return false;
        if (HolidayCalendar.isHoliday(prefs, date)) return false;
        if (prefs.getBoolean(LEAVE_PREFIX + date, false)) return false;
        if (prefs.getBoolean(REST_PREFIX + date, false)) return false;
        if (prefs.contains(OVERRIDE_PREFIX + date)) return true;
        if ("monthly".equals(prefs.getString(REST_RULE_MODE_KEY, "weekly")))
            return !getMonthlyRestDays(prefs).contains(date.getDayOfMonth());
        int dayIndex = date.getDayOfWeek().getValue() - 1;
        return prefs.getBoolean("day_" + dayIndex, dayIndex < 5);
    }

    private static Set<Integer> getMonthlyRestDays(SharedPreferences prefs) {
        Set<Integer> result = new HashSet<>();
        String raw = prefs.getString(MONTHLY_REST_DAYS_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return result;
        for (String part : raw.replace('，', ',').split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 31) result.add(value);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try { return LocalDate.parse(raw.trim()); }
        catch (DateTimeParseException e) { return null; }
    }
}
''')

(java/'WorkAlarmReceiver.java').write_text(r'''package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class WorkAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_FIRE = "com.example.workhours.WORK_ALARM_FIRE";
    @Override public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, WorkAlarmRingService.class)
                .setAction(WorkAlarmRingService.ACTION_START)
                .putExtra("alarm_date", intent == null ? "" : intent.getStringExtra("alarm_date"));
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }
}
''')

(java/'WorkAlarmRingService.java').write_text(r'''package com.example.workhours;

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
''')

(java/'WorkAlarmActivity.java').write_text(r'''package com.example.workhours;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WorkAlarmActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppThemeManager.apply(this);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(40), dp(28), dp(40));
        root.setBackgroundColor(UiStyle.PAGE_BG);

        TextView title = new TextView(this); title.setText("上班闹钟"); title.setTextSize(30); title.setTextColor(UiStyle.TEXT); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView time = new TextView(this); time.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); time.setTextSize(64); time.setTextColor(UiStyle.PRIMARY); time.setGravity(Gravity.CENTER); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(18);root.addView(time,tp);
        TextView message = new TextView(this); message.setText("到上班时间了"); message.setTextSize(18); message.setTextColor(UiStyle.TEXT_MUTED); message.setGravity(Gravity.CENTER); LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(8);root.addView(message,mp);

        Button snooze = new Button(this); snooze.setText("稍后 10 分钟"); UiStyle.button(this,snooze,false); snooze.setOnClickListener(v->{ sendAction(WorkAlarmRingService.ACTION_SNOOZE); finishAndRemoveTask(); }); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(54));sp.topMargin=dp(44);root.addView(snooze,sp);
        Button stop = new Button(this); stop.setText("停止闹钟"); UiStyle.button(this,stop,true); stop.setOnClickListener(v->{ sendAction(WorkAlarmRingService.ACTION_STOP); finishAndRemoveTask(); }); LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(-1,dp(58));xp.topMargin=dp(12);root.addView(stop,xp);
        setContentView(root);
        AppThemeManager.applySystemBars(this);
    }
    private void sendAction(String action) { startService(new Intent(this, WorkAlarmRingService.class).setAction(action)); }
    @Override public void onBackPressed() { }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
''')

# Receiver: all weekly/boot/time events rebuild alarms; no external-clock retry notification.
p=java/'WorkAlarmRescheduleReceiver.java'
p.write_text(r'''package com.example.workhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class WorkAlarmRescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("work_hours_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(WorkAlarmManager.ENABLED_KEY, false)) {
            WorkAlarmManager.cancel(context);
            WorkAlarmUpdateScheduler.cancel(context);
            return;
        }
        WorkAlarmManager.forceSync(context);
        WorkAlarmUpdateScheduler.schedule(context);
    }
}
''')

# Manifest permissions/components.
p=base/'AndroidManifest.xml'
s=p.read_text()
insert='''    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />\n    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />\n    <uses-permission android:name="android.permission.VIBRATE" />\n    <uses-permission android:name="android.permission.WAKE_LOCK" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />\n'''
if 'android.permission.SCHEDULE_EXACT_ALARM' not in s:
    s=s.replace('    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n','    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n'+insert)
comp='''\n        <receiver android:name=".WorkAlarmReceiver" android:exported="false" />\n\n        <service\n            android:name=".WorkAlarmRingService"\n            android:exported="false"\n            android:foregroundServiceType="mediaPlayback" />\n\n        <activity\n            android:name=".WorkAlarmActivity"\n            android:exported="false"\n            android:excludeFromRecents="true"\n            android:launchMode="singleTop" />\n'''
if '.WorkAlarmReceiver' not in s:
    s=s.replace('        <receiver\n            android:name=".WorkAlarmRescheduleReceiver"',comp+'\n        <receiver\n            android:name=".WorkAlarmRescheduleReceiver"')
p.write_text(s)

# Settings text + request exact/full-screen access when enabled.
p=java/'SettingsActivity.java'
s=p.read_text()
s=s.replace('开启后按本周实际工作日期同步到手机系统时钟。公共假日、请假、手动休息和每月固定休息日会从本周闹钟星期中排除；App 本身不响铃。','开启后由系统 AlarmManager 保存未来工作日闹钟，App 被清后台也不会影响已设置的闹钟。使用系统默认闹钟铃声和震动；公共假日、请假和休息日自动排除。')
s=s.replace('if (isChecked) WorkAlarmNotification.requestPermissionIfNeeded(this);','if (isChecked) { WorkAlarmNotification.requestPermissionIfNeeded(this); WorkAlarmManager.requestAlarmPermissions(this); }')
s=s.replace('每周日到这个时间重新计算下一周工作日并同步系统时钟。','每周日到这个时间重新计算未来工作日并刷新系统闹钟；不会重复创建同一天的闹钟。')
p.write_text(s)
