package com.example.workhours;

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
