package com.example.workhours;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WorkAlarmActivity extends Activity {
    private Ringtone ringtone;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(36), dp(28), dp(36));
        UiStyle.page(root);

        TextView title = new TextView(this);
        title.setText("上班闹钟");
        title.setTextSize(30);
        title.setTextColor(UiStyle.TEXT);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView time = new TextView(this);
        time.setText(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm\nM月d日 EEEE", Locale.CHINA)));
        time.setTextSize(24);
        time.setTextColor(UiStyle.TEXT);
        time.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
        timeParams.topMargin = dp(18);
        root.addView(time, timeParams);

        TextView message = new TextView(this);
        message.setText("该上班了");
        message.setTextSize(18);
        message.setTextColor(0xFF5F6368);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(-1, -2);
        msgParams.topMargin = dp(12);
        root.addView(message, msgParams);

        Button stop = new Button(this);
        stop.setText("停止闹钟");
        UiStyle.button(this, stop, true);
        stop.setOnClickListener(v -> stopAndFinish());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, dp(58));
        stopParams.topMargin = dp(32);
        root.addView(stop, stopParams);

        setContentView(root);
        startAlarmSound();

        // 当前闹钟已经触发，立即安排下一个有效工作日。
        WorkAlarmManager.sync(this);
    }

    private void startAlarmSound() {
        try {
            android.net.Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.setLooping(true);
                ringtone.play();
            }
        } catch (Exception ignored) { }

        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 700, 350, 700, 350}, 0));
            }
        } catch (Exception ignored) { }
    }

    private void stopAndFinish() {
        stopAlarmSound();
        finish();
    }

    private void stopAlarmSound() {
        try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); }
        catch (Exception ignored) { }
        try { if (vibrator != null) vibrator.cancel(); }
        catch (Exception ignored) { }
    }

    @Override
    protected void onDestroy() {
        stopAlarmSound();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        stopAndFinish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
