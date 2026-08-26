from pathlib import Path

base=Path('app/src/main')
java=base/'java/com/example/workhours'

# Central alarm option keys/defaults.
(java/'WorkAlarmOptions.java').write_text(r'''package com.example.workhours;

final class WorkAlarmOptions {
    static final String RINGTONE_URI_KEY = "work_alarm_ringtone_uri";
    static final String VIBRATE_KEY = "work_alarm_vibrate";
    static final String VIBRATE_PATTERN_KEY = "work_alarm_vibrate_pattern";
    static final String FADE_SECONDS_KEY = "work_alarm_fade_seconds";
    static final String AUTO_STOP_MINUTES_KEY = "work_alarm_auto_stop_minutes";
    static final String SNOOZE_ENABLED_KEY = "work_alarm_snooze_enabled";
    static final String SNOOZE_MINUTES_KEY = "work_alarm_snooze_minutes";
    static final String SNOOZE_MAX_KEY = "work_alarm_snooze_max";
    static final String KEY_ACTION_KEY = "work_alarm_key_action";
    static final String BACK_ACTION_KEY = "work_alarm_back_action";

    static final boolean DEFAULT_VIBRATE = true;
    static final String DEFAULT_VIBRATE_PATTERN = "normal";
    static final int DEFAULT_FADE_SECONDS = 30;
    static final int DEFAULT_AUTO_STOP_MINUTES = 15;
    static final boolean DEFAULT_SNOOZE_ENABLED = true;
    static final int DEFAULT_SNOOZE_MINUTES = 10;
    static final int DEFAULT_SNOOZE_MAX = 3;
    static final String DEFAULT_KEY_ACTION = "snooze";
    static final String DEFAULT_BACK_ACTION = "snooze";

    private WorkAlarmOptions() { }
}
''')

# Replace ring service with configurable ringtone/vibration/fade/auto-stop/snooze count.
(java/'WorkAlarmRingService.java').write_text(r'''package com.example.workhours;

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
''')

# Update AlarmManager snooze to carry snooze count.
p=java/'WorkAlarmManager.java'
s=p.read_text()
s=s.replace('''    public static void scheduleSnooze(Context context, int minutes) {''','''    public static void scheduleSnooze(Context context, int minutes, int snoozeCount) {''')
s=s.replace('''.putExtra("snooze", true);''','''.putExtra("snooze", true)\n                .putExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, snoozeCount);''')
p.write_text(s)

# Receiver forwards snooze count.
p=java/'WorkAlarmReceiver.java'
s=p.read_text()
s=s.replace('''.putExtra("alarm_date", intent == null ? "" : intent.getStringExtra("alarm_date"));''','''.putExtra("alarm_date", intent == null ? "" : intent.getStringExtra("alarm_date"))\n                .putExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, intent == null ? 0 : intent.getIntExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, 0));''')
p.write_text(s)

