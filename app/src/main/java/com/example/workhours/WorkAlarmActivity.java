package com.example.workhours;

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