# Full-screen activity with configurable key/back actions.
(java/'WorkAlarmActivity.java').write_text(r'''package com.example.workhours;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WorkAlarmActivity extends Activity {
    private static final String PREFS = "work_hours_prefs";
    private int snoozeCount;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppThemeManager.apply(this);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        snoozeCount = getIntent().getIntExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT, 0);

        SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int snoozeMinutes = p.getInt(WorkAlarmOptions.SNOOZE_MINUTES_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES);
        int snoozeMax = p.getInt(WorkAlarmOptions.SNOOZE_MAX_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MAX);
        boolean snoozeEnabled = p.getBoolean(WorkAlarmOptions.SNOOZE_ENABLED_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_ENABLED) && snoozeCount < snoozeMax;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(40), dp(28), dp(40));
        root.setBackgroundColor(UiStyle.PAGE_BG);

        TextView title = new TextView(this); title.setText("上班闹钟"); title.setTextSize(30); title.setTextColor(UiStyle.TEXT); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView time = new TextView(this); time.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); time.setTextSize(64); time.setTextColor(UiStyle.PRIMARY); time.setGravity(Gravity.CENTER); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(18);root.addView(time,tp);
        TextView message = new TextView(this); message.setText(snoozeCount > 0 ? "稍后提醒 · 第 " + snoozeCount + " 次" : "到上班时间了"); message.setTextSize(18); message.setTextColor(UiStyle.TEXT_MUTED); message.setGravity(Gravity.CENTER); LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(8);root.addView(message,mp);

        if (snoozeEnabled) {
            Button snooze = new Button(this); snooze.setText("稍后 " + snoozeMinutes + " 分钟"); UiStyle.button(this,snooze,false); snooze.setOnClickListener(v->doAction(WorkAlarmRingService.ACTION_SNOOZE)); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(54));sp.topMargin=dp(44);root.addView(snooze,sp);
        }
        Button stop = new Button(this); stop.setText("停止闹钟"); UiStyle.button(this,stop,true); stop.setOnClickListener(v->doAction(WorkAlarmRingService.ACTION_STOP)); LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(-1,dp(58));xp.topMargin=dp(12);root.addView(stop,xp);
        setContentView(root);
        AppThemeManager.applySystemBars(this);
    }

    private void doAction(String action) {
        startService(new Intent(this, WorkAlarmRingService.class).setAction(action));
        finishAndRemoveTask();
    }

    private boolean handleConfiguredKey() {
        SharedPreferences p=getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String action=p.getString(WorkAlarmOptions.KEY_ACTION_KEY, WorkAlarmOptions.DEFAULT_KEY_ACTION);
        if ("stop".equals(action)) { doAction(WorkAlarmRingService.ACTION_STOP); return true; }
        if ("snooze".equals(action)) { doAction(WorkAlarmRingService.ACTION_SNOOZE); return true; }
        return false;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction()==KeyEvent.ACTION_DOWN) {
            int k=event.getKeyCode();
            if (k==KeyEvent.KEYCODE_VOLUME_UP || k==KeyEvent.KEYCODE_VOLUME_DOWN || k==KeyEvent.KEYCODE_VOLUME_MUTE
                    || k==KeyEvent.KEYCODE_HEADSETHOOK || k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || k==KeyEvent.KEYCODE_MEDIA_STOP || k==KeyEvent.KEYCODE_MEDIA_NEXT || k==KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                if (handleConfiguredKey()) return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() {
        String action=getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WorkAlarmOptions.BACK_ACTION_KEY, WorkAlarmOptions.DEFAULT_BACK_ACTION);
        if ("stop".equals(action)) doAction(WorkAlarmRingService.ACTION_STOP);
        else if ("snooze".equals(action)) doAction(WorkAlarmRingService.ACTION_SNOOZE);
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
''')

# Manifest permissions for full alarm behavior.
p=base/'AndroidManifest.xml'
s=p.read_text()
for perm in [
    '<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />',
    '<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />',
    '<uses-permission android:name="android.permission.VIBRATE" />',
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />']:
    if perm not in s:
        s=s.replace('<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />', '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n    '+perm)
s=s.replace('''        <service\n            android:name=".WorkAlarmRingService"\n            android:exported="false" />''','''        <service\n            android:name=".WorkAlarmRingService"\n            android:exported="false"\n            android:foregroundServiceType="mediaPlayback" />''')
p.write_text(s)

# Settings UI fields/imports/options.
p=java/'SettingsActivity.java'
s=p.read_text()
s=s.replace('import android.app.DatePickerDialog;','import android.app.DatePickerDialog;\nimport android.media.RingtoneManager;')
s=s.replace('''    private static final int REQUEST_IMPORT = 2002;''','''    private static final int REQUEST_IMPORT = 2002;\n    private static final int REQUEST_RINGTONE = 2003;''')
s=s.replace('''    private EditText alarmUpdateTimeInput;''','''    private EditText alarmUpdateTimeInput;\n    private Button alarmRingtoneButton;\n    private CheckBox alarmVibrateCheck;\n    private Button vibrationPatternButton;\n    private Button fadeButton;\n    private Button autoStopButton;\n    private CheckBox snoozeEnabledCheck;\n    private Button snoozeMinutesButton;\n    private Button snoozeMaxButton;\n    private Button alarmKeyActionButton;\n    private Button alarmBackActionButton;''')

anchor='''        LinearLayout dailyHoursRow = settingInputRow("每天工时（自动）", dp(126));'''
ui=r'''        LinearLayout alarmBehaviorSection = createCollapsibleSection(root, "闹钟响铃设置", false);
        TextView behaviorInfo = text("单独设置铃声、震动、渐强、稍后提醒和按键行为。电源键/Home 键由 Android 系统保留，App 无法可靠拦截。", 13, false);
        behaviorInfo.setPadding(0, 0, 0, dp(8));
        alarmBehaviorSection.addView(behaviorInfo);

        alarmRingtoneButton = new Button(this);
        UiStyle.button(this, alarmRingtoneButton, false);
        alarmRingtoneButton.setOnClickListener(v -> chooseAlarmRingtone());
        LinearLayout ringtoneRow = settingInputRow("闹钟铃声", dp(180));
        ringtoneRow.addView(alarmRingtoneButton, compactInputParams(dp(180)));
        alarmBehaviorSection.addView(ringtoneRow);

        alarmVibrateCheck = new CheckBox(this);
        alarmVibrateCheck.setText("响铃时震动"); alarmVibrateCheck.setTextSize(16);
        alarmBehaviorSection.addView(alarmVibrateCheck);

        vibrationPatternButton = optionButton();
        vibrationPatternButton.setOnClickListener(v -> chooseOption("震动方式", vibrationPatternButton,
                new String[]{"普通","强烈","间歇"}, new String[]{"normal","strong","pulse"}));
        LinearLayout vibrationRow = settingInputRow("震动方式", dp(150));
        vibrationRow.addView(vibrationPatternButton, compactInputParams(dp(150))); alarmBehaviorSection.addView(vibrationRow);

        fadeButton = optionButton();
        fadeButton.setOnClickListener(v -> chooseIntOption("音量渐强", fadeButton,
                new String[]{"关闭","15 秒","30 秒","60 秒"}, new int[]{0,15,30,60}, WorkAlarmOptions.FADE_SECONDS_KEY));
        LinearLayout fadeRow = settingInputRow("音量渐强", dp(150)); fadeRow.addView(fadeButton, compactInputParams(dp(150))); alarmBehaviorSection.addView(fadeRow);

        autoStopButton = optionButton();
        autoStopButton.setOnClickListener(v -> chooseIntOption("自动停止", autoStopButton,
                new String[]{"不自动停止","5 分钟","10 分钟","15 分钟","30 分钟"}, new int[]{0,5,10,15,30}, WorkAlarmOptions.AUTO_STOP_MINUTES_KEY));
        LinearLayout autoStopRow = settingInputRow("自动停止", dp(150)); autoStopRow.addView(autoStopButton, compactInputParams(dp(150))); alarmBehaviorSection.addView(autoStopRow);

        snoozeEnabledCheck = new CheckBox(this); snoozeEnabledCheck.setText("允许稍后提醒"); snoozeEnabledCheck.setTextSize(16); alarmBehaviorSection.addView(snoozeEnabledCheck);
        snoozeMinutesButton = optionButton();
        snoozeMinutesButton.setOnClickListener(v -> chooseIntOption("稍后提醒间隔", snoozeMinutesButton,
                new String[]{"5 分钟","10 分钟","15 分钟","20 分钟"}, new int[]{5,10,15,20}, WorkAlarmOptions.SNOOZE_MINUTES_KEY));
        LinearLayout snoozeRow = settingInputRow("稍后提醒间隔", dp(150)); snoozeRow.addView(snoozeMinutesButton, compactInputParams(dp(150))); alarmBehaviorSection.addView(snoozeRow);

        snoozeMaxButton = optionButton();
        snoozeMaxButton.setOnClickListener(v -> chooseIntOption("最多稍后次数", snoozeMaxButton,
                new String[]{"1 次","2 次","3 次","5 次","不限"}, new int[]{1,2,3,5,99}, WorkAlarmOptions.SNOOZE_MAX_KEY));
        LinearLayout snoozeMaxRow = settingInputRow("最多稍后次数", dp(150)); snoozeMaxRow.addView(snoozeMaxButton, compactInputParams(dp(150))); alarmBehaviorSection.addView(snoozeMaxRow);

        alarmKeyActionButton = optionButton();
        alarmKeyActionButton.setOnClickListener(v -> chooseOption("音量/媒体键行为", alarmKeyActionButton,
                new String[]{"稍后提醒","停止闹钟","保持系统音量功能"}, new String[]{"snooze","stop","volume"}));
        LinearLayout keyRow = settingInputRow("音量/媒体键", dp(180)); keyRow.addView(alarmKeyActionButton, compactInputParams(dp(180))); alarmBehaviorSection.addView(keyRow);

        alarmBackActionButton = optionButton();
        alarmBackActionButton.setOnClickListener(v -> chooseOption("返回键行为", alarmBackActionButton,
                new String[]{"稍后提醒","停止闹钟","禁用返回键"}, new String[]{"snooze","stop","none"}));
        LinearLayout backRow = settingInputRow("返回键", dp(150)); backRow.addView(alarmBackActionButton, compactInputParams(dp(150))); alarmBehaviorSection.addView(backRow);

        Button testAlarm = new Button(this); testAlarm.setText("测试闹钟（立即响铃）"); UiStyle.button(this, testAlarm, true);
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(-1, dp(52)); testParams.topMargin=dp(12); alarmBehaviorSection.addView(testAlarm,testParams);
        testAlarm.setOnClickListener(v -> testAlarmNow());

        TextView permissionState = text("精确闹钟和全屏闹钟权限会在开启自动闹钟时申请；如被系统关闭，可再次点“自动设置上班闹钟”进入授权。", 12, false);
        permissionState.setPadding(0,dp(8),0,0); alarmBehaviorSection.addView(permissionState);

'''
if anchor not in s: raise SystemExit('daily hours anchor missing')
s=s.replace(anchor,ui+anchor,1)

# Add ringtone result handling.
s=s.replace('''        if (requestCode == REQUEST_EXPORT) writeBackup(uri);\n        else if (requestCode == REQUEST_IMPORT) readBackupForImport(uri);''','''        if (requestCode == REQUEST_EXPORT) writeBackup(uri);\n        else if (requestCode == REQUEST_IMPORT) readBackupForImport(uri);\n        else if (requestCode == REQUEST_RINGTONE) {\n            Uri picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);\n            prefs.edit().putString(WorkAlarmOptions.RINGTONE_URI_KEY, picked == null ? "" : picked.toString()).apply();\n            updateRingtoneButton();\n        }''')
# Existing early return rejects ringtone picker null data URI. Replace method guard.
s=s.replace('''        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;\n        Uri uri = data.getData();''','''        if (resultCode != RESULT_OK || data == null) return;\n        if (requestCode == REQUEST_RINGTONE) {\n            Uri picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);\n            prefs.edit().putString(WorkAlarmOptions.RINGTONE_URI_KEY, picked == null ? "" : picked.toString()).apply();\n            updateRingtoneButton();\n            return;\n        }\n        if (data.getData() == null) return;\n        Uri uri = data.getData();''')
# remove accidental duplicate ringtone else if if present
s=s.replace('''\n        else if (requestCode == REQUEST_RINGTONE) {\n            Uri picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);\n            prefs.edit().putString(WorkAlarmOptions.RINGTONE_URI_KEY, picked == null ? "" : picked.toString()).apply();\n            updateRingtoneButton();\n        }''','')

# Load option UI.
load_anchor='''        alarmUpdateTimeInput.setText(prefs.getString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, "12:00"));'''
load=r'''        alarmUpdateTimeInput.setText(prefs.getString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, "12:00"));
        alarmVibrateCheck.setChecked(prefs.getBoolean(WorkAlarmOptions.VIBRATE_KEY, WorkAlarmOptions.DEFAULT_VIBRATE));
        snoozeEnabledCheck.setChecked(prefs.getBoolean(WorkAlarmOptions.SNOOZE_ENABLED_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_ENABLED));
        setOptionButton(vibrationPatternButton, prefs.getString(WorkAlarmOptions.VIBRATE_PATTERN_KEY, WorkAlarmOptions.DEFAULT_VIBRATE_PATTERN), new String[]{"普通","强烈","间歇"}, new String[]{"normal","strong","pulse"});
        setIntOptionButton(fadeButton, prefs.getInt(WorkAlarmOptions.FADE_SECONDS_KEY, WorkAlarmOptions.DEFAULT_FADE_SECONDS), new String[]{"关闭","15 秒","30 秒","60 秒"}, new int[]{0,15,30,60});
        setIntOptionButton(autoStopButton, prefs.getInt(WorkAlarmOptions.AUTO_STOP_MINUTES_KEY, WorkAlarmOptions.DEFAULT_AUTO_STOP_MINUTES), new String[]{"不自动停止","5 分钟","10 分钟","15 分钟","30 分钟"}, new int[]{0,5,10,15,30});
        setIntOptionButton(snoozeMinutesButton, prefs.getInt(WorkAlarmOptions.SNOOZE_MINUTES_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MINUTES), new String[]{"5 分钟","10 分钟","15 分钟","20 分钟"}, new int[]{5,10,15,20});
        setIntOptionButton(snoozeMaxButton, prefs.getInt(WorkAlarmOptions.SNOOZE_MAX_KEY, WorkAlarmOptions.DEFAULT_SNOOZE_MAX), new String[]{"1 次","2 次","3 次","5 次","不限"}, new int[]{1,2,3,5,99});
        setOptionButton(alarmKeyActionButton, prefs.getString(WorkAlarmOptions.KEY_ACTION_KEY, WorkAlarmOptions.DEFAULT_KEY_ACTION), new String[]{"稍后提醒","停止闹钟","保持系统音量功能"}, new String[]{"snooze","stop","volume"});
        setOptionButton(alarmBackActionButton, prefs.getString(WorkAlarmOptions.BACK_ACTION_KEY, WorkAlarmOptions.DEFAULT_BACK_ACTION), new String[]{"稍后提醒","停止闹钟","禁用返回键"}, new String[]{"snooze","stop","none"});
        updateRingtoneButton();'''
if load_anchor not in s: raise SystemExit('load anchor missing')
s=s.replace(load_anchor,load,1)

# Save checkbox booleans; button selections are saved immediately by chooser.
s=s.replace('''.putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime);''','''.putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime)\n                .putBoolean(WorkAlarmOptions.VIBRATE_KEY, alarmVibrateCheck.isChecked())\n                .putBoolean(WorkAlarmOptions.SNOOZE_ENABLED_KEY, snoozeEnabledCheck.isChecked());''')
s=s.replace('''"设置已保存 ｜ 系统闹钟写入请求已成功发送"''','''"设置已保存 ｜ 工作日闹钟已刷新"''')
s=s.replace('''"设置已保存 ｜ 系统闹钟写入失败，请确认 OxygenOS 时钟可用"''','''"设置已保存 ｜ 无法设置精确闹钟，请检查“闹钟和提醒”权限"''')

# Helper methods before setAppTheme.
helper_anchor='''    private void setAppTheme(String mode) {'''
helpers=r'''    private Button optionButton() {
        Button b=new Button(this); UiStyle.button(this,b,false); return b;
    }

    private void chooseAlarmRingtone() {
        Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        String saved=prefs.getString(WorkAlarmOptions.RINGTONE_URI_KEY,"");
        Uri existing=saved==null||saved.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(saved);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,existing);
        startActivityForResult(i,REQUEST_RINGTONE);
    }

    private void updateRingtoneButton() {
        if(alarmRingtoneButton==null)return;
        String saved=prefs.getString(WorkAlarmOptions.RINGTONE_URI_KEY,"");
        Uri uri=saved==null||saved.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(saved);
        try { android.media.Ringtone r=RingtoneManager.getRingtone(this,uri); String title=r==null?"系统默认":r.getTitle(this); alarmRingtoneButton.setText(title); }
        catch(Exception e){alarmRingtoneButton.setText("系统默认");}
    }

    private void chooseOption(String title, Button button, String[] labels, String[] values) {
        new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,w)->{
            button.setTag(values[w]); button.setText(labels[w]);
            String key = button==vibrationPatternButton ? WorkAlarmOptions.VIBRATE_PATTERN_KEY : button==alarmKeyActionButton ? WorkAlarmOptions.KEY_ACTION_KEY : WorkAlarmOptions.BACK_ACTION_KEY;
            prefs.edit().putString(key,values[w]).apply();
        }).show();
    }

    private void chooseIntOption(String title, Button button, String[] labels, int[] values, String key) {
        new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,w)->{button.setTag(values[w]);button.setText(labels[w]);prefs.edit().putInt(key,values[w]).apply();}).show();
    }

    private void setOptionButton(Button button,String value,String[] labels,String[] values){
        int found=0;for(int i=0;i<values.length;i++)if(values[i].equals(value)){found=i;break;}button.setTag(values[found]);button.setText(labels[found]);
    }
    private void setIntOptionButton(Button button,int value,String[] labels,int[] values){
        int found=0;for(int i=0;i<values.length;i++)if(values[i]==value){found=i;break;}button.setTag(values[found]);button.setText(labels[found]);
    }

    private void testAlarmNow() {
        WorkAlarmNotification.requestPermissionIfNeeded(this);
        WorkAlarmManager.requestAlarmPermissions(this);
        Intent i=new Intent(this,WorkAlarmRingService.class).setAction(WorkAlarmRingService.ACTION_START);
        i.putExtra(WorkAlarmRingService.EXTRA_SNOOZE_COUNT,0);
        try { startForegroundService(i); Toast.makeText(this,"测试闹钟已启动",Toast.LENGTH_SHORT).show(); }
        catch(Exception e){ Toast.makeText(this,"无法启动测试闹钟："+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

'''
if helper_anchor not in s: raise SystemExit('helper anchor missing')
s=s.replace(helper_anchor,helpers+helper_anchor,1)
p.write_text(s)
